package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.UserNumberProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNumberProductRepository extends JpaRepository<UserNumberProduct, Long> {
}
