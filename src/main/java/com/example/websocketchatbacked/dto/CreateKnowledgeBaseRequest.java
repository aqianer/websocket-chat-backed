package com.example.websocketchatbacked.dto;

public class CreateKnowledgeBaseRequest {
    private String name;
    private String type;
    private String owner;
    private String department;
    private Integer vectorDim;
    private String description;

    public CreateKnowledgeBaseRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getVectorDim() {
        return vectorDim;
    }

    public void setVectorDim(Integer vectorDim) {
        this.vectorDim = vectorDim;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
