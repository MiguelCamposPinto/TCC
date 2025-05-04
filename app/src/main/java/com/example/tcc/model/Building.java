package com.example.tcc.model;

public class Building {
    private String id; // ID do Firestore
    private String name;
    private String address;

    public Building() {}

    public Building(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getAddress() { return address; }

    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
}

