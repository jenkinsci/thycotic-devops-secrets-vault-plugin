package com.thycotic.secrets.jenkins;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thycotic.secrets.vault.spring.Secret;
import com.thycotic.secrets.vault.spring.SecretsVault;
import com.thycotic.secrets.vault.spring.SecretsVaultFactoryBean;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;

public class VaultBuildWrapper extends SimpleBuildWrapper {
    private static final String CLIENT_ID_PROPERTY = "secrets_vault.client_id";
    private static final String CLIENT_SECRET_PROPERTY = "secrets_vault.client_secret";
    private static final String TENANT_PROPERTY = "secrets_vault.tenant";

    private List<VaultSecret> secrets;

    @DataBoundConstructor
    public VaultBuildWrapper(final List<VaultSecret> secrets) {
        this.secrets = secrets;
    }

    public List<VaultSecret> getSecrets() {
        return secrets;
    }

    @DataBoundSetter
    public void setSecrets(final List<VaultSecret> secrets) {
        this.secrets = secrets;
    }

    @Override
    public void setUp(final Context context, final Run<?, ?> build, final FilePath workspace, final Launcher launcher,
            final TaskListener listener, final EnvVars initialEnvironment) throws IOException, InterruptedException {
        final VaultConfiguration configuration = VaultConfiguration.get();
        final Map<String, Object> properties = new HashMap<>();

        secrets.forEach(vaultSecret -> {
            final String overrideCredentialId = vaultSecret.getCredentialId();
            final ClientSecret clientSecret;

            if (StringUtils.isNotBlank(overrideCredentialId)) {
                clientSecret = ClientSecret.get(overrideCredentialId, null);
            } else {
                clientSecret = ClientSecret.get(configuration.getCredentialId(), null);
            }
            assert (clientSecret != null); // see VaultSecret.DescriptorImpl.doCheckCredentialId

            final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
            // create a new Spring ApplicationContext using a Map as the PropertySource
            properties.put(CLIENT_ID_PROPERTY, clientSecret.getClientId());
            properties.put(CLIENT_SECRET_PROPERTY, clientSecret.getSecret());
            properties.put(TENANT_PROPERTY,
                    StringUtils.defaultIfBlank(vaultSecret.getTenant(), configuration.getTenant()));
            applicationContext.getEnvironment().getPropertySources()
                    .addLast(new MapPropertySource("properties", properties));
            // Register the factoryBean from secrets-java-sdk
            applicationContext.registerBean(SecretsVaultFactoryBean.class);
            applicationContext.refresh();
            // Fetch the secret
            final Secret secret = applicationContext.getBean(SecretsVault.class).getSecret(vaultSecret.getPath());
            // Add each of the dataFields to the environment
            vaultSecret.getMappings().forEach(mapping -> {
                // Prepend the the environment variable prefix
                context.env(StringUtils.trimToEmpty(
                        ExtensionList.lookupSingleton(VaultConfiguration.class).getEnvironmentVariablePrefix())
                        + mapping.getEnvironmentVariable(), secret.getData().get(mapping.getDataField()));
            });
            applicationContext.close();
        });
    }

    @Extension
    @Symbol("withDevOpsSecretsVault")
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(final AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Use Thycotic DevOps Secrets Vault Secrets";
        }
    }
}
