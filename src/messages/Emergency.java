package messages;

import jade.core.AID;

import java.io.Serializable;

public class Emergency implements Serializable {
    private AID originSensor;
    private AID originDevice;
    private EmergencyStatus status;
    private String message;


    public Emergency(AID originDevice, AID originSensor, String message) {
        this.originDevice = originDevice;
        this.message = message;
        this.originSensor = originSensor;
        status = EmergencyStatus.DISCOVERED;
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
}
