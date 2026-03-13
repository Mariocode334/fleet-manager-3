package com.fleet.mqtt;

import com.fleet.database.DroneRepository;
import com.fleet.database.LogRepository;
import com.fleet.model.DroneState;
import com.fleet.model.FleetLog;
import com.fleet.util.AppLogger;
import com.fleet.util.GrafanaDashboardUpdater;
import com.fleet.util.JsonParser;
import com.fleet.validation.MessageValidator;
import com.fleet.validation.SequenceTracker;

public class MessageHandler {
    private final JsonParser jsonParser;
    private final MessageValidator validator;
    private final SequenceTracker sequenceTracker;
    private final DroneRepository droneRepository;
    private final LogRepository logRepository;
    private final AppLogger logger;
    private final GrafanaDashboardUpdater grafanaUpdater;

    public MessageHandler() {
        this.jsonParser = JsonParser.getInstance();
        this.validator = new MessageValidator();
        this.sequenceTracker = SequenceTracker.getInstance();
        this.droneRepository = DroneRepository.getInstance();
        this.logRepository = LogRepository.getInstance();
        this.logger = AppLogger.getInstance();
        this.grafanaUpdater = GrafanaDashboardUpdater.getInstance();
    }

    public void handleMessage(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            logger.error("UNKNOWN", "Received empty message");
            return;
        }

        if (!jsonParser.isValidJson(payload)) {
            logger.error("UNKNOWN", "Invalid JSON format");
            return;
        }

        DroneState state;
        try {
            state = jsonParser.parseMessage(payload);
        } catch (Exception e) {
            logger.error("UNKNOWN", "Failed to parse message: " + e.getMessage());
            return;
        }

        String vehicleIdStr = String.valueOf(state.getVehicleId());
        
        // Verificar si es la primera vez que vemos este vehículo
        boolean isFirstMessage = !droneRepository.vehicleHasMessages(state.getVehicleId());

        // Verificar que el vehicle_id existe en la base de datos
        if (!droneRepository.vehicleExists(state.getVehicleId())) {
            logger.error(vehicleIdStr, "Rejected: vehicle_id " + state.getVehicleId() + " not found in database");
            
            FleetLog fleetLog = new FleetLog(
                    state.getVehicleId(),
                    "ERROR",
                    "Rejected: vehicle_id not registered in database",
                    state.getSequenceNumber(),
                    state.getSequenceNumber()
            );
            logRepository.saveLog(fleetLog);
            return;
        }

        MessageValidator.ValidationResult validationResult = validator.validate(state);
        if (!validationResult.isValid()) {
            logger.warning(vehicleIdStr, "Validation failed: " + validationResult.getErrorMessage());
            return;
        }

        SequenceTracker.SequenceCheckResult seqResult = sequenceTracker.checkSequence(
                state.getVehicleId(), state.getSequenceNumber());

        if (seqResult.hasPacketLoss()) {
            String logMessage = String.format("Paquetes perdidos detectados. Perdidos: %d", 
                    seqResult.getLostPackets());
            logger.warning(vehicleIdStr, logMessage);

            FleetLog fleetLog = new FleetLog(
                    state.getVehicleId(),
                    "WARNING",
                    logMessage,
                    seqResult.getExpectedSequence(),
                    seqResult.getReceivedSequence()
            );
            logRepository.saveLog(fleetLog);

            droneRepository.updatePacketLossTracking(
                    state.getVehicleId(),
                    seqResult.getExpectedSequence() - 1,
                    seqResult.getReceivedSequence(),
                    seqResult.getLostPackets()
            );
        }

        boolean saved = droneRepository.saveDroneState(state);
        if (!saved) {
            logger.warning(vehicleIdStr, "Failed to save state (possible duplicate)");
        } else {
            // Si es el primer mensaje de este vehículo, actualizar dashboard de Grafana
            if (isFirstMessage) {
                logger.info(vehicleIdStr, "Primer mensaje recibido, actualizando dashboard de Grafana");
                grafanaUpdater.updateDashboard();
            }
        }
    }
}
