package etu.leoh.powerhome.ui.reservation;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.Device;
import etu.leoh.powerhome.model.DeviceReservation;
import etu.leoh.powerhome.repository.DeviceRepository;
import etu.leoh.powerhome.repository.DeviceReservationRepository;
import etu.leoh.powerhome.repository.UserRepository;
import etu.leoh.powerhome.util.FirebaseAuthHelper;
import etu.leoh.powerhome.util.PowerUsageSimulator;

/**
 * Activité pour la création de nouvelles réservations d'appareils
 */
public class NewReservationActivity extends AppCompatActivity {
    private static final String TAG = "NewReservationActivity";
    
    private Spinner deviceSpinner;
    private Button startTimeButton;
    private Button endTimeButton;
    private Button bookButton;
    private TextView consumptionLevelTextView;
    private TextView ecoCoinsInfoTextView;
    
    private DeviceRepository deviceRepository;
    private DeviceReservationRepository reservationRepository;
    private UserRepository userRepository;
    private FirebaseAuthHelper authHelper;
    private PowerUsageSimulator powerUsageSimulator;
    
    private List<Device> deviceList;
    private Map<String, Device> deviceMap;
    private Device selectedDevice;
    
    private Calendar startCalendar;
    private Calendar endCalendar;
    
    private SimpleDateFormat dateTimeFormatter;
    private DeviceReservation.ConsumptionLevel currentConsumptionLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_reservation);
        
        // Initialiser les repositories et helpers
        deviceRepository = new DeviceRepository();
        reservationRepository = new DeviceReservationRepository();
        userRepository = new UserRepository();
        authHelper = new FirebaseAuthHelper();
        powerUsageSimulator = new PowerUsageSimulator();
        
        // Initialiser les objets de date et le formateur
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR_OF_DAY, 1); // Par défaut, 1h après l'heure de début
        dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);
        
        // Initialiser les collections pour les appareils
        deviceList = new ArrayList<>();
        deviceMap = new HashMap<>();
        
        // Niveau de consommation par défaut
        currentConsumptionLevel = DeviceReservation.ConsumptionLevel.LOW;
        
        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.new_reservation);
        
        // Initialiser les vues
        deviceSpinner = findViewById(R.id.spinnerDevice);
        startTimeButton = findViewById(R.id.btnSelectStartTime);
        endTimeButton = findViewById(R.id.btnSelectEndTime);
        bookButton = findViewById(R.id.btnBook);
        consumptionLevelTextView = findViewById(R.id.tvConsumptionLevel);
        ecoCoinsInfoTextView = findViewById(R.id.tvEcoCoinsInfo);
        
        // Mettre à jour les textes des boutons de temps
        updateTimeButtonText();
        
        // Charger les appareils disponibles
        loadAvailableDevices();
        
        // Configurer les écouteurs
        setupListeners();
    }
    
    /**
     * Configure les écouteurs d'événements pour les boutons et le spinner
     */
    private void setupListeners() {
        // Écouteur pour le spinner d'appareils
        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < deviceList.size()) {
                    selectedDevice = deviceList.get(position);
                    updateConsumptionLevel();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDevice = null;
            }
        });
        
        // Écouteur pour le bouton d'heure de début
        startTimeButton.setOnClickListener(v -> showDateTimePicker(true));
        
        // Écouteur pour le bouton d'heure de fin
        endTimeButton.setOnClickListener(v -> showDateTimePicker(false));
        
        // Configurer le bouton de réservation
        bookButton.setOnClickListener(v -> createReservation());
    }
    
    /**
     * Charge les appareils disponibles pour l'habitat de l'utilisateur
     */
    private void loadAvailableDevices() {
        String userId = authHelper.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Récupérer l'habitatId de l'utilisateur courant
        userRepository.getUserById(userId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String habitatId = task.getResult().getString("habitatId");
                if (habitatId != null && !habitatId.isEmpty()) {
                    // Récupérer les appareils de cet habitat
                    deviceRepository.getDevicesByHabitatId(habitatId).addOnCompleteListener(devicesTask -> {
                        if (devicesTask.isSuccessful()) {
                            deviceList.clear();
                            deviceMap.clear();
                            
                            for (QueryDocumentSnapshot document : devicesTask.getResult()) {
                                Device device = document.toObject(Device.class);
                                deviceList.add(device);
                                deviceMap.put(device.getId(), device);
                            }
                            
                            if (deviceList.isEmpty()) {
                                Toast.makeText(this, "Aucun appareil disponible pour cet habitat", Toast.LENGTH_SHORT).show();
                            } else {
                                // Remplir le spinner avec les noms des appareils
                                List<String> deviceNames = new ArrayList<>();
                                for (Device device : deviceList) {
                                    deviceNames.add(device.getName());
                                }
                                
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        this, android.R.layout.simple_spinner_item, deviceNames);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                deviceSpinner.setAdapter(adapter);
                                
                                // Sélectionner le premier appareil par défaut
                                if (!deviceList.isEmpty()) {
                                    selectedDevice = deviceList.get(0);
                                    updateConsumptionLevel();
                                }
                            }
                        } else {
                            Log.e(TAG, "Erreur lors de la récupération des appareils", devicesTask.getException());
                            Toast.makeText(this, "Impossible de charger les appareils", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Vous n'êtes associé à aucun habitat", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Erreur lors de la récupération de l'utilisateur", task.getException());
                Toast.makeText(this, "Impossible de charger les informations utilisateur", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Affiche un sélecteur de date et d'heure
     * @param isStartTime true pour l'heure de début, false pour l'heure de fin
     */
    private void showDateTimePicker(final boolean isStartTime) {
        final Calendar calendar = isStartTime ? startCalendar : endCalendar;
        
        // Créer un DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    // Après avoir sélectionné la date, ouvrir le sélecteur d'heure
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (view1, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                
                                // Mettre à jour le texte du bouton
                                updateTimeButtonText();
                                
                                // Recalculer le niveau de consommation
                                updateConsumptionLevel();
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }
    
    /**
     * Met à jour le texte des boutons de sélection d'heure
     */
    private void updateTimeButtonText() {
        startTimeButton.setText(dateTimeFormatter.format(startCalendar.getTime()));
        endTimeButton.setText(dateTimeFormatter.format(endCalendar.getTime()));
    }
    
    /**
     * Détermine le niveau de consommation actuel et met à jour l'interface
     */
    private void updateConsumptionLevel() {
        if (selectedDevice == null) return;
        
        // Simuler le niveau de consommation à l'heure sélectionnée
        // Cette méthode devrait en réalité récupérer les données de consommation réelles
        currentConsumptionLevel = powerUsageSimulator.simulateConsumptionLevel(
                startCalendar.getTime(), selectedDevice.getPowerConsumption());
        
        // Mettre à jour l'affichage
        switch (currentConsumptionLevel) {
            case LOW:
                consumptionLevelTextView.setText(getString(R.string.low_consumption));
                consumptionLevelTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                ecoCoinsInfoTextView.setText("Vous gagnerez 10 éco-coins avec cette réservation !");
                break;
            case MEDIUM:
                consumptionLevelTextView.setText(getString(R.string.medium_consumption));
                consumptionLevelTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                ecoCoinsInfoTextView.setText("Cette réservation ne modifiera pas votre solde d'éco-coins.");
                break;
            case HIGH:
                consumptionLevelTextView.setText(getString(R.string.high_consumption));
                consumptionLevelTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                ecoCoinsInfoTextView.setText("Attention : vous perdrez 5 éco-coins avec cette réservation.");
                break;
        }
    }
    
    /**
     * Crée une nouvelle réservation avec les données sélectionnées
     */
    private void createReservation() {
        // Vérifier que toutes les données nécessaires sont présentes
        if (selectedDevice == null) {
            Toast.makeText(this, "Veuillez sélectionner un appareil", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Vérifier que l'heure de fin est après l'heure de début
        if (endCalendar.before(startCalendar)) {
            Toast.makeText(this, "L'heure de fin doit être après l'heure de début", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = authHelper.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Créer la réservation
        String reservationId = "reservation_" + UUID.randomUUID().toString();
        DeviceReservation reservation = new DeviceReservation(
                reservationId,
                selectedDevice.getId(),
                userId,
                startCalendar.getTime(),
                endCalendar.getTime()
        );
        
        // Définir le niveau de consommation et les éco-coins
        reservation.setConsumptionLevelAtReservation(currentConsumptionLevel);
        reservation.setEcoCoinsEarned(reservation.calculateEcoCoins(currentConsumptionLevel));
        
        // Sauvegarder la réservation
        reservationRepository.createReservation(reservation)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Réservation créée avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de la création de la réservation", e);
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 