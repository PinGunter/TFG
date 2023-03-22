package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

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
            // get the protocol as an enum
            String sender = msg.getSender().getLocalName();
            Protocols p;
            try {
                p = Protocols.valueOf(msg.getProtocol());
            } catch (IllegalArgumentException e) {
                p = Protocols.NULL;
                logger.error("Not a valid protocol" + msg.getProtocol());
            }
            // is a login msg
            if (p == Protocols.CONTROLLER_LOGIN) {
                /**
                 * First, we confirm the login, then we communicate the new device
                 * to the telegram agent
                 */
                String newDevice = msg.getSender().getLocalName();
                devices.add(newDevice);
                ACLMessage reply = msg.createReply();
                reply.setContent("hello, " + newDevice);
                reply.setPerformative(ACLMessage.INFORM);
                sendMsg(reply);

                // -------- Telegram
                ACLMessage out = new ACLMessage();
                out.setSender(getAID());
                out.setProtocol(Protocols.CONTROLLER_LOGIN.toString());
                out.setPerformative(ACLMessage.INFORM);
                out.setContent(newDevice);
                notifiers.forEach(notifier -> out.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                sendMsg(out);

                return status;
            }
            // is a normal msg
            //  from notifier
            else if (notifiers.contains(sender)) {
                switch (p) {
                    case COMMAND -> {

                    }
                    case WARNING -> {

                    }
                    default -> {

                    }
                }
            }
            // from some device
            else if (devices.contains(sender)) { // alarm system
                switch (p) {
                    case CONTROLLER_LOGOUT -> {
                        devices.remove(sender);
                        ACLMessage m = new ACLMessage();
                        m.setSender(getAID());
                        m.setProtocol(Protocols.CONTROLLER_LOGOUT.toString());
                        m.setPerformative(ACLMessage.INFORM);
                        m.setContent(sender);
                        notifiers.forEach(notifier -> m.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                        sendMsg(m);
                    }
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
