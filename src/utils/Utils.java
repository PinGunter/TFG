package utils;

import com.squareup.gifencoder.FloydSteinbergDitherer;
import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.ImageOptions;
import messages.Emergency;
import org.bytedeco.javacpp.Loader;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static void RemoveEmergency(List<Emergency> list, String toRemove) {
        boolean found = false;
        int emToRemove = -1;
        for (int i = 0; i < list.size() && !found; i++) {
            if (list.get(i).getMessage().equals(toRemove)) {
                emToRemove = i;
                found = true;
            }
        }
        if (emToRemove != -1) list.remove(emToRemove);
    }

    public static Emergency FindEmergencyByName(List<Emergency> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getMessage().equals(name)) {
                return list.get(i);
            }
        }
        return null;
    }

    public static String DateToString(Date d) {
        return d.toString().replaceAll(" ", "_").replaceAll(":", "-");
    }

    public static byte[] EncryptObj(byte[] data, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        Cipher cipher = Cipher.getInstance("AES");
        SecretKey originalKey = new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, originalKey);
        byte[] cipherText = cipher.doFinal(data);
        return Base64.getEncoder().encode(cipherText);
    }


    public static byte[] DecryptObj(byte[] data, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        Cipher cipher = Cipher.getInstance("AES");
        SecretKey originalKey = new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");
        cipher.init(Cipher.DECRYPT_MODE, originalKey);
        return cipher.doFinal(Base64.getDecoder().decode(data));
    }

    public static int[][] ImageToRGBArray(BufferedImage img) {
        int[][] rgbArray = new int[img.getHeight()][img.getWidth()];
        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth(); j++) {
                rgbArray[i][j] = img.getRGB(j, i);
            }
        }
        return rgbArray;
    }

    public static byte[] CreateGIF(List<BufferedImage> images, double delay) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageOptions imageOptions = new ImageOptions();
        imageOptions.setDelay((long) delay, TimeUnit.MILLISECONDS);
        imageOptions.setDitherer(FloydSteinbergDitherer.INSTANCE);
        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();

        GifEncoder gifEncoder = new GifEncoder(bos, width, height, 0);
        for (BufferedImage img : images) {
            gifEncoder.addImage(ImageToRGBArray(img), imageOptions);
        }

        gifEncoder.finishEncoding();

        return bos.toByteArray();
    }

    /**
     * reads the file from the path, decrypts it and returns it
     */
    public static Object ReadEncryptedFile(String path, String key) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        byte[] encrypted = Files.readAllBytes(Path.of(path));
        byte[] nocrypt = DecryptObj(encrypted, key);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(nocrypt));
        return ois.readObject();
    }

    /**
     * writes to the path the object encrypted with the key
     */
    public static void WriteEncryptedFile(Serializable data, String path, String key) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(bos);
        objectOutputStream.writeObject(data);
        objectOutputStream.flush();
        byte[] encrypted = EncryptObj(bos.toByteArray(), key);
        Files.write(Path.of(path), encrypted);
    }

    public static String toWav(String path) throws IOException, InterruptedException {
        String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-i", path, path + ".wav");
        System.out.println("CONVIRTIENDO");
        pb.inheritIO().start().waitFor();
        return path + ".wav";
    }

    public static double clamp(double val, double min, double max) {
        return Math.min(max, Math.max(min, val));
    }

    public static int clamp(int val, int min, int max) {
        return Math.min(max, Math.max(min, val));
    }
}
