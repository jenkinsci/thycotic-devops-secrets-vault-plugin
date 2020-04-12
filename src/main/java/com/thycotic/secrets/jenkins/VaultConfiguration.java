package com.thycotic.secrets.jenkins;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;

@Extension
@Symbol("devOpsSecretsVault")
public class VaultConfiguration extends GlobalConfiguration {
    public static final String DEFAULT_ENVIRONMENT_VARIABLE_PREFIX = "DSV_";
    public static final String DEFAULT_TLD = "com";

    /**
     * Calls {@link hudson.ExtensionList#lookupSingleton(VaultConfiguration.class)}
     * to get the singleton instance of this class which is how the Jenkins
     * documentation recommends that it be accessed.
     *
     * @return the singleton instance of this class
     */
    public static VaultConfiguration get() {
        return ExtensionList.lookupSingleton(VaultConfiguration.class);
    }

    private String credentialId, tenant, tld = DEFAULT_TLD,
            environmentVariablePrefix = DEFAULT_ENVIRONMENT_VARIABLE_PREFIX;

    public VaultConfiguration() {
        load();
    }

    // populates the credentials dropdown menu in the UI
    public ListBoxModel doFillCredentialIdItems(@AncestorInPath final Item item) {
        return new StandardListBoxModel().includeEmptyValue().includeAs(ACL.SYSTEM, item, ClientSecret.class);
    }

    public String getCredentialId() {
        return credentialId;
    }

    @DataBoundSetter
    public void setCredentialId(final String credentialId) {
        this.credentialId = credentialId;
        save();
    }

    public String getEnvironmentVariablePrefix() {
        return environmentVariablePrefix;
    }

    @DataBoundSetter
    public void setEnvironmentVariablePrefix(final String environmentVariablePrefix) {
        this.environmentVariablePrefix = environmentVariablePrefix;
        save();
    }

    public String getTenant() {
        return tenant;
    }

    @DataBoundSetter
    public void setTenant(final String tenant) {
        this.tenant = tenant;
        save();
    }

    public String getTld() {
        return tld;
    }

    @DataBoundSetter
    public void setTld(String tld) {
        this.tld = tld;
        save();
    }
}
