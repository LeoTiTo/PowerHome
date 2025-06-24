package etu.leoh.powerhome.util;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import etu.leoh.powerhome.model.Device;
import etu.leoh.powerhome.model.DeviceReservation;
import etu.leoh.powerhome.model.Habitat;
import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.repository.DeviceRepository;
import etu.leoh.powerhome.repository.HabitatRepository;
import etu.leoh.powerhome.repository.ReservationRepository;
import etu.leoh.powerhome.repository.UserRepository;

/**
 * Classe utilitaire pour initialiser des données de test dans l'application
 */
public class DataInitializer {
    private static final String TAG = "DataInitializer";
    
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final UserRepository userRepository;
    private final HabitatRepository habitatRepository;
    private final DeviceRepository deviceRepository;
    private final ReservationRepository reservationRepository;
    
    // Configuration
    private static final String RESIDENCE_ID = "residence1";
    private static final String DEFAULT_PASSWORD = "password123";
    
    // Compteurs pour suivre l'avancement
    private final AtomicInteger pendingTasks = new AtomicInteger(0);
    
    // Callback pour informer quand l'initialisation est terminée
    public interface InitializationCallback {
        void onInitializationComplete();
        void onInitializationFailed(Exception e);
    }
    
    public DataInitializer() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();
        habitatRepository = new HabitatRepository();
        deviceRepository = new DeviceRepository();
        reservationRepository = new ReservationRepository();
    }
    
    /**
     * Vérifie si les données de test existent déjà
     * @param callback Callback appelé avec le résultat
     */
    public void checkIfDataExists(OnSuccessListener<Boolean> callback) {
        habitatRepository.getHabitatsByResidence(RESIDENCE_ID)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean exists = !queryDocumentSnapshots.isEmpty();
                    callback.onSuccess(exists);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de la vérification des données", e);
                    callback.onSuccess(false);
                });
    }
    
    /**
     * Initialise les données de test
     * @param callback Callback appelé lorsque l'initialisation est terminée
     */
    public void initializeData(InitializationCallback callback) {
        checkIfDataExists(exists -> {
            if (exists) {
                Log.d(TAG, "Les données existent déjà");
                callback.onInitializationComplete();
                return;
            }
            
            Log.d(TAG, "Démarrage de l'initialisation des données");
            try {
                // Création des habitats
                List<Habitat> habitats = createHabitats();
                
                // Création des appareils
                List<Device> devices = createDevices(habitats);
                
                // Création des utilisateurs
                List<User> users = createUsers(habitats);
                
                // Création des réservations
                createReservations(users, devices);
                
                // Attendre que toutes les tâches soient terminées
                waitForCompletion(callback);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'initialisation des données", e);
                callback.onInitializationFailed(e);
            }
        });
    }
    
    /**
     * Crée plusieurs habitats de test
     * @return Liste des habitats créés
     */
    private List<Habitat> createHabitats() {
        List<Habitat> habitats = new ArrayList<>();
        
        // Créer 5 habitats
        for (int i = 1; i <= 5; i++) {
            String habitatId = "habitat" + i;
            String name = "Appartement " + i;
            String apartmentNumber = String.valueOf(100 + i);
            String accessCode = "CODE" + i;
            
            Habitat habitat = new Habitat(habitatId, name, apartmentNumber, RESIDENCE_ID, accessCode);
            habitats.add(habitat);
            
            // Incrémenter le compteur de tâches en attente
            pendingTasks.incrementAndGet();
            
            // Sauvegarder l'habitat dans Firestore
            habitatRepository.createHabitat(habitat)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Habitat créé avec succès: " + habitat.getName());
                        pendingTasks.decrementAndGet();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur lors de la création de l'habitat", e);
                        pendingTasks.decrementAndGet();
                    });
        }
        
        return habitats;
    }
    
    /**
     * Crée plusieurs appareils de test
     * @param habitats Liste des habitats auxquels associer les appareils
     * @return Liste des appareils créés
     */
    private List<Device> createDevices(List<Habitat> habitats) {
        List<Device> devices = new ArrayList<>();
        
        // Types d'appareils
        String[] deviceTypes = {"Lave-linge", "Sèche-linge", "Lave-vaisselle", "Four", "Réfrigérateur", "Aspirateur", "Télévision"};
        
        // Puissances de consommation typiques (en watts)
        int[] powerConsumptions = {500, 2000, 1200, 2400, 150, 800, 100};
        
        int deviceCount = 0;
        
        // Pour chaque habitat
        for (Habitat habitat : habitats) {
            // Créer 3 appareils par habitat
            for (int i = 0; i < 3; i++) {
                deviceCount++;
                
                // Sélectionner un type d'appareil de manière cyclique
                int typeIndex = (deviceCount - 1) % deviceTypes.length;
                
                String deviceId = "device" + deviceCount;
                String name = deviceTypes[typeIndex] + " " + deviceCount;
                String type = deviceTypes[typeIndex];
                int powerConsumption = powerConsumptions[typeIndex];
                
                Device device = new Device(deviceId, name, type, powerConsumption, habitat.getId());
                device.setActive(Math.random() < 0.3); // 30% de chance d'être actif
                devices.add(device);
                
                // Mettre à jour l'habitat avec l'ID de l'appareil
                habitat.getDeviceIds().add(deviceId);
                
                // Incrémenter le compteur de tâches en attente
                pendingTasks.incrementAndGet();
                
                // Sauvegarder l'appareil dans Firestore
                deviceRepository.addDevice(device)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Appareil créé avec succès: " + device.getName());
                            pendingTasks.decrementAndGet();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Erreur lors de la création de l'appareil", e);
                            pendingTasks.decrementAndGet();
                        });
            }
            
            // Mettre à jour l'habitat avec les appareils
            pendingTasks.incrementAndGet();
            habitatRepository.updateHabitat(habitat)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Habitat mis à jour avec les appareils: " + habitat.getName());
                        pendingTasks.decrementAndGet();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur lors de la mise à jour de l'habitat", e);
                        pendingTasks.decrementAndGet();
                    });
        }
        
        return devices;
    }
    
    /**
     * Crée plusieurs utilisateurs de test
     * @param habitats Liste des habitats auxquels associer les utilisateurs
     * @return Liste des utilisateurs créés
     */
    private List<User> createUsers(List<Habitat> habitats) {
        List<User> users = new ArrayList<>();
        
        // Créer d'abord l'utilisateur de test spécial
        String testUserId = "testuser";
        String testEmail = "test@test.com";
        User testUser = new User(testUserId, testEmail, "Test", "User", habitats.get(0).getId());
        testUser.setEcoCoins(100);
        users.add(testUser);
        
        // Mettre à jour l'habitat avec l'ID de l'utilisateur de test
        habitats.get(0).getResidentIds().add(testUserId);
        
        // Créer l'utilisateur de test dans Firebase Auth
        pendingTasks.incrementAndGet();
        createAuthUser(testEmail, DEFAULT_PASSWORD, testUser);
        
        // Mettre à jour l'habitat avec le résident de test
        pendingTasks.incrementAndGet();
        habitatRepository.updateHabitat(habitats.get(0))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Habitat mis à jour avec l'utilisateur de test");
                    pendingTasks.decrementAndGet();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de la mise à jour de l'habitat", e);
                    pendingTasks.decrementAndGet();
                });
        
        // Noms et prénoms pour les utilisateurs
        String[] firstNames = {"Alice", "Bob", "Charlie", "Diana", "Ethan", "Fiona", "George", "Hannah"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson"};
        
        int userCount = 0;
        
        // Pour chaque habitat
        for (Habitat habitat : habitats) {
            // Créer 1-2 utilisateurs par habitat
            int usersPerHabitat = 1 + (int)(Math.random() * 2); // 1 ou 2
            
            for (int i = 0; i < usersPerHabitat; i++) {
                userCount++;
                
                String userId = "user" + userCount;
                String firstName = firstNames[(userCount - 1) % firstNames.length];
                String lastName = lastNames[(userCount - 1) % lastNames.length];
                String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + userCount + "@example.com";
                
                User user = new User(userId, email, firstName, lastName, habitat.getId());
                user.setEcoCoins((int)(Math.random() * 50) + 10); // 10-60 éco-coins
                users.add(user);
                
                // Mettre à jour l'habitat avec l'ID de l'utilisateur
                habitat.getResidentIds().add(userId);
                
                // Créer l'utilisateur dans Firebase Auth (ne sera pas utilisé pour se connecter)
                pendingTasks.incrementAndGet();
                createAuthUser(email, DEFAULT_PASSWORD, user);
            }
            
            // Mettre à jour l'habitat avec les résidents
            pendingTasks.incrementAndGet();
            habitatRepository.updateHabitat(habitat)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Habitat mis à jour avec les résidents: " + habitat.getName());
                        pendingTasks.decrementAndGet();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur lors de la mise à jour de l'habitat", e);
                        pendingTasks.decrementAndGet();
                    });
        }
        
        return users;
    }
    
    /**
     * Crée un utilisateur dans Firebase Auth et Firestore
     * @param email Email de l'utilisateur
     * @param password Mot de passe de l'utilisateur
     * @param user Objet User à sauvegarder dans Firestore
     */
    private void createAuthUser(String email, String password, User user) {
        // Stocker l'état de connexion actuel
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        // Utiliser la méthode pour créer un utilisateur sans se déconnecter
        userRepository.saveUserInfo(user)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Utilisateur Firestore créé avec succès: " + user.getFirstName());
                pendingTasks.decrementAndGet();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de la création de l'utilisateur Firestore", e);
                pendingTasks.decrementAndGet();
            });
            
        // Ne pas essayer de créer l'utilisateur Auth, cela peut provoquer des problèmes
        // quand plusieurs utilisateurs sont créés rapidement et que l'un d'eux est connecté
    }
    
    /**
     * Crée plusieurs réservations de test
     * @param users Liste des utilisateurs
     * @param devices Liste des appareils
     */
    private void createReservations(List<User> users, List<Device> devices) {
        // Pour chaque utilisateur
        for (User user : users) {
            // Créer 1-3 réservations par utilisateur
            int reservationsPerUser = 1 + (int)(Math.random() * 3); // 1-3
            
            for (int i = 0; i < reservationsPerUser; i++) {
                // Choisir un appareil du même habitat que l'utilisateur
                List<Device> habitatDevices = new ArrayList<>();
                for (Device device : devices) {
                    if (device.getHabitatId().equals(user.getHabitatId())) {
                        habitatDevices.add(device);
                    }
                }
                
                if (habitatDevices.isEmpty()) continue;
                
                Device device = habitatDevices.get((int)(Math.random() * habitatDevices.size()));
                
                // Créer une réservation
                String reservationId = "reservation_" + user.getId() + "_" + device.getId() + "_" + i;
                
                // Dates de réservation (entre aujourd'hui et les 7 prochains jours)
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.HOUR, (int)(Math.random() * 24 * 7)); // 0-7 jours
                Date startTime = calendar.getTime();
                
                calendar.add(Calendar.HOUR, 1 + (int)(Math.random() * 3)); // 1-3 heures après
                Date endTime = calendar.getTime();
                
                DeviceReservation reservation = new DeviceReservation(reservationId, device.getId(), user.getId(), startTime, endTime);
                
                // Certaines réservations sont déjà terminées
                if (Math.random() < 0.3) { // 30% de chance
                    reservation.setCompleted(true);
                    reservation.setEcoCoinsEarned((int)(Math.random() * 10) + 5); // 5-15 éco-coins
                }
                
                // Définir le niveau de consommation
                double rand = Math.random();
                if (rand < 0.5) {
                    reservation.setConsumptionLevelAtReservation(DeviceReservation.ConsumptionLevel.LOW);
                } else if (rand < 0.8) {
                    reservation.setConsumptionLevelAtReservation(DeviceReservation.ConsumptionLevel.MEDIUM);
                } else {
                    reservation.setConsumptionLevelAtReservation(DeviceReservation.ConsumptionLevel.HIGH);
                }
                
                // Incrémenter le compteur de tâches en attente
                pendingTasks.incrementAndGet();
                
                // Sauvegarder la réservation dans Firestore
                reservationRepository.addReservation(reservation)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Réservation créée avec succès pour " + user.getFirstName());
                            pendingTasks.decrementAndGet();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Erreur lors de la création de la réservation", e);
                            pendingTasks.decrementAndGet();
                        });
            }
        }
    }
    
    /**
     * Attend que toutes les tâches asynchrones soient terminées
     * @param callback Callback à appeler une fois terminé
     */
    private void waitForCompletion(InitializationCallback callback) {
        new Thread(() -> {
            try {
                // Attendre que toutes les tâches soient terminées (avec timeout de sécurité)
                int timeout = 0;
                while (pendingTasks.get() > 0 && timeout < 30) { // Réduire à 30 secondes max
                    Thread.sleep(1000);
                    timeout++;
                    Log.d(TAG, "Tâches en attente: " + pendingTasks.get() + ", timeout: " + timeout);
                }
                
                if (pendingTasks.get() > 0) {
                    Log.w(TAG, "Timeout atteint avec " + pendingTasks.get() + " tâches encore en attente - continuons quand même");
                    // Forcer la réinitialisation du compteur pour éviter de bloquer l'application
                    pendingTasks.set(0);
                }
                
                // Appeler le callback sur le thread principal
                if (callback != null) {
                    FirebaseAuthHelper.getMainHandler().post(() -> callback.onInitializationComplete());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur pendant l'attente des tâches", e);
                if (callback != null) {
                    FirebaseAuthHelper.getMainHandler().post(() -> callback.onInitializationFailed(e));
                }
            }
        }).start();
    }
} 