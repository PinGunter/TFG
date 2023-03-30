package agents.sensors;

import agents.AgentStatus;
import agents.BaseAgent;
import agents.Protocols;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.io.Serializable;

public abstract class SensorAgent extends BaseAgent {
    protected AID deviceController;
    protected int pollingPeriod;

    protected boolean emergency = false;

    @Override
    public void execute() {
        switch (status) {
            case LOGIN -> status = login();
            case IDLE -> status = idle();
            case LOGOUT -> status = logout();
            case END -> exit = true;
        }
    }

    protected AgentStatus login() {
        return status;
    }

    protected AgentStatus idle() {
        try {
            Thread.sleep(pollingPeriod);
            ACLMessage m = receiveMsg();
            if (m != null) {
                if (m.getSender().equals(deviceController) && m.getPerformative() == ACLMessage.INFORM && m.getProtocol().equals(Protocols.WARNING.toString())) {
                    emergency = false;
                }
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        return status;
    }

    protected AgentStatus logout() {
        return status;
    }

    protected void sendAlert(Serializable content) {
        try {
            emergency = true;
            ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
            m.setProtocol(Protocols.WARNING.toString());
            m.setSender(getAID());
            m.addReceiver(deviceController);
            m.setContentObject(content);
            sendMsg(m);
        } catch (IOException e) {
            logger.error("error serializing emergency");
        }
    }
}