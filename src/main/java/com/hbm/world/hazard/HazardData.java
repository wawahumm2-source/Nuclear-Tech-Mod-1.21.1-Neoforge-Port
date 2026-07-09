package com.hbm.world.hazard;

public record HazardData(HazardType type, double strength) {
    public static HazardData radiation(double strength) {
        return new HazardData(HazardType.RADIATION, strength);
    }

    public static HazardData digamma(double strength) {
        return new HazardData(HazardType.DIGAMMA, strength);
    }

    public static HazardData fallout(double strength) {
        return new HazardData(HazardType.FALLOUT, strength);
    }

    public static HazardData contamination(double strength) {
        return new HazardData(HazardType.CONTAMINATION, strength);
    }

    public static HazardData heat(double strength) {
        return new HazardData(HazardType.HEAT, strength);
    }

    public static HazardData poison(double strength) {
        return new HazardData(HazardType.POISON, strength);
    }

    public static HazardData blinding(double strength) {
        return new HazardData(HazardType.BLINDING, strength);
    }

    public static HazardData asbestos(double strength) {
        return new HazardData(HazardType.ASBESTOS, strength);
    }

    public static HazardData coal(double strength) {
        return new HazardData(HazardType.COAL, strength);
    }

    public static HazardData hydroactive(double strength) {
        return new HazardData(HazardType.HYDROACTIVE, strength);
    }

    public static HazardData explosive(double strength) {
        return new HazardData(HazardType.EXPLOSIVE, strength);
    }
}
