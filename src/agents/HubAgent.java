package agents;

import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HubAgent extends BaseAgent {
    private ArrayList<String> notifiers;
    private ArrayList<String> devices;

    private boolean registered = false;

    @Override
    public void setup() {
        super.setup();
        notifiers = new ArrayList<String>();
        devices = new ArrayList<String>();
        this.status = AgentStatus.LOGIN;
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
        if (!registered) {
            this.DFAddMyServices(List.of("HUB"));
            registered = true;
        }

        ACLMessage notifierMsg = receiveMsg();
        if (notifierMsg != null) {
            if (notifierMsg.getPerformative() == ACLMessage.REQUEST && notifierMsg.getContent().equals("Hello")) {
                if (notifierMsg.getProtocol().equals(Protocols.NOTIFIER_LOGIN.toString())) {
                    notifiers.add(notifierMsg.getSender().getLocalName());
                    ACLMessage reply = notifierMsg.createReply();
                    reply.setContent("helloTelegram");
                    reply.setPerformative(ACLMessage.INFORM);
                    sendMsg(reply);
                    return AgentStatus.IDLE;
                }
            }

        }
        return AgentStatus.LOGIN;
    }

    public AgentStatus idle() {
        ACLMessage msg = receiveMsg();
        if (msg != null) {
            // is a login / logout msg
            if (msg.getProtocol().equals(Protocols.CONTROLLER_LOGIN.toString())) {
                devices.add(msg.getSender().getLocalName());
                ACLMessage reply = msg.createReply();
                reply.setContent("hello, " + msg.getSender().getLocalName());
                reply.setPerformative(ACLMessage.INFORM);
                sendMsg(reply);
                return status;
            }
            // is a normal msg
            //  from notifier
            else if (notifiers.contains(msg.getSender().getLocalName())) {
                Protocols p;
                try {
                    p = Protocols.valueOf(msg.getProtocol());
                } catch (IllegalArgumentException e) {
                    p = Protocols.NULL;
                    logger.error("Not a valid protocol" + msg.getProtocol());
                }
                switch (p) {
                    case ONLINE_DEVICES -> {
                        if (msg.getPerformative() == ACLMessage.QUERY_REF) {
                            ACLMessage out = new ACLMessage();
                            out.setSender(getAID());
                            out.addReceiver(msg.getSender());
                            out.setProtocol(Protocols.ONLINE_DEVICES.toString());
                            out.setPerformative(ACLMessage.INFORM);
                            try {
                                out.setContentObject(devices);
                            } catch (IOException e) {
                                logger.error("Error while serializing\n" + e.getMessage());
                            }
                            sendMsg(out);
                        }
                    }
                    case COMMAND -> {

                    }
                    default -> {

                    }
                }
            }
            // from some device
            else if (devices.contains(msg.getSender().getLocalName())) { // alarm system
                if (msg.getProtocol().equals("WARNING")) {
                    return AgentStatus.WARNING;
                }
            }
        }
        return status;
    }

    public AgentStatus warning() {
        return status;
    }

    public AgentStatus logout() {
        return AgentStatus.END;
    }


}
