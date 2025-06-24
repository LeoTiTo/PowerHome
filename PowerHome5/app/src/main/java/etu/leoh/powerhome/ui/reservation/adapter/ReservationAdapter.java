package etu.leoh.powerhome.ui.reservation.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.DeviceReservation;
import etu.leoh.powerhome.repository.DeviceRepository;

/**
 * Adaptateur pour afficher les réservations dans un RecyclerView
 */
public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {

    private final List<DeviceReservation> reservationList;
    private final ReservationInteractionListener listener;
    private final DeviceRepository deviceRepository;
    private final DateFormat dateFormat;

    public ReservationAdapter(List<DeviceReservation> reservationList, ReservationInteractionListener listener) {
        this.reservationList = reservationList;
        this.listener = listener;
        this.deviceRepository = new DeviceRepository();
        this.dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        DeviceReservation reservation = reservationList.get(position);
        holder.bind(reservation);
    }

    @Override
    public int getItemCount() {
        return reservationList.size();
    }

    /**
     * ViewHolder pour les éléments de la liste des réservations
     */
    class ReservationViewHolder extends RecyclerView.ViewHolder {
        private final TextView deviceNameTextView;
        private final TextView timeRangeTextView;
        private final TextView statusTextView;
        private final TextView ecoCoinsTextView;
        private final Button completeButton;
        private final Button cancelButton;

        ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.tvDeviceName);
            timeRangeTextView = itemView.findViewById(R.id.tvTimeRange);
            statusTextView = itemView.findViewById(R.id.tvStatus);
            ecoCoinsTextView = itemView.findViewById(R.id.tvEcoCoins);
            completeButton = itemView.findViewById(R.id.btnComplete);
            cancelButton = itemView.findViewById(R.id.btnCancel);
        }

        void bind(DeviceReservation reservation) {
            // Récupérer les informations sur l'appareil
            deviceRepository.getDeviceById(reservation.getDeviceId())
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String deviceName = documentSnapshot.getString("name");
                            if (deviceName != null) {
                                deviceNameTextView.setText(deviceName);
                            } else {
                                deviceNameTextView.setText("Appareil #" + reservation.getDeviceId());
                            }
                        } else {
                            deviceNameTextView.setText("Appareil inconnu");
                        }
                    })
                    .addOnFailureListener(e -> deviceNameTextView.setText("Appareil #" + reservation.getDeviceId()));
            
            // Formater l'intervalle de temps
            String startTime = dateFormat.format(reservation.getStartTime());
            String endTime = dateFormat.format(reservation.getEndTime());
            timeRangeTextView.setText(itemView.getContext().getString(R.string.time_range, startTime, endTime));
            
            // Afficher le statut
            if (reservation.isCompleted()) {
                statusTextView.setText(R.string.completed);
                statusTextView.setTextColor(Color.GREEN);
                completeButton.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                
                // Afficher les éco-coins gagnés
                if (reservation.getEcoCoinsEarned() > 0) {
                    ecoCoinsTextView.setTextColor(Color.GREEN);
                    ecoCoinsTextView.setText("+" + reservation.getEcoCoinsEarned() + " éco-coins");
                } else if (reservation.getEcoCoinsEarned() < 0) {
                    ecoCoinsTextView.setTextColor(Color.RED);
                    ecoCoinsTextView.setText(reservation.getEcoCoinsEarned() + " éco-coins");
                } else {
                    ecoCoinsTextView.setTextColor(Color.GRAY);
                    ecoCoinsTextView.setText("0 éco-coin");
                }
                ecoCoinsTextView.setVisibility(View.VISIBLE);
            } else {
                statusTextView.setText(R.string.pending);
                statusTextView.setTextColor(Color.GRAY);
                completeButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                ecoCoinsTextView.setVisibility(View.GONE);
            }
            
            // Configurer les écouteurs de boutons
            completeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReservationComplete(reservation);
                }
            });
            
            cancelButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReservationCancel(reservation);
                }
            });
        }
    }

    /**
     * Interface pour gérer les interactions avec les réservations
     */
    public interface ReservationInteractionListener {
        void onReservationComplete(DeviceReservation reservation);
        void onReservationCancel(DeviceReservation reservation);
    }
} 