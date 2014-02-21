/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.gatein.security.oauth.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.impl.UserImpl;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.spi.AccessTokenContext;
import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.spi.OAuthProviderType;
import org.gatein.security.oauth.spi.OAuthRegistrationServices;
import org.gatein.security.oauth.utils.OAuthUtils;

public class OAuthRegistrationServicesImpl implements OAuthRegistrationServices {
    private static Logger log = LoggerFactory.getLogger(OAuthRegistrationServicesImpl.class);

    private final List<String> registerOnFly;

    private final OrganizationService orgService;

    public OAuthRegistrationServicesImpl(InitParams initParams, OrganizationService orgService) {
        ValueParam onFly = initParams.getValueParam("registerOnFly");
        String onFlyValue = onFly == null ? "" : onFly.getValue();
        if(onFlyValue != null && !onFlyValue.isEmpty()) {
            registerOnFly = Arrays.asList(onFlyValue.split(","));
        } else {
            registerOnFly = Collections.EMPTY_LIST;
        }

        this.orgService = orgService;
    }

    @Override
    public boolean isRegistrationOnFly(OAuthProviderType<? extends AccessTokenContext> oauthProviderType) {
        return registerOnFly.contains(oauthProviderType.getKey());
    }

    @Override
    public User detectGateInUser(HttpServletRequest request, OAuthPrincipal<? extends AccessTokenContext> principal) {
        OAuthProviderType providerType = principal.getOauthProviderType();

        String email = principal.getEmail();
        String username = principal.getUserName();
        if(OAuthConstants.OAUTH_PROVIDER_KEY_LINKEDIN.equalsIgnoreCase(providerType.getKey())) {
            username = email.substring(0, email.indexOf('@'));
        }

        User foundUser = null;

        try {
            UserHandler userHandler = orgService.getUserHandler();
            Query query = null;
            ListAccess<User> users = null;

            //Find user by username
            if(username != null) {
                query = new Query();
                query.setUserName(username);
                users = userHandler.findUsersByQuery(query, UserStatus.BOTH);
                if(users != null && users.getSize() > 0) {
                    foundUser = users.load(0, 1)[0];
                }
            }

            //Find by email
            if(foundUser == null && email != null) {
                query = new Query();
                query.setEmail(email);
                users = userHandler.findUsersByQuery(query, UserStatus.BOTH);
                if(users != null && users.getSize() > 0) {
                    foundUser = users.load(0, 1)[0];
                }
            }

            //TODO: Find by other strategy


        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Exception when trying to detect user: ", ex);
            }
        }


        return foundUser;
    }

    @Override
    public User createGateInUser(OAuthPrincipal<? extends AccessTokenContext> principal) {
        OAuthProviderType providerType = principal.getOauthProviderType();

        String email = principal.getEmail();
        String username = principal.getUserName();
        if(OAuthConstants.OAUTH_PROVIDER_KEY_LINKEDIN.equalsIgnoreCase(providerType.getKey())) {
            username = email.substring(0, email.indexOf('@'));
        }

        User user = new UserImpl(username);
        user.setFirstName(principal.getFirstName());
        user.setLastName(principal.getLastName());
        user.setEmail(email);
        user.setDisplayName(principal.getDisplayName());

        try {
            orgService.getUserHandler().createUser(user, true);

            //User profile
            UserProfileHandler profileHandler = orgService.getUserProfileHandler();

            UserProfile newUserProfile = profileHandler.findUserProfileByName(user.getUserName());
            if (newUserProfile == null) {
                newUserProfile = orgService.getUserProfileHandler().createUserProfileInstance(user.getUserName());
            }

            newUserProfile.setAttribute(providerType.getUserNameAttrName(), principal.getUserName());
            profileHandler.saveUserProfile(newUserProfile, true);

        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Exception when trying to create user: " + user + " on-fly");
            }
            user = null;
        }

        return user;
    }
}
