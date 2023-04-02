package agents.actuators;

import agents.AgentStatus;
import agents.Protocols;
import device.speakers.Speakers;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SpeakerAgent extends ActuatorAgent {
    Speakers speakers;

    @Override
    public void setup() {
        super.setup();
        speakers = new Speakers();
        logger.info("Hello");
    }

    @Override
    protected AgentStatus login() {
        return AgentStatus.IDLE;
    }

    protected AgentStatus idle() {
        ACLMessage m = receiveMsg(MessageTemplate.and(
                MessageTemplate.MatchProtocol(Protocols.COMMAND.toString()),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
        ));
        if (m != null) {
            if (m.getContent().equals("ALARM")) {
                playAlarm();
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

    void playAlarm() {
        speakers.playSound("./data/sounds/alarm.wav");
    }

}
