package com.speedyteam101.demonslayer;

/** Demon Slayer Corps ranks, earned with merit from slaying demons. */
public enum Rank {
    MIZUNOTO("Mizunoto", 0),
    MIZUNOE("Mizunoe", 5),
    KANOTO("Kanoto", 15),
    KANOE("Kanoe", 30),
    TSUCHINOTO("Tsuchinoto", 50),
    TSUCHINOE("Tsuchinoe", 80),
    HINOTO("Hinoto", 120),
    HINOE("Hinoe", 170),
    KINOTO("Kinoto", 230),
    KINOE("Kinoe", 300),
    HASHIRA("Hashira", 400);

    private final String displayName;
    private final int merit;

    Rank(String displayName, int merit) {
        this.displayName = displayName;
        this.merit = merit;
    }

    public String displayName() {
        return displayName;
    }

    /** Merit needed to reach this rank. */
    public int merit() {
        return merit;
    }

    /** Technique and Nichirin damage multiplier: 1.0 at Mizunoto up to 2.0 at Hashira. */
    public double power() {
        return 1.0 + ordinal() * 0.1;
    }

    public int maxBreath() {
        return 100 + ordinal() * 10;
    }

    public Rank next() {
        return this == HASHIRA ? null : values()[ordinal() + 1];
    }

    public static Rank of(int merit) {
        Rank result = MIZUNOTO;
        for (Rank r : values()) {
            if (merit >= r.merit) {
                result = r;
            }
        }
        return result;
    }
}
