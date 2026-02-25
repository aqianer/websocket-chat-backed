package com.example.websocketchatbacked.config;

import cn.dev33.satoken.stp.StpInterface;
import com.example.websocketchatbacked.entity.Admin;
import com.example.websocketchatbacked.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MySaTokenConfig implements StpInterface {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissionList = new ArrayList<>();
        try {
            Long userId = Long.valueOf(loginId.toString());
            Admin user = adminRepository.findById(userId).orElse(null);
            if (user != null && user.getAuthority() != null) {
                permissionList.add(String.valueOf(user.getAuthority()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return permissionList;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}
