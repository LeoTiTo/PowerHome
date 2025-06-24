package etu.leoh.powerhome.ui.habitat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.Habitat;

/**
 * Adaptateur pour afficher les habitats dans un RecyclerView
 */
public class HabitatAdapter extends RecyclerView.Adapter<HabitatAdapter.HabitatViewHolder> {

    private final List<Habitat> habitatList;
    private final OnHabitatClickListener listener;
    private final HabitatInfoProvider infoProvider;

    /**
     * Interface pour écouter les clics sur les habitats
     */
    public interface OnHabitatClickListener {
        void onHabitatClick(Habitat habitat);
    }

    /**
     * Interface permettant de fournir des informations supplémentaires sur les habitats
     */
    public interface HabitatInfoProvider {
        int getResidentsCount(String habitatId);
        int getConsumptionWatts(String habitatId);
    }

    public HabitatAdapter(List<Habitat> habitatList, OnHabitatClickListener listener, HabitatInfoProvider infoProvider) {
        this.habitatList = habitatList;
        this.listener = listener;
        this.infoProvider = infoProvider;
    }

    @NonNull
    @Override
    public HabitatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habitat, parent, false);
        return new HabitatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitatViewHolder holder, int position) {
        Habitat habitat = habitatList.get(position);
        holder.bind(habitat, listener, infoProvider);
    }

    @Override
    public int getItemCount() {
        return habitatList.size();
    }

    /**
     * ViewHolder pour les éléments de la liste des habitats
     */
    static class HabitatViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView apartmentNumberTextView;
        private final TextView residentsCountTextView;
        private final TextView consumptionTextView;

        HabitatViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tvHabitatName);
            apartmentNumberTextView = itemView.findViewById(R.id.tvApartmentNumber);
            residentsCountTextView = itemView.findViewById(R.id.tvResidentsCount);
            consumptionTextView = itemView.findViewById(R.id.tvConsumption);
        }

        void bind(Habitat habitat, OnHabitatClickListener listener, HabitatInfoProvider infoProvider) {
            // Définir le nom et le numéro d'appartement
            nameTextView.setText(habitat.getName());
            String apartmentText = "Appartement n°" + habitat.getApartmentNumber();
            apartmentNumberTextView.setText(apartmentText);
            
            // Obtenir et afficher le nombre de résidents
            int residentsCount = infoProvider.getResidentsCount(habitat.getId());
            String residentsText = residentsCount + " résident(s)";
            residentsCountTextView.setText(residentsText);
            
            // Obtenir et afficher la consommation
            int consumptionWatts = infoProvider.getConsumptionWatts(habitat.getId());
            String consumptionText = consumptionWatts + " W";
            consumptionTextView.setText(consumptionText);
            
            // Configurer le clic sur l'élément
            itemView.setOnClickListener(v -> listener.onHabitatClick(habitat));
        }
    }
} 