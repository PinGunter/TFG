package device.microphone;

import utils.Utils;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Date;

public class Microphone {
    TargetDataLine targetLine;
    Thread audioRecorderThread;

    File output;

    public boolean supportsRecording() {
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);

        Line.Info dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        return AudioSystem.isLineSupported(dataInfo);
    }

    public void startRecording() {
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 2, 4, 16000, false);

        Line.Info dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        try {
            targetLine = (TargetDataLine) AudioSystem.getLine(dataInfo);
            targetLine.open();
            targetLine.start();


            audioRecorderThread = new Thread(() -> {
                AudioInputStream recordingStream = new AudioInputStream(targetLine);
                output = new File("temp/" + Utils.DateToString(Date.from(Instant.now())) + ".wav");
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

    public byte[] stopRecording() {
        targetLine.stop();
        targetLine.close();
        try {
            byte[] audio = Files.readAllBytes(output.toPath());
            if (output.delete()) {
                return audio;
            } else return null;
        } catch (IOException e) {
            return null;
        }
    }
}
