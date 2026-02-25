package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.websocketchatbacked.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/public")
    public ApiResponse<String> publicAccess() {
        return ApiResponse.success("这是一个公开接口，无需登录即可访问");
    }

    @GetMapping("/protected")
    @SaCheckLogin
    public ApiResponse<String> protectedAccess() {
        return ApiResponse.success("这是一个需要登录的接口，您已成功登录");
    }

    @GetMapping("/write")
    @SaCheckLogin
    @SaCheckPermission("1")
    public ApiResponse<String> writeAccess() {
        return ApiResponse.success("这是一个需要写权限的接口");
    }

    @GetMapping("/read")
    @SaCheckLogin
    @SaCheckPermission("2")
    public ApiResponse<String> readAccess() {
        return ApiResponse.success("这是一个需要读权限的接口");
    }

    @GetMapping("/full")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<String> fullAccess() {
        return ApiResponse.success("这是一个需要读写权限的接口");
    }

    @GetMapping("/user-info")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> getUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("loginId", cn.dev33.satoken.stp.StpUtil.getLoginId());
        userInfo.put("tokenValue", cn.dev33.satoken.stp.StpUtil.getTokenValue());
        return ApiResponse.success(userInfo);
    }
}
