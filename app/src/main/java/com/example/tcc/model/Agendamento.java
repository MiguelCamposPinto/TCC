package com.example.tcc.model;

public class Agendamento {
    private String userId;
    private String userName;
    private String date;
    private String startTime;
    private String endTime;
    private String status;
    private String firestorePath;
    private String spaceName;
    private String machineName;
    private String buildingId;
    private String spaceId;
    private String machineId;
    private int durationMin;
    private String spaceType;

    public Agendamento() {
    }

    public Agendamento(String userId, String date, String startTime, String endTime, String status, int durationMin, String spaceType) {
        this.userId = userId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.durationMin = durationMin;
        this.spaceType = spaceType;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getFirestorePath() {
        return firestorePath;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public String getMachineName() {
        return machineName;
    }


    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setData(String data) {
        this.date = data;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFirestorePath(String firestorePath) {
        this.firestorePath = firestorePath;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public int getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(int durationMin) {
        this.durationMin = durationMin;
    }

    public String getBuildingID() {
        return buildingId;
    }

    public void setBuildingID(String buildingId) {
        this.buildingId = buildingId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getSpaceType() {
        return spaceType;
    }

    public void setSpaceType(String spaceType) {
        this.spaceType = spaceType;
    }
}
