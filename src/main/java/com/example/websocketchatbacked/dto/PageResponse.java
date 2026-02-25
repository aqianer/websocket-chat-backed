package com.example.websocketchatbacked.dto;

import java.util.List;

public class PageResponse<T> {
    private List<T> list;
    private Long total;

    public PageResponse() {
    }

    public PageResponse(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
