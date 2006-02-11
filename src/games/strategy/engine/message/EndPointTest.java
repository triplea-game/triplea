/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package games.strategy.engine.message;

import java.util.List;

import junit.framework.TestCase;

/**
 * @author Sean Bridges
 */
public class EndPointTest extends TestCase
{
    public void testEndPoint()
    {
        EndPoint endPoint = new EndPoint("", new Class[] {String.class}, false, null);
        endPoint.addImplementor("test");
        RemoteMethodCall call = new RemoteMethodCall("", "toString", new Object[0], new Class[0]);
        List results = endPoint.invokeLocal(call, endPoint.takeANumber());
        assertEquals(results.size(), 1);
        assertEquals("test", ((RemoteMethodCallResults) results.iterator().next()).getRVal() );    
    }
}
