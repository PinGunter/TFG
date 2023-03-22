package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import utils.Timeout;

import java.util.List;

public class ControllerAgent extends ClientAgent {

    private List<String> sensors;
    private List<String> actuators;

    private boolean logout = false;

    private Timeout timeout;

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
//        timeout.setTimeout(() -> status = AgentStatus.LOGOUT, new Random().nextInt(20000, 30000));
        ACLMessage msg = receiveMsg();
        if (msg != null) {
            String sender = msg.getSender().getLocalName();
            Protocols p;
            try {
                p = Protocols.valueOf(msg.getProtocol());
            } catch (IllegalArgumentException e) {
                p = Protocols.NULL;
                logger.error("Not a valid protocol" + msg.getProtocol());
            }
            switch (p) {
                case CHECK_CONNECTION -> {
                    if (msg.getPerformative() == ACLMessage.QUERY_IF && sender.equals(hub)) {
                        ACLMessage m = new ACLMessage();
                        m.setSender(getAID());
                        m.setProtocol(Protocols.CHECK_CONNECTION.toString());
                        m.setPerformative(ACLMessage.CONFIRM);
                        m.addReceiver(new AID(hub, AID.ISLOCALNAME));
                        sendMsg(m);
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
        return status;
    }

}
