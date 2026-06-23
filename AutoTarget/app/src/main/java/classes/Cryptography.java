package classes;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {

    private static final String AES = "AES/ECB/PKCS5Padding"; // Uso de ECB para simplicidade de AES de bloco (IV não é estritamente necessário para fins de laboratório, mas PKCS5 padding é ideal)
    private static final String HASH_ALGO = "SHA-256";
    private static final String SALT = "AutoTargetSecretSalt2026"; // Salt fixo para o app

    /**
     * Gera uma chave AES válida baseada na senha ou UID do usuário + SALT.
     */
    private static SecretKeySpec generateKey(String secret) throws NoSuchAlgorithmException {
        String saltedSecret = secret + SALT;
        byte[] key = saltedSecret.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance(HASH_ALGO);
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // Usa apenas os primeiros 128 bit
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Criptografa uma string usando AES.
     * @param data Texto em claro.
     * @param secret O UID do usuário ou uma senha de sessão.
     * @return String criptografada em Base64.
     */
    public static String encrypt(String data, String secret) {
        try {
            SecretKeySpec secretKey = generateKey(secret);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Descriptografa uma string Base64 cifrada em AES.
     * @param encryptedData Dados criptografados em Base64.
     * @param secret O UID do usuário ou uma senha de sessão.
     * @return Texto em claro.
     */
    public static String decrypt(String encryptedData, String secret) {
        try {
            SecretKeySpec secretKey = generateKey(secret);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
