package etu.leoh.powerhome.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import etu.leoh.powerhome.model.Device;

/**
 * Repository pour gérer les opérations sur les appareils électroménagers
 */
public class DeviceRepository {
    private static final String COLLECTION_DEVICES = "devices";
    private final FirebaseFirestore firestore;

    public DeviceRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Récupération de la référence de la collection des appareils
     * @return Référence de la collection
     */
    public CollectionReference getDevicesCollection() {
        return firestore.collection(COLLECTION_DEVICES);
    }

    /**
     * Ajout d'un nouvel appareil
     * @param device Appareil à ajouter
     * @return Tâche asynchrone
     */
    public Task<Void> addDevice(Device device) {
        return firestore.collection(COLLECTION_DEVICES)
                .document(device.getId())
                .set(device);
    }

    /**
     * Récupération d'un appareil par son ID
     * @param deviceId ID de l'appareil
     * @return Tâche asynchrone
     */
    public Task<DocumentSnapshot> getDeviceById(String deviceId) {
        return firestore.collection(COLLECTION_DEVICES)
                .document(deviceId)
                .get();
    }

    /**
     * Mise à jour d'un appareil
     * @param device Appareil avec les nouvelles informations
     * @return Tâche asynchrone
     */
    public Task<Void> updateDevice(Device device) {
        return firestore.collection(COLLECTION_DEVICES)
                .document(device.getId())
                .update(
                        "name", device.getName(),
                        "type", device.getType(),
                        "powerConsumption", device.getPowerConsumption(),
                        "habitatId", device.getHabitatId(),
                        "isActive", device.isActive(),
                        "active", device.isActive()
                );
    }

    /**
     * Suppression d'un appareil
     * @param deviceId ID de l'appareil
     * @return Tâche asynchrone
     */
    public Task<Void> deleteDevice(String deviceId) {
        return firestore.collection(COLLECTION_DEVICES)
                .document(deviceId)
                .delete();
    }

    /**
     * Récupération de tous les appareils d'un habitat
     * @param habitatId ID de l'habitat
     * @return Tâche asynchrone
     */
    public Task<QuerySnapshot> getDevicesByHabitatId(String habitatId) {
        return firestore.collection(COLLECTION_DEVICES)
                .whereEqualTo("habitatId", habitatId)
                .get();
    }

    /**
     * Mise à jour du statut d'activité d'un appareil
     * @param deviceId ID de l'appareil
     * @param isActive Statut d'activité
     * @return Tâche asynchrone
     */
    public Task<Void> updateDeviceActivity(String deviceId, boolean isActive) {
        return firestore.collection(COLLECTION_DEVICES)
                .document(deviceId)
                .update("isActive", isActive);
    }

    /**
     * Récupération de tous les appareils actifs dans un habitat
     * @param habitatId ID de l'habitat
     * @return Tâche asynchrone
     */
    public Task<QuerySnapshot> getActiveDevicesByHabitatId(String habitatId) {
        return firestore.collection(COLLECTION_DEVICES)
                .whereEqualTo("habitatId", habitatId)
                .whereEqualTo("isActive", true)
                .get();
    }

    /**
     * Récupération de tous les appareils par type
     * @param type Type d'appareil
     * @return Tâche asynchrone
     */
    public Task<QuerySnapshot> getDevicesByType(String type) {
        return firestore.collection(COLLECTION_DEVICES)
                .whereEqualTo("type", type)
                .get();
    }
} 