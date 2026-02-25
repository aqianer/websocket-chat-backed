package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileRecord, Long> {
}
