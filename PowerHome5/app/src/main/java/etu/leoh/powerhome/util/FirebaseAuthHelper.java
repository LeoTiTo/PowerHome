package etu.leoh.powerhome.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.repository.UserRepository;

/**
 * Classe utilitaire pour gérer l'authentification Firebase
 */
public class FirebaseAuthHelper {

    private final UserRepository userRepository;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public FirebaseAuthHelper() {
        this.userRepository = new UserRepository();
    }

    /**
     * Récupère le Handler du thread principal pour exécuter du code sur l'UI thread
     * @return Handler du thread principal
     */
    public static Handler getMainHandler() {
        return mainHandler;
    }

    /**
     * Enregistrement d'un nouvel utilisateur
     * @param context Contexte de l'application
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @param firstName Prénom
     * @param lastName Nom
     * @param habitatId ID de l'habitat (peut être null)
     * @param accessCode Code d'accès pour vérifier l'accès à l'habitat
     * @param listener Écouteur pour gérer le résultat
     */
    public void registerUser(final Context context, final String email, String password, 
                           final String firstName, final String lastName, 
                           final String habitatId, final String accessCode,
                           final OnRegistrationListener listener) {
        
        // Si un code d'accès est fourni, vérifier d'abord le code
        if (accessCode != null && !accessCode.isEmpty()) {
            userRepository.verifyHabitatAccessCode(accessCode, habitatId)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // Utiliser l'ID de l'habitat trouvé, pas celui qui a été passé en paramètre
                                String realHabitatId = document.getId();
                                // Code correct, procéder à l'enregistrement avec l'ID réel de l'habitat
                                performRegistration(context, email, password, firstName, lastName, realHabitatId, listener);
                            } else {
                                // Code d'accès non trouvé
                                listener.onFailure("Code d'accès invalide. Veuillez vérifier votre code.");
                            }
                        } else {
                            // Erreur lors de la vérification
                            listener.onFailure("Erreur lors de la vérification du code d'accès: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Erreur inconnue"));
                        }
                    });
        } else {
            // Aucun code d'accès spécifié, procéder à l'enregistrement sans vérification
            performRegistration(context, email, password, firstName, lastName, null, listener);
        }
    }

    /**
     * Réalise l'enregistrement après vérification du code d'accès
     */
    private void performRegistration(final Context context, final String email, String password,
                                   final String firstName, final String lastName,
                                   final String habitatId, final OnRegistrationListener listener) {
        
        userRepository.registerUser(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = userRepository.getCurrentUser();
                        if (firebaseUser != null) {
                            // Création du profil utilisateur dans Firestore
                            User user = new User(firebaseUser.getUid(), email, firstName, lastName, habitatId);
                            
                            userRepository.saveUserInfo(user)
                                    .addOnCompleteListener(saveTask -> {
                                        if (saveTask.isSuccessful()) {
                                            listener.onSuccess(user);
                                        } else {
                                            // Suppression du compte si échec de sauvegarde du profil
                                            firebaseUser.delete();
                                            listener.onFailure("Échec de la création du profil: " + saveTask.getException().getMessage());
                                        }
                                    });
                        } else {
                            listener.onFailure("Erreur de création de l'utilisateur");
                        }
                    } else {
                        listener.onFailure("Échec de l'enregistrement: " + task.getException().getMessage());
                    }
                });
    }

    /**
     * Connexion d'un utilisateur existant
     * @param email Email
     * @param password Mot de passe
     * @param listener Écouteur pour gérer le résultat
     */
    public void loginUser(String email, String password, final OnLoginListener listener) {
        userRepository.loginUser(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = userRepository.getCurrentUser();
                        if (firebaseUser != null) {
                            // Récupération des données du profil depuis Firestore
                            userRepository.getUserById(firebaseUser.getUid())
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful() && profileTask.getResult() != null) {
                                            User user = profileTask.getResult().toObject(User.class);
                                            if (user != null) {
                                                listener.onSuccess(user);
                                            } else {
                                                listener.onFailure("Profil utilisateur introuvable");
                                            }
                                        } else {
                                            listener.onFailure("Échec de récupération du profil: " 
                                                    + (profileTask.getException() != null ? profileTask.getException().getMessage() : "Erreur inconnue"));
                                        }
                                    });
                        } else {
                            listener.onFailure("Erreur d'authentification");
                        }
                    } else {
                        listener.onFailure("Échec de connexion: " + task.getException().getMessage());
                    }
                });
    }

    /**
     * Déconnexion de l'utilisateur
     */
    public void logoutUser() {
        userRepository.logoutUser();
    }

    /**
     * Réinitialisation du mot de passe
     * @param email Email de l'utilisateur
     * @param listener Écouteur pour gérer le résultat
     */
    public void resetPassword(String email, final OnResetPasswordListener listener) {
        userRepository.resetPassword(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess();
                    } else {
                        listener.onFailure("Échec de réinitialisation du mot de passe: " + task.getException().getMessage());
                    }
                });
    }

    /**
     * Vérification si un utilisateur est connecté
     * @return true si un utilisateur est connecté
     */
    public boolean isUserLoggedIn() {
        return userRepository.getCurrentUser() != null;
    }

    /**
     * Récupération de l'ID de l'utilisateur actuellement connecté
     * @return ID de l'utilisateur ou null si non connecté
     */
    public String getCurrentUserId() {
        FirebaseUser user = userRepository.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // Interfaces pour les écouteurs
    public interface OnRegistrationListener {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface OnLoginListener {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface OnResetPasswordListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }
} 