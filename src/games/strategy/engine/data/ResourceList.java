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
	private Map m_resourceList = new HashMap();
	
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
		return (Resource) m_resourceList.get(name);
	}
}
