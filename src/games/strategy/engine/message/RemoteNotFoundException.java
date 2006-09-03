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
 * No Remote could be found.<p>
 * 
 * This can be thrown by the remote messenger in two cases,<p>
 * 
 * 1) looking up a someRemoteMessenger.getRemote(...)<br>
 * 2) invoking a method on the object returned by someRemoteMessenger.getRemote(...).<p>
 * 
 * There are two possibel causes.  Either the remote never existed, or a remote was once
 * bound to that name, but is no longer bound. 
 * 
 * @author Sean Bridges
 */
public class RemoteNotFoundException extends MessengerException
{

    public RemoteNotFoundException(String string)
    {
        super(string, new IllegalStateException("remote not found"));
    }

}
