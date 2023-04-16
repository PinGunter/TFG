package utils;

import messages.Emergency;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

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

}
