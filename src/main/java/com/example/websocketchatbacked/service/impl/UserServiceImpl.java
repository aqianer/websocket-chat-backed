package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.dto.UserListDTO;
import com.example.websocketchatbacked.entity.Account;
import com.example.websocketchatbacked.entity.UserNumber;
import com.example.websocketchatbacked.repository.AccountRepository;
import com.example.websocketchatbacked.repository.UserNumberRepository;
import com.example.websocketchatbacked.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserNumberRepository userNumberRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public PageResponse<UserListDTO> getUserList(Integer page, Integer pageSize) {
        int pageNum = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum - 1, size);
        Page<Account> accountPage = accountRepository.findAll(pageable);

        List<UserListDTO> dtoList = accountPage.getContent().stream().map(account -> {
            UserListDTO dto = new UserListDTO();
            dto.setId(account.getId());
            dto.setUsername(account.getAccountId());
            dto.setRole("普通用户");
            dto.setStatus(account.getAccountStatus() ? "正常" : "禁用");
            dto.setAccountStatus(account.getAccountStatus() ? "正常" : "欠费");
            dto.setPackageName("畅享套餐99元");

            dto.setPhone(account.getAccountId());

            List<UserListDTO.PhoneInfo> phoneInfos = new ArrayList<>();
            phoneInfos.add(new UserListDTO.PhoneInfo(account.getAccountId(), account.getAccountId()));
            dto.setPhones(phoneInfos);

            dto.setLastRechargeTime(account.getUpdatedAt() != null
                    ? DATE_FORMATTER.format(account.getUpdatedAt()) : "");

            return dto;
        }).collect(Collectors.toList());

        return new PageResponse<>(dtoList, accountPage.getTotalElements());
    }
}
