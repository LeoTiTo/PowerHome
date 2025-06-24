package etu.leoh.powerhome.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

/**
 * Écran de réinitialisation de mot de passe
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetButton;
    private TextView loginTextView;
    private ProgressBar progressBar;
    
    private FirebaseAuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        
        // Initialiser FirebaseAuthHelper
        authHelper = new FirebaseAuthHelper();
        
        // Initialiser les vues
        emailEditText = findViewById(R.id.etEmail);
        resetButton = findViewById(R.id.btnReset);
        loginTextView = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
        
        // Configurer les écouteurs
        resetButton.setOnClickListener(view -> attemptPasswordReset());
        
        loginTextView.setOnClickListener(view -> {
            // Retour à l'écran de connexion
            finish();
        });
    }
    
    /**
     * Tente de réinitialiser le mot de passe de l'utilisateur
     */
    private void attemptPasswordReset() {
        // Récupérer l'email
        String email = emailEditText.getText().toString().trim();
        
        // Validation basique
        if (email.isEmpty()) {
            emailEditText.setError("L'email est requis");
            emailEditText.requestFocus();
            return;
        }
        
        // Afficher la barre de progression
        progressBar.setVisibility(View.VISIBLE);
        
        // Tentative de réinitialisation du mot de passe
        authHelper.resetPassword(email, new FirebaseAuthHelper.OnResetPasswordListener() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this, 
                        "Un email de réinitialisation a été envoyé à " + email, 
                        Toast.LENGTH_LONG).show();
                
                // Rediriger vers l'écran de connexion après un court délai
                new android.os.Handler().postDelayed(() -> {
                    startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                    finish();
                }, 2000);
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
} 