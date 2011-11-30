package games.strategy.triplea.attatchments;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;

import java.util.HashMap;
import java.util.List;

/**
 * The purpose of this class is to have all ATTACHMENT classes that use "conditions" implement this class,
 * so that conditions can be tested for independently and recursively.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public interface IConditions
{
	public void setConditions(final String conditions) throws GameParseException;
	
	public List<RulesAttachment> getConditions();
	
	public void clearConditions();
	
	public void setConditionType(final String s) throws GameParseException;
	
	public String getConditionType();
	
	public void setInvert(final String s);
	
	public boolean getInvert();
	
	/**
	 * Tests if the attachment, as a whole, is satisfied. This includes and takes account of all conditions that make up this ICondition, as well as invert, conditionType,
	 * and in the case of RulesAttachments it also takes into account all the different rules that must be tested for, like alliedOwnershipTerritories, unitPresence, etc etc.
	 * 
	 * @param testedConditions
	 * @param data
	 * @return
	 */
	public boolean isSatisfied(HashMap<IConditions, Boolean> testedConditions, final GameData data);
}
