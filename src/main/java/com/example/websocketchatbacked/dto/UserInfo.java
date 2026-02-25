package com.example.websocketchatbacked.dto;

import java.util.List;

public class UserInfo {
    private String username;
    private String role;
    private List<String> permissions;

    public UserInfo() {
    }

    public UserInfo(String username, String role, List<String> permissions) {
        this.username = username;
        this.role = role;
        this.permissions = permissions;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
