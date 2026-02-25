package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.dto.PackageUsageDTO;
import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.entity.Account;
import com.example.websocketchatbacked.entity.PackageDetail;
import com.example.websocketchatbacked.repository.AccountRepository;
import com.example.websocketchatbacked.repository.PackageDetailRepository;
import com.example.websocketchatbacked.service.PackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PackageServiceImpl implements PackageService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PackageDetailRepository packageDetailRepository;

    @Override
    public PageResponse<PackageUsageDTO> getPackageUsageList(Integer page, Integer pageSize) {
        int pageNum = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum - 1, size);
        Page<Account> accountPage = accountRepository.findAll(pageable);

        List<PackageUsageDTO> dtoList = accountPage.getContent().stream().map(account -> {
            PackageUsageDTO dto = new PackageUsageDTO();
            dto.setUsername(account.getAccountId());
            dto.setPhone(account.getAccountId());
            dto.setPackageName("畅享套餐99元");

            Long packageId = 1L;
            List<PackageDetail> packageDetails = packageDetailRepository.findByPackageId(packageId);

            for (PackageDetail detail : packageDetails) {
                String resourceType = detail.getResourceType();
                String resourceValue = detail.getResourceValue();

                if ("data".equalsIgnoreCase(resourceType)) {
                    BigDecimal totalData = parseResourceValue(resourceValue);
                    dto.setTotalData(totalData);
                    BigDecimal usedData = generateRandomUsage(totalData);
                    dto.setUsedData(usedData);
                    dto.setDataPercentage(calculatePercentage(usedData, totalData));
                } else if ("voice".equalsIgnoreCase(resourceType)) {
                    Integer totalVoice = parseResourceValue(resourceValue).intValue();
                    dto.setTotalVoice(totalVoice);
                    Integer usedVoice = generateRandomUsage(parseResourceValue(resourceValue)).intValue();
                    dto.setUsedVoice(usedVoice);
                    dto.setVoicePercentage(calculatePercentage(
                            new BigDecimal(usedVoice), new BigDecimal(totalVoice)));
                } else if ("sms".equalsIgnoreCase(resourceType)) {
                    Integer totalSms = parseResourceValue(resourceValue).intValue();
                    dto.setTotalSms(totalSms);
                    Integer usedSms = generateRandomUsage(parseResourceValue(resourceValue)).intValue();
                    dto.setUsedSms(usedSms);
                    dto.setSmsPercentage(calculatePercentage(
                            new BigDecimal(usedSms), new BigDecimal(totalSms)));
                }
            }

            return dto;
        }).collect(Collectors.toList());

        return new PageResponse<>(dtoList, accountPage.getTotalElements());
    }

    private BigDecimal parseResourceValue(String value) {
        if (value == null || value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal generateRandomUsage(BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        double percentage = 0.1 + Math.random() * 0.8;
        return total.multiply(BigDecimal.valueOf(percentage)).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer calculatePercentage(BigDecimal used, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return used.divide(total, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}
