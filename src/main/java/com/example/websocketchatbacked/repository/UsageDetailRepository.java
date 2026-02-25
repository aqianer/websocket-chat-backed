package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.UsageDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsageDetailRepository extends JpaRepository<UsageDetail, Long> {

    List<UsageDetail> findByUserNumber(String userNumber);

    List<UsageDetail> findByUserNumberOrderByUsageStartTimeDesc(String userNumber);
}
