/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import static java.lang.System.err;

public class ZtxZicoCommand {

    public static void help(String msg) {
        if (msg != null) err.println(msg);
        err.println("Available commands: ");
        err.println(" register [-o <output-file>] - register agent");
    }

    public static void main(String[] args) {
        if ("help".equals(args[1])) {
            help(null);
        } else {
            help("Unknown zico subcommand: " + args[1]);
            System.exit(1);
        }
    }
    
}
