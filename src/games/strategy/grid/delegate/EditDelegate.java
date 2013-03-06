package games.strategy.grid.delegate;

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.remote.IGridEditDelegate;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.Match;

import java.util.Collection;

/**
 * 
 * @author veqryn
 * 
 */
public class EditDelegate extends BaseEditDelegate implements IGridEditDelegate
{
	public String removeUnits(final Territory territory, final Collection<Unit> units)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = validateRemoveUnits(getData(), territory, units)))
			return result;
		if (units == null || units.isEmpty())
			return null;
		logEvent("Removing units owned by " + units.iterator().next().getOwner().getName() + " from " + territory.getName() + ": " + MyFormatter.unitsToTextNoOwner(units), units);
		m_bridge.addChange(ChangeFactory.removeUnits(territory, units));
		return null;
	}
	
	public static String validateRemoveUnits(final GameData data, final Territory territory, final Collection<Unit> units)
	{
		final String result = null;
		if (units.isEmpty())
			return "No units selected";
		final PlayerID player = units.iterator().next().getOwner();
		// all units should be same owner
		if (!Match.allMatch(units, Matches.unitIsOwnedBy(player)))
			return "Not all units have the same owner";
		return result;
	}
	
	public String addUnits(final Territory territory, final Collection<Unit> units)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = validateAddUnits(getData(), territory, units)))
			return result;
		if (units == null || units.isEmpty())
			return null;
		logEvent("Adding units owned by " + units.iterator().next().getOwner().getName() + " to " + territory.getName() + ": " + MyFormatter.unitsToTextNoOwner(units), units);
		m_bridge.addChange(ChangeFactory.addUnits(territory, units));
		return null;
	}
	
	public static String validateAddUnits(final GameData data, final Territory territory, final Collection<Unit> units)
	{
		final String result = null;
		if (units.isEmpty())
			return "No units selected";
		if (territory.getUnits().getUnitCount() > 0)
			return "Territory already contains units";
		return result;
	}
	
	public String changeTerritoryOwner(final Territory territory, final PlayerID player)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = validateChangeTerritoryOwner(getData(), territory, player)))
			return result;
		if (territory == null)
			return null;
		logEvent("Changing ownership of " + territory.getName() + " from " + territory.getOwner().getName() + " to " + player.getName(), territory);
		m_bridge.addChange(ChangeFactory.changeOwner(territory, player));
		return null;
	}
	
	public static String validateChangeTerritoryOwner(final GameData data, final Territory territory, final PlayerID player)
	{
		final String result = null;
		final TerritoryAttachment ta = TerritoryAttachment.get(territory, true);
		if (ta == null)
			return "Territory has no attachment";
		return result;
	}
	
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IGridEditDelegate.class;
	}
}
