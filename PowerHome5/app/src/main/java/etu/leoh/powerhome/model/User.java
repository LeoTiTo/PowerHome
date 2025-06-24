package etu.leoh.powerhome.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant un utilisateur/résident dans l'application PowerHome
 */
public class User {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String habitatId;
    private int ecoCoins;
    private List<String> devices;

    public User() {
        // Constructeur vide requis pour Firestore
        this.devices = new ArrayList<>();
        this.ecoCoins = 0;
    }

    public User(String id, String email, String firstName, String lastName, String habitatId) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.habitatId = habitatId;
        this.devices = new ArrayList<>();
        this.ecoCoins = 0;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getHabitatId() {
        return habitatId;
    }

    public void setHabitatId(String habitatId) {
        this.habitatId = habitatId;
    }

    public int getEcoCoins() {
        return ecoCoins;
    }

    public void setEcoCoins(int ecoCoins) {
        this.ecoCoins = ecoCoins;
    }

    public List<String> getDevices() {
        return devices;
    }

    public void setDevices(List<String> devices) {
        this.devices = devices;
    }

    public void addDevice(String deviceId) {
        if (this.devices == null) {
            this.devices = new ArrayList<>();
        }
        this.devices.add(deviceId);
    }

    public void removeDevice(String deviceId) {
        if (this.devices != null) {
            this.devices.remove(deviceId);
        }
    }
} 