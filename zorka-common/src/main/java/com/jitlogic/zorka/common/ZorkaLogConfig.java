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

package com.jitlogic.zorka.common;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Maintains zorka log filtering configuration. It allows for
 * fine tuning of which things are to be logged (useful when
 * developing/debugging more sophiscated configuration scripts.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaLogConfig {

    /** Logger object */
    private static final ZorkaLog log = ZorkaLogger.getLog(ZorkaLogConfig.class);


}
