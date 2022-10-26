package com.thycotic.secrets.jenkins;

import com.thycotic.secrets.vault.spring.Secret;
import com.thycotic.secrets.vault.spring.SecretsVault;
import com.thycotic.secrets.vault.spring.SecretsVaultFactoryBean;

import hudson.console.ConsoleLogFilter;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import javax.annotation.Nonnull;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VaultSecretStep extends Step implements Serializable {
    private String tenant;
    private String secretPath;
    private String secretDataKey;
    private String credentialsId;
    private String tld;

    @DataBoundConstructor
    public VaultSecretStep(VaultSecretStepConfig config, String secretPath, String secretDataKey) {
        this.tenant = config.getTenant();
        this.secretPath = secretPath;
        this.secretDataKey = secretDataKey;
        this.credentialsId = config.getCredentialId();
        this.tld = config.getTld();
    }

    @DataBoundSetter
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }

    @DataBoundSetter
    public void setSecretPath(String secretPath) {
        this.secretPath = secretPath;
    }

    public String getSecretPath() {
        return secretPath;
    }

    @DataBoundSetter
    public void setSecretDataKey(String secretDataKey) {
        this.secretDataKey = secretDataKey;
    }

    public String getSecretDataKey() {
        return secretDataKey;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setTld(String tld) {
        this.tld = tld;
    }

    public String getTld() {
        return tld;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new VaultSecretStepExecution(this, stepContext);
    }

    private static final class VaultSecretStepExecution extends AbstractStepExecutionImpl {
        private static final String CLIENT_ID_PROPERTY = "secrets_vault.client_id";
        private static final String CLIENT_SECRET_PROPERTY = "secrets_vault.client_secret";
        private static final String TENANT_PROPERTY = "secrets_vault.tenant";
        private static final String TLD_PROPERTY = "secrets_vault.tld";

        private static final long serialVersionUID = 1L;
        private transient final VaultSecretStep step;

        private VaultSecretStepExecution(VaultSecretStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        public void onResume() {}

        private void writeObject(ObjectOutputStream stream) throws Exception {
            stream.defaultWriteObject();
        }

        private void readObject(ObjectInputStream stream) throws Exception, ClassNotFoundException {
            stream.defaultReadObject();
        }

        @Override
        public boolean start() throws Exception {
            final ClientSecret clientSecret = ClientSecret.get(step.getCredentialsId(), null);
            final VaultConfiguration configuration = VaultConfiguration.get();
            final Map<String, Object> properties = new HashMap<>();
            final List<String> valuesToMask = new ArrayList<>();

            final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
            properties.put(CLIENT_ID_PROPERTY, clientSecret.getClientId());
            properties.put(CLIENT_SECRET_PROPERTY, clientSecret.getSecret());
            properties.put(TENANT_PROPERTY, StringUtils.defaultIfBlank(step.getTenant(), configuration.getTenant()));
            properties.put(TLD_PROPERTY, StringUtils.defaultIfBlank(step.getTld(), configuration.getTld()));
            applicationContext.getEnvironment().getPropertySources()
                    .addLast(new MapPropertySource("properties", properties));

            // Register the factoryBean from secrets-java-sdk
            applicationContext.registerBean(SecretsVaultFactoryBean.class);
            applicationContext.refresh();

            StepContext context = getContext();

            try {
                // Fetch the secret
                final Secret secret = applicationContext.getBean(SecretsVault.class).getSecret(step.getSecretPath());
                valuesToMask.add(secret.getData().get(step.getSecretDataKey()));
                context.onSuccess(secret.getData().get(step.getSecretDataKey()));
            } catch (Exception e) {
                context.onFailure(e);
            }
            applicationContext.close();
            
            Run<?, ?> run = context.get(Run.class);
            ConsoleLogFilter original = context.get(ConsoleLogFilter.class);
            ConsoleLogFilter subsequent = new VaultConsoleLogFilter(run.getCharset().name(), valuesToMask);

            context.newBodyInvoker().
                withContext(BodyInvoker.mergeConsoleLogFilters(original, subsequent)).
                withCallback(BodyExecutionCallback.wrap(context)).
            	start();

            return false;
        }

        @Override
        public void stop(@Nonnull Throwable throwable) throws Exception {
            getContext().onFailure(throwable);
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor implements Serializable {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<Class<?>>() {{
                add(Run.class);
                add(TaskListener.class);
            }};
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public String getFunctionName() {
            return "dsvSecret";
        }

        private void writeObject(ObjectOutputStream stream) throws Exception {
            stream.defaultWriteObject();
        }

        private void readObject(ObjectInputStream stream) throws Exception, ClassNotFoundException {
            stream.defaultReadObject();
        }
    }
}
