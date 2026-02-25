package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.dto.ConsumptionDTO;

public interface ConsumptionService {

    PageResponse<ConsumptionDTO> getConsumptionList(Integer page, Integer pageSize);
}
