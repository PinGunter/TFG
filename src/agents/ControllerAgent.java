package agents;

import device.Capabilities;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.ControllerID;
import utils.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ControllerAgent extends ClientAgent {

    private List<String> sensors;
    private List<String> actuators;

    private boolean logout = false;
    private boolean hasWarned = false;
    private Timeout timeout;

    private boolean hasSentEmergency = false;

    private boolean hasCamera, hasMicrophone, hasSpeakers, hasBattery, hasScreen;
    ArrayList<Capabilities> capabilities;


    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.LOGIN;
        timeout = new Timeout();

        //TODO this is currently a mockup, when sensors and actuators are done, they will communicate their capabilities
        hasCamera = new Random().nextBoolean();
        hasMicrophone = new Random().nextBoolean();
        hasSpeakers = new Random().nextBoolean();
        hasBattery = new Random().nextBoolean();
        hasScreen = new Random().nextBoolean();
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
        // TESTING PURPOSES
        if (!hasSentEmergency) {
            hasSentEmergency = true;
            timeout.setTimeout(() -> status = AgentStatus.WARNING, new Random().nextInt(20000, 30000));
        }

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
        if (!hasWarned) {
            hasWarned = true;
            ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
            m.setProtocol(Protocols.WARNING.toString());
            m.setContent("EXAMPLE WARNING - " + getLocalName());
            m.addReceiver(new AID(hub, AID.ISLOCALNAME));
            sendMsg(m);
        }

        ACLMessage response = blockingReceiveMsg(
                MessageTemplate.and(
                        MessageTemplate.and(
                                MessageTemplate.MatchProtocol(Protocols.WARNING.toString()),
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                        ),
                        MessageTemplate.MatchSender(new AID(hub, AID.ISLOCALNAME))
                )
        );
        if (response != null) {
            // maybe process the response msg or fw to the sensor/actuator
            logger.info("Warning finished");
            hasWarned = false;
            return AgentStatus.IDLE;
        }
        return status;
    }

}
