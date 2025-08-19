package com.example.tcc.model;

public class Spaces {
    private String id;
    private String name;
    private String buildingId;
    private String type;

    public Spaces() {}

    public Spaces(String id, String name, String buildingId, String type) {
        this.id = id;
        this.name = name;
        this.buildingId = buildingId;
        this.type = type;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBuildingId() { return buildingId; }
    public String getType() { return type; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public void setType(String type) { this.type = type; }
}
