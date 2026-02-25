package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.dto.UserListDTO;
import com.example.websocketchatbacked.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @SaCheckPermission("user:manage")
    public ApiResponse<PageResponse<UserListDTO>> getUserList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        PageResponse<UserListDTO> result = userService.getUserList(page, pageSize);
        return ApiResponse.success(result);
    }
}
