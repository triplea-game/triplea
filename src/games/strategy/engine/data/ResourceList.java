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
 * ResourceList.java
 *
 * Created on October 19, 2001, 10:29 AM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ResourceList extends GameDataComponent
{
	private Map<String, Resource> m_resourceList = new HashMap<String, Resource>();
	
    public ResourceList(GameData data) 
	{
		super(data);
    }

	protected void addResource(Resource resource)
	{
		m_resourceList.put(resource.getName(), resource);
	}
	
	public int size()
	{
		return m_resourceList.size();
	}

	public Resource getResource(String name)
	{
		return m_resourceList.get(name);
	}
}
