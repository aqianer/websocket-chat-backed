package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.ProductRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRateRepository extends JpaRepository<ProductRate, Long> {
}
