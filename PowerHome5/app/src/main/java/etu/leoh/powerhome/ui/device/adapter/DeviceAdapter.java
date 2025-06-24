package etu.leoh.powerhome.ui.device.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import etu.leoh.powerhome.R;
import etu.leoh.powerhome.model.Device;

/**
 * Adaptateur pour afficher les appareils électroménagers dans un RecyclerView
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final List<Device> deviceList;
    private final DeviceInteractionListener listener;

    public DeviceAdapter(List<Device> deviceList, DeviceInteractionListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    /**
     * ViewHolder pour les éléments de la liste des appareils
     */
    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView typeTextView;
        private final TextView powerTextView;
        private final TextView statusTextView;
        private final ImageButton editButton;
        private final ImageButton deleteButton;
        private final ImageButton toggleButton;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tvDeviceName);
            typeTextView = itemView.findViewById(R.id.tvDeviceType);
            powerTextView = itemView.findViewById(R.id.tvDevicePower);
            statusTextView = itemView.findViewById(R.id.tvDeviceStatus);
            editButton = itemView.findViewById(R.id.btnEdit);
            deleteButton = itemView.findViewById(R.id.btnDelete);
            toggleButton = itemView.findViewById(R.id.btnToggle);
        }

        void bind(Device device) {
            nameTextView.setText(device.getName());
            typeTextView.setText(device.getType());
            powerTextView.setText(itemView.getContext().getString(R.string.power_watts, device.getPowerConsumption()));
            
            // Mettre à jour le statut
            if (device.isActive()) {
                statusTextView.setText(R.string.active);
                statusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                toggleButton.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                statusTextView.setText(R.string.inactive);
                statusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                toggleButton.setImageResource(android.R.drawable.ic_media_play);
            }
            
            // Configurer les écouteurs de clics
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceEdit(device);
                }
            });
            
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceDelete(device);
                }
            });
            
            toggleButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceStatusToggle(device);
                }
            });
        }
    }

    /**
     * Interface pour gérer les interactions avec les appareils
     */
    public interface DeviceInteractionListener {
        void onDeviceEdit(Device device);
        void onDeviceDelete(Device device);
        void onDeviceStatusToggle(Device device);
    }
} 