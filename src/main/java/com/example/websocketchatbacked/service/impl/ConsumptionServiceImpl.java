package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.dto.ConsumptionDTO;
import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.entity.UsageDetail;
import com.example.websocketchatbacked.repository.UsageDetailRepository;
import com.example.websocketchatbacked.service.ConsumptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsumptionServiceImpl implements ConsumptionService {

    @Autowired
    private UsageDetailRepository usageDetailRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public PageResponse<ConsumptionDTO> getConsumptionList(Integer page, Integer pageSize) {
        int pageNum = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum - 1, size, Sort.by(Sort.Direction.DESC, "usageStartTime"));
        Page<UsageDetail> usageDetailPage = usageDetailRepository.findAll(pageable);

        List<ConsumptionDTO> dtoList = usageDetailPage.getContent().stream().map(usageDetail -> {
            ConsumptionDTO dto = new ConsumptionDTO();
            dto.setId(usageDetail.getId());
            dto.setUsername(usageDetail.getUserNumber());
            dto.setPhone(usageDetail.getUserNumber());
            dto.setAmount(usageDetail.getUsageValue());
            dto.setType(getConsumptionType(usageDetail.getUsageType()));
            dto.setTime(usageDetail.getUsageStartTime() != null
                    ? DATE_FORMATTER.format(usageDetail.getUsageStartTime()) : "");
            dto.setDescription(generateDescription(usageDetail));
            return dto;
        }).collect(Collectors.toList());

        return new PageResponse<>(dtoList, usageDetailPage.getTotalElements());
    }

    private String getConsumptionType(String usageType) {
        if (usageType == null) {
            return "其他";
        }
        switch (usageType.toLowerCase()) {
            case "data":
            case "流量":
                return "流量包";
            case "voice":
            case "语音":
                return "通话费";
            case "sms":
            case "短信":
                return "短信费";
            case "package":
            case "套餐":
                return "套餐费";
            default:
                return "其他";
        }
    }

    private String generateDescription(UsageDetail usageDetail) {
        StringBuilder sb = new StringBuilder();
        String type = getConsumptionType(usageDetail.getUsageType());
        String unit = usageDetail.getUsageUnit();
        String value = usageDetail.getUsageValue().toString();

        switch (type) {
            case "流量包":
                sb.append("购买").append(value).append(unit != null ? unit : "MB").append("流量包");
                break;
            case "通话费":
                sb.append("通话").append(value).append(unit != null ? unit : "分钟");
                break;
            case "短信费":
                sb.append("发送").append(value).append(unit != null ? unit : "条").append("短信");
                break;
            case "套餐费":
                sb.append("套餐费用");
                break;
            default:
                sb.append("消费").append(value).append(unit != null ? unit : "");
        }
        return sb.toString();
    }
}
