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
 * RandomNumberMessage.java
 *
 * Created on March 12, 2003, 8:19 PM
 */

package games.strategy.engine.message;

/**
 *
 * @author  Ben Giddings
 * @version 1.0
 */
public class RandomNumberMessage implements Message
{

  public static final int NO_REQUEST   = 0;
  public static final int SEND_TRIPLET = 1;
  public static final int SEND_KEY     = 2;
  public static final int SEND_RANDOM  = 3;

  public int m_request;
  public Object m_obj;


  public RandomNumberMessage(Object in_obj)
  {
    m_request = NO_REQUEST;
    m_obj = in_obj;
  }
  
  public RandomNumberMessage(int in_request, Object in_obj)
  {
    m_request = in_request;
    m_obj = in_obj;
  }

  public String toString()
  {
    StringBuffer returned = new StringBuffer();
    switch (m_request)
    {
    case NO_REQUEST:
      returned.append("NO_REQUEST: ");
      break;
    case SEND_TRIPLET:
      returned.append("SEND_TRIPLET: ");
      break;
    case SEND_KEY:
      returned.append("SEND_KEY: ");
      break;
    case SEND_RANDOM:
      returned.append("SEND_RANDOM: ");
      break;
    default:
      returned.append("BAD REQUEST!!!!!: ");
      break;
    }

    returned.append(m_obj.toString());

    return returned.toString();
  }
  
}
