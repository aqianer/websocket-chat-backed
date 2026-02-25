package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.PackageDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageDetailRepository extends JpaRepository<PackageDetail, Long> {

    List<PackageDetail> findByPackageId(Long packageId);
}
