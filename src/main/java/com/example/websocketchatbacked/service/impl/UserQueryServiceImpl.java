package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.dto.UserQueryDTO;
import com.example.websocketchatbacked.entity.Account;
import com.example.websocketchatbacked.repository.AccountRepository;
import com.example.websocketchatbacked.service.UserQueryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserQueryServiceImpl implements UserQueryService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    @Tool(description = "根据用户名查询系统内的用户信息，当用户说查询用户、找用户时必须调用这个方法")
    public List<UserQueryDTO> queryUser(@ToolParam(description = "用户的用户名") String username) {
        if (username == null || username.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Account> accounts = accountRepository.findAll().stream()
                .filter(account -> account.getAccountId() != null
                        && account.getAccountId().contains(username))
                .collect(Collectors.toList());

        return accounts.stream().map(account -> new UserQueryDTO(
                account.getId(),
                account.getAccountId(),
                account.getTotalBalance()
        )).collect(Collectors.toList());
    }
}
