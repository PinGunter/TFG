package device.microphone;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Microphone {
    TargetDataLine targetLine;
    Thread audioRecorderThread;

    public boolean supportsRecording() {
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);

        Line.Info dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        return AudioSystem.isLineSupported(dataInfo);
    }

    public void startRecording(String path) {
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);

        Line.Info dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        try {
            targetLine = (TargetDataLine) AudioSystem.getLine(dataInfo);
            targetLine.open();
            targetLine.start();


            audioRecorderThread = new Thread(() -> {
                AudioInputStream recordingStream = new AudioInputStream(targetLine);
                File output = new File(path + ".wav");
                try {
                    AudioSystem.write(recordingStream, AudioFileFormat.Type.WAVE, output);
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            });


            audioRecorderThread.start();
        } catch (LineUnavailableException e) {
            System.err.println(e.getMessage());
        }


    }

    public void stopRecording() {
        targetLine.stop();
        targetLine.close();
    }
}
