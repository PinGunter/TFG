package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class ClientAgent extends BaseAgent {
    protected String hub;
    protected boolean isFirstTime = true;

    public AgentStatus lookForHub(String protocol) {
        return lookForHub(protocol, "Hello", null);
    }

    public AgentStatus lookForHub(String protocol, String content, Serializable contentObject) {
        List<String> hubProvider = DFGetAllProvidersOf("HUB");
        if (!hubProvider.isEmpty()) {
            hub = hubProvider.get(0);
            ACLMessage helloHub = new ACLMessage();
            helloHub.addReceiver(new AID(hub, AID.ISLOCALNAME));
            helloHub.setContent(content != null ? content : "Hello");
            if (contentObject != null) {
                try {
                    helloHub.setContentObject(contentObject);
                } catch (IOException e) {
                    logger.error("Error while serializing");
                }
            }
            helloHub.setPerformative(ACLMessage.REQUEST);
            helloHub.setProtocol(protocol);
            sendMsg(helloHub);

            logger.info("Waiting for hub");
            helloHub = blockingReceiveMsg(5000);
            if (helloHub != null) {
                if (helloHub.getPerformative() == ACLMessage.INFORM) {
                    logger.info("Greeted Hub");
                    return AgentStatus.IDLE;
                }
            }
        }
        return AgentStatus.LOGIN;
    }

    public void goodBye(String protocol) {
        ACLMessage bye = new ACLMessage();
        bye.setPerformative(ACLMessage.REQUEST);
        bye.setSender(getAID());
        bye.setContent("Bye bye!");
        bye.setProtocol(protocol);
        bye.addReceiver(new AID(hub, AID.ISLOCALNAME));
        sendMsg(bye);
    }

    /**
     * Checks if the msg is a checking connection message
     *
     * @param msg the msg received
     * @return null if it was a checking connection (the agents dont need that msg) or the msg received originally
     */
    protected ACLMessage confirmConnection(ACLMessage msg) {
        if (msg != null) {
            if (msg.getProtocol().equals(Protocols.CHECK_CONNECTION.toString())) {
                if (msg.getPerformative() == ACLMessage.QUERY_IF && msg.getSender().getLocalName().equals(hub)) {
                    ACLMessage m = new ACLMessage();
                    m.setSender(getAID());
                    m.setProtocol(Protocols.CHECK_CONNECTION.toString());
                    m.setPerformative(ACLMessage.CONFIRM);
                    m.addReceiver(new AID(hub, AID.ISLOCALNAME));
                    sendMsg(m);
                    return null;
                }
            }
        }
        return msg;
    }

    @Override
    public ACLMessage receiveMsg() {
        ACLMessage msg = super.receiveMsg();
        return confirmConnection(msg);
    }

    @Override
    public ACLMessage receiveMsg(MessageTemplate template) {
        ACLMessage msg = super.receiveMsg(template);
        return confirmConnection(msg);
    }

    @Override
    public ACLMessage blockingReceiveMsg() {
        ACLMessage msg = super.blockingReceiveMsg();
        return confirmConnection(msg);
    }


    @Override
    public ACLMessage blockingReceiveMsg(int milis) {
        ACLMessage msg = super.blockingReceiveMsg(milis);
        return confirmConnection(msg);
    }

    @Override
    public ACLMessage blockingReceiveMsg(MessageTemplate template) {
        ACLMessage msg = super.blockingReceiveMsg(template);
        return confirmConnection(msg);
    }

    @Override
    public ACLMessage blockingReceiveMsg(MessageTemplate template, int milis) {
        ACLMessage msg = super.blockingReceiveMsg(template, milis);
        return confirmConnection(msg);
    }
}
