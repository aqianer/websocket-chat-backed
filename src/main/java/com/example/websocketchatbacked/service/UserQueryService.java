package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.dto.UserQueryDTO;

import java.util.List;

public interface UserQueryService {

    List<UserQueryDTO> queryUser(String username);
}
