package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.Timeout;

import java.util.List;
import java.util.Random;

public class ControllerAgent extends ClientAgent {

    private List<String> sensors;
    private List<String> actuators;

    private boolean logout = false;
    private boolean hasWarned = false;
    private Timeout timeout;

    private boolean hasSentEmergency = false;

    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.LOGIN;
        timeout = new Timeout();
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
            isFirstTime = false;
        }
        return this.lookForHub(Protocols.CONTROLLER_LOGIN.toString());
    }

    public AgentStatus idle() {
        // TESTING PURPOSES
        if (!hasSentEmergency) {
            hasSentEmergency = true;
            timeout.setTimeout(() -> status = AgentStatus.WARNING, new Random().nextInt(20000, 30000));
        }

        ACLMessage msg = receiveMsg();

        // this is currently useless since the auto-response has been moved to the ClientAgent class
//        if (msg != null) {
//            String sender = msg.getSender().getLocalName();
//            Protocols p;
//            try {
//                p = Protocols.valueOf(msg.getProtocol());
//            } catch (IllegalArgumentException e) {
//                p = Protocols.NULL;
//                logger.error("Not a valid protocol" + msg.getProtocol());
//            }
//            switch (p) {
//                case CHECK_CONNECTION -> {
//                    if (msg.getPerformative() == ACLMessage.QUERY_IF && sender.equals(hub)) {
//                        ACLMessage m = new ACLMessage();
//                        m.setSender(getAID());
//                        m.setProtocol(Protocols.CHECK_CONNECTION.toString());
//                        m.setPerformative(ACLMessage.CONFIRM);
//                        m.addReceiver(new AID(hub, AID.ISLOCALNAME));
//                        sendMsg(m);
//                    }
//                }
//            }
//        }
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
