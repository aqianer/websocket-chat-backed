package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.UserQueryDTO;
import com.example.websocketchatbacked.dto.UserQueryRequest;
import com.example.websocketchatbacked.service.UserQueryService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserQueryController {

    @Autowired
    private UserQueryService userQueryService;

    @PostMapping("/query")
    @SaCheckPermission("query:user")
    public ApiResponse<List<UserQueryDTO>> queryUser(@Valid @RequestBody UserQueryRequest request) {
        List<UserQueryDTO> result = userQueryService.queryUser(request.getUsername());
        return ApiResponse.success(result);
    }
}
