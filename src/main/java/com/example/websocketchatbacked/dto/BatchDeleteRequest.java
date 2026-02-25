package com.example.websocketchatbacked.dto;

import java.util.List;

public class BatchDeleteRequest {
    private List<Long> ids;

    public BatchDeleteRequest() {
    }

    public BatchDeleteRequest(List<Long> ids) {
        this.ids = ids;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
