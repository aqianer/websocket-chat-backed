package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.KbPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbPermissionRepository extends JpaRepository<KbPermission, Long> {

    List<KbPermission> findByKbId(Long kbId);

    List<KbPermission> findByKbIdAndRoleId(Long kbId, Long roleId);
}
