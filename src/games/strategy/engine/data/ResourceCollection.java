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
	
	public void addResource(final Resource resource, final int quantity)
	{
		if (quantity < 0)
			throw new IllegalArgumentException("quantity must be positive");
		change(resource, quantity);
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
	
	private void change(final Resource resource, final int quantity)
	{
		m_resources.add(resource, quantity);
	}
	
	public int getQuantity(final Resource resource)
	{
		return m_resources.getInt(resource);
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
}
