package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.List;

/**
 * Attaches to technologies.
 * Also contains static methods of interpreting data from all technology attachments that a player has.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
public class TechAbilityAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = 1866305599625384294L;
	
	/**
	 * Convenience method.
	 */
	public static TechAbilityAttachment get(final TechAdvance type)
	{
		final TechAbilityAttachment rVal = (TechAbilityAttachment) type.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME);
		return rVal;
	}
	
	/**
	 * Convenience method.
	 */
	public static TechAbilityAttachment get(final TechAdvance type, final String nameOfAttachment)
	{
		final TechAbilityAttachment rVal = (TechAbilityAttachment) type.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("No technology attachment for:" + type.getName() + " with name:" + nameOfAttachment);
		return rVal;
	}
	
	//
	// attachment fields
	//
	private IntegerMap<UnitType> m_movementBonus = new IntegerMap<UnitType>();
	
	//
	// constructor
	//
	public TechAbilityAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	//
	// setters and getters
	//
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setMovementBonus(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new GameParseException("movementBonus can not be empty or have more than two fields" + thisErrorMsg());
		String unitType;
		unitType = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitType);
		if (ut == null)
			throw new GameParseException("No unit called:" + unitType + thisErrorMsg());
		// we should allow positive and negative numbers
		final int n = getInt(s[0]);
		m_movementBonus.put(ut, n);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMovementBonus(final IntegerMap<UnitType> value)
	{
		m_movementBonus = value;
	}
	
	public IntegerMap<UnitType> getMovementBonus()
	{
		return m_movementBonus;
	}
	
	public void clearMovementBonus()
	{
		m_movementBonus.clear();
	}
	
	//
	// Static Methods for interpreting data in attachments
	//
	public static int getMovementBonus(final UnitType ut, final PlayerID player, final GameData data)
	{
		int rVal = 0;
		for (final TechAdvance ta : TechTracker.getTechAdvances(player, data))
		{
			final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa != null)
			{
				rVal += taa.getMovementBonus().getInt(ut);
			}
		}
		return rVal;
	}
	
	/**
	 * Must be done only in GameParser, and only after we have already parsed ALL technologies, attachments, and game options/properties.
	 * 
	 * @param data
	 * @throws GameParseException
	 */
	public static void setDefaultTechnologyAttachments(final GameData data) throws GameParseException
	{
		// loop through all technologies. any "default/hard-coded" tech that doesn't have an attachment, will get its "default" attachment. any non-default tech are ignored.
		for (final TechAdvance ta : data.getTechnologyFrontier().getTechs())
		{
			TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
			if (taa == null)
			{
				// TODO: debating if we should have flags for things like "air", "land", "sea", "aaGun", "factory", "strategic bomber", etc.
				// perhaps just the easy ones, of air, land, and sea?
				if (ta.equals(TechAdvance.LONG_RANGE_AIRCRAFT))
				{
					taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
					ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
					final List<UnitType> allAir = Match.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsAir);
					for (final UnitType air : allAir)
					{
						taa.setMovementBonus("2:" + air.getName());
					}
				}
				// else if .... (add rest here)
			}
		}
	}
	
	//
	// validator
	//
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
