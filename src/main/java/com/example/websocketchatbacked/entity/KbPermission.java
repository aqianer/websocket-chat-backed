package com.example.websocketchatbacked.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "kb_permission")
public class KbPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permissions", nullable = false, length = 500)
    private String permissions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kb_id", insertable = false, updatable = false)
    private KnowledgeBase knowledgeBase;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }
}
