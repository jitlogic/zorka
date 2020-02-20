package com.jitlogic.zorka.util;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.UtilLib;

import static java.lang.System.err;

public class CryptCommand {

    public static void help(String msg) {
        if (msg != null) err.println(msg);
        err.println("Available commands: ");
        err.println(" register [-o <output-file>] - register agent");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            help("Missing subcommand.");
            System.exit(1);
        } else if ("genkey".equalsIgnoreCase(args[1])) {
            System.out.println(ZorkaUtil.generateKey());
        } else if ("passwd".equalsIgnoreCase(args[1])) {
            String s1 = new String(System.console().readPassword("Enter password:"));
            String s2 = new String(System.console().readPassword("Repeat password:"));
            if (!s1.equals(s2)) {
                err.println("Passwords do not match.");
                System.exit(1);
            }
            System.out.println(new UtilLib().pwdenc(s1));
        } else if ("decrypt".equalsIgnoreCase(args[1])) {
            String s = System.console().readLine("Enter ecrypted data (with 'ENC:' prefix):");
            System.out.println(new UtilLib().pwddec(s));
        } else {
            help("Unknown subcommand: " + args[1]);
            System.exit(1);
        }
    }
}
