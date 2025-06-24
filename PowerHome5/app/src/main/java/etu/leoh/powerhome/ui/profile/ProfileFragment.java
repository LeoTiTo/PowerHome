package etu.leoh.powerhome.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.repository.UserRepository;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

/**
 * Fragment de profil utilisateur
 */
public class ProfileFragment extends Fragment {

    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText emailEditText;
    private EditText habitatCodeEditText;
    private TextView ecoCoinsTextView;
    private Button saveButton;
    private ProgressBar progressBar;
    
    private FirebaseAuthHelper authHelper;
    private UserRepository userRepository;
    private User currentUser;
    
    // Pour stocker la référence au listener
    private UserRepository.UserChangeListener userChangeListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Initialiser les helpers et repositories
        authHelper = new FirebaseAuthHelper();
        userRepository = new UserRepository();
        
        // Initialiser les vues
        firstNameEditText = view.findViewById(R.id.etFirstName);
        lastNameEditText = view.findViewById(R.id.etLastName);
        emailEditText = view.findViewById(R.id.etEmail);
        habitatCodeEditText = view.findViewById(R.id.etHabitatCode);
        ecoCoinsTextView = view.findViewById(R.id.tvEcoCoins);
        saveButton = view.findViewById(R.id.btnSave);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Configurer le bouton d'enregistrement
        saveButton.setOnClickListener(v -> saveProfile());
        
        // Charger les données du profil
        loadProfileData();
        
        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Détacher le listener pour éviter les fuites de mémoire
        if (authHelper.getCurrentUserId() != null) {
            userRepository.detachUserListener(authHelper.getCurrentUserId());
        }
    }
    
    /**
     * Charge les données du profil utilisateur depuis Firestore
     */
    private void loadProfileData() {
        progressBar.setVisibility(View.VISIBLE);
        
        String userId = authHelper.getCurrentUserId();
        if (userId != null) {
            // Utiliser le listener en temps réel plutôt qu'une requête ponctuelle
            userChangeListener = (user, error) -> {
                progressBar.setVisibility(View.GONE);
                
                if (error != null) {
                    Toast.makeText(getContext(), 
                            "Erreur lors du chargement du profil: " + error.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (user != null) {
                    currentUser = user;
                    updateUI();
                }
            };
            
            userRepository.listenForUserChanges(userId, userChangeListener);
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Met à jour l'interface utilisateur avec les données du profil
     */
    private void updateUI() {
        if (currentUser != null) {
            firstNameEditText.setText(currentUser.getFirstName());
            lastNameEditText.setText(currentUser.getLastName());
            emailEditText.setText(currentUser.getEmail());
            
            // Si le habitat ID est au format "habitat_CODE", extraire le code
            String habitatId = currentUser.getHabitatId();
            if (habitatId != null && habitatId.startsWith("habitat_")) {
                habitatCodeEditText.setText(habitatId.substring("habitat_".length()));
            } else {
                habitatCodeEditText.setText("");
            }
            
            ecoCoinsTextView.setText(getString(R.string.my_eco_coins, currentUser.getEcoCoins()));
        }
    }
    
    /**
     * Enregistre les modifications du profil dans Firestore
     */
    private void saveProfile() {
        // Récupérer les valeurs des champs
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String habitatCode = habitatCodeEditText.getText().toString().trim();
        
        // Validation basique
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
        
        // Mettre à jour l'objet User
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        
        // Mettre à jour le habitat ID si le code a changé
        if (!habitatCode.isEmpty()) {
            currentUser.setHabitatId("habitat_" + habitatCode);
        }
        
        // Enregistrer les modifications
        userRepository.updateUser(currentUser)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), 
                                "Erreur lors de la mise à jour du profil: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
} 