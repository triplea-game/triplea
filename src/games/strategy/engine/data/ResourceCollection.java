/*
 * ResourceCollection.java
 *
 * Created on October 23, 2001, 8:24 PM
 */

package games.strategy.engine.data;

import java.util.*;

import games.strategy.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ResourceCollection extends GameDataComponent
{

	private final IntegerMap m_resources = new IntegerMap();
	
	/** Creates new ResourceCollection */
    public ResourceCollection(GameData data) 
	{
		super(data);
    }

	public void addResource(Resource resource, int quantity)
	{
		if(quantity < 0)
			throw new IllegalArgumentException("quantity must be positive");
		change(resource, quantity);
	
	}
	
	/**
	 * You cannot remove more than the collection contains.
	 */
	public void removeResource(Resource resource, int quantity)
	{
		if(quantity < 0)
			throw new IllegalArgumentException("quantity must be positive");
		int current = getQuantity(resource);
		if((current - quantity) < 0)
			throw new IllegalArgumentException("Cant remove more than player has. current:" + current + " toRemove: " + quantity);
				
		change(resource, -quantity);	
	}

	private void change(Resource resource, int quantity)
	{
		m_resources.add(resource, quantity);
	}	
	
	public int getQuantity(Resource resource)
	{
		return m_resources.getInt(resource);
	}
	
	public int getQuantity(String name)
	{
		Resource resource = getData().getResourceList().getResource(name);
		if(resource == null)
			throw new IllegalArgumentException("No resource named:" + name);
		return getQuantity(resource);
	}
	
	public boolean has(IntegerMap map)
	{
		return m_resources.greaterThanOrEqualTo(map);
	}
}
