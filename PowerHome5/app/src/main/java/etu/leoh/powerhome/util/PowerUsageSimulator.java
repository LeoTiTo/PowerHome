package etu.leoh.powerhome.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import etu.leoh.powerhome.model.DeviceReservation;

/**
 * Classe utilitaire pour simuler les niveaux de consommation électrique
 * Cette classe sera à remplacer par des données réelles de consommation
 */
public class PowerUsageSimulator {
    
    private final Random random = new Random();
    
    /**
     * Simule le niveau de consommation pour une date et une consommation d'appareil données
     * @param date Date et heure pour la simulation
     * @param devicePower Puissance de l'appareil en watts
     * @return Niveau de consommation estimé
     */
    public DeviceReservation.ConsumptionLevel simulateConsumptionLevel(Date date, int devicePower) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // Heures de pointe en semaine: 7h-9h et 18h-20h
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
        boolean isMorningPeak = (hourOfDay >= 7 && hourOfDay < 9);
        boolean isEveningPeak = (hourOfDay >= 18 && hourOfDay < 20);
        
        // Base de consommation (pourcentage de la capacité maximale)
        double baseConsumption;
        
        if (isWeekend) {
            // Weekend a une consommation plus répartie
            if (hourOfDay >= 10 && hourOfDay < 22) {
                baseConsumption = 50.0 + random.nextDouble() * 20.0; // 50-70%
            } else {
                baseConsumption = 20.0 + random.nextDouble() * 20.0; // 20-40%
            }
        } else {
            // Jours de semaine
            if (isMorningPeak || isEveningPeak) {
                baseConsumption = 70.0 + random.nextDouble() * 30.0; // 70-100% (heures de pointe)
            } else if ((hourOfDay >= 9 && hourOfDay < 18) || (hourOfDay >= 20 && hourOfDay < 22)) {
                baseConsumption = 40.0 + random.nextDouble() * 30.0; // 40-70% (heures actives)
            } else {
                baseConsumption = 10.0 + random.nextDouble() * 20.0; // 10-30% (heures creuses)
            }
        }
        
        // Ajustement en fonction de la puissance de l'appareil
        // Les appareils très puissants augmentent le niveau de consommation
        if (devicePower > 2000) { // Appareils très énergivores
            baseConsumption += 10.0;
        } else if (devicePower < 500) { // Appareils peu énergivores
            baseConsumption -= 10.0;
        }
        
        // Limiter entre 0 et 100%
        baseConsumption = Math.max(0.0, Math.min(100.0, baseConsumption));
        
        // Déterminer le niveau de consommation
        if (baseConsumption < 30.0) {
            return DeviceReservation.ConsumptionLevel.LOW;
        } else if (baseConsumption < 70.0) {
            return DeviceReservation.ConsumptionLevel.MEDIUM;
        } else {
            return DeviceReservation.ConsumptionLevel.HIGH;
        }
    }
    
    /**
     * Simule le pourcentage de consommation pour une date donnée
     * @param date Date pour la simulation
     * @return Pourcentage de consommation (0-100)
     */
    public double simulateConsumptionPercentage(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // Heures de pointe en semaine: 7h-9h et 18h-20h
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
        boolean isMorningPeak = (hourOfDay >= 7 && hourOfDay < 9);
        boolean isEveningPeak = (hourOfDay >= 18 && hourOfDay < 20);
        
        double baseConsumption;
        
        if (isWeekend) {
            if (hourOfDay >= 10 && hourOfDay < 22) {
                baseConsumption = 50.0 + random.nextDouble() * 20.0;
            } else {
                baseConsumption = 20.0 + random.nextDouble() * 20.0;
            }
        } else {
            if (isMorningPeak || isEveningPeak) {
                baseConsumption = 70.0 + random.nextDouble() * 30.0;
            } else if ((hourOfDay >= 9 && hourOfDay < 18) || (hourOfDay >= 20 && hourOfDay < 22)) {
                baseConsumption = 40.0 + random.nextDouble() * 30.0;
            } else {
                baseConsumption = 10.0 + random.nextDouble() * 20.0;
            }
        }
        
        // Ajouter un peu de variation aléatoire
        baseConsumption += (random.nextDouble() * 10.0) - 5.0;
        
        // Limiter entre 0 et 100%
        return Math.max(0.0, Math.min(100.0, baseConsumption));
    }
} 