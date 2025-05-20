package com.example.tcc.model;

public class Spaces {
    private String id;
    private String name;
    private String buildingId;

    public Spaces() {} // necessário para o Firestore

    public Spaces(String id, String name, String buildingId) {
        this.id = id;
        this.name = name;
        this.buildingId = buildingId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBuildingId() { return buildingId; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
}
