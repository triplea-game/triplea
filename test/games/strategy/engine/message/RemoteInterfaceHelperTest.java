/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.engine.message;

import java.util.Collection;
import java.util.Comparator;

import junit.framework.TestCase;

public class RemoteInterfaceHelperTest extends TestCase
{
    public void testSimple()
    {
        assertEquals("compare", RemoteInterfaceHelper.getMethodInfo(0, Comparator.class).getFirst() );
        
        assertEquals("add", RemoteInterfaceHelper.getMethodInfo(0, Collection.class).getFirst() );
        
        assertEquals(0, RemoteInterfaceHelper.getNumber("add", new Class[] {Object.class}, Collection.class));
        assertEquals(2, RemoteInterfaceHelper.getNumber("clear", new Class[] {}, Collection.class));
    }
}
