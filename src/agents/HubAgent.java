package agents;

import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;

public class HubAgent extends BaseAgent {
    private List<String> notifiers;
    private List<String> devices;

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
                if (notifierMsg.getProtocol().equals("NOTIFIER-LOGIN")) {
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
            if (msg.getProtocol().equals("CONTROLLER-LOGIN")) {
                devices.add(msg.getSender().getLocalName());
                ACLMessage reply = msg.createReply();
                reply.setContent("hello, " + msg.getSender().getLocalName());
                reply.setPerformative(ACLMessage.INFORM);
                sendMsg(reply);
                return status;
            }
            // is a normal msg
            else if (notifiers.contains(msg.getSender().getLocalName())) {
                logger.info("FW from " + msg.getSender().getLocalName() + ": " + msg.getContent());
            } else if (devices.contains(msg.getSender().getLocalName())) {
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
