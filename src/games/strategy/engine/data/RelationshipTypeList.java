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
 * RelationshipTypeList.java
 * 
 * Created on July 10, 2011
 */

package games.strategy.engine.data;

/**
 * 
 * @author Edwin van der Wal
 * @version 0.1
 * 
 *          A collection of Relationship types
 */

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.RelationshipTypeAttachment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class RelationshipTypeList extends GameDataComponent implements Iterable<RelationshipType>
{
	
	private final HashMap<String, RelationshipType> m_relationshipTypes = new HashMap<String, RelationshipType>();
	
	/**
	 * convenience method to return the RELATIONSHIP_TYPE_SELF relation (the relation you have with yourself)
	 * 
	 * @return the relation one has with oneself.
	 */
	public RelationshipType getSelfRelation()
	{
		return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_SELF);
	}
	
	/**
	 * convenience method to return the RELATIONSHIP_TYPE_NULL relation (the relation you have with the Neutral Player)
	 * 
	 * @return the relation one has with the Neutral.
	 */
	public RelationshipType getNullRelation()
	{
		return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_NULL);
	}
	
	/**
	 * Constructs a new RelationshipTypeList
	 * 
	 * @param data
	 *            GameData used for construction
	 * @throws GameParseException
	 */
	protected RelationshipTypeList(GameData data)
	{
		super(data);
		try
		{
			createDefaultRelationship(Constants.RELATIONSHIP_TYPE_SELF, RelationshipTypeAttachment.ARCHETYPE_ALLIED, data);
			createDefaultRelationship(Constants.RELATIONSHIP_TYPE_NULL, RelationshipTypeAttachment.ARCHETYPE_WAR, data);
			createDefaultRelationship(Constants.RELATIONSHIP_TYPE_DEFAULT_WAR, RelationshipTypeAttachment.ARCHETYPE_WAR, data);
			createDefaultRelationship(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED, RelationshipTypeAttachment.ARCHETYPE_ALLIED, data);
		} catch (GameParseException e)
		{
			// this should never happen, createDefaultRelationship only throws a GameParseException when the wrong ArcheType is supplied, but we never do that
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * Creates a default relationship
	 * 
	 * @param relationshipTypeConstant
	 *            the type of relationship
	 * @param relationshipArcheType
	 *            the archetype of the relationship
	 * @param data
	 *            the GameData object for this relationship
	 * @throws GameParseException
	 *             if the wrong relationshipArcheType is used
	 */
	
	private void createDefaultRelationship(final String relationshipTypeConstant, final String relationshipArcheType, GameData data) throws GameParseException
	{
		// create a new relationshipType with the name from the constant
		RelationshipType relationshipType = new RelationshipType(relationshipTypeConstant, data);
		// create a new attachment to attach to this type
		RelationshipTypeAttachment at = new RelationshipTypeAttachment();
		// set the archeType to this attachment
		at.setArcheType(relationshipArcheType);
		// attach this attachment to this type
		relationshipType.addAttachment(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME, at);
		at.setAttatchedTo(relationshipType);
		addRelationshipType(relationshipType);
	}
	
	/**
	 * adds a new RelationshipType, this should only be called by the GameParser.
	 * 
	 * @param p
	 *            RelationshipType
	 * @return the RelationshipType just created (convenience method for the GameParser)
	 */
	protected RelationshipType addRelationshipType(RelationshipType p)
	{
		m_relationshipTypes.put(p.getName(), p);
		return p;
	}
	
	/**
	 * Gets a relationshipType from the list by name;
	 * 
	 * @param name
	 *            name of the relationshipType
	 * @return RelationshipType with this name
	 */
	public RelationshipType getRelationshipType(String name)
	{
		return m_relationshipTypes.get(name);
	}
	
	/**
	 * returns a relationshipTypeIterator
	 * 
	 */
	
	public Iterator<RelationshipType> iterator()
	{
		return m_relationshipTypes.values().iterator();
	}
	
	/**
	 * 
	 * @return site of the relationshipTypeList, be aware that the standard size = 4 (Allied, War, Self and Null Relation)
	 */
	public int size()
	{
		return m_relationshipTypes.size();
	}
	
	public RelationshipType getDefaultAlliedRelationship()
	{
		return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED);
	}
	
	public RelationshipType getDefaultWarRelationship()
	{
		return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_WAR);
	}
	
	public Collection<RelationshipType> getAllRelationshipTypes()
	{
		return m_relationshipTypes.values();
	}
	
}
