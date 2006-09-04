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

import games.strategy.net.GUID;
import games.strategy.net.INode;
import games.strategy.net.Node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class SpokeInvoke extends Invoke
{
    
  private INode m_invoker;

  public SpokeInvoke()
  {
      super();
  }

  public SpokeInvoke(GUID methodCallID, boolean needReturnValues, RemoteMethodCall call, INode invoker)
  {
      super(methodCallID, needReturnValues, call);
      m_invoker = invoker;
  }
  
  public INode getInvoker()
  {
      return m_invoker;
  }
  
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
  {
      super.readExternal(in);
      m_invoker = new Node();
      m_invoker.readExternal(in);
      
  }

  public void writeExternal(ObjectOutput out) throws IOException
  {
      super.writeExternal(out);
      m_invoker.writeExternal(out);
  }
  
  
}