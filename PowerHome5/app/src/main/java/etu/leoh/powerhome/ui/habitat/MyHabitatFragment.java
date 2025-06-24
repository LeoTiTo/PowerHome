package etu.leoh.powerhome.ui.habitat;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.Device;
import etu.leoh.powerhome.model.DeviceReservation;
import etu.leoh.powerhome.model.Habitat;
import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.repository.DeviceRepository;
import etu.leoh.powerhome.repository.HabitatRepository;
import etu.leoh.powerhome.repository.UserRepository;
import etu.leoh.powerhome.ui.device.DeviceListFragment;
import etu.leoh.powerhome.ui.habitat.adapter.ResidentAdapter;
import etu.leoh.powerhome.util.ConsumptionCalculator;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

/**
 * Fragment affichant les informations sur l'habitat de l'utilisateur
 */
public class MyHabitatFragment extends Fragment {

    private TextView habitatCodeTextView;
    private TextView consumptionValueTextView;
    private TextView consumptionLevelTextView;
    private ProgressBar consumptionProgressBar;
    private RecyclerView residentsRecyclerView;
    private TextView devicesCountTextView;
    private Button manageDevicesButton;
    private ProgressBar progressBar;
    
    private HabitatRepository habitatRepository;
    private UserRepository userRepository;
    private DeviceRepository deviceRepository;
    private ConsumptionCalculator consumptionCalculator;
    private FirebaseAuthHelper authHelper;
    
    private ResidentAdapter residentAdapter;
    private List<User> residentList;
    
    private User currentUser;
    private Habitat currentHabitat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_habitat, container, false);
        
        // Initialiser les repositories et helpers
        habitatRepository = new HabitatRepository();
        userRepository = new UserRepository();
        deviceRepository = new DeviceRepository();
        consumptionCalculator = new ConsumptionCalculator();
        authHelper = new FirebaseAuthHelper();
        
        // Initialiser les vues
        habitatCodeTextView = view.findViewById(R.id.tvHabitatCode);
        consumptionValueTextView = view.findViewById(R.id.tvConsumptionValue);
        consumptionLevelTextView = view.findViewById(R.id.tvConsumptionLevel);
        consumptionProgressBar = view.findViewById(R.id.progressBarConsumption);
        residentsRecyclerView = view.findViewById(R.id.recyclerViewResidents);
        devicesCountTextView = view.findViewById(R.id.tvDevicesCount);
        manageDevicesButton = view.findViewById(R.id.btnManageDevices);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Configurer la liste des résidents
        residentList = new ArrayList<>();
        residentAdapter = new ResidentAdapter(residentList);
        residentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        residentsRecyclerView.setAdapter(residentAdapter);
        
        // Configurer le bouton de gestion des appareils
        manageDevicesButton.setOnClickListener(v -> navigateToDeviceList());
        
        // Charger les données de l'utilisateur puis de l'habitat
        loadCurrentUser();
        
        return view;
    }

    /**
     * Charge les données de l'utilisateur connecté
     */
    private void loadCurrentUser() {
        progressBar.setVisibility(View.VISIBLE);
        
        String userId = authHelper.getCurrentUserId();
        if (userId != null) {
            userRepository.getUserById(userId)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            currentUser = task.getResult().toObject(User.class);
                            if (currentUser != null && currentUser.getHabitatId() != null) {
                                loadHabitatData(currentUser.getHabitatId());
                            } else {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), 
                                        "Veuillez d'abord configurer votre habitat dans votre profil", 
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), 
                                    "Erreur lors du chargement des données utilisateur", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Charge les données de l'habitat de l'utilisateur
     * @param habitatId ID de l'habitat à charger
     */
    private void loadHabitatData(String habitatId) {
        habitatRepository.getHabitatById(habitatId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            currentHabitat = document.toObject(Habitat.class);
                            
                            // Mettre à jour l'UI avec les infos de l'habitat
                            if (currentHabitat != null) {
                                updateHabitatUI();
                                
                                // Charger la liste des résidents et le niveau de consommation
                                loadResidents();
                                loadDevices();
                                calculateConsumption();
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), 
                                    "L'habitat n'existe pas", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), 
                                "Erreur lors du chargement des données de l'habitat", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Met à jour l'interface avec les informations de l'habitat
     */
    private void updateHabitatUI() {
        if (currentHabitat != null) {
            // Afficher le code d'accès
            String codeText = "Code: " + currentHabitat.getAccessCode();
            habitatCodeTextView.setText(codeText);
        }
    }

    /**
     * Charge la liste des résidents de l'habitat
     */
    private void loadResidents() {
        if (currentHabitat == null || currentHabitat.getResidentIds() == null) {
            return;
        }
        
        residentList.clear();
        
        // Charger chaque résident un par un
        for (String residentId : currentHabitat.getResidentIds()) {
            userRepository.getUserById(residentId)
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            User resident = document.toObject(User.class);
                            if (resident != null) {
                                residentList.add(resident);
                                residentAdapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
        
        // Si la liste est vide, on ajoute automatiquement l'utilisateur actuel
        if (currentHabitat.getResidentIds().isEmpty() && currentUser != null) {
            currentHabitat.getResidentIds().add(currentUser.getId());
            habitatRepository.updateHabitat(currentHabitat);
            
            residentList.add(currentUser);
            residentAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Charge les informations sur les appareils de l'habitat
     */
    private void loadDevices() {
        if (currentHabitat == null) {
            return;
        }
        
        deviceRepository.getDevicesByHabitatId(currentHabitat.getId())
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (task.isSuccessful()) {
                        int deviceCount = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            deviceCount++;
                        }
                        
                        // Mettre à jour le compteur d'appareils
                        String deviceCountText = getString(R.string.devices_count, deviceCount);
                        devicesCountTextView.setText(deviceCountText);
                    }
                });
    }

    /**
     * Calcule et affiche le niveau de consommation de l'habitat
     */
    private void calculateConsumption() {
        if (currentHabitat == null) {
            return;
        }
        
        // Récupérer d'abord tous les appareils de l'habitat
        deviceRepository.getDevicesByHabitatId(currentHabitat.getId())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int totalPossibleConsumption = 0;
                        int activeConsumption = 0;
                        int activeDevicesCount = 0;
                        int totalDevicesCount = 0;
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Device device = document.toObject(Device.class);
                            if (device != null) {
                                totalDevicesCount++;
                                totalPossibleConsumption += device.getPowerConsumption();
                                
                                // Vérifier le champ principal isActive
                                if (device.isActive()) {
                                    activeDevicesCount++;
                                    activeConsumption += device.getPowerConsumption();
                                }
                                // En cas de double vérification avec le champ active (si présent dans le document)
                                else if (document.contains("active") && Boolean.TRUE.equals(document.getBoolean("active"))) {
                                    // Si active est true alors que isActive est false, on met à jour l'appareil
                                    // pour synchroniser les deux champs
                                    activeDevicesCount++;
                                    activeConsumption += device.getPowerConsumption();
                                    device.setActive(true);
                                    deviceRepository.updateDevice(device);
                                }
                            }
                        }
                        
                        // Calculer le pourcentage de la consommation active par rapport à la consommation maximale possible
                        final int activeWatts = activeConsumption;
                        final int maxPossible = totalPossibleConsumption > 0 ? totalPossibleConsumption : 5000; // par défaut 5kW si pas d'appareils
                        
                        // Calculer le pourcentage de consommation (0-100)
                        final int consumptionPercentage = totalPossibleConsumption > 0 
                                ? (int)((double)activeConsumption / totalPossibleConsumption * 100) 
                                : 0;
                        
                        // Déterminer le niveau de consommation basé sur le pourcentage et la valeur absolue
                        String levelText;
                        int colorResId;
                        int progressValue = consumptionPercentage;
                        
                        // Ajuster les seuils en fonction du nombre d'appareils
                        if (activeConsumption < 1000 || (totalDevicesCount > 0 && consumptionPercentage < 30)) {
                            levelText = getString(R.string.low_consumption);
                            colorResId = android.R.color.holo_green_dark;
                        } else if (activeConsumption < 3000 || (totalDevicesCount > 0 && consumptionPercentage < 70)) {
                            levelText = getString(R.string.medium_consumption);
                            colorResId = android.R.color.holo_orange_dark;
                        } else {
                            levelText = getString(R.string.high_consumption);
                            colorResId = android.R.color.holo_red_dark;
                        }
                        
                        // Garantir que la ProgressBar reflète correctement l'utilisation des appareils
                        progressValue = Math.min(100, Math.max(0, progressValue)); // Limiter entre 0 et 100
                        
                        final String finalLevelText = levelText;
                        final int finalColorResId = colorResId;
                        final int finalProgressValue = progressValue;
                        final int finalTotalDevicesCount = totalDevicesCount;
                        final int finalActiveDevicesCount = activeDevicesCount;
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Afficher la consommation en Watts
                                String consumptionText = activeWatts + " W";
                                if (finalTotalDevicesCount > 0) {
                                    consumptionText += " (" + finalActiveDevicesCount + "/" + finalTotalDevicesCount + " appareils)";
                                }
                                consumptionValueTextView.setText(consumptionText);
                                
                                // Mettre à jour l'UI
                                consumptionLevelTextView.setText(finalLevelText);
                                consumptionLevelTextView.setTextColor(getResources().getColor(finalColorResId));
                                consumptionProgressBar.setProgress(finalProgressValue);
                                
                                // Message d'information sur l'état de la consommation
                                if (finalTotalDevicesCount == 0) {
                                    Toast.makeText(getContext(), 
                                            "Ajoutez des appareils pour visualiser la consommation", 
                                            Toast.LENGTH_SHORT).show();
                                } else if (finalActiveDevicesCount == 0) {
                                    Toast.makeText(getContext(), 
                                            "Aucun appareil actif. Activez-les dans la liste des appareils", 
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        // En cas d'erreur
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), 
                                        "Erreur lors du calcul de la consommation", 
                                        Toast.LENGTH_SHORT).show();
                                
                                // Valeurs par défaut
                                consumptionValueTextView.setText("0 W");
                                consumptionLevelTextView.setText(getString(R.string.low_consumption));
                                consumptionLevelTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                consumptionProgressBar.setProgress(0);
                            });
                        }
                    }
                });
    }

    /**
     * Navigue vers le fragment de liste des appareils
     */
    private void navigateToDeviceList() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DeviceListFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Recalculer la consommation pour prendre en compte les changements d'état des appareils
        if (currentHabitat != null) {
            calculateConsumption();
        }
    }
} 