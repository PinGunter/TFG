package messages;

import java.io.Serializable;

/**
 * Represents a user order to perform something on a device
 * might need more fields in order to detail the specific sensor/actuator
 * or the type of order
 */
public class Command implements Serializable {
    private String order;
    private String targetDevice;

    private String targetChild;

    public Command(String order, String targetDevice, String targetChild) {
        this.order = order;
        this.targetDevice = targetDevice;
        this.targetChild = targetChild;
    }

    public String getOrder() {
        return order;
    }

    public String getTargetDevice() {
        return targetDevice;
    }

    public String getTargetChild() {
        return targetChild;
    }
}
