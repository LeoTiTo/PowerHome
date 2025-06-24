package etu.leoh.powerhome.repository;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import etu.leoh.powerhome.model.Habitat;

/**
 * Repository pour gérer les interactions avec la collection des habitats dans Firestore
 */
public class HabitatRepository {

    private static final String TAG = "HabitatRepository";
    private static final String COLLECTION_HABITATS = "habitats";
    private final FirebaseFirestore db;

    public HabitatRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Récupère un habitat par son ID
     * @param habitatId ID de l'habitat
     * @return Task contenant le document de l'habitat
     */
    public Task<DocumentSnapshot> getHabitatById(String habitatId) {
        Log.d(TAG, "Recherche de l'habitat avec ID: " + habitatId);
        return db.collection(COLLECTION_HABITATS).document(habitatId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Habitat trouvé: " + documentSnapshot.getId());
                    } else {
                        Log.d(TAG, "Aucun habitat trouvé avec l'ID: " + habitatId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erreur lors de la recherche de l'habitat: " + e.getMessage()));
    }

    /**
     * Récupère un habitat par son code d'accès
     * @param code Code d'accès de l'habitat
     * @return Task contenant les documents correspondants
     */
    public Task<QuerySnapshot> getHabitatByCode(String code) {
        Log.d(TAG, "Recherche de l'habitat avec code d'accès: " + code);
        return db.collection(COLLECTION_HABITATS)
                .whereEqualTo("accessCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Habitat trouvé avec le code: " + code);
                    } else {
                        Log.d(TAG, "Aucun habitat trouvé avec le code: " + code);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erreur lors de la recherche de l'habitat par code: " + e.getMessage()));
    }

    /**
     * Récupère tous les habitats de la résidence
     * @param residenceId ID de la résidence
     * @return Task contenant les documents des habitats
     */
    public Task<QuerySnapshot> getHabitatsByResidence(String residenceId) {
        return db.collection(COLLECTION_HABITATS)
                .whereEqualTo("residenceId", residenceId)
                .get();
    }

    /**
     * Récupère la référence à la collection des habitats
     * @return Référence à la collection
     */
    public CollectionReference getHabitatsCollection() {
        return db.collection(COLLECTION_HABITATS);
    }

    /**
     * Ajoute un résident à un habitat
     * @param habitatId ID de l'habitat
     * @param userId ID de l'utilisateur à ajouter
     * @return Task de la mise à jour
     */
    public Task<Void> addResidentToHabitat(String habitatId, String userId) {
        DocumentReference habitatRef = db.collection(COLLECTION_HABITATS).document(habitatId);
        return habitatRef.update("residentIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId));
    }

    /**
     * Retire un résident d'un habitat
     * @param habitatId ID de l'habitat
     * @param userId ID de l'utilisateur à retirer
     * @return Task de la mise à jour
     */
    public Task<Void> removeResidentFromHabitat(String habitatId, String userId) {
        DocumentReference habitatRef = db.collection(COLLECTION_HABITATS).document(habitatId);
        return habitatRef.update("residentIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId));
    }

    /**
     * Ajoute un appareil à un habitat
     * @param habitatId ID de l'habitat
     * @param deviceId ID de l'appareil à ajouter
     * @return Task de la mise à jour
     */
    public Task<Void> addDeviceToHabitat(String habitatId, String deviceId) {
        DocumentReference habitatRef = db.collection(COLLECTION_HABITATS).document(habitatId);
        return habitatRef.update("deviceIds", com.google.firebase.firestore.FieldValue.arrayUnion(deviceId));
    }

    /**
     * Retire un appareil d'un habitat
     * @param habitatId ID de l'habitat
     * @param deviceId ID de l'appareil à retirer
     * @return Task de la mise à jour
     */
    public Task<Void> removeDeviceFromHabitat(String habitatId, String deviceId) {
        DocumentReference habitatRef = db.collection(COLLECTION_HABITATS).document(habitatId);
        return habitatRef.update("deviceIds", com.google.firebase.firestore.FieldValue.arrayRemove(deviceId));
    }

    /**
     * Crée un nouvel habitat
     * @param habitat Objet Habitat à créer
     * @return Task de création
     */
    public Task<Void> createHabitat(Habitat habitat) {
        DocumentReference newHabitatRef = db.collection(COLLECTION_HABITATS).document(habitat.getId());
        return newHabitatRef.set(habitat);
    }

    /**
     * Met à jour un habitat
     * @param habitat Objet Habitat à mettre à jour
     * @return Task de mise à jour
     */
    public Task<Void> updateHabitat(Habitat habitat) {
        DocumentReference habitatRef = db.collection(COLLECTION_HABITATS).document(habitat.getId());
        return habitatRef.set(habitat);
    }
} 