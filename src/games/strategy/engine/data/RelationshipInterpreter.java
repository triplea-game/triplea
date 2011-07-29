package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;

import java.util.Collection;
import java.util.Map;

import javax.xml.crypto.Data;

public class RelationshipInterpreter extends GameDataComponent
{

    public RelationshipInterpreter(GameData data)
    {
        super(data);
    }

    /**
     * @param p1 first referring player
     * @param p2 second referring player
     * @return whether player p1 is allied to player p2
     */
    public boolean isAllied(PlayerID p1, PlayerID p2)
    {
    	if(useRelationshipModel()) {
    		return Matches.RelationshipIsAllied.match((getRelationshipType(p1,p2)));
    	}

    	if(p1 == null || p2 == null)
    		throw new IllegalArgumentException("Arguments cannot be null p1:" + p1 + " p2:" + p2);

    	if(p1.equals(p2))
    		return true;


    	 Map<PlayerID, Collection<String>> alliances = getData().getAllianceTracker().getAlliancesMap();
    	if(!alliances.containsKey(p1) || !alliances.containsKey(p2))
    		return false;

    	Collection<String> a1 = alliances.get(p1);
    	Collection<String> a2 = alliances.get(p2);

    	return games.strategy.util.Util.someIntersect(a1,a2);

    }

    /**
     * returns true if p1 is at war with p2
     * @param p1 player1
     * @param p2 player2
     * @return whether p1 is at war with p2
     */
    public boolean isAtWar(PlayerID p1, PlayerID p2)
    {
    	if(useRelationshipModel())
    		return Matches.RelationshipIsAtWar.match((getRelationshipType(p1,p2)));

    	return !isAllied(p1,p2); //holder method war not yet implemented
    }

    /**
     *
     * @param p1 player1
     * @param p2 player2
     * @return whether player1 is neutral to player2
     */
    public boolean isNeutral(PlayerID p1, PlayerID p2)
    {
    	if(useRelationshipModel())
    		return Matches.RelationshipIsNeutral.match((getRelationshipType(p1,p2)));
    	return false; // alliancemodel doesn't know neutrality so always return false.
    }

    /**
     * <strong>example</strong> method on how to extract a boolean from isAlliance();
     * use this method instead of isAlliance in the spots to be used
     * @param p1 first referring player
     * @param p2 second referring player
     * @return whether player p1 helps defend at sea player p2
     */
    public boolean helpsDefendAtSea(PlayerID p1, PlayerID p2)
    {
    	if(useRelationshipModel())
    		return Matches.RelationshipHelpsDefendAtSea.match((getRelationshipType(p1,p2)));
    	return isAllied(p1,p1); // when using alliances only people in your alliance help defend at sea.
    }

    /**
     * Convenience method to see whether we are using relationshipModel, relationshipModel means we are looking at
     * individual relationships between players rather then alliance memberships
     * @return whether we are using relationship model
     */
    boolean useRelationshipModel()
    {
    	return getData().getRelationshipTracker().useRelationshipModel();
    }

    /**
     * Convenience method to get RelationshipType so you can do relationshipChecks on the relationship between these 2 players
     * @return RelationshipType between these to players
     */
    RelationshipType getRelationshipType(PlayerID p1, PlayerID p2)
    {
    	return getData().getRelationshipTracker().getRelationshipType(p1, p2);
    }

}
