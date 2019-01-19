/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.netkit.log;

import java.io.PrintStream;
import java.util.Date;

public class ConsoleLogger implements LoggerOutput {

    private PrintStream out;

    public ConsoleLogger(int level, PrintStream out) {
        this.out = out;
    }

    @Override
    public void log(int level, String tag, String msg, Throwable e) {
        out.println(String.format("%s %s [%s] %s - %s", new Date(), LoggerFactory.LEVELS.get(level), tag,
                Thread.currentThread().getName(), msg));
        if (e != null) e.printStackTrace(out);
    }

}
