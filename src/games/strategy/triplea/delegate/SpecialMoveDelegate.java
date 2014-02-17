package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.common.delegate.GameDelegateBridge;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAbilityAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SpecialMoveDelegate is a move delegate made for special movements like the new paratrooper/airborne movement.
 * 
 * Airborne Attacks is actually Paratroopers tech for Global 1940, except that I really do not want to confuse myself by naming yet another thing Paratroopers, so this is now getting a new name.
 * This is very different than "paratroopers" for AA50. We are actually launching the units from a static unit (an airbase) to another territory, instead of carrying them.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public class SpecialMoveDelegate extends AbstractMoveDelegate implements IMoveDelegate
{
	private boolean m_needToInitialize = true;
	
	// private boolean m_allowAirborne = true;
	
	public SpecialMoveDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run, AND before "start" is called.
	 */
	@Override
	public void setDelegateBridgeAndPlayer(final IDelegateBridge iDelegateBridge)
	{
		super.setDelegateBridgeAndPlayer(new GameDelegateBridge(iDelegateBridge));
	}
	
	@Override
	public void start()
	{
		super.start();
		final GameData data = getData();
		if (!allowAirborne(m_player, data))
		{
			// m_allowAirborne = false;
			return;
		}
		final boolean onlyWhereUnderAttackAlready = games.strategy.triplea.Properties.getAirborneAttacksOnlyInExistingBattles(data);
		// final boolean onlyEnemyTerritories = games.strategy.triplea.Properties.getAirborneAttacksOnlyInEnemyTerritories(data);
		final BattleTracker battleTracker = AbstractMoveDelegate.getBattleTracker(data);
		if (m_needToInitialize && onlyWhereUnderAttackAlready)
		{
			BattleDelegate.doInitialize(battleTracker, m_bridge); // we do this to clear any 'finishedBattles' and also to create battles for units that didn't move
			m_needToInitialize = false;
		}
	}
	
	@Override
	public void end()
	{
		super.end();
		m_needToInitialize = true;
		// m_allowAirborne = true;
	}
	
	@Override
	public Serializable saveState()
	{
		final SpecialMoveExtendedDelegateState state = new SpecialMoveExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_needToInitialize = m_needToInitialize;
		// state.m_allowAirborne = m_allowAirborne;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final SpecialMoveExtendedDelegateState s = (SpecialMoveExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_needToInitialize = s.m_needToInitialize;
		// m_allowAirborne = s.m_allowAirborne;
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		if (!allowAirborne(m_player, getData()))
			return false;
		return true;
	}
	
	@Override
	public String move(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded, final Map<Unit, Collection<Unit>> newDependents)
	{
		if (!allowAirborne(m_player, getData()))
			return "No Airborne Movement Allowed Yet";
		final GameData data = getData();
		// there reason we use this, is because if we are in edit mode, we may have a different unit owner than the current player.
		final PlayerID player = getUnitsOwner(units);
		// here we have our own new validation method....
		final MoveValidationResult result = SpecialMoveDelegate.validateMove(units, route, player, transportsThatCanBeLoaded, newDependents, GameStepPropertiesHelper.isNonCombatMove(data),
					m_movesToUndo,
					data);
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
		// add dependencies (any move that came before this, from this start territory, is a dependency)
		for (final UndoableMove otherMove : m_movesToUndo)
		{
			if (otherMove.getStart().equals(route.getStart()))
				currentMove.addDependency(otherMove);
		}
		// make the units airborne
		final CompositeChange airborneChange = new CompositeChange();
		for (final Unit u : units)
		{
			airborneChange.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.AIRBORNE));
		}
		currentMove.addChange(airborneChange);
		// make the bases start filling up their capacity
		final Collection<Unit> basesAtStart = route.getStart().getUnits().getMatches(getAirborneBaseMatch(player, data));
		final Change fillLaunchCapacity = getNewAssignmentOfNumberLaunchedChange(units.size(), basesAtStart, player, data);
		currentMove.addChange(fillLaunchCapacity);
		
		// start event
		final String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName() + " to " + route.getEnd().getName();
		m_bridge.getHistoryWriter().startEvent(transcriptText, currentMove.getDescriptionObject());
		// actually do our special changes
		m_bridge.addChange(airborneChange);
		m_bridge.addChange(fillLaunchCapacity);
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
			return result;
		if (MoveValidator.validateFuel(data, units, route, player, result).getError() != null)
			return result;
		final boolean isEditMode = getEditMode(data);
		if (!isEditMode)
		{
			// make sure all units are at least friendly
			for (final Unit unit : Match.getMatches(units, Matches.unitIsOwnedBy(player).invert()))
				result.addDisallowedUnit("Can only move owned units", unit);
		}
		if (validateAirborneMovements(data, units, route, player, result).getError() != null)
			return result;
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private static MoveValidationResult validateAirborneMovements(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		if (!TechAbilityAttachment.getAllowAirborneForces(player, data))
			return result.setErrorReturnResult("Do Not Have Airborne Tech");
		final int airborneDistance = TechAbilityAttachment.getAirborneDistance(player, data);
		final Set<UnitType> airborneBases = TechAbilityAttachment.getAirborneBases(player, data);
		final Set<UnitType> airborneTypes = TechAbilityAttachment.getAirborneTypes(player, data);
		if (airborneDistance <= 0 || airborneBases.isEmpty() || airborneTypes.isEmpty())
			return result.setErrorReturnResult("Require Airborne Forces And Launch Capacity Tech");
		if (route.numberOfSteps() > airborneDistance)
			return result.setErrorReturnResult("Destination Is Out Of Range");
		final Match<Unit> airborneBaseMatch = getAirborneMatch(player, airborneBases);
		final Territory start = route.getStart();
		final Territory end = route.getEnd();
		final Collection<Unit> basesAtStart = start.getUnits().getMatches(airborneBaseMatch);
		if (basesAtStart.isEmpty())
			return result.setErrorReturnResult("Require Airborne Base At Originating Territory");
		final int airborneCapacity = TechAbilityAttachment.getAirborneCapacity(basesAtStart, player, data);
		if (airborneCapacity <= 0)
			return result.setErrorReturnResult("Airborne Bases Must Have Launch Capacity");
		else if (airborneCapacity < units.size())
		{
			final Collection<Unit> overMax = new ArrayList<Unit>(units);
			overMax.removeAll(Match.getNMatches(units, airborneCapacity, Match.ALWAYS_MATCH));
			for (final Unit u : overMax)
			{
				result.addDisallowedUnit("Airborne Base Capacity Has Been Reached", u);
			}
		}
		final Collection<Unit> airborne = new ArrayList<Unit>();
		for (final Unit u : units)
		{
			// final Match<Unit> airborneTypesMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitIsOfTypes(airborneTypes), Matches.UnitIsDisabled().invert(), Matches.unitHasNotMoved, Matches.UnitIsAirborne.invert());
			// should match "getAirborneTypesMatch", but I've written it out by line in order to get fine grained warnings
			if (!Matches.unitIsOwnedBy(player).match(u))
				result.addDisallowedUnit("Must Own All Airborne Forces", u);
			else if (!Matches.unitIsOfTypes(airborneTypes).match(u))
				result.addDisallowedUnit("Can Only Launch Airborne Forces", u);
			else if (Matches.UnitIsDisabled.match(u))
				result.addDisallowedUnit("Must Not Be Disabled", u);
			else if (!Matches.unitHasNotMoved.match(u))
				result.addDisallowedUnit("Must Not Have Previously Moved Airborne Forces", u);
			else if (Matches.UnitIsAirborne.match(u))
				result.addDisallowedUnit("Can Not Move Units Already Airborne", u);
			else
				airborne.add(u);
		}
		if (airborne.isEmpty())
			return result;
		final BattleTracker battleTracker = AbstractMoveDelegate.getBattleTracker(data);
		final boolean onlyWhereUnderAttackAlready = games.strategy.triplea.Properties.getAirborneAttacksOnlyInExistingBattles(data);
		final boolean onlyEnemyTerritories = games.strategy.triplea.Properties.getAirborneAttacksOnlyInEnemyTerritories(data);
		// final Match<Territory> allowedPathForAirDrop = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player, data), Matches.TerritoryAllowsCanMoveAirUnitsOverOwnedLand(player, data));
		if (!Match.allMatch(route.getSteps(), Matches.TerritoryIsPassableAndNotRestricted(player, data)))
			return result.setErrorReturnResult("May Not Fly Over Impassable or Restricted Territories");
		if (!Match.allMatch(route.getSteps(), Matches.TerritoryAllowsCanMoveAirUnitsOverOwnedLand(player, data)))
			return result.setErrorReturnResult("May Only Fly Over Territories Where Air May Move");
		final boolean someLand = Match.someMatch(airborne, Matches.UnitIsLand);
		final boolean someSea = Match.someMatch(airborne, Matches.UnitIsSea);
		// final boolean allAir = Match.allMatch(airborne, Matches.UnitIsAir);
		final boolean land = Matches.TerritoryIsLand.match(end);
		final boolean sea = Matches.TerritoryIsWater.match(end);
		if (someLand && someSea)
			return result.setErrorReturnResult("Can Not Mix Land and Sea Units");
		else if (someLand)
		{
			if (!land)
				return result.setErrorReturnResult("Can Not Move Land Units To Sea");
		}
		else if (someSea)
		{
			if (!sea)
				return result.setErrorReturnResult("Can Not Move Sea Units To Land");
		}
		if (onlyWhereUnderAttackAlready)
		{
			final IBattle battle = battleTracker.getPendingBattle(end, false, BattleType.NORMAL);
			if (battle == null)
				return result.setErrorReturnResult("Airborne May Only Attack Territories Already Under Assault");
			else if (land && someLand && !Match.someMatch(battle.getAttackingUnits(), Matches.UnitIsLand))
				return result.setErrorReturnResult("Battle Must Have Some Land Units Participating Already");
			else if (sea && someSea && !Match.someMatch(battle.getAttackingUnits(), Matches.UnitIsSea))
				return result.setErrorReturnResult("Battle Must Have Some Sea Units Participating Already");
		}
		else if (onlyEnemyTerritories)
		{
			if (!(Matches.isTerritoryEnemyAndNotUnownedWater(player, data).match(end) || Matches.territoryHasEnemyUnits(player, data).match(end)))
				return result.setErrorReturnResult("Destination Must Be Enemy Or Contain Enemy Units");
		}
		return result;
	}
	
	public static Match<Unit> getAirborneTypesMatch(final PlayerID player, final GameData data)
	{
		return getAirborneMatch(player, TechAbilityAttachment.getAirborneTypes(player, data));
	}
	
	public static Match<Unit> getAirborneBaseMatch(final PlayerID player, final GameData data)
	{
		return getAirborneMatch(player, TechAbilityAttachment.getAirborneBases(player, data));
	}
	
	public static Match<Unit> getAirborneMatch(final PlayerID player, final Set<UnitType> types)
	{
		return new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitIsOfTypes(types), Matches.UnitIsNotDisabled, Matches.unitHasNotMoved, Matches.UnitIsAirborne.invert());
	}
	
	private static Change getNewAssignmentOfNumberLaunchedChange(int newNumberLaunched, final Collection<Unit> bases, final PlayerID player, final GameData data)
	{
		final CompositeChange launchedChange = new CompositeChange();
		if (newNumberLaunched <= 0)
			return launchedChange;
		final IntegerMap<UnitType> capacityMap = TechAbilityAttachment.getAirborneCapacity(player, data);
		for (final Unit u : bases)
		{
			if (newNumberLaunched <= 0)
				break;
			final int numberLaunchedAlready = ((TripleAUnit) u).getLaunched();
			final int capacity = capacityMap.getInt(u.getType());
			final int toAdd = Math.min(newNumberLaunched, capacity - numberLaunchedAlready);
			if (toAdd <= 0)
				continue;
			newNumberLaunched -= toAdd;
			launchedChange.add(ChangeFactory.unitPropertyChange(u, (toAdd + numberLaunchedAlready), TripleAUnit.LAUNCHED));
		}
		return launchedChange;
	}
	
	/*public static int getNumberOfAirborneAlreadyLaunchedBy(final Collection<Unit> bases, final GameData data)
	{
		int rVal = 0;
		for (final Unit u : bases)
		{
			rVal += Math.max(0, ((TripleAUnit) u).getLaunched());
		}
		return rVal;
	}*/
	
	public static boolean allowAirborne(final PlayerID player, final GameData data)
	{
		if (!TechAbilityAttachment.getAllowAirborneForces(player, data))
		{
			return false;
		}
		final int airborneDistance = TechAbilityAttachment.getAirborneDistance(player, data);
		final Set<UnitType> airborneBases = TechAbilityAttachment.getAirborneBases(player, data);
		final Set<UnitType> airborneTypes = TechAbilityAttachment.getAirborneTypes(player, data);
		if (airborneDistance <= 0 || airborneBases.isEmpty() || airborneTypes.isEmpty())
		{
			return false;
		}
		final GameMap map = data.getMap();
		final Collection<Territory> territoriesWeCanLaunchFrom = Match.getMatches(map.getTerritories(), Matches.territoryHasUnitsThatMatch(getAirborneMatch(player, airborneBases)));
		// territoriesWeCanLaunchFrom.retainAll(Match.getMatches(territoriesWeCanLaunchFrom, Matches.territoryHasUnitsThatMatch(airborneTypesMatch)));
		if (territoriesWeCanLaunchFrom.isEmpty())
		{
			return false;
		}
		return true;
	}
	
	private static boolean getEditMode(final GameData data)
	{
		return BaseEditDelegate.getEditMode(data);
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


class SpecialMoveExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 7781410008392307104L;
	Serializable superState;
	// add other variables here:
	public boolean m_needToInitialize;
	// public boolean m_allowAirborne;
}
