/**
 * Copyright (C) 2014 eXo Platform SAS.
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

import org.apache.shindig.common.Nullable;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.oauth2.OAuth2RequestException;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;
import org.apache.shindig.gadgets.oauth2.OAuth2Token.Type;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Encrypter;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2PersistenceException;
import org.apache.shindig.gadgets.oauth2.persistence.sample.JSONOAuth2Persister;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.gadget.core.GateInContainerConfigLoader;
import org.exoplatform.portal.gadget.core.GateInGuiceServletContextListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Persistence implementation that reads <code>config/oauth2.json</code> on startup
 */
@Singleton
public class OAuth2Persister extends JSONOAuth2Persister {
    private static final String OAUTH2_CONFIG = "config/oauth2.json";
    private static final Log LOG = ExoLogger.getLogger(OAuth2Persister.class);
    private OAuth2TokenService service;

    @Inject
    public OAuth2Persister(OAuth2Encrypter encrypter, Authority authority, String globalRedirectUri,
            @Nullable @Named("shindig.contextroot") String contextRoot) throws OAuth2PersistenceException {
        super(encrypter, authority, globalRedirectUri, contextRoot, OAuth2Persister
                .getJSONConfig(OAuth2Persister.OAUTH2_CONFIG));
    }

    public OAuth2Token findToken(String gadgetUri, String serviceName, String user, String scope, Type type)
            throws OAuth2PersistenceException {
        return getService().getToken(gadgetUri, serviceName, user, scope, type.toString());
    }

    public void insertToken(OAuth2Token token) {
        try {
            getService().createToken(build(token));
        } catch (Exception e) {
            LOG.error("Can't save token", e);
        }
    }

    public boolean removeToken(String gadgetUri, String serviceName, String user, String scope, Type type) {
        OAuth2GadgetToken token = getService().deleteToken(gadgetUri, serviceName, user, scope, type.toString());
        return token != null;
    }

    public void updateToken(OAuth2Token token) {
        //Create or update
        insertToken(token);
    }

    private OAuth2GadgetToken build(OAuth2Token token) throws OAuth2RequestException {
        OAuth2GadgetToken tkn = new OAuth2GadgetToken();
        tkn.setExpiresAt(token.getExpiresAt());
        tkn.setGadgetUri(token.getGadgetUri());
        tkn.setIssuedAt(token.getIssuedAt());
        tkn.setMacAlgorithm(token.getMacAlgorithm());
        tkn.setMacExt(token.getMacExt());
        tkn.setMacSecret(token.getMacSecret());
        tkn.setSecret(token.getSecret());
        tkn.setProperties(token.getProperties());
        tkn.setScope(token.getScope());
        tkn.setType(token.getType());
        tkn.setUser(token.getUser());
        tkn.setServiceName(token.getServiceName());
        tkn.setTokenType(token.getTokenType());
        return tkn;
    }

    private static JSONObject getJSONConfig(String location) throws OAuth2PersistenceException {
        try {
            GateInContainerConfigLoader currentLoader = GateInGuiceServletContextListener.getCurrentLoader();
            String content = currentLoader.loadContentAsString(location, "UTF-8");
            if (content == null) {
                content = ResourceLoader.getContent(location);
            }
            return new JSONObject(content);
        } catch (Exception e) {
            LOG.error("Can't load oauth2 json configuration file", e);
            throw new OAuth2PersistenceException(e);
        }
    }

    private OAuth2TokenService getService() {
        if (service == null) {
            ExoContainer container = PortalContainer.getInstance();
            service = (OAuth2TokenService) container.getComponentInstanceOfType(OAuth2TokenService.class);
        }
        return service;
    }
}