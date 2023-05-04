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
            if (webcam.open(true)) {
                detectMotion = enable;
                detector = new WebcamMotionDetector(webcam);
                onMotion = onMotionEvent;
                detector.setInterval(interval);
                detector.setPixelThreshold(100);
                detector.addMotionListener(this);
                detector.setPixelThreshold(50);
                if (enable) startDetection();
            }

        }
    }

    public void stopDetection() {
        if (webcam.isOpen()) {
            detector.stop();
            detectMotion = false;
        }
    }

    public void startDetection() {
        if (webcam.isOpen()) {
            detector.start();
            detectMotion = true;
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

    public ArrayList<BufferedImage> startBurst(int n, double frequency) {
        ArrayList<BufferedImage> burst = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            try {
                BufferedImage image = webcam.getImage();
                burst.add(image);
                Thread.sleep((long) frequency);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return burst;
    }

    @Override
    public void motionDetected(WebcamMotionEvent webcamMotionEvent) {
        if (detectMotion) onMotion.accept(webcamMotionEvent);
    }

    public boolean getDetectMotion() {
        return detectMotion;
    }

    public void setDetectMotion(boolean detectMotion) {
        this.detectMotion = detectMotion;
    }
}
