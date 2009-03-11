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
 * @author Sean Bridges
 * 
 * A marker interface, used to indicate that the interface
 * can be used by IChannelMessenger
 * 
 * All arguments to all methods of an IChannelSubscriber 
 * must be serializable, since the methods
 * may be called by a remote VM.
 * 
 * Return values of an IChannelSubscriber will be ignored.
 * 
 * Exceptions thrown by methods of an IChannelSubscriber will
 * be printed to standard error, but otherwise ignored.
 * 
 * Arguments to the methods of IChannelSubscribor should not be modified 
 * in any way.  The values may be used in method calls to other 
 * subscribors.
 * 
 */
public interface IChannelSubscribor
{

}
