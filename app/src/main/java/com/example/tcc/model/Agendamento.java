package com.example.tcc.model;

public class Agendamento {
    private String userId;
    private String userName;
    private String data;
    private String horaInicio;
    private String horaFim;
    private String status;
    private String firestorePath;
    private String espacoNome;
    private String machineName;

    public Agendamento() {
    }

    public Agendamento(String userId, String data, String horaInicio, String horaFim, String status) {
        this.userId = userId;
        this.data = data;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getData() {
        return data;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public String getHoraFim() {
        return horaFim;
    }

    public String getStatus() {
        return status;
    }

    public String getFirestorePath() {
        return firestorePath;
    }

    public String getEspacoNome() {
        return espacoNome;
    }

    public String getMachineName() {
        return machineName;
    }

    // Setters

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public void setHoraFim(String horaFim) {
        this.horaFim = horaFim;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFirestorePath(String firestorePath) {
        this.firestorePath = firestorePath;
    }

    public void setEspacoNome(String espacoNome) {
        this.espacoNome = espacoNome;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }
}
