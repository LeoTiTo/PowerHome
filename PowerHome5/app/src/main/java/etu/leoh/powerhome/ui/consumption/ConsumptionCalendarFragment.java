package etu.leoh.powerhome.ui.consumption;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.util.PowerUsageSimulator;

/**
 * Fragment qui affiche un calendrier coloré des niveaux de consommation
 */
public class ConsumptionCalendarFragment extends Fragment {

    private PowerUsageSimulator powerUsageSimulator;
    
    private GridLayout calendarGrid;
    private TextView tvMonthYear;
    private ImageButton btnPreviousMonth;
    private ImageButton btnNextMonth;
    private TextView tvSelectedDate;
    private LinearLayout selectedDateDetails;
    private TextView tvConsumptionValue;
    private ProgressBar progressConsumption;
    private TextView tvEcoTips;
    
    private Calendar currentCalendar;
    private Map<Integer, View> dayViewMap;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat dayFormat;
    private SimpleDateFormat fullDateFormat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        powerUsageSimulator = new PowerUsageSimulator();
        currentCalendar = Calendar.getInstance();
        dayViewMap = new HashMap<>();
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.FRANCE);
        dayFormat = new SimpleDateFormat("d", Locale.FRANCE);
        fullDateFormat = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_consumption_calendar, container, false);
        
        // Initialiser les vues
        calendarGrid = view.findViewById(R.id.calendarGrid);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        btnPreviousMonth = view.findViewById(R.id.btnPreviousMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        selectedDateDetails = view.findViewById(R.id.selectedDateDetails);
        tvConsumptionValue = view.findViewById(R.id.tvConsumptionValue);
        progressConsumption = view.findViewById(R.id.progressConsumption);
        tvEcoTips = view.findViewById(R.id.tvEcoTips);
        
        // Configurer les écouteurs
        btnPreviousMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarView();
        });
        
        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarView();
        });
        
        // Afficher le calendrier initial
        updateCalendarView();
        
        return view;
    }
    
    /**
     * Met à jour le calendrier pour le mois actuel
     */
    private void updateCalendarView() {
        // Mettre à jour le titre du mois et de l'année
        tvMonthYear.setText(monthYearFormat.format(currentCalendar.getTime()));
        
        // Supprimer les cellules du mois précédent
        for (int i = 7; i < calendarGrid.getChildCount(); i++) {
            calendarGrid.removeViewAt(7);
            i--;
        }
        
        // Réinitialiser la map des vues de jours
        dayViewMap.clear();
        
        // Obtenir le premier jour du mois et le nombre de jours
        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        // Ajuster pour que lundi soit le premier jour (2-7, 1 -> 1-6, 0)
        firstDayOfWeek = firstDayOfWeek == Calendar.SUNDAY ? 6 : firstDayOfWeek - 2;
        
        int numDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Ajouter les cellules vides pour les jours avant le début du mois
        for (int i = 0; i < firstDayOfWeek; i++) {
            View emptyView = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(i, 1f);
            calendarGrid.addView(emptyView, params);
        }
        
        // Ajouter les cellules pour chaque jour du mois
        for (int day = 1; day <= numDaysInMonth; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            
            // Créer la vue pour le jour
            View dayView = createDayView(day, calendar.getTime());
            
            // Calculer la position dans la grille
            int position = firstDayOfWeek + day - 1;
            int row = position / 7 + 1; // +1 pour la ligne d'en-tête
            int col = position % 7;
            
            // Définir les paramètres de mise en page
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(col, 1f);
            params.rowSpec = GridLayout.spec(row);
            params.setMargins(2, 2, 2, 2);
            
            // Ajouter la vue à la grille
            calendarGrid.addView(dayView, params);
            
            // Stocker la référence dans la map
            dayViewMap.put(day, dayView);
        }
    }
    
    /**
     * Crée une vue pour un jour du calendrier
     * @param day Numéro du jour
     * @param date Date complète
     * @return Vue pour ce jour
     */
    private View createDayView(final int day, final Date date) {
        // Créer un layout pour le jour
        LinearLayout dayLayout = new LinearLayout(getContext());
        dayLayout.setOrientation(LinearLayout.VERTICAL);
        dayLayout.setPadding(8, 8, 8, 8);
        
        // Simuler le niveau de consommation pour ce jour
        double consumptionPercentage = powerUsageSimulator.simulateConsumptionPercentage(date);
        
        // Définir la couleur de fond en fonction du niveau de consommation
        if (consumptionPercentage < 30) {
            dayLayout.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.holo_green_light));
        } else if (consumptionPercentage < 70) {
            dayLayout.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.holo_orange_light));
        } else {
            dayLayout.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_light));
        }
        
        // Ajouter le numéro du jour
        TextView dayNumber = new TextView(getContext());
        dayNumber.setText(dayFormat.format(date));
        dayNumber.setTextSize(16);
        dayNumber.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
        dayLayout.addView(dayNumber);
        
        // Gérer le clic sur le jour
        dayLayout.setOnClickListener(v -> showDayDetails(date, consumptionPercentage));
        
        return dayLayout;
    }
    
    /**
     * Affiche les détails de consommation pour un jour spécifique
     * @param date Date sélectionnée
     * @param consumptionPercentage Pourcentage de consommation
     */
    private void showDayDetails(Date date, double consumptionPercentage) {
        // Mettre à jour le texte de la date sélectionnée
        tvSelectedDate.setText(fullDateFormat.format(date));
        
        // Mettre à jour les détails de consommation
        tvConsumptionValue.setText(String.format(Locale.FRANCE, 
                "Consommation: %.1f%% de la capacité maximale", consumptionPercentage));
        
        // Mettre à jour la barre de progression
        progressConsumption.setProgress((int) consumptionPercentage);
        
        // Définir un conseil écologique en fonction du niveau de consommation
        if (consumptionPercentage < 30) {
            tvEcoTips.setText("Conseil éco: C'est le moment idéal pour utiliser les appareils les plus énergivores !");
            progressConsumption.setProgressTintList(ContextCompat.getColorStateList(getContext(), android.R.color.holo_green_light));
        } else if (consumptionPercentage < 70) {
            tvEcoTips.setText("Conseil éco: Si possible, reportez l'utilisation des gros appareils aux heures creuses.");
            progressConsumption.setProgressTintList(ContextCompat.getColorStateList(getContext(), android.R.color.holo_orange_light));
        } else {
            tvEcoTips.setText("Conseil éco: Évitez d'utiliser plusieurs appareils gourmands en énergie simultanément.");
            progressConsumption.setProgressTintList(ContextCompat.getColorStateList(getContext(), android.R.color.holo_red_light));
        }
        
        // Afficher les détails
        selectedDateDetails.setVisibility(View.VISIBLE);
    }
} 