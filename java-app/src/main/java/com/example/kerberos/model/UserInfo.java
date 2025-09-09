package com.example.kerberos.model;

import java.util.List;

public class UserInfo {
    private String username;
    private String principal;
    private List<String> groups;
    private String realm;
    private boolean authenticated;

    public UserInfo() {}

    public UserInfo(String username, String principal, List<String> groups, String realm, boolean authenticated) {
        this.username = username;
        this.principal = principal;
        this.groups = groups;
        this.realm = realm;
        this.authenticated = authenticated;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
