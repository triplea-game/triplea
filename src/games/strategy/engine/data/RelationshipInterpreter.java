package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;

import java.util.Collection;

public class RelationshipInterpreter extends GameDataComponent
{
	private static final long serialVersionUID = -643454441052535241L;
	
	public RelationshipInterpreter(final GameData data)
	{
		super(data);
	}
	
	/**
	 * @param p1
	 *            first referring player
	 * @param p2
	 *            second referring player
	 * @return whether player p1 is allied to player p2
	 */
	public boolean isAllied(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeIsAllied.match((getRelationshipType(p1, p2)));
	}
	
	public boolean isAlliedWithAnyOfThesePlayers(final PlayerID p1, final Collection<PlayerID> p2s)
	{
		for (final PlayerID p2 : p2s)
		{
			if (Matches.RelationshipTypeIsAllied.match((getRelationshipType(p1, p2))))
				return true;
		}
		return false;
	}
	
	/**
	 * returns true if p1 is at war with p2
	 * 
	 * @param p1
	 *            player1
	 * @param p2
	 *            player2
	 * @return whether p1 is at war with p2
	 */
	public boolean isAtWar(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeIsAtWar.match((getRelationshipType(p1, p2)));
	}
	
	public boolean isAtWarWithAnyOfThesePlayers(final PlayerID p1, final Collection<PlayerID> p2s)
	{
		for (final PlayerID p2 : p2s)
		{
			if (Matches.RelationshipTypeIsAtWar.match((getRelationshipType(p1, p2))))
				return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param p1
	 *            player1
	 * @param p2
	 *            player2
	 * @return whether player1 is neutral to player2
	 */
	public boolean isNeutral(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeIsNeutral.match((getRelationshipType(p1, p2)));
	}
	
	public boolean isNeutralWithAnyOfThesePlayers(final PlayerID p1, final Collection<PlayerID> p2s)
	{
		for (final PlayerID p2 : p2s)
		{
			if (Matches.RelationshipTypeIsNeutral.match((getRelationshipType(p1, p2))))
				return true;
		}
		return false;
	}
	
	/*
	 * <strong>example</strong> method on how to extract a boolean from isAlliance();
	 * use this method instead of isAlliance in the spots to be used
	 * 
	 * @param p1
	 *            first referring player
	 * @param p2
	 *            second referring player
	 * @return whether player p1 helps defend at sea player p2
	 *
	public boolean helpsDefendAtSea(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeHelpsDefendAtSea.match((getRelationshipType(p1, p2)));
	}*/

	public boolean canMoveLandUnitsOverOwnedLand(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeCanMoveLandUnitsOverOwnedLand.match(getRelationshipType(p1, p2));
	}
	
	public boolean canMoveAirUnitsOverOwnedLand(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeCanMoveAirUnitsOverOwnedLand.match(getRelationshipType(p1, p2));
	}
	
	public boolean canLandAirUnitsOnOwnedLand(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeCanLandAirUnitsOnOwnedLand.match(getRelationshipType(p1, p2));
	}
	
	public String getUpkeepCost(final PlayerID p1, final PlayerID p2)
	{
		return getRelationshipType(p1, p2).getRelationshipTypeAttachment().getUpkeepCost();
	}
	
	public boolean alliancesCanChainTogether(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether.match(getRelationshipType(p1, p2));
	}
	
	public boolean isDefaultWarPosition(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeIsDefaultWarPosition.match(getRelationshipType(p1, p2));
	}
	
	public boolean canTakeOverOwnedTerritory(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeCanTakeOverOwnedTerritory.match(getRelationshipType(p1, p2));
	}
	
	public boolean givesBackOriginalTerritories(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeGivesBackOriginalTerritories.match(getRelationshipType(p1, p2));
	}
	
	public boolean canMoveIntoDuringCombatMove(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeCanMoveIntoDuringCombatMove.match(getRelationshipType(p1, p2));
	}
	
	public boolean canMoveThroughCanals(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeCanMoveThroughCanals.match(getRelationshipType(p1, p2));
	}
	
	public boolean rocketsCanFlyOver(final PlayerID p1, final PlayerID p2)
	{
		return Matches.RelationshipTypeRocketsCanFlyOver.match(getRelationshipType(p1, p2));
	}
	
	/**
	 * Convenience method to get RelationshipType so you can do relationshipChecks on the relationship between these 2 players
	 * 
	 * @return RelationshipType between these to players
	 */
	RelationshipType getRelationshipType(final PlayerID p1, final PlayerID p2)
	{
		return getData().getRelationshipTracker().getRelationshipType(p1, p2);
	}
}
