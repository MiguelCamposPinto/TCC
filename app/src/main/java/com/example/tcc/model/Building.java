package com.example.tcc.model;

import java.util.HashMap;
import java.util.Map;

public class Building {
    private String id;
    private String name;
    private String address;
    private String password;
    private String adminId;


    public Building() {}

    public Building(String name, String address, String password, String adminId) {
        this.name = name;
        this.address = address;
        this.password = password;
        this.adminId = adminId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getAddress() { return address; }

    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("address", address);
        map.put("adminId", adminId);
        map.put("password", password);
        return map;
    }
}

