package etu.leoh.powerhome.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import etu.leoh.powerhome.MainActivity;
import etu.leoh.powerhome.PowerHomeApp;
import etu.leoh.powerhome.R;
import etu.leoh.powerhome.ui.auth.LoginActivity;
import etu.leoh.powerhome.util.DataInitializer;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

/**
 * Écran de démarrage (Splash Screen)
 */
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    // Durée du splash screen en millisecondes
    private static final int SPLASH_DURATION = 2000;
    private static final int SPLASH_MIN_DURATION = 1500;
    
    private FirebaseAuthHelper authHelper;
    private DataInitializer dataInitializer;
    private boolean isFirebaseAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Vérifier si Firebase est disponible
        isFirebaseAvailable = ((PowerHomeApp) getApplication()).isFirebaseAvailable();
        Log.d(TAG, "Firebase disponible: " + isFirebaseAvailable);
        
        authHelper = new FirebaseAuthHelper();
        dataInitializer = new DataInitializer();
        
        // Animation du logo
        ImageView logoImageView = findViewById(R.id.ivLogo);
        TextView appNameTextView = findViewById(R.id.tvAppName);
        
        // Charger les animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        
        // Appliquer les animations
        logoImageView.startAnimation(fadeIn);
        appNameTextView.startAnimation(slideUp);
        
        // Si Firebase n'est pas disponible, attendre juste pour l'animation puis rediriger
        if (!isFirebaseAvailable) {
            Log.w(TAG, "Firebase n'est pas disponible, redirection directe après animation");
            new Handler().postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }, SPLASH_MIN_DURATION);
            return;
        }
        
        // Initialiser les données avant de rediriger
        initializeDataAndRedirect();
    }
    
    /**
     * Initialise les données de test et redirige vers l'écran approprié
     */
    private void initializeDataAndRedirect() {
        try {
            // Vérifier si les données existent déjà
            dataInitializer.checkIfDataExists(exists -> {
                if (!exists) {
                    // Initialiser les données avec un timeout plus court
                    dataInitializer.initializeData(new DataInitializer.InitializationCallback() {
                        @Override
                        public void onInitializationComplete() {
                            Log.d(TAG, "Données initialisées avec succès");
                            Toast.makeText(SplashActivity.this, "Données initialisées avec succès", Toast.LENGTH_SHORT).show();
                            // Rediriger immédiatement
                            checkUserAndRedirect();
                        }
    
                        @Override
                        public void onInitializationFailed(Exception e) {
                            Log.e(TAG, "Erreur d'initialisation des données: " + e.getMessage(), e);
                            Toast.makeText(SplashActivity.this, "L'initialisation n'a pas pu être terminée complètement, certaines fonctionnalités peuvent être limitées", Toast.LENGTH_LONG).show();
                            // Rediriger malgré l'erreur
                            checkUserAndRedirect();
                        }
                    });
                } else {
                    Log.d(TAG, "Les données existent déjà");
                    // Les données existent déjà, attendre un peu avant de rediriger pour l'animation
                    new Handler().postDelayed(() -> checkUserAndRedirect(), SPLASH_MIN_DURATION);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur critique lors de l'initialisation: " + e.getMessage(), e);
            // En cas d'erreur grave, attendre un peu pour l'animation puis rediriger
            new Handler().postDelayed(() -> {
                // Rediriger vers l'écran de connexion
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }, SPLASH_MIN_DURATION);
        }
    }
    
    /**
     * Vérifie si un utilisateur est connecté et redirige vers l'écran approprié
     */
    private void checkUserAndRedirect() {
        try {
            // Vérifier si un utilisateur est déjà connecté
            if (authHelper.isUserLoggedIn()) {
                Log.d(TAG, "Utilisateur déjà connecté, redirection vers MainActivity");
                // Rediriger vers l'écran principal
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                Log.d(TAG, "Aucun utilisateur connecté, redirection vers LoginActivity");
                // Rediriger vers l'écran de connexion
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la vérification de l'utilisateur: " + e.getMessage(), e);
            // En cas d'erreur, rediriger vers l'écran de connexion
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        } finally {
            // Dans tous les cas, fermer l'activité de splash screen
            finish();
        }
    }
} 