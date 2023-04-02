package device.speakers;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Speakers {
    Clip clip;

    public void playSound(String path) {
        File file = new File(path);
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

}
