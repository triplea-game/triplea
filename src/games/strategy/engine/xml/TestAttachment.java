/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * TestAttachment.java
 * 
 * Created on October 22, 2001, 7:32 PM
 */
package games.strategy.engine.xml;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;

/**
 * 
 * @author Sean Bridges
 * @version
 */
public class TestAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = 4886924951201479496L;
	private String m_value;
	
	/** Creates new TestAttachment */
	public TestAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	@Override
	public Attachable getAttachedTo()
	{
		return null;
	}
	
	@Override
	public void setAttachedTo(final Attachable unused)
	{
	}
	
	@Override
	public String getName()
	{
		return null;
	}
	
	@Override
	public void setName(final String aString)
	{
	}
	
	public void setValue(final String value)
	{
		m_value = value;
	}
	
	public String getValue()
	{
		return m_value;
	}
	
	@Override
	public void setData(final GameData m_data)
	{
	}
	
	@Override
	public void validate(final GameData data)
	{
	}
}
