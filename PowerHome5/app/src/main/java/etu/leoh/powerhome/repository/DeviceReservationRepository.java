package etu.leoh.powerhome.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;

import etu.leoh.powerhome.model.DeviceReservation;

/**
 * Repository pour gérer les opérations sur les réservations d'appareils
 */
public class DeviceReservationRepository {
    private static final String COLLECTION_RESERVATIONS = "device_reservations";
    private final FirebaseFirestore firestore;

    public DeviceReservationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Récupération de la référence à la collection des réservations
     * @return Référence à la collection
     */
    public CollectionReference getReservationsCollection() {
        return firestore.collection(COLLECTION_RESERVATIONS);
    }

    /**
     * Création d'une nouvelle réservation
     * @param reservation Réservation à créer
     * @return Tâche asynchrone
     */
    public Task<Void> createReservation(DeviceReservation reservation) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservation.getId())
                .set(reservation);
    }

    /**
     * Récupération d'une réservation par son ID
     * @param reservationId ID de la réservation
     * @return Tâche asynchrone contenant la réservation
     */
    public Task<DocumentSnapshot> getReservationById(String reservationId) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .get();
    }

    /**
     * Mise à jour d'une réservation
     * @param reservation Réservation avec les nouvelles informations
     * @return Tâche asynchrone
     */
    public Task<Void> updateReservation(DeviceReservation reservation) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservation.getId())
                .set(reservation);
    }

    /**
     * Suppression d'une réservation
     * @param reservationId ID de la réservation
     * @return Tâche asynchrone
     */
    public Task<Void> deleteReservation(String reservationId) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .delete();
    }

    /**
     * Récupération des réservations d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Tâche asynchrone contenant les réservations
     */
    public Task<QuerySnapshot> getReservationsByUserId(String userId) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Récupération des réservations pour un appareil
     * @param deviceId ID de l'appareil
     * @return Tâche asynchrone contenant les réservations
     */
    public Task<QuerySnapshot> getReservationsByDeviceId(String deviceId) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("deviceId", deviceId)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get();
    }

    /**
     * Récupération des réservations à venir pour un appareil
     * @param deviceId ID de l'appareil
     * @param now Date actuelle
     * @return Tâche asynchrone contenant les réservations
     */
    public Task<QuerySnapshot> getUpcomingReservationsByDeviceId(String deviceId, Date now) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("deviceId", deviceId)
                .whereGreaterThan("startTime", now)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get();
    }

    /**
     * Marque une réservation comme terminée
     * @param reservationId ID de la réservation
     * @return Tâche asynchrone
     */
    public Task<Void> markReservationAsCompleted(String reservationId) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .update("isCompleted", true);
    }

    /**
     * Met à jour les éco-coins gagnés pour une réservation
     * @param reservationId ID de la réservation
     * @param ecoCoinsEarned Nombre d'éco-coins gagnés
     * @return Tâche asynchrone
     */
    public Task<Void> updateEcoCoinsEarned(String reservationId, int ecoCoinsEarned) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .update("ecoCoinsEarned", ecoCoinsEarned);
    }
} 