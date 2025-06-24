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
public class ReservationRepository {
    private static final String COLLECTION_RESERVATIONS = "reservations";
    private final FirebaseFirestore firestore;

    public ReservationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Récupération de la référence de la collection des réservations
     * @return Référence de la collection
     */
    public CollectionReference getReservationsCollection() {
        return firestore.collection(COLLECTION_RESERVATIONS);
    }

    /**
     * Ajout d'une nouvelle réservation
     * @param reservation Réservation à ajouter
     * @return Tâche asynchrone
     */
    public Task<Void> addReservation(DeviceReservation reservation) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservation.getId())
                .set(reservation);
    }

    /**
     * Récupération d'une réservation par son ID
     * @param reservationId ID de la réservation
     * @return Tâche asynchrone
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
     * Récupération de toutes les réservations d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Tâche asynchrone
     */
    public Task<QuerySnapshot> getReservationsByUserId(String userId) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .get();
    }

    /**
     * Récupération de toutes les réservations d'un appareil
     * @param deviceId ID de l'appareil
     * @return Tâche asynchrone
     */
    public Task<QuerySnapshot> getReservationsByDeviceId(String deviceId) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("deviceId", deviceId)
                .get();
    }

    /**
     * Récupération des réservations d'un utilisateur sur une période donnée
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Tâche asynchrone
     */
    public Task<QuerySnapshot> getUserReservationsInPeriod(String userId, Date startDate, Date endDate) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("startTime", startDate)
                .whereLessThanOrEqualTo("endTime", endDate)
                .orderBy("endTime")
                .orderBy("startTime")
                .get();
    }

    /**
     * Récupération des réservations d'un appareil sur une période donnée
     * @param deviceId ID de l'appareil
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Tâche asynchrone
     */
    public Task<QuerySnapshot> getDeviceReservationsInPeriod(String deviceId, Date startDate, Date endDate) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("deviceId", deviceId)
                .whereGreaterThanOrEqualTo("startTime", startDate)
                .whereLessThanOrEqualTo("endTime", endDate)
                .orderBy("endTime")
                .orderBy("startTime")
                .get();
    }

    /**
     * Récupération de toutes les réservations dans une période donnée
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Tâche asynchrone
     */
    public Task<QuerySnapshot> getAllReservationsInPeriod(Date startDate, Date endDate) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .whereGreaterThanOrEqualTo("startTime", startDate)
                .whereLessThanOrEqualTo("endTime", endDate)
                .orderBy("endTime")
                .orderBy("startTime")
                .get();
    }

    /**
     * Mise à jour du statut de complétion d'une réservation
     * @param reservationId ID de la réservation
     * @param isCompleted Statut de complétion
     * @return Tâche asynchrone
     */
    public Task<Void> updateReservationCompletionStatus(String reservationId, boolean isCompleted) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .update("isCompleted", isCompleted);
    }

    /**
     * Mise à jour des éco-coins gagnés pour une réservation
     * @param reservationId ID de la réservation
     * @param ecoCoinsEarned Nombre d'éco-coins gagnés
     * @return Tâche asynchrone
     */
    public Task<Void> updateReservationEcoCoins(String reservationId, int ecoCoinsEarned) {
        return firestore.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .update("ecoCoinsEarned", ecoCoinsEarned);
    }
} 