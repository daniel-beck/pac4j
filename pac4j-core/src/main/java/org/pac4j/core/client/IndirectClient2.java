/*
  Copyright 2012 - 2015 pac4j organization

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.core.client;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.authenticator.LocalCachingAuthenticator;
import org.pac4j.core.credentials.extractor.CredentialsExtractor;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.creator.AuthenticatorProfileCreator;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.core.redirect.RedirectActionBuilder;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.InitializableWebObject;

/**
 * New indirect client type using the {@link RedirectActionBuilder}, {@link CredentialsExtractor}, {@link Authenticator}
 * and {@link ProfileCreator} concepts.
 * 
 * @author Jerome Leleu
 * @since 1.9.0
 */
public abstract class IndirectClient2<C extends Credentials, U extends CommonProfile> extends IndirectClient<C, U> {

    private RedirectActionBuilder redirectActionBuilder;

    private CredentialsExtractor<C> credentialsExtractor;

    private Authenticator<C> authenticator;

    private ProfileCreator<C, U> profileCreator =  AuthenticatorProfileCreator.INSTANCE;

    @Override
    protected void internalInit(final WebContext context) {
        super.internalInit(context);
        CommonHelper.assertNotNull("redirectActionBuilder", this.redirectActionBuilder);
        CommonHelper.assertNotNull("credentialsExtractor", this.credentialsExtractor);
        CommonHelper.assertNotNull("authenticator", this.authenticator);
        CommonHelper.assertNotNull("profileCreator", this.profileCreator);
        if (authenticator instanceof InitializableWebObject) {
            ((InitializableWebObject) this.authenticator).init(context);
        }
    }

    @Override
    protected RedirectAction retrieveRedirectAction(final WebContext context) throws RequiresHttpAction {
        return redirectActionBuilder.redirect(context);
    }

    @Override
    protected C retrieveCredentials(final WebContext context) throws RequiresHttpAction {
        try {
            final C credentials = this.credentialsExtractor.extract(context);
            if (credentials == null) {
                return null;
            }
            this.authenticator.validate(credentials);
            return credentials;
        } catch (CredentialsException e) {
            logger.error("Failed to retrieve or validate credentials", e);
            return null;
        }
    }

    @Override
    protected U retrieveUserProfile(final C credentials, final WebContext context) {
        final U profile = this.profileCreator.create(credentials);
        logger.debug("profile: {}", profile);
        return profile;
    }

    protected void assertAuthenticatorTypes(final Class<? extends Authenticator>... classes) {
        if (this.authenticator != null && classes != null) {
            for (final Class<? extends Authenticator> clazz : classes) {
                Class<? extends Authenticator> authClazz = this.authenticator.getClass();
                if (LocalCachingAuthenticator.class.isAssignableFrom(authClazz)) {
                    authClazz = ((LocalCachingAuthenticator) this.authenticator).getDelegate().getClass();
                }
                if (!clazz.isAssignableFrom(authClazz)) {
                    throw new TechnicalException("Unsupported authenticator type: " + authClazz);
                }
            }
        }
    }

    @Override
    public String toString() {
        return CommonHelper.toString(this.getClass(), "name", getName(), "redirectActionBuilder", this.redirectActionBuilder, "credentialsExtractor", this.credentialsExtractor,
                "authenticator", this.authenticator, "profileCreator", this.profileCreator);
    }

    public RedirectActionBuilder getRedirectActionBuilder() {
        return redirectActionBuilder;
    }

    public void setRedirectActionBuilder(final RedirectActionBuilder redirectActionBuilder) {
        if (this.redirectActionBuilder == null) {
            this.redirectActionBuilder = redirectActionBuilder;
        }
    }

    public CredentialsExtractor<C> getCredentialsExtractor() {
        return credentialsExtractor;
    }

    public void setCredentialsExtractor(final CredentialsExtractor<C> credentialsExtractor) {
        if (this.credentialsExtractor == null) {
            this.credentialsExtractor = credentialsExtractor;
        }
    }

    public Authenticator<C> getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(final Authenticator<C> authenticator) {
        if (this.authenticator == null) {
            this.authenticator = authenticator;
        }
    }

    public ProfileCreator<C, U> getProfileCreator() {
        return profileCreator;
    }

    public void setProfileCreator(final ProfileCreator<C, U> profileCreator) {
        if (this.profileCreator == null || this.profileCreator == AuthenticatorProfileCreator.INSTANCE) {
            this.profileCreator = profileCreator;
        }
    }
}