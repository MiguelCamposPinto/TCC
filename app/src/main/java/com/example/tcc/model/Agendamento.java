package com.example.tcc.model;

public class Agendamento {
    private String id;
    private String userId;
    private String data;
    private String horaInicio;
    private String horaFim;
    private String status;
    private String firestorePath;

    public Agendamento() {
        // Requerido pelo Firestore
    }

    public Agendamento(String id, String userId, String data, String horaInicio, String horaFim, String status) {
        this.id = id;
        this.userId = userId;
        this.data = data;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.status = status;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(String horaFim) {
        this.horaFim = horaFim;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFirestorePath() {
        return firestorePath;
    }

    public void setFirestorePath(String firestorePath) {
        this.firestorePath = firestorePath;
    }
}
