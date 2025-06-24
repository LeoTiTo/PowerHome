package etu.leoh.powerhome.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import etu.leoh.powerhome.R;

/**
 * Fragment des paramètres de l'application
 */
public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "PowerHomePrefs";
    private static final String KEY_PUSH_NOTIFICATIONS = "push_notifications";
    private static final String KEY_CONSUMPTION_ALERTS = "consumption_alerts";
    private static final String KEY_RESERVATION_REMINDERS = "reservation_reminders";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_TEXT_SIZE = "text_size";

    private Switch pushNotificationsSwitch;
    private Switch consumptionAlertsSwitch;
    private Switch reservationRemindersSwitch;
    private Switch darkModeSwitch;
    private RadioGroup textSizeRadioGroup;
    private RadioButton smallRadioButton;
    private RadioButton mediumRadioButton;
    private RadioButton largeRadioButton;
    private Button clearCacheButton;
    private Button sendFeedbackButton;
    private Button privacyPolicyButton;
    private Button saveButton;

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialiser les SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialiser les vues
        pushNotificationsSwitch = view.findViewById(R.id.switchPushNotifications);
        consumptionAlertsSwitch = view.findViewById(R.id.switchConsumptionAlerts);
        reservationRemindersSwitch = view.findViewById(R.id.switchReservationReminders);
        darkModeSwitch = view.findViewById(R.id.switchDarkMode);
        textSizeRadioGroup = view.findViewById(R.id.radioGroupTextSize);
        smallRadioButton = view.findViewById(R.id.rbSmall);
        mediumRadioButton = view.findViewById(R.id.rbMedium);
        largeRadioButton = view.findViewById(R.id.rbLarge);
        clearCacheButton = view.findViewById(R.id.btnClearCache);
        sendFeedbackButton = view.findViewById(R.id.btnSendFeedback);
        privacyPolicyButton = view.findViewById(R.id.btnPrivacyPolicy);
        saveButton = view.findViewById(R.id.btnSaveSettings);

        // Charger les paramètres sauvegardés
        loadSettings();

        // Configurer les écouteurs
        setupListeners();

        return view;
    }

    /**
     * Charge les paramètres depuis les SharedPreferences
     */
    private void loadSettings() {
        // Paramètres de notification
        pushNotificationsSwitch.setChecked(sharedPreferences.getBoolean(KEY_PUSH_NOTIFICATIONS, true));
        consumptionAlertsSwitch.setChecked(sharedPreferences.getBoolean(KEY_CONSUMPTION_ALERTS, true));
        reservationRemindersSwitch.setChecked(sharedPreferences.getBoolean(KEY_RESERVATION_REMINDERS, true));

        // Paramètres d'affichage
        darkModeSwitch.setChecked(sharedPreferences.getBoolean(KEY_DARK_MODE, false));

        // Taille du texte
        String textSize = sharedPreferences.getString(KEY_TEXT_SIZE, "medium");
        switch (textSize) {
            case "small":
                smallRadioButton.setChecked(true);
                break;
            case "medium":
                mediumRadioButton.setChecked(true);
                break;
            case "large":
                largeRadioButton.setChecked(true);
                break;
        }
    }

    /**
     * Configure les écouteurs pour les interactions utilisateur
     */
    private void setupListeners() {
        // Bouton Enregistrer
        saveButton.setOnClickListener(v -> saveSettings());

        // Bouton Vider le cache
        clearCacheButton.setOnClickListener(v -> {
            // Simuler le nettoyage du cache
            Toast.makeText(getContext(), "Cache vidé avec succès", Toast.LENGTH_SHORT).show();
        });

        // Bouton Envoyer des commentaires
        sendFeedbackButton.setOnClickListener(v -> {
            // Afficher un dialogue pour envoyer des commentaires
            Toast.makeText(getContext(), "Fonctionnalité à implémenter", Toast.LENGTH_SHORT).show();
        });

        // Bouton Politique de confidentialité
        privacyPolicyButton.setOnClickListener(v -> {
            // Afficher la politique de confidentialité
            Toast.makeText(getContext(), "Fonctionnalité à implémenter", Toast.LENGTH_SHORT).show();
        });

        // Mode sombre
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Appliquer immédiatement le mode sombre
            applyDarkMode(isChecked);
        });
    }

    /**
     * Sauvegarde les paramètres dans les SharedPreferences
     */
    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Paramètres de notification
        editor.putBoolean(KEY_PUSH_NOTIFICATIONS, pushNotificationsSwitch.isChecked());
        editor.putBoolean(KEY_CONSUMPTION_ALERTS, consumptionAlertsSwitch.isChecked());
        editor.putBoolean(KEY_RESERVATION_REMINDERS, reservationRemindersSwitch.isChecked());

        // Paramètres d'affichage
        editor.putBoolean(KEY_DARK_MODE, darkModeSwitch.isChecked());

        // Taille du texte
        String textSize = "medium";
        int checkedRadioButtonId = textSizeRadioGroup.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rbSmall) {
            textSize = "small";
        } else if (checkedRadioButtonId == R.id.rbLarge) {
            textSize = "large";
        }
        editor.putString(KEY_TEXT_SIZE, textSize);

        // Appliquer les changements
        editor.apply();

        // Appliquer le mode sombre
        applyDarkMode(darkModeSwitch.isChecked());

        // Appliquer la taille du texte
        applyTextSize(textSize);

        Toast.makeText(getContext(), "Paramètres enregistrés", Toast.LENGTH_SHORT).show();
    }

    /**
     * Applique le mode sombre
     */
    private void applyDarkMode(boolean darkMode) {
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Applique la taille du texte
     */
    private void applyTextSize(String textSize) {
        // Dans une vraie application, on modifierait les ressources de style
        // Pour simplifier, on affiche juste un message
        Toast.makeText(getContext(), "Taille de texte '" + textSize + "' appliquée", Toast.LENGTH_SHORT).show();
    }
} 