package com.jitlogic.netkit.util;

import java.nio.charset.Charset;

public class TextUtil {

    public static final byte CR = 13; // \r
    public static final byte LF = 10; // \n
    public static final byte SP = 32;
    public static final byte COLON = 58;

    public static final Charset ASCII = Charset.forName("US-ASCII");
    public static final Charset UTF_8 = Charset.forName("utf8");


    public static String camelCase(String key) {
        StringBuilder sb = new StringBuilder(key.length());
        boolean upper = true;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (upper) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
            upper = c == '-';
        }
        return sb.toString();
    }

}
