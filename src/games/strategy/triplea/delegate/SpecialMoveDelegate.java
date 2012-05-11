package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.formatter.MyFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SpecialMoveDelegate is a move delegate made for special movements like the new paratrooper/airborne movement.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public class SpecialMoveDelegate extends AbstractMoveDelegate implements IMoveDelegate
{
	
	public SpecialMoveDelegate()
	{
	}
	
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		super.start(new TripleADelegateBridge(aBridge));
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public String move(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded, final Map<Unit, Collection<Unit>> newDependents)
	{
		final GameData data = getData();
		// there reason we use this, is because if we are in edit mode, we may have a different unit owner than the current player.
		final PlayerID player = getUnitsOwner(units);
		// here we have our own new validation method....
		final MoveValidationResult result = SpecialMoveDelegate.validateMove(units, route, player, transportsThatCanBeLoaded, newDependents, m_nonCombat, m_movesToUndo, data);
		final StringBuilder errorMsg = new StringBuilder(100);
		final int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);
		final String numErrorsMsg = numProblems > 0 ? ("; " + numProblems + " " + MyFormatter.pluralize("error", numProblems) + " not shown") : "";
		if (result.hasError())
			return errorMsg.append(result.getError()).append(numErrorsMsg).toString();
		if (result.hasDisallowedUnits())
			return errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString();
		if (result.hasUnresolvedUnits())
			return errorMsg.append(result.getUnresolvedUnitWarning(0)).append(numErrorsMsg).toString();
		// allow user to cancel move if aa guns will fire
		final AAInMoveUtil aaInMoveUtil = new AAInMoveUtil();
		aaInMoveUtil.initialize(m_bridge);
		final Collection<Territory> aaFiringTerritores = aaInMoveUtil.getTerritoriesWhereAAWillFire(route, units);
		if (!aaFiringTerritores.isEmpty())
		{
			if (!getRemotePlayer().confirmMoveInFaceOfAA(aaFiringTerritores))
				return null;
		}
		// do the move
		final UndoableMove currentMove = new UndoableMove(data, units, route);
		final String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName() + " to " + route.getEnd().getName();
		m_bridge.getHistoryWriter().startEvent(transcriptText);
		m_bridge.getHistoryWriter().setRenderingData(currentMove.getDescriptionObject());
		m_tempMovePerformer = new MovePerformer();
		m_tempMovePerformer.initialize(this);
		m_tempMovePerformer.moveUnits(units, route, player, transportsThatCanBeLoaded, newDependents, currentMove);
		m_tempMovePerformer = null;
		return null;
	}
	
	public static MoveValidationResult validateMove(final Collection<Unit> units, final Route route, final PlayerID player, final Collection<Unit> transportsToLoad,
				final Map<Unit, Collection<Unit>> newDependents, final boolean isNonCombat, final List<UndoableMove> undoableMoves, final GameData data)
	{
		final MoveValidationResult result = new MoveValidationResult();
		if (route.hasNoSteps())
			return result;
		if (MoveValidator.validateFirst(data, units, route, player, result).getError() != null)
		{
			return result;
		}
		return result;
	}
	
	private static boolean getEditMode(final GameData data)
	{
		return EditDelegate.getEditMode(data);
	}
	
	@Override
	public int PUsAlreadyLost(final Territory t)
	{
		// Auto-generated method stub
		return 0;
	}
	
	@Override
	public void PUsLost(final Territory t, final int amt)
	{
		// Auto-generated method stub
	}
}
