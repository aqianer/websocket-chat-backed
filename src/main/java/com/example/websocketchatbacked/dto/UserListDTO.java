package com.example.websocketchatbacked.dto;

import java.math.BigDecimal;
import java.util.List;

public class UserListDTO {
    private Long id;
    private String username;
    private String role;
    private String status;
    private String phone;
    private String packageName;
    private String accountStatus;
    private String lastRechargeTime;
    private List<PhoneInfo> phones;

    public static class PhoneInfo {
        private String phone;
        private String phoneId;

        public PhoneInfo() {
        }

        public PhoneInfo(String phone, String phoneId) {
            this.phone = phone;
            this.phoneId = phoneId;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPhoneId() {
            return phoneId;
        }

        public void setPhoneId(String phoneId) {
            this.phoneId = phoneId;
        }
    }

    public UserListDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getLastRechargeTime() {
        return lastRechargeTime;
    }

    public void setLastRechargeTime(String lastRechargeTime) {
        this.lastRechargeTime = lastRechargeTime;
    }

    public List<PhoneInfo> getPhones() {
        return phones;
    }

    public void setPhones(List<PhoneInfo> phones) {
        this.phones = phones;
    }
}
