/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.common.util;

/**
 */
public enum ZorkaLogLevel {

    TRACE(0, 7),
    DEBUG(1, 7),
    INFO(2, 6),
    WARN(3, 4),
    ERROR(4, 3),
    FATAL(5, 0);

    private final int priority;
    private final int severity;

    private ZorkaLogLevel(int priority, int severity) {
        this.priority = priority;
        this.severity = severity;
    }

    public int getPriority() {
        return priority;
    }

    public int getSeverity() {
        return severity;
    }
}
