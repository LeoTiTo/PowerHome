package etu.leoh.powerhome;

import android.app.Application;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Classe d'application principale
 */
public class PowerHomeApp extends Application {
    private static final String TAG = "PowerHomeApp";
    private boolean firebaseInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Démarrage de l'application PowerHome");
        
        // Initialiser Firebase avec gestion d'erreur
        try {
            if (!FirebaseApp.getApps(this).isEmpty()) {
                // Firebase déjà initialisé
                Log.d(TAG, "Firebase déjà initialisé");
                firebaseInitialized = true;
            } else {
                // Initialiser Firebase
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialisé avec succès");
                firebaseInitialized = true;
            }
            
            // Configurer Firestore si Firebase est disponible
            if (firebaseInitialized) {
                configureFirestore();
            }
        } catch (Exception e) {
            // En cas d'erreur, afficher un message et continuer sans Firebase
            Log.e(TAG, "Erreur lors de l'initialisation de Firebase: " + e.getMessage(), e);
            Toast.makeText(this, 
                    "Mode hors ligne: certaines fonctionnalités peuvent être limitées", 
                    Toast.LENGTH_LONG).show();
            firebaseInitialized = false;
        }
    }
    
    /**
     * Configure Firestore avec des paramètres optimisés
     */
    private void configureFirestore() {
        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            firestore.setFirestoreSettings(settings);
            
            // Stratégie de connectivité pour améliorer la stabilité
            firestore.disableNetwork()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Réseau Firestore temporairement désactivé pendant le démarrage");
                        
                        // Réactiver le réseau après un court délai
                        new Handler().postDelayed(() -> {
                            firestore.enableNetwork()
                                    .addOnSuccessListener(aVoid2 -> 
                                        Log.d(TAG, "Réseau Firestore réactivé après initialisation"));
                        }, 1000);
                    })
                    .addOnFailureListener(e -> 
                        Log.w(TAG, "Impossible de désactiver le réseau Firestore: " + e.getMessage()));
            
            Log.d(TAG, "Firestore configuré avec succès");
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la configuration de Firestore: " + e.getMessage(), e);
        }
    }
    
    /**
     * Vérifie si Firebase a été initialisé avec succès
     * @return true si Firebase est disponible
     */
    public boolean isFirebaseAvailable() {
        return firebaseInitialized;
    }
} 