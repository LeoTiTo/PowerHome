package etu.leoh.powerhome.model;

/**
 * Classe représentant un appareil électroménager
 */
public class Device {
    private String id;
    private String name;
    private String type;
    private int powerConsumption; // en watts
    private String habitatId;
    private boolean isActive;

    public Device() {
        // Constructeur vide requis pour Firestore
    }

    public Device(String id, String name, String type, int powerConsumption, String habitatId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.powerConsumption = powerConsumption;
        this.habitatId = habitatId;
        this.isActive = false;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPowerConsumption() {
        return powerConsumption;
    }

    public void setPowerConsumption(int powerConsumption) {
        this.powerConsumption = powerConsumption;
    }

    public String getHabitatId() {
        return habitatId;
    }

    public void setHabitatId(String habitatId) {
        this.habitatId = habitatId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
} 