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
 * TestAttatchment.java
 *
 * Created on October 22, 2001, 7:32 PM
 */

package games.strategy.engine.xml;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version
 */
public class TestAttachment implements games.strategy.engine.data.IAttachment {

	private String m_value;



  /** Creates new TestAttatchment */
  public TestAttachment()
  {
  }

  public Attatchable getAttatchedTo()
  {
    return null;
  }

  public void setAttatchedTo(Attatchable unused)
  {

  }

  public String getName()
  {
    return null;
  }

  public void setName(String aString)
  {

  }



  public void setValue(String value)
	{
		m_value = value;
	}

	public String getValue()
	{
		return m_value;
	}

	public void setData(GameData m_data)
	{
	}

	/**
	 * Called after the attatchment is created.
	 * IF an error occurs should throw a runtime
	 * exception to halt the vm.
	 */
	public void validate() {
	}

}
