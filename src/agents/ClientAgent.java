package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.List;

public class ClientAgent extends BaseAgent {
    protected String hub;
    protected boolean isFirstTime = true;

    public AgentStatus lookForHub(String protocol) {
        List<String> hubProvider = DFGetAllProvidersOf("HUB");
        if (!hubProvider.isEmpty()) {
            hub = hubProvider.get(0);
            ACLMessage helloHub = new ACLMessage();
            helloHub.addReceiver(new AID(hub, AID.ISLOCALNAME));
            helloHub.setContent("Hello");
            helloHub.setPerformative(ACLMessage.REQUEST);
            helloHub.setProtocol(protocol);
            sendMsg(helloHub);

            logger.info("Waiting for hub");
            helloHub = blockingReceive(5000);
            if (helloHub != null) {
                if (helloHub.getPerformative() == ACLMessage.INFORM) {
                    logger.info("Greeted Hub");
                    return AgentStatus.IDLE;
                }
            }
        }
        return AgentStatus.LOGIN;
    }
}
