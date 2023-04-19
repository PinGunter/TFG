package agents;

import agents.actuators.MicrophoneAgent;
import agents.actuators.ScreenAgent;
import agents.actuators.SpeakerAgent;
import agents.sensors.CameraAgent;
import device.Capabilities;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.ControllerID;
import messages.Emergency;
import messages.EmergencyStatus;
import utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ControllerAgent extends ClientAgent {

    private List<String> sensors;
    private List<String> actuators;

    private boolean logout = false;
    private List<Emergency> emergencies;

    private boolean hasCamera = true, hasMicrophone = true, hasSpeakers = true, hasBattery = false, hasScreen = true;
    ArrayList<Capabilities> capabilities;


    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.LOGIN;
        sensors = new ArrayList<>();
        actuators = new ArrayList<>();
        emergencies = new ArrayList<>();

        // launching sensors and actuators
        Object[] args = new Object[3];
        args[0] = cryptKey;
        args[1] = getAID();
        args[2] = true; // motion detection

//        launchSubAgent("BATTERY_" + getLocalName(), BatteryAgent.class, args);
        launchSubAgent("SPEAKERS_" + getLocalName(), SpeakerAgent.class, args);
//        sensors.add("BATTERY_" + getLocalName());
        launchSubAgent("SCREEN_" + getLocalName(), ScreenAgent.class, args);
        launchSubAgent("MICROPHONE_" + getLocalName(), MicrophoneAgent.class, args);
        actuators.add("SPEAKERS_" + getLocalName());
        actuators.add("MICROPHONE_" + getLocalName());
        actuators.add("SCREEN_" + getLocalName());
        launchSubAgent("CAMERA_" + getLocalName(), CameraAgent.class, args);
        sensors.add("CAMERA_" + getLocalName());


        capabilities = new ArrayList<>();
        if (hasCamera) capabilities.add(Capabilities.CAMERA);
        if (hasMicrophone) capabilities.add(Capabilities.MICROPHONE);
        if (hasSpeakers) capabilities.add(Capabilities.SPEAKERS);
        if (hasBattery) capabilities.add(Capabilities.BATTERY);
        if (hasScreen) capabilities.add(Capabilities.SCREEN);
    }

    @Override
    public void execute() {
        switch (status) {
            case LOGIN -> status = login();
            case IDLE -> status = idle();
            case WARNING -> status = warning();
            case LOGOUT -> status = logout();
            case END -> exit = true;
        }
    }

    public AgentStatus login() {
        if (isFirstTime) {
            this.DFAddMyServices(List.of("DEVICE-CONTROLLER"));
            this.DFAddMyServices(capabilities.stream().map(Enum::toString).toList());
            isFirstTime = false;
        }
        return this.lookForHub(Protocols.CONTROLLER_LOGIN.toString(), null, new ControllerID(getLocalName(), capabilities));
    }


    public AgentStatus idle() {
        ACLMessage msg = receiveMsg();

        if (msg != null) {
            String sender = msg.getSender().getLocalName();
            Protocols p;
            try {
                p = Protocols.valueOf(msg.getProtocol());
            } catch (IllegalArgumentException e) {
                p = Protocols.NULL;
                logger.error("Not a valid protocol: " + msg.getProtocol());
            }
            switch (p) {
                case COMMAND -> {
                    if (msg.getPerformative() == ACLMessage.REQUEST && sender.equals(hub)) {
                        try {
                            Command c = (Command) msg.getContentObject();
                            String receiver = c.getTargetDevice() + "_" + getLocalName();
                            if (actuators.contains(receiver) || sensors.contains(receiver)) {
                                ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
                                forward.setProtocol(Protocols.COMMAND.toString());
                                forward.setSender(getAID());
                                forward.addReceiver(new AID(receiver, AID.ISLOCALNAME));
                                forward.setContentObject(c);
                                sendMsg(forward);
                            }
                        } catch (UnreadableException e) {
                            logger.error("Error while deserializing");
                        } catch (IOException e) {
                            logger.error("Error forwarding command");
                        }
                    }
                    if (msg.getPerformative() == ACLMessage.INFORM) {
                        try {
                            Command c = (Command) msg.getContentObject();
                            ACLMessage confirm = new ACLMessage(ACLMessage.INFORM);
                            confirm.setContentObject(c);
                            confirm.setSender(getAID());
                            confirm.addReceiver(new AID(hub, AID.ISLOCALNAME));
                            confirm.setProtocol(Protocols.COMMAND.toString());
                            sendMsg(confirm);
                        } catch (UnreadableException | IOException e) {
                            logger.error("Error forwarding command from device");
                        }
                    }
                }
                case WARNING -> {
                    if (msg.getPerformative() == ACLMessage.REQUEST && sensors.contains(sender)) {
                        Emergency em = null;
                        try {
                            em = (Emergency) msg.getContentObject();
                        } catch (UnreadableException e) {
                            logger.error("Error deserializing");
                        }
                        if (em != null) emergencies.add(em);
                    }
                }
                case AUDIO -> {
                    if (msg.getPerformative() == ACLMessage.INFORM && actuators.contains(sender)) {
                        try {
                            ACLMessage forward = new ACLMessage(ACLMessage.INFORM);
                            forward.setProtocol(Protocols.AUDIO.toString());
                            forward.setSender(getAID());
                            forward.addReceiver(new AID(hub, AID.ISLOCALNAME));
                            forward.setContentObject(msg.getContentObject());
                            sendMsg(forward);
                        } catch (IOException | UnreadableException e) {
                            logger.error("error forwarding audio");
                        }
                    }
                }
            }
        }
        if (emergencies.size() > 0) {
            return AgentStatus.WARNING;
        }
        return status;
    }

    public AgentStatus logout() {
        // logout process
        goodBye(Protocols.CONTROLLER_LOGOUT.toString());
        return AgentStatus.END;
    }

    public AgentStatus warning() {
        // warn the hub and wait for response
        emergencies.forEach((emergency) -> {
            if (emergency.getStatus() == EmergencyStatus.DISCOVERED) {
                try {
                    ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
                    m.setProtocol(Protocols.WARNING.toString());
                    m.setContentObject(emergency);
                    m.addReceiver(new AID(hub, AID.ISLOCALNAME));
                    sendMsg(m);
                    emergency.setStatus(EmergencyStatus.ALERTED);
                } catch (IOException e) {
                    logger.error("error serializing emergency");
                }
            }
        });


        ACLMessage response = receiveMsg();
        if (response != null) {
            if (response.getSender().getLocalName().equals(hub)) {
                if (response.getPerformative() == ACLMessage.INFORM && response.getProtocol().equals(Protocols.WARNING.toString())) {
                    Emergency em = null;
                    try {
                        em = (Emergency) response.getContentObject();
                    } catch (UnreadableException e) {
                        logger.error("Error deserializing");
                    }
                    if (em != null) {
                        Utils.RemoveEmergency(emergencies, em.getMessage());
                        ACLMessage informSensor = new ACLMessage(ACLMessage.INFORM);
                        informSensor.setProtocol(Protocols.WARNING.toString());
                        informSensor.setSender(getAID());
                        informSensor.addReceiver(em.getOriginSensor());
                        sendMsg(informSensor);
                    }

                }
            } else if (sensors.contains(response.getSender().getLocalName())) { // another emergency
                if (response.getPerformative() == ACLMessage.REQUEST && response.getProtocol().equals(Protocols.WARNING.toString())) {
                    Emergency em = null;
                    try {
                        em = (Emergency) response.getContentObject();
                    } catch (UnreadableException e) {
                        logger.error("Error deserializing");
                    }
                    if (em != null) emergencies.add(em);
                } else if (response.getPerformative() == ACLMessage.INFORM && response.getProtocol().equals(Protocols.WARNING.toString())) { // emergency finished by itself
                    Emergency em = null;
                    try {
                        em = (Emergency) response.getContentObject();
                        if (em != null) {
                            Utils.RemoveEmergency(emergencies, em.getMessage());
                            ACLMessage informHub = new ACLMessage(ACLMessage.INFORM);
                            informHub.setContentObject(em);
                            informHub.setProtocol(Protocols.WARNING.toString());
                            informHub.setSender(getAID());
                            informHub.addReceiver(new AID(hub, AID.ISLOCALNAME));
                            sendMsg(informHub);
                        }
                    } catch (UnreadableException | IOException e) {
                        logger.error("Error deserializing/serializing");
                    }
                }
            }

        }
        if (emergencies.size() == 0) {
            logger.info("Finished emergency");
            return AgentStatus.IDLE;
        }
        return status;
    }

}
