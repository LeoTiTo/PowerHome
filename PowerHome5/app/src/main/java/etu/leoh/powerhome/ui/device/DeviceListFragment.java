package etu.leoh.powerhome.ui.device;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.Device;
import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.repository.DeviceRepository;
import etu.leoh.powerhome.repository.UserRepository;
import etu.leoh.powerhome.ui.device.adapter.DeviceAdapter;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

/**
 * Fragment affichant la liste des appareils de l'utilisateur
 */
public class DeviceListFragment extends Fragment implements DeviceAdapter.DeviceInteractionListener {

    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private List<Device> deviceList;
    private ProgressBar progressBar;
    private FloatingActionButton addDeviceButton;
    
    private DeviceRepository deviceRepository;
    private UserRepository userRepository;
    private FirebaseAuthHelper authHelper;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        
        // Initialiser les repositories et helpers
        deviceRepository = new DeviceRepository();
        userRepository = new UserRepository();
        authHelper = new FirebaseAuthHelper();
        
        // Initialiser les vues
        recyclerView = view.findViewById(R.id.recyclerViewDevices);
        progressBar = view.findViewById(R.id.progressBar);
        addDeviceButton = view.findViewById(R.id.fabAddDevice);
        
        // Configurer le RecyclerView
        deviceList = new ArrayList<>();
        adapter = new DeviceAdapter(deviceList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Configurer le bouton d'ajout
        addDeviceButton.setOnClickListener(v -> showAddDeviceDialog());
        
        // Charger l'utilisateur actuel puis ses appareils
        loadCurrentUser();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Recharger les appareils lors du retour au fragment
        if (currentUser != null && currentUser.getHabitatId() != null) {
            loadDevices();
        }
    }

    /**
     * Charge les informations de l'utilisateur actuel
     */
    private void loadCurrentUser() {
        progressBar.setVisibility(View.VISIBLE);
        
        String userId = authHelper.getCurrentUserId();
        if (userId != null) {
            userRepository.getUserById(userId)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            currentUser = task.getResult().toObject(User.class);
                            loadDevices();
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
     * Charge la liste des appareils de l'utilisateur
     */
    private void loadDevices() {
        // Vérifier si l'utilisateur a un habitat
        if (currentUser == null || currentUser.getHabitatId() == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), 
                    "Veuillez d'abord configurer votre habitat dans votre profil", 
                    Toast.LENGTH_LONG).show();
            return;
        }
        
        // Récupérer les appareils de l'habitat
        deviceRepository.getDevicesByHabitatId(currentUser.getHabitatId())
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (task.isSuccessful()) {
                        deviceList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Device device = document.toObject(Device.class);
                            
                            // Vérifier si les champs isActive et active sont différents
                            if (document.contains("active") && document.contains("isActive")) {
                                Boolean activeField = document.getBoolean("active");
                                Boolean isActiveField = document.getBoolean("isActive");
                                
                                // Si les champs sont différents, mettre à jour l'appareil pour synchroniser
                                if (activeField != null && isActiveField != null && activeField != isActiveField) {
                                    // On utilise isActive comme valeur de référence
                                    device.setActive(isActiveField);
                                    deviceRepository.updateDevice(device);
                                }
                            }
                            
                            deviceList.add(device);
                        }
                        adapter.notifyDataSetChanged();
                        
                        // Afficher un message si la liste est vide
                        if (deviceList.isEmpty()) {
                            Toast.makeText(getContext(), 
                                    "Aucun appareil trouvé. Ajoutez-en un!", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), 
                                "Erreur lors du chargement des appareils: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Affiche un dialogue pour ajouter un nouvel appareil
     */
    private void showAddDeviceDialog() {
        // Vérifier si l'utilisateur a un habitat
        if (currentUser == null || currentUser.getHabitatId() == null) {
            Toast.makeText(getContext(), 
                    "Veuillez d'abord configurer votre habitat dans votre profil", 
                    Toast.LENGTH_LONG).show();
            return;
        }
        
        // Créer le dialogue avec un formulaire personnalisé
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Ajouter un appareil");
        
        // Créer la vue personnalisée
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_device, null);
        builder.setView(dialogView);
        
        // Récupérer les références aux champs
        final EditText nameEditText = dialogView.findViewById(R.id.etDeviceName);
        final AutoCompleteTextView typeAutoComplete = dialogView.findViewById(R.id.etDeviceType);
        final AutoCompleteTextView powerAutoComplete = dialogView.findViewById(R.id.etDevicePower);
        
        // Préparer les options pour le type d'appareil
        String[] deviceTypes = new String[] {
            "Lave-linge", "Sèche-linge", "Lave-vaisselle", "Four", "Réfrigérateur", 
            "Congélateur", "Aspirateur", "Télévision", "Ordinateur", "Climatiseur", 
            "Chauffage électrique", "Micro-ondes", "Bouilloire", "Fer à repasser"
        };
        
        // Préparer les options pour la consommation électrique (en Watts)
        String[] powerOptions = new String[] {
            "100", "150", "200", "300", "500", "750", "1000", "1200", 
            "1500", "2000", "2500", "3000"
        };
        
        // Créer et configurer l'adaptateur pour le type d'appareil
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                deviceTypes);
        typeAutoComplete.setAdapter(typeAdapter);
        
        // Créer et configurer l'adaptateur pour la consommation électrique
        ArrayAdapter<String> powerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                powerOptions);
        powerAutoComplete.setAdapter(powerAdapter);
        
        // Configurer les boutons
        builder.setPositiveButton("Ajouter", null); // Sera remplacé pour éviter fermeture auto
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        
        // Créer et afficher le dialogue
        final AlertDialog dialog = builder.create();
        dialog.show();
        
        // Remplacer le listener du bouton positif pour validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Récupérer les valeurs
            String name = nameEditText.getText().toString().trim();
            String type = typeAutoComplete.getText().toString().trim();
            String powerStr = powerAutoComplete.getText().toString().trim();
            
            // Valider les entrées
            if (name.isEmpty()) {
                nameEditText.setError("Le nom est requis");
                nameEditText.requestFocus();
                return;
            }
            
            if (type.isEmpty()) {
                typeAutoComplete.setError("Le type est requis");
                typeAutoComplete.requestFocus();
                return;
            }
            
            if (powerStr.isEmpty()) {
                powerAutoComplete.setError("La consommation est requise");
                powerAutoComplete.requestFocus();
                return;
            }
            
            int power;
            try {
                power = Integer.parseInt(powerStr);
            } catch (NumberFormatException e) {
                powerAutoComplete.setError("Entrez un nombre valide");
                powerAutoComplete.requestFocus();
                return;
            }
            
            // Tout est valide, créer l'appareil
            String deviceId = UUID.randomUUID().toString();
            Device newDevice = new Device(deviceId, name, type, power, currentUser.getHabitatId());
            
            // Afficher la progression
            progressBar.setVisibility(View.VISIBLE);
            
            // Enregistrer l'appareil
            deviceRepository.addDevice(newDevice)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        
                        if (task.isSuccessful()) {
                            // Ajouter l'appareil à la liste et rafraîchir
                            deviceList.add(newDevice);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getContext(), "Appareil ajouté avec succès", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            
                            // Ajouter l'ID de l'appareil à l'utilisateur
                            currentUser.addDevice(deviceId);
                            userRepository.updateUser(currentUser);
                        } else {
                            Toast.makeText(getContext(), 
                                    "Erreur lors de l'ajout de l'appareil: " + task.getException().getMessage(), 
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    /**
     * Affiche un dialogue pour modifier un appareil existant
     */
    private void showEditDeviceDialog(Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Modifier l'appareil");
        
        // Créer la vue personnalisée
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_device, null);
        builder.setView(dialogView);
        
        // Récupérer les références aux champs
        final EditText nameEditText = dialogView.findViewById(R.id.etDeviceName);
        final AutoCompleteTextView typeAutoComplete = dialogView.findViewById(R.id.etDeviceType);
        final AutoCompleteTextView powerAutoComplete = dialogView.findViewById(R.id.etDevicePower);
        
        // Préparer les options pour le type d'appareil
        String[] deviceTypes = new String[] {
            "Lave-linge", "Sèche-linge", "Lave-vaisselle", "Four", "Réfrigérateur", 
            "Congélateur", "Aspirateur", "Télévision", "Ordinateur", "Climatiseur", 
            "Chauffage électrique", "Micro-ondes", "Bouilloire", "Fer à repasser"
        };
        
        // Préparer les options pour la consommation électrique (en Watts)
        String[] powerOptions = new String[] {
            "100", "150", "200", "300", "500", "750", "1000", "1200", 
            "1500", "2000", "2500", "3000"
        };
        
        // Créer et configurer l'adaptateur pour le type d'appareil
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                deviceTypes);
        typeAutoComplete.setAdapter(typeAdapter);
        
        // Créer et configurer l'adaptateur pour la consommation électrique
        ArrayAdapter<String> powerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                powerOptions);
        powerAutoComplete.setAdapter(powerAdapter);
        
        // Pré-remplir les champs
        nameEditText.setText(device.getName());
        typeAutoComplete.setText(device.getType());
        powerAutoComplete.setText(String.valueOf(device.getPowerConsumption()));
        
        // Configurer les boutons
        builder.setPositiveButton("Enregistrer", null); // Sera remplacé
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        
        // Créer et afficher le dialogue
        final AlertDialog dialog = builder.create();
        dialog.show();
        
        // Remplacer le listener du bouton positif
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Récupérer les valeurs
            String name = nameEditText.getText().toString().trim();
            String type = typeAutoComplete.getText().toString().trim();
            String powerStr = powerAutoComplete.getText().toString().trim();
            
            // Valider les entrées
            if (name.isEmpty()) {
                nameEditText.setError("Le nom est requis");
                nameEditText.requestFocus();
                return;
            }
            
            if (type.isEmpty()) {
                typeAutoComplete.setError("Le type est requis");
                typeAutoComplete.requestFocus();
                return;
            }
            
            if (powerStr.isEmpty()) {
                powerAutoComplete.setError("La consommation est requise");
                powerAutoComplete.requestFocus();
                return;
            }
            
            int power;
            try {
                power = Integer.parseInt(powerStr);
            } catch (NumberFormatException e) {
                powerAutoComplete.setError("Entrez un nombre valide");
                powerAutoComplete.requestFocus();
                return;
            }
            
            // Mettre à jour l'appareil
            device.setName(name);
            device.setType(type);
            device.setPowerConsumption(power);
            
            // Afficher la progression
            progressBar.setVisibility(View.VISIBLE);
            
            // Enregistrer les modifications
            deviceRepository.updateDevice(device)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        
                        if (task.isSuccessful()) {
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getContext(), "Appareil mis à jour avec succès", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), 
                                    "Erreur lors de la mise à jour de l'appareil: " + task.getException().getMessage(), 
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    /**
     * Supprime un appareil après confirmation
     */
    private void deleteDevice(Device device) {
        new AlertDialog.Builder(getContext())
                .setTitle("Supprimer l'appareil")
                .setMessage("Êtes-vous sûr de vouloir supprimer cet appareil ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    // Afficher la progression
                    progressBar.setVisibility(View.VISIBLE);
                    
                    // Supprimer l'appareil
                    deviceRepository.deleteDevice(device.getId())
                            .addOnCompleteListener(task -> {
                                progressBar.setVisibility(View.GONE);
                                
                                if (task.isSuccessful()) {
                                    // Supprimer de la liste et rafraîchir
                                    deviceList.remove(device);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(getContext(), "Appareil supprimé avec succès", Toast.LENGTH_SHORT).show();
                                    
                                    // Supprimer l'ID de l'appareil de l'utilisateur
                                    currentUser.removeDevice(device.getId());
                                    userRepository.updateUser(currentUser);
                                } else {
                                    Toast.makeText(getContext(), 
                                            "Erreur lors de la suppression de l'appareil: " + task.getException().getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    /**
     * Change l'état actif/inactif d'un appareil
     */
    private void toggleDeviceStatus(Device device) {
        // Changer l'état localement
        boolean newActiveState = !device.isActive();
        device.setActive(newActiveState);
        
        // Mettre immédiatement à jour l'affichage pour meilleure réactivité
        adapter.notifyDataSetChanged();
        
        // Mettre à jour l'appareil dans Firebase
        deviceRepository.updateDevice(device)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String status = device.isActive() ? "activé" : "désactivé";
                        Toast.makeText(getContext(), "Appareil " + status, Toast.LENGTH_SHORT).show();
                    } else {
                        // Annuler le changement en cas d'erreur
                        device.setActive(!device.isActive());
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), 
                                "Erreur lors de la mise à jour du statut: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Implémentation des callbacks de l'adaptateur
    @Override
    public void onDeviceEdit(Device device) {
        showEditDeviceDialog(device);
    }

    @Override
    public void onDeviceDelete(Device device) {
        deleteDevice(device);
    }

    @Override
    public void onDeviceStatusToggle(Device device) {
        toggleDeviceStatus(device);
    }
} 