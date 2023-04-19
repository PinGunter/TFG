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

    private Serializable result;

    private String status;

    private String resultType;

    public Command(String order, String targetDevice, String targetChild) {
        this.order = order;
        this.targetDevice = targetDevice;
        this.targetChild = targetChild;
        this.status = "CREATED";
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public void setResult(Serializable result, String resultType) {
        this.resultType = resultType;
        this.result = result;
    }

    public Serializable getResult() {
        return result;
    }

    public String getResultType() {
        return resultType;
    }

    public String getStatus() {
        return status;
    }
}
