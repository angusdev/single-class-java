/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// @formatter:off
/**
 * Simple encryption functions for MD5, AES. 
 *  
 * Reference: https://gist.github.com/bricef/2436364
 *            https://forums.bukkit.org/threads/encryption.96731/
 * 
 * @author http://twitter.com/angusdev
 * @version 1.0
 */
// @formatter:on
public class SimpleCrypt {
    private static String AES_IV = "AAAAAAAAAAAAAAAA";
    private static String ENCRYPT_KEY = "0123456789abcdef";

    private static String pad16(String text) {
        String topad = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0";
        if (text.length() % 16 > 0) {
            return text + topad.substring(0, 16 - text.length() % 16);
        }
        else {
            return text;
        }
    }

    private static String unpad16(String text) {
        int pos = text.indexOf('\0');
        if (pos >= 0) {
            return text.substring(0, pos);
        }
        else {
            return text;
        }
    }

    private static String bytesToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    private static byte[] hexToBytes(String text) {
        int len = text.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(text.charAt(i), 16) << 4) + Character.digit(text.charAt(i + 1), 16));
        }
        return data;
    }

    public static String md5(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[32];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md5hash = md.digest();
        return bytesToHex(md5hash);
    }

    public static String saltedMd5(String text, String salt) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        return md5(salt + text);
    }

    public static boolean isMatchMD5(String uid, String password, String salt) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        return password != null && password.equals(saltedMd5(uid, salt));
    }

    public static String aesEncrypt(String plainText, String encryptionKey) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return bytesToHex(aesEncryptToBytes(pad16(plainText), encryptionKey));
    }

    public static String aesDecrypt(String cipherText, String encryptionKey) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return unpad16(aesDecryptBytes(hexToBytes(cipherText), encryptionKey));
    }

    public static byte[] aesEncryptToBytes(String plainText, String encryptionKey) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(AES_IV.getBytes("UTF-8")));
        return cipher.doFinal(plainText.getBytes("UTF-8"));
    }

    public static String aesDecryptBytes(byte[] cipherText, String encryptionKey) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(AES_IV.getBytes("UTF-8")));
        return new String(cipher.doFinal(cipherText), "UTF-8");
    }

    public static void main(String[] args) throws Exception {
        String t = "someplain";
        String m = md5(t);
        String s = SimpleCrypt.saltedMd5(t, ENCRYPT_KEY);
        String a = SimpleCrypt.aesEncrypt(t, ENCRYPT_KEY);
        String u = SimpleCrypt.aesDecrypt(a, ENCRYPT_KEY);
        System.out.println("plain         :" + t);
        System.out.println("md5           :" + m);
        System.out.println("salted md5    :" + s);
        System.out.println("matched md5   :" + SimpleCrypt.isMatchMD5(t, s, ENCRYPT_KEY));
        System.out.println("unmatched md5 :" + SimpleCrypt.isMatchMD5(t, s + "0", ENCRYPT_KEY));
        System.out.println("aes encrypted :" + a);
        System.out.println("aes decrypted :" + u);
        System.out.println("aes matched   :" + t.equals(u));
    }
}