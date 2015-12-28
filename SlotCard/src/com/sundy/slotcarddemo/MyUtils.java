package com.sundy.slotcarddemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Locale;

/**
 * Created by sundy on 15/12/23.
 */
public class MyUtils {

    /**
     * Decrypts the data using AES.
     *
     * @param key   the key.
     * @param input the input buffer.
     * @return the output buffer.
     * @throws GeneralSecurityException if there is an error in the decryption process.
     */
    public static byte[] aesDecrypt(byte key[], byte[] input)
            throws GeneralSecurityException {

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(new byte[16]);

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        return cipher.doFinal(input);
    }

    /**
     * Shows the message dialog.
     *
     * @param messageId the message ID.
     */
    public static void showMessageDialog(Context mContext, int messageId) {
        try {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_ok, null);
            final Dialog dialog = new Dialog(mContext, R.style.dialog);
            dialog.setContentView(view);
            TextView txt_msg = (TextView) view.findViewById(R.id.txt_msg);
            txt_msg.setText(messageId);
            Button btn_done = (Button) view.findViewById(R.id.btn_done);
            btn_done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.cancel();
                    dialog.dismiss();
                }
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Concatenates two strings with carriage return.
     *
     * @param string1 the first string.
     * @param string2 the second string.
     * @return the combined string.
     */
    public static String concatString(String string1, String string2) {

        String ret = string1;

        if ((string1.length() > 0) && (string2.length() > 0)) {
            ret += "\n";
        }

        ret += string2;

        return ret;
    }

    /**
     * Decrypts the data using Triple DES.
     *
     * @param key   the key.
     * @param input the input buffer.
     * @return the output buffer.
     * @throws GeneralSecurityException if there is an error in the decryption process.
     */
    public static byte[] tripleDesDecrypt(byte[] key, byte[] input)
            throws GeneralSecurityException {

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "DESede");
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(new byte[8]);

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        return cipher.doFinal(input);
    }

    /**
     * Converts the byte array to HEX string.
     *
     * @param buffer the buffer.
     * @return the HEX string.
     */
    public static String toHexString(byte[] buffer) {

        String bufferString = "";

        if (buffer != null) {

            for (int i = 0; i < buffer.length; i++) {

                String hexChar = Integer.toHexString(buffer[i] & 0xFF);
                if (hexChar.length() == 1) {
                    hexChar = "0" + hexChar;
                }

                bufferString += hexChar.toUpperCase(Locale.US) + " ";
            }
        }

        return bufferString;
    }

    /**
     * Converts the integer to HEX string.
     *
     * @param i the integer.
     * @return the HEX string.
     */
    public static String toHexString(int i) {

        String hexString = Integer.toHexString(i);

        if (hexString.length() % 2 == 1) {
            hexString = "0" + hexString;
        }

        return hexString.toUpperCase(Locale.US);
    }

    /**
     * Converts the HEX string to byte array.
     *
     * @param hexString the HEX string.
     * @return the number of bytes.
     */
    public static int toByteArray(String hexString, byte[] byteArray) {

        char c = 0;
        boolean first = true;
        int length = 0;
        int value = 0;
        int i = 0;

        for (i = 0; i < hexString.length(); i++) {

            c = hexString.charAt(i);
            if ((c >= '0') && (c <= '9')) {
                value = c - '0';
            } else if ((c >= 'A') && (c <= 'F')) {
                value = c - 'A' + 10;
            } else if ((c >= 'a') && (c <= 'f')) {
                value = c - 'a' + 10;
            } else {
                value = -1;
            }

            if (value >= 0) {

                if (first) {

                    byteArray[length] = (byte) (value << 4);

                } else {

                    byteArray[length] |= value;
                    length++;
                }

                first = !first;
            }

            if (length >= byteArray.length) {
                break;
            }
        }

        return length;
    }

    public static String formatCard(String s) {
        char[] Str = s.toCharArray();
        String code = "";
        for (int i = 0; i < Str.length; i++) {
            code += Str[i];
            if (((i + 1) % 4 == 0) && i != Str.length - 1)
                code += "  ";
        }

        return code;
    }


}
