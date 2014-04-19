/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.portal.gadget;

import java.util.HashMap;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.gadget.core.oauth2.OAuth2GadgetToken;
import org.exoplatform.portal.gadget.core.oauth2.OAuth2TokenService;
import org.exoplatform.portal.gadget.core.oauth2.TokenKey;
import org.exoplatform.web.security.AbstractTokenServiceTest;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/oauth2-token-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/gadget-jcr-configuration.xml") })
public class TestOAuth2TokenService extends AbstractTokenServiceTest<OAuth2TokenService> {

    private OAuth2GadgetToken token;

    public TestOAuth2TokenService() {
        super();
    }

    protected void setUp() throws Exception {
        PortalContainer container = getContainer();
        service = (OAuth2TokenService) container.getComponentInstanceOfType(OAuth2TokenService.class);

        token = new OAuth2GadgetToken();
        token.setExpiresAt(System.currentTimeMillis() + 1000);
        token.setGadgetUri("testUri");
        token.setIssuedAt(1000);
        token.setMacAlgorithm("testAlg");
        token.setMacExt("testMacExt");
        token.setMacSecret("testMacSecret".getBytes());
        HashMap<String, String> props = new HashMap<String, String>();
        props.put("test", "value");
        token.setProperties(props);
        token.setScope("testScope");
        token.setSecret("testSecret".getBytes());
        token.setServiceName("testServiceName");
        token.setTokenType("testTokenType");
        token.setType(OAuth2GadgetToken.Type.ACCESS);
        token.setUser("testUser");

        service.createToken(token);
    }

    @Override
    protected void tearDown() throws Exception {
        for (TokenKey key : service.getAllTokens()) {
            service.deleteToken(key);
        }
    }

    @Override
    public void testGetToken() throws Exception {
        OAuth2GadgetToken t = service.getToken(token.getGadgetUri(), token.getServiceName(), token.getUser(), token.getScope(),
                token.getType().toString());
        assertNotNull(t);
        assertEquals(token.getGadgetUri(), t.getGadgetUri());
        assertEquals(token.getUser(), t.getUser());
        assertEquals(token.getTokenType(), t.getTokenType());
    }

    @Override
    public void testGetAllToken() throws Exception {
        TokenKey[] keys = service.getAllTokens();
        assertEquals(1, keys.length);
    }

    @Override
    public void testSize() throws Exception {
        assertEquals(1, service.size());
    }

    @Override
    public void testDeleteToken() throws Exception {
        assertEquals(1, service.size());
        service.deleteToken(token.getGadgetUri(), token.getServiceName(), token.getUser(), token.getScope(), token.getType().toString());
        assertEquals(0, service.size());
    }

    public void testCleanExpiredTokens() throws Exception {
        assertEquals(1, service.size());

        Thread.sleep(2100);
        service.cleanExpiredTokens();
        assertEquals(0, service.size());
    }
}