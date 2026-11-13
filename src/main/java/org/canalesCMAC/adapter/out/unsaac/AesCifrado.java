package org.canalesCMAC.adapter.out.unsaac;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado compatible con aes-everywhere (OpenSSL "Salted__" + AES-256-CBC).
 */
public final class AesCifrado {

    private static final String CABECERA = "Salted__";
    private static final int LONGITUD_SAL = 8;
    private static final int LONGITUD_CLAVE = 32;
    private static final int LONGITUD_IV = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private AesCifrado() {
    }

    public static String cifrar(String texto, String clave) {
        try {
            byte[] sal = new byte[LONGITUD_SAL];
            ALEATORIO.nextBytes(sal);
            byte[][] claveIv = derivar(clave.getBytes(StandardCharsets.UTF_8), sal);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(claveIv[0], "AES"),
                new IvParameterSpec(claveIv[1]));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] salida = new byte[CABECERA.length() + sal.length + cifrado.length];
            System.arraycopy(CABECERA.getBytes(StandardCharsets.US_ASCII), 0, salida, 0, CABECERA.length());
            System.arraycopy(sal, 0, salida, CABECERA.length(), sal.length);
            System.arraycopy(cifrado, 0, salida, CABECERA.length() + sal.length, cifrado.length);
            return Base64.getEncoder().encodeToString(salida);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo cifrar el contenido", e);
        }
    }

    public static String descifrar(String contenido, String clave) {
        try {
            byte[] datos = Base64.getDecoder().decode(contenido);
            if (datos.length < CABECERA.length() + LONGITUD_SAL + LONGITUD_IV) {
                throw new IllegalArgumentException("Contenido cifrado demasiado corto");
            }
            String cabecera = new String(datos, 0, CABECERA.length(), StandardCharsets.US_ASCII);
            if (!CABECERA.equals(cabecera)) {
                throw new IllegalArgumentException("El contenido no tiene el formato Salted__");
            }
            byte[] sal = new byte[LONGITUD_SAL];
            System.arraycopy(datos, CABECERA.length(), sal, 0, LONGITUD_SAL);
            byte[] cifrado = new byte[datos.length - CABECERA.length() - LONGITUD_SAL];
            System.arraycopy(datos, CABECERA.length() + LONGITUD_SAL, cifrado, 0, cifrado.length);
            byte[][] claveIv = derivar(clave.getBytes(StandardCharsets.UTF_8), sal);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(claveIv[0], "AES"),
                new IvParameterSpec(claveIv[1]));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo descifrar el contenido", e);
        }
    }

    private static byte[][] derivar(byte[] clave, byte[] sal) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] material = new byte[LONGITUD_CLAVE + LONGITUD_IV];
        byte[] anterior = new byte[0];
        int offset = 0;
        while (offset < material.length) {
            md.reset();
            md.update(anterior);
            md.update(clave);
            md.update(sal);
            anterior = md.digest();
            int copiar = Math.min(anterior.length, material.length - offset);
            System.arraycopy(anterior, 0, material, offset, copiar);
            offset += copiar;
        }
        byte[] claveFinal = new byte[LONGITUD_CLAVE];
        byte[] iv = new byte[LONGITUD_IV];
        System.arraycopy(material, 0, claveFinal, 0, LONGITUD_CLAVE);
        System.arraycopy(material, LONGITUD_CLAVE, iv, 0, LONGITUD_IV);
        return new byte[][] { claveFinal, iv };
    }
}
