package agents.sensors;

import agents.AgentStatus;
import agents.Protocols;
import com.github.sarxos.webcam.WebcamMotionEvent;
import device.camera.Camera;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.Emergency;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CameraAgent extends SensorAgent {
    Camera camera;
    boolean motionDetectionEnabled;

    int interval = 2000;

    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.IDLE;
        try {
            motionDetectionEnabled = (boolean) getArguments()[2];
        } catch (Exception e) {
            motionDetectionEnabled = false;
        }
        camera = new Camera(motionDetectionEnabled, this::onMotion, interval);
        if (motionDetectionEnabled) camera.startDetection();
    }

    @Override
    public AgentStatus login() {
        return AgentStatus.IDLE;
    }

    void onMotion(WebcamMotionEvent event) {
        logger.info("motion detected");
        try {
            BufferedImage image = event.getCurrentImage();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", bos);
            sendAlert(new Emergency(deviceController, getAID(), "Motion detected", "Motion", bos.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected AgentStatus idle() {
        ACLMessage m = receiveMsg(MessageTemplate.and(
                MessageTemplate.MatchProtocol(Protocols.COMMAND.toString()),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
        if (m != null) {

            try {
                Command c = (Command) m.getContentObject();
                if (c.getOrder().equals("photo") || c.getOrder().equals("ALARM")) { //TODO the ALARM is for easy of use. Will be changed to be better
                    byte[] image = camera.getImage();
                    c.setStatus("DONE");
                    c.setResult(image, "img");
                    ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                    response.setProtocol(Protocols.COMMAND.toString());
                    response.setSender(getAID());
                    response.addReceiver(deviceController);
                    response.setContentObject(c);
                    sendMsg(response);
                } else if (c.getOrder().startsWith("burst")) {
                    int n = Integer.parseInt(c.getOrder().split(" ")[1]);
                    int interval = Integer.parseInt(c.getOrder().split(" ")[2]) * 1000;
                    //TODO in the future send an acknowledged message to show that is running the command
                    ArrayList<BufferedImage> burst = camera.startBurst(n, interval);
                    ArrayList<byte[]> photos = new ArrayList<>(burst.size());
                    for (int i = 0; i < n; i++) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ImageIO.write(burst.get(i), "jpg", bos);
                        photos.add(bos.toByteArray());
                    }
                    c.setStatus("DONE");
                    c.setResult(photos, "burst");
                    ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                    response.setProtocol(Protocols.COMMAND.toString());
                    response.setSender(getAID());
                    response.addReceiver(deviceController);
                    response.setContentObject(c);
                    sendMsg(response);
//                    camera.startBurst(n, interval, (burst) -> {
//                        try {
//                            burst.forEach(photo -> {
//                                try {
//                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                                    ImageIO.write(photo, "jpg", bos);
//                                    photos.add(bos.toByteArray());
//                                } catch (IOException e) {
//                                    logger.error("Error processing burst");
//                                }
//                            });
//                            c.setStatus("DONE");
//                            c.setResult(photos, "burst");
//                            ACLMessage response = new ACLMessage(ACLMessage.INFORM);
//                            response.setProtocol(Protocols.COMMAND.toString());
//                            response.setSender(getAID());
//                            response.addReceiver(deviceController);
//                            response.setContentObject(c);
//                            sendMsg(response);
//                        } catch (IOException e) {
//                            logger.error("Error getting burst");
//                        }
//                    });

                }
            } catch (UnreadableException | IOException e) {
                logger.error("Error processing command");
            }
        }


        return status;
    }

}
