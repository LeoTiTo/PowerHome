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
 * Écran d'inscription
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText habitatCodeEditText;
    private Button registerButton;
    private TextView loginTextView;
    private ProgressBar progressBar;
    
    private FirebaseAuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialiser FirebaseAuthHelper
        authHelper = new FirebaseAuthHelper();
        
        // Initialiser les vues
        emailEditText = findViewById(R.id.etEmail);
        passwordEditText = findViewById(R.id.etPassword);
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword);
        firstNameEditText = findViewById(R.id.etFirstName);
        lastNameEditText = findViewById(R.id.etLastName);
        habitatCodeEditText = findViewById(R.id.etHabitatCode);
        registerButton = findViewById(R.id.btnRegister);
        loginTextView = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
        
        // Configurer les écouteurs
        registerButton.setOnClickListener(view -> attemptRegistration());
        
        loginTextView.setOnClickListener(view -> {
            // Rediriger vers l'écran de connexion
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }
    
    /**
     * Tente d'inscrire l'utilisateur avec les informations saisies
     */
    private void attemptRegistration() {
        // Récupérer les valeurs des champs
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String habitatCode = habitatCodeEditText.getText().toString().trim();
        
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
        
        if (password.length() < 6) {
            passwordEditText.setError("Le mot de passe doit contenir au moins 6 caractères");
            passwordEditText.requestFocus();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Les mots de passe ne correspondent pas");
            confirmPasswordEditText.requestFocus();
            return;
        }
        
        if (firstName.isEmpty()) {
            firstNameEditText.setError("Le prénom est requis");
            firstNameEditText.requestFocus();
            return;
        }
        
        if (lastName.isEmpty()) {
            lastNameEditText.setError("Le nom est requis");
            lastNameEditText.requestFocus();
            return;
        }
        
        // Afficher la barre de progression
        progressBar.setVisibility(View.VISIBLE);
        
        // Pas besoin de calculer un habitatId, la vérification se fait directement par code d'accès
        String habitatId = null; // L'ID réel sera récupéré par FirebaseAuthHelper
        
        // Tentative d'inscription
        authHelper.registerUser(this, email, password, firstName, lastName, habitatId, habitatCode, 
                new FirebaseAuthHelper.OnRegistrationListener() {
            @Override
            public void onSuccess(User user) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, "Inscription réussie", Toast.LENGTH_SHORT).show();
                
                // Rediriger vers l'écran principal
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
} 