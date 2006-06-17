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

/*
 * Node.java
 *
 * Created on December 11, 2001, 8:13 PM
 */

package games.strategy.net;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */

//written very often over the network, so make externalizable to make faster and reduce traffic
public class Node implements INode, Externalizable
{
  static final long serialVersionUID = -2908980662926959943L;
    
  private String m_name;
  private int m_port;
  private InetAddress m_address;

  //needed to support Externalizable
  public Node()
  {

  }

  /** Creates new Node */
  public Node(String name, InetAddress address, int port)
  {
    m_name = name;
    m_address = address;
    m_port = port;
  }

  public String getName()
  {
    return m_name;
  }
  
  /**
   * Node equality is done based on network adress/port.
   * The name is not part of the node identity. 
   */
  public boolean equals(Object obj)
  {
    if(obj == this)
        return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Node))
      return false;

    Node other = (Node) obj;

    return other.m_port == this.m_port
        && other.m_address.equals(this.m_address);

  }

  public int hashCode()
  {
    return  (37 * m_port) +  m_address.hashCode();
  }

  public String toString()
  {
    return m_name + " port:" + m_port + " ip:" + m_address.getHostAddress();
  }

  public int getPort()
  {
    return m_port;
  }

  public InetAddress getAddress()
  {
    return m_address;
  }

  public void readExternal(ObjectInput in) throws IOException,
      ClassNotFoundException
  {
    m_name = (String) in.readObject();
    m_port = in.readInt();
    m_address = (InetAddress) in.readObject();
  }

  public void writeExternal(ObjectOutput out) throws IOException
  {
    out.writeObject(m_name);
    out.writeInt(m_port);
    out.writeObject(m_address);
  }

}