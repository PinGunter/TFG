package agents.actuators;

import agents.AgentStatus;
import agents.Protocols;
import device.microphone.Microphone;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.CommandStatus;

import java.io.IOException;

public class MicrophoneAgent extends ActuatorAgent {

    Microphone micro;
    boolean isRecording;

    Command lastCommand;


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
                MessageTemplate.or(MessageTemplate.MatchProtocol(Protocols.COMMAND.toString()), MessageTemplate.MatchProtocol(Protocols.LOGOUT.toString())),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
        ));
        if (m != null) {
            if (m.getProtocol().equals(Protocols.COMMAND.toString())) {
                try {
                    Command c = (Command) m.getContentObject();
                    logger.info("COMMAND RECEIVED " + c.getOrder());
                    if (c.getOrder().startsWith("record")) {
                        int seconds = Integer.parseInt(c.getOrder().split(" ")[1]);
                        if (start(seconds + 1)) { // +1 because its always a second short
                            lastCommand = c;
                            c.setStatus(CommandStatus.IN_PROGRESS);
                            c.setResult("Started recording", "msg");
                            ACLMessage res = new ACLMessage(ACLMessage.INFORM);
                            res.setProtocol(Protocols.COMMAND.toString());
                            res.setSender(getAID());
                            res.addReceiver(deviceController);
                            res.setContentObject(c);
                            sendMsg(res);
                        } else { // already recording
                            c.setStatus(CommandStatus.FAILURE);
                            c.setResult("A recording was already in progress", "err");
                            ACLMessage res = new ACLMessage(ACLMessage.INFORM);
                            res.setProtocol(Protocols.COMMAND.toString());
                            res.setSender(getAID());
                            res.addReceiver(deviceController);
                            res.setContentObject(c);
                            sendMsg(res);
                        }
                    } else if (c.getOrder().equals("startstop")) {
                        if (start()) {
                            lastCommand = c;
                            c.setStatus(CommandStatus.IN_PROGRESS);
                            c.setResult("Started recording", "audio");
                            ACLMessage res = new ACLMessage(ACLMessage.INFORM);
                            res.setProtocol(Protocols.COMMAND.toString());
                            res.setSender(getAID());
                            res.addReceiver(deviceController);
                            res.setContentObject(c);
                            sendMsg(res);
                        } else { // already recording
                            stop();
                        }
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


    boolean start() {
        return start(3 * 60); // 3 minute max
    }

    boolean start(int seconds) {
        if (!isRecording) {
            micro.startRecording();
            isRecording = true;
            timer.setTimeout(this::stop, seconds * 1000);
            return true;
        }
        return false;
    }

    void stop() {
        if (isRecording) {
            byte[] audioFile = micro.stopRecording();
            isRecording = false;
            try {
                lastCommand.setStatus(CommandStatus.DONE);
                lastCommand.setResult(audioFile, "audio");
                ACLMessage res = new ACLMessage(ACLMessage.INFORM);
                res.setProtocol(Protocols.COMMAND.toString());
                res.setSender(getAID());
                res.addReceiver(deviceController);
                res.setContentObject(lastCommand);
                sendMsg(res);
            } catch (IOException e) {
                logger.error("Error serializing audio");
            }
        }
    }

}
