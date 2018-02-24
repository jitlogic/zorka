/**
 *
 * This file is a part of ZOOLA - an extensible BeanShell implementation.
 * Zoola is based on original BeanShell code created by Pat Niemeyer.
 *
 * Portions created as a part of beanshell fork at
 * http://code.google.com/p/beanshell2 and are licensed under GNU Lesser GPL.
 *
 * Original BeanShell code is Copyright (C) 2000 Pat Niemeyer <pat@pat.net>.
 *
 * New portions are Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZOOLA. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package bsh;

import bsh.ast.SimpleNode;

/**
 * An internal error in the interpreter has occurred.
 */
public class InterpreterError extends RuntimeException {

	public InterpreterError(final String s) {
		super(s);
	}

    public InterpreterError(final String s, final SimpleNode node) {
        super(s + " (at " + node.getSourceFile() + ":" + node.getLineNumber() + "\n" + node.getText());
    }

	public InterpreterError(final String s, final Throwable cause) {
		super(s, cause);
	}


}

