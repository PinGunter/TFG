package agents;

import agents.actuators.MicrophoneAgent;
import agents.actuators.ScreenAgent;
import agents.actuators.SpeakerAgent;
import agents.sensors.BatteryAgent;
import agents.sensors.CameraAgent;
import device.Capabilities;
import gui.ControllerGUI;
import jade.core.AID;
import jade.core.MicroRuntime;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.ControllerID;
import messages.Emergency;
import messages.EmergencyStatus;
import utils.Utils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ControllerAgent extends ClientAgent {

    private List<String> sensors;
    private List<String> actuators;

    private boolean logout = false;
    private List<Emergency> emergencies;

    ArrayList<Capabilities> capabilities;

    ControllerGUI gui;

    @Override
    public void setup() {
        super.setup();
        gui = new ControllerGUI(getLocalName(), (e) -> logout());
        status = AgentStatus.LOGIN;
        sensors = new ArrayList<>();
        actuators = new ArrayList<>();
        emergencies = new ArrayList<>();
        capabilities = (ArrayList<Capabilities>) getArguments()[1];

        // launching sensors and actuators
        Object[] args = new Object[3];
        args[0] = cryptKey;
        args[1] = getAID();
        args[2] = false; // motion detection

        for (Capabilities c : capabilities) {
            switch (c) {
                case BATTERY -> {
                    launchSubAgent("BATTERY_" + getLocalName(), BatteryAgent.class, args);
                    sensors.add("BATTERY_" + getLocalName());
                }
                case SPEAKERS -> {
                    launchSubAgent("SPEAKERS_" + getLocalName(), SpeakerAgent.class, args);
                    actuators.add("SPEAKERS_" + getLocalName());
                }
                case SCREEN -> {
                    launchSubAgent("SCREEN_" + getLocalName(), ScreenAgent.class, args);
                    actuators.add("SCREEN_" + getLocalName());
                }
                case MICROPHONE -> {
                    launchSubAgent("MICROPHONE_" + getLocalName(), MicrophoneAgent.class, args);
                    actuators.add("MICROPHONE_" + getLocalName());
                }
                case CAMERA -> {
                    launchSubAgent("CAMERA_" + getLocalName(), CameraAgent.class, args);
                    sensors.add("CAMERA_" + getLocalName());
                }
            }
        }

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
        gui.setStatus(status.toString(), status == AgentStatus.WARNING ? new Color(255, 0, 0) : new Color(0, 0, 0));
    }

    public AgentStatus login() {
        if (isFirstTime) {
            gui.show();
            timer.setTimeout(() -> {
                if (status == AgentStatus.LOGIN) {
                    gui.setStatus("LOGIN ERROR", new Color(255, 0, 0));
                    gui.setTextArea("Couldn't connect to the HUB. Is the password correct?", new Color(255, 0, 0));

                    ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
                    m.setProtocol(Protocols.LOGOUT.toString());
                    m.setSender(getAID());
                    sensors.forEach(sensor -> m.addReceiver(new AID(sensor, AID.ISLOCALNAME)));
                    actuators.forEach(actuator -> m.addReceiver(new AID(actuator, AID.ISLOCALNAME)));
                    sendMsg(m);
                    status = AgentStatus.END;
                    doDelete();
                    if (isMicroBoot) MicroRuntime.stopJADE();
                    timer.setTimeout(() -> System.exit(0), 30000);

                }
            }, 60000);
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
                case LOGOUT -> {
                    return AgentStatus.LOGOUT;
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
        // first, close the sensors/actuators
        ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
        m.setProtocol(Protocols.LOGOUT.toString());
        m.setSender(getAID());
        sensors.forEach(sensor -> m.addReceiver(new AID(sensor, AID.ISLOCALNAME)));
        actuators.forEach(actuator -> m.addReceiver(new AID(actuator, AID.ISLOCALNAME)));
        sendMsg(m);
        goodBye();

        status = AgentStatus.END;
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
