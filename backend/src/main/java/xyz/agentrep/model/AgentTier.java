package xyz.agentrep.model;

public enum AgentTier {
    UNKNOWN,
    EMERGING,
    TRUSTED,
    ELITE;

    public static AgentTier fromScore(java.math.BigDecimal score, int totalOutcomes) {
        if (totalOutcomes < 5) return UNKNOWN;
        double s = score.doubleValue();
        if (s >= 90 && totalOutcomes >= 50) return ELITE;
        if (s >= 75 && totalOutcomes >= 20) return TRUSTED;
        if (s >= 50) return EMERGING;
        return UNKNOWN;
    }
}
