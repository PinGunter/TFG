package agents.sensors;

import agents.AgentStatus;
import agents.Protocols;
import com.github.sarxos.webcam.WebcamMotionEvent;
import device.camera.Camera;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.CommandStatus;
import messages.Emergency;
import utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CameraAgent extends SensorAgent {
    Camera camera;
    boolean motionDetectionEnabled;

    int interval = 5000;

    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.IDLE;
        try {
            motionDetectionEnabled = (boolean) getArguments()[2];
        } catch (Exception e) {
            motionDetectionEnabled = false;
        }
        try {
            camera = new Camera(motionDetectionEnabled, this::onMotion, interval);
        } catch (Exception e) {
            logger.error("Error opening camera, shutting down agent");
            status = AgentStatus.END;
            ACLMessage m = new ACLMessage(ACLMessage.INFORM);
            m.setProtocol(Protocols.ENDPOINT_LOGOUT.toString());
            m.setSender(getAID());
            m.addReceiver(deviceController);
            sendMsg(m);
            logout();
        }
    }

    @Override
    public AgentStatus login() {
        return AgentStatus.IDLE;
    }

    void onMotion(WebcamMotionEvent event) {
        logger.info("motion detected");
        try {
            Color c = new Color(50, 200, 0);
            BufferedImage img = event.getCurrentImage();
            ArrayList<Point> points = event.getPoints();
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for (Point p : points) {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
            // top and bottom lines
            for (int k = -1; k <= 1; k++) {
                for (int i = minX; i <= maxX; i++) {
                    img.setRGB(i, Utils.clamp(minY + k, 0, img.getHeight() - 1), c.getRGB());
                    img.setRGB(i, Utils.clamp(maxY + k, 0, img.getHeight() - 1), c.getRGB());
                }
                // left and right ones
                for (int i = minY; i <= maxY; i++) {
                    img.setRGB(Utils.clamp(minX + k, 0, img.getWidth() - 1), i, c.getRGB());
                    img.setRGB(Utils.clamp(maxX + k, 0, img.getWidth() - 1), i, c.getRGB());
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", bos);
            sendAlert(new Emergency(deviceController, getAID(), "Motion detected", "Motion", bos.toByteArray()));
        } catch (IOException e) {
            System.err.println("Error writing img");
        }
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
                    if (c.getOrder().equals("photo")) {
                        byte[] image = camera.getImage();
                        c.setStatus(CommandStatus.DONE);
                        c.setResult(image, "img");
                        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                        response.setProtocol(Protocols.COMMAND.toString());
                        response.setSender(getAID());
                        response.addReceiver(deviceController);
                        response.setContentObject(c);
                        sendMsg(response);
                    } else if (c.getOrder().startsWith("burst")) {
                        int n = Integer.parseInt(c.getOrder().split(" ")[1]);
                        double interval = Double.parseDouble(c.getOrder().split(" ")[2]) * 1000;
                        //TODO in the future send an acknowledged message to show that is running the command
                        ArrayList<BufferedImage> burst = camera.startBurst(n, interval);
//                    ArrayList<byte[]> photos = new ArrayList<>(burst.size());
//                    for (int i = 0; i < n; i++) {
//                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                        ImageIO.write(burst.get(i), "jpg", bos);
//                        photos.add(bos.toByteArray());
//                    }
                        byte[] gif = Utils.CreateGIF(burst, interval / 2);
                        c.setStatus(CommandStatus.DONE);
                        c.setResult(gif, "burst");
                        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                        response.setProtocol(Protocols.COMMAND.toString());
                        response.setSender(getAID());
                        response.addReceiver(deviceController);
                        response.setContentObject(c);
                        sendMsg(response);


                    } else if (c.getOrder().equals("toggleMotion")) {
                        camera.setDetectMotion(!camera.getDetectMotion());
                        c.setStatus(CommandStatus.DONE);
                        c.setResult("Motion Detection is now: " + (camera.getDetectMotion() ? "on" : "off"), "msg");
                        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                        response.setProtocol(Protocols.COMMAND.toString());
                        response.setSender(getAID());
                        response.addReceiver(deviceController);
                        response.setContentObject(c);
                        sendMsg(response);
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

}
