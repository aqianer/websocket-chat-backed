package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.LoginRequest;
import com.example.websocketchatbacked.dto.LoginResponseData;
import com.example.websocketchatbacked.dto.UserInfo;
import com.example.websocketchatbacked.entity.Admin;
import com.example.websocketchatbacked.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AdminRepository adminRepository;

    @PostMapping("/login")
    public ApiResponse<LoginResponseData> login(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        Admin user = adminRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ApiResponse.error(401, "用户不存在");
        }
        // TODO：明文对比不合理，待优化
        if (!user.getPassword().equals(password)) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        //
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        LoginResponseData loginResponseData = new LoginResponseData();
        loginResponseData.setToken(token);
        ArrayList<String> list = new ArrayList<>();
        String role;
        if (3 == user.getAuthority()) {
            list.add("user:manage");
            list.add("recharge:audit");
            role = "超级管理员";

        } else {
            list.add("recharge:handle");
            list.add("query:user");
            role = "业务操作员";
        }
        loginResponseData.setUserInfo(new UserInfo(user.getUsername(), role, list));
        return ApiResponse.success(loginResponseData);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.success(null);
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        Admin user = adminRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("authority", user.getAuthority());
        return ApiResponse.success(userInfo);
    }
}
