package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.PromotionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRuleRepository extends JpaRepository<PromotionRule, Long> {
}
