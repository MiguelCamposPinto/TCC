package com.example.tcc.model;

public class Spaces {
    private String id;
    private String name;
    private String type; // exemplo: "Lavanderia", "Quadra"
    private String buildingId;

    public Spaces() {} // necessário para o Firestore

    public Spaces(String id, String name, String type, String buildingId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.buildingId = buildingId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getBuildingId() { return buildingId; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
}
