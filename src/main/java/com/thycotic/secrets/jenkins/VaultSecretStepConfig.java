package com.thycotic.secrets.jenkins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class VaultSecretStepConfig extends AbstractDescribableImpl<VaultSecretStepConfig> implements Serializable {
    private String tenant;
    private String credentialsId;
    private String tld;

    @DataBoundConstructor
    public VaultSecretStepConfig(String tenant, String credentialsId, String tld) {
        this.tenant = tenant;
        this.credentialsId = credentialsId;
        this.tld = tld;
    }

    public String getTenant() {
        return tenant;
    }

    public String getCredentialId() {
        return credentialsId;
    }

    public String getTld() {
        return tld;
    }

    @DataBoundSetter
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @DataBoundSetter
    public void setCredentialId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setTld(String tld) {
        this.tld = tld;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<VaultSecretStepConfig> {
        @Override
        public String getDisplayName() {
            return "DSV Step Configuration";
        }
    }
}
