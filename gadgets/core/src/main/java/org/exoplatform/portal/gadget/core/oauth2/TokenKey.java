package org.exoplatform.portal.gadget.core.oauth2;


public class TokenKey  {
    private String gadgetUri;
    private String serviceName;
    private String user;
    private String scope;
    private String type;

    public TokenKey(String gadgetUri, String serviceName, String user, String scope, String type) {
        this.gadgetUri = gadgetUri;
        this.serviceName = serviceName;
        this.user = user;
        this.scope = scope;
        this.type = type;
    }

    public String getGadgetUri() {
        return gadgetUri;
    }

    public void setGadgetUri(String gadgetUri) {
        this.gadgetUri = gadgetUri;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
