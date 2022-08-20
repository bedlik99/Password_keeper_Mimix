package Util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;

public class FileEncrypterDecrypter {

    private SecretKey secretKey;
    private Cipher cipher;
    private byte[] iv;
    private String salt;

    public FileEncrypterDecrypter(String secretPassword, byte[] iv, String salt)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException {
        this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        this.iv = iv;
        this.salt = salt;
        this.secretKey = getKeyFromPassword(secretPassword);
    }

    public boolean encryptAndEncodeFile(String content, String filePathString) {
        if (!new File(filePathString).exists()) return false;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        } catch (Exception e) {
            return false;
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePathString);
             CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
            cipherOut.write(content.getBytes());
            cipherOut.flush();
        } catch (Exception e) {
            rollbackInitialFileData(filePathString, content.getBytes());
            return false;
        }

        byte[] encContent;
        try (FileInputStream fileIn = new FileInputStream(filePathString)) {
            encContent = new byte[fileIn.available()];
            fileIn.read(encContent);
        } catch (Exception e) {
            rollbackInitialFileData(filePathString, content.getBytes());
            return false;
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePathString)) {
            fileOut.write(Base64.getEncoder().encode(encContent));
            fileOut.flush();
        } catch (Exception e) {
            rollbackInitialFileData(filePathString, content.getBytes());
        }
        return true;
    }

    public boolean decodeAndDecryptFile(String filePathString) {
        if (!new File(filePathString).exists()) return false;
        String content;
        byte[] encContent, initialContentCopy;
        try (FileInputStream fileIn = new FileInputStream(filePathString)) {
            encContent = new byte[fileIn.available()];
            fileIn.read(encContent);
        } catch (Exception e) {
            return false;
        }
        initialContentCopy = encContent;

        try (FileOutputStream fileOut = new FileOutputStream(filePathString)) {
            fileOut.write(Base64.getDecoder().decode(encContent));
            fileOut.flush();
        } catch (Exception e) {
            rollbackInitialFileData(filePathString, initialContentCopy);
            return false;
        }

        try (FileInputStream fileIn = new FileInputStream(filePathString)) {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            try (CipherInputStream cipherIn = new CipherInputStream(fileIn, cipher);
                 InputStreamReader inputReader = new InputStreamReader(cipherIn);
                 BufferedReader reader = new BufferedReader(inputReader)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                content = sb.toString();
            }
        } catch (Exception e) {
            rollbackInitialFileData(filePathString, initialContentCopy);
            return false;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePathString))) {
            bw.write(content);
            bw.flush();
        } catch (Exception e) {
            rollbackInitialFileData(filePathString, initialContentCopy);
            return false;
        }
        return true;
    }

    private void rollbackInitialFileData(String filePathString, byte[] initialContentCopy) {
        try (FileOutputStream fileOut = new FileOutputStream(filePathString)) {
            fileOut.write(initialContentCopy);
            fileOut.flush();
        } catch (Exception ignored) {
        }
    }

    private SecretKey getKeyFromPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8),
                65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public static String generateRandomText(int textLength) {
        String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder result = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < textLength; i++) {
            char randomChar = base64Chars.charAt(random.nextInt(64));
            result.append(randomChar);
        }
        return result.toString();
    }

    public void clearSecrets() {
        this.cipher = null;
        this.iv = null;
        this.salt = null;
        this.secretKey = null;
    }

    public String getSalt() {
        return salt;
    }
}
