/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.gadget.core.oauth2;

import java.util.Collection;

import org.apache.shindig.gadgets.oauth2.OAuth2RequestException;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2EncryptionException;
import org.chromattic.api.ChromatticSession;
import org.exoplatform.commons.chromattic.ChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.chromattic.ContextualTask;
import org.exoplatform.commons.chromattic.SessionContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.web.security.security.PlainTokenService;
import org.exoplatform.web.security.security.TokenServiceInitializationException;
import org.gatein.wci.security.Credentials;

public class OAuth2TokenService extends PlainTokenService<OAuth2GadgetToken, TokenKey> {
    private ChromatticLifeCycle chromatticLifeCycle;

    public OAuth2TokenService(InitParams initParams, ChromatticManager chromatticManager)
            throws TokenServiceInitializationException {
        super(initParams);
        chromatticLifeCycle = chromatticManager.getLifeCycle("oauth2tokens");
    }

    @Override
    public void start() {
        try {
            chromatticLifeCycle.getManager().beginRequest();
            ChromatticSession session = chromatticLifeCycle.openContext().getSession();
            OAuth2TokenContainer tkContainer = session.findByPath(OAuth2TokenContainer.class, "oauth2tokens");
            if (tkContainer == null) {
                session.insert(OAuth2TokenContainer.class, "oauth2tokens");
            }
        } catch (Exception e) {
            log.error("Can't start service", e);
        } finally {
            chromatticLifeCycle.getManager().endRequest(true);
        }

        super.start();
    }

    public OAuth2GadgetToken createToken(final OAuth2GadgetToken token) {
        return new TokenTask<OAuth2GadgetToken>() {
            @Override
            protected OAuth2GadgetToken execute() {
                OAuth2TokenContainer container = getGadgetTokenContainer();
                container.saveToken(token);
                return token;
            }
        }.executeWith(chromatticLifeCycle);
    }

    public OAuth2GadgetToken getToken(TokenKey key) {
        return getToken(key.getGadgetUri(), key.getServiceName(), key.getUser(), key.getScope(), key.getType());
    }

    public OAuth2GadgetToken getToken(final String gadgetUri, final String serviceName, final String user, final String scope,
            final String type) {
        return new TokenTask<OAuth2GadgetToken>() {
            @Override
            protected OAuth2GadgetToken execute() {
                OAuth2TokenEntity entity = getGadgetTokenContainer().getToken(gadgetUri, serviceName, user, scope, type);
                try {
                    return getToken(entity);
                } catch (Exception e) {
                    log.error("Can't find token", e);
                    return null;
                }
            }
        }.executeWith(chromatticLifeCycle);
    }

    public OAuth2GadgetToken deleteToken(TokenKey key) {
        return deleteToken(key.getGadgetUri(), key.getServiceName(), key.getUser(), key.getScope(), key.getType());
    }

    public OAuth2GadgetToken deleteToken(final String gadgetUri, final String serviceName, final String user,
            final String scope, final String type) {
        return new TokenTask<OAuth2GadgetToken>() {
            @Override
            protected OAuth2GadgetToken execute() {
                OAuth2GadgetToken token = getToken(gadgetUri, serviceName, user, scope, type);
                getGadgetTokenContainer().removeToken(gadgetUri, serviceName, user, scope, type);
                return token;
            }
        }.executeWith(chromatticLifeCycle);
    }

    @Override
    public TokenKey[] getAllTokens() {
        return new TokenTask<TokenKey[]>() {
            @Override
            protected TokenKey[] execute() {
                OAuth2TokenContainer container = getGadgetTokenContainer();
                Collection<OAuth2TokenEntity> tokens = container.getGadgetTokens().values();

                int i = 0;
                TokenKey[] keys = new TokenKey[tokens.size()];
                for (OAuth2TokenEntity t : tokens) {
                    keys[i++] = new TokenKey(t.getGadgetUri(), t.getServiceName(), t.getUser(), t.getScope(), t.getType());
                }
                return keys;
            }
        }.executeWith(chromatticLifeCycle);
    }

    @Override
    public long size() {
        return new TokenTask<Long>() {
            @Override
            protected Long execute() {
                OAuth2TokenContainer container = getGadgetTokenContainer();
                Collection<OAuth2TokenEntity> tokens = container.getGadgetTokens().values();
                return (long) tokens.size();
            }
        }.executeWith(chromatticLifeCycle);
    }

    public String createToken(Credentials credentials) throws IllegalArgumentException, NullPointerException {
        throw new UnsupportedOperationException();
    }

    protected TokenKey decodeKey(String stringKey) {
        throw new UnsupportedOperationException();
    }

    private OAuth2GadgetToken getToken(OAuth2TokenEntity entity) throws OAuth2EncryptionException, OAuth2RequestException {
        OAuth2GadgetToken token = new OAuth2GadgetToken();
        if (entity == null) {
            return null;
        }
        token.setExpiresAt(entity.getExpiresAt());
        token.setGadgetUri(entity.getGadgetUri());
        token.setIssuedAt(entity.getIssuedAt());
        token.setMacAlgorithm(entity.getMacAlgorithm());
        token.setMacExt(entity.getMacExt());
        token.setMacSecret(entity.getMacSecret());
        token.setProperties(entity.getProperties());
        token.setScope(entity.getScope());
        token.setSecret(entity.getSecret());
        token.setServiceName(entity.getServiceName());
        token.setTokenType(entity.getTokenType());
        token.setType(OAuth2GadgetToken.Type.valueOf(entity.getType()));
        token.setUser(entity.getUser());
        return token;
    }

    /**
     * Wraps token store logic conveniently.
     *
     * @param <V> the return type
     */
    private abstract class TokenTask<V> extends ContextualTask<V> {

        /** . */
        private SessionContext context;

        protected final OAuth2TokenContainer getGadgetTokenContainer() {
            ChromatticSession session = context.getSession();
            return session.findByPath(OAuth2TokenContainer.class, "oauth2tokens");
        }

        @Override
        protected V execute(SessionContext context) {
            this.context = context;

            //
            try {
                return execute();
            } finally {
                this.context = null;
            }
        }

        protected abstract V execute();

    }
}
