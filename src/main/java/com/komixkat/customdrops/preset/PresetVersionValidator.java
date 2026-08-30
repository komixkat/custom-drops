package com.komixkat.customdrops.preset;

public final class PresetVersionValidator {

    private PresetVersionValidator() {}

    public static final class PresetVersionMismatchException extends RuntimeException {
        public PresetVersionMismatchException(String presetId, String presetVersion, String runningVersion) {
            super("Preset '" + presetId + "' was verified against Minecraft " + presetVersion
                + " but this instance is running " + runningVersion
                + ". Refusing to load it to avoid silently misapplying a mismatched loot table format.");
        }
    }

    public static void validate(String presetId, String presetVersion, String runningVersion) {
        if (!presetVersion.equals(runningVersion)) {
            throw new PresetVersionMismatchException(presetId, presetVersion, runningVersion);
        }
    }
}
