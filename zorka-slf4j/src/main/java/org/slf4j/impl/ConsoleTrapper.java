package org.slf4j.impl;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConsoleTrapper implements ZorkaTrapper {

    private static String join(String sep, Object... vals) {
        StringBuilder sb = new StringBuilder();

        for (Object val : vals) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(""+val);
        }

        return sb.toString();
    }

    private static String format(String message, Object... args) {
        if (args.length == 0) {
            return message;
        } else {
            try {
                return String.format(message, args);
            } catch (Exception e) {
                return "Invalid format '" + message + "' [" + join(",", args) + "]: " + e;
            }
        }
    }

    @Override
    public void trap(ZorkaLogLevel logLevel, String tag, String message, Throwable e, Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        sb.append(" ");
        sb.append(logLevel);
        sb.append(" ");
        sb.append(tag);
        sb.append(" ");
        sb.append(format(message, args));

        if (e != null) {
            sb.append(" [");
            sb.append(e.toString());
            sb.append("]");

            StringWriter sw = new StringWriter(512);
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            sb.append("\n");
            sb.append(sw.toString());
        }

        System.out.println(sb.toString());
    }
}
