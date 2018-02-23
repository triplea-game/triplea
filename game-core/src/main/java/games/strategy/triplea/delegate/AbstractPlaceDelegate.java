package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.message.IRemote;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

/**
 * Logic for placing units.
 *
 * <p>
 * Known limitations.
 * Doesn't take into account limits on number of factories that can be produced.
 * Solved (by frigoref):
 * The situation where one has two non original factories a,b each with
 * production 2. If sea zone e neighbors a,b and sea zone f neighbors b. Then
 * producing 2 in e was making it such that you cannot produce in f. The reason
 * was that the production in e could be assigned to the factory in b, leaving no
 * capacity to produce in f.
 * A workaround was that if anyone ever accidently run into this situation
 * then they could undo the production, produce in f first, and then produce in e.
 * </p>
 */
public abstract class AbstractPlaceDelegate extends BaseTripleADelegate implements IAbstractPlaceDelegate {
  // maps Territory-> Collection of units
  protected Map<Territory, Collection<Unit>> produced = new HashMap<>();
  // a list of CompositeChanges
  protected List<UndoablePlacement> placements = new ArrayList<>();

  public void initialize(final String name) {
    initialize(name, name);
  }

  @Override
  public void start() {
    super.start();
  }

  @Override
  public void end() {
    super.end();
    doAfterEnd();
  }

  protected void doAfterEnd() {
    final PlayerID player = bridge.getPlayerId();
    // clear all units not placed
    final Collection<Unit> units = player.getUnits().getUnits();
    final GameData data = getData();
    if (!Properties.getUnplacedUnitsLive(data) && !units.isEmpty()) {
      bridge.getHistoryWriter()
          .startEvent(MyFormatter.unitsToTextNoOwner(units) + " were produced but were not placed", units);
      final Change change = ChangeFactory.removeUnits(player, units);
      bridge.addChange(change);
    }
    // reset ourselves for next turn
    produced = new HashMap<>();
    placements.clear();
    if (GameStepPropertiesHelper.isRemoveAirThatCanNotLand(data)) {
      removeAirThatCantLand();
    }
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    // nothing to place
    return !((player == null) || ((player.getUnits().size() == 0) && (getPlacementsMade() == 0)));
  }

  protected void removeAirThatCantLand() {
    // for LHTR type games
    final GameData data = getData();
    final AirThatCantLandUtil util = new AirThatCantLandUtil(bridge);
    util.removeAirThatCantLand(player, false);
    // if edit mode has been on, we need to clean up after all players
    for (final PlayerID player : data.getPlayerList()) {
      if (!player.equals(this.player)) {
        util.removeAirThatCantLand(player, false);
      }
    }
  }

  @Override
  public Serializable saveState() {
    final PlaceExtendedDelegateState state = new PlaceExtendedDelegateState();
    state.superState = super.saveState();
    state.m_produced = produced;
    state.m_placements = placements;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final PlaceExtendedDelegateState s = (PlaceExtendedDelegateState) state;
    super.loadState(s.superState);
    produced = s.m_produced;
    placements = s.m_placements;
  }

  /**
   * @param t territory of interest.
   * @return a COPY of the collection of units that are produced at territory t
   */
  protected Collection<Unit> getAlreadyProduced(final Territory t) {
    // this list might be modified later
    final Collection<Unit> alreadyProducedUnits = new ArrayList<>();
    if (produced.containsKey(t)) {
      alreadyProducedUnits.addAll(produced.get(t));
    }
    return alreadyProducedUnits;
  }

  @Override
  public int getPlacementsMade() {
    return placements.size();
  }

  void setProduced(final Map<Territory, Collection<Unit>> produced) {
    this.produced = produced;
  }

  /**
   * @return The actual produced variable, allowing direct editing of the variable.
   */
  protected final Map<Territory, Collection<Unit>> getProduced() {
    return produced;
  }

  @Override
  public List<UndoablePlacement> getMovesMade() {
    return placements;
  }

  @Override
  public String undoMove(final int moveIndex) {
    if ((moveIndex < placements.size()) && (moveIndex >= 0)) {
      final UndoablePlacement undoPlace = placements.get(moveIndex);
      undoPlace.undo(bridge);
      placements.remove(moveIndex);
      updateUndoablePlacementIndexes();
    }
    return null;
  }

  private void updateUndoablePlacementIndexes() {
    for (int i = 0; i < placements.size(); i++) {
      placements.get(i).setIndex(i);
    }
  }

  @Override
  public PlaceableUnits getPlaceableUnits(final Collection<Unit> units, final Territory to) {
    final String error = canProduce(to, units, player);
    if (error != null) {
      return new PlaceableUnits(error);
    }
    final Collection<Unit> placeableUnits = getUnitsToBePlaced(to, units, player);
    final int maxUnits = getMaxUnitsToBePlaced(placeableUnits, to, player, true);
    return new PlaceableUnits(placeableUnits, maxUnits);
  }

  @Override
  public String placeUnits(final Collection<Unit> units, final Territory at, final BidMode bidMode) {
    if ((units == null) || units.isEmpty()) {
      return null;
    }
    final String error = isValidPlacement(units, at, player);
    if (error != null) {
      return error;
    }
    final List<Territory> producers = getAllProducers(at, player, units);
    producers.sort(getBestProducerComparator(at, units, player));
    final IntegerMap<Territory> maxPlaceableMap = getMaxUnitsToBePlacedMap(units, at, player, true);

    // sort both producers and units so that the "to/at" territory comes first, and so that all constructions come first
    // this is because the PRODUCER for ALL CONSTRUCTIONS must be the SAME as the TERRITORY they are going into
    final List<Unit> unitsLeftToPlace = new ArrayList<>(units);
    unitsLeftToPlace.sort(getUnitConstructionComparator());

    final List<Unit> remainingUnitsToPlace = new ArrayList<>(player.getUnits().getUnits());
    remainingUnitsToPlace.removeAll(unitsLeftToPlace);
    while (!unitsLeftToPlace.isEmpty() && !producers.isEmpty()) {
      // Get next producer territory
      final Territory producer = producers.remove(0);

      int maxPlaceable = maxPlaceableMap.getInt(producer);
      if (maxPlaceable == 0) {
        if (bidMode == BidMode.NOT_BID) {
          continue;
        }
        maxPlaceable = 1;
      }

      // units may have special restrictions like RequiresUnits
      final List<Unit> unitsCanBePlacedByThisProducer;
      if (bidMode == BidMode.BID) {
        unitsCanBePlacedByThisProducer = new ArrayList<>(unitsLeftToPlace);
      } else {
        unitsCanBePlacedByThisProducer = (isUnitPlacementRestrictions()
            ? CollectionUtils.getMatches(unitsLeftToPlace, unitWhichRequiresUnitsHasRequiredUnits(producer, true))
            : new ArrayList<>(unitsLeftToPlace));
      }

      unitsCanBePlacedByThisProducer.sort(getHardestToPlaceWithRequiresUnitsRestrictions(true));
      final int maxForThisProducer = getMaxUnitsToBePlacedFrom(producer, unitsCanBePlacedByThisProducer, at, player);
      // don't forget that -1 == infinite
      if ((maxForThisProducer == -1) || (maxForThisProducer >= unitsCanBePlacedByThisProducer.size())) {
        performPlaceFrom(producer, unitsCanBePlacedByThisProducer, at, player);
        unitsLeftToPlace.removeAll(unitsCanBePlacedByThisProducer);
        continue;
      }
      final int neededExtra = unitsCanBePlacedByThisProducer.size() - maxForThisProducer;
      if (maxPlaceable > maxForThisProducer) {
        freePlacementCapacity(producer, neededExtra, unitsCanBePlacedByThisProducer, at, player);
        final int newMaxForThisProducer =
            getMaxUnitsToBePlacedFrom(producer, unitsCanBePlacedByThisProducer, at, player);
        if ((newMaxForThisProducer != maxPlaceable) && (neededExtra > newMaxForThisProducer)) {
          throw new IllegalStateException("getMaxUnitsToBePlaced originally returned: " + maxPlaceable
              + ", \nWhich is not the same as it is returning after using freePlacementCapacity: "
              + newMaxForThisProducer + ", \nFor territory: " + at.getName() + ", Current Producer: "
              + producer.getName() + ", All Producers: " + producers + ", \nUnits Total: "
              + MyFormatter.unitsToTextNoOwner(units) + ", Units Left To Place By This Producer: "
              + MyFormatter.unitsToTextNoOwner(unitsCanBePlacedByThisProducer));
        }
      }
      final Collection<Unit> placedUnits =
          CollectionUtils.getNMatches(unitsCanBePlacedByThisProducer, maxPlaceable, Matches.always());
      performPlaceFrom(producer, placedUnits, at, player);
      unitsLeftToPlace.removeAll(placedUnits);
    }

    if (!unitsLeftToPlace.isEmpty()) {
      getDisplay().reportMessageToPlayers(
          Collections.singletonList(player),
          Collections.emptyList(),
          "Not enough unit production territories available",
          "Unit Placement Canceled");
    }

    // play a sound
    if (units.stream().anyMatch(Matches.unitIsInfrastructure())) {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_PLACED_INFRASTRUCTURE, player);
    } else if (units.stream().anyMatch(Matches.unitIsSea())) {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_PLACED_SEA, player);
    } else if (units.stream().anyMatch(Matches.unitIsAir())) {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_PLACED_AIR, player);
    } else {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_PLACED_LAND, player);
    }
    return null;
  }

  /**
   * @param producer
   *        territory that produces the new units.
   * @param placeableUnits
   *        the new units
   * @param at
   *        territory where the new units get placed
   */
  protected void performPlaceFrom(final Territory producer, final Collection<Unit> placeableUnits, final Territory at,
      final PlayerID player) {
    final CompositeChange change = new CompositeChange();
    // make sure we can place consuming units
    final boolean didIt = canWeConsumeUnits(placeableUnits, at, true, change);
    if (!didIt) {
      throw new IllegalStateException("Something wrong with consuming/upgrading units");
    }
    final Collection<Unit> factoryAndInfrastructure =
        CollectionUtils.getMatches(placeableUnits, Matches.unitIsInfrastructure());
    if (!factoryAndInfrastructure.isEmpty()) {
      change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, player));
    }
    // can we move planes to land there
    final String movedAirTranscriptTextForHistory =
        moveAirOntoNewCarriers(at, producer, placeableUnits, player, change);
    final Change remove = ChangeFactory.removeUnits(player, placeableUnits);
    final Change place = ChangeFactory.addUnits(at, placeableUnits);
    change.add(remove);
    change.add(place);
    final UndoablePlacement currentPlacement = new UndoablePlacement(change, producer, at, placeableUnits);
    placements.add(currentPlacement);
    updateUndoablePlacementIndexes();
    final String transcriptText = MyFormatter.unitsToTextNoOwner(placeableUnits) + " placed in " + at.getName();
    bridge.getHistoryWriter().startEvent(transcriptText, currentPlacement.getDescriptionObject());
    if (movedAirTranscriptTextForHistory != null) {
      bridge.getHistoryWriter().addChildToEvent(movedAirTranscriptTextForHistory);
    }
    bridge.addChange(change);
    updateProducedMap(producer, placeableUnits);
  }

  protected void updateProducedMap(final Territory producer, final Collection<Unit> additionallyProducedUnits) {
    final Collection<Unit> newProducedUnits = getAlreadyProduced(producer);
    newProducedUnits.addAll(additionallyProducedUnits);
    produced.put(producer, newProducedUnits);
  }

  protected void removeFromProducedMap(final Territory producer, final Collection<Unit> unitsToRemove) {
    final Collection<Unit> newProducedUnits = getAlreadyProduced(producer);
    newProducedUnits.removeAll(unitsToRemove);
    if (newProducedUnits.isEmpty()) {
      produced.remove(producer);
    } else {
      produced.put(producer, newProducedUnits);
    }
  }

  /**
   * frees the requested amount of capacity for the given producer by trying to hand over already made placements to
   * other territories.
   * This only works if one of the placements is done for another territory, more specific for a sea zone.
   * If such placements exists it will be tried to let them be done by other adjacent territories.
   *
   * @param producer
   *        territory that needs more placement capacity
   * @param freeSize
   *        amount of capacity that is requested
   */
  protected void freePlacementCapacity(final Territory producer, final int freeSize,
      final Collection<Unit> unitsLeftToPlace, final Territory at, final PlayerID player) {
    // placements of the producer that could be redone by other territories
    final List<UndoablePlacement> redoPlacements = new ArrayList<>();
    // territories the producer produced for (but not itself) and the amount of units it produced
    final HashMap<Territory, Integer> redoPlacementsCount = new HashMap<>();
    // find map place territory -> possible free space for producer
    for (final UndoablePlacement placement : placements) {
      // find placement move of producer that can be taken over
      if (placement.getProducerTerritory().equals(producer)) {
        final Territory placeTerritory = placement.getPlaceTerritory();
        // units with requiresUnits are too difficult to mess with logically, so do not move them around at all
        if (placeTerritory.isWater() && !placeTerritory.equals(producer) && (!isUnitPlacementRestrictions()
            || !placement.getUnits().stream().anyMatch(Matches.unitRequiresUnitsOnCreation()))) {
          // found placement move of producer that can be taken over
          // remember move and amount of placements in that territory
          redoPlacements.add(placement);
          final Integer integer = redoPlacementsCount.get(placeTerritory);
          if (integer == null) {
            redoPlacementsCount.put(placeTerritory, placement.getUnits().size());
          } else {
            redoPlacementsCount.put(placeTerritory, integer + placement.getUnits().size());
          }
        }
      }
    }
    // let other producers take over placements of producer
    // remember placement move and new territory if a placement has to be split up
    final Collection<Tuple<UndoablePlacement, Territory>> splitPlacements =
        new ArrayList<>();
    int foundSpaceTotal = 0;
    for (final Entry<Territory, Integer> entry : redoPlacementsCount.entrySet()) {
      final Territory placeTerritory = entry.getKey();
      final int maxProductionThatCanBeTakenOverFromThisPlacement = entry.getValue();
      // find other producers that could produce for the placeTerritory
      final List<Territory> potentialNewProducers = getAllProducers(placeTerritory, player, unitsLeftToPlace);
      potentialNewProducers.remove(producer);
      Collections.sort(potentialNewProducers, getBestProducerComparator(placeTerritory, unitsLeftToPlace, player));
      // we can just free a certain amount or still need a certain amount of space
      final int maxSpaceToBeFree =
          Math.min(maxProductionThatCanBeTakenOverFromThisPlacement, freeSize - foundSpaceTotal);
      // space that got free this on this placeTerritory
      int spaceAlreadyFree = 0;
      for (final Territory potentialNewProducerTerritory : potentialNewProducers) {
        int leftToPlace = getMaxUnitsToBePlacedFrom(potentialNewProducerTerritory,
            unitsPlacedInTerritorySoFar(placeTerritory), placeTerritory, player);
        if (leftToPlace == -1) {
          leftToPlace = maxProductionThatCanBeTakenOverFromThisPlacement;
        }
        // TODO: should we continue if leftToPlace is zero or less, now?
        // find placements of the producer the potentialNewProducerTerritory can take over
        for (final UndoablePlacement placement : redoPlacements) {
          if (!placement.getPlaceTerritory().equals(placeTerritory)) {
            continue;
          }
          final Collection<Unit> placedUnits = placement.getUnits();
          final int placementSize = placedUnits.size();
          // System.out.println("UndoPlacement: " + placement.getMoveLabel());
          if (placementSize <= leftToPlace) {
            // potentialNewProducerTerritory can take over complete production
            placement.setProducerTerritory(potentialNewProducerTerritory);
            removeFromProducedMap(producer, placedUnits);
            updateProducedMap(potentialNewProducerTerritory, placedUnits);
            spaceAlreadyFree += placementSize;
          } else {
            // potentialNewProducerTerritory can take over ONLY parts of the production
            // remember placement and potentialNewProducerTerritory but try to avoid splitting a placement
            splitPlacements.add(Tuple.of(placement, potentialNewProducerTerritory));
          }
          if (spaceAlreadyFree >= maxSpaceToBeFree) {
            break;
          }
        }
        if (spaceAlreadyFree >= maxSpaceToBeFree) {
          break;
        }
      }
      foundSpaceTotal += spaceAlreadyFree;
      if (foundSpaceTotal >= freeSize) {
        break;
      }
    }
    // we had a bug where we tried to split the same undoable placement twice (it can only be undone once!)
    boolean unusedSplitPlacments = false;
    if (foundSpaceTotal < freeSize) {
      // we need to split some placement moves
      final Collection<UndoablePlacement> usedUnoablePlacements = new ArrayList<>();
      for (final Tuple<UndoablePlacement, Territory> tuple : splitPlacements) {
        final UndoablePlacement placement = tuple.getFirst();
        if (usedUnoablePlacements.contains(placement)) {
          unusedSplitPlacments = true;
          continue;
        }
        final Territory newProducer = tuple.getSecond();
        int leftToPlace = getMaxUnitsToBePlacedFrom(newProducer, unitsLeftToPlace, at, player);
        foundSpaceTotal += leftToPlace;
        // divide set of units that get placed
        final Collection<Unit> unitsForOldProducer = new ArrayList<>(placement.getUnits());
        final Collection<Unit> unitsForNewProducer = new ArrayList<>();
        for (final Unit unit : unitsForOldProducer) {
          if (leftToPlace == 0) {
            break;
          }
          unitsForNewProducer.add(unit);
          --leftToPlace;
        }
        unitsForOldProducer.removeAll(unitsForNewProducer);
        // split move, by undo and creating two new ones
        if (!unitsForNewProducer.isEmpty()) {
          // there is a chance we have 2 or more splitPlacements that are using the same placement (trying to split the
          // same placement).
          // So we must make sure that after we undo it the first time, it can never be undone again.
          usedUnoablePlacements.add(placement);
          undoMove(placement.getIndex());
          performPlaceFrom(newProducer, unitsForNewProducer, placement.getPlaceTerritory(), player);
          performPlaceFrom(producer, unitsForOldProducer, placement.getPlaceTerritory(), player);
        }
      }
    }
    if ((foundSpaceTotal < freeSize) && unusedSplitPlacments) {
      freePlacementCapacity(producer, (freeSize - foundSpaceTotal), unitsLeftToPlace, at, player);
    }
  }

  // TODO Here's the spot for special air placement rules
  protected String moveAirOntoNewCarriers(final Territory at, final Territory producer, final Collection<Unit> units,
      final PlayerID player, final CompositeChange placeChange) {
    if (!at.isWater()) {
      return null;
    }
    if (!canMoveExistingFightersToNewCarriers() || AirThatCantLandUtil.isLhtrCarrierProduction(getData())) {
      return null;
    }
    if (units.stream().noneMatch(Matches.unitIsCarrier())) {
      return null;
    }
    // do we have any spare carrier capacity
    int capacity = AirMovementValidator.carrierCapacity(units, at);
    // subtract fighters that have already been produced with this carrier
    // this turn.
    capacity -= AirMovementValidator.carrierCost(units);
    if (capacity <= 0) {
      return null;
    }
    if (!Matches.territoryIsLand().test(producer)) {
      return null;
    }
    if (!producer.getUnits().anyMatch(Matches.unitCanProduceUnits())) {
      return null;
    }
    final Predicate<Unit> ownedFighters = Matches.unitCanLandOnCarrier().and(Matches.unitIsOwnedBy(player));
    if (!producer.getUnits().anyMatch(ownedFighters)) {
      return null;
    }
    if (wasConquered(producer)) {
      return null;
    }
    if (getAlreadyProduced(producer).stream().anyMatch(Matches.unitCanProduceUnits())) {
      return null;
    }
    final List<Unit> fighters = producer.getUnits().getMatches(ownedFighters);
    final Collection<Unit> movedFighters = getRemotePlayer().getNumberOfFightersToMoveToNewCarrier(fighters, producer);
    if ((movedFighters == null) || movedFighters.isEmpty()) {
      return null;
    }
    final Change change = ChangeFactory.moveUnits(producer, at, movedFighters);
    placeChange.add(change);
    final String transcriptText =
        MyFormatter.unitsToTextNoOwner(movedFighters) + " moved from " + producer.getName() + " to " + at.getName();
    return transcriptText;
  }

  /**
   * Subclasses can over ride this to change the way placements are made.
   *
   * @return null if placement is valid
   */
  protected String isValidPlacement(final Collection<Unit> units, final Territory at, final PlayerID player) {
    // do we hold enough units
    String error = playerHasEnoughUnits(units, player);
    if (error != null) {
      return error;
    }
    // can we produce that much
    error = canProduce(at, units, player);
    if (error != null) {
      return error;
    }
    // can we produce that much
    error = checkProduction(at, units, player);
    if (error != null) {
      return error;
    }
    // can we place it
    error = canUnitsBePlaced(at, units, player);
    if (error != null) {
      return error;
    }
    return null;
  }

  /**
   * Make sure the player has enough in hand to place the units.
   */
  private static String playerHasEnoughUnits(final Collection<Unit> units, final PlayerID player) {
    // make sure the player has enough units in hand to place
    if (!player.getUnits().getUnits().containsAll(units)) {
      return "Not enough units";
    }
    return null;
  }

  /**
   * Test whether or not the territory has the factory resources to support
   * the placement. AlreadyProduced maps territory->units already produced
   * this turn by that territory.
   */
  protected String canProduce(final Territory to, final Collection<Unit> units, final PlayerID player) {
    final Collection<Territory> producers = getAllProducers(to, player, units, true);
    // the only reason it could be empty is if its water and no
    // territories adjacent have factories
    if (producers.isEmpty()) {
      return "No factory in or adjacent to " + to.getName();
    }
    if (producers.size() == 1) {
      return canProduce(producers.iterator().next(), to, units, player);
    }
    final Collection<Territory> failingProducers = new ArrayList<>();
    final StringBuilder error = new StringBuilder();
    for (final Territory producer : producers) {
      final String errorP = canProduce(producer, to, units, player);
      if (errorP != null) {
        failingProducers.add(producer);
        // do not include the error for same territory, if water, because users do not want to see this error report for
        // 99.9% of games
        if (!(producer.equals(to) && producer.isWater())) {
          error.append(", ").append(errorP);
        }
      }
    }
    if (producers.size() == failingProducers.size()) {
      return "Adjacent territories to " + to.getName() + " cannot produce, due to: \n " + error.toString()
          .replaceFirst(", ", "");
    }
    return null;
  }

  protected String canProduce(final Territory producer, final Territory to, final Collection<Unit> units,
      final PlayerID player) {
    return canProduce(producer, to, units, player, false);
  }

  /**
   * Tests if this territory can produce units. (Does not check if it has space left to do so)
   *
   * @param producer
   *        - Territory doing the producing.
   * @param to
   *        - Territory to be placed in.
   * @param units
   *        - Units to be placed.
   * @param player
   *        - Player doing the placing.
   * @param simpleCheck
   *        - If true you return true even if a factory is not present. Used when you do not want an infinite loop
   *        (getAllProducers ->
   *        canProduce -> howManyOfEachConstructionCanPlace -> getAllProducers -> etc)
   * @return - null if allowed to produce, otherwise an error String.
   */
  protected String canProduce(final Territory producer, final Territory to, final Collection<Unit> units,
      final PlayerID player, final boolean simpleCheck) {
    // units can be null if we are just testing the territory itself...
    final Collection<Unit> testUnits = ((units == null) ? new ArrayList<>() : units);
    final boolean canProduceInConquered = isPlacementAllowedInCapturedTerritory(player);
    if (!producer.getOwner().equals(player)) {
      // sea constructions require either owning the sea zone or owning a surrounding land territory
      if (producer.isWater()
          && testUnits.stream().anyMatch(Matches.unitIsSea().and(Matches.unitIsConstruction()))) {
        boolean ownedNeighbor = false;
        for (final Territory current : getData().getMap().getNeighbors(to, Matches.territoryIsLand())) {
          if (current.getOwner().equals(player) && (canProduceInConquered || !wasConquered(current))) {
            ownedNeighbor = true;
            break;
          }
        }
        if (!ownedNeighbor) {
          return producer.getName() + " is not owned by you, and you have no owned neighbors which can produce";
        }
      } else {
        return producer.getName() + " is not owned by you";
      }
    }
    // make sure the territory wasnt conquered this turn
    if (!canProduceInConquered && wasConquered(producer)) {
      return producer.getName() + " was conquered this turn and cannot produce till next turn";
    }
    if (isPlayerAllowedToPlacementAnyTerritoryOwnedLand(player) && Matches.territoryIsLand().test(to)
        && Matches.isTerritoryOwnedBy(player).test(to)) {
      return null;
    }
    if (isPlayerAllowedToPlacementAnySeaZoneByOwnedLand(player) && Matches.territoryIsWater().test(to)
        && Matches.isTerritoryOwnedBy(player).test(producer)) {
      return null;
    }
    if (simpleCheck) {
      return null;
    }
    // make sure some unit has fullfilled requiresUnits requirements
    if (isUnitPlacementRestrictions() && !testUnits.isEmpty()
        && !testUnits.stream().anyMatch(unitWhichRequiresUnitsHasRequiredUnits(producer, true))) {
      return "You do not have the required units to build in " + producer.getName();
    }
    if (to.isWater() && (!isWW2V2() && !isUnitPlacementInEnemySeas())
        && to.getUnits().anyMatch(Matches.enemyUnit(player, getData()))) {
      return "Cannot place sea units with enemy naval units";
    }
    // make sure there is a factory
    if (wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(producer, player)) {
      return null;
    }
    // check to see if we are producing a factory or construction
    if (testUnits.stream().anyMatch(Matches.unitIsConstruction())) {
      if (howManyOfEachConstructionCanPlace(to, producer, testUnits, player).totalValues() > 0) {
        return null;
      }
      return "No more Constructions Allowed in " + producer.getName();
    }
    // check we havent just put a factory there (should we be checking producer?)
    if (getAlreadyProduced(producer).stream().anyMatch(Matches.unitCanProduceUnits())
        || getAlreadyProduced(to).stream().anyMatch(Matches.unitCanProduceUnits())) {
      return "Factory in " + producer.getName() + " cant produce until 1 turn after it is created";
    }
    return "No Factory in " + producer.getName();
  }

  /**
   * Returns the territories that would do the producing if units are to be placed in a given territory. Returns an
   * empty list if no
   * suitable territory could be found.
   *
   * @param to
   *        - Territory to place in.
   * @param player
   *        - player that is placing.
   * @param unitsToPlace
   *        - Can be null, otherwise is the units that will be produced.
   * @param simpleCheck
   *        - If true you return true even if a factory is not present. Used when you do not want an infinite loop
   *        (getAllProducers ->
   *        canProduce -> howManyOfEachConstructionCanPlace -> getAllProducers -> etc)
   * @return - List of territories that can produce here.
   */
  protected List<Territory> getAllProducers(final Territory to, final PlayerID player,
      final Collection<Unit> unitsToPlace, final boolean simpleCheck) {
    final List<Territory> producers = new ArrayList<>();
    // if not water then must produce in that territory
    if (!to.isWater()) {
      if (simpleCheck || (canProduce(to, to, unitsToPlace, player, simpleCheck) == null)) {
        producers.add(to);
      }
      return producers;
    }
    if (canProduce(to, to, unitsToPlace, player, simpleCheck) == null) {
      producers.add(to);
    }
    for (final Territory current : getData().getMap().getNeighbors(to, Matches.territoryIsLand())) {
      if (canProduce(current, to, unitsToPlace, player, simpleCheck) == null) {
        producers.add(current);
      }
    }
    return producers;
  }

  protected List<Territory> getAllProducers(final Territory to, final PlayerID player,
      final Collection<Unit> unitsToPlace) {
    return getAllProducers(to, player, unitsToPlace, false);
  }

  /**
   * Test whether or not the territory has the factory resources to support
   * the placement. AlreadyProduced maps territory->units already produced
   * this turn by that territory.
   */
  protected String checkProduction(final Territory to, final Collection<Unit> units, final PlayerID player) {
    final List<Territory> producers = getAllProducers(to, player, units);
    if (producers.isEmpty()) {
      return "No factory in or adjacent to " + to.getName();
    }
    // if its an original factory then unlimited production
    Collections.sort(producers, getBestProducerComparator(to, units, player));
    if (!getCanAllUnitsWithRequiresUnitsBePlacedCorrectly(units, to)) {
      return "Cannot place more units which require units, than production capacity of territories with the required "
          + "units";
    }
    final int maxUnitsToBePlaced = getMaxUnitsToBePlaced(units, to, player, true);
    if ((maxUnitsToBePlaced != -1) && (maxUnitsToBePlaced < units.size())) {
      return "Cannot place " + units.size() + " more units in " + to.getName();
    }
    return null;
  }

  public String canUnitsBePlaced(final Territory to, final Collection<Unit> units, final PlayerID player) {
    final Collection<Unit> allowedUnits = getUnitsToBePlaced(to, units, player);
    if ((allowedUnits == null) || !allowedUnits.containsAll(units)) {
      return "Cannot place these units in " + to.getName();
    }
    final IntegerMap<String> constructionMap = howManyOfEachConstructionCanPlace(to, to, units, player);
    for (final Unit currentUnit : CollectionUtils.getMatches(units, Matches.unitIsConstruction())) {
      final UnitAttachment ua = UnitAttachment.get(currentUnit.getType());
      /*
       * if (ua.getIsFactory() && !ua.getIsConstruction())
       * constructionMap.add("factory", -1);
       * else
       */
      constructionMap.add(ua.getConstructionType(), -1);
    }
    if (!constructionMap.isPositive()) {
      return "Too many constructions in " + to.getName();
    }
    final List<Territory> capitalsListOwned =
        new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, getData()));
    if (!capitalsListOwned.contains(to) && isPlacementInCapitalRestricted(player)) {
      return "Cannot place these units outside of the capital";
    }
    if (to.isWater()) {
      final String canLand = validateNewAirCanLandOnCarriers(to, units);
      if (canLand != null) {
        return canLand;
      }
    } else {
      // make sure we own the territory
      if (!to.getOwner().equals(player)) {
        if (GameStepPropertiesHelper.isBid(getData())) {
          final PlayerAttachment pa = PlayerAttachment.get(to.getOwner());
          if (((pa == null) || (pa.getGiveUnitControl() == null) || !pa.getGiveUnitControl().contains(player))
              && !to.getUnits().anyMatch(Matches.unitIsOwnedBy(player))) {
            return "You don't own " + to.getName();
          }
        } else {
          return "You don't own " + to.getName();
        }
      }
      // make sure all units are land
      if (units.isEmpty() || !units.stream().allMatch(Matches.unitIsNotSea())) {
        return "Cant place sea units on land";
      }
    }
    // make sure we can place consuming units
    if (!canWeConsumeUnits(units, to, false, null)) {
      return "Not Enough Units To Upgrade or Be Consumed";
    }
    // now check for stacking limits
    final Collection<UnitType> typesAlreadyChecked = new ArrayList<>();
    for (final Unit currentUnit : units) {
      final UnitType ut = currentUnit.getType();
      if (typesAlreadyChecked.contains(ut)) {
        continue;
      }
      typesAlreadyChecked.add(ut);
      final int maxForThisType = UnitAttachment.getMaximumNumberOfThisUnitTypeToReachStackingLimit("placementLimit", ut,
          to, player, getData());
      if (CollectionUtils.countMatches(units, Matches.unitIsOfType(ut)) > maxForThisType) {
        return "UnitType " + ut.getName() + " is over stacking limit of " + maxForThisType;
      }
    }
    if (!PlayerAttachment.getCanTheseUnitsMoveWithoutViolatingStackingLimit("placementLimit", units, to, player,
        getData())) {
      return "Units Cannot Go Over Stacking Limit";
    }
    // now return null (valid placement) if we have placement restrictions disabled in game options
    if (!isUnitPlacementRestrictions()) {
      return null;
    }
    // account for any unit placement restrictions by territory
    for (final Unit currentUnit : units) {
      final UnitAttachment ua = UnitAttachment.get(currentUnit.getType());
      // Can be null!
      final TerritoryAttachment ta = TerritoryAttachment.get(to);
      if ((ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1)
          && (ua.getCanOnlyBePlacedInTerritoryValuedAtX() > ((ta == null) ? 0 : ta.getProduction()))) {
        return "Cannot place these units in " + to.getName() + " due to Unit Placement Restrictions on Territory Value";
      }
      final String[] terrs = ua.getUnitPlacementRestrictions();
      final Collection<Territory> listedTerrs = getListedTerritories(terrs);
      if (listedTerrs.contains(to)) {
        return "Cannot place these units in " + to.getName() + " due to Unit Placement Restrictions";
      }
      if (Matches.unitCanOnlyPlaceInOriginalTerritories().test(currentUnit)
          && !Matches.territoryIsOriginallyOwnedBy(player).test(to)) {
        return "Cannot place these units in " + to.getName() + " as territory is not originally owned";
      }
    }
    return null;
  }

  // Separate it out so we can Override it in sub classes.
  protected Collection<Unit> getUnitsToBePlaced(final Territory to, final Collection<Unit> units,
      final PlayerID player) {
    if (to.isWater()) {
      return getUnitsToBePlacedSea(to, units, player);
    }
    // if land
    return getUnitsToBePlacedLand(to, units, player);
  }

  protected Collection<Unit> getUnitsToBePlacedSea(final Territory to, final Collection<Unit> units,
      final PlayerID player) {
    return getUnitsToBePlacedAllDefault(to, units, player);
  }

  protected Collection<Unit> getUnitsToBePlacedLand(final Territory to, final Collection<Unit> units,
      final PlayerID player) {
    return getUnitsToBePlacedAllDefault(to, units, player);
  }

  protected Collection<Unit> getUnitsToBePlacedAllDefault(final Territory to, final Collection<Unit> allUnits,
      final PlayerID player) {
    final boolean water = to.isWater();
    if (water && (!isWW2V2() && !isUnitPlacementInEnemySeas())
        && to.getUnits().anyMatch(Matches.enemyUnit(player, getData()))) {
      return null;
    }
    final Collection<Unit> units = new ArrayList<>(allUnits);
    // if water, remove land. if land, remove water.
    units.removeAll(CollectionUtils.getMatches(units, water ? Matches.unitIsLand() : Matches.unitIsSea()));
    final Collection<Unit> placeableUnits = new ArrayList<>();
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Collection<Unit> allProducedUnits = unitsPlacedInTerritorySoFar(to);
    final boolean isBid = GameStepPropertiesHelper.isBid(getData());
    final boolean wasFactoryThereAtStart =
        wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(to, player);

    // we add factories and constructions later
    if (water || wasFactoryThereAtStart || (!water && isPlayerAllowedToPlacementAnyTerritoryOwnedLand(player))) {
      final Predicate<Unit> seaOrLandMatch = water ? Matches.unitIsSea() : Matches.unitIsLand();
      placeableUnits.addAll(CollectionUtils.getMatches(units, seaOrLandMatch.and(Matches.unitIsNotConstruction())));
      if (!water) {
        placeableUnits
            .addAll(CollectionUtils.getMatches(units, Matches.unitIsAir().and(Matches.unitIsNotConstruction())));
      } else if (((isBid || canProduceFightersOnCarriers() || AirThatCantLandUtil.isLhtrCarrierProduction(getData()))
          && allProducedUnits.stream().anyMatch(Matches.unitIsCarrier()))
          || ((isBid || canProduceNewFightersOnOldCarriers() || AirThatCantLandUtil.isLhtrCarrierProduction(getData()))
              && to.getUnits().anyMatch(Matches.unitIsCarrier()))) {
        placeableUnits
            .addAll(CollectionUtils.getMatches(units, Matches.unitIsAir().and(Matches.unitCanLandOnCarrier())));
      }
    }
    if (units.stream().anyMatch(Matches.unitIsConstruction())) {
      final IntegerMap<String> constructionsMap = howManyOfEachConstructionCanPlace(to, to, units, player);
      final Collection<Unit> skipUnits = new ArrayList<>();
      for (final Unit currentUnit : CollectionUtils.getMatches(units, Matches.unitIsConstruction())) {
        final int maxUnits = howManyOfConstructionUnit(currentUnit, constructionsMap);
        if (maxUnits > 0) {
          // we are doing this because we could have multiple unitTypes with the same constructionType, so we have to be
          // able to place the
          // max placement by constructionType of each unitType
          if (skipUnits.contains(currentUnit)) {
            continue;
          }
          placeableUnits
              .addAll(CollectionUtils.getNMatches(units, maxUnits, Matches.unitIsOfType(currentUnit.getType())));
          skipUnits.addAll(CollectionUtils.getMatches(units, Matches.unitIsOfType(currentUnit.getType())));
        }
      }
    }
    // remove any units that require other units to be consumed on creation, if we don't have enough to consume (veqryn)
    if (placeableUnits.stream().anyMatch(Matches.unitConsumesUnitsOnCreation())) {
      final Collection<Unit> unitsWhichConsume =
          CollectionUtils.getMatches(placeableUnits, Matches.unitConsumesUnitsOnCreation());
      for (final Unit unit : unitsWhichConsume) {
        if (Matches.unitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTo).negate().test(unit)) {
          placeableUnits.remove(unit);
        }
      }
    }
    // now check stacking limits
    final Collection<Unit> placeableUnits2 = new ArrayList<>();
    final Collection<UnitType> typesAlreadyChecked = new ArrayList<>();
    for (final Unit currentUnit : placeableUnits) {
      final UnitType ut = currentUnit.getType();
      if (typesAlreadyChecked.contains(ut)) {
        continue;
      }
      typesAlreadyChecked.add(ut);
      placeableUnits2.addAll(
          CollectionUtils.getNMatches(placeableUnits, UnitAttachment.getMaximumNumberOfThisUnitTypeToReachStackingLimit(
              "placementLimit", ut, to, player, getData()), Matches.unitIsOfType(ut)));
    }
    if (!isUnitPlacementRestrictions()) {
      return placeableUnits2;
    }
    final Collection<Unit> placeableUnits3 = new ArrayList<>();
    for (final Unit currentUnit : placeableUnits2) {
      final UnitAttachment ua = UnitAttachment.get(currentUnit.getType());
      // Can be null!
      final TerritoryAttachment ta = TerritoryAttachment.get(to);
      if ((ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1)
          && (ua.getCanOnlyBePlacedInTerritoryValuedAtX() > ((ta == null) ? 0 : ta.getProduction()))) {
        continue;
      }
      if (unitWhichRequiresUnitsHasRequiredUnits(to, false).negate().test(currentUnit)) {
        continue;
      }
      if (Matches.unitCanOnlyPlaceInOriginalTerritories().test(currentUnit)
          && !Matches.territoryIsOriginallyOwnedBy(player).test(to)) {
        continue;
      }
      // account for any unit placement restrictions by territory
      final String[] terrs = ua.getUnitPlacementRestrictions();
      final Collection<Territory> listedTerrs = getListedTerritories(terrs);
      if (!listedTerrs.contains(to)) {
        placeableUnits3.add(currentUnit);
      }
    }
    return placeableUnits3;
  }

  protected boolean canWeConsumeUnits(final Collection<Unit> units, final Territory to, final boolean actuallyDoIt,
      final CompositeChange change) {
    boolean weCanConsume = true;
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Collection<Unit> removedUnits = new ArrayList<>();
    final Collection<Unit> unitsWhichConsume = CollectionUtils.getMatches(units, Matches.unitConsumesUnitsOnCreation());
    for (final Unit unit : unitsWhichConsume) {
      if (Matches.unitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTo).negate().test(unit)) {
        weCanConsume = false;
      }
      if (!weCanConsume) {
        break;
      }
      // remove units which are now consumed, then test the rest of the consuming units on the diminishing pile of units
      // which were in the
      // territory at start of turn
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
      final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
      for (final UnitType ut : requiredUnits) {
        final int requiredNumber = requiredUnitsMap.getInt(ut);
        final Predicate<Unit> unitIsOwnedByAndOfTypeAndNotDamaged = Matches.unitIsOwnedBy(unit.getOwner())
            .and(Matches.unitIsOfType(ut))
            .and(Matches.unitHasNotTakenAnyBombingUnitDamage())
            .and(Matches.unitHasNotTakenAnyDamage())
            .and(Matches.unitIsNotDisabled());
        final Collection<Unit> unitsBeingRemoved =
            CollectionUtils.getNMatches(unitsAtStartOfTurnInTo, requiredNumber, unitIsOwnedByAndOfTypeAndNotDamaged);
        unitsAtStartOfTurnInTo.removeAll(unitsBeingRemoved);
        // if we should actually do it, not just test, then add to bridge
        if (actuallyDoIt && (change != null)) {
          final Change remove = ChangeFactory.removeUnits(to, unitsBeingRemoved);
          change.add(remove);
          removedUnits.addAll(unitsBeingRemoved);
        }
      }
    }
    if (weCanConsume && actuallyDoIt && (change != null) && !change.isEmpty()) {
      bridge.getHistoryWriter().startEvent(
          "Units in " + to.getName() + " being upgraded or consumed: " + MyFormatter.unitsToTextNoOwner(removedUnits),
          removedUnits);
    }
    return weCanConsume;
  }

  /**
   * Returns -1 if can place unlimited units.
   */
  protected int getMaxUnitsToBePlaced(final Collection<Unit> units, final Territory to, final PlayerID player,
      final boolean countSwitchedProductionToNeighbors) {
    final IntegerMap<Territory> map = getMaxUnitsToBePlacedMap(units, to, player, countSwitchedProductionToNeighbors);
    int production = 0;
    for (final Entry<Territory, Integer> entry : map.entrySet()) {
      final int prodT = entry.getValue();
      if (prodT == -1) {
        return -1;
      }
      production += prodT;
    }
    return production;
  }

  /**
   * Returns -1 somewhere in the map if can place unlimited units.
   */
  protected IntegerMap<Territory> getMaxUnitsToBePlacedMap(final Collection<Unit> units, final Territory to,
      final PlayerID player, final boolean countSwitchedProductionToNeighbors) {
    final IntegerMap<Territory> maxUnitsToBePlacedMap = new IntegerMap<>();
    final List<Territory> producers = getAllProducers(to, player, units);
    if (producers.isEmpty()) {
      return maxUnitsToBePlacedMap;
    }
    Collections.sort(producers, getBestProducerComparator(to, units, player));
    final Collection<Territory> notUsableAsOtherProducers = new ArrayList<>();
    notUsableAsOtherProducers.addAll(producers);
    final Map<Territory, Integer> currentAvailablePlacementForOtherProducers = new HashMap<>();
    for (final Territory producerTerritory : producers) {
      final Collection<Unit> unitsCanBePlacedByThisProducer = (isUnitPlacementRestrictions()
          ? CollectionUtils.getMatches(units, unitWhichRequiresUnitsHasRequiredUnits(producerTerritory, true))
          : new ArrayList<>(units));
      final int prodT = getMaxUnitsToBePlacedFrom(producerTerritory, unitsCanBePlacedByThisProducer, to, player,
          countSwitchedProductionToNeighbors, notUsableAsOtherProducers, currentAvailablePlacementForOtherProducers);
      maxUnitsToBePlacedMap.put(producerTerritory, prodT);
    }
    return maxUnitsToBePlacedMap;
  }

  /**
   * Returns -1 if can place unlimited units.
   */
  protected int getMaxUnitsToBePlacedFrom(final Territory producer, final Collection<Unit> units, final Territory to,
      final PlayerID player) {
    return getMaxUnitsToBePlacedFrom(producer, units, to, player, false, null, null);
  }

  /**
   * Returns -1 if can place unlimited units.
   */
  protected int getMaxUnitsToBePlacedFrom(final Territory producer, final Collection<Unit> units, final Territory to,
      final PlayerID player, final boolean countSwitchedProductionToNeighbors,
      final Collection<Territory> notUsableAsOtherProducers,
      final Map<Territory, Integer> currentAvailablePlacementForOtherProducers) {
    // we may have special units with requiresUnits restrictions
    final Collection<Unit> unitsCanBePlacedByThisProducer = (isUnitPlacementRestrictions()
        ? CollectionUtils.getMatches(units, unitWhichRequiresUnitsHasRequiredUnits(producer, true))
        : new ArrayList<>(units));
    if (unitsCanBePlacedByThisProducer.size() <= 0) {
      return 0;
    }
    // if its an original factory then unlimited production
    // Can be null!
    final TerritoryAttachment ta = TerritoryAttachment.get(producer);
    final Predicate<Unit> factoryMatch = Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
        .and(Matches.unitIsBeingTransported().negate())
        .and(producer.isWater()
            ? Matches.unitIsLand().negate()
            : Matches.unitIsSea().negate());
    final Collection<Unit> factoryUnits = producer.getUnits().getMatches(factoryMatch);
    // boolean placementRestrictedByFactory = isPlacementRestrictedByFactory();
    final boolean unitPlacementPerTerritoryRestricted = isUnitPlacementPerTerritoryRestricted();
    final boolean originalFactory = ((ta != null) && ta.getOriginalFactory());
    final boolean playerIsOriginalOwner =
        (factoryUnits.size() > 0) && this.player.equals(getOriginalFactoryOwner(producer));
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    final Collection<Unit> alreadProducedUnits = getAlreadyProduced(producer);
    final int unitCountAlreadyProduced = alreadProducedUnits.size();
    if (originalFactory && playerIsOriginalOwner) {
      if ((ra != null) && (ra.getMaxPlacePerTerritory() != -1)) {
        return Math.max(0, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced);
      }
      return -1;
    }
    // Restricts based on the STARTING number of units in a territory (otherwise it is infinite placement)
    if (unitPlacementPerTerritoryRestricted) {
      if ((ra != null) && (ra.getPlacementPerTerritory() > 0)) {
        final int allowedPlacement = ra.getPlacementPerTerritory();
        final int ownedUnitsInTerritory =
            CollectionUtils.countMatches(to.getUnits().getUnits(), Matches.unitIsOwnedBy(player));
        if (ownedUnitsInTerritory >= allowedPlacement) {
          return 0;
        }
        if (ra.getMaxPlacePerTerritory() == -1) {
          return -1;
        }
        return Math.max(0, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced);
      }
    }
    // a factory can produce the same number of units as the number of PUs the territory generates each turn (or not, if
    // it has canProduceXUnits)
    final int maxConstructions =
        howManyOfEachConstructionCanPlace(to, producer, unitsCanBePlacedByThisProducer, player).totalValues();
    final boolean wasFactoryThereAtStart =
        wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(producer, player);
    // If there's NO factory, allow placement of the factory
    if (!wasFactoryThereAtStart) {
      if ((ra != null) && (ra.getMaxPlacePerTerritory() > 0)) {
        return Math.max(0, Math.min(maxConstructions, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced));
      }
      return Math.max(0, maxConstructions);
    }
    // getHowMuchCanUnitProduce accounts for IncreasedFactoryProduction, but does not account for maxConstructions
    int production = TripleAUnit.getProductionPotentialOfTerritory(unitsAtStartOfStepInTerritory(producer), producer,
        player, getData(), true, true);
    // increase the production by the number of constructions allowed
    if (maxConstructions > 0) {
      production += maxConstructions;
    }
    // return 0 if less than 0
    if (production < 0) {
      return 0;
    }
    production += CollectionUtils.countMatches(alreadProducedUnits, Matches.unitIsConstruction());
    // Now we check if units we have already produced here could be produced by a different producer
    int unitCountHaveToAndHaveBeenBeProducedHere = unitCountAlreadyProduced;
    if (countSwitchedProductionToNeighbors && (unitCountAlreadyProduced > 0)) {
      if (notUsableAsOtherProducers == null) {
        throw new IllegalStateException(
            "notUsableAsOtherProducers cannot be null if countSwitchedProductionToNeighbors is true");
      }
      if (currentAvailablePlacementForOtherProducers == null) {
        throw new IllegalStateException(
            "currentAvailablePlacementForOtherProducers cannot be null if countSwitchedProductionToNeighbors is true");
      }
      int productionCanNotBeMoved = 0;
      int productionThatCanBeTakenOver = 0;
      // try to find a placement move (to an adjacent sea zone) that can be taken over by some other territory factory
      for (final UndoablePlacement placementMove : placements) {
        if (placementMove.getProducerTerritory().equals(producer)) {
          final Territory placeTerritory = placementMove.getPlaceTerritory();
          final Collection<Unit> unitsPlacedByCurrentPlacementMove = placementMove.getUnits();
          // TODO: Units which have the unit attachment property, requiresUnits, are too difficult to mess with
          // logically, so we ignore them
          // for our special 'move shit around' methods.
          if (!placeTerritory.isWater() || (isUnitPlacementRestrictions()
              && unitsPlacedByCurrentPlacementMove.stream().anyMatch(Matches.unitRequiresUnitsOnCreation()))) {
            productionCanNotBeMoved += unitsPlacedByCurrentPlacementMove.size();
          } else {
            final int maxProductionThatCanBeTakenOverFromThisPlacement = unitsPlacedByCurrentPlacementMove.size();
            // find other producers for this placement move to the same water territory
            final List<Territory> newPotentialOtherProducers =
                getAllProducers(placeTerritory, player, unitsCanBePlacedByThisProducer);
            newPotentialOtherProducers.removeAll(notUsableAsOtherProducers);
            Collections.sort(newPotentialOtherProducers,
                getBestProducerComparator(placeTerritory, unitsCanBePlacedByThisProducer, player));
            int productionThatCanBeTakenOverFromThisPlacement = 0;
            for (final Territory potentialOtherProducer : newPotentialOtherProducers) {
              Integer potential = currentAvailablePlacementForOtherProducers.get(potentialOtherProducer);
              if (potential == null) {
                potential = getMaxUnitsToBePlacedFrom(potentialOtherProducer,
                    unitsPlacedInTerritorySoFar(placeTerritory), placeTerritory, player);
              }
              if (potential == -1) {
                currentAvailablePlacementForOtherProducers.put(potentialOtherProducer, -1);
                productionThatCanBeTakenOverFromThisPlacement = maxProductionThatCanBeTakenOverFromThisPlacement;
                break;
              }

              final int needed =
                  maxProductionThatCanBeTakenOverFromThisPlacement - productionThatCanBeTakenOverFromThisPlacement;
              final int surplus = potential - needed;
              if (surplus > 0) {
                currentAvailablePlacementForOtherProducers.put(potentialOtherProducer, surplus);
                productionThatCanBeTakenOverFromThisPlacement += needed;
              } else {
                currentAvailablePlacementForOtherProducers.put(potentialOtherProducer, 0);
                productionThatCanBeTakenOverFromThisPlacement += potential;
                notUsableAsOtherProducers.add(potentialOtherProducer);
              }
              if (surplus >= 0) {
                break;
              }
            }
            if (productionThatCanBeTakenOverFromThisPlacement > maxProductionThatCanBeTakenOverFromThisPlacement) {
              throw new IllegalStateException("productionThatCanBeTakenOverFromThisPlacement should never be larger "
                  + "than maxProductionThatCanBeTakenOverFromThisPlacement");
            }
            productionThatCanBeTakenOver += productionThatCanBeTakenOverFromThisPlacement;
          }
          if (productionThatCanBeTakenOver >= (unitCountAlreadyProduced - productionCanNotBeMoved)) {
            break;
          }
        }
      }
      unitCountHaveToAndHaveBeenBeProducedHere = Math.max(0, unitCountAlreadyProduced - productionThatCanBeTakenOver);
    }
    if ((ra != null) && (ra.getMaxPlacePerTerritory() > 0)) {
      return Math.max(0, Math.min(production - unitCountHaveToAndHaveBeenBeProducedHere,
          ra.getMaxPlacePerTerritory() - unitCountHaveToAndHaveBeenBeProducedHere));
    }
    return Math.max(0, production - unitCountHaveToAndHaveBeenBeProducedHere);
  }

  /**
   * @return gets the production of the territory.
   */
  protected int getProduction(final Territory territory) {
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    return (ta == null) ? 0 : ta.getProduction();
  }

  /**
   * @param to
   *        referring territory.
   * @param units
   *        units to place
   * @param player
   *        PlayerID
   * @return an empty IntegerMap if you can't produce any constructions (will never return null)
   */
  public IntegerMap<String> howManyOfEachConstructionCanPlace(final Territory to, final Territory producer,
      final Collection<Unit> units, final PlayerID player) {
    // constructions can ONLY be produced BY the same territory that they are going into!
    if (!to.equals(producer) || (units == null) || units.isEmpty()
        || !units.stream().anyMatch(Matches.unitIsConstruction())) {
      return new IntegerMap<>();
    }
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Collection<Unit> unitsInTo = to.getUnits().getUnits();
    final Collection<Unit> unitsPlacedAlready = getAlreadyProduced(to);
    // build an integer map of each unit we have in our list of held units, as well as integer maps for maximum units
    // and units per turn
    final IntegerMap<String> unitMapHeld = new IntegerMap<>();
    final IntegerMap<String> unitMapMaxType = new IntegerMap<>();
    final IntegerMap<String> unitMapTypePerTurn = new IntegerMap<>();
    final int maxFactory = Properties.getFactoriesPerCountry(getData());
    // Can be null!
    final TerritoryAttachment terrAttachment = TerritoryAttachment.get(to);
    int toProduction = 0;
    if (terrAttachment != null) {
      toProduction = terrAttachment.getProduction();
    }
    for (final Unit currentUnit : CollectionUtils.getMatches(units, Matches.unitIsConstruction())) {
      final UnitAttachment ua = UnitAttachment.get(currentUnit.getType());
      // account for any unit placement restrictions by territory
      if (isUnitPlacementRestrictions()) {
        final String[] terrs = ua.getUnitPlacementRestrictions();
        final Collection<Territory> listedTerrs = getListedTerritories(terrs);
        if (listedTerrs.contains(to)) {
          continue;
        }
        if ((ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1)
            && (ua.getCanOnlyBePlacedInTerritoryValuedAtX() > toProduction)) {
          continue;
        }
        if (unitWhichRequiresUnitsHasRequiredUnits(to, false).negate().test(currentUnit)) {
          continue;
        }
      }
      // remove any units that require other units to be consumed on creation (veqryn)
      if (Matches.unitConsumesUnitsOnCreation().test(currentUnit)
          && Matches.unitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTo).negate().test(currentUnit)) {
        continue;
      }
      unitMapHeld.add(ua.getConstructionType(), 1);
      unitMapTypePerTurn.put(ua.getConstructionType(), ua.getConstructionsPerTerrPerTypePerTurn());
      if (ua.getConstructionType().equals(Constants.CONSTRUCTION_TYPE_FACTORY)) {
        unitMapMaxType.put(ua.getConstructionType(), maxFactory);
      } else {
        unitMapMaxType.put(ua.getConstructionType(), ua.getMaxConstructionsPerTypePerTerr());
      }
    }
    final boolean moreWithoutFactory = Properties.getMoreConstructionsWithoutFactory(getData());
    final boolean moreWithFactory = Properties.getMoreConstructionsWithFactory(getData());
    final boolean unlimitedConstructions = Properties.getUnlimitedConstructions(getData());
    final boolean wasFactoryThereAtStart =
        wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(to, player);
    // build an integer map of each construction unit in the territory
    final IntegerMap<String> unitMapTo = new IntegerMap<>();
    if (unitsInTo.stream().anyMatch(Matches.unitIsConstruction())) {
      for (final Unit currentUnit : CollectionUtils.getMatches(unitsInTo, Matches.unitIsConstruction())) {
        final UnitAttachment ua = UnitAttachment.get(currentUnit.getType());
        /*
         * if (Matches.UnitIsFactory.test(currentUnit) && !ua.getIsConstruction())
         * unitMapTO.add("factory", 1);
         * else
         */
        unitMapTo.add(ua.getConstructionType(), 1);
      }
      // account for units already in the territory, based on max
      for (final String constructionType : unitMapHeld.keySet()) {
        int unitMax = unitMapMaxType.getInt(constructionType);
        if (wasFactoryThereAtStart && !constructionType.equals(Constants.CONSTRUCTION_TYPE_FACTORY)
            && !constructionType.endsWith("structure")) {
          unitMax =
              Math.max(Math.max(unitMax, (moreWithFactory ? toProduction : 0)), (unlimitedConstructions ? 10000 : 0));
        }
        if (!wasFactoryThereAtStart && !constructionType.equals(Constants.CONSTRUCTION_TYPE_FACTORY)
            && !constructionType.endsWith("structure")) {
          unitMax = Math.max(Math.max(unitMax, (moreWithoutFactory ? toProduction : 0)),
              (unlimitedConstructions ? 10000 : 0));
        }
        unitMapHeld.put(constructionType,
            Math.max(0, Math.min(unitMax - unitMapTo.getInt(constructionType), unitMapHeld.getInt(constructionType))));
      }
    }
    // deal with already placed units
    for (final Unit currentUnit : CollectionUtils.getMatches(unitsPlacedAlready, Matches.unitIsConstruction())) {
      final UnitAttachment ua = UnitAttachment.get(currentUnit.getType());
      unitMapTypePerTurn.add(ua.getConstructionType(), -1);
    }
    // modify this list based on how many we can place per turn
    final IntegerMap<String> unitsAllowed = new IntegerMap<>();
    for (final String constructionType : unitMapHeld.keySet()) {
      final int unitAllowed =
          Math.max(0, Math.min(unitMapTypePerTurn.getInt(constructionType), unitMapHeld.getInt(constructionType)));
      if (unitAllowed > 0) {
        unitsAllowed.put(constructionType, unitAllowed);
      }
    }
    // return our integer map
    return unitsAllowed;
  }

  int howManyOfConstructionUnit(final Unit unit, final IntegerMap<String> constructionsMap) {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (!ua.getIsConstruction() || (ua.getConstructionsPerTerrPerTypePerTurn() < 1)
        || (ua.getMaxConstructionsPerTypePerTerr() < 1)) {
      return 0;
    }
    return Math.max(0, constructionsMap.getInt(ua.getConstructionType()));
  }

  /**
   * @param to
   *        - Territory we are testing for required units
   * @param doNotCountNeighbors
   *        - If false, and 'to' is water, then we will test neighboring land territories to see if they have any of the
   *        required units as
   *        well.
   * @return - Whether the territory contains one of the required combos of units
   *         (and if 'doNotCountNeighbors' is false, and unit is Sea unit, will return true if an adjacent land
   *         territory has one of the
   *         required combos as well).
   */
  public Predicate<Unit> unitWhichRequiresUnitsHasRequiredUnits(final Territory to, final boolean doNotCountNeighbors) {
    return unitWhichRequiresUnits -> {
      if (!Matches.unitRequiresUnitsOnCreation().test(unitWhichRequiresUnits)) {
        return true;
      }
      final Collection<Unit> unitsAtStartOfTurnInProducer = unitsAtStartOfStepInTerritory(to);
      // do not need to remove unowned here, as this match will remove unowned units from consideration.
      if (Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInProducer)
          .test(unitWhichRequiresUnits)) {
        return true;
      }
      if (!doNotCountNeighbors) {
        if (Matches.unitIsSea().test(unitWhichRequiresUnits)) {
          for (final Territory current : getAllProducers(to, player,
              Collections.singletonList(unitWhichRequiresUnits), true)) {
            final Collection<Unit> unitsAtStartOfTurnInCurrent = unitsAtStartOfStepInTerritory(current);
            if (Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInCurrent)
                .test(unitWhichRequiresUnits)) {
              return true;
            }
          }
        }
      }
      return false;
    };
  }

  private boolean getCanAllUnitsWithRequiresUnitsBePlacedCorrectly(final Collection<Unit> units, final Territory to) {
    if (!isUnitPlacementRestrictions() || !units.stream().anyMatch(Matches.unitRequiresUnitsOnCreation())) {
      return true;
    }
    final IntegerMap<Territory> producersMap = getMaxUnitsToBePlacedMap(units, to, player, true);
    final List<Territory> producers = getAllProducers(to, player, units);
    if (producers.isEmpty()) {
      return false;
    }
    Collections.sort(producers, getBestProducerComparator(to, units, player));
    final Collection<Unit> unitsLeftToPlace = new ArrayList<>(units);
    for (final Territory t : producers) {
      if (unitsLeftToPlace.isEmpty()) {
        return true;
      }
      final int productionHere = producersMap.getInt(t);
      final List<Unit> canBePlacedHere =
          CollectionUtils.getMatches(unitsLeftToPlace, unitWhichRequiresUnitsHasRequiredUnits(t, true));
      if ((productionHere == -1) || (productionHere >= canBePlacedHere.size())) {
        unitsLeftToPlace.removeAll(canBePlacedHere);
        continue;
      }
      Collections.sort(canBePlacedHere, getHardestToPlaceWithRequiresUnitsRestrictions(true));
      final Collection<Unit> placedHere =
          CollectionUtils.getNMatches(canBePlacedHere, productionHere, Matches.always());
      unitsLeftToPlace.removeAll(placedHere);
    }
    return unitsLeftToPlace.isEmpty();
  }

  protected Comparator<Territory> getBestProducerComparator(final Territory to, final Collection<Unit> units,
      final PlayerID player) {
    return (t1, t2) -> {
      if (Objects.equals(t1, t2)) {
        return 0;
      }
      // producing to territory comes first
      if (Objects.equals(to, t1)) {
        return -1;
      } else if (Objects.equals(to, t2)) {
        return 1;
      }
      final int left1 = getMaxUnitsToBePlacedFrom(t1, units, to, player);
      final int left2 = getMaxUnitsToBePlacedFrom(t2, units, to, player);
      if (left1 == left2) {
        return 0;
      }
      // production of -1 == infinite
      if (left1 == -1) {
        return -1;
      }
      if (left2 == -1) {
        return 1;
      }
      if (left1 > left2) {
        return -1;
      }
      return 1;
    };
  }

  protected Comparator<Unit> getUnitConstructionComparator() {
    return (u1, u2) -> {
      final boolean construction1 = Matches.unitIsConstruction().test(u1);
      final boolean construction2 = Matches.unitIsConstruction().test(u2);
      if (construction1 == construction2) {
        return 0;
      } else if (construction1) {
        return -1;
      } else {
        return 1;
      }
    };
  }

  protected Comparator<Unit> getHardestToPlaceWithRequiresUnitsRestrictions(final boolean sortConstructionsToFront) {
    return (u1, u2) -> {
      if (Objects.equals(u1, u2)) {
        return 0;
      }
      final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
      final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
      if ((ua1 == null) && (ua2 == null)) {
        return 0;
      }
      if ((ua1 != null) && (ua2 == null)) {
        return -1;
      }
      if ((ua1 == null) && (ua2 != null)) {
        return 1;
      }
      // constructions go ahead first
      if (sortConstructionsToFront) {
        final int constructionSort = getUnitConstructionComparator().compare(u1, u2);
        if (constructionSort != 0) {
          return constructionSort;
        }
      }
      final List<String[]> ru1 = ua1.getRequiresUnits();
      final List<String[]> ru2 = ua2.getRequiresUnits();
      final int rus1 = ((ru1 == null) ? Integer.MAX_VALUE : (ru1.isEmpty() ? Integer.MAX_VALUE : ru1.size()));
      final int rus2 = ((ru2 == null) ? Integer.MAX_VALUE : (ru2.isEmpty() ? Integer.MAX_VALUE : ru2.size()));
      if (rus1 == rus2) {
        return 0;
      }
      // fewer means more difficult, and more difficult goes to front of list.
      if (rus1 < rus2) {
        return -1;
      }
      return 1;
    };
  }

  /**
   * @param to
   *        referring territory.
   * @return collection of units that were there at start of turn
   */
  public Collection<Unit> unitsAtStartOfStepInTerritory(final Territory to) {
    if (to == null) {
      return new ArrayList<>();
    }
    final Collection<Unit> unitsInTo = to.getUnits().getUnits();
    final Collection<Unit> unitsPlacedAlready = getAlreadyProduced(to);
    if (Matches.territoryIsWater().test(to)) {
      for (final Territory current : getAllProducers(to, player, null, true)) {
        unitsPlacedAlready.addAll(getAlreadyProduced(current));
      }
    }
    final Collection<Unit> unitsAtStartOfTurnInTo = new ArrayList<>(unitsInTo);
    unitsAtStartOfTurnInTo.removeAll(unitsPlacedAlready);
    return unitsAtStartOfTurnInTo;
  }

  private Collection<Unit> unitsPlacedInTerritorySoFar(final Territory to) {
    if (to == null) {
      return new ArrayList<>();
    }
    final Collection<Unit> unitsInTo = to.getUnits().getUnits();
    final Collection<Unit> unitsAtStartOfStep = unitsAtStartOfStepInTerritory(to);
    unitsInTo.removeAll(unitsAtStartOfStep);
    return unitsInTo;
  }

  /**
   * @param to referring territory.
   * @param player PlayerID
   * @return whether there was an owned unit capable of producing, in this territory at the start of this phase/step
   */
  public boolean wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(final Territory to,
      final PlayerID player) {
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Predicate<Unit> factoryMatch = Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
        .and(Matches.unitIsBeingTransported().negate())
        // land factories in water can't produce, and sea factories in land can't produce.
        // air can produce like land if in land, and like sea if in water.
        .and(to.isWater()
            ? Matches.unitIsLand().negate()
            : Matches.unitIsSea().negate());
    return CollectionUtils.countMatches(unitsAtStartOfTurnInTo, factoryMatch) > 0;
  }

  /**
   * There must be a factory in the territory or an illegal state exception
   * will be thrown. return value may be null.
   */
  protected PlayerID getOriginalFactoryOwner(final Territory territory) {
    final Collection<Unit> factoryUnits = territory.getUnits().getMatches(Matches.unitCanProduceUnits());
    if (factoryUnits.size() == 0) {
      throw new IllegalStateException("No factory in territory:" + territory);
    }
    for (final Unit factory : factoryUnits) {
      if (player.equals(OriginalOwnerTracker.getOriginalOwner(factory))) {
        return OriginalOwnerTracker.getOriginalOwner(factory);
      }
    }
    return OriginalOwnerTracker.getOriginalOwner(factoryUnits.iterator().next());
  }

  /**
   * The rule is that new fighters can be produced on new carriers. This does
   * not allow for fighters to be produced on old carriers. THIS ISN'T CORRECT.
   */
  protected String validateNewAirCanLandOnCarriers(final Territory to, final Collection<Unit> units) {
    final int cost = AirMovementValidator.carrierCost(units);
    int capacity = AirMovementValidator.carrierCapacity(units, to);
    capacity += AirMovementValidator.carrierCapacity(to.getUnits().getUnits(), to);
    // TODO: This method considers existing carriers but not existing air units
    if (cost > capacity) {
      return "Not enough new carriers to land all the fighters";
    }
    return null;
  }

  @Override
  public Collection<Territory> getTerritoriesWhereAirCantLand() {
    return new AirThatCantLandUtil(bridge).getTerritoriesWhereAirCantLand(player);
  }

  protected boolean canProduceFightersOnCarriers() {
    return Properties.getProduceFightersOnCarriers(getData());
  }

  protected boolean canProduceNewFightersOnOldCarriers() {
    return Properties.getProduceNewFightersOnOldCarriers(getData());
  }

  protected boolean canMoveExistingFightersToNewCarriers() {
    return Properties.getMoveExistingFightersToNewCarriers(getData());
  }

  protected boolean isWW2V2() {
    return Properties.getWW2V2(getData());
  }

  protected boolean isUnitPlacementInEnemySeas() {
    return Properties.getUnitPlacementInEnemySeas(getData());
  }

  protected boolean wasConquered(final Territory t) {
    final BattleTracker tracker = DelegateFinder.battleDelegate(getData()).getBattleTracker();
    return tracker.wasConquered(t);
  }

  protected boolean isPlaceInAnyTerritory() {
    return Properties.getPlaceInAnyTerritory(getData());
  }

  protected boolean isUnitPlacementPerTerritoryRestricted() {
    return Properties.getUnitPlacementPerTerritoryRestricted(getData());
  }

  protected boolean isUnitPlacementRestrictions() {
    return Properties.getUnitPlacementRestrictions(getData());
  }

  protected boolean isPlayerAllowedToPlacementAnyTerritoryOwnedLand(final PlayerID player) {
    if (isPlaceInAnyTerritory()) {
      final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
      return (ra != null) && ra.getPlacementAnyTerritory();
    }
    return false;
  }

  protected boolean isPlayerAllowedToPlacementAnySeaZoneByOwnedLand(final PlayerID player) {
    if (isPlaceInAnyTerritory()) {
      final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
      return (ra != null) && ra.getPlacementAnySeaZone();
    }
    return false;
  }

  protected boolean isPlacementAllowedInCapturedTerritory(final PlayerID player) {
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    return (ra != null) && ra.getPlacementCapturedTerritory();
  }

  protected boolean isPlacementInCapitalRestricted(final PlayerID player) {
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    return (ra != null) && ra.getPlacementInCapitalRestricted();
  }

  protected Collection<Territory> getListedTerritories(final String[] list) {
    final List<Territory> territories = new ArrayList<>();
    if (list == null) {
      return territories;
    }
    for (final String name : list) {
      // Validate all territories exist
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new IllegalStateException("Rules & Conditions: No territory called:" + name);
      }
      territories.add(territory);
    }
    return territories;
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return IAbstractPlaceDelegate.class;
  }
}


