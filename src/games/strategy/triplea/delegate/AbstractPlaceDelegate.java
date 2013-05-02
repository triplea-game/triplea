/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/**
 * PlaceDelegate.java
 * 
 * Overriding
 * 
 * Subclasses can over ride one of these methods to change the way this class
 * works. playerHasEnoughUnits(...), canProduce(...), canUnitsBePlaced(...)
 * 
 * For a simpler way you can override getProduction(...) which is called in the
 * default canProduce(...) method
 * 
 * Created on November 2, 2001, 12:29 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseTripleADelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.message.IRemote;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * Logic for placing units.
 * <p>
 * 
 * @author Sean Bridges and Veqryn
 * @version 1.0
 * 
 *          Known limitations.
 * 
 *          Doesn't take into account limits on number of factories that can be produced.
 * 
 *          Solved (by frigoref):
 *          The situation where one has two non original factories a,b each with
 *          production 2. If sea zone e neighbors a,b and sea zone f neighbors b. Then
 *          producing 2 in e was making it such that you cannot produce in f. The reason
 *          was that the production in e could be assigned to the factory in b, leaving no
 *          capacity to produce in f.
 *          A workaround was that if anyone ever accidently run into this situation
 *          then they could undo the production, produce in f first, and then produce in e.
 */
public abstract class AbstractPlaceDelegate extends BaseTripleADelegate implements IAbstractPlaceDelegate
{
	// maps Territory-> Collection of units
	protected Map<Territory, Collection<Unit>> m_produced = new HashMap<Territory, Collection<Unit>>();
	// a list of CompositeChanges
	protected List<UndoablePlacement> m_placements = new ArrayList<UndoablePlacement>();
	
	public void initialize(final String name)
	{
		initialize(name, name);
	}
	
	@Override
	public void start()
	{
		super.start();
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		super.end();
		doAfterEnd();
	}
	
	protected void doAfterEnd()
	{
		final PlayerID player = m_bridge.getPlayerID();
		// clear all units not placed
		final Collection<Unit> units = player.getUnits().getUnits();
		final GameData data = getData();
		if (!Properties.getUnplacedUnitsLive(data) && !units.isEmpty())
		{
			m_bridge.getHistoryWriter().startEvent(MyFormatter.unitsToTextNoOwner(units) + " were produced but were not placed", units);
			final Change change = ChangeFactory.removeUnits(player, units);
			m_bridge.addChange(change);
		}
		// reset ourselves for next turn
		m_produced = new HashMap<Territory, Collection<Unit>>();
		m_placements.clear();
		removeAirThatCantLand();
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		// nothing to place
		// nothing placed
		if (m_player == null || (m_player.getUnits().size() == 0 && getPlacementsMade() == 0))
			return false;
		return true;
	}
	
	protected void removeAirThatCantLand()
	{
		// for LHTR type games
		final GameData data = getData();
		final AirThatCantLandUtil util = new AirThatCantLandUtil(m_bridge);
		util.removeAirThatCantLand(m_player, false);
		// if edit mode has been on, we need to clean up after all players
		for (final PlayerID player : data.getPlayerList())
		{
			if (!player.equals(m_player))
				util.removeAirThatCantLand(player, false);
		}
	}
	
	@Override
	public Serializable saveState()
	{
		final PlaceExtendedDelegateState state = new PlaceExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_produced = m_produced;
		state.m_placements = m_placements;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final PlaceExtendedDelegateState s = (PlaceExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_produced = s.m_produced;
		m_placements = s.m_placements;
	}
	
	/**
	 * 
	 * @param t
	 *            territory of interest
	 * @return a COPY of the collection of units that are produced at territory t
	 */
	protected Collection<Unit> getAlreadyProduced(final Territory t)
	{
		// this list might be modified later
		final Collection<Unit> rVal = new ArrayList<Unit>();
		if (m_produced.containsKey(t))
			rVal.addAll(m_produced.get(t));
		return rVal;
	}
	
	public int getPlacementsMade()
	{
		return m_placements.size();
	}
	
	void setProduced(final Map<Territory, Collection<Unit>> produced)
	{
		m_produced = produced;
	}
	
	/**
	 * 
	 * @return the actual m_produced variable, allowing direct editing of the variable.
	 */
	protected final Map<Territory, Collection<Unit>> getProduced()
	{
		return m_produced;
	}
	
	// returns List<AbstractUndoableMove>
	public List<UndoablePlacement> getMovesMade()
	{
		return m_placements;
	}
	
	public String undoMove(final int moveIndex)
	{
		if (moveIndex < m_placements.size() && moveIndex >= 0)
		{
			final UndoablePlacement undoPlace = m_placements.get(moveIndex);
			undoPlace.undo(getData(), m_bridge);
			m_placements.remove(moveIndex);
			updateUndoablePlacementIndexes();
		}
		return null;
	}
	
	protected void updateUndoablePlacementIndexes()
	{
		for (int i = 0; i < m_placements.size(); i++)
		{
			m_placements.get(i).setIndex(i);
		}
	}
	
	public PlaceableUnits getPlaceableUnits(final Collection<Unit> units, final Territory to)
	{
		final String error = canProduce(to, units, m_player);
		if (error != null)
			return new PlaceableUnits(error);
		final Collection<Unit> placeableUnits = getUnitsToBePlaced(to, units, m_player);
		final int maxUnits = getMaxUnitsToBePlaced(placeableUnits, to, m_player, true);
		return new PlaceableUnits(placeableUnits, maxUnits);
	}
	
	public String placeUnits(final Collection<Unit> units, final Territory at)
	{
		if (units == null || units.isEmpty())
		{
			return null;
		}
		final String error = isValidPlacement(units, at, m_player);
		if (error != null)
			return error;
		performPlace(new ArrayList<Unit>(units), at, m_player);
		return null;
	}
	
	protected void performPlace(final Collection<Unit> units, final Territory at, final PlayerID player)
	{
		// System.out.println("Placing " + MyFormatter.unitsToTextNoOwner(units) + " at " + at.getName() + " by " + player.getName());
		final List<Territory> producers = getAllProducers(at, player, units);
		Collections.sort(producers, getBestProducerComparator(at, units, player));
		// System.out.println("Producers: " + producers);
		final IntegerMap<Territory> maxPlaceableMap = getMaxUnitsToBePlacedMap(units, at, player, true);
		// System.out.println("Max Place Map: " + maxPlaceableMap);
		final List<Unit> unitsLeftToPlace = new ArrayList<Unit>(units);
		Collections.sort(unitsLeftToPlace, getUnitConstructionComparator());
		// sort both producers and units so that the "to/at" territory comes first, and so that all constructions come first
		// this is because the PRODUCER for ALL CONSTRUCTIONS must be the SAME as the TERRITORY they are going into
		for (final Territory producer : producers)
		{
			if (unitsLeftToPlace.isEmpty())
				break;
			// units may have special restrictions like RequiresUnits
			final List<Unit> unitsCanBePlacedByThisProducer = (isUnitPlacementRestrictions() ? Match.getMatches(unitsLeftToPlace, unitWhichRequiresUnitsHasRequiredUnits(producer, true)) :
						new ArrayList<Unit>(unitsLeftToPlace));
			Collections.sort(unitsCanBePlacedByThisProducer, getHardestToPlaceWithRequiresUnitsRestrictions(true));
			final int maxPlaceable = maxPlaceableMap.getInt(producer);
			// System.out.println("Max Placeable: " + maxPlaceable + " for this producer: " + producer);
			if (maxPlaceable == 0)
				continue;
			final int maxForThisProducer = getMaxUnitsToBePlacedFrom(producer, unitsCanBePlacedByThisProducer, at, player);
			// System.out.println("Max Units to be placed from this producer: " + maxForThisProducer);
			// don't forget that -1 == infinite
			if (maxForThisProducer == -1 || maxForThisProducer >= unitsCanBePlacedByThisProducer.size())
			{
				performPlaceFrom(producer, unitsCanBePlacedByThisProducer, at, player);
				unitsLeftToPlace.removeAll(unitsCanBePlacedByThisProducer);
				continue;
			}
			final int neededExtra = unitsCanBePlacedByThisProducer.size() - maxForThisProducer;
			// System.out.println("Needs Extra: " + neededExtra);
			if (maxPlaceable > maxForThisProducer)
			{
				freePlacementCapacity(producer, neededExtra, unitsCanBePlacedByThisProducer, at, player);
				final int newMaxForThisProducer = getMaxUnitsToBePlacedFrom(producer, unitsCanBePlacedByThisProducer, at, player);
				if (newMaxForThisProducer != maxPlaceable)
				{
					throw new IllegalStateException("getMaxUnitsToBePlaced originally returned: " + maxPlaceable + ", \r\nWhich is not the same as it is returning after using freePlacementCapacity: "
								+ newMaxForThisProducer + ", \r\nFor territory: " + at.getName() + ", Current Producer: " + producer.getName() + ", All Producers: " + producers
								+ ", \r\nUnits Total: " + MyFormatter.unitsToTextNoOwner(units) + ", Units Left To Place By This Producer: "
								+ MyFormatter.unitsToTextNoOwner(unitsCanBePlacedByThisProducer));
				}
			}
			@SuppressWarnings("unchecked")
			final Collection<Unit> placedUnits = Match.getNMatches(unitsCanBePlacedByThisProducer, maxPlaceable, Match.ALWAYS_MATCH);
			performPlaceFrom(producer, placedUnits, at, player);
			unitsLeftToPlace.removeAll(placedUnits);
		}
		if (!unitsLeftToPlace.isEmpty())
			throw new IllegalStateException("Not all units placed in: " + at.getName() + " units: " + unitsLeftToPlace);
		// play a sound
		if (Match.someMatch(units, Matches.UnitIsInfrastructure))
			m_bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_PLACED_INFRASTRUCTURE, m_player.getName());
		else if (Match.someMatch(units, Matches.UnitIsSea))
			m_bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_PLACED_SEA, m_player.getName());
		else if (Match.someMatch(units, Matches.UnitIsAir))
			m_bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_PLACED_AIR, m_player.getName());
		else
			m_bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_PLACED_LAND, m_player.getName());
		// System.out.println("");
	}
	
	/**
	 * 
	 * @param producer
	 *            territory that produces the new units
	 * @param placeableUnits
	 *            the new units
	 * @param at
	 *            territory where the new units get placed
	 */
	protected void performPlaceFrom(final Territory producer, final Collection<Unit> placeableUnits, final Territory at, final PlayerID player)
	{
		final CompositeChange change = new CompositeChange();
		// make sure we can place consuming units
		final boolean didIt = canWeConsumeUnits(placeableUnits, at, true, change);
		if (!didIt)
			throw new IllegalStateException("Something wrong with consuming/upgrading units");
		final Collection<Unit> factoryAndInfrastructure = Match.getMatches(placeableUnits, Matches.UnitIsInfrastructure);
		if (!factoryAndInfrastructure.isEmpty())
			change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, player));
		// can we move planes to land there
		final String movedAirTranscriptTextForHistory = moveAirOntoNewCarriers(at, producer, placeableUnits, player, change);
		
		final Change remove = ChangeFactory.removeUnits(player, placeableUnits);
		final Change place = ChangeFactory.addUnits(at, placeableUnits);
		change.add(remove);
		change.add(place);
		final UndoablePlacement current_placement = new UndoablePlacement(m_player, change, producer, at, placeableUnits);
		m_placements.add(current_placement);
		updateUndoablePlacementIndexes();
		final String transcriptText = MyFormatter.unitsToTextNoOwner(placeableUnits) + " placed in " + at.getName();
		m_bridge.getHistoryWriter().startEvent(transcriptText, current_placement.getDescriptionObject());
		if (movedAirTranscriptTextForHistory != null)
			m_bridge.getHistoryWriter().addChildToEvent(movedAirTranscriptTextForHistory);
		m_bridge.addChange(change);
		updateProducedMap(producer, placeableUnits);
	}
	
	protected void updateProducedMap(final Territory producer, final Collection<Unit> additionallyProducedUnits)
	{
		final Collection<Unit> newProducedUnits = getAlreadyProduced(producer);
		newProducedUnits.addAll(additionallyProducedUnits);
		m_produced.put(producer, newProducedUnits);
	}
	
	protected void removeFromProducedMap(final Territory producer, final Collection<Unit> unitsToRemove)
	{
		final Collection<Unit> newProducedUnits = getAlreadyProduced(producer);
		newProducedUnits.removeAll(unitsToRemove);
		if (newProducedUnits.isEmpty())
			m_produced.remove(producer);
		else
			m_produced.put(producer, newProducedUnits);
	}
	
	/**
	 * frees the requested amount of capacity for the given producer by trying to hand over already made placements to other territories.
	 * This only works if one of the placements is done for another territory, more specific for a sea zone.
	 * If such placements exists it will be tried to let them be done by other adjacent territories.
	 * 
	 * @param producer
	 *            territory that needs more placement capacity
	 * @param freeSize
	 *            amount of capacity that is requested
	 */
	protected void freePlacementCapacity(final Territory producer, final int freeSize, final Collection<Unit> unitsLeftToPlace, final Territory at, final PlayerID player)
	{
		// System.out.println("Freeing Placement Capacity of: " + freeSize + " at: " + producer);
		int foundSpaceTotal = 0;
		final List<UndoablePlacement> redoPlacements = new ArrayList<UndoablePlacement>(); // placements of the producer that could be redone by other territories
		final HashMap<Territory, Integer> redoPlacementsCount = new HashMap<Territory, Integer>(); // territories the producer produced for (but not itself) and the amount of units it produced
		// find map place territory -> possible free space for producer
		for (final UndoablePlacement placement : m_placements)
		{
			// find placement move of producer that can be taken over
			if (placement.getProducerTerritory().equals(producer))
			{
				final Territory placeTerritory = placement.getPlaceTerritory();
				// units with requiresUnits are too difficult to mess with logically, so do not move them around at all
				if (placeTerritory.isWater() && !placeTerritory.equals(producer) && (!isUnitPlacementRestrictions() || !Match.someMatch(placement.getUnits(), Matches.UnitRequiresUnitsOnCreation)))
				{
					// found placement move of producer that can be taken over
					// remember move and amount of placements in that territory
					redoPlacements.add(placement);
					final Integer integer = redoPlacementsCount.get(placeTerritory);
					if (integer == null)
						redoPlacementsCount.put(placeTerritory, placement.getUnits().size());
					else
						redoPlacementsCount.put(placeTerritory, integer + placement.getUnits().size());
				}
			}
		}
		// let other producers take over placements of producer
		// remember placement move and new territory if a placement has to be split up
		final Collection<Tuple<UndoablePlacement, Territory>> splitPlacements = new ArrayList<Tuple<UndoablePlacement, Territory>>();
		for (final Entry<Territory, Integer> entry : redoPlacementsCount.entrySet())
		{
			final Territory placeTerritory = entry.getKey();
			final int maxProductionThatCanBeTakenOverFromThisPlacement = entry.getValue();
			// find other producers that could produce for the placeTerritory
			final List<Territory> potentialNewProducers = getAllProducers(placeTerritory, player, unitsLeftToPlace);
			potentialNewProducers.remove(producer);
			Collections.sort(potentialNewProducers, getBestProducerComparator(placeTerritory, unitsLeftToPlace, player));
			// we can just free a certain amount or still need a certain amount of space
			final int maxSpaceToBeFree = Math.min(maxProductionThatCanBeTakenOverFromThisPlacement, freeSize - foundSpaceTotal);
			int spaceAlreadyFree = 0; // space that got free this on this placeTerritory
			for (final Territory potentialNewProducerTerritory : potentialNewProducers)
			{
				int leftToPlace = getMaxUnitsToBePlacedFrom(potentialNewProducerTerritory, unitsPlacedInTerritorySoFar(placeTerritory), placeTerritory, player);
				if (leftToPlace == -1)
					leftToPlace = maxProductionThatCanBeTakenOverFromThisPlacement;
				// TODO: should we continue if leftToPlace is zero or less, now?
				// find placements of the producer the potentialNewProducerTerritory can take over
				for (final UndoablePlacement placement : redoPlacements)
				{
					if (!placement.getPlaceTerritory().equals(placeTerritory))
						continue;
					final Collection<Unit> placedUnits = placement.getUnits();
					final int placementSize = placedUnits.size();
					// System.out.println("UndoPlacement: " + placement.getMoveLabel());
					if (placementSize <= leftToPlace)
					{
						// potentialNewProducerTerritory can take over complete production
						placement.setProducerTerritory(potentialNewProducerTerritory);
						removeFromProducedMap(producer, placedUnits);
						updateProducedMap(potentialNewProducerTerritory, placedUnits);
						spaceAlreadyFree += placementSize;
					}
					else
					{
						// potentialNewProducerTerritory can take over ONLY parts of the production
						// remember placement and potentialNewProducerTerritory but try to avoid splitting a placement
						splitPlacements.add(new Tuple<UndoablePlacement, Territory>(placement, potentialNewProducerTerritory));
					}
					if (spaceAlreadyFree >= maxSpaceToBeFree)
						break;
				}
				if (spaceAlreadyFree >= maxSpaceToBeFree)
					break;
			}
			foundSpaceTotal += spaceAlreadyFree;
			if (foundSpaceTotal >= freeSize)
				break;
		}
		boolean unusedSplitPlacments = false; // we had a bug where we tried to split the same undoable placement twice (it can only be undone once!)
		final Collection<UndoablePlacement> usedUnoablePlacements = new ArrayList<UndoablePlacement>();
		if (foundSpaceTotal < freeSize)
		{
			// we need to split some placement moves
			for (final Tuple<UndoablePlacement, Territory> tuple : splitPlacements)
			{
				final UndoablePlacement placement = tuple.getFirst();
				if (usedUnoablePlacements.contains(placement))
				{
					unusedSplitPlacments = true;
					continue;
				}
				final Territory newProducer = tuple.getSecond();
				int leftToPlace = getMaxUnitsToBePlacedFrom(newProducer, unitsLeftToPlace, at, player);
				foundSpaceTotal += leftToPlace;
				// divide set of units that get placed
				final Collection<Unit> unitsForOldProducer = new ArrayList<Unit>(placement.getUnits());
				final Collection<Unit> unitsForNewProducer = new ArrayList<Unit>();
				for (final Unit unit : unitsForOldProducer)
				{
					if (leftToPlace == 0)
						break;
					unitsForNewProducer.add(unit);
					--leftToPlace;
				}
				unitsForOldProducer.removeAll(unitsForNewProducer);
				// split move, by undo and creating two new ones
				if (!unitsForNewProducer.isEmpty())
				{
					// there is a chance we have 2 or more splitPlacements that are using the same placement (trying to split the same placement).
					// So we must make sure that after we undo it the first time, it can never be undone again.
					usedUnoablePlacements.add(placement);
					undoMove(placement.getIndex());
					performPlaceFrom(newProducer, unitsForNewProducer, placement.getPlaceTerritory(), player);
					performPlaceFrom(producer, unitsForOldProducer, placement.getPlaceTerritory(), player);
				}
			}
		}
		if (foundSpaceTotal < freeSize && unusedSplitPlacments)
			freePlacementCapacity(producer, (freeSize - foundSpaceTotal), unitsLeftToPlace, at, player);
	}
	
	// TODO Here's the spot for special air placement rules
	protected String moveAirOntoNewCarriers(final Territory at, final Territory producer, final Collection<Unit> units, final PlayerID player, final CompositeChange placeChange)
	{
		// not water, dont bother
		if (!at.isWater())
			return null;
		// not enabled
		// if (!canProduceFightersOnCarriers())
		if (!canMoveExistingFightersToNewCarriers() || AirThatCantLandUtil.isLHTRCarrierProduction(getData()))
			return null;
		if (Match.noneMatch(units, Matches.UnitIsCarrier))
			return null;
		// do we have any spare carrier capacity
		int capacity = AirMovementValidator.carrierCapacity(units, at);
		// subtract fighters that have already been produced with this carrier
		// this turn.
		capacity -= AirMovementValidator.carrierCost(units);
		if (capacity <= 0)
			return null;
		if (!Matches.TerritoryIsLand.match(producer))
			return null;
		if (!producer.getUnits().someMatch(Matches.UnitCanProduceUnits))
			return null;
		final CompositeMatch<Unit> ownedFighters = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, Matches.unitIsOwnedBy(player));
		if (!producer.getUnits().someMatch(ownedFighters))
			return null;
		if (wasConquered(producer))
			return null;
		if (Match.someMatch(getAlreadyProduced(producer), Matches.UnitCanProduceUnits))
			return null;
		final List<Unit> fighters = producer.getUnits().getMatches(ownedFighters);
		while (fighters.size() > 0 && AirMovementValidator.carrierCost(fighters) > capacity)
		{
			fighters.remove(0);
		}
		if (fighters.size() == 0)
			return null;
		final Collection<Unit> movedFighters = getRemotePlayer().getNumberOfFightersToMoveToNewCarrier(fighters, producer);
		final Change change = ChangeFactory.moveUnits(producer, at, movedFighters);
		placeChange.add(change);
		final String transcriptText = MyFormatter.unitsToTextNoOwner(movedFighters) + " moved from " + producer.getName() + " to " + at.getName();
		return transcriptText;
		/*
		final Collection<Territory> neighbors = getData().getMap().getNeighbors(at, 1);
		final Iterator<Territory> iter = neighbors.iterator();
		while (iter.hasNext())
		{
			final Territory neighbor = iter.next();
			if (neighbor.isWater())
				continue;
			// check to see if we have a factory, only fighters from territories
			// that could
			// have produced the carrier can move there
			if (!neighbor.getUnits().someMatch(Matches.UnitCanProduceUnits))
				continue;
			// are there some fighers there that can be moved?
			if (!neighbor.getUnits().someMatch(ownedFighters))
				continue;
			if (wasConquered(neighbor))
				continue;
			if (Match.someMatch(getAlreadyProduced(neighbor), Matches.UnitCanProduceUnits))
				continue;
			final List<Unit> fighters = neighbor.getUnits().getMatches(ownedFighters);
			while (fighters.size() > 0 && AirMovementValidator.carrierCost(fighters) > capacity)
			{
				fighters.remove(0);
			}
			if (fighters.size() == 0)
				continue;
			final Collection<Unit> movedFighters = getRemotePlayer().getNumberOfFightersToMoveToNewCarrier(fighters, neighbor);
			final Change change = ChangeFactory.moveUnits(neighbor, at, movedFighters);
			placeChange.add(change);
			final String transcriptText = MyFormatter.unitsToTextNoOwner(movedFighters) + " moved from " + neighbor.getName() + " to " + at.getName();
			// only allow 1 movement
			// technically only the territory that produced the
			// carrier should be able to move fighters to the new
			// territory
			return transcriptText;
		}
		return null;*/
	}
	
	/**
	 * Subclasses can over ride this to change the way placements are made.
	 * 
	 * @return null if placement is valid
	 */
	protected String isValidPlacement(final Collection<Unit> units, final Territory at, final PlayerID player)
	{
		// do we hold enough units
		String error = playerHasEnoughUnits(units, at, player);
		if (error != null)
			return error;
		// can we produce that much
		error = canProduce(at, units, player);
		if (error != null)
			return error;
		// can we produce that much
		error = checkProduction(at, units, player);
		if (error != null)
			return error;
		// can we place it
		error = canUnitsBePlaced(at, units, player);
		if (error != null)
			return error;
		return null;
	}
	
	/**
	 * Make sure the player has enough in hand to place the units.
	 */
	String playerHasEnoughUnits(final Collection<Unit> units, final Territory at, final PlayerID player)
	{
		// make sure the player has enough units in hand to place
		if (!player.getUnits().getUnits().containsAll(units))
			return "Not enough units";
		return null;
	}
	
	/**
	 * Test whether or not the territory has the factory resources to support
	 * the placement. AlreadyProduced maps territory->units already produced
	 * this turn by that territory.
	 */
	protected String canProduce(final Territory to, final Collection<Unit> units, final PlayerID player)
	{
		final Collection<Territory> producers = getAllProducers(to, player, units, true);
		// the only reason it could be empty is if its water and no
		// territories adjacent have factories
		if (producers.isEmpty())
			return "No factory in or adjacent to " + to.getName();
		if (producers.size() == 1)
			return canProduce(producers.iterator().next(), to, units, player);
		final Collection<Territory> failingProducers = new ArrayList<Territory>();
		String error = "";
		for (final Territory producer : producers)
		{
			final String errorP = canProduce(producer, to, units, player);
			if (errorP != null)
			{
				failingProducers.add(producer);
				// do not include the error for same territory, if water, because users do not want to see this error report for 99.9% of games
				if (!(producer.equals(to) && producer.isWater()))
					error += ", " + errorP;
			}
		}
		if (producers.size() == failingProducers.size())
			return "Adjacent territories to " + to.getName() + " cannot produce, due to: \n " + error.replaceFirst(", ", "");
		return null;
	}
	
	protected String canProduce(final Territory producer, final Territory to, final Collection<Unit> units, final PlayerID player)
	{
		return canProduce(producer, to, units, player, false);
	}
	
	/**
	 * Tests if this territory can produce units. (Does not check if it has space left to do so)
	 * 
	 * @param producer
	 *            - Territory doing the producing.
	 * @param to
	 *            - Territory to be placed in.
	 * @param units
	 *            - Units to be placed.
	 * @param player
	 *            - Player doing the placing.
	 * @param simpleCheck
	 *            - If true you return true even if a factory is not present. Used when you do not want an infinite loop (getAllProducers -> canProduce -> howManyOfEachConstructionCanPlace -> getAllProducers -> etc)
	 * @return - null if allowed to produce, otherwise an error String.
	 */
	protected String canProduce(final Territory producer, final Territory to, final Collection<Unit> units, final PlayerID player, final boolean simpleCheck)
	{
		if (!producer.getOwner().equals(player))
			return producer.getName() + " is not owned by you";
		// make sure the territory wasnt conquered this turn
		if (wasConquered(producer) && !isPlacementAllowedInCapturedTerritory(player))
			return producer.getName() + " was conquered this turn and cannot produce till next turn";
		if (isPlayerAllowedToPlacementAnyTerritoryOwnedLand(player) && Matches.TerritoryIsLand.match(to) && Matches.isTerritoryOwnedBy(player).match(to))
			return null;
		if (isPlayerAllowedToPlacementAnySeaZoneByOwnedLand(player) && Matches.TerritoryIsWater.match(to) && Matches.isTerritoryOwnedBy(player).match(producer))
			return null;
		if (simpleCheck)
			return null;
		// units can be null if we are just testing the territory itself...
		final Collection<Unit> testUnits = (units == null ? new ArrayList<Unit>() : units);
		// make sure some unit has fullfilled requiresUnits requirements
		if (isUnitPlacementRestrictions() && !testUnits.isEmpty() && !Match.someMatch(testUnits, unitWhichRequiresUnitsHasRequiredUnits(producer, true)))
			return "You do not have the required units to build in " + producer.getName();
		// land factories in water can't produce, and sea factories in land can't produce. air can produce like land if in land, and like sea if in water.
		final CompositeMatchAnd<Unit> factoryMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsOwnedAndIsFactoryOrCanProduceUnits(player), Matches.unitIsBeingTransported().invert());
		if (producer.isWater())
			factoryMatch.add(Matches.UnitIsLand.invert());
		else
			factoryMatch.add(Matches.UnitIsSea.invert());
		final List<Unit> factories = producer.getUnits().getMatches(factoryMatch);
		// make sure there is a factory
		if (factories.isEmpty())
		{
			// check to see if we are producing a factory
			if (Match.someMatch(testUnits, Matches.UnitIsConstruction))
			{
				if (howManyOfEachConstructionCanPlace(to, producer, testUnits, player).totalValues() > 0) // No error, Construction to place
					return null;
				return "No more Constructions Allowed in " + producer.getName();
			}
			return "No Factory in " + producer.getName();
		}
		// check we havent just put a factory there (should we be checking producer?)
		if (Match.someMatch(getAlreadyProduced(producer), Matches.UnitCanProduceUnits) || Match.someMatch(getAlreadyProduced(to), Matches.UnitCanProduceUnits))
		{
			if (Match.someMatch(testUnits, Matches.UnitIsConstruction) && howManyOfEachConstructionCanPlace(to, producer, testUnits, player).totalValues() > 0) // you can still place a Construction
				return null;
			return "Factory in " + producer.getName() + " cant produce until 1 turn after it is created";
		}
		if (to.isWater() && (!isWW2V2() && !isUnitPlacementInEnemySeas()) && to.getUnits().someMatch(Matches.enemyUnit(player, getData())))
			return "Cannot place sea units with enemy naval units";
		return null;
	}
	
	/**
	 * Returns the territories that would do the producing if units are to be placed in a given territory. Returns an empty list if no suitable territory could be found.
	 * 
	 * @param to
	 *            - Territory to place in.
	 * @param player
	 *            - player that is placing.
	 * @param unitsToPlace
	 *            - Can be null, otherwise is the units that will be produced.
	 * @param simpleCheck
	 *            - If true you return true even if a factory is not present. Used when you do not want an infinite loop (getAllProducers -> canProduce -> howManyOfEachConstructionCanPlace -> getAllProducers -> etc)
	 * @return - List of territories that can produce here.
	 */
	protected List<Territory> getAllProducers(final Territory to, final PlayerID player, final Collection<Unit> unitsToPlace, final boolean simpleCheck)
	{
		final List<Territory> producers = new ArrayList<Territory>();
		// if not water then must produce in that territory
		if (!to.isWater())
		{
			if (simpleCheck || canProduce(to, to, unitsToPlace, player, simpleCheck) == null)
			{
				producers.add(to);
			}
			return producers;
		}
		if (canProduce(to, to, unitsToPlace, player, simpleCheck) == null)
		{
			producers.add(to);
		}
		for (final Territory current : getData().getMap().getNeighbors(to, Matches.TerritoryIsLand))
		{
			if (canProduce(current, to, unitsToPlace, player, simpleCheck) == null)
			{
				producers.add(current);
			}
		}
		return producers;
	}
	
	/**
	 * Test whether or not the territory has the factory resources to support
	 * the placement. AlreadyProduced maps territory->units already produced
	 * this turn by that territory.
	 */
	protected String checkProduction(final Territory to, final Collection<Unit> units, final PlayerID player)
	{
		final List<Territory> producers = getAllProducers(to, player, units);
		if (producers.isEmpty())
			return "No factory in or adjacent to " + to.getName();
		// if its an original factory then unlimited production
		Collections.sort(producers, getBestProducerComparator(to, units, player));
		final TerritoryAttachment ta = TerritoryAttachment.get(producers.iterator().next());
		// WW2V2, you cant place factories in territories with no production
		if (isWW2V2() && ta.getProduction() == 0 && !Match.someMatch(units, Matches.UnitIsConstruction))
			return "Cannot place factory, that territory cant produce any units";
		if (!getCanAllUnitsWithRequiresUnitsBePlacedCorrectly(units, to))
			return "Cannot place more units which require units, than production capacity of territories with the required units";
		final int maxUnitsToBePlaced = getMaxUnitsToBePlaced(units, to, player, true);
		if ((maxUnitsToBePlaced != -1) && (maxUnitsToBePlaced < units.size()))
			return "Cannot place " + units.size() + " more units in " + to.getName();
		return null;
	}
	
	public String canUnitsBePlaced(final Territory to, final Collection<Unit> units, final PlayerID player)
	{
		final Collection<Unit> allowedUnits = getUnitsToBePlaced(to, units, player);
		if (allowedUnits == null || !allowedUnits.containsAll(units))
		{
			return "Cannot place these units in " + to.getName();
		}
		final IntegerMap<String> constructionMap = howManyOfEachConstructionCanPlace(to, to, units, player);
		for (final Unit currentUnit : Match.getMatches(units, Matches.UnitIsConstruction))
		{
			final UnitAttachment ua = UnitAttachment.get(currentUnit.getUnitType());
			/*if (ua.getIsFactory() && !ua.getIsConstruction())
				constructionMap.add("factory", -1);
			else*/
			constructionMap.add(ua.getConstructionType(), -1);
		}
		if (!constructionMap.isPositive())
			return "Too many constructions in " + to.getName();
		final List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, getData()));
		if (!capitalsListOwned.contains(to) && isPlacementInCapitalRestricted(player))
			return "Cannot place these units outside of the capital";
		if (to.isWater())
		{
			final String canLand = validateNewAirCanLandOnCarriers(to, units);
			if (canLand != null)
				return canLand;
		}
		else
		{
			// make sure we own the territory
			if (!to.getOwner().equals(player))
				return "You don't own " + to.getName();
			// make sure all units are land
			if (!Match.allMatch(units, Matches.UnitIsNotSea))
				return "Cant place sea units on land";
		}
		// make sure we can place consuming units
		if (!canWeConsumeUnits(units, to, false, null))
			return "Not Enough Units To Upgrade or Be Consumed";
		// now check for stacking limits
		final Collection<UnitType> typesAlreadyChecked = new ArrayList<UnitType>();
		for (final Unit currentUnit : units)
		{
			final UnitType ut = currentUnit.getType();
			if (typesAlreadyChecked.contains(ut))
				continue;
			typesAlreadyChecked.add(ut);
			final int maxForThisType = UnitAttachment.getMaximumNumberOfThisUnitTypeToReachStackingLimit("placementLimit", ut, to, player, getData());
			if (Match.countMatches(units, Matches.unitIsOfType(ut)) > maxForThisType)
				return "UnitType " + ut.getName() + " is over stacking limit of " + maxForThisType;
		}
		if (!PlayerAttachment.getCanTheseUnitsMoveWithoutViolatingStackingLimit("placementLimit", units, to, player, getData()))
			return "Units Can Not Go Over Stacking Limit";
		// now return null (valid placement) if we have placement restrictions disabled in game options
		if (!isUnitPlacementRestrictions())
			return null;
		// account for any unit placement restrictions by territory
		for (final Unit currentUnit : units)
		{
			final UnitAttachment ua = UnitAttachment.get(currentUnit.getUnitType());
			final TerritoryAttachment ta = TerritoryAttachment.get(to);
			if (ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1 && ua.getCanOnlyBePlacedInTerritoryValuedAtX() > ta.getProduction())
				return "Cannot place these units in " + to.getName() + " due to Unit Placement Restrictions on Territory Value";
			final String[] terrs = ua.getUnitPlacementRestrictions();
			final Collection<Territory> listedTerrs = getListedTerritories(terrs);
			if (listedTerrs.contains(to))
				return "Cannot place these units in " + to.getName() + " due to Unit Placement Restrictions";
			if (unitWhichRequiresUnitsHasRequiredUnits(to, false).invert().match(currentUnit))
				return "Cannot place these units in " + to.getName() + " as territory does not contain required units at start of turn";
			if (Matches.UnitCanOnlyPlaceInOriginalTerritories.match(currentUnit) && !Matches.TerritoryIsOriginallyOwnedBy(player).match(to))
				return "Cannot place these units in " + to.getName() + " as territory is not originally owned";
		}
		return null;
	}
	
	protected Collection<Unit> getUnitsToBePlaced(final Territory to, final Collection<Unit> units, final PlayerID player)
	{
		if (to.isWater())
		{
			return getUnitsToBePlacedSea(to, units, player);
		}
		// if land
		return getUnitsToBePlacedLand(to, units, player);
	}
	
	protected Collection<Unit> getUnitsToBePlacedSea(final Territory to, final Collection<Unit> units, final PlayerID player)
	{
		final Collection<Unit> unitsAtStartOfTurnInTO = unitsAtStartOfStepInTerritory(to);
		final Collection<Unit> allProducedUnits = unitsPlacedInTerritorySoFar(to);
		final Collection<Unit> placeableUnits = new ArrayList<Unit>();
		// Land units wont do
		placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsSea));
		// if can place new fighters on NEW CVs ---OR--- can place new fighters on OLD CVs
		if (((canProduceFightersOnCarriers() || AirThatCantLandUtil.isLHTRCarrierProduction(getData())) && Match.someMatch(allProducedUnits, Matches.UnitIsCarrier))
					|| ((canProduceNewFightersOnOldCarriers() || AirThatCantLandUtil.isLHTRCarrierProduction(getData())) && Match.someMatch(to.getUnits().getUnits(), Matches.UnitIsCarrier)))
		{
			final CompositeMatch<Unit> airThatCanLandOnCarrier = new CompositeMatchAnd<Unit>();
			airThatCanLandOnCarrier.add(Matches.UnitIsAir);
			airThatCanLandOnCarrier.add(Matches.UnitCanLandOnCarrier);
			placeableUnits.addAll(Match.getMatches(units, airThatCanLandOnCarrier));
		}
		if ((!isWW2V2() && !isUnitPlacementInEnemySeas()) && to.getUnits().someMatch(Matches.enemyUnit(player, getData())))
			return null;
		// remove any units that require other units to be consumed on creation (veqryn)
		if (Match.someMatch(placeableUnits, Matches.UnitConsumesUnitsOnCreation))
		{
			final Collection<Unit> unitsWhichConsume = Match.getMatches(placeableUnits, Matches.UnitConsumesUnitsOnCreation);
			for (final Unit unit : unitsWhichConsume)
			{
				if (Matches.UnitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTO, to).invert().match(unit))
					placeableUnits.remove(unit);
			}
		}
		if (!isUnitPlacementRestrictions())
			return placeableUnits;
		final Collection<Unit> placeableUnits2 = new ArrayList<Unit>();
		for (final Unit currentUnit : placeableUnits)
		{
			final UnitAttachment ua = UnitAttachment.get(currentUnit.getUnitType());
			final TerritoryAttachment ta = TerritoryAttachment.get(to);
			if (ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1 && ua.getCanOnlyBePlacedInTerritoryValuedAtX() > ta.getProduction())
				continue;
			if (unitWhichRequiresUnitsHasRequiredUnits(to, false).invert().match(currentUnit))
				continue;
			if (Matches.UnitCanOnlyPlaceInOriginalTerritories.match(currentUnit) && !Matches.TerritoryIsOriginallyOwnedBy(player).match(to))
				continue;
			// account for any unit placement restrictions by territory
			final String[] terrs = ua.getUnitPlacementRestrictions();
			final Collection<Territory> listedTerrs = getListedTerritories(terrs);
			if (!listedTerrs.contains(to))
				placeableUnits2.add(currentUnit);
		}
		return placeableUnits2;
	}
	
	protected Collection<Unit> getUnitsToBePlacedLand(final Territory to, final Collection<Unit> units, final PlayerID player)
	{
		final Collection<Unit> placeableUnits = new ArrayList<Unit>();
		final Collection<Unit> unitsAtStartOfTurnInTO = unitsAtStartOfStepInTerritory(to);
		final boolean wasFactoryThereAtStart = wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(to, player);
		if (wasFactoryThereAtStart || isPlayerAllowedToPlacementAnyTerritoryOwnedLand(player))
		{
			final CompositeMatch<Unit> groundUnits = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitIsNotConstruction); // we add factories and constructions later
			final CompositeMatch<Unit> airUnits = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.UnitIsNotConstruction);
			placeableUnits.addAll(Match.getMatches(units, groundUnits));
			placeableUnits.addAll(Match.getMatches(units, airUnits));
		}
		if (Match.someMatch(units, Matches.UnitIsConstruction))
		{
			final IntegerMap<String> constructionsMap = howManyOfEachConstructionCanPlace(to, to, units, player);
			final Collection<Unit> skipUnit = new ArrayList<Unit>();
			for (final Unit currentUnit : Match.getMatches(units, Matches.UnitIsConstruction))
			{
				final int maxUnits = howManyOfConstructionUnit(currentUnit, constructionsMap);
				if (maxUnits > 0)
				{
					// we are doing this because we could have multiple unitTypes with the same constructionType, so we have to be able to place the max placement by constructionType of each unitType
					if (skipUnit.contains(currentUnit))
						continue;
					placeableUnits.addAll(Match.getNMatches(units, maxUnits, Matches.unitIsOfType(currentUnit.getType())));
					skipUnit.addAll(Match.getMatches(units, Matches.unitIsOfType(currentUnit.getType())));
				}
			}
		}
		// remove any units that require other units to be consumed on creation (veqryn)
		if (Match.someMatch(placeableUnits, Matches.UnitConsumesUnitsOnCreation))
		{
			final Collection<Unit> unitsWhichConsume = Match.getMatches(placeableUnits, Matches.UnitConsumesUnitsOnCreation);
			for (final Unit unit : unitsWhichConsume)
			{
				if (Matches.UnitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTO, to).invert().match(unit))
					placeableUnits.remove(unit);
			}
		}
		// now check stacking limits
		final Collection<Unit> placeableUnits2 = new ArrayList<Unit>();
		final Collection<UnitType> typesAlreadyChecked = new ArrayList<UnitType>();
		for (final Unit currentUnit : placeableUnits)
		{
			final UnitType ut = currentUnit.getType();
			if (typesAlreadyChecked.contains(ut))
				continue;
			typesAlreadyChecked.add(ut);
			placeableUnits2.addAll(Match.getNMatches(placeableUnits, UnitAttachment.getMaximumNumberOfThisUnitTypeToReachStackingLimit("placementLimit", ut, to, player, getData()),
						Matches.unitIsOfType(ut)));
		}
		if (!isUnitPlacementRestrictions())
			return placeableUnits2;
		final Collection<Unit> placeableUnits3 = new ArrayList<Unit>();
		for (final Unit currentUnit : placeableUnits2)
		{
			final UnitAttachment ua = UnitAttachment.get(currentUnit.getUnitType());
			final TerritoryAttachment ta = TerritoryAttachment.get(to);
			if (ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1 && ua.getCanOnlyBePlacedInTerritoryValuedAtX() > ta.getProduction())
				continue;
			if (unitWhichRequiresUnitsHasRequiredUnits(to, false).invert().match(currentUnit))
				continue;
			if (Matches.UnitCanOnlyPlaceInOriginalTerritories.match(currentUnit) && !Matches.TerritoryIsOriginallyOwnedBy(player).match(to))
				continue;
			// account for any unit placement restrictions by territory
			final String[] terrs = ua.getUnitPlacementRestrictions();
			final Collection<Territory> listedTerrs = getListedTerritories(terrs);
			if (!listedTerrs.contains(to))
				placeableUnits3.add(currentUnit);
		}
		return placeableUnits3;
	}
	
	protected boolean canWeConsumeUnits(final Collection<Unit> units, final Territory to, final boolean actuallyDoIt, final CompositeChange change)
	{
		boolean weCanConsume = true;
		final Collection<Unit> unitsAtStartOfTurnInTO = unitsAtStartOfStepInTerritory(to);
		final Collection<Unit> removedUnits = new ArrayList<Unit>();
		final Collection<Unit> unitsWhichConsume = Match.getMatches(units, Matches.UnitConsumesUnitsOnCreation);
		for (final Unit unit : unitsWhichConsume)
		{
			if (Matches.UnitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTO, to).invert().match(unit))
				weCanConsume = false;
			if (!weCanConsume)
				break;
			// remove units which are now consumed, then test the rest of the consuming units on the diminishing pile of units which were in the territory at start of turn
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
			final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
			for (final UnitType ut : requiredUnits)
			{
				final int requiredNumber = requiredUnitsMap.getInt(ut);
				final Match<Unit> unitIsOwnedByAndOfTypeAndNotDamaged = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(unit.getOwner()), Matches.unitIsOfType(ut), Matches.UnitHasSomeUnitDamage()
							.invert(), Matches.UnitIsNotDamaged, Matches.UnitIsDisabled().invert(), Matches.unitIsInTerritoryThatHasTerritoryDamage(to).invert());
				final Collection<Unit> unitsBeingRemoved = Match.getNMatches(unitsAtStartOfTurnInTO, requiredNumber, unitIsOwnedByAndOfTypeAndNotDamaged);
				unitsAtStartOfTurnInTO.removeAll(unitsBeingRemoved);
				// if we should actually do it, not just test, then add to bridge
				if (actuallyDoIt && change != null)
				{
					final Change remove = ChangeFactory.removeUnits(to, unitsBeingRemoved);
					change.add(remove);
					removedUnits.addAll(unitsBeingRemoved);
				}
			}
		}
		if (weCanConsume && actuallyDoIt && change != null && !change.isEmpty())
		{
			m_bridge.getHistoryWriter().startEvent("Units in " + to.getName() + " being upgraded or consumed: " + MyFormatter.unitsToTextNoOwner(removedUnits), removedUnits);
		}
		return weCanConsume;
	}
	
	/**
	 * Returns -1 if can place unlimited units
	 */
	protected int getMaxUnitsToBePlaced(final Collection<Unit> units, final Territory to, final PlayerID player, final boolean countSwitchedProductionToNeighbors)
	{
		final IntegerMap<Territory> map = getMaxUnitsToBePlacedMap(units, to, player, countSwitchedProductionToNeighbors);
		int production = 0;
		for (final Entry<Territory, Integer> entry : map.entrySet())
		{
			final int prodT = entry.getValue();
			if (prodT == -1)
				return -1;
			production += prodT;
		}
		return production;
	}
	
	/**
	 * Returns -1 somewhere in the map if can place unlimited units
	 */
	protected IntegerMap<Territory> getMaxUnitsToBePlacedMap(final Collection<Unit> units, final Territory to, final PlayerID player, final boolean countSwitchedProductionToNeighbors)
	{
		final IntegerMap<Territory> rVal = new IntegerMap<Territory>();
		final List<Territory> producers = getAllProducers(to, player, units);
		if (producers.isEmpty())
			return rVal;
		Collections.sort(producers, getBestProducerComparator(to, units, player));
		final Collection<Territory> notUsableAsOtherProducers = new ArrayList<Territory>();
		notUsableAsOtherProducers.addAll(producers);
		final Map<Territory, Integer> currentAvailablePlacementForOtherProducers = new HashMap<Territory, Integer>();
		for (final Territory producerTerritory : producers)
		{
			final Collection<Unit> unitsCanBePlacedByThisProducer = (isUnitPlacementRestrictions() ? Match.getMatches(units, unitWhichRequiresUnitsHasRequiredUnits(producerTerritory, true))
						: new ArrayList<Unit>(units));
			final int prodT = getMaxUnitsToBePlacedFrom(producerTerritory, unitsCanBePlacedByThisProducer, to, player, countSwitchedProductionToNeighbors, notUsableAsOtherProducers,
						currentAvailablePlacementForOtherProducers);
			rVal.put(producerTerritory, prodT);
		}
		return rVal;
	}
	
	/**
	 * Returns -1 if can place unlimited units
	 */
	protected int getMaxUnitsToBePlacedFrom(final Territory producer, final Collection<Unit> units, final Territory to, final PlayerID player)
	{
		return getMaxUnitsToBePlacedFrom(producer, units, to, player, false, null, null);
	}
	
	/**
	 * Returns -1 if can place unlimited units
	 */
	protected int getMaxUnitsToBePlacedFrom(final Territory producer, final Collection<Unit> units, final Territory to, final PlayerID player, final boolean countSwitchedProductionToNeighbors,
				final Collection<Territory> notUsableAsOtherProducers, final Map<Territory, Integer> currentAvailablePlacementForOtherProducers)
	{
		// we may have special units with requiresUnits restrictions
		final Collection<Unit> unitsCanBePlacedByThisProducer = (isUnitPlacementRestrictions() ? Match.getMatches(units, unitWhichRequiresUnitsHasRequiredUnits(producer, true)) :
					new ArrayList<Unit>(units));
		if (unitsCanBePlacedByThisProducer.size() <= 0)
			return 0;
		// if its an original factory then unlimited production
		final TerritoryAttachment ta = TerritoryAttachment.get(producer);
		final CompositeMatchAnd<Unit> factoryMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsOwnedAndIsFactoryOrCanProduceUnits(player), Matches.unitIsBeingTransported().invert());
		if (producer.isWater())
			factoryMatch.add(Matches.UnitIsLand.invert());
		else
			factoryMatch.add(Matches.UnitIsSea.invert());
		final Collection<Unit> factoryUnits = producer.getUnits().getMatches(factoryMatch);
		// boolean placementRestrictedByFactory = isPlacementRestrictedByFactory();
		final boolean unitPlacementPerTerritoryRestricted = isUnitPlacementPerTerritoryRestricted();
		final boolean originalFactory = ta.getOriginalFactory();
		final boolean playerIsOriginalOwner = factoryUnits.size() > 0 ? m_player.equals(getOriginalFactoryOwner(producer)) : false;
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		final Collection<Unit> alreadProducedUnits = getAlreadyProduced(producer);
		final int unitCountAlreadyProduced = alreadProducedUnits.size();
		if (originalFactory && playerIsOriginalOwner) // && !placementRestrictedByFactory && !unitPlacementPerTerritoryRestricted
		{
			if (ra != null && ra.getMaxPlacePerTerritory() != -1)
			{
				return Math.max(0, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced);
			}
			return -1;
		}
		// Restricts based on the STARTING number of units in a territory (otherwise it is infinite placement)
		if (unitPlacementPerTerritoryRestricted)
		{
			if (ra != null && ra.getPlacementPerTerritory() > 0)
			{
				final int allowedPlacement = ra.getPlacementPerTerritory();
				final int ownedUnitsInTerritory = Match.countMatches(to.getUnits().getUnits(), Matches.unitIsOwnedBy(player));
				if (ownedUnitsInTerritory >= allowedPlacement)
					return 0;
				if (ra.getMaxPlacePerTerritory() == -1)
					return -1;
				return Math.max(0, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced);
			}
		}
		// a factory can produce the same number of units as the number of PUs the territory generates each turn (or not, if it has canProduceXUnits)
		int production = 0;
		// int territoryValue = getProduction(producer);
		final int maxConstructions = howManyOfEachConstructionCanPlace(to, producer, unitsCanBePlacedByThisProducer, player).totalValues();
		final boolean wasFactoryThereAtStart = wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(producer, player);
		// If there's NO factory, allow placement of the factory
		if (!wasFactoryThereAtStart)
		{
			if (ra != null && ra.getMaxPlacePerTerritory() > 0)
				return Math.max(0, Math.min(maxConstructions, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced));
			return Math.max(0, maxConstructions);
		}
		// getHowMuchCanUnitProduce accounts for IncreasedFactoryProduction, but does not account for maxConstructions
		production = TripleAUnit.getProductionPotentialOfTerritory(unitsAtStartOfStepInTerritory(producer), producer, player, getData(), true, true);
		// increase the production by the number of constructions allowed
		if (maxConstructions > 0)
			production += maxConstructions;
		// return 0 if less than 0
		if (production < 0)
			return 0;
		production += Match.countMatches(alreadProducedUnits, Matches.UnitIsConstruction);
		
		// Now we check if units we have already produced here could be produced by a different producer
		int unitCountHaveToAndHaveBeenBeProducedHere = unitCountAlreadyProduced;
		if (countSwitchedProductionToNeighbors && unitCountAlreadyProduced > 0)
		{
			if (notUsableAsOtherProducers == null)
				throw new IllegalStateException("notUsableAsOtherProducers can not be null if countSwitchedProductionToNeighbors is true");
			if (currentAvailablePlacementForOtherProducers == null)
				throw new IllegalStateException("currentAvailablePlacementForOtherProducers can not be null if countSwitchedProductionToNeighbors is true");
			int productionCanNotBeMoved = 0;
			int productionThatCanBeTakenOver = 0;
			// try to find a placement move (to an adjacent sea zone) that can be taken over by some other territory factory
			for (final UndoablePlacement placementMove : m_placements)
			{
				if (placementMove.getProducerTerritory().equals(producer))
				{
					final Territory placeTerritory = placementMove.getPlaceTerritory();
					final Collection<Unit> unitsPlacedByCurrentPlacementMove = placementMove.getUnits();
					// TODO: Units which have the unit attachment property, requiresUnits, are too difficult to mess with logically, so we ignore them for our special 'move shit around' methods.
					if (!placeTerritory.isWater() || (isUnitPlacementRestrictions() && Match.someMatch(unitsPlacedByCurrentPlacementMove, Matches.UnitRequiresUnitsOnCreation)))
					{
						productionCanNotBeMoved += unitsPlacedByCurrentPlacementMove.size();
					}
					else
					{
						final int maxProductionThatCanBeTakenOverFromThisPlacement = unitsPlacedByCurrentPlacementMove.size();
						int productionThatCanBeTakenOverFromThisPlacement = 0;
						// find other producers for this placement move to the same water territory
						final List<Territory> newPotentialOtherProducers = getAllProducers(placeTerritory, player, unitsCanBePlacedByThisProducer);
						newPotentialOtherProducers.removeAll(notUsableAsOtherProducers);
						Collections.sort(newPotentialOtherProducers, getBestProducerComparator(placeTerritory, unitsCanBePlacedByThisProducer, player));
						for (final Territory potentialOtherProducer : newPotentialOtherProducers)
						{
							Integer potential = currentAvailablePlacementForOtherProducers.get(potentialOtherProducer);
							if (potential == null)
								potential = getMaxUnitsToBePlacedFrom(potentialOtherProducer, unitsPlacedInTerritorySoFar(placeTerritory), placeTerritory, player);
							if (potential == -1)
							{
								currentAvailablePlacementForOtherProducers.put(potentialOtherProducer, potential);
								productionThatCanBeTakenOverFromThisPlacement = maxProductionThatCanBeTakenOverFromThisPlacement;
								break;
							}
							else
							{
								final int needed = maxProductionThatCanBeTakenOverFromThisPlacement - productionThatCanBeTakenOverFromThisPlacement;
								final int surplus = potential - needed;
								if (surplus > 0)
								{
									currentAvailablePlacementForOtherProducers.put(potentialOtherProducer, surplus);
									productionThatCanBeTakenOverFromThisPlacement += needed;
								}
								else
								{
									currentAvailablePlacementForOtherProducers.put(potentialOtherProducer, 0);
									productionThatCanBeTakenOverFromThisPlacement += potential;
									notUsableAsOtherProducers.add(potentialOtherProducer);
								}
								if (surplus >= 0)
									break;
							}
						}
						if (productionThatCanBeTakenOverFromThisPlacement > maxProductionThatCanBeTakenOverFromThisPlacement)
							throw new IllegalStateException("productionThatCanBeTakenOverFromThisPlacement should never be larger than maxProductionThatCanBeTakenOverFromThisPlacement");
						productionThatCanBeTakenOver += productionThatCanBeTakenOverFromThisPlacement;
					}
					if (productionThatCanBeTakenOver >= unitCountAlreadyProduced - productionCanNotBeMoved)
						break;
				}
			}
			unitCountHaveToAndHaveBeenBeProducedHere = Math.max(0, unitCountAlreadyProduced - productionThatCanBeTakenOver);
		}
		if (ra != null && ra.getMaxPlacePerTerritory() > 0)
			return Math.max(0, Math.min(production - unitCountHaveToAndHaveBeenBeProducedHere, ra.getMaxPlacePerTerritory() - unitCountHaveToAndHaveBeenBeProducedHere));
		return Math.max(0, production - unitCountHaveToAndHaveBeenBeProducedHere);
	}
	
	/**
	 * @return gets the production of the territory
	 */
	protected int getProduction(final Territory territory)
	{
		final TerritoryAttachment ta = TerritoryAttachment.get(territory);
		if (ta != null)
			return ta.getProduction();
		return 0;
	}
	
	/**
	 * @param to
	 *            referring territory
	 * @param units
	 *            units to place
	 * @param player
	 *            PlayerID
	 * @return an empty IntegerMap if you can't produce any constructions (will never return null)
	 */
	public IntegerMap<String> howManyOfEachConstructionCanPlace(final Territory to, final Territory producer, final Collection<Unit> units, final PlayerID player)
	{
		// constructions can ONLY be produced BY the same territory that they are going into!
		if (!to.equals(producer) || units == null || units.isEmpty() || !Match.someMatch(units, Matches.UnitIsConstruction))
			return new IntegerMap<String>();
		final Collection<Unit> unitsAtStartOfTurnInTO = unitsAtStartOfStepInTerritory(to);
		final Collection<Unit> unitsInTO = to.getUnits().getUnits();
		final Collection<Unit> unitsPlacedAlready = getAlreadyProduced(to);
		// build an integer map of each unit we have in our list of held units, as well as integer maps for maximum units and units per turn
		final IntegerMap<String> unitMapHeld = new IntegerMap<String>();
		final IntegerMap<String> unitMapMaxType = new IntegerMap<String>();
		final IntegerMap<String> unitMapTypePerTurn = new IntegerMap<String>();
		final int maxFactory = games.strategy.triplea.Properties.getFactoriesPerCountry(getData());
		final Iterator<Unit> unitHeldIter = Match.getMatches(units, Matches.UnitIsConstruction).iterator();
		final TerritoryAttachment terrAttachment = TerritoryAttachment.get(to);
		int toProduction = 0;
		if (terrAttachment != null)
			toProduction = terrAttachment.getProduction();
		while (unitHeldIter.hasNext())
		{
			final Unit currentUnit = unitHeldIter.next();
			final UnitAttachment ua = UnitAttachment.get(currentUnit.getUnitType());
			// account for any unit placement restrictions by territory
			if (isUnitPlacementRestrictions())
			{
				final String[] terrs = ua.getUnitPlacementRestrictions();
				final Collection<Territory> listedTerrs = getListedTerritories(terrs);
				if (listedTerrs.contains(to))
					continue;
				if (ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1 && ua.getCanOnlyBePlacedInTerritoryValuedAtX() > toProduction)
					continue;
				if (unitWhichRequiresUnitsHasRequiredUnits(to, false).invert().match(currentUnit))
					continue;
			}
			// remove any units that require other units to be consumed on creation (veqryn)
			if (Matches.UnitConsumesUnitsOnCreation.match(currentUnit) && Matches.UnitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTO, to).invert().match(currentUnit))
				continue;
			unitMapHeld.add(ua.getConstructionType(), 1);
			unitMapTypePerTurn.put(ua.getConstructionType(), ua.getConstructionsPerTerrPerTypePerTurn());
			if (ua.getConstructionType().equals("factory"))
				unitMapMaxType.put(ua.getConstructionType(), maxFactory);
			else
				unitMapMaxType.put(ua.getConstructionType(), ua.getMaxConstructionsPerTypePerTerr());
		}
		final boolean moreWithoutFactory = games.strategy.triplea.Properties.getMoreConstructionsWithoutFactory(getData());
		final boolean moreWithFactory = games.strategy.triplea.Properties.getMoreConstructionsWithFactory(getData());
		final boolean unlimitedConstructions = games.strategy.triplea.Properties.getUnlimitedConstructions(getData());
		final boolean wasFactoryThereAtStart = wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(to, player);
		// build an integer map of each construction unit in the territory
		final IntegerMap<String> unitMapTO = new IntegerMap<String>();
		if (Match.someMatch(unitsInTO, Matches.UnitIsConstruction))
		{
			for (final Unit currentUnit : Match.getMatches(unitsInTO, Matches.UnitIsConstruction))
			{
				final UnitAttachment ua = UnitAttachment.get(currentUnit.getUnitType());
				/*if (Matches.UnitIsFactory.match(currentUnit) && !ua.getIsConstruction())
					unitMapTO.add("factory", 1);
				else*/
				unitMapTO.add(ua.getConstructionType(), 1);
			}
			// account for units already in the territory, based on max
			final Iterator<String> mapString = unitMapHeld.keySet().iterator();
			while (mapString.hasNext())
			{
				final String constructionType = mapString.next();
				int unitMax = unitMapMaxType.getInt(constructionType);
				if (wasFactoryThereAtStart && !constructionType.equals("factory") && !constructionType.endsWith("structure"))
					unitMax = Math.max(Math.max(unitMax, (moreWithFactory ? toProduction : 0)), (unlimitedConstructions ? 10000 : 0));
				if (!wasFactoryThereAtStart && !constructionType.equals("factory") && !constructionType.endsWith("structure"))
					unitMax = Math.max(Math.max(unitMax, (moreWithoutFactory ? toProduction : 0)), (unlimitedConstructions ? 10000 : 0));
				unitMapHeld.put(constructionType, Math.max(0, Math.min(unitMax - unitMapTO.getInt(constructionType), unitMapHeld.getInt(constructionType))));
			}
		}
		// deal with already placed units
		final Iterator<Unit> unitAlready = Match.getMatches(unitsPlacedAlready, Matches.UnitIsConstruction).iterator();
		while (unitAlready.hasNext())
		{
			final Unit currentUnit = unitAlready.next();
			final UnitAttachment ua = UnitAttachment.get(currentUnit.getUnitType());
			unitMapTypePerTurn.add(ua.getConstructionType(), -1);
		}
		// modify this list based on how many we can place per turn
		final IntegerMap<String> unitsAllowed = new IntegerMap<String>();
		final Iterator<String> mapString2 = unitMapHeld.keySet().iterator();
		while (mapString2.hasNext())
		{
			final String constructionType = mapString2.next();
			final int unitAllowed = Math.max(0, Math.min(unitMapTypePerTurn.getInt(constructionType), unitMapHeld.getInt(constructionType)));
			if (unitAllowed > 0)
				unitsAllowed.put(constructionType, unitAllowed);
		}
		// return our integer map
		return unitsAllowed;
	}
	
	public int howManyOfConstructionUnit(final Unit unit, final IntegerMap<String> constructionsMap)
	{
		final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
		if (/*!ua.getIsFactory() &&*/(!ua.getIsConstruction() || ua.getConstructionsPerTerrPerTypePerTurn() < 1 || ua.getMaxConstructionsPerTypePerTerr() < 1))
			return 0;
		/*if (ua.getIsFactory() && !ua.getIsConstruction())
			return constructionsMap.getInt("factory");*/
		return Math.max(0, constructionsMap.getInt(ua.getConstructionType()));
	}
	
	/**
	 * 
	 * @param to
	 *            - Territory we are testing for required units
	 * @param doNotCountNeighbors
	 *            - If false, and 'to' is water, then we will test neighboring land territories to see if they have any of the required units as well.
	 * @return - Whether the territory contains one of the required combos of units
	 *         (and if 'doNotCountNeighbors' is false, and unit is Sea unit, will return true if an adjacent land territory has one of the required combos as well).
	 */
	public Match<Unit> unitWhichRequiresUnitsHasRequiredUnits(final Territory to, final boolean doNotCountNeighbors)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unitWhichRequiresUnits)
			{
				if (!Matches.UnitRequiresUnitsOnCreation.match(unitWhichRequiresUnits))
					return true;
				final Collection<Unit> unitsAtStartOfTurnInProducer = unitsAtStartOfStepInTerritory(to);
				// do not need to remove unowned here, as this match will remove unowned units from consideration.
				if (Matches.UnitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInProducer).match(unitWhichRequiresUnits))
					return true;
				if (!doNotCountNeighbors)
				{
					if (Matches.UnitIsSea.match(unitWhichRequiresUnits))
					{
						for (final Territory current : getAllProducers(to, m_player, Collections.singletonList(unitWhichRequiresUnits), true))
						{
							final Collection<Unit> unitsAtStartOfTurnInCurrent = unitsAtStartOfStepInTerritory(current);
							if (Matches.UnitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInCurrent).match(unitWhichRequiresUnits))
								return true;
						}
					}
				}
				return false;
			}
		};
	}
	
	public boolean getCanAllUnitsWithRequiresUnitsBePlacedCorrectly(final Collection<Unit> units, final Territory to)
	{
		if (!isUnitPlacementRestrictions() || !Match.someMatch(units, Matches.UnitRequiresUnitsOnCreation))
			return true;
		final IntegerMap<Territory> producersMap = getMaxUnitsToBePlacedMap(units, to, m_player, true);
		final List<Territory> producers = getAllProducers(to, m_player, units);
		if (producers.isEmpty())
			return false;
		Collections.sort(producers, getBestProducerComparator(to, units, m_player));
		final Collection<Unit> unitsLeftToPlace = new ArrayList<Unit>(units);
		for (final Territory t : producers)
		{
			if (unitsLeftToPlace.isEmpty())
				return true;
			final int productionHere = producersMap.getInt(t);
			final List<Unit> canBePlacedHere = Match.getMatches(unitsLeftToPlace, unitWhichRequiresUnitsHasRequiredUnits(t, true));
			if (productionHere == -1 || productionHere >= canBePlacedHere.size())
			{
				unitsLeftToPlace.removeAll(canBePlacedHere);
				continue;
			}
			Collections.sort(canBePlacedHere, getHardestToPlaceWithRequiresUnitsRestrictions(true));
			@SuppressWarnings("unchecked")
			final Collection<Unit> placedHere = Match.getNMatches(canBePlacedHere, productionHere, Match.ALWAYS_MATCH);
			unitsLeftToPlace.removeAll(placedHere);
		}
		if (unitsLeftToPlace.isEmpty())
			return true;
		return false;
	}
	
	/*public Map<Unit, Collection<Territory>> getTerritoriesWhereUnitsWithRequiresUnitsCanBeProducedFrom(final Collection<Unit> units, final Territory to)
	{
		final Map<Unit, Collection<Territory>> rVal = new HashMap<Unit, Collection<Territory>>();
		for (final Unit u : units)
		{
			final Collection<Territory> allowedTerrs = new ArrayList<Territory>();
			final Collection<Territory> allProducers = getAllProducers(to, m_player, Collections.singletonList(u));
			// if our units has the required unit in the territory it is being produced to, then we are all good (ex: the sea unit requires a Wet_Dock in the sea zone)
			if (!Matches.UnitRequiresUnitsOnCreation.match(u) || unitWhichRequiresUnitsHasRequiredUnits(to, true).match(u))
			{
				allowedTerrs.addAll(allProducers);
			}
			else
			{
				for (final Territory t : allProducers)
				{
					if (unitWhichRequiresUnitsHasRequiredUnits(t, true).match(u))
						allowedTerrs.add(t);
				}
			}
			rVal.put(u, allowedTerrs);
		}
		return rVal;
	}*/

	protected Comparator<Territory> getBestProducerComparator(final Territory to, final Collection<Unit> units, final PlayerID player)
	{
		return new Comparator<Territory>()
		{
			public int compare(final Territory t1, final Territory t2)
			{
				if (t1 == t2 || t1.equals(t2))
					return 0;
				// producing to territory comes first
				if (to == t1 || to.equals(t1))
					return -1;
				else if (to == t2 || to.equals(t2))
					return 1;
				final int left1 = getMaxUnitsToBePlacedFrom(t1, units, to, player);
				final int left2 = getMaxUnitsToBePlacedFrom(t2, units, to, player);
				if (left1 == left2)
					return 0;
				// production of -1 == infinite
				if (left1 == -1)
					return -1;
				if (left2 == -1)
					return 1;
				if (left1 > left2)
					return -1;
				return 1;
			}
		};
	}
	
	protected Comparator<Unit> getUnitConstructionComparator()
	{
		return new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				final boolean construction1 = Matches.UnitIsConstruction.match(u1);
				final boolean construction2 = Matches.UnitIsConstruction.match(u2);
				if (construction1 == construction2)
					return 0;
				else if (construction1)
					return -1;
				else
					return 1;
			}
		};
	}
	
	protected Comparator<Unit> getHardestToPlaceWithRequiresUnitsRestrictions(final boolean sortConstructionsToFront)
	{
		return new Comparator<Unit>()
		{
			@SuppressWarnings("null")
			public int compare(final Unit u1, final Unit u2)
			{
				if (u1 == u2 || u1.equals(u2))
					return 0;
				final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
				final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
				if (ua1 == null && ua2 == null)
					return 0;
				if (ua1 != null && ua2 == null)
					return -1;
				if (ua1 == null && ua2 != null)
					return 1;
				// constructions go ahead first
				if (sortConstructionsToFront)
				{
					final int constructionSort = getUnitConstructionComparator().compare(u1, u2);
					if (constructionSort != 0)
						return constructionSort;
				}
				final ArrayList<String[]> ru1 = ua1.getRequiresUnits();
				final ArrayList<String[]> ru2 = ua2.getRequiresUnits();
				final int rus1 = (ru1 == null ? Integer.MAX_VALUE : (ru1.isEmpty() ? Integer.MAX_VALUE : ru1.size()));
				final int rus2 = (ru2 == null ? Integer.MAX_VALUE : (ru2.isEmpty() ? Integer.MAX_VALUE : ru2.size()));
				if (rus1 == rus2)
					return 0;
				// fewer means more difficult, and more difficult goes to front of list.
				if (rus1 < rus2)
					return -1;
				return 1;
			}
		};
	}
	
	/**
	 * @param to
	 *            referring territory
	 * @return collection of units that were there at start of turn
	 */
	public Collection<Unit> unitsAtStartOfStepInTerritory(final Territory to)
	{
		if (to == null)
			return new ArrayList<Unit>();
		final Collection<Unit> unitsInTO = to.getUnits().getUnits();
		final Collection<Unit> unitsPlacedAlready = getAlreadyProduced(to);
		if (Matches.TerritoryIsWater.match(to))
		{
			for (final Territory current : getAllProducers(to, m_player, null, true))
			{
				unitsPlacedAlready.addAll(getAlreadyProduced(current));
			}
		}
		final Collection<Unit> unitsAtStartOfTurnInTO = new ArrayList<Unit>(unitsInTO);
		unitsAtStartOfTurnInTO.removeAll(unitsPlacedAlready);
		return unitsAtStartOfTurnInTO;
	}
	
	public Collection<Unit> unitsPlacedInTerritorySoFar(final Territory to)
	{
		if (to == null)
			return new ArrayList<Unit>();
		final Collection<Unit> unitsInTO = to.getUnits().getUnits();
		final Collection<Unit> unitsAtStartOfStep = unitsAtStartOfStepInTerritory(to);
		unitsInTO.removeAll(unitsAtStartOfStep);
		return unitsInTO;
	}
	
	/**
	 * @param to
	 *            referring territory
	 * @param player
	 *            PlayerID
	 * @return whether there was an owned unit capable of producing, in this territory at the start of this phase/step
	 */
	public boolean wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(final Territory to, final PlayerID player)
	{
		final Collection<Unit> unitsAtStartOfTurnInTO = unitsAtStartOfStepInTerritory(to);
		final CompositeMatchAnd<Unit> factoryMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsOwnedAndIsFactoryOrCanProduceUnits(player), Matches.unitIsBeingTransported().invert());
		if (to.isWater())
			factoryMatch.add(Matches.UnitIsLand.invert());
		else
			factoryMatch.add(Matches.UnitIsSea.invert());
		return Match.countMatches(unitsAtStartOfTurnInTO, factoryMatch) > 0;
	}
	
	/**
	 * There must be a factory in the territory or an illegal state exception
	 * will be thrown. return value may be null.
	 */
	protected PlayerID getOriginalFactoryOwner(final Territory territory)
	{
		final Collection<Unit> factoryUnits = territory.getUnits().getMatches(Matches.UnitCanProduceUnits);
		if (factoryUnits.size() == 0)
			throw new IllegalStateException("No factory in territory:" + territory);
		final Iterator<Unit> iter = factoryUnits.iterator();
		// final GameData data = getData();
		while (iter.hasNext())
		{
			final Unit factory2 = iter.next();
			if (m_player.equals(OriginalOwnerTracker.getOriginalOwner(factory2)))
				return OriginalOwnerTracker.getOriginalOwner(factory2);
		}
		final Unit factory = factoryUnits.iterator().next();
		// return DelegateFinder.battleDelegate(data).getOriginalOwnerTracker().getOriginalOwner(factory);
		return OriginalOwnerTracker.getOriginalOwner(factory);
	}
	
	/**
	 * The rule is that new fighters can be produced on new carriers. This does
	 * not allow for fighters to be produced on old carriers.
	 */
	protected String validateNewAirCanLandOnCarriers(final Territory to, final Collection<Unit> units)
	{
		final int cost = AirMovementValidator.carrierCost(units);
		int capacity = AirMovementValidator.carrierCapacity(units, to);
		capacity += AirMovementValidator.carrierCapacity(to.getUnits().getUnits(), to);
		if (cost > capacity)
			return "Not enough new carriers to land all the fighters";
		return null;
	}
	
	/**
	 * Get what air units must move before the end of the players turn
	 * 
	 * @return a list of Territories with air units that must move
	 */
	public Collection<Territory> getTerritoriesWhereAirCantLand()
	{
		return new AirThatCantLandUtil(m_bridge).getTerritoriesWhereAirCantLand(m_player);
	}
	
	protected boolean canProduceFightersOnCarriers()
	{
		return games.strategy.triplea.Properties.getProduceFightersOnCarriers(getData());
	}
	
	protected boolean canProduceNewFightersOnOldCarriers()
	{
		return games.strategy.triplea.Properties.getProduceNewFightersOnOldCarriers(getData());
	}
	
	protected boolean canMoveExistingFightersToNewCarriers()
	{
		return games.strategy.triplea.Properties.getMoveExistingFightersToNewCarriers(getData());
	}
	
	protected boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(getData());
	}
	
	protected boolean isUnitPlacementInEnemySeas()
	{
		return games.strategy.triplea.Properties.getUnitPlacementInEnemySeas(getData());
	}
	
	protected boolean wasConquered(final Territory t)
	{
		final BattleTracker tracker = DelegateFinder.battleDelegate(getData()).getBattleTracker();
		return tracker.wasConquered(t);
	}
	
	protected boolean isPlaceInAnyTerritory()
	{
		return games.strategy.triplea.Properties.getPlaceInAnyTerritory(getData());
	}
	
	protected boolean isUnitPlacementPerTerritoryRestricted()
	{
		return games.strategy.triplea.Properties.getUnitPlacementPerTerritoryRestricted(getData());
	}
	
	protected boolean isUnitPlacementRestrictions()
	{
		return games.strategy.triplea.Properties.getUnitPlacementRestrictions(getData());
	}
	
	protected List<Territory> getAllProducers(final Territory to, final PlayerID player, final Collection<Unit> unitsToPlace)
	{
		return getAllProducers(to, player, unitsToPlace, false);
	}
	
	protected boolean isPlayerAllowedToPlacementAnyTerritoryOwnedLand(final PlayerID player)
	{
		if (isPlaceInAnyTerritory())
		{
			final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
			if (ra != null && ra.getPlacementAnyTerritory())
			{
				return true;
			}
		}
		return false;
	}
	
	protected boolean isPlayerAllowedToPlacementAnySeaZoneByOwnedLand(final PlayerID player)
	{
		if (isPlaceInAnyTerritory())
		{
			final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
			if (ra != null && ra.getPlacementAnySeaZone())
			{
				return true;
			}
		}
		return false;
	}
	
	protected boolean isPlacementAllowedInCapturedTerritory(final PlayerID player)
	{
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (ra != null && ra.getPlacementCapturedTerritory())
		{
			return true;
		}
		return false;
	}
	
	protected boolean isPlacementInCapitalRestricted(final PlayerID player)
	{
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (ra != null && ra.getPlacementInCapitalRestricted())
		{
			return true;
		}
		return false;
	}
	
	protected Collection<Territory> getListedTerritories(final String[] list)
	{
		final List<Territory> rVal = new ArrayList<Territory>();
		if (list == null)
			return rVal;
		for (final String name : list)
		{
			// Validate all territories exist
			final Territory territory = getData().getMap().getTerritory(name);
			if (territory == null)
				throw new IllegalStateException("Rules & Conditions: No territory called:" + name);
			rVal.add(territory);
		}
		return rVal;
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IAbstractPlaceDelegate.class;
	}
	
	/*
	protected boolean isSBRAffectsUnitProduction()
	{
	    return games.strategy.triplea.Properties.getSBRAffectsUnitProduction(m_data);
	}
	protected boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories()
	{
	    return games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data);
	}
	protected boolean isPlacementRestrictedByFactory()
	{
	    return games.strategy.triplea.Properties.getPlacementRestrictedByFactory(m_data);
	}
	protected boolean isIncreasedFactoryProduction(PlayerID player)
	{
	    TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
	    if(ta == null)
	        return false;
	    return ta.hasIncreasedFactoryProduction();
	}
	protected boolean hasConstruction(Territory to)
	{
	    return to.getUnits().someMatch(Matches.UnitIsConstruction);
	}
	protected boolean isWW2V3()
	{
		return games.strategy.triplea.Properties.getWW2V3(getData());
	}
	protected boolean isMultipleAAPerTerritory()
	{
		return games.strategy.triplea.Properties.getMultipleAAPerTerritory(getData());
	}
	protected boolean isOriginalOwner(final Territory t, final PlayerID id)
	{
		final OriginalOwnerTracker tracker = DelegateFinder.battleDelegate(getData()).getOriginalOwnerTracker();
		return tracker.getOriginalOwner(t).equals(id);
	}
	*/
}


class PlaceExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -4926754941623641735L;
	Serializable superState;
	// add other variables here:
	public Map<Territory, Collection<Unit>> m_produced;
	public List<UndoablePlacement> m_placements;
}
