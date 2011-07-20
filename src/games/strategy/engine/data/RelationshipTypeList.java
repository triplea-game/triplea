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
 * RelationshipTypeList.java
 *
 * Created on July 10, 2011
 */


package games.strategy.engine.data;

/**
*
* @author  Edwin van der Wal
* @version 0.1
*
* A collection of Relationship types
*/

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.RelationshipTypeAttachment;

import java.util.HashMap;
import java.util.Iterator;

public class RelationshipTypeList extends GameDataComponent implements Iterable<RelationshipType>{
	
	private final HashMap<String, RelationshipType> m_relationshipTypes = new HashMap<String,RelationshipType>(); 

	/**
	 * convenience method to return the RELATIONSHIP_TYPE_SELF relation (the relation you have with yourself)
	 * @return the relation one has with oneself.
	 */
	public RelationshipType getSelfRelation() {
		return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_SELF);
	}
	/**
	 * convenience method to return the RELATIONSHIP_TYPE_NULL relation (the relation you have with the Neutral Player)
	 * @return the relation one has with the Neutral.
	 */
	public RelationshipType getNullRelation() {
		return this.getRelationshipType(Constants.RELATIONSHIP_TYPE_NULL);
	}

	/**
	 * Constructs a new RelationshipTypeList
	 * @param data GameData used for construction
	 */
	protected RelationshipTypeList(GameData data) {
		super(data);
		createSelfRelation(data);
		createNullRelation(data);
	}

	/** 
	 * Creates the NullRelation and puts it in the relationshipTypeList
	 * @param data used for construction
	 */
	private void createNullRelation(GameData data) {
		RelationshipType relation = new RelationshipType(Constants.RELATIONSHIP_TYPE_NULL,data);
	    RelationshipTypeAttachment at = new RelationshipTypeAttachment();
	    try {
			at.setArcheType(RelationshipTypeAttachment.WAR_ARCHETYPE); // for now relationships with NULL_Players are "war"
		} catch (GameParseException e) {
			// won't happen
		}
		relation.addAttachment(Constants.RELATIONSHIPTYPE_ATTATCHMENT_NAME, at);
	    at.setAttatchedTo(relation);
		addRelationshipType(relation);
	 }

	/**
	 * Creates the SelfRelation and puts it in the relationshipTypeList
	 * @param data used for construction
	 */
	private void createSelfRelation(GameData data) {
		RelationshipType relation = new RelationshipType(Constants.RELATIONSHIP_TYPE_SELF,data);
	    RelationshipTypeAttachment at = new RelationshipTypeAttachment();
	    try {
			at.setArcheType(RelationshipTypeAttachment.ALLIED_ARCHETYPE); // for now relationships with Self are "allied"
		} catch (GameParseException e) {
			// won't happen
		}
		relation.addAttachment(Constants.RELATIONSHIPTYPE_ATTATCHMENT_NAME, at);
	    at.setAttatchedTo(relation);
		addRelationshipType(relation);
	}

	/** adds a new RelationshipType, this should only be called by the GameParser.
	 * 
	 * @param p RelationshipType
	 * @return the RelationshipType just created (convenience method for the GameParser)
	 */
	protected RelationshipType addRelationshipType(RelationshipType p) {
		m_relationshipTypes.put(p.getName(),p);
		return p;
	}
	
	/**
	 * Gets a relationshipType from the list by name;
	 * @param name name of the relationshipType
	 * @return RelationshipType with this name
	 */
	public RelationshipType getRelationshipType(String name) {
		return m_relationshipTypes.get(name);
	}

	/** returns a relationshipTypeIterator
	 * 
	 */
	public Iterator<RelationshipType> iterator() {
		return m_relationshipTypes.values().iterator();
	}

	/**
	 * 
	 * @return site of the relationshipTypeList, be aware that the standard size = 2 (Self and Null Relation)
	 */
	public int size() {
		return m_relationshipTypes.size();
	}


}
