package com.example.tcc.model;

public class User {
    private String uid;
    private String name;
    private String email;
    private String buildingId;
    private String type;
    private String photoUrl;

    public User() {
    }

    public User(String uid, String name, String email, String buildingId, String type) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.buildingId = buildingId;
        this.type = type;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getBuildingId() { return buildingId; }
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public String getType() {return type;}

    public void setType(String type) {this.type = type;}
    public String getPhotoUrl() {return photoUrl;}
    public void setPhotoUrl(String photoUrl) {this.photoUrl = photoUrl;}
}
