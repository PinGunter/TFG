package agents;

public enum Protocols {
    CONTROLLER_LOGIN("CONTROLLER_LOGIN"),
    NOTIFIER_LOGIN("NOTIFIER_LOGIN"),
    NOTIFY_USER("NOTIFY_USER"),
    WARNING("WARNING"),
    COMMAND("COMMAND"),
    CHECK_CONNECTION("CHECK_CONNECTION"),
    CONTROLLER_LOGOUT("CONTROLLER_LOGOUT"),
    NOTIFIER_LOGOUT("NOTIFIER_LOGOUT"),

    CONTROLLER_DISCONNECT("CONTROLLER_DISCONNECT"),
    AUDIO("AUDIO"),

    NULL("");
    private String name;

    Protocols(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }


}
