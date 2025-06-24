package etu.leoh.powerhome.util;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import etu.leoh.powerhome.model.Device;
import etu.leoh.powerhome.model.DeviceReservation;
import etu.leoh.powerhome.repository.DeviceRepository;
import etu.leoh.powerhome.repository.ReservationRepository;

/**
 * Classe utilitaire pour calculer le niveau de consommation électrique
 */
public class ConsumptionCalculator {

    private final DeviceRepository deviceRepository;
    private final ReservationRepository reservationRepository;

    // Constantes pour les seuils de consommation
    private static final double LOW_THRESHOLD = 0.3; // 30%
    private static final double MEDIUM_THRESHOLD = 0.7; // 70%

    // Capacité maximale théorique en watts (à définir selon les besoins du projet)
    private static final int MAX_CAPACITY = 5000; // 5 kW

    public ConsumptionCalculator() {
        this.deviceRepository = new DeviceRepository();
        this.reservationRepository = new ReservationRepository();
    }

    /**
     * Calcule le niveau de consommation à un moment donné
     * @param date Date pour laquelle calculer la consommation
     * @param callback Callback pour retourner le résultat
     */
    public void calculateConsumptionLevel(Date date, ConsumptionLevelCallback callback) {
        // Définir les bornes de l'heure (par exemple, pour 14h30, on prend de 14h00 à 14h59)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startHour = calendar.getTime();

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date endHour = calendar.getTime();

        // Récupérer toutes les réservations actives pendant cette heure
        reservationRepository.getAllReservationsInPeriod(startHour, endHour)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot reservationsSnapshot = task.getResult();
                        
                        // Liste des IDs d'appareils à récupérer
                        List<String> deviceIds = new ArrayList<>();
                        Map<String, DeviceReservation> reservationMap = new HashMap<>();
                        
                        for (QueryDocumentSnapshot document : reservationsSnapshot) {
                            DeviceReservation reservation = document.toObject(DeviceReservation.class);
                            deviceIds.add(reservation.getDeviceId());
                            reservationMap.put(reservation.getDeviceId(), reservation);
                        }
                        
                        if (deviceIds.isEmpty()) {
                            // Aucune réservation, consommation nulle
                            callback.onConsumptionLevelCalculated(0, DeviceReservation.ConsumptionLevel.LOW);
                            return;
                        }
                        
                        // Récupérer les informations sur les appareils
                        retrieveDevicesAndCalculateConsumption(deviceIds, reservationMap, callback);
                    } else {
                        callback.onError("Erreur lors de la récupération des réservations");
                    }
                });
    }

    /**
     * Récupère les appareils et calcule la consommation totale
     */
    private void retrieveDevicesAndCalculateConsumption(List<String> deviceIds, 
                                                       Map<String, DeviceReservation> reservationMap, 
                                                       ConsumptionLevelCallback callback) {
        // Compteur pour suivre le nombre de requêtes terminées
        final int[] completedRequests = {0};
        final int totalRequests = deviceIds.size();
        final int[] totalConsumption = {0};
        
        for (String deviceId : deviceIds) {
            deviceRepository.getDeviceById(deviceId)
                    .addOnCompleteListener(deviceTask -> {
                        completedRequests[0]++;
                        
                        if (deviceTask.isSuccessful() && deviceTask.getResult() != null) {
                            DocumentSnapshot deviceDoc = deviceTask.getResult();
                            if (deviceDoc.exists()) {
                                Device device = deviceDoc.toObject(Device.class);
                                if (device != null) {
                                    totalConsumption[0] += device.getPowerConsumption();
                                }
                            }
                        }
                        
                        // Toutes les requêtes sont terminées, calculer le niveau de consommation
                        if (completedRequests[0] == totalRequests) {
                            double consumptionPercentage = (double) totalConsumption[0] / MAX_CAPACITY;
                            DeviceReservation.ConsumptionLevel level;
                            
                            if (consumptionPercentage <= LOW_THRESHOLD) {
                                level = DeviceReservation.ConsumptionLevel.LOW;
                            } else if (consumptionPercentage <= MEDIUM_THRESHOLD) {
                                level = DeviceReservation.ConsumptionLevel.MEDIUM;
                            } else {
                                level = DeviceReservation.ConsumptionLevel.HIGH;
                            }
                            
                            callback.onConsumptionLevelCalculated(consumptionPercentage, level);
                        }
                    });
        }
    }

    /**
     * Calcule la consommation totale pour un créneau horaire (par heure)
     * @param date Date pour laquelle calculer la consommation
     * @param callback Callback pour retourner le résultat
     */
    public void calculateHourlyConsumption(Date date, ConsumptionCallback callback) {
        // Définir les bornes de l'heure
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startHour = calendar.getTime();

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date endHour = calendar.getTime();

        // Récupérer toutes les réservations actives pendant cette heure
        reservationRepository.getAllReservationsInPeriod(startHour, endHour)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot reservationsSnapshot = task.getResult();
                        
                        // Liste des IDs d'appareils à récupérer
                        List<String> deviceIds = new ArrayList<>();
                        
                        for (QueryDocumentSnapshot document : reservationsSnapshot) {
                            DeviceReservation reservation = document.toObject(DeviceReservation.class);
                            deviceIds.add(reservation.getDeviceId());
                        }
                        
                        if (deviceIds.isEmpty()) {
                            // Aucune réservation, consommation nulle
                            callback.onConsumptionCalculated(0);
                            return;
                        }
                        
                        // Récupérer les informations sur les appareils et calculer la consommation
                        calculateTotalConsumption(deviceIds, callback);
                    } else {
                        callback.onError("Erreur lors de la récupération des réservations");
                    }
                });
    }

    /**
     * Calcule la consommation totale pour une liste d'appareils
     */
    private void calculateTotalConsumption(List<String> deviceIds, ConsumptionCallback callback) {
        final int[] completedRequests = {0};
        final int totalRequests = deviceIds.size();
        final int[] totalConsumption = {0};
        
        for (String deviceId : deviceIds) {
            deviceRepository.getDeviceById(deviceId)
                    .addOnCompleteListener(deviceTask -> {
                        completedRequests[0]++;
                        
                        if (deviceTask.isSuccessful() && deviceTask.getResult() != null) {
                            DocumentSnapshot deviceDoc = deviceTask.getResult();
                            if (deviceDoc.exists()) {
                                Device device = deviceDoc.toObject(Device.class);
                                if (device != null) {
                                    totalConsumption[0] += device.getPowerConsumption();
                                }
                            }
                        }
                        
                        // Toutes les requêtes sont terminées
                        if (completedRequests[0] == totalRequests) {
                            callback.onConsumptionCalculated(totalConsumption[0]);
                        }
                    });
        }
    }

    /**
     * Calcule la consommation d'un habitat spécifique à un moment donné
     * @param habitatId ID de l'habitat
     * @param date Date pour laquelle calculer la consommation
     * @param callback Callback pour retourner le résultat
     */
    public void calculateHabitatConsumption(String habitatId, Date date, ConsumptionCallback callback) {
        deviceRepository.getDevicesByHabitatId(habitatId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int totalConsumption = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Device device = document.toObject(Device.class);
                            if (device.isActive()) {
                                totalConsumption += device.getPowerConsumption();
                            }
                        }
                        callback.onConsumptionCalculated(totalConsumption);
                    } else {
                        callback.onError("Erreur lors de la récupération des appareils");
                    }
                });
    }

    // Interfaces pour les callbacks
    public interface ConsumptionLevelCallback {
        void onConsumptionLevelCalculated(double consumptionPercentage, DeviceReservation.ConsumptionLevel level);
        void onError(String errorMessage);
    }

    public interface ConsumptionCallback {
        void onConsumptionCalculated(int consumptionWatts);
        void onError(String errorMessage);
    }
} 