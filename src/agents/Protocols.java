package agents;

public enum Protocols {
    CONTROLLER_LOGIN("CONTROLLER_LOGIN"),
    NOTIFIER_LOGIN("NOTIFIER_LOGIN"),
    ONLINE_DEVICES("ONLINE_DEVICES"),
    WARNING("WARNING"),
    COMMAND("COMMAND"),

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
