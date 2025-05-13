package com.example.tcc.model;

public class Machine {
    private String id;
    private String name;
    private String status;
    private String espacoId;

    public Machine() {
        // Construtor vazio necessário para o Firestore
    }

    public Machine(String id, String name, String status, String espacoId) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.espacoId = espacoId;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getEspacoId() {
        return espacoId;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEspacoId(String espacoId) {
        this.espacoId = espacoId;
    }
}
