package etu.leoh.powerhome.repository;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import etu.leoh.powerhome.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository pour gérer les opérations sur les utilisateurs
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USERS = "users";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;

    // Map pour stocker les listeners actifs
    private final Map<String, ListenerRegistration> userListeners = new HashMap<>();

    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    /**
     * Création d'un compte utilisateur
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @return Tâche asynchrone
     */
    public Task<AuthResult> registerUser(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    /**
     * Connexion d'un utilisateur
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @return Tâche asynchrone
     */
    public Task<AuthResult> loginUser(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    /**
     * Déconnexion de l'utilisateur
     */
    public void logoutUser() {
        firebaseAuth.signOut();
    }

    /**
     * Récupération de l'utilisateur actuellement connecté
     * @return Utilisateur Firebase ou null
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Réinitialisation du mot de passe
     * @param email Email de l'utilisateur
     * @return Tâche asynchrone
     */
    public Task<Void> resetPassword(String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    /**
     * Sauvegarde des informations de l'utilisateur dans Firestore
     * @param user Utilisateur à sauvegarder
     * @return Tâche asynchrone
     */
    public Task<Void> saveUserInfo(User user) {
        return firestore.collection(COLLECTION_USERS)
                .document(user.getId())
                .set(user);
    }

    /**
     * Récupération des informations d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Tâche asynchrone
     */
    public Task<DocumentSnapshot> getUserById(String userId) {
        return firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get();
    }

    /**
     * Mise à jour des informations d'un utilisateur
     * @param user Utilisateur avec les nouvelles informations
     * @return Tâche asynchrone
     */
    public Task<Void> updateUser(User user) {
        return firestore.collection(COLLECTION_USERS)
                .document(user.getId())
                .update(
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName(),
                        "email", user.getEmail(),
                        "habitatId", user.getHabitatId(),
                        "ecoCoins", user.getEcoCoins(),
                        "devices", user.getDevices()
                );
    }

    /**
     * Suppression d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Tâche asynchrone
     */
    public Task<Void> deleteUser(String userId) {
        return firestore.collection(COLLECTION_USERS)
                .document(userId)
                .delete();
    }

    /**
     * Vérifie un code d'accès à un habitat
     * @param accessCode Code d'accès à vérifier
     * @param habitatId ID de l'habitat (non utilisé dans cette implémentation)
     * @return Tâche asynchrone contenant un document snapshot
     */
    public Task<DocumentSnapshot> verifyHabitatAccessCode(String accessCode, String habitatId) {
        Log.d(TAG, "Vérification du code d'accès: " + accessCode);
        
        // Lister tous les habitats pour le débogage
        listAllHabitats();
        
        // Créer une tâche pour rechercher l'habitat par code d'accès
        return firestore.collection("habitats")
                .whereEqualTo("accessCode", accessCode)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Code d'accès trouvé, renvoyer le premier document
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        Log.d(TAG, "Habitat trouvé avec le code: " + accessCode + ", ID: " + document.getId());
                        return document;
                    } else {
                        // Code d'accès non trouvé ou erreur
                        Log.d(TAG, "Aucun habitat trouvé avec le code: " + accessCode);
                        return null;
                    }
                });
    }

    /**
     * Liste tous les habitats disponibles (pour le débogage)
     */
    private void listAllHabitats() {
        firestore.collection("habitats").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "=== Liste de tous les habitats disponibles ===");
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "Aucun habitat trouvé dans la base de données!");
                    return;
                }
                
                queryDocumentSnapshots.forEach(document -> {
                    Log.d(TAG, "Habitat ID: " + document.getId() + 
                                ", Nom: " + document.getString("name") + 
                                ", Code d'accès: " + document.getString("accessCode"));
                });
                Log.d(TAG, "=== Fin de la liste des habitats ===");
            })
            .addOnFailureListener(e -> Log.e(TAG, "Erreur lors de la récupération des habitats: " + e.getMessage()));
    }

    /**
     * Mise à jour du solde d'éco-coins d'un utilisateur
     * @param userId ID de l'utilisateur
     * @param ecoCoins Nouveau solde d'éco-coins
     * @return Tâche asynchrone
     */
    public Task<Void> updateEcoCoins(String userId, int ecoCoins) {
        return firestore.collection(COLLECTION_USERS)
                .document(userId)
                .update("ecoCoins", ecoCoins);
    }

    /**
     * Interface de callback pour les changements utilisateur
     */
    public interface UserChangeListener {
        void onUserChanged(User user, Exception error);
    }
    
    /**
     * Écoute les changements en temps réel sur un document utilisateur
     * @param userId ID de l'utilisateur à écouter
     * @param listener Callback à appeler lors de changements
     */
    public void listenForUserChanges(String userId, UserChangeListener listener) {
        // Détacher l'ancien listener s'il existe
        detachUserListener(userId);
        
        // Ajouter un nouveau listener
        ListenerRegistration registration = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        // Une erreur s'est produite
                        listener.onUserChanged(null, e);
                        return;
                    }
                    
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Document trouvé, convertir en objet User
                        User user = documentSnapshot.toObject(User.class);
                        listener.onUserChanged(user, null);
                    } else {
                        // Document non trouvé
                        listener.onUserChanged(null, new Exception("Utilisateur non trouvé"));
                    }
                });
        
        // Stocker la référence du listener
        userListeners.put(userId, registration);
    }
    
    /**
     * Détache l'écouteur pour un utilisateur spécifique
     * @param userId ID de l'utilisateur
     */
    public void detachUserListener(String userId) {
        ListenerRegistration registration = userListeners.get(userId);
        if (registration != null) {
            registration.remove();
            userListeners.remove(userId);
        }
    }
} 