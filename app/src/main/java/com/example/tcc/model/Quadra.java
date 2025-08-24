package com.example.tcc.model;

public class Quadra extends Resource{
    private String id;
    private String name;
    private String status;
    private String spaceType;

    public Quadra() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String nome) { this.name = nome; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSpaceType() {
        return spaceType;
    }

    public void setSpaceType(String spaceType) {
        this.spaceType = spaceType;
    }
}
