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
 * DefaultNamed.java
 *
 * Created on October 17, 2001, 9:29 PM
 */

package games.strategy.engine.data;

import java.io.Serializable;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class DefaultNamed extends GameDataComponent implements Named, Serializable
{

  private final String m_name;

  /** Creates new DefaultNamed */
  public DefaultNamed(String name, GameData data)
  {
    super(data);
    if (name == null || name.length() == 0)
      throw new IllegalArgumentException("Name must not be null");
    m_name = name;
  }

  public String getName()
  {
    return m_name;
  }

  public boolean equals(Object o)
  {
    if (o == null || ! (o instanceof Named))
      return false;

    Named other = (Named) o;

    return this.m_name.equals(other.getName());
  }

  public int hashCode()
  {
    return m_name.hashCode();
  }

  public String toString()
  {
    return this.getClass().getName() + " called " + m_name;
  }

}
