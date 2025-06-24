package etu.leoh.powerhome.ui.reservation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.DeviceReservation;
import etu.leoh.powerhome.repository.DeviceRepository;

/**
 * Adaptateur pour afficher les événements du calendrier
 */
public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {

    private final List<DeviceReservation> events;
    private final CalendarEventListener listener;
    private final DeviceRepository deviceRepository;
    private final SimpleDateFormat timeFormat;

    /**
     * Interface pour les interactions avec les événements du calendrier
     */
    public interface CalendarEventListener {
        void onEventComplete(DeviceReservation event);
        void onEventCancel(DeviceReservation event);
    }

    public CalendarEventAdapter(List<DeviceReservation> events, CalendarEventListener listener) {
        this.events = events;
        this.listener = listener;
        this.deviceRepository = new DeviceRepository();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        DeviceReservation event = events.get(position);
        
        // Obtenir le nom de l'appareil
        deviceRepository.getDeviceById(event.getDeviceId())
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String deviceName = document.getString("name");
                        holder.deviceNameTextView.setText(deviceName);
                    } else {
                        holder.deviceNameTextView.setText("Appareil inconnu");
                    }
                })
                .addOnFailureListener(e -> 
                        holder.deviceNameTextView.setText("Appareil inconnu"));

        // Configurer l'heure
        String startTime = timeFormat.format(event.getStartTime());
        String endTime = timeFormat.format(event.getEndTime());
        holder.eventTimeTextView.setText(String.format("%s - %s", startTime, endTime));

        // Configurer le statut
        if (event.isCompleted()) {
            holder.statusTextView.setText("Terminé");
            holder.statusTextView.setBackgroundResource(R.drawable.bg_status_completed);
            
            // Afficher les éco-coins gagnés
            int ecoCoins = event.getEcoCoinsEarned();
            String ecoCoinsText = ecoCoins >= 0 ? 
                    "+" + ecoCoins + " éco-coins" : 
                    ecoCoins + " éco-coins";
            holder.ecoCoinsTextView.setText(ecoCoinsText);
            holder.ecoCoinsTextView.setVisibility(View.VISIBLE);
        } else {
            holder.statusTextView.setText("En attente");
            holder.statusTextView.setBackgroundResource(R.drawable.bg_status_pending);
            holder.ecoCoinsTextView.setVisibility(View.GONE);
        }

        // Configurer les interactions
        holder.itemView.setOnClickListener(v -> {
            if (!event.isCompleted()) {
                if (System.currentTimeMillis() >= event.getEndTime().getTime()) {
                    listener.onEventComplete(event);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!event.isCompleted()) {
                listener.onEventCancel(event);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView deviceNameTextView;
        TextView eventTimeTextView;
        TextView statusTextView;
        TextView ecoCoinsTextView;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.tvDeviceName);
            eventTimeTextView = itemView.findViewById(R.id.tvEventTime);
            statusTextView = itemView.findViewById(R.id.tvStatus);
            ecoCoinsTextView = itemView.findViewById(R.id.tvEcoCoins);
        }
    }
} 