package agents.actuators;

import agents.AgentStatus;
import agents.Protocols;
import device.FullScreenWindow;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.CommandStatus;

import java.io.IOException;

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
                MessageTemplate.or(MessageTemplate.MatchProtocol(Protocols.COMMAND.toString()), MessageTemplate.MatchProtocol(Protocols.LOGOUT.toString())),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
        ));
        if (m != null) {
            if (m.getProtocol().equals(Protocols.COMMAND.toString())) {

                try {
                    Command c = (Command) m.getContentObject();
                    if (c.getOrder().equals("toggle")) {
                        toggleScreen();
                        c.setStatus(CommandStatus.DONE);
                        c.setResult("Screen " + (window.isVisible() ? "on" : "off"), "msg");
                        ACLMessage res = new ACLMessage(ACLMessage.INFORM);
                        res.setProtocol(Protocols.COMMAND.toString());
                        res.setSender(getAID());
                        res.addReceiver(deviceController);
                        res.setContentObject(c);
                        sendMsg(res);
                    }
                } catch (UnreadableException | IOException e) {
                    logger.error("Error processing command");
                }
            } else if (m.getProtocol().equals(Protocols.LOGOUT.toString())) {
                return AgentStatus.LOGOUT;
            }

        }
        return status;
    }

    void toggleScreen() {
        logger.info("Toggling screen");
        window.setVisible(!window.isVisible());
    }
}
