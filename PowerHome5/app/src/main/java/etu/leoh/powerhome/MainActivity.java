package etu.leoh.powerhome;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.repository.UserRepository;
import etu.leoh.powerhome.ui.about.AboutFragment;
import etu.leoh.powerhome.ui.auth.LoginActivity;
import etu.leoh.powerhome.ui.consumption.ConsumptionCalendarFragment;
import etu.leoh.powerhome.ui.device.DeviceListFragment;
import etu.leoh.powerhome.ui.habitat.AllHabitatsFragment;
import etu.leoh.powerhome.ui.habitat.MyHabitatFragment;
import etu.leoh.powerhome.ui.home.HomeFragment;
import etu.leoh.powerhome.ui.profile.ProfileFragment;
import etu.leoh.powerhome.ui.reservation.ReservationListFragment;
import etu.leoh.powerhome.ui.settings.SettingsFragment;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private NavigationView navigationView;
    private FirebaseAuthHelper authHelper;
    private UserRepository userRepository;
    private User currentUser;
    
    // Variable pour stocker la référence au listener
    private UserRepository.UserChangeListener userChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialiser les helpers et repositories
        authHelper = new FirebaseAuthHelper();
        userRepository = new UserRepository();
        
        // Vérifier si l'utilisateur est connecté
        if (!authHelper.isUserLoggedIn()) {
            // Rediriger vers l'écran de connexion
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Configurer la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Configurer le Navigation Drawer
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        
        // Charger les informations de l'utilisateur
        loadUserData();
        
        // Afficher le fragment d'accueil par défaut
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    /**
     * Charge les données de l'utilisateur connecté et met à jour l'en-tête du drawer
     */
    private void loadUserData() {
        String userId = authHelper.getCurrentUserId();
        if (userId != null) {
            // Utiliser un listener en temps réel plutôt qu'une requête ponctuelle
            userChangeListener = (user, error) -> {
                if (error != null) {
                    Toast.makeText(MainActivity.this, 
                            "Erreur lors du chargement des données utilisateur: " + error.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (user != null) {
                    currentUser = user;
                    updateNavigationHeader();
                }
            };
            
            userRepository.listenForUserChanges(userId, userChangeListener);
        }
    }

    /**
     * Met à jour l'en-tête du navigation drawer avec les informations de l'utilisateur
     */
    private void updateNavigationHeader() {
        if (currentUser != null) {
            View headerView = navigationView.getHeaderView(0);
            
            TextView nameTextView = headerView.findViewById(R.id.tvUserName);
            TextView emailTextView = headerView.findViewById(R.id.tvUserEmail);
            TextView ecoCoinsTextView = headerView.findViewById(R.id.tvEcoCoins);
            
            String fullName = currentUser.getFirstName() + " " + currentUser.getLastName();
            nameTextView.setText(fullName);
            emailTextView.setText(currentUser.getEmail());
            
            // S'assurer que les éco-coins sont formatés correctement
            String formattedEcoCoins = String.format(getString(R.string.my_eco_coins), currentUser.getEcoCoins());
            ecoCoinsTextView.setText(formattedEcoCoins);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données utilisateur à chaque fois que l'activité devient visible
        // pour s'assurer que les éco-coins sont à jour
        loadUserData();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Gérer les sélections dans le menu du drawer
        Fragment selectedFragment = null;
        String title = getString(R.string.app_name);
        
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
            title = getString(R.string.home);
        } else if (itemId == R.id.nav_my_habitat) {
            selectedFragment = new MyHabitatFragment();
            title = getString(R.string.my_habitat);
        } else if (itemId == R.id.nav_all_habitats) {
            selectedFragment = new AllHabitatsFragment();
            title = getString(R.string.all_habitats);
        } else if (itemId == R.id.nav_my_devices) {
            selectedFragment = new DeviceListFragment();
            title = getString(R.string.my_devices);
        } else if (itemId == R.id.nav_my_reservations) {
            selectedFragment = new ReservationListFragment();
            title = getString(R.string.my_reservations);
        } else if (itemId == R.id.nav_consumption_calendar) {
            selectedFragment = new ConsumptionCalendarFragment();
            title = getString(R.string.consumption_calendar);
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
            title = getString(R.string.my_profile);
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
            title = getString(R.string.settings);
        } else if (itemId == R.id.nav_about) {
            selectedFragment = new AboutFragment();
            title = getString(R.string.about);
        } else if (itemId == R.id.nav_logout) {
            logoutUser();
            return true;
        }
        
        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    selectedFragment).commit();
            setTitle(title);
        }
        
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Déconnecte l'utilisateur et redirige vers l'écran de connexion
     */
    private void logoutUser() {
        authHelper.logoutUser();
        Toast.makeText(this, "Vous avez été déconnecté", Toast.LENGTH_SHORT).show();
        
        // Rediriger vers l'écran de connexion
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Détacher le listener pour éviter les fuites de mémoire
        if (userChangeListener != null && authHelper.getCurrentUserId() != null) {
            userRepository.detachUserListener(authHelper.getCurrentUserId());
        }
    }
}