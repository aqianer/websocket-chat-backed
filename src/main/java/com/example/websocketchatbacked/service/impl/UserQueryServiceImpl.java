package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.dto.UserQueryDTO;
import com.example.websocketchatbacked.entity.Account;
import com.example.websocketchatbacked.repository.AccountRepository;
import com.example.websocketchatbacked.service.UserQueryService;
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
    public List<UserQueryDTO> queryUser(String username) {
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
