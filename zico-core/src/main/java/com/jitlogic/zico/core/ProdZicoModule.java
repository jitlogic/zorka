/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zico.core;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.io.File;


public class ProdZicoModule extends AbstractZicoModule {

    @Provides
    @Singleton
    public ZicoConfig provideConfig() {
        String homeDir = System.getProperty("zico.home.dir");

        if (homeDir == null) {
            throw new RuntimeException("Missing home dir configuration property. " +
                    "Add '-Dzico.home.dir=/path/to/zico/home' to JVM options.");
        }

        if (!new File(homeDir).isDirectory()) {
            throw new RuntimeException("Home dir property does not point to a directory.");
        }

        return new ZicoConfig(homeDir);
    }


}
