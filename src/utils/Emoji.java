package utils;

public enum Emoji {
    OWL("\uD83E\uDD89"),
    NERD("\uD83E\uDD13"),
    COLD_SWEAT("\uD83D\uDE05"),
    FINGER_UP("☝"),
    HELLO("👋"),
    RIGHT_ARROW("➡"),
    LEFT_ARROW("⬅"),
    HOUSE("\uD83C\uDFE0"),
    LAPTOP("\uD83D\uDCBB"),
    GEAR("⚙"),
    WARNING("⚠"),
    ALERT("\uD83D\uDEA8"),
    ERROR("❗"),

    NOTIFY("🔔"),
    LOCATION_PIN("\uD83D\uDCCD"),
    BROADCAST("\uD83D\uDCE2"),
    SPEAKER("\uD83D\uDD09"),
    SILENCE("\uD83D\uDD07"),
    CHECK("✅");


    private String keycode;

    Emoji(String s) {
        keycode = s;
    }

    @Override
    public String toString() {
        return keycode;
    }
}
