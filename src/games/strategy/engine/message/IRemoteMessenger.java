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

/**
 * Very similar to RMI<br><br>
 * 
 * Objects can be registered with a remote messenger under a public name and
 * declared to implement a particular interface.  Methods on this registered 
 * object can then be called on different vms.<br><br>
 * 
 * To call a method on the registered object, you get a remote reference to it using the 
 * getRemote(...) method.  This returns an object that can be used and called
 * as if it were the original implementor.  <br><br>
 * 
 * The getRemote(...) method though may be called on a different vm than 
 * the registerRemote(...) method.  In that case the calls on the return value of
 * getRemote(...) will be routed to the implementor object passed to registerRemote(...)
 *
 * On VM A you have code like 
 * <pre>
 * 
 * IFoo aFoo = new Foo(); 
 * aRemoteMessenger.registerRemote(IFoo.class, aFoo, "FOO");
 * 
 * </pre>
 * 
 * After this runs, on VM B you can do
 * 
 * <pre>
 *   
 * IFoo someFoo = anotherRemoteMessenger.getRemote("FOO");
 * boolean rVal = someFoo.fee();
 * if(rVal)
 *   ...
 * 
 * </pre>
 * 
 * What will happen is that the fee() call in VM B will be invoked on aFoo in VM A.<br>
 * 
 * This is done using reflection and dynamic proxies (java.lang.reflect.Proxy)<br>
 * 
 * To avoid naming conflicts, it is advised to use the fully qualified java name and 
 * and a constant be used as the channel name.  For example names should be in the form<br>
 * <br><br>
 * "foo.fee.Fi.SomeConstant"
 * <br><br>
 * where SomeConstant is defined in the class foo.fee.Fi<br> 
 * <br>
 * <br>
 * 
 * <p><b>Remotes and threading</b>
 * <p>Remotes are multithreaded.  Method calls may arrive out of order that methods
 * were called.  
 * 
 * @see IRemote, java.lang.reflect.Proxy 
 * 
 * @author Sean Bridges
 */
public interface IRemoteMessenger
{
    /**
     * 
     * @param name the name the remote is registered under.
     * @return a remote reference to the registered remote.
     */
    public IRemote getRemote(String name);
   
    
    /**
     * @param remoteInterface - the remote interface that implementor implements, 
     *                          must be a subclass of IRemote
     * @param implementor - an object that implements remoteInterface
     * @param name - the name that implementor will be registered under
     */
    public void registerRemote(Class remoteInterface, Object implementor, String name);
    
    /**
     * Remove the remote registered under name.
     */
    public void unregisterRemote(String name);
    
    /**
     * Is there a remote registered with the given name 
     */
    public boolean hasRemote(String name);
    
    /**
     * wait for the underlying transport layer to finish transmitting all data queued
     */
    public void flush();
    
    public boolean isServer();

    /**
     * Wait for a remote to be visible to this IRemoteMessenger.
     * IRemote registered in one vm will not be instantly visible to 
     * all vms
     * 
     * @param name the remote name
     * @param timeout if -1, means wait forever
     */
    public void waitForRemote(String name, long timeoutMS);
    
}
