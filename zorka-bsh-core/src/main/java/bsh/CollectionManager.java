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

package bsh;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.lang.reflect.Array;

/**
	The default CollectionManager
	supports iteration over objects of type:
	Enumeration, Iterator, Iterable, CharSequence, and array.
*/
public final class CollectionManager
{
	private static final CollectionManager manager = new CollectionManager();

	public synchronized static CollectionManager getCollectionManager()
	{
		return manager;
	}

	/**
	*/
	public boolean isBshIterable( Object obj ) 
	{
		// This could be smarter...
		try { 
			getBshIterator( obj ); 
			return true;
		} catch( IllegalArgumentException e ) { 
			return false;
		}
	}

	public Iterator getBshIterator( Object obj ) 
		throws IllegalArgumentException
	{
		if(obj==null)
			throw new NullPointerException("Cannot iterate over null.");

		if (obj instanceof Enumeration) {
			final Enumeration enumeration = (Enumeration)obj;
			return new Iterator<Object>() {
				public boolean hasNext() {
					return enumeration.hasMoreElements();
				}
				public Object next() {
					return enumeration.nextElement();
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		if (obj instanceof Iterator)
			return (Iterator)obj;

		if (obj instanceof Iterable)
			return ((Iterable)obj).iterator();

		if (obj.getClass().isArray()) {
			final Object array = obj;
			return new Iterator() {
				private int index = 0;
				private final int length = Array.getLength(array);

				public boolean hasNext() {
					return index < length;
				}
				public Object next() {
					return Array.get(array, index++);
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		} 
		
		if (obj instanceof CharSequence)
			return getBshIterator(
				obj.toString().toCharArray());

		throw new IllegalArgumentException(
			"Cannot iterate over object of type "+obj.getClass());
	}

	public boolean isMap( Object obj ) {
		return obj instanceof Map;
	}

	public Object getFromMap( Object map, Object key ) {
		return ((Map)map).get(key);
	}

	public Object putInMap( Object map, Object key, Object value ) 
	{
		return ((Map)map).put(key, value);
	}

}
