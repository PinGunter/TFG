package messages;

import jade.core.AID;

import java.io.Serializable;

public class Emergency implements Serializable {
    private AID originSensor;
    private AID originDevice;
    private EmergencyStatus status;
    private String message;

    private Serializable object;

    private String type;


    public Emergency(AID originDevice, AID originSensor, String message) {
        this.originDevice = originDevice;
        this.message = message;
        this.originSensor = originSensor;
        status = EmergencyStatus.DISCOVERED;
        type = "generic";
    }

    public Emergency(AID originDevice, AID originSensor, String message, String type, Serializable obj) {
        this.originDevice = originDevice;
        this.message = message;
        this.originSensor = originSensor;
        status = EmergencyStatus.DISCOVERED;
        this.type = type;
        this.object = obj;
    }


    public String getMessage() {
        return message;
    }

    public AID getOriginDevice() {
        return originDevice;
    }

    public AID getOriginSensor() {
        return originSensor;
    }

    public EmergencyStatus getStatus() {
        return status;
    }

    public void setStatus(EmergencyStatus es) {
        status = es;
    }

    public String getType() {
        return type;
    }

    public Serializable getObject() {
        return object;
    }
}
