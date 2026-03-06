package com.fleet.validation;

import com.fleet.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class SequenceTracker {
    private static SequenceTracker instance;
    private final ConcurrentHashMap<Integer, SequenceInfo> sequences;

    private SequenceTracker() {
        this.sequences = new ConcurrentHashMap<>();
        loadSequencesFromDatabase();
    }

    private void loadSequencesFromDatabase() {
        String sql = "SELECT vehicle_id, MAX(sequence_number) as max_sequence FROM drone_states GROUP BY vehicle_id";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int vehicleId = rs.getInt("vehicle_id");
                int maxSequence = rs.getInt("max_sequence");
                sequences.put(vehicleId, new SequenceInfo(maxSequence, maxSequence));
                System.out.println("Loaded sequence for vehicle " + vehicleId + ": " + maxSequence);
            }
        } catch (SQLException e) {
            System.err.println("Failed to load sequences from database: " + e.getMessage());
        }
    }

    public static synchronized SequenceTracker getInstance() {
        if (instance == null) {
            instance = new SequenceTracker();
        }
        return instance;
    }

    public SequenceCheckResult checkSequence(int vehicleId, int currentSequence) {
        SequenceInfo info = sequences.get(vehicleId);
        
        if (info == null) {
            sequences.put(vehicleId, new SequenceInfo(currentSequence, currentSequence));
            return SequenceCheckResult.sequential(currentSequence);
        }

        int lastSequence = info.getLastSequence();
        
        if (currentSequence == lastSequence + 1) {
            info.updateLastSequence(currentSequence);
            return SequenceCheckResult.sequential(currentSequence);
        } else if (currentSequence > lastSequence + 1) {
            int lostPackets = currentSequence - lastSequence - 1;
            info.updateLastSequence(currentSequence);
            return SequenceCheckResult.packetLoss(lostPackets, lastSequence + 1, currentSequence);
        } else {
            return SequenceCheckResult.duplicate(currentSequence);
        }
    }

    public Integer getLastSequence(int vehicleId) {
        SequenceInfo info = sequences.get(vehicleId);
        return info != null ? info.getLastSequence() : null;
    }

    public void resetSequence(int vehicleId) {
        sequences.remove(vehicleId);
    }

    private static class SequenceInfo {
        private int lastSequence;
        private int maxReceived;

        SequenceInfo(int lastSequence, int maxReceived) {
            this.lastSequence = lastSequence;
            this.maxReceived = maxReceived;
        }

        int getLastSequence() { return lastSequence; }
        
        void updateLastSequence(int seq) {
            this.lastSequence = seq;
            if (seq > maxReceived) {
                this.maxReceived = seq;
            }
        }
    }

    public static class SequenceCheckResult {
        private final boolean isSequential;
        private final boolean hasPacketLoss;
        private final boolean isDuplicate;
        private final int lostPackets;
        private final int expectedSequence;
        private final int receivedSequence;
        private final int currentSequence;

        private SequenceCheckResult(boolean isSequential, boolean hasPacketLoss, boolean isDuplicate,
                                     int lostPackets, int expectedSequence, int receivedSequence, int currentSequence) {
            this.isSequential = isSequential;
            this.hasPacketLoss = hasPacketLoss;
            this.isDuplicate = isDuplicate;
            this.lostPackets = lostPackets;
            this.expectedSequence = expectedSequence;
            this.receivedSequence = receivedSequence;
            this.currentSequence = currentSequence;
        }

        public static SequenceCheckResult sequential(int currentSequence) {
            return new SequenceCheckResult(true, false, false, 0, 0, 0, currentSequence);
        }

        public static SequenceCheckResult packetLoss(int lostPackets, int expected, int received) {
            return new SequenceCheckResult(false, true, false, lostPackets, expected, received, received);
        }

        public static SequenceCheckResult duplicate(int currentSequence) {
            return new SequenceCheckResult(false, false, true, 0, 0, 0, currentSequence);
        }

        public boolean isSequential() { return isSequential; }
        public boolean hasPacketLoss() { return hasPacketLoss; }
        public boolean isDuplicate() { return isDuplicate; }
        public int getLostPackets() { return lostPackets; }
        public int getExpectedSequence() { return expectedSequence; }
        public int getReceivedSequence() { return receivedSequence; }
        public int getCurrentSequence() { return currentSequence; }
    }
}
