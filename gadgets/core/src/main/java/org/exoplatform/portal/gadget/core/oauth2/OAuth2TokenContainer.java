package org.exoplatform.portal.gadget.core.oauth2;

import java.util.Map;

import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.OneToMany;
import org.chromattic.api.annotations.PrimaryType;

@PrimaryType(name = "tkn:oauth2tokencontainer")
public abstract class OAuth2TokenContainer {
  @Create
  protected abstract OAuth2TokenEntity createGadgetToken();

  @OneToMany
  protected abstract Map<String, OAuth2TokenEntity> getGadgetTokens();

  public OAuth2TokenEntity getToken(String gadgetUri,
                                    String serviceName,
                                    String user,
                                    String scope,
                                    String type) {
    String key = buildKey(gadgetUri, serviceName, user, scope, type);
    return getGadgetTokens().get(key);
  }

  public OAuth2TokenEntity removeToken(String gadgetUri,
                                       String serviceName,
                                       String user,
                                       String scope,
                                       String type) {
    OAuth2TokenEntity token = getToken(gadgetUri, serviceName, user, scope, type);
    if (token != null) {
      token.remove();
    }

    return token;
  }

  public OAuth2TokenEntity saveToken(OAuth2GadgetToken token) {
    OAuth2TokenEntity entry = getToken(token.getGadgetUri(),
                                       token.getServiceName(),
                                       token.getUser(),
                                       token.getScope(),
                                       token.getType().toString());

    if (entry == null) {
      entry = createGadgetToken();
      String key = buildKey(token.getGadgetUri(),
              token.getServiceName(),
              token.getUser(),
              token.getScope(),
              token.getType().toString());
      getGadgetTokens().put(key, entry);
    }
    entry.setMacSecret(token.getMacSecret());
    entry.setSecret(token.getSecret());
    entry.setExpiresAt(token.getExpiresAt());
    entry.setGadgetUri(token.getGadgetUri());
    entry.setIssuedAt(token.getIssuedAt());
    entry.setMacAlgorithm(token.getMacAlgorithm());
    entry.setMacExt(token.getMacExt());
    if (token.getProperties() != null) {
        entry.getProperties().putAll(token.getProperties());
    }
    entry.setScope(token.getScope());
    entry.setServiceName(token.getServiceName());
    entry.setTokenType(token.getTokenType());
    entry.setType(token.getType().toString());
    entry.setUser(token.getUser());

    return entry;
  }

  private String buildKey(String gadgetUri,
                                    String serviceName,
                                    String user,
                                    String scope,
                                    String type) {
      gadgetUri = gadgetUri == null ? "" : String.valueOf(gadgetUri.hashCode());
      serviceName = serviceName == null ? "" : String.valueOf(serviceName.hashCode());
      user = user == null ? "" : String.valueOf(user.hashCode());
      scope = scope == null ? "" : String.valueOf(scope.hashCode());
      type = type == null ? "" : String.valueOf(type.hashCode());
      return String.format("%s#%s#%s#%s#%s", gadgetUri, serviceName, user, scope, type);
  }
}
