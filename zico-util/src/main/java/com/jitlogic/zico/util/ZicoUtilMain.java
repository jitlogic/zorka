/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zico.util;

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Map;


public class ZicoUtilMain {

    private static final Map<String,ZicoCommand> commands = ZorkaUtil.map(
            "check", new ZicoHostStoreCheckCommand(),
            "help", new ZicoHelpCommand()
    );

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            commands.get("help").run(args);
            return;
        }

        ZicoCommand cmd = commands.get(args[0]);

        if (cmd == null) {
            System.err.println("Unknown command: " + args[0]);
            commands.get("help").run(args);
            return;
        }

        cmd.run(args);
    }

}
