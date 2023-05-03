package agents.actuators;

import agents.AgentStatus;
import agents.Protocols;
import device.speakers.Speakers;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.CommandStatus;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpeakerAgent extends ActuatorAgent {
    Speakers speakers;

    final String alarmPath = "data/sounds/alarm.wav";

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
                } else if (c.getOrder().startsWith("play")) {
                    String audioPath = c.getOrder().split(" ")[1];
                    byte[] audioBytes = (byte[]) c.getObj();
                    Files.write(Path.of(audioPath), audioBytes);

                    try {
                        // notify we are in progress of playing
                        c.setStatus(CommandStatus.IN_PROGRESS);
                        c.setResult(deviceController.getLocalName() + " Started playing audio", "audio");
                        ACLMessage res = new ACLMessage(ACLMessage.INFORM);
                        res.setProtocol(Protocols.COMMAND.toString());
                        res.setSender(getAID());
                        res.addReceiver(deviceController);
                        res.setContentObject(c);
                        sendMsg(res);

                        // play the sound
                        playSound(audioPath);

                        c.setStatus(CommandStatus.DONE);
                        c.setResult(deviceController.getLocalName() + " finished playing sound", "msg");
                    } catch (InterruptedException | IOException e) {
                        // warn about error
                        logger.error("Error converting audio to mp3");
                        c.setStatus(CommandStatus.FAILURE);
                        c.setResult("Error playing audio", "err");

                    }

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
        speakers.playSound(alarmPath);
    }

    void playSound(String path) throws IOException, InterruptedException {
        // first convert to mp3, then play sound
        String newPath = Utils.ToMP3(path);
        speakers.playSound(newPath);

        // we now delete the files
        File old = new File(path);
        old.delete();
        if (!newPath.equals(path)) {
            File mp3 = new File(newPath);
            mp3.delete();
        }
    }
}
