package agents.actuators;

import agents.AgentStatus;
import agents.Protocols;
import device.speakers.Speakers;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.CommandStatus;

import java.io.IOException;

public class SpeakerAgent extends ActuatorAgent {
    Speakers speakers;

    @Override
    public void setup() {
        super.setup();
        speakers = new Speakers();
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
            try {
                Command c = (Command) m.getContentObject();
                if (c.getOrder().equals("ALARM")) {
                    playAlarm();
                    c.setStatus(CommandStatus.DONE);
                    c.setResult("Alarm played", "msg");
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
        }
        return status;
    }

    void playAlarm() {
        speakers.playSound("./data/sounds/alarm.wav");
    }

}
