package etu.leoh.powerhome.ui.reservation;

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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.kizitonwose.calendarview.CalendarView;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.model.CalendarMonth;
import com.kizitonwose.calendarview.model.DayOwner;
import com.kizitonwose.calendarview.ui.DayBinder;
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder;
import com.kizitonwose.calendarview.ui.ViewContainer;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.DeviceReservation;
import etu.leoh.powerhome.model.User;
import etu.leoh.powerhome.repository.DeviceReservationRepository;
import etu.leoh.powerhome.repository.UserRepository;
import etu.leoh.powerhome.ui.reservation.adapter.CalendarEventAdapter;
import etu.leoh.powerhome.ui.reservation.adapter.ReservationAdapter;
import etu.leoh.powerhome.util.ConsumptionCalculator;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

/**
 * Fragment affichant la liste des réservations d'appareils de l'utilisateur
 */
public class ReservationListFragment extends Fragment implements 
        ReservationAdapter.ReservationInteractionListener,
        CalendarEventAdapter.CalendarEventListener {

    private RecyclerView recyclerView;
    private RecyclerView recyclerViewCalendarEvents;
    private ReservationAdapter adapter;
    private CalendarEventAdapter calendarEventAdapter;
    private List<DeviceReservation> reservationList;
    private List<DeviceReservation> selectedDateReservations;
    private Map<LocalDate, List<DeviceReservation>> reservationsByDate;
    private ProgressBar progressBar;
    private FloatingActionButton addReservationButton;
    private CalendarView calendarView;
    private MaterialButtonToggleGroup viewToggleButton;
    private Button btnListView;
    private Button btnCalendarView;
    private ConstraintLayout calendarContainer;
    private LocalDate selectedDate;
    
    private DeviceReservationRepository reservationRepository;
    private UserRepository userRepository;
    private FirebaseAuthHelper authHelper;
    private ConsumptionCalculator consumptionCalculator;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reservation_list, container, false);
        
        // Initialiser les repositories et helpers
        reservationRepository = new DeviceReservationRepository();
        userRepository = new UserRepository();
        authHelper = new FirebaseAuthHelper();
        consumptionCalculator = new ConsumptionCalculator();
        
        // Initialiser les vues
        recyclerView = view.findViewById(R.id.recyclerViewReservations);
        recyclerViewCalendarEvents = view.findViewById(R.id.recyclerViewCalendarEvents);
        progressBar = view.findViewById(R.id.progressBar);
        addReservationButton = view.findViewById(R.id.fabAddReservation);
        calendarView = view.findViewById(R.id.calendarView);
        viewToggleButton = view.findViewById(R.id.viewToggleButton);
        btnListView = view.findViewById(R.id.btnListView);
        btnCalendarView = view.findViewById(R.id.btnCalendarView);
        calendarContainer = view.findViewById(R.id.calendarContainer);
        
        // Initialiser les listes
        reservationList = new ArrayList<>();
        selectedDateReservations = new ArrayList<>();
        reservationsByDate = new HashMap<>();
        
        // Date du jour sélectionnée par défaut
        selectedDate = LocalDate.now();
        
        // Configurer le RecyclerView des réservations
        adapter = new ReservationAdapter(reservationList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Configurer le RecyclerView des événements du calendrier
        calendarEventAdapter = new CalendarEventAdapter(selectedDateReservations, this);
        recyclerViewCalendarEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCalendarEvents.setAdapter(calendarEventAdapter);
        
        // Configurer le bouton d'ajout
        addReservationButton.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), NewReservationActivity.class));
        });
        
        // Configurer le toggle des vues
        viewToggleButton.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnListView) {
                    showListView();
                } else if (checkedId == R.id.btnCalendarView) {
                    showCalendarView();
                }
            }
        });
        
        // Par défaut, sélectionner la vue liste
        viewToggleButton.check(R.id.btnListView);
        
        // Configurer le calendrier
        setupCalendarView();
        
        // Charger l'utilisateur actuel puis ses réservations
        loadCurrentUser();
        
        // Ajout d'un bouton de rafraîchissement manuel
        addReservationButton.setOnLongClickListener(v -> {
            forceRefresh();
            return true;
        });
        
        return view;
    }

    private void setupCalendarView() {
        // Définir un formateur de mois
        DateTimeFormatter monthHeaderFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
        
        // Binder pour les jours
        calendarView.setDayBinder(new DayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                container.textView.setText(String.valueOf(day.getDate().getDayOfMonth()));
                
                // Style du jour
                if (day.getOwner() == DayOwner.THIS_MONTH) {
                    container.textView.setAlpha(1f);
                    
                    // Style du jour sélectionné
                    if (day.getDate().equals(selectedDate)) {
                        container.textView.setBackgroundResource(R.drawable.selected_day_background);
                        container.textView.setTextColor(getResources().getColor(android.R.color.white, null));
                    } else {
                        container.textView.setBackground(null);
                        container.textView.setTextColor(getResources().getColor(android.R.color.black, null));
                    }
                    
                    // Afficher l'indicateur si des réservations existent
                    List<DeviceReservation> dayReservations = reservationsByDate.get(day.getDate());
                    boolean hasReservations = dayReservations != null && !dayReservations.isEmpty();
                    container.indicator.setVisibility(hasReservations ? View.VISIBLE : View.INVISIBLE);
                } else {
                    // Jours des autres mois
                    container.textView.setAlpha(0.3f);
                    container.textView.setBackground(null);
                    container.textView.setTextColor(getResources().getColor(android.R.color.black, null));
                    container.indicator.setVisibility(View.INVISIBLE);
                }
                
                // Configurer le clic sur le jour
                container.view.setOnClickListener(v -> {
                    // Si on clique sur un jour d'un autre mois, on change de mois
                    if (day.getOwner() != DayOwner.THIS_MONTH) {
                        calendarView.smoothScrollToMonth(YearMonth.from(day.getDate()));
                    }
                    
                    // Mettre à jour le jour sélectionné
                    if (selectedDate != day.getDate()) {
                        LocalDate oldDate = selectedDate;
                        selectedDate = day.getDate();
                        
                        // Rafraîchir les jours concernés
                        calendarView.notifyDateChanged(oldDate);
                        calendarView.notifyDateChanged(day.getDate());
                        
                        // Mettre à jour la liste des événements
                        updateSelectedDateEvents();
                    }
                });
            }
        });
        
        // Binder pour les en-têtes de mois
        calendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<MonthViewContainer>() {
            @Override
            public MonthViewContainer create(View view) {
                return new MonthViewContainer(view);
            }

            @Override
            public void bind(MonthViewContainer container, CalendarMonth month) {
                container.textView.setText(monthHeaderFormatter.format(month.getYearMonth()));
            }
        });
        
        // Définir la plage du calendrier
        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = currentMonth.minusMonths(6);
        YearMonth lastMonth = currentMonth.plusMonths(12);
        
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        DayOfWeek firstDayOfWeek = weekFields.getFirstDayOfWeek();
        
        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);
    }
    
    private void updateSelectedDateEvents() {
        selectedDateReservations.clear();
        
        // Obtenir les réservations pour la date sélectionnée
        List<DeviceReservation> dayReservations = reservationsByDate.get(selectedDate);
        if (dayReservations != null) {
            selectedDateReservations.addAll(dayReservations);
        }
        
        calendarEventAdapter.notifyDataSetChanged();
    }
    
    private void organizeReservationsByDate() {
        reservationsByDate.clear();
        
        for (DeviceReservation reservation : reservationList) {
            // Convertir la date de début en LocalDate
            LocalDate startDate = reservation.getStartTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            // Stocker la réservation à la date correspondante
            reservationsByDate.computeIfAbsent(startDate, k -> new ArrayList<>()).add(reservation);
            
            // Si la réservation s'étend sur plusieurs jours
            LocalDate endDate = reservation.getEndTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            if (!startDate.equals(endDate)) {
                LocalDate currentDate = startDate.plusDays(1);
                while (!currentDate.isAfter(endDate)) {
                    reservationsByDate.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(reservation);
                    currentDate = currentDate.plusDays(1);
                }
            }
        }
        
        // Mettre à jour la vue calendrier
        calendarView.notifyCalendarChanged();
        
        // Mettre à jour les événements pour la date sélectionnée
        updateSelectedDateEvents();
    }

    private void showListView() {
        recyclerView.setVisibility(View.VISIBLE);
        calendarContainer.setVisibility(View.GONE);
    }

    private void showCalendarView() {
        recyclerView.setVisibility(View.GONE);
        calendarContainer.setVisibility(View.VISIBLE);
        
        // Si c'est la première fois qu'on affiche le calendrier, il faut organiser les réservations par date
        if (reservationsByDate.isEmpty() && !reservationList.isEmpty()) {
            organizeReservationsByDate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recharger les réservations quand on revient sur ce fragment
        if (authHelper.isUserLoggedIn()) {
            loadReservations();
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
                            loadReservations();
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
     * Charge la liste des réservations de l'utilisateur
     */
    private void loadReservations() {
        if (currentUser == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Récupérer les réservations de l'utilisateur
        reservationRepository.getReservationsByUserId(currentUser.getId())
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (task.isSuccessful()) {
                        reservationList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            DeviceReservation reservation = document.toObject(DeviceReservation.class);
                            reservationList.add(reservation);
                        }
                        adapter.notifyDataSetChanged();
                        
                        // Mettre à jour le calendrier
                        organizeReservationsByDate();
                        
                        // Afficher un message si la liste est vide
                        if (reservationList.isEmpty()) {
                            Toast.makeText(getContext(), 
                                    "Aucune réservation trouvée. Créez-en une!", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), 
                                "Erreur lors du chargement des réservations: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Marque une réservation comme terminée et attribue les éco-coins
     */
    private void completeReservation(DeviceReservation reservation) {
        // Vérifier si la réservation est déjà terminée
        if (reservation.isCompleted()) {
            Toast.makeText(getContext(), "Cette réservation est déjà terminée", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Afficher la barre de progression
        progressBar.setVisibility(View.VISIBLE);
        
        // Calculer le niveau de consommation actuel pour déterminer les éco-coins
        consumptionCalculator.calculateConsumptionLevel(new Date(), new ConsumptionCalculator.ConsumptionLevelCallback() {
            @Override
            public void onConsumptionLevelCalculated(double consumptionPercentage, DeviceReservation.ConsumptionLevel level) {
                // Calculer les éco-coins gagnés
                int ecoCoinsEarned = reservation.calculateEcoCoins(level);
                
                // Mettre à jour la réservation
                reservation.setCompleted(true);
                reservation.setEcoCoinsEarned(ecoCoinsEarned);
                
                // Enregistrer les modifications
                reservationRepository.updateReservation(reservation)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Mettre à jour les éco-coins de l'utilisateur
                                int newEcoCoins = currentUser.getEcoCoins() + ecoCoinsEarned;
                                currentUser.setEcoCoins(newEcoCoins);
                                
                                userRepository.updateEcoCoins(currentUser.getId(), newEcoCoins)
                                        .addOnCompleteListener(updateTask -> {
                                            progressBar.setVisibility(View.GONE);
                                            
                                            if (updateTask.isSuccessful()) {
                                                adapter.notifyDataSetChanged();
                                                calendarEventAdapter.notifyDataSetChanged();
                                                calendarView.notifyCalendarChanged();
                                                
                                                String message = ecoCoinsEarned >= 0 ? 
                                                        "Réservation terminée ! Vous avez gagné " + ecoCoinsEarned + " éco-coins." :
                                                        "Réservation terminée ! Vous avez perdu " + Math.abs(ecoCoinsEarned) + " éco-coins.";
                                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(getContext(), 
                                                        "Erreur lors de la mise à jour des éco-coins: " + updateTask.getException().getMessage(), 
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            } else {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), 
                                        "Erreur lors de la mise à jour de la réservation: " + task.getException().getMessage(), 
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Supprime une réservation après confirmation
     */
    private void cancelReservation(DeviceReservation reservation) {
        // Vérifier si la réservation est déjà terminée
        if (reservation.isCompleted()) {
            Toast.makeText(getContext(), "Impossible d'annuler une réservation terminée", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Demander confirmation
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Annuler la réservation")
                .setMessage("Êtes-vous sûr de vouloir annuler cette réservation ?")
                .setPositiveButton("Annuler la réservation", (dialog, which) -> {
                    // Afficher la progression
                    progressBar.setVisibility(View.VISIBLE);
                    
                    // Supprimer la réservation
                    reservationRepository.deleteReservation(reservation.getId())
                            .addOnCompleteListener(task -> {
                                progressBar.setVisibility(View.GONE);
                                
                                if (task.isSuccessful()) {
                                    // Supprimer de la liste et rafraîchir
                                    reservationList.remove(reservation);
                                    adapter.notifyDataSetChanged();
                                    
                                    // Mettre à jour le calendrier
                                    organizeReservationsByDate();
                                    calendarView.notifyCalendarChanged();
                                    
                                    Toast.makeText(getContext(), "Réservation annulée avec succès", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), 
                                            "Erreur lors de l'annulation de la réservation: " + task.getException().getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
    
    // Classes internes pour les vues du calendrier
    
    // Conteneur pour les jours du calendrier
    public class DayViewContainer extends ViewContainer {
        public final TextView textView;
        public final View indicator;
        public final View view;

        public DayViewContainer(View view) {
            super(view);
            this.view = view;
            this.textView = view.findViewById(R.id.calendarDayText);
            this.indicator = view.findViewById(R.id.eventIndicator);
        }
    }
    
    // Conteneur pour l'en-tête du mois
    public class MonthViewContainer extends ViewContainer {
        public final TextView textView;

        public MonthViewContainer(View view) {
            super(view);
            this.textView = view.findViewById(R.id.headerTextView);
        }
    }

    // Implémentation des interfaces des adaptateurs
    
    @Override
    public void onReservationComplete(DeviceReservation reservation) {
        completeReservation(reservation);
    }

    @Override
    public void onReservationCancel(DeviceReservation reservation) {
        cancelReservation(reservation);
    }

    @Override
    public void onEventComplete(DeviceReservation event) {
        completeReservation(event);
    }

    @Override
    public void onEventCancel(DeviceReservation event) {
        cancelReservation(event);
    }

    /**
     * Méthode pour forcer le rafraîchissement des données
     */
    private void forceRefresh() {
        Toast.makeText(getContext(), "Actualisation des réservations...", Toast.LENGTH_SHORT).show();
        if (authHelper.isUserLoggedIn()) {
            // Recharger l'utilisateur complet puis les réservations
            loadCurrentUser();
        } else {
            Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
        }
    }
} 