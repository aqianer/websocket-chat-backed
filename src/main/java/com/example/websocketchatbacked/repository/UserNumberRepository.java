package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.UserNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNumberRepository extends JpaRepository<UserNumber, Long> {

    Optional<UserNumber> findByNumberId(String numberId);
}
