package etu.leoh.powerhome.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.DeviceReservation;
import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.repository.DeviceReservationRepository;
import etu.leoh.powerhome.repository.UserRepository;
import etu.leoh.powerhome.ui.reservation.NewReservationActivity;
import etu.leoh.powerhome.util.ConsumptionCalculator;
import etu.leoh.powerhome.util.FirebaseAuthHelper;
import etu.leoh.powerhome.model.Habitat;
import etu.leoh.powerhome.repository.HabitatRepository;
import etu.leoh.powerhome.model.Device;
import etu.leoh.powerhome.repository.DeviceRepository;

/**
 * Fragment d'accueil affichant le niveau de consommation et les options principales
 */
public class HomeFragment extends Fragment {

    private ProgressBar consumptionProgressBar;
    private TextView consumptionLevelTextView;
    private TextView consumptionPercentageTextView;
    private TextView dateTextView;
    private Button bookDeviceButton;
    private CardView lowConsumptionCard, mediumConsumptionCard, highConsumptionCard;
    
    // Nouvelles propriétés pour les statistiques de réservations
    private ProgressBar reservationsProgressBar;
    private TextView completedReservationsCountTextView;
    private TextView totalEcoCoinsEarnedTextView;
    
    private ConsumptionCalculator consumptionCalculator;
    private DeviceReservationRepository reservationRepository;
    private UserRepository userRepository;
    private FirebaseAuthHelper authHelper;
    private User currentUser;
    private HabitatRepository habitatRepository;
    private DeviceRepository deviceRepository;
    
    // Variables pour la consommation totale de la résidence
    private int totalMaxConsumption = 0;
    private int totalActiveConsumption = 0;
    private static final String DEFAULT_RESIDENCE_ID = "residence1";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialiser les repositories et helpers
        consumptionCalculator = new ConsumptionCalculator();
        reservationRepository = new DeviceReservationRepository();
        userRepository = new UserRepository();
        authHelper = new FirebaseAuthHelper();
        habitatRepository = new HabitatRepository();
        deviceRepository = new DeviceRepository();
        
        // Initialiser les vues
        consumptionProgressBar = view.findViewById(R.id.consumptionProgressBar);
        consumptionLevelTextView = view.findViewById(R.id.tvConsumptionLevel);
        consumptionPercentageTextView = view.findViewById(R.id.tvConsumptionPercentage);
        dateTextView = view.findViewById(R.id.tvDate);
        bookDeviceButton = view.findViewById(R.id.btnBookDevice);
        
        lowConsumptionCard = view.findViewById(R.id.cardLowConsumption);
        mediumConsumptionCard = view.findViewById(R.id.cardMediumConsumption);
        highConsumptionCard = view.findViewById(R.id.cardHighConsumption);
        
        // Initialiser les vues pour les statistiques de réservations
        reservationsProgressBar = view.findViewById(R.id.reservationsProgressBar);
        completedReservationsCountTextView = view.findViewById(R.id.tvCompletedReservationsCount);
        totalEcoCoinsEarnedTextView = view.findViewById(R.id.tvTotalEcoCoinsEarned);
        
        // Afficher la date actuelle
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        dateTextView.setText(dateFormat.format(new Date()));
        
        // Configurer le bouton de réservation
        bookDeviceButton.setOnClickListener(v -> {
            // Rediriger vers l'écran de nouvelle réservation
            startActivity(new Intent(getActivity(), NewReservationActivity.class));
        });
        
        // Charger l'utilisateur actuel
        loadCurrentUser();
        
        return view;
    }

    /**
     * Charge les informations de l'utilisateur courant
     */
    private void loadCurrentUser() {
        if (authHelper.isUserLoggedIn()) {
            String userId = authHelper.getCurrentUserId();
            if (userId != null) {
                userRepository.getUserById(userId)
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                currentUser = documentSnapshot.toObject(User.class);
                                
                                // Charger les données de consommation
                                loadConsumptionData();
                                
                                // Charger les statistiques de réservations
                                loadReservationStats();
                            } else {
                                Toast.makeText(getContext(), "Utilisateur introuvable", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Erreur lors du chargement de l'utilisateur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Charge les données de consommation de toute la résidence
     */
    private void loadConsumptionData() {
        // Afficher un indicateur de chargement
        consumptionProgressBar.setIndeterminate(true);
        consumptionLevelTextView.setText(R.string.loading);
        consumptionPercentageTextView.setText("");
        
        // Réinitialiser les compteurs
        totalMaxConsumption = 0;
        totalActiveConsumption = 0;
        
        // Récupérer tous les habitats de la résidence
        habitatRepository.getHabitatsByResidence(DEFAULT_RESIDENCE_ID)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> habitatIds = new ArrayList<>();
                        
                        // Collecter les IDs des habitats
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Habitat habitat = document.toObject(Habitat.class);
                            habitatIds.add(habitat.getId());
                        }
                        
                        if (habitatIds.isEmpty()) {
                            // Aucun habitat trouvé
                            updateConsumptionUI(0, DeviceReservation.ConsumptionLevel.LOW);
                            return;
                        }
                        
                        // Compteur pour suivre le nombre d'habitats traités
                        final int[] processedHabitats = {0};
                        final int totalHabitats = habitatIds.size();
                        
                        // Parcourir chaque habitat et récupérer ses appareils
                        for (String habitatId : habitatIds) {
                            deviceRepository.getDevicesByHabitatId(habitatId)
                                    .addOnCompleteListener(deviceTask -> {
                                        if (deviceTask.isSuccessful() && deviceTask.getResult() != null) {
                                            int habitatMaxConsumption = 0;
                                            int habitatActiveConsumption = 0;
                                            
                                            for (QueryDocumentSnapshot deviceDoc : deviceTask.getResult()) {
                                                Device device = deviceDoc.toObject(Device.class);
                                                if (device != null) {
                                                    // Ajouter à la consommation maximale possible
                                                    habitatMaxConsumption += device.getPowerConsumption();
                                                    
                                                    // Si l'appareil est actif, ajouter à la consommation active
                                                    if (device.isActive()) {
                                                        habitatActiveConsumption += device.getPowerConsumption();
                                                    }
                                                }
                                            }
                                            
                                            // Ajouter au total
                                            totalMaxConsumption += habitatMaxConsumption;
                                            totalActiveConsumption += habitatActiveConsumption;
                                        }
                                        
                                        // Incrémenter le compteur d'habitats traités
                                        processedHabitats[0]++;
                                        
                                        // Si tous les habitats ont été traités, mettre à jour l'UI
                                        if (processedHabitats[0] == totalHabitats) {
                                            calculateAndUpdateConsumptionUI();
                                        }
                                    });
                        }
                    } else {
                        // En cas d'erreur, afficher un niveau par défaut
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getActivity(), 
                                        "Erreur lors du chargement des données de la résidence", 
                                        Toast.LENGTH_SHORT).show();
                                updateConsumptionUI(0, DeviceReservation.ConsumptionLevel.LOW);
                            });
                        }
                    }
                });
    }

    /**
     * Calcule le pourcentage de consommation et met à jour l'UI
     */
    private void calculateAndUpdateConsumptionUI() {
        if (getActivity() == null) return;
        
        // Calculer le pourcentage de consommation
        double consumptionPercentage = 0;
        if (totalMaxConsumption > 0) {
            consumptionPercentage = (double) totalActiveConsumption / totalMaxConsumption;
        }
        
        // Déterminer le niveau de consommation
        DeviceReservation.ConsumptionLevel level;
        if (consumptionPercentage <= 0.3) {
            level = DeviceReservation.ConsumptionLevel.LOW;
        } else if (consumptionPercentage <= 0.7) {
            level = DeviceReservation.ConsumptionLevel.MEDIUM;
        } else {
            level = DeviceReservation.ConsumptionLevel.HIGH;
        }
        
        // Mettre à jour l'UI
        final double finalPercentage = consumptionPercentage;
        final DeviceReservation.ConsumptionLevel finalLevel = level;
        
        getActivity().runOnUiThread(() -> {
            // Afficher les données de consommation totale
            updateConsumptionUI(finalPercentage, finalLevel);
            
            // Ajouter un message informatif
            String infoMessage = String.format("Consommation totale: %d W / %d W", 
                    totalActiveConsumption, totalMaxConsumption);
            Toast.makeText(getActivity(), infoMessage, Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * Charge les statistiques des réservations de l'utilisateur
     */
    private void loadReservationStats() {
        if (currentUser == null) return;
        
        // Afficher un indicateur de chargement
        reservationsProgressBar.setIndeterminate(true);
        completedReservationsCountTextView.setText("...");
        totalEcoCoinsEarnedTextView.setText("Total d'éco-coins gagnés: ...");
        
        // Récupérer toutes les réservations de l'utilisateur
        reservationRepository.getReservationsByUserId(currentUser.getId())
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (getActivity() != null) {
                        List<DeviceReservation> allReservations = new ArrayList<>();
                        List<DeviceReservation> completedReservations = new ArrayList<>();
                        // Utiliser un tableau d'un élément comme conteneur mutable
                        final int[] totalEcoCoinsEarned = {0};
                        
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            DeviceReservation reservation = document.toObject(DeviceReservation.class);
                            allReservations.add(reservation);
                            
                            if (reservation.isCompleted()) {
                                completedReservations.add(reservation);
                                totalEcoCoinsEarned[0] += reservation.getEcoCoinsEarned();
                            }
                        }
                        
                        getActivity().runOnUiThread(() -> {
                            // Mettre à jour l'UI avec les statistiques de réservations
                            updateReservationStatsUI(completedReservations.size(), allReservations.size(), totalEcoCoinsEarned[0]);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), "Erreur lors du chargement des réservations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            updateReservationStatsUI(0, 0, 0);
                        });
                    }
                });
    }

    /**
     * Met à jour l'interface utilisateur avec les données de consommation
     */
    private void updateConsumptionUI(double consumptionPercentage, DeviceReservation.ConsumptionLevel level) {
        // Convertir le pourcentage en valeur entière (0-100)
        int percentage = (int) (consumptionPercentage * 100);
        
        // Mettre à jour la barre de progression
        consumptionProgressBar.setIndeterminate(false);
        consumptionProgressBar.setProgress(percentage);
        
        // Mettre à jour le texte du pourcentage
        consumptionPercentageTextView.setText(percentage + "%");
        
        // Mettre à jour le texte du niveau
        String levelText;
        
        // Mettre en évidence la carte correspondant au niveau de consommation
        lowConsumptionCard.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        mediumConsumptionCard.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        highConsumptionCard.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        
        switch (level) {
            case LOW:
                levelText = getString(R.string.low_consumption);
                consumptionLevelTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                lowConsumptionCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                break;
            case MEDIUM:
                levelText = getString(R.string.medium_consumption);
                consumptionLevelTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                mediumConsumptionCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                break;
            case HIGH:
                levelText = getString(R.string.high_consumption);
                consumptionLevelTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                highConsumptionCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                break;
            default:
                levelText = getString(R.string.low_consumption);
                consumptionLevelTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                lowConsumptionCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                break;
        }
        
        consumptionLevelTextView.setText(levelText);
    }
    
    /**
     * Met à jour l'interface utilisateur avec les statistiques de réservations
     */
    private void updateReservationStatsUI(int completedCount, int totalCount, int totalEcoCoins) {
        // Mettre à jour le compteur de réservations
        completedReservationsCountTextView.setText(completedCount + "/" + totalCount);
        
        // Mettre à jour la barre de progression
        reservationsProgressBar.setIndeterminate(false);
        int progressPercentage = totalCount > 0 ? (completedCount * 100) / totalCount : 0;
        reservationsProgressBar.setProgress(progressPercentage);
        
        // Mettre à jour le texte des éco-coins gagnés
        String ecoCoinsText = "Total d'éco-coins gagnés: " + totalEcoCoins;
        totalEcoCoinsEarnedTextView.setText(ecoCoinsText);
        
        // Définir la couleur du texte en fonction du total des éco-coins
        if (totalEcoCoins > 0) {
            totalEcoCoinsEarnedTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (totalEcoCoins < 0) {
            totalEcoCoinsEarnedTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            totalEcoCoinsEarnedTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Recharger les données à chaque retour sur le fragment
        if (authHelper != null && authHelper.isUserLoggedIn()) {
            // Si l'utilisateur est déjà chargé, recharger uniquement les statistiques
            if (currentUser != null) {
                loadConsumptionData();
                loadReservationStats();
            } else {
                loadCurrentUser();
            }
        }
    }
} 