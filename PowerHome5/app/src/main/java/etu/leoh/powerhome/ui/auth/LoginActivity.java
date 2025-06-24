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

import etu.leoh.powerhome.MainActivity;
import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

/**
 * Écran de connexion
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView forgotPasswordTextView;
    private TextView registerTextView;
    private ProgressBar progressBar;
    
    private FirebaseAuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialiser FirebaseAuthHelper
        authHelper = new FirebaseAuthHelper();
        
        // Initialiser les vues
        emailEditText = findViewById(R.id.etEmail);
        passwordEditText = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        forgotPasswordTextView = findViewById(R.id.tvForgotPassword);
        registerTextView = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);
        
        // Configurer les écouteurs
        loginButton.setOnClickListener(view -> attemptLogin());
        
        forgotPasswordTextView.setOnClickListener(view -> {
            // Rediriger vers l'écran de réinitialisation de mot de passe
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
        
        registerTextView.setOnClickListener(view -> {
            // Rediriger vers l'écran d'inscription
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
    
    /**
     * Tente de connecter l'utilisateur avec les informations saisies
     */
    private void attemptLogin() {
        // Récupérer les valeurs des champs
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // Validation basique
        if (email.isEmpty()) {
            emailEditText.setError("L'email est requis");
            emailEditText.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            passwordEditText.setError("Le mot de passe est requis");
            passwordEditText.requestFocus();
            return;
        }
        
        // Afficher la barre de progression
        progressBar.setVisibility(View.VISIBLE);
        
        // Tentative de connexion
        authHelper.loginUser(email, password, new FirebaseAuthHelper.OnLoginListener() {
            @Override
            public void onSuccess(User user) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                
                // Rediriger vers l'écran principal
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
} 