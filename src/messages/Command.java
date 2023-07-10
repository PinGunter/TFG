package messages;

import java.io.Serializable;

/**
 * Represents a user order to perform something on a device
 * might need more fields in order to detail the specific sensor/actuator
 * or the type of order
 */
public class Command implements Serializable {
    private String order;

    private Serializable obj;
    private String targetDevice;

    private String targetChild;

    private Serializable result;

    private CommandStatus status;

    private String resultType;

    public Command(String order, String targetDevice, String targetChild) {
        this.order = order;
        this.targetDevice = targetDevice;
        this.targetChild = targetChild;
        this.status = CommandStatus.CREATED;
    }

    public Command(String order, Serializable obj, String targetDevice, String targetChild) {
        this.order = order;
        this.targetDevice = targetDevice;
        this.targetChild = targetChild;
        this.status = CommandStatus.CREATED;
        this.obj = obj;
    }

    public String getOrder() {
        return order;
    }

    public void setObj(Serializable o) {
        this.obj = o;
    }

    public Serializable getObj() {
        return obj;
    }

    public String getTargetDevice() {
        return targetDevice;
    }

    public String getTargetChild() {
        return targetChild;
    }

    public void setStatus(CommandStatus status) {
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

    public CommandStatus getStatus() {
        return status;
    }
}
