package etu.leoh.powerhome.ui.habitat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.Habitat;
import etu.leoh.powerhome.repository.DeviceRepository;
import etu.leoh.powerhome.repository.HabitatRepository;
import etu.leoh.powerhome.repository.UserRepository;
import etu.leoh.powerhome.ui.habitat.adapter.HabitatAdapter;
import etu.leoh.powerhome.util.ConsumptionCalculator;
import etu.leoh.powerhome.util.FirebaseAuthHelper;

/**
 * Fragment affichant la liste des habitats de la résidence
 */
public class AllHabitatsFragment extends Fragment implements HabitatAdapter.OnHabitatClickListener, HabitatAdapter.HabitatInfoProvider {

    private RecyclerView habitatsRecyclerView;
    private TextView descriptionTextView;
    private ProgressBar progressBar;
    
    private HabitatRepository habitatRepository;
    private UserRepository userRepository;
    private DeviceRepository deviceRepository;
    private ConsumptionCalculator consumptionCalculator;
    private FirebaseAuthHelper authHelper;
    
    private HabitatAdapter adapter;
    private List<Habitat> habitatList;
    
    // Cache pour stocker des informations calculées sur les habitats
    private Map<String, Integer> residentsCountCache = new HashMap<>();
    private Map<String, Integer> consumptionCache = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_habitats, container, false);
        
        // Initialiser les repositories et helpers
        habitatRepository = new HabitatRepository();
        userRepository = new UserRepository();
        deviceRepository = new DeviceRepository();
        consumptionCalculator = new ConsumptionCalculator();
        authHelper = new FirebaseAuthHelper();
        
        // Initialiser les vues
        habitatsRecyclerView = view.findViewById(R.id.recyclerViewHabitats);
        descriptionTextView = view.findViewById(R.id.tvDescription);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Mettre à jour le texte de description
        descriptionTextView.setText(R.string.habitats_description);
        
        // Configurer la liste des habitats
        habitatList = new ArrayList<>();
        adapter = new HabitatAdapter(habitatList, this, this);
        habitatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        habitatsRecyclerView.setAdapter(adapter);
        
        // Charger les données
        loadHabitats();
        
        return view;
    }

    /**
     * Charge la liste des habitats de la résidence
     */
    private void loadHabitats() {
        progressBar.setVisibility(View.VISIBLE);
        
        // On assume une résidence par défaut (dans une vraie application, on récupérerait l'ID de la résidence de l'utilisateur)
        String defaultResidenceId = "residence1";
        
        habitatRepository.getHabitatsByResidence(defaultResidenceId)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (task.isSuccessful()) {
                        habitatList.clear();
                        residentsCountCache.clear();
                        consumptionCache.clear();
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Habitat habitat = document.toObject(Habitat.class);
                            habitatList.add(habitat);
                            
                            // Précharger le nombre de résidents
                            if (habitat.getResidentIds() != null) {
                                residentsCountCache.put(habitat.getId(), habitat.getResidentIds().size());
                            } else {
                                residentsCountCache.put(habitat.getId(), 0);
                            }
                            
                            // Précharger les consommations
                            loadHabitatConsumption(habitat.getId());
                        }
                        
                        adapter.notifyDataSetChanged();
                        
                        if (habitatList.isEmpty()) {
                            Toast.makeText(getContext(), "Aucun habitat trouvé dans la résidence", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), 
                                "Erreur lors du chargement des habitats: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    /**
     * Charge la consommation d'un habitat
     * @param habitatId ID de l'habitat
     */
    private void loadHabitatConsumption(String habitatId) {
        consumptionCalculator.calculateHabitatConsumption(habitatId, new Date(), new ConsumptionCalculator.ConsumptionCallback() {
            @Override
            public void onConsumptionCalculated(int consumptionWatts) {
                consumptionCache.put(habitatId, consumptionWatts);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                // En cas d'erreur, mettre une valeur par défaut
                consumptionCache.put(habitatId, 0);
            }
        });
    }

    @Override
    public void onHabitatClick(Habitat habitat) {
        // Ici, on pourrait ouvrir un dialogue de détails ou naviguer vers un fragment de détails d'habitat
        Toast.makeText(getContext(), "Habitat sélectionné: " + habitat.getName(), Toast.LENGTH_SHORT).show();
        
        // Exemple: afficher plus d'informations sur l'habitat
        String message = String.format("Habitat: %s\nAppartement n°%s\nRésidents: %d\nConsommation: %d W",
                habitat.getName(),
                habitat.getApartmentNumber(),
                getResidentsCount(habitat.getId()),
                getConsumptionWatts(habitat.getId()));
        
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public int getResidentsCount(String habitatId) {
        return residentsCountCache.getOrDefault(habitatId, 0);
    }

    @Override
    public int getConsumptionWatts(String habitatId) {
        return consumptionCache.getOrDefault(habitatId, 0);
    }
} 