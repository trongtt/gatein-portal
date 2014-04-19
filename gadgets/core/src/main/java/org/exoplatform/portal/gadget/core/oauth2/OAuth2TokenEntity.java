package org.exoplatform.portal.gadget.core.oauth2;

import java.util.Map;

import org.chromattic.api.annotations.Destroy;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.Properties;
import org.chromattic.api.annotations.Property;
import org.exoplatform.portal.gadget.core.GadgetToken;

@PrimaryType(name = "tkn:oauth2token")
public abstract class OAuth2TokenEntity {

  @Property(name = "expiresAt")
  public abstract long getExpiresAt();

  public abstract void setExpiresAt(long expiresAt);

  @Property(name = "gadgetUri")
  public abstract String getGadgetUri();

  public abstract void setGadgetUri(String gadgetUri);

  @Property(name = "issuedAt")
  public abstract long getIssuedAt();

  public abstract void setIssuedAt(long issuedAt);

  @Property(name = "macAlgorithm")
  public abstract String getMacAlgorithm();

  public abstract void setMacAlgorithm(String macAlgorithm);

  @Property(name = "macExt")
  public abstract String getMacExt();

  public abstract void setMacExt(String macExt);

  @Property(name = "macSecret")
  public abstract byte[] getMacSecret();

  public abstract void setMacSecret(byte[] macSecret);

  @Property(name = "scope")
  public abstract String getScope();

  public abstract void setScope(String scope);

  @Property(name = "secret")
  public abstract byte[] getSecret();

  public abstract void setSecret(byte[] secret);

  @Property(name = "serviceName")
  public abstract String getServiceName();

  public abstract void setServiceName(String serviceName);

  @Property(name = "tokenType")
  public abstract String getTokenType();

  public abstract void setTokenType(String tokenType);

  @Property(name = "type")
  public abstract String getType();

  public abstract void setType(String type);

  @Property(name = "user")
  public abstract String getUser();

  public abstract void setUser(String user);

  @Properties()
  public abstract Map<String, String> getProperties();

  @Destroy
  public abstract void remove();
}
