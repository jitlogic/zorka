/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.util;

import com.jitlogic.zorka.util.ztx.ZtxProcCommand;
import com.jitlogic.zorka.util.ztx.ZtxZicoCommand;

import java.io.IOException;

import static java.lang.System.err;

public class ZorkaUtilMain {

    public static void help(String msg) {
        if (msg != null) err.println(msg);
        err.println("Available commands: ");
        err.println(" ztx <args> - process and filter ZTX files");
        err.println(" zico <args> - ZICO collector connection handling");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            help("Missing command.");
        } else if ("crypt".equalsIgnoreCase(args[0])) {
            CryptCommand.main(args);
        } else if ("ztx".equalsIgnoreCase(args[0])) {
            ZtxProcCommand.main(args);
        } else if ("zico".equalsIgnoreCase(args[0])) {
            ZtxZicoCommand.main(args);
        } else if ("help".equals(args[0])) {
            help(null);
        } else {
            help("Unknown command: " + args[0]);
            System.exit(1);
        }
    }

}
