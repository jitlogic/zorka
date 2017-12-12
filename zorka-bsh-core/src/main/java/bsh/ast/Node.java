/**
 *
 * This file is a part of ZOOLA - an extensible BeanShell implementation.
 * Zoola is based on original BeanShell code created by Pat Niemeyer.
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

package bsh.ast;

import bsh.BshNodeVisitor;

/*
    All BSH nodes must implement this interface.  It provides basic
    machinery for constructing the parent and child relationships
    between nodes.
*/
public interface Node extends java.io.Serializable
{
/**
	This method is called after the node has been made the current
	node.  It indicates that child nodes can now be added to it.
*/
	public void jjtOpen();

/**
	This method is called after all the child nodes have been
	added.
*/
	public void jjtClose();

/**
	This pair of methods are used to inform the node of its
	parent.
*/
	public void jjtSetParent(Node n);
	public Node jjtGetParent();

/**
	This method tells the node to add its argument to the node's
	list of children.
*/
	public void jjtAddChild(Node n, int i);

/**
	This method returns a child node.  The children are numbered
	from zero, left to right.
*/
	public Node jjtGetChild(int i);

/**
	Return the number of children the node has.
*/
	public int jjtGetNumChildren();

    public <T> T accept(BshNodeVisitor<T> visitor);
}

