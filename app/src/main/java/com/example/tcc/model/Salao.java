package com.example.tcc.model;

public class Salao extends Resource{
    private String id;
    private String name;
    private int capacidadeMax;
    private String status;
    private String spaceType;

    public Salao() {
    }

    public Salao(String id, String name, int capacidadeMax, String status) {
        this.id = id;
        this.name = name;
        this.capacidadeMax = capacidadeMax;
        this.status = status;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacidadeMax() {
        return capacidadeMax;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setNome(String name) {
        this.name = name;
    }

    public void setCapacidadeMaxima(int capacidadeMaxima) {
        this.capacidadeMax = capacidadeMaxima;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSpaceType() {
        return spaceType;
    }

    public void setSpaceType(String spaceType) {
        this.spaceType = spaceType;
    }
}
