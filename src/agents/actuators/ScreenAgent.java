package agents.actuators;

import agents.AgentStatus;
import agents.Protocols;
import device.FullScreenWindow;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ScreenAgent extends ActuatorAgent {

    FullScreenWindow window;


    @Override
    public void setup() {
        super.setup();
        window = new FullScreenWindow();
    }

    @Override
    protected AgentStatus login() {
        return AgentStatus.IDLE;
    }

    @Override
    protected AgentStatus idle() {
        ACLMessage m = receiveMsg(MessageTemplate.and(
                MessageTemplate.MatchProtocol(Protocols.COMMAND.toString()),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
        ));
        if (m != null) {
            if (m.getContent().equals("ALARM")) {
                toggleScreen();
                ACLMessage res = new ACLMessage(ACLMessage.INFORM);
                res.setProtocol(Protocols.COMMAND.toString());
                res.setSender(getAID());
                res.addReceiver(deviceController);
                res.setContent(m.getContent());
                sendMsg(res);
            }
        }
        return status;
    }

    void toggleScreen() {
        logger.info("Toggling screen");
        window.setVisible(!window.isVisible());
    }
}
