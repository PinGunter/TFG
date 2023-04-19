package device.camera;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamMotionDetector;
import com.github.sarxos.webcam.WebcamMotionEvent;
import com.github.sarxos.webcam.WebcamMotionListener;
import utils.Timeout;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Camera implements WebcamMotionListener {
    Webcam webcam;
    boolean detectMotion;
    WebcamMotionDetector detector;

    Consumer<WebcamMotionEvent> onMotion;

    Timeout timeout;

    public Camera(boolean enable, Consumer<WebcamMotionEvent> onMotionEvent, int interval) {
        timeout = new Timeout();
        webcam = Webcam.getDefault();
        if (webcam != null) {
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open(true);
            if (enable) {
                detectMotion = true;
                detector = new WebcamMotionDetector(webcam);
                onMotion = onMotionEvent;
                detector.setInterval(interval);
                detector.addMotionListener(this);
                detector.setPixelThreshold(50);
            }
        }
    }

    public void stopDetection() {
        if (detectMotion) {
            detector.stop();
        }
    }

    public void startDetection() {
        if (detectMotion) {
            detector.start();
        }
    }

    public byte[] getImage() throws IOException {
        if (webcam != null) {
            BufferedImage image = webcam.getImage();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", bos);
            return bos.toByteArray();
        }
        return null;
    }

    public ArrayList<BufferedImage> startBurst(int n, int frequency) {
        ArrayList<BufferedImage> burst = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            try {
                BufferedImage image = webcam.getImage();
                burst.add(image);
                ImageIO.write(image, "jpg", new File("temp/" + i + ".jpg"));
                Thread.sleep(frequency);

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = 0; i < n; i++) {
            try {
                ImageIO.write(burst.get(i), "jpg", new File("temp/burst" + i + ".jpg"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return burst;
    }

    @Override
    public void motionDetected(WebcamMotionEvent webcamMotionEvent) {
        System.out.println("MOTION====================");
        onMotion.accept(webcamMotionEvent);
    }

}
