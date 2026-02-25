package com.example.websocketchatbacked.dto;

import jakarta.validation.constraints.NotBlank;

public class UserQueryRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;



    public UserQueryRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
