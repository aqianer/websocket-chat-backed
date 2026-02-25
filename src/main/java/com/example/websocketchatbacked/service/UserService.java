package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.dto.UserListDTO;

public interface UserService {

    PageResponse<UserListDTO> getUserList(Integer page, Integer pageSize);
}
