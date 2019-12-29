package com.thycotic.secrets.jenkins;

import java.io.Serializable;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;

public class ClientSecret extends BaseStandardCredentials implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The credentials of this type with this credentialId that apply to this item
     *
     * @param credentialId
     * @param item
     * @return the credentials or {@code null} if no matching credentials exist
     */
    static ClientSecret get(@Nonnull final String credentialId, @Nullable Item item) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(ClientSecret.class, item, ACL.SYSTEM, Collections.emptyList()),
                new IdMatcher(credentialId));
    }

    private final String clientId, secret;

    @DataBoundConstructor
    public ClientSecret(CredentialsScope scope, final String id, final String description, final String clientId,
            final String secret) {
        super(scope, id, description);
        this.clientId = clientId;
        this.secret = secret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSecret() {
        return secret;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return "DevOps Secrets Vault Client Secret";
        }
    }
}
