/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.util.ztx;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.AgentConfig;
import com.jitlogic.zorka.core.spy.ZicoHttpOutput;
import com.jitlogic.zorka.core.spy.stracer.STraceHttpOutput;

import java.io.*;

import static java.lang.System.err;

public class ZtxZicoCommand {

    private static final String AGENT_UUID = "tracer.net.agent.uuid";
    private static final String SESSN_KEY = "tracer.net.sessn.key";


    public static void help(String msg) {
        if (msg != null) err.println(msg);
        err.println("Available commands: ");
        err.println(" register [-o <output-file>] - register agent");
    }


    public static void register(String[] args) {
        String agentHome = System.getProperty("zorka.home", ".");
        File confFile = new File(agentHome, "zorka.properties");
        if (!confFile.exists()) {
            err.println("File " + confFile + " not found.");
            err.println("Please run this command from agent home directory or use -Dzorka.home property");
            System.exit(1);
        }

        File outf = null;
        boolean force = false;

        for (int i = 2; i < args.length-1; i++) {
            if ("-o".equals(args[i])) {
                outf = new File(args[i+1]);
                i++;
            }
            if ("-f".equals(args[i])) {
                force = true;
            }
        }

        if (outf != null && outf.exists() && force) {
            err.println("File " + outf + " exists. Remove it first or use -f option.");
            System.exit(1);
        }

        AgentConfig cfg = new AgentConfig(agentHome);
        if (cfg.hasCfg(AGENT_UUID) && cfg.hasCfg(SESSN_KEY) && force) {
            err.println("Agent already registered. Skipping.");
            return;
        }

        cfg.setPersistent(false);

        ZicoHttpOutput output = new STraceHttpOutput(cfg,
                cfg.mapCfg("tracer.net", "hostname", cfg.stringCfg("zorka.hostname", null)),
                new SymbolRegistry());
        output.register();

        if (outf == null) {
            System.out.println(AGENT_UUID + "=" + cfg.get(AGENT_UUID));
            System.out.println(SESSN_KEY + "=" + cfg.get(SESSN_KEY));
        }

        PrintStream ps = null;
        try {
            ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(outf)));
            ps.println(AGENT_UUID + "=" + cfg.get(AGENT_UUID));
            ps.println(SESSN_KEY + "=" + cfg.get(SESSN_KEY));
            err.println("Registration succesfull. Credentials written to " + outf);
        } catch (IOException e) {
            err.println("Error writing file " + outf + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            ZorkaUtil.close(ps);
        }
    }


    public static void main(String[] args) {
        if ("register".equals(args[1])) {
            register(args);
        } else if ("help".equals(args[1])) {
            help(null);
        } else {
            help("Unknown zico subcommand: " + args[1]);
            System.exit(1);
        }
    }
    
}
