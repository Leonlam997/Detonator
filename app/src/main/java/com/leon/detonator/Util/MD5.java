package com.leon.detonator.Util;

import com.leon.detonator.Base.BaseApplication;

import java.security.MessageDigest;

public class MD5 {
    public static String encryptTo16BitString(String s) {
        return encrypt(s, 16);
    }

    public static String encryptTo32BitString(String s) {
        return encrypt(s, 32);
    }

    private static String encrypt(String s, int bit) {
        try {
            char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            byte[] btInput = s.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;

            char[] str;
            int k = 0;
            if (bit == 16) {
                str = new char[j];
                for (int i = 4; i < 12; i++) {
                    str[k++] = hexDigits[md[i] >>> 4 & 0xf];
                    str[k++] = hexDigits[md[i] & 0xf];
                }
            } else {
                str = new char[j * 2];
                for (byte b : md) {
                    str[k++] = hexDigits[b >>> 4 & 0xf];
                    str[k++] = hexDigits[b & 0xf];
                }
            }
            return new String(str);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            return "";
        }
    }
}
