package etu.leoh.powerhome.ui.habitat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.User;

/**
 * Adaptateur pour afficher les résidents dans un RecyclerView
 */
public class ResidentAdapter extends RecyclerView.Adapter<ResidentAdapter.ResidentViewHolder> {

    private final List<User> residentList;

    public ResidentAdapter(List<User> residentList) {
        this.residentList = residentList;
    }

    @NonNull
    @Override
    public ResidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resident, parent, false);
        return new ResidentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResidentViewHolder holder, int position) {
        User resident = residentList.get(position);
        holder.bind(resident);
    }

    @Override
    public int getItemCount() {
        return residentList.size();
    }

    /**
     * ViewHolder pour les éléments de la liste des résidents
     */
    static class ResidentViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView emailTextView;

        ResidentViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tvResidentName);
            emailTextView = itemView.findViewById(R.id.tvResidentEmail);
        }

        void bind(User resident) {
            String fullName = resident.getFirstName() + " " + resident.getLastName();
            nameTextView.setText(fullName);
            emailTextView.setText(resident.getEmail());
        }
    }
} 