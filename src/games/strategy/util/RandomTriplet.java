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
 * RandomTriplet.java
 *
 * Created on March 12, 2003, 8:19 PM
 */

package games.strategy.util;

/**
 *
 * @author  Ben Giddings
 * @version 1.0
 */
public class RandomTriplet implements java.io.Serializable
{
  public final byte[] m_encrypted_random;
  public final byte[] m_encrypted_known;
  public final Integer m_max_num;
  private final String m_annotation;
  private final int m_count;
  


  public RandomTriplet(byte[] enc_rnd, byte[] enc_known, Integer in_max_num, int count, String annotation)
  {
    if(count < 0)
        throw new IllegalArgumentException("Invalid count:" + count);
    if(annotation == null || annotation.length() == 0)
        throw new IllegalArgumentException("No annotation");
      
    m_encrypted_random = enc_rnd;
    m_encrypted_known = enc_known;
    m_max_num = in_max_num;
    m_count = count;
    m_annotation = annotation;
  }

  public String toString()
  {
    StringBuffer buff = new StringBuffer("RandomTriplet: <");

    buff.append("Encrypted Random: '");
    for (int i=0; i<m_encrypted_random.length; i++)
    {
      buff.append(Integer.toHexString((int)m_encrypted_random[i]));
    }
    buff.append("', ");

    buff.append("Encrypted Known: '");
    for (int i=0; i<m_encrypted_known.length; i++)
    {
      buff.append(Integer.toHexString((int)m_encrypted_known[i]));
    }
    buff.append("', ");

    if (null != m_max_num)
    {
      buff.append("Max Num: " + m_max_num.toString());
    }

    buff.append(">");

    return buff.toString();
  }
  
  public String getAnnotation()
  {
      return m_annotation;
  }
  
  public int getRandomCount()
  {
      return m_count;
  }
}
