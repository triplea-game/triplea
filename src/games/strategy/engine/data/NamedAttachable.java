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
 * NamedAttachable.java
 * 
 * Created on October 22, 2001, 6:49 PM
 */
package games.strategy.engine.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class NamedAttachable extends DefaultNamed implements Attachable
{
	private static final long serialVersionUID = 8597712929519099255L;
	private final Map<String, IAttachment> m_attachments = new HashMap<String, IAttachment>();
	
	/** Creates new NamedAttachable */
	public NamedAttachable(final String name, final GameData data)
	{
		super(name, data);
	}
	
	public IAttachment getAttachment(final String key)
	{
		return m_attachments.get(key);
	}
	
	public Map<String, IAttachment> getAttachments()
	{
		return Collections.unmodifiableMap(m_attachments);
	}
	
	public void addAttachment(final String key, final IAttachment value)
	{
		m_attachments.put(key, value);
	}
}
