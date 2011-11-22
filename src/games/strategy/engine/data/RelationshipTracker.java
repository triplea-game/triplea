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
 * RelationshipTracker.java
 * 
 * Created on July 13th, 2011
 */
/**
 * @author Edwin van der Wal
 * @version 1.0
 * 
 */
package games.strategy.engine.data;

import games.strategy.triplea.attatchments.RelationshipTypeAttachment;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

public class RelationshipTracker extends RelationshipInterpreter
{
	// map of "playername:playername" to RelationshipType that exists between those 2 players
	private final HashMap<RelatedPlayers, Relationship> m_relationships = new HashMap<RelatedPlayers, Relationship>();
	
	public RelationshipTracker(final GameData data)
	{
		super(data);
	}
	
	/**
	 * Method for setting a relationship between two players, this should only be called through the Change Factory.
	 * 
	 * @param p1
	 *            Player1 that will get the relationship
	 * @param p2
	 *            Player2 that will get the relationship
	 * @param r
	 *            the RelationshipType between those two players that will be set.
	 */
	protected void setRelationship(final PlayerID p1, final PlayerID p2, final RelationshipType r)
	{
		m_relationships.put(new RelatedPlayers(p1, p2), new Relationship(r));
	}
	
	/**
	 * Method for setting a relationship between two players, this should only be called during the Game Parser.
	 * 
	 * @param p1
	 * @param p2
	 * @param r
	 * @param roundValue
	 */
	protected void setRelationship(final PlayerID p1, final PlayerID p2, final RelationshipType r, final int roundValue)
	{
		m_relationships.put(new RelatedPlayers(p1, p2), new Relationship(r, roundValue));
	}
	
	/**
	 * will give you the relationshipType that currently exists between 2 players.
	 * 
	 * @param p1
	 *            Player1 in the relationship
	 * @param p2
	 *            Player2 in the relationship
	 * @return the current RelationshipType between those two players
	 */
	@Override
	public RelationshipType getRelationshipType(final PlayerID p1, final PlayerID p2)
	{
		return getRelationship(p1, p2).getRelationshipType();
	}
	
	public Relationship getRelationship(final PlayerID p1, final PlayerID p2)
	{
		return m_relationships.get(new RelatedPlayers(p1, p2));
	}
	
	public HashSet<Relationship> getRelationships(final PlayerID player1)
	{
		final HashSet<Relationship> relationships = new HashSet<Relationship>();
		for (final PlayerID player2 : getData().getPlayerList().getPlayers())
		{
			if (player2 == null || player2.equals(player1))
				continue;
			relationships.add(getRelationship(player1, player2));
		}
		return relationships;
	}
	
	public int getRoundRelationshipWasCreated(final PlayerID p1, final PlayerID p2)
	{
		return m_relationships.get(new RelatedPlayers(p1, p2)).getRoundCreated();
	}
	
	/**
	 * Convenience method to directly access relationshipTypeAttachment on the relationship that exists between two players
	 * 
	 * @param p1
	 *            Player 1 in the relationship
	 * @param p2
	 *            Player 2 in the relationship
	 * @return the current RelationshipTypeAttachment attached to the current relationship that exists between those 2 players
	 * 
	 */
	protected RelationshipTypeAttachment getRelationshipTypeAttachment(final PlayerID p1, final PlayerID p2)
	{
		final RelationshipType relation = getRelationshipType(p1, p2);
		return RelationshipTypeAttachment.get(relation);
	}
	
	/**
	 * This methods will create all SelfRelations of all players including NullPlayer with oneself.
	 * This method should only be called once.
	 */
	protected void setSelfRelations()
	{
		for (final PlayerID p : getData().getPlayerList().getPlayers())
		{
			setRelationship(p, p, getSelfRelationshipType());
		}
		setRelationship(PlayerID.NULL_PLAYERID, PlayerID.NULL_PLAYERID, getSelfRelationshipType());
	}
	
	/**
	 * This methods will create all relationship of all players with the NullPlayer.
	 * This method should only be called once.
	 */
	protected void setNullPlayerRelations()
	{
		for (final PlayerID p : getData().getPlayerList().getPlayers())
		{
			setRelationship(p, PlayerID.NULL_PLAYERID, getNullRelationshipType());
		}
	}
	
	/** convenience method to get the SelfRelationshipType added for readability */
	private RelationshipType getSelfRelationshipType()
	{
		return getData().getRelationshipTypeList().getSelfRelation();
	}
	
	/** convenience method to get the NullRelationshipType (relationship with the Nullplayer) added for readability */
	private RelationshipType getNullRelationshipType()
	{
		return getData().getRelationshipTypeList().getNullRelation();
	}
	
	
	/**
	 * RelatedPlayers is a class of 2 players that are related, used in relationships.
	 * 
	 * @author Edwin van der Wal
	 * 
	 */
	public class RelatedPlayers implements Serializable
	{
		/**
		 * override hashCode to make sure that each new instance of this class can be matched in the Hashtable
		 * even if it was put in as (p1,p2) and you want to get it out as (p2,p1)
		 * and
		 */
		@Override
		public int hashCode()
		{
			return m_p1.hashCode() + m_p2.hashCode();
		}
		
		private final PlayerID m_p1;
		private final PlayerID m_p2;
		
		public RelatedPlayers(final PlayerID p1, final PlayerID p2)
		{
			m_p1 = p1;
			m_p2 = p2;
		}
		
		@Override
		public boolean equals(final Object object)
		{
			if (object instanceof RelatedPlayers)
			{
				final RelatedPlayers relatedPlayers2 = (RelatedPlayers) object;
				return (relatedPlayers2.m_p1.equals(m_p1) && relatedPlayers2.m_p2.equals(m_p2)) || (relatedPlayers2.m_p2.equals(m_p1) && relatedPlayers2.m_p1.equals(m_p2));
			}
			return super.equals(object);
		}
		
		/**
		 * convenience method to get relationshipType from a new RelatedPlayers(p1,p2).getRelationshipType();
		 * 
		 * @return RelationshipType between these RelatedPlayers
		 */
		public RelationshipType getRelationshipType()
		{
			return m_p1.getData().getRelationshipTracker().getRelationshipType(m_p1, m_p2);
		}
		
		/**
		 * convenience method to get relationshipTypeAttachment from a new RelatedPlayers(p1,p2).getRelationshipTypeAttachment();
		 * 
		 * @return RelationshipTypeAttachment between these RelatedPlayers
		 */
		public RelationshipTypeAttachment getRelationshipTypeAttachment()
		{
			return m_p1.getData().getRelationshipTracker().getRelationshipTypeAttachment(m_p1, m_p2);
		}
		
		@Override
		public String toString()
		{
			return m_p1.getName() + "-" + m_p2.getName();
		}
	}
	

	public class Relationship implements Serializable
	{
		/**
		 * This should never be called outside of the change factory.
		 * 
		 * @param relationshipType
		 */
		public Relationship(final RelationshipType relationshipType)
		{
			this.relationshipType = relationshipType;
			this.roundCreated = getData().getSequence().getRound();
		}
		
		/**
		 * This should never be called outside of the game parser.
		 * 
		 * @param relationshipType
		 * @param roundValue
		 */
		public Relationship(final RelationshipType relationshipType, final int roundValue)
		{
			this.relationshipType = relationshipType;
			this.roundCreated = roundValue;
		}
		
		private final RelationshipType relationshipType;
		private final int roundCreated;
		
		public int getRoundCreated()
		{
			return roundCreated;
		}
		
		public RelationshipType getRelationshipType()
		{
			return relationshipType;
		}
		
		@Override
		public String toString()
		{
			return roundCreated + ":" + relationshipType;
		}
	}
}
