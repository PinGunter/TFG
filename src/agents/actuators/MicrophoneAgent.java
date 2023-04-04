package agents.actuators;

import agents.AgentStatus;
import agents.Protocols;
import device.microphone.Microphone;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.Instant;
import java.util.Date;

public class MicrophoneAgent extends ActuatorAgent {

    Microphone micro;
    boolean isRecording;

    String lastRecording;

    @Override
    public void setup() {
        super.setup();
        isRecording = false;
        micro = new Microphone();
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
                startStop();
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

    void startStop() {
        if (!isRecording) {
            lastRecording = "./temp/" + Date.from(Instant.now());
            micro.startRecording(lastRecording);
            isRecording = true;
        } else {
            micro.stopRecording();
            isRecording = false;
            ACLMessage audio = new ACLMessage(ACLMessage.INFORM);
            audio.setProtocol(Protocols.AUDIO.toString());
            audio.setSender(getAID());
            audio.addReceiver(deviceController);
            audio.setContent(lastRecording + ".wav");
            sendMsg(audio);
        }
    }
}
