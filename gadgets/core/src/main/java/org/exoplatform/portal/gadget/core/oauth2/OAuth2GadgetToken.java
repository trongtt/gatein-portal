package org.exoplatform.portal.gadget.core.oauth2;

import org.apache.shindig.gadgets.oauth2.persistence.OAuth2TokenPersistence;
import org.exoplatform.web.security.Token;
import org.gatein.wci.security.Credentials;

public class OAuth2GadgetToken extends OAuth2TokenPersistence implements Token {

  private static final long serialVersionUID = -5182283888172190456L;

  public boolean isExpired() {
    return System.currentTimeMillis() > getExpirationTimeMillis();
  }

  public long getExpirationTimeMillis() {
    return getExpiresAt();
  }

  public Credentials getPayload() {
    // Should we return something ?
    return null;
  }
}
