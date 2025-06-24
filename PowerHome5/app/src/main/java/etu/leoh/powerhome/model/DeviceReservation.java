package etu.leoh.powerhome.model;

import java.util.Date;

/**
 * Classe représentant une réservation d'utilisation d'un appareil électroménager
 */
public class DeviceReservation {
    private String id;
    private String deviceId;
    private String userId;
    private Date startTime;
    private Date endTime;
    private int ecoCoinsEarned;
    private boolean isCompleted;
    private ConsumptionLevel consumptionLevelAtReservation;

    // Enumération des niveaux de consommation
    public enum ConsumptionLevel {
        LOW,    // Vert (0-30%)
        MEDIUM, // Orange (30-70%)
        HIGH    // Rouge (70-100%)
    }

    public DeviceReservation() {
        // Constructeur vide requis pour Firestore
    }

    public DeviceReservation(String id, String deviceId, String userId, Date startTime, Date endTime) {
        this.id = id;
        this.deviceId = deviceId;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ecoCoinsEarned = 0;
        this.isCompleted = false;
        this.consumptionLevelAtReservation = ConsumptionLevel.LOW; // Valeur par défaut
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getEcoCoinsEarned() {
        return ecoCoinsEarned;
    }

    public void setEcoCoinsEarned(int ecoCoinsEarned) {
        this.ecoCoinsEarned = ecoCoinsEarned;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public ConsumptionLevel getConsumptionLevelAtReservation() {
        return consumptionLevelAtReservation;
    }

    public void setConsumptionLevelAtReservation(ConsumptionLevel consumptionLevelAtReservation) {
        this.consumptionLevelAtReservation = consumptionLevelAtReservation;
    }

    /**
     * Calcule les éco-coins gagnés ou perdus en fonction du niveau de consommation
     * au moment de l'utilisation
     * @param currentLevel Niveau de consommation actuel
     * @return Nombre d'éco-coins (positif pour bonus, négatif pour malus)
     */
    public int calculateEcoCoins(ConsumptionLevel currentLevel) {
        // Logique de calcul des éco-coins
        switch (currentLevel) {
            case LOW:
                return 10; // Bonus pour utilisation en période creuse
            case MEDIUM:
                return 0; // Neutre
            case HIGH:
                return -5; // Malus pour utilisation en période de pointe
            default:
                return 0;
        }
    }
} 