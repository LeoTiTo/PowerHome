package etu.leoh.powerhome.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant un habitat (appartement) dans la résidence
 */
public class Habitat {
    private String id;
    private String name;
    private String apartmentNumber;
    private String residenceId;
    private String accessCode;
    private List<String> residentIds;
    private List<String> deviceIds;

    public Habitat() {
        // Constructeur vide requis pour Firestore
        this.residentIds = new ArrayList<>();
        this.deviceIds = new ArrayList<>();
    }

    public Habitat(String id, String name, String apartmentNumber, String residenceId, String accessCode) {
        this.id = id;
        this.name = name;
        this.apartmentNumber = apartmentNumber;
        this.residenceId = residenceId;
        this.accessCode = accessCode;
        this.residentIds = new ArrayList<>();
        this.deviceIds = new ArrayList<>();
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public String getResidenceId() {
        return residenceId;
    }

    public void setResidenceId(String residenceId) {
        this.residenceId = residenceId;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public List<String> getResidentIds() {
        return residentIds;
    }

    public void setResidentIds(List<String> residentIds) {
        this.residentIds = residentIds;
    }

    public void addResidentId(String residentId) {
        if (this.residentIds == null) {
            this.residentIds = new ArrayList<>();
        }
        this.residentIds.add(residentId);
    }

    public void removeResidentId(String residentId) {
        if (this.residentIds != null) {
            this.residentIds.remove(residentId);
        }
    }

    public List<String> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(List<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public void addDeviceId(String deviceId) {
        if (this.deviceIds == null) {
            this.deviceIds = new ArrayList<>();
        }
        this.deviceIds.add(deviceId);
    }

    public void removeDeviceId(String deviceId) {
        if (this.deviceIds != null) {
            this.deviceIds.remove(deviceId);
        }
    }
} 