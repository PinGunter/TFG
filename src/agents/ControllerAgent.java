package agents;

import agents.sensors.BatteryAgent;
import appboot.JADEBoot;
import device.Capabilities;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.ControllerID;

import java.util.*;

public class ControllerAgent extends ClientAgent {

    private List<String> sensors;
    private List<String> actuators;

    private boolean logout = false;
    private Queue<String> emergencies;

    private boolean hasCamera = true, hasMicrophone = false, hasSpeakers = false, hasBattery = false, hasScreen = false;
    ArrayList<Capabilities> capabilities;


    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.LOGIN;
        sensors = new ArrayList<>();
        emergencies = new ArrayDeque<>();
        JADEBoot boot = new JADEBoot();
        boot.Boot("localhost", 1099);
        Object[] args = new Object[1];
        args[0] = getAID();
        boot.launchAgent("Battery", BatteryAgent.class, args);
        sensors.add("Battery");


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
                            // TODO here, the controller would send the order to the specific sensor/actuator (command.getTarget())
                            // since we dont have those yet, its gonna send a confirmation back to the user
                            ACLMessage confirm = new ACLMessage(ACLMessage.INFORM);
                            confirm.setContent("ORDER: " + c.getOrder() + " received");
                            confirm.setSender(getAID());
                            confirm.addReceiver(new AID(hub, AID.ISLOCALNAME));
                            confirm.setProtocol(Protocols.COMMAND.toString());
                            sendMsg(confirm);
                        } catch (UnreadableException e) {
                            logger.error("Error while deserializing");
                        }
                    }
                }
                case WARNING -> {
                    if (msg.getPerformative() == ACLMessage.REQUEST && sensors.contains(sender) && msg.getProtocol().equals(Protocols.WARNING.toString())) {
                        emergencies.add(msg.getContent());
                    }
                }
            }
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
        if (!emergencies.isEmpty()) {
            String em = emergencies.poll();
            if (em != null) {
                ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
                m.setProtocol(Protocols.WARNING.toString());
                m.setContent(em + " - " + getLocalName());
                m.addReceiver(new AID(hub, AID.ISLOCALNAME));
                sendMsg(m);
            }
        }

        ACLMessage response = receiveMsg();
        if (response != null) {
            if (response.getSender().equals(hub)) {
                if (response.getPerformative() == ACLMessage.INFORM && response.getProtocol().equals(Protocols.WARNING.toString())) {
                    logger.info("Warning finished");
                    return AgentStatus.IDLE;
                }
            } else if (sensors.contains(response.getSender().getLocalName())) { // another emergency
                if (response.getPerformative() == ACLMessage.REQUEST && response.getProtocol().equals(Protocols.WARNING.toString())) {
                    emergencies.add(response.getContent());
                }
            }

        }
        if
    }

}
