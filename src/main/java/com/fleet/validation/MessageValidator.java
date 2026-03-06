package com.fleet.validation;

import com.fleet.model.DroneState;
import com.fleet.model.Location;
import com.fleet.model.Orientation;
import com.fleet.model.Battery;

public class MessageValidator {
    public ValidationResult validate(DroneState state) {
        if (state == null) {
            return ValidationResult.failure("Message is null");
        }

        if (state.getVehicleId() <= 0) {
            return ValidationResult.failure("vehicleId is required and must be positive");
        }

        if (state.getSequenceNumber() < 0) {
            return ValidationResult.failure("sequenceNumber must be non-negative");
        }

        if (state.getLastUpdate() <= 0) {
            return ValidationResult.failure("lastUpdate is required (epoch time in seconds)");
        }

        if (state.getLocation() != null) {
            Location loc = state.getLocation();
            if (!isValidLatitude(loc.getLatitude())) {
                return ValidationResult.failure("latitude must be between -90 and 90");
            }
            if (!isValidLongitude(loc.getLongitude())) {
                return ValidationResult.failure("longitude must be between -180 and 180");
            }
            if (loc.getAltitude() != 0 && !isValidAltitude(loc.getAltitude())) {
                return ValidationResult.failure("altitude must be between -500 and 15000");
            }
        }

        if (state.getOrientation() != null) {
            Orientation ori = state.getOrientation();
            if (!isValidAngle(ori.getRoll())) {
                return ValidationResult.failure("roll must be between -180 and 180");
            }
            if (!isValidAngle(ori.getPitch())) {
                return ValidationResult.failure("pitch must be between -90 and 90");
            }
            if (!isValidAngle(ori.getYaw())) {
                return ValidationResult.failure("yaw must be between -180 and 180");
            }
        }

        if (state.getBattery() != null) {
            Battery bat = state.getBattery();
            if (bat.getBatteryCapacity() < 0) {
                return ValidationResult.failure("batteryCapacity must be non-negative");
            }
            if (bat.getBatteryPercentage() < 0 || bat.getBatteryPercentage() > 1) {
                return ValidationResult.failure("batteryPercentage must be between 0 and 1");
            }
        }

        if (state.getLinearSpeed() < 0) {
            return ValidationResult.failure("linearSpeed must be non-negative");
        }

        return ValidationResult.success();
    }

    private boolean isValidLatitude(double lat) {
        return lat >= -90 && lat <= 90;
    }

    private boolean isValidLongitude(double lon) {
        return lon >= -180 && lon <= 180;
    }

    private boolean isValidAltitude(double alt) {
        return alt >= -500 && alt <= 15000;
    }

    private boolean isValidAngle(double angle) {
        return angle >= -180 && angle <= 180;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
