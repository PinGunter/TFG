package utils;

import com.squareup.gifencoder.FloydSteinbergDitherer;
import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.ImageOptions;
import messages.Emergency;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public static byte[] CreateGIF(List<BufferedImage> images, int delay) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageOptions imageOptions = new ImageOptions();
        imageOptions.setDelay(delay, TimeUnit.MILLISECONDS);
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

}
