package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.FileOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileOperationLogRepository extends JpaRepository<FileOperationLog, Long> {
}
