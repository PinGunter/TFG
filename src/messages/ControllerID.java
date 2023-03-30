package messages;

import device.Capabilities;

import java.io.Serializable;
import java.util.ArrayList;

public class ControllerID implements Serializable {
    private ArrayList<Capabilities> capabilities;
    private String name;

    public ControllerID(String name, ArrayList<Capabilities> capabilities) {
        this.name = name;
        this.capabilities = capabilities;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Capabilities> getCapabilities() {
        return capabilities;
    }
}
