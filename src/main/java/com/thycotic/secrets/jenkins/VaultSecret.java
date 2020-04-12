package com.thycotic.secrets.jenkins;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

/**
 * A DevOps Secrets Vault Secret, identified by it's tenant and path, and a list
 * of mappings from the secret's data fields to environment variables.
 */
public class VaultSecret extends AbstractDescribableImpl<VaultSecret> {
    private final String path;
    private final List<Mapping> mappings;

    public String getPath() {
        return path;
    }

    public List<Mapping> getMappings() {
        return mappings;
    }

    @DataBoundConstructor
    public VaultSecret(final String path, final List<Mapping> mappings) {
        this.path = path;
        this.mappings = mappings;
    }

    private String credentialId, tenant, tld;

    public String getCredentialId() {
        return credentialId;
    }

    @DataBoundSetter
    public void setCredentialId(final String credentialId) {
        this.credentialId = credentialId;
    }

    public String getTenant() {
        return tenant;
    }

    @DataBoundSetter
    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    public String getTld() {
        return tld;
    }

    @DataBoundSetter
    public void setTld(String tld) {
        this.tld = tld;
    }

    public static class Mapping extends AbstractDescribableImpl<Mapping> {
        private final String dataField, environmentVariable;

        public String getDataField() {
            return dataField;
        }

        public String getEnvironmentVariable() {
            return environmentVariable;
        }

        @DataBoundConstructor
        public Mapping(final String dataField, final String environmentVariable) {
            this.dataField = dataField;
            this.environmentVariable = environmentVariable;
        }

        @Extension
        public static final class DescriptorImpl extends Descriptor<Mapping> {
            private static final String NAME_PATTERN = "[a-zA-Z_][a-zA-Z0-9]*";

            @Override
            public String getDisplayName() {
                return "Secret Data Field to Environment Variable Mapping";
            }

            private FormValidation checkPattern(final String value, final String name) {
                if (Pattern.matches(NAME_PATTERN, value))
                    return FormValidation.ok();
                return FormValidation.error(String.format("%s must match %s", name, NAME_PATTERN));

            }

            public FormValidation doCheckCredentialId(@QueryParameter final String value)
                    throws IOException, ServletException {
                if (StringUtils.isBlank(value) && StringUtils.isBlank(VaultConfiguration.get().getCredentialId()))
                    return FormValidation.error("Credentials are required");
                return FormValidation.ok();
            }

            public FormValidation doCheckTenant(@QueryParameter final String value)
                    throws IOException, ServletException {
                if (StringUtils.isBlank(value) && StringUtils.isBlank(VaultConfiguration.get().getTenant()))
                    return FormValidation.error("Tenant is required");
                return FormValidation.ok();
            }

            public FormValidation doCheckTld(@QueryParameter final String value) throws IOException, ServletException {
                if (StringUtils.isBlank(value) && StringUtils.isBlank(VaultConfiguration.get().getTld()))
                    return FormValidation.error("TLD is required");
                return FormValidation.ok();
            }

            public FormValidation doCheckEnvironmentVariable(@QueryParameter final String value)
                    throws IOException, ServletException {
                return checkPattern(value, "Environment Variable");
            }

            public FormValidation doCheckDataField(@QueryParameter final String value)
                    throws IOException, ServletException {
                return checkPattern(value, "Secret Field Name");
            }

        }
    }

    @Extension
    @Symbol("devOpsVaultSecret")
    public static final class DescriptorImpl extends Descriptor<VaultSecret> {
        public String getDisplayName() {
            return "DevOps Secrets Vault Secret";
        }

        // TODO support for credential domains and permissions
        public ListBoxModel doFillCredentialIdItems(@AncestorInPath final Item item) {
            return new StandardListBoxModel().includeAs(ACL.SYSTEM, item, ClientSecret.class).includeEmptyValue();
        }
    }
}
