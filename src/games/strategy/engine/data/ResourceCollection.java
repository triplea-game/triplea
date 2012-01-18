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
 * ResourceCollection.java
 * 
 * Created on October 23, 2001, 8:24 PM
 */
package games.strategy.engine.data;

import games.strategy.util.IntegerMap;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
@SuppressWarnings("serial")
public class ResourceCollection extends GameDataComponent
{
	private final IntegerMap<Resource> m_resources = new IntegerMap<Resource>();
	
	/**
	 * Creates new ResourceCollection
	 * 
	 * @param data
	 *            game data
	 */
	public ResourceCollection(final GameData data)
	{
		super(data);
	}
	
	public ResourceCollection(final ResourceCollection other)
	{
		super(other.getData());
		m_resources.add(other.m_resources);
	}
	
	public ResourceCollection(final GameData data, final IntegerMap<Resource> resources)
	{
		this(data);
		m_resources.add(resources);
	}
	
	public void addResource(final Resource resource, final int quantity)
	{
		if (quantity < 0)
			throw new IllegalArgumentException("quantity must be positive");
		change(resource, quantity);
	}
	
	public void add(final ResourceCollection otherResources)
	{
		m_resources.add(otherResources.m_resources);
	}
	
	/**
	 * You cannot remove more than the collection contains.
	 * 
	 * @param resource
	 *            referring resource
	 * @param quantity
	 *            quantity of the resource that should be removed
	 */
	public void removeResource(final Resource resource, final int quantity)
	{
		if (quantity < 0)
			throw new IllegalArgumentException("quantity must be positive");
		final int current = getQuantity(resource);
		if ((current - quantity) < 0)
			throw new IllegalArgumentException("Cant remove more than player has. current:" + current + " toRemove: " + quantity);
		change(resource, -quantity);
	}
	
	public void removeAllOfResource(final Resource resource)
	{
		m_resources.removeKey(resource);
	}
	
	private void change(final Resource resource, final int quantity)
	{
		m_resources.add(resource, quantity);
	}
	
	/**
	 * Overwrites any current resource with the same name.
	 * 
	 * @param resource
	 * @param quantity
	 */
	public void putResource(final Resource resource, final int quantity)
	{
		if (quantity < 0)
			throw new IllegalArgumentException("quantity must be positive");
		m_resources.put(resource, quantity);
	}
	
	public int getQuantity(final Resource resource)
	{
		return m_resources.getInt(resource);
	}
	
	public IntegerMap<Resource> getResourcesCopy()
	{
		return new IntegerMap<Resource>(m_resources);
	}
	
	public int getQuantity(final String name)
	{
		getData().acquireReadLock();
		try
		{
			final Resource resource = getData().getResourceList().getResource(name);
			if (resource == null)
				throw new IllegalArgumentException("No resource named:" + name);
			return getQuantity(resource);
		} finally
		{
			getData().releaseReadLock();
		}
	}
	
	public boolean has(final IntegerMap<Resource> map)
	{
		return m_resources.greaterThanOrEqualTo(map);
	}
	
	/**
	 * @param spent
	 * @return new ResourceCollection containing the difference between both collections
	 */
	public ResourceCollection difference(final ResourceCollection otherCollection)
	{
		final ResourceCollection returnCollection = new ResourceCollection(getData(), m_resources);
		returnCollection.subtract(otherCollection);
		return returnCollection;
	}
	
	private void subtract(final ResourceCollection resourceCollection)
	{
		subtract(resourceCollection.m_resources);
	}
	
	public void subtract(final IntegerMap<Resource> cost)
	{
		for (final Resource resource : cost.keySet())
		{
			removeResource(resource, cost.getInt(resource));
		}
		
	}
	
	public void subtract(final IntegerMap<Resource> cost, final int quantity)
	{
		for (int i = 0; i < quantity; i++)
		{
			subtract(cost);
		}
	}
	
	public void add(final IntegerMap<Resource> resources)
	{
		for (final Resource resource : resources.keySet())
		{
			addResource(resource, resources.getInt(resource));
		}
	}
	
	public void add(final IntegerMap<Resource> resources, final int quantity)
	{
		for (int i = 0; i < quantity; i++)
		{
			add(resources);
		}
	}
	
	public int fitsHowOften(final IntegerMap<Resource> cost)
	{
		if (cost.size() == 0)
			return Integer.MAX_VALUE;
		final ResourceCollection resources = new ResourceCollection(getData(), m_resources);
		for (int i = 0; i <= 1000; i++)
		{
			try
			{
				resources.subtract(cost);
			} catch (final IllegalArgumentException iae)
			{
				return i; // when the subtraction isn't possible it will throw an exception, which means we can return i;
			}
		}
		throw new IllegalArgumentException("Unlimited purchases shouldn't be possible");
	}
	
	@Override
	public String toString()
	{
		return toString(m_resources);
	}
	
	public static String toString(final IntegerMap<Resource> resources)
	{
		String returnString = "";
		for (final Resource resource : resources.keySet())
		{
			returnString += ", ";
			returnString += resources.getInt(resource);
			returnString += " " + resource.getName();
		}
		if (returnString.length() > 0 && returnString.startsWith(", "))
			returnString = returnString.replaceFirst(", ", "");
		return returnString;
	}
	
	public String toStringForHTML()
	{
		return toStringForHTML(m_resources);
	}
	
	public static String toStringForHTML(final IntegerMap<Resource> resources)
	{
		String returnString = "";
		for (final Resource resource : resources.keySet())
		{
			returnString += "<br>";
			returnString += resources.getInt(resource);
			returnString += " " + resource.getName();
		}
		if (returnString.length() > 0 && returnString.startsWith("<br>"))
			returnString = returnString.replaceFirst("<br>", "");
		return returnString;
	}
	
	/**
	 * @param times
	 *            multiply this Collection times times.
	 */
	public void multiply(final int times)
	{
		final IntegerMap<Resource> base = new IntegerMap<Resource>(m_resources);
		add(base, times - 1);
	}
}
