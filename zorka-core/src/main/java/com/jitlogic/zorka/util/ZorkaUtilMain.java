package com.jitlogic.zorka.util;

import com.jitlogic.zorka.util.ztx.ZtxProcCommand;

import java.io.IOException;

import static java.lang.System.err;

public class ZorkaUtilMain {

    public static void help(String msg) {
        err.println(msg);
        err.println("Available commands: ");
        err.println(" ztx <options> - process and filter ZTX files");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            help("Missing command.");
        } else if ("ztx".equalsIgnoreCase(args[0])) {
            ZtxProcCommand.main(args);
        } else if ("-h".equals(args[0])) {
            help("");
        } else {
            help("Unknown command: " + args[0]);
        }
    }

}
