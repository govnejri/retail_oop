package com.retail.model;

import java.time.LocalDateTime;


public class Unit {
    private Integer id;
    private String name;
    private String shortName;
    private LocalDateTime createdAt;

    public Unit() {
    }

    public Unit(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("Unit{id=%d, name='%s', shortName='%s'}", id, name, shortName);
    }
}
