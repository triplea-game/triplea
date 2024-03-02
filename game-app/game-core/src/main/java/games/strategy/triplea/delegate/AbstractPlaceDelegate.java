package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.move.validation.UnitStackingLimitFilter.PLACEMENT_LIMIT;
import static java.util.function.Predicate.not;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.move.validation.AirMovementValidator;
import games.strategy.triplea.delegate.move.validation.UnitStackingLimitFilter;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;
import org.triplea.util.Tuple;

/**
 * Logic for placing units.
 *
 * <p>Known limitations. Doesn't take into account limits on number of factories that can be
 * produced. Solved (by frigoref): The situation where one has two non-original factories a,b each
 * with production 2. If sea zone e neighbors a,b and sea zone f neighbors b. Then producing 2 in e
 * was making it such that you cannot produce in f. The reason was that the production in e could be
 * assigned to the factory in b, leaving no capacity to produce in f. A workaround was that if
 * anyone ever accidentally run into this situation then they could undo the production, produce in
 * first, and then produce in e.
 */
public abstract class AbstractPlaceDelegate extends BaseTripleADelegate
    implements IAbstractPlaceDelegate {
  // maps Territory-> Collection of units
  protected Map<Territory, Collection<Unit>> produced = new HashMap<>();
  // a list of CompositeChanges
  protected List<UndoablePlacement> placements = new ArrayList<>();

  public void initialize(final String name) {
    initialize(name, name);
  }

  @Override
  public void end() {
    super.end();
    doAfterEnd();
  }

  private void doAfterEnd() {
    final GamePlayer player = bridge.getGamePlayer();
    // clear all units not placed
    final Collection<Unit> units = player.getUnits();
    final GameData data = getData();
    if (!Properties.getUnplacedUnitsLive(data.getProperties()) && !units.isEmpty()) {
      bridge
          .getHistoryWriter()
          .startEvent(
              MyFormatter.unitsToTextNoOwner(units) + " were produced but were not placed", units);
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
    return !(player == null || (player.getUnitCollection().isEmpty() && getPlacementsMade() == 0));
  }

  protected void removeAirThatCantLand() {
    // for LHTR type games
    final GameState data = getData();
    final AirThatCantLandUtil util = new AirThatCantLandUtil(bridge);
    util.removeAirThatCantLand(player, false);
    // if edit mode has been on, we need to clean up after all players
    for (final GamePlayer player : data.getPlayerList()) {
      if (!player.equals(this.player)) {
        util.removeAirThatCantLand(player, false);
      }
    }
  }

  @Override
  public Serializable saveState() {
    final PlaceExtendedDelegateState state = new PlaceExtendedDelegateState();
    state.superState = super.saveState();
    state.produced = produced;
    state.placements = placements;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final PlaceExtendedDelegateState s = (PlaceExtendedDelegateState) state;
    super.loadState(s.superState);
    produced = s.produced;
    placements = s.placements;
  }

  /**
   * Returns a COPY of the collection of units that are produced at territory t.
   *
   * @param t territory of interest.
   */
  private Collection<Unit> getAlreadyProduced(final Territory t) {
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

  /** Returns the actual produced variable, allowing direct editing of the variable. */
  protected final Map<Territory, Collection<Unit>> getProduced() {
    return produced;
  }

  @Override
  public List<UndoablePlacement> getMovesMade() {
    return placements;
  }

  @Override
  public @Nullable String undoMove(final int moveIndex) {
    if (moveIndex < placements.size() && moveIndex >= 0) {
      final UndoablePlacement undoPlace = placements.get(moveIndex);
      undoPlace.undo(bridge);
      placements.remove(moveIndex);
      updateUndoablePlacementIndexes();
    }
    return null;
  }

  private void updateUndoablePlacementIndexes() {
    IntStream.range(0, placements.size()) //
        .forEach(i -> placements.get(i).setIndex(i));
  }

  @Override
  public PlaceableUnits getPlaceableUnits(final Collection<Unit> units, final Territory to) {
    final String error = canProduce(to, units, player);
    if (error != null) {
      return new PlaceableUnits(error);
    }
    final Collection<Unit> placeableUnits = getUnitsToBePlaced(to, units, player);
    final int maxUnits = getMaxUnitsToBePlaced(placeableUnits, to, player);
    return new PlaceableUnits(placeableUnits, maxUnits);
  }

  @Override
  public @Nullable String placeUnits(
      final Collection<Unit> units, final Territory at, final BidMode bidMode) {
    if (units.isEmpty()) {
      return null;
    }
    final String error = isValidPlacement(units, at, player);
    if (error != null) {
      return error;
    }
    final List<Territory> producers = getAllProducers(at, player, units);
    producers.sort(getBestProducerComparator(at, units, player));
    final IntegerMap<Territory> maxPlaceableMap = getMaxUnitsToBePlacedMap(units, at, player);

    // sort both producers and units so that the "to/at" territory comes first, and so that all
    // constructions come first
    // this is because the PRODUCER for ALL CONSTRUCTIONS must be the SAME as the TERRITORY they are
    // going into
    final List<Unit> unitsLeftToPlace = new ArrayList<>(units);
    unitsLeftToPlace.sort(getUnitConstructionComparator());

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
        unitsCanBePlacedByThisProducer =
            (Properties.getUnitPlacementRestrictions(getData().getProperties())
                ? CollectionUtils.getMatches(
                    unitsLeftToPlace, unitWhichRequiresUnitsHasRequiredUnits(producer, true))
                : new ArrayList<>(unitsLeftToPlace));
      }

      unitsCanBePlacedByThisProducer.sort(getHardestToPlaceWithRequiresUnitsRestrictions());
      final int maxForThisProducer =
          getMaxUnitsToBePlacedFrom(producer, unitsCanBePlacedByThisProducer, at, player);
      // don't forget that -1 == infinite
      if (maxForThisProducer == -1 || maxForThisProducer >= unitsCanBePlacedByThisProducer.size()) {
        performPlaceFrom(producer, unitsCanBePlacedByThisProducer, at, player);
        unitsLeftToPlace.removeAll(unitsCanBePlacedByThisProducer);
        continue;
      }
      final int neededExtra = unitsCanBePlacedByThisProducer.size() - maxForThisProducer;
      if (maxPlaceable > maxForThisProducer) {
        freePlacementCapacity(producer, neededExtra, unitsCanBePlacedByThisProducer, at, player);
        final int newMaxForThisProducer =
            getMaxUnitsToBePlacedFrom(producer, unitsCanBePlacedByThisProducer, at, player);
        if (newMaxForThisProducer != maxPlaceable && neededExtra > newMaxForThisProducer) {
          throw new IllegalStateException(
              "getMaxUnitsToBePlaced originally returned: "
                  + maxPlaceable
                  + ", \nWhich is not the same as it is returning after using "
                  + "freePlacementCapacity: "
                  + newMaxForThisProducer
                  + ", \nFor territory: "
                  + at.getName()
                  + ", Current Producer: "
                  + producer.getName()
                  + ", All Producers: "
                  + producers
                  + ", \nUnits Total: "
                  + MyFormatter.unitsToTextNoOwner(units)
                  + ", Units Left To Place By This Producer: "
                  + MyFormatter.unitsToTextNoOwner(unitsCanBePlacedByThisProducer));
        }
      }
      final Collection<Unit> placedUnits =
          CollectionUtils.getNMatches(unitsCanBePlacedByThisProducer, maxPlaceable, it -> true);
      performPlaceFrom(producer, placedUnits, at, player);
      unitsLeftToPlace.removeAll(placedUnits);
    }

    if (!unitsLeftToPlace.isEmpty()) {
      bridge
          .getDisplayChannelBroadcaster()
          .reportMessageToPlayers(
              List.of(player),
              List.of(),
              "Not enough unit production territories available",
              "Unit Placement Canceled");
    }

    // play a sound
    if (units.stream().anyMatch(Matches.unitIsInfrastructure())) {
      bridge
          .getSoundChannelBroadcaster()
          .playSoundForAll(SoundPath.CLIP_PLACED_INFRASTRUCTURE, player);
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
   * Places the specified units in the specified territory.
   *
   * @param producer territory that produces the new units.
   * @param placeableUnits the new units
   * @param at territory where the new units get placed
   */
  private void performPlaceFrom(
      final Territory producer,
      final Collection<Unit> placeableUnits,
      final Territory at,
      final GamePlayer player) {
    final CompositeChange change = new CompositeChange();
    // make sure we can place consuming units
    final boolean didIt = canWeConsumeUnits(placeableUnits, at, change);
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
    final UndoablePlacement currentPlacement =
        new UndoablePlacement(change, producer, at, placeableUnits);
    placements.add(currentPlacement);
    updateUndoablePlacementIndexes();
    final String transcriptText =
        MyFormatter.unitsToTextNoOwner(placeableUnits) + " placed in " + at.getName();
    bridge.getHistoryWriter().startEvent(transcriptText, currentPlacement.getDescriptionObject());
    if (movedAirTranscriptTextForHistory != null) {
      bridge.getHistoryWriter().addChildToEvent(movedAirTranscriptTextForHistory);
    }
    bridge.addChange(change);
    updateProducedMap(producer, placeableUnits);
  }

  private void updateProducedMap(
      final Territory producer, final Collection<Unit> additionallyProducedUnits) {
    final Collection<Unit> newProducedUnits = getAlreadyProduced(producer);
    newProducedUnits.addAll(additionallyProducedUnits);
    produced.put(producer, newProducedUnits);
  }

  private void removeFromProducedMap(
      final Territory producer, final Collection<Unit> unitsToRemove) {
    final Collection<Unit> newProducedUnits = getAlreadyProduced(producer);
    newProducedUnits.removeAll(unitsToRemove);
    if (newProducedUnits.isEmpty()) {
      produced.remove(producer);
    } else {
      produced.put(producer, newProducedUnits);
    }
  }

  /**
   * frees the requested amount of capacity for the given producer by trying to hand over already
   * made placements to other territories. This only works if one of the placements is done for
   * another territory, more specific for a sea zone. If such placements exist it will be tried to
   * let them be done by other adjacent territories.
   *
   * @param producer territory that needs more placement capacity
   * @param freeSize amount of capacity that is requested
   */
  private void freePlacementCapacity(
      final Territory producer,
      final int freeSize,
      final Collection<Unit> unitsLeftToPlace,
      final Territory at,
      final GamePlayer player) {
    // placements of the producer that could be redone by other territories
    final List<UndoablePlacement> redoPlacements = new ArrayList<>();
    // territories the producer produced for (but not itself) and the amount of units it produced
    final Map<Territory, Integer> redoPlacementsCount = new HashMap<>();
    // find map place territory -> possible free space for producer
    for (final UndoablePlacement placement : placements) {
      // find placement move of producer that can be taken over
      if (placement.getProducerTerritory().equals(producer)) {
        final Territory placeTerritory = placement.getPlaceTerritory();
        // units with requiresUnits are too difficult to mess with logically, so do not move them
        // around at all
        if (placeTerritory.isWater()
            && !placeTerritory.equals(producer)
            && (!Properties.getUnitPlacementRestrictions(getData().getProperties())
                || placement.getUnits().stream()
                    .noneMatch(Matches.unitRequiresUnitsOnCreation()))) {
          // found placement move of producer that can be taken over
          // remember move and amount of placements in that territory
          redoPlacements.add(placement);
          redoPlacementsCount.merge(placeTerritory, placement.getUnits().size(), Integer::sum);
        }
      }
    }
    // let other producers take over placements of producer
    // remember placement move and new territory if a placement has to be split up
    final Collection<Tuple<UndoablePlacement, Territory>> splitPlacements = new ArrayList<>();
    int foundSpaceTotal = 0;
    for (final Entry<Territory, Integer> entry : redoPlacementsCount.entrySet()) {
      final Territory placeTerritory = entry.getKey();
      final int maxProductionThatCanBeTakenOverFromThisPlacement = entry.getValue();
      // find other producers that could produce for the placeTerritory
      final List<Territory> potentialNewProducers =
          getAllProducers(placeTerritory, player, unitsLeftToPlace);
      potentialNewProducers.remove(producer);
      potentialNewProducers.sort(
          getBestProducerComparator(placeTerritory, unitsLeftToPlace, player));
      // we can just free a certain amount or still need a certain amount of space
      final int maxSpaceToBeFree =
          Math.min(maxProductionThatCanBeTakenOverFromThisPlacement, freeSize - foundSpaceTotal);
      // space that got free this on this placeTerritory
      int spaceAlreadyFree = 0;
      for (final Territory potentialNewProducerTerritory : potentialNewProducers) {
        int leftToPlace =
            getMaxUnitsToBePlacedFrom(
                potentialNewProducerTerritory,
                unitsPlacedInTerritorySoFar(placeTerritory),
                placeTerritory,
                player);
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
          if (placementSize <= leftToPlace) {
            // potentialNewProducerTerritory can take over complete production
            placement.setProducerTerritory(potentialNewProducerTerritory);
            removeFromProducedMap(producer, placedUnits);
            updateProducedMap(potentialNewProducerTerritory, placedUnits);
            spaceAlreadyFree += placementSize;
          } else {
            // potentialNewProducerTerritory can take over ONLY parts of the production
            // remember placement and potentialNewProducerTerritory but try to avoid splitting a
            // placement
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
    // we had a bug where we tried to split the same undoable placement twice (it can only be undone
    // once!)
    boolean unusedSplitPlacements = false;
    if (foundSpaceTotal < freeSize) {
      // we need to split some placement moves
      final var usedUndoablePlacements = new HashSet<UndoablePlacement>();
      for (final Tuple<UndoablePlacement, Territory> tuple : splitPlacements) {
        final UndoablePlacement placement = tuple.getFirst();
        if (usedUndoablePlacements.contains(placement)) {
          unusedSplitPlacements = true;
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
          // there is a chance we have 2 or more splitPlacements that are using the same placement
          // (trying to split the same placement).
          // So we must make sure that after we undo it the first time, it can never be undone
          // again.
          usedUndoablePlacements.add(placement);
          undoMove(placement.getIndex());
          performPlaceFrom(newProducer, unitsForNewProducer, placement.getPlaceTerritory(), player);
          performPlaceFrom(producer, unitsForOldProducer, placement.getPlaceTerritory(), player);
        }
      }
    }
    if (foundSpaceTotal < freeSize && unusedSplitPlacements) {
      freePlacementCapacity(producer, (freeSize - foundSpaceTotal), unitsLeftToPlace, at, player);
    }
  }

  // TODO Here's the spot for special air placement rules
  private @Nullable String moveAirOntoNewCarriers(
      final Territory at,
      final Territory producer,
      final Collection<Unit> units,
      final GamePlayer player,
      final CompositeChange placeChange) {
    if (!at.isWater()) {
      return null;
    }
    if (!Properties.getMoveExistingFightersToNewCarriers(getData().getProperties())
        || Properties.getLhtrCarrierProductionRules(getData().getProperties())) {
      return null;
    }
    if (units.stream().noneMatch(Matches.unitIsCarrier())) {
      return null;
    }
    // do we have any spare carrier capacity
    int capacity = AirMovementValidator.carrierCapacity(units, at);
    // subtract fighters that have already been produced with this carrier this turn.
    capacity -= AirMovementValidator.carrierCost(units);
    if (capacity <= 0) {
      return null;
    }
    if (!Matches.territoryIsLand().test(producer)) {
      return null;
    }
    if (!producer.anyUnitsMatch(Matches.unitCanProduceUnits())) {
      return null;
    }
    final Predicate<Unit> ownedFighters =
        Matches.unitCanLandOnCarrier().and(Matches.unitIsOwnedBy(player));
    if (!producer.anyUnitsMatch(ownedFighters)) {
      return null;
    }
    if (wasConquered(producer)) {
      return null;
    }
    if (getAlreadyProduced(producer).stream().anyMatch(Matches.unitCanProduceUnits())) {
      return null;
    }
    final List<Unit> fighters = producer.getUnitCollection().getMatches(ownedFighters);
    final Collection<Unit> movedFighters =
        bridge.getRemotePlayer().getNumberOfFightersToMoveToNewCarrier(fighters, producer);
    if (movedFighters == null || movedFighters.isEmpty()) {
      return null;
    }
    final Change change = ChangeFactory.moveUnits(producer, at, movedFighters);
    placeChange.add(change);
    return MyFormatter.unitsToTextNoOwner(movedFighters)
        + " moved from "
        + producer.getName()
        + " to "
        + at.getName();
  }

  /**
   * Subclasses can override this to change the way placements are made.
   *
   * @return null if placement is valid
   */
  private String isValidPlacement(
      final Collection<Unit> units, final Territory at, final GamePlayer player) {
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
    return canUnitsBePlaced(at, units, player);
  }

  /** Make sure the player has enough in hand to place the units. */
  private static @Nullable String playerHasEnoughUnits(
      final Collection<Unit> units, final GamePlayer player) {
    // make sure the player has enough units in hand to place
    if (!player.getUnits().containsAll(units)) {
      return "Not enough units";
    }
    return null;
  }

  /**
   * Test whether the territory has the factory resources to support the placement. AlreadyProduced
   * maps territory->units already produced this turn by that territory.
   */
  protected @Nullable String canProduce(
      final Territory to, final Collection<Unit> units, final GamePlayer player) {
    final Collection<Territory> producers = getAllProducers(to, player, units, true);
    // the only reason it could be empty is if its water and no territories adjacent have factories
    if (producers.isEmpty()) {
      return "No factory in or adjacent to " + to.getName();
    }
    if (producers.size() == 1) {
      return canProduce(CollectionUtils.getAny(producers), to, units, player);
    }
    final Collection<Territory> failingProducers = new ArrayList<>();
    final StringBuilder error = new StringBuilder();
    for (final Territory producer : producers) {
      final String errorP = canProduce(producer, to, units, player);
      if (errorP != null) {
        failingProducers.add(producer);
        // do not include the error for same territory, if water, because users do not want to see
        // this error report for 99.9% of games
        if (!(producer.equals(to) && producer.isWater())) {
          error.append(errorP).append(".\n");
        }
      }
    }
    if (producers.size() == failingProducers.size()) {
      return String.format(
          "Adjacent territories to %s cannot produce because:\n\n%s", to.getName(), error);
    }
    return null;
  }

  protected @Nullable String canProduce(
      final Territory producer,
      final Territory to,
      final Collection<Unit> units,
      final GamePlayer player) {
    return canProduce(producer, to, units, player, false);
  }

  /**
   * Tests if this territory can produce units. (Does not check if it has space left to do so)
   *
   * @param producer - Territory doing the producing.
   * @param to - Territory to be placed in.
   * @param units - Units to be placed.
   * @param player - Player doing the placing.
   * @param simpleCheck If true you return true even if a factory is not present. Used when you do
   *     not want an infinite loop (getAllProducers -> canProduce ->
   *     howManyOfEachConstructionCanPlace -> getAllProducers -> etc.)
   * @return - null if allowed to produce, otherwise an error String.
   */
  private @Nullable String canProduce(
      final Territory producer,
      final Territory to,
      final @Nullable Collection<Unit> units,
      final GamePlayer player,
      final boolean simpleCheck) {
    // units can be null if we are just testing the territory itself...
    final Collection<Unit> testUnits = (units == null ? List.of() : units);
    final boolean canProduceInConquered = isPlacementAllowedInCapturedTerritory(player);
    if (!producer.isOwnedBy(player)) {
      // sea constructions require either owning the sea zone or owning a surrounding land territory
      if (producer.isWater()
          && testUnits.stream().anyMatch(Matches.unitIsSea().and(Matches.unitIsConstruction()))) {
        boolean ownedNeighbor = false;
        for (final Territory current :
            getData().getMap().getNeighbors(to, Matches.territoryIsLand())) {
          if (current.isOwnedBy(player) && (canProduceInConquered || !wasConquered(current))) {
            ownedNeighbor = true;
            break;
          }
        }
        if (!ownedNeighbor) {
          return producer.getName()
              + " is not owned by you, and you have no owned neighbors which can produce";
        }
      } else {
        return producer.getName() + " is not owned by you";
      }
    }
    // make sure the territory wasn't conquered this turn
    if (!canProduceInConquered && wasConquered(producer)) {
      return producer.getName() + " was conquered this turn and cannot produce till next turn";
    }
    if (isPlayerAllowedToPlacementAnyTerritoryOwnedLand(player)
        && Matches.territoryIsLand().test(to)
        && Matches.isTerritoryOwnedBy(player).test(to)) {
      return null;
    }
    if (isPlayerAllowedToPlacementAnySeaZoneByOwnedLand(player)
        && Matches.territoryIsWater().test(to)
        && Matches.isTerritoryOwnedBy(player).test(producer)) {
      return null;
    }
    if (simpleCheck) {
      return null;
    }
    // make sure some unit has fulfilled requiresUnits requirements
    if (Properties.getUnitPlacementRestrictions(getData().getProperties())
        && !testUnits.isEmpty()
        && testUnits.stream().noneMatch(unitWhichRequiresUnitsHasRequiredUnits(producer, true))) {
      return "You do not have the required units to build in " + producer.getName();
    }
    if (to.isWater()
        && (!Properties.getWW2V2(getData().getProperties())
            && !Properties.getUnitPlacementInEnemySeas(getData().getProperties()))
        && to.anyUnitsMatch(Matches.enemyUnit(player))) {
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
      return "No more constructions allowed in " + producer.getName();
    }
    // check we haven't just put a factory there (should we be checking producer?)
    if (getAlreadyProduced(producer).stream().anyMatch(Matches.unitCanProduceUnits())
        || getAlreadyProduced(to).stream().anyMatch(Matches.unitCanProduceUnits())) {
      return "Factory in " + producer.getName() + " can't produce until 1 turn after it is created";
    }
    return "No factory in " + producer.getName();
  }

  /**
   * Returns the territories that would do the producing if units are to be placed in a given
   * territory. Returns an empty list if no suitable territory could be found.
   *
   * @param to - Territory to place in.
   * @param player - player that is placing.
   * @param unitsToPlace - Can be null, otherwise is the units that will be produced.
   * @param simpleCheck If true you return true even if a factory is not present. Used when you do
   *     not want an infinite loop (getAllProducers -> canProduce ->
   *     howManyOfEachConstructionCanPlace -> getAllProducers -> etc.)
   * @return - List of territories that can produce here.
   */
  private List<Territory> getAllProducers(
      final Territory to,
      final GamePlayer player,
      final @Nullable Collection<Unit> unitsToPlace,
      final boolean simpleCheck) {
    final List<Territory> producers = new ArrayList<>();
    // if not water then must produce in that territory
    if (!to.isWater()) {
      if (simpleCheck || canProduce(to, to, unitsToPlace, player, false) == null) {
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

  private List<Territory> getAllProducers(
      final Territory to, final GamePlayer player, final Collection<Unit> unitsToPlace) {
    return getAllProducers(to, player, unitsToPlace, false);
  }

  /**
   * Test whether the territory has the factory resources to support the placement. AlreadyProduced
   * maps territory->units already produced this turn by that territory.
   */
  protected @Nullable String checkProduction(
      final Territory to, final Collection<Unit> units, final GamePlayer player) {
    final List<Territory> producers = getAllProducers(to, player, units);
    if (producers.isEmpty()) {
      return "No factory in or adjacent to " + to.getName();
    }
    // if it's an original factory then unlimited production
    producers.sort(getBestProducerComparator(to, units, player));
    if (!getCanAllUnitsWithRequiresUnitsBePlacedCorrectly(units, to)) {
      return "Cannot place more units which require units, than production capacity of "
          + "territories with the required units";
    }
    final int maxUnitsToBePlaced = getMaxUnitsToBePlaced(units, to, player);
    if ((maxUnitsToBePlaced != -1) && (maxUnitsToBePlaced < units.size())) {
      return "Cannot place " + units.size() + " more units in " + to.getName();
    }
    return null;
  }

  /**
   * Returns {@code null} if the specified units can be placed in the specified territory; otherwise
   * returns an error message explaining why the units cannot be placed in the territory.
   */
  public @Nullable String canUnitsBePlaced(
      final Territory to, final Collection<Unit> units, final GamePlayer player) {
    final Collection<Unit> allowedUnits = getUnitsToBePlaced(to, units, player);
    if (allowedUnits == null || !allowedUnits.containsAll(units)) {
      return "Cannot place these units in " + to.getName();
    }
    // Although getUnitsToBePlaced() has checked stacking limits, it did it on a per-unit type
    // basis, which is not sufficient, since units may be mutually exclusive. So we need to also
    // check stacking limits over the full collection.
    Collection<Unit> filteredUnits =
        UnitStackingLimitFilter.filterUnits(
            units, PLACEMENT_LIMIT, player, to, produced.getOrDefault(to, List.of()));
    if (units.size() != filteredUnits.size()) {
      return "Cannot place these units in " + to.getName();
    }
    final IntegerMap<String> constructionMap =
        howManyOfEachConstructionCanPlace(to, to, units, player);
    for (final Unit currentUnit : CollectionUtils.getMatches(units, Matches.unitIsConstruction())) {
      final UnitAttachment ua = currentUnit.getUnitAttachment();
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
        TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, getData().getMap());
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
      if (!to.isOwnedBy(player)) {
        if (GameStepPropertiesHelper.isBid(getData())) {
          final PlayerAttachment pa = PlayerAttachment.get(to.getOwner());
          if ((pa == null || !pa.getGiveUnitControl().contains(player))
              && !to.anyUnitsMatch(Matches.unitIsOwnedBy(player))) {
            return "You don't own " + to.getName();
          }
        } else {
          return "You don't own " + to.getName();
        }
      }
      // make sure all units are land
      if (units.isEmpty() || !units.stream().allMatch(Matches.unitIsNotSea())) {
        return "Can't place sea units on land";
      }
    }
    // make sure we can place consuming units
    if (!canWeConsumeUnits(units, to, null)) {
      return "Not Enough Units To Upgrade or Be Consumed";
    }
    // now return null (valid placement) if we have placement restrictions disabled in game options
    if (!Properties.getUnitPlacementRestrictions(getData().getProperties())) {
      return null;
    }
    // account for any unit placement restrictions by territory
    for (final Unit currentUnit : units) {
      final UnitAttachment ua = currentUnit.getUnitAttachment();
      // Can be null!
      final TerritoryAttachment ta = TerritoryAttachment.get(to);
      if (ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1
          && ua.getCanOnlyBePlacedInTerritoryValuedAtX() > (ta == null ? 0 : ta.getProduction())) {
        return "Cannot place these units in "
            + to.getName()
            + " due to Unit Placement Restrictions on Territory Value";
      }
      if (ua.unitPlacementRestrictionsContain(to)) {
        return "Cannot place these units in "
            + to.getName()
            + " due to Unit Placement Restrictions";
      }
      if (Matches.unitCanOnlyPlaceInOriginalTerritories().test(currentUnit)
          && !Matches.territoryIsOriginallyOwnedBy(player).test(to)) {
        return "Cannot place these units in "
            + to.getName()
            + " as territory is not originally owned";
      }
    }
    return null;
  }

  protected @Nullable Collection<Unit> getUnitsToBePlaced(
      final Territory to, final Collection<Unit> allUnits, final GamePlayer player) {
    final GameProperties properties = getData().getProperties();
    final boolean water = to.isWater();
    if (water
        && (!Properties.getWW2V2(properties) && !Properties.getUnitPlacementInEnemySeas(properties))
        && to.anyUnitsMatch(Matches.enemyUnit(player))) {
      return null;
    }
    // if water, remove land. if land, remove water.
    final Collection<Unit> units =
        CollectionUtils.getMatches(
            allUnits, water ? not(Matches.unitIsLand()) : not(Matches.unitIsSea()));
    final Collection<Unit> placeableUnits = new ArrayList<>();
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Collection<Unit> allProducedUnits = unitsPlacedInTerritorySoFar(to);
    final boolean isBid = GameStepPropertiesHelper.isBid(getData());
    final boolean wasFactoryThereAtStart =
        wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(to, player);

    // we add factories and constructions later
    if (water
        || wasFactoryThereAtStart
        || isPlayerAllowedToPlacementAnyTerritoryOwnedLand(player)) {
      final Predicate<Unit> seaOrLandMatch = water ? Matches.unitIsSea() : Matches.unitIsLand();
      placeableUnits.addAll(
          CollectionUtils.getMatches(units, seaOrLandMatch.and(Matches.unitIsNotConstruction())));
      if (!water) {
        placeableUnits.addAll(
            CollectionUtils.getMatches(
                units, Matches.unitIsAir().and(Matches.unitIsNotConstruction())));
      } else {
        final boolean canProduceFightersOnCarriers =
            isBid
                || Properties.getProduceFightersOnCarriers(properties)
                || Properties.getLhtrCarrierProductionRules(properties);
        if (canProduceFightersOnCarriers
                && (allProducedUnits.stream().anyMatch(Matches.unitIsCarrier()))
            || to.anyUnitsMatch(unitIsCarrierOwnedByCombinedPlayers(player))) {
          placeableUnits.addAll(
              CollectionUtils.getMatches(
                  units, Matches.unitIsAir().and(Matches.unitCanLandOnCarrier())));
        }
      }
    }
    if (units.stream().anyMatch(Matches.unitIsConstruction())) {
      final IntegerMap<String> constructionsMap =
          howManyOfEachConstructionCanPlace(to, to, units, player);
      final var skipUnits = new HashSet<Unit>();
      for (final Unit currentUnit :
          CollectionUtils.getMatches(units, Matches.unitIsConstruction())) {
        final int maxUnits = howManyOfConstructionUnit(currentUnit, constructionsMap);
        if (maxUnits > 0) {
          // we are doing this because we could have multiple unitTypes with the same
          // constructionType, so we have to be able to place the max placement by constructionType
          // of each unitType
          if (skipUnits.contains(currentUnit)) {
            continue;
          }
          placeableUnits.addAll(
              CollectionUtils.getNMatches(
                  units, maxUnits, Matches.unitIsOfType(currentUnit.getType())));
          skipUnits.addAll(
              CollectionUtils.getMatches(units, Matches.unitIsOfType(currentUnit.getType())));
        }
      }
    }
    // remove any units that require other units to be consumed on creation, if we don't have enough
    // to consume (veqryn)
    placeableUnits.removeIf(
        Matches.unitConsumesUnitsOnCreation()
            .and(not(Matches.unitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTo))));

    final Collection<Unit> placeableUnits2;
    if (Properties.getUnitPlacementRestrictions(properties)) {
      final int territoryProduction = TerritoryAttachment.getProduction(to);
      placeableUnits2 = new ArrayList<>();
      for (final Unit currentUnit : placeableUnits) {
        final UnitAttachment ua = currentUnit.getUnitAttachment();
        if (ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1
            && ua.getCanOnlyBePlacedInTerritoryValuedAtX() > territoryProduction) {
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
        if (!ua.unitPlacementRestrictionsContain(to)) {
          placeableUnits2.add(currentUnit);
        }
      }
    } else {
      placeableUnits2 = placeableUnits;
    }
    // Limit count of each unit type to the max that can be placed based on unit requirements.
    for (UnitType ut : UnitUtils.getUnitTypesFromUnitList(placeableUnits)) {
      var unitsOfType = CollectionUtils.getMatches(placeableUnits2, Matches.unitIsOfType(ut));
      placeableUnits2.removeAll(getUnitsThatCantBePlacedThatRequireUnits(unitsOfType, to));
    }
    // now check stacking limits
    // Filter each type separately, since we don't want a max on one type to filter out all units of
    // another type, if the two types have a combined limit. UnitStackingLimitFilter doesn't do
    // that directly since other call sites (e.g. move validation) do need the combined filtering.
    // But we need to do it this way here, since the result will be shown for choosing which units
    // to build (where we want to show all the types that can be built).
    final var result = new ArrayList<Unit>();
    for (UnitType ut : UnitUtils.getUnitTypesFromUnitList(units)) {
      result.addAll(
          UnitStackingLimitFilter.filterUnits(
              CollectionUtils.getMatches(placeableUnits2, Matches.unitIsOfType(ut)),
              PLACEMENT_LIMIT,
              player,
              to,
              produced.getOrDefault(to, List.of())));
    }
    return result;
  }

  private Predicate<Unit> unitIsCarrierOwnedByCombinedPlayers(GamePlayer player) {
    final Predicate<Unit> ownedByMatcher =
        Matches.unitIsOwnedByAnyOf(GameStepPropertiesHelper.getCombinedTurns(getData(), player));
    return Matches.unitIsCarrier().and(ownedByMatcher);
  }

  private boolean canWeConsumeUnits(
      final Collection<Unit> units, final Territory to, final @Nullable CompositeChange change) {
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Collection<Unit> removedUnits = new ArrayList<>();
    final Collection<Unit> unitsWhichConsume =
        CollectionUtils.getMatches(units, Matches.unitConsumesUnitsOnCreation());
    for (final Unit unit : unitsWhichConsume) {
      if (!Matches.unitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTo).test(unit)) {
        return false;
      }
      // remove units which are now consumed, then test the rest of the consuming units on the
      // diminishing pile of units which were in the territory at start of turn
      final IntegerMap<UnitType> requiredUnitsMap = unit.getUnitAttachment().getConsumesUnits();
      for (final UnitType ut : requiredUnitsMap.keySet()) {
        final int requiredNumber = requiredUnitsMap.getInt(ut);
        final Predicate<Unit> unitIsOwnedByAndOfTypeAndNotDamaged =
            Matches.unitIsOwnedBy(unit.getOwner())
                .and(Matches.unitIsOfType(ut))
                .and(Matches.unitHasNotTakenAnyBombingUnitDamage())
                .and(Matches.unitHasNotTakenAnyDamage())
                .and(Matches.unitIsNotDisabled());
        final Collection<Unit> unitsBeingRemoved =
            CollectionUtils.getNMatches(
                unitsAtStartOfTurnInTo, requiredNumber, unitIsOwnedByAndOfTypeAndNotDamaged);
        unitsAtStartOfTurnInTo.removeAll(unitsBeingRemoved);
        // if we should actually do it, not just test, then add to bridge
        if (change != null) {
          change.add(ChangeFactory.removeUnits(to, unitsBeingRemoved));
          removedUnits.addAll(unitsBeingRemoved);
        }
      }
    }
    if (change != null && !change.isEmpty()) {
      String message =
          String.format(
              "Units in %s being upgraded or consumed: %s",
              to.getName(), MyFormatter.unitsToTextNoOwner(removedUnits));
      bridge.getHistoryWriter().startEvent(message, removedUnits);
    }
    return true;
  }

  /** Returns -1 if we can place unlimited units. */
  protected int getMaxUnitsToBePlaced(
      final Collection<Unit> units, final Territory to, final GamePlayer player) {
    final IntegerMap<Territory> map = getMaxUnitsToBePlacedMap(units, to, player);
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

  /** Returns -1 somewhere in the map if we can place unlimited units. */
  private IntegerMap<Territory> getMaxUnitsToBePlacedMap(
      final Collection<Unit> units, final Territory to, final GamePlayer player) {
    final IntegerMap<Territory> maxUnitsToBePlacedMap = new IntegerMap<>();
    final List<Territory> producers = getAllProducers(to, player, units);
    if (producers.isEmpty()) {
      return maxUnitsToBePlacedMap;
    }
    producers.sort(getBestProducerComparator(to, units, player));
    final Collection<Territory> notUsableAsOtherProducers = new ArrayList<>(producers);
    final Map<Territory, Integer> currentAvailablePlacementForOtherProducers = new HashMap<>();
    for (final Territory producerTerritory : producers) {
      final int prodT =
          getMaxUnitsToBePlacedFrom(
              producerTerritory,
              units,
              to,
              player,
              true,
              notUsableAsOtherProducers,
              currentAvailablePlacementForOtherProducers);
      maxUnitsToBePlacedMap.put(producerTerritory, prodT);
    }
    return maxUnitsToBePlacedMap;
  }

  /** Returns -1 if we can place unlimited units. */
  protected int getMaxUnitsToBePlacedFrom(
      final Territory producer,
      final Collection<Unit> units,
      final Territory to,
      final GamePlayer player) {
    return getMaxUnitsToBePlacedFrom(producer, units, to, player, false, null, null);
  }

  /** Returns -1 if we can place unlimited units. */
  protected int getMaxUnitsToBePlacedFrom(
      final Territory producer,
      final Collection<Unit> units,
      final Territory to,
      final GamePlayer player,
      final boolean countSwitchedProductionToNeighbors,
      final Collection<Territory> notUsableAsOtherProducers,
      final Map<Territory, Integer> currentAvailablePlacementForOtherProducers) {
    // we may have special units with requiresUnits restrictions
    final Collection<Unit> unitsCanBePlacedByThisProducer =
        (Properties.getUnitPlacementRestrictions(getData().getProperties())
            ? CollectionUtils.getMatches(
                units, unitWhichRequiresUnitsHasRequiredUnits(producer, true))
            : new ArrayList<>(units));
    if (unitsCanBePlacedByThisProducer.isEmpty()) {
      return 0;
    }
    // if it's an original factory then unlimited production
    // Can be null!
    final TerritoryAttachment ta = TerritoryAttachment.get(producer);
    final Predicate<Unit> factoryMatch =
        Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
            .and(Matches.unitIsBeingTransported().negate())
            .and(producer.isWater() ? Matches.unitIsLand().negate() : Matches.unitIsSea().negate());
    final Collection<Unit> factoryUnits = producer.getUnitCollection().getMatches(factoryMatch);
    // boolean placementRestrictedByFactory = isPlacementRestrictedByFactory();
    final boolean unitPlacementPerTerritoryRestricted =
        Properties.getUnitPlacementPerTerritoryRestricted(getData().getProperties());
    final boolean originalFactory = (ta != null && ta.getOriginalFactory());
    final boolean playerIsOriginalOwner =
        !factoryUnits.isEmpty() && this.player.equals(getOriginalFactoryOwner(producer));
    final RulesAttachment ra = player.getRulesAttachment();
    final Collection<Unit> alreadyProducedUnits = getAlreadyProduced(producer);
    final int unitCountAlreadyProduced = alreadyProducedUnits.size();
    if (originalFactory && playerIsOriginalOwner) {
      if (ra != null && ra.getMaxPlacePerTerritory() != -1) {
        return Math.max(0, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced);
      }
      return -1;
    }
    // Restricts based on the STARTING number of units in a territory (otherwise it is infinite
    // placement)
    if (unitPlacementPerTerritoryRestricted && ra != null && ra.getPlacementPerTerritory() > 0) {
      final int allowedPlacement = ra.getPlacementPerTerritory();
      final int ownedUnitsInTerritory =
          CollectionUtils.countMatches(to.getUnits(), Matches.unitIsOwnedBy(player));
      if (ownedUnitsInTerritory >= allowedPlacement) {
        return 0;
      }
      if (ra.getMaxPlacePerTerritory() == -1) {
        return -1;
      }
      return Math.max(0, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced);
    }
    // a factory can produce the same number of units as the number of PUs the territory generates
    // each turn (or not, if it has canProduceXUnits)
    final int maxConstructions =
        howManyOfEachConstructionCanPlace(to, producer, unitsCanBePlacedByThisProducer, player)
            .totalValues();
    final boolean wasFactoryThereAtStart =
        wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(producer, player);
    // If there's NO factory, allow placement of the factory
    if (!wasFactoryThereAtStart) {
      if (ra != null && ra.getMaxPlacePerTerritory() > 0) {
        return Math.max(
            0, Math.min(maxConstructions, ra.getMaxPlacePerTerritory() - unitCountAlreadyProduced));
      }
      return Math.max(0, maxConstructions);
    }
    // getHowMuchCanUnitProduce accounts for IncreasedFactoryProduction, but does not account for
    // maxConstructions
    int production =
        UnitUtils.getProductionPotentialOfTerritory(
            unitsAtStartOfStepInTerritory(producer),
            producer,
            player,
            getData().getProperties(),
            true,
            true);
    // increase the production by the number of constructions allowed
    if (maxConstructions > 0) {
      production += maxConstructions;
    }
    // return 0 if less than 0
    if (production < 0) {
      return 0;
    }
    production += CollectionUtils.countMatches(alreadyProducedUnits, Matches.unitIsConstruction());
    // Now we check if units we have already produced here could be produced by a different producer
    int unitCountHaveToAndHaveBeenBeProducedHere = unitCountAlreadyProduced;
    if (countSwitchedProductionToNeighbors && unitCountAlreadyProduced > 0) {
      if (notUsableAsOtherProducers == null) {
        throw new IllegalStateException(
            "notUsableAsOtherProducers cannot be null if "
                + "countSwitchedProductionToNeighbors is true");
      }
      if (currentAvailablePlacementForOtherProducers == null) {
        throw new IllegalStateException(
            "currentAvailablePlacementForOtherProducers cannot be null if "
                + "countSwitchedProductionToNeighbors is true");
      }
      int productionCanNotBeMoved = 0;
      int productionThatCanBeTakenOver = 0;
      // try to find a placement move (to an adjacent sea zone) that can be taken over by some other
      // territory factory
      for (final UndoablePlacement placementMove : placements) {
        if (placementMove.getProducerTerritory().equals(producer)) {
          final Territory placeTerritory = placementMove.getPlaceTerritory();
          final Collection<Unit> unitsPlacedByCurrentPlacementMove = placementMove.getUnits();
          // TODO: Units which have the unit attachment property, requiresUnits, are too difficult
          // to mess with logically, so we ignore them for our special 'move shit around' methods.
          if (!placeTerritory.isWater()
              || (Properties.getUnitPlacementRestrictions(getData().getProperties())
                  && unitsPlacedByCurrentPlacementMove.stream()
                      .anyMatch(Matches.unitRequiresUnitsOnCreation()))) {
            productionCanNotBeMoved += unitsPlacedByCurrentPlacementMove.size();
          } else {
            final int maxProductionThatCanBeTakenOverFromThisPlacement =
                unitsPlacedByCurrentPlacementMove.size();
            // find other producers for this placement move to the same water territory
            final List<Territory> newPotentialOtherProducers =
                getAllProducers(placeTerritory, player, unitsCanBePlacedByThisProducer);
            newPotentialOtherProducers.removeAll(notUsableAsOtherProducers);
            newPotentialOtherProducers.sort(
                getBestProducerComparator(placeTerritory, unitsCanBePlacedByThisProducer, player));
            int productionThatCanBeTakenOverFromThisPlacement = 0;
            for (final Territory potentialOtherProducer : newPotentialOtherProducers) {
              Integer potential =
                  currentAvailablePlacementForOtherProducers.get(potentialOtherProducer);
              if (potential == null) {
                potential =
                    getMaxUnitsToBePlacedFrom(
                        potentialOtherProducer,
                        unitsPlacedInTerritorySoFar(placeTerritory),
                        placeTerritory,
                        player);
              }
              if (potential == -1) {
                currentAvailablePlacementForOtherProducers.put(potentialOtherProducer, -1);
                productionThatCanBeTakenOverFromThisPlacement =
                    maxProductionThatCanBeTakenOverFromThisPlacement;
                break;
              }

              final int needed =
                  maxProductionThatCanBeTakenOverFromThisPlacement
                      - productionThatCanBeTakenOverFromThisPlacement;
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
            if (productionThatCanBeTakenOverFromThisPlacement
                > maxProductionThatCanBeTakenOverFromThisPlacement) {
              throw new IllegalStateException(
                  "productionThatCanBeTakenOverFromThisPlacement should never be larger "
                      + "than maxProductionThatCanBeTakenOverFromThisPlacement");
            }
            productionThatCanBeTakenOver += productionThatCanBeTakenOverFromThisPlacement;
          }
          if (productionThatCanBeTakenOver >= unitCountAlreadyProduced - productionCanNotBeMoved) {
            break;
          }
        }
      }
      unitCountHaveToAndHaveBeenBeProducedHere =
          Math.max(0, unitCountAlreadyProduced - productionThatCanBeTakenOver);
    }
    if (ra != null && ra.getMaxPlacePerTerritory() > 0) {
      int currentValue = unitCountHaveToAndHaveBeenBeProducedHere;
      int value = Math.min(production - currentValue, ra.getMaxPlacePerTerritory() - currentValue);
      return Math.max(0, value);
    }
    return Math.max(0, production - unitCountHaveToAndHaveBeenBeProducedHere);
  }

  /**
   * Calculates how many of each of the specified construction units can be placed in the specified
   * territory.
   *
   * @param to referring territory.
   * @param units units to place
   * @param player PlayerId
   * @return an empty IntegerMap if you can't produce any constructions (will never return null)
   */
  IntegerMap<String> howManyOfEachConstructionCanPlace(
      final Territory to,
      final Territory producer,
      final @Nullable Collection<Unit> units,
      final GamePlayer player) {
    // constructions can ONLY be produced BY the same territory that they are going into!
    if (!to.equals(producer)
        || units == null
        || units.isEmpty()
        || units.stream().noneMatch(Matches.unitIsConstruction())) {
      return new IntegerMap<>();
    }
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Collection<Unit> unitsInTo = to.getUnits();
    final Collection<Unit> unitsPlacedAlready = getAlreadyProduced(to);
    // build an integer map of each unit we have in our list of held units, as well as integer maps
    // for maximum units and units per turn
    final IntegerMap<String> unitMapHeld = new IntegerMap<>();
    final IntegerMap<String> unitMapMaxType = new IntegerMap<>();
    final IntegerMap<String> unitMapTypePerTurn = new IntegerMap<>();
    final int maxFactory = Properties.getFactoriesPerCountry(getData().getProperties());
    // Can be null!
    final TerritoryAttachment terrAttachment = TerritoryAttachment.get(to);
    int toProduction = 0;
    if (terrAttachment != null) {
      toProduction = terrAttachment.getProduction();
    }
    for (final Unit currentUnit : CollectionUtils.getMatches(units, Matches.unitIsConstruction())) {
      final UnitAttachment ua = currentUnit.getUnitAttachment();
      // account for any unit placement restrictions by territory
      if (Properties.getUnitPlacementRestrictions(getData().getProperties())) {
        if (ua.unitPlacementRestrictionsContain(to)) {
          continue;
        }
        if (ua.getCanOnlyBePlacedInTerritoryValuedAtX() != -1
            && ua.getCanOnlyBePlacedInTerritoryValuedAtX() > toProduction) {
          continue;
        }
        if (unitWhichRequiresUnitsHasRequiredUnits(to, false).negate().test(currentUnit)) {
          continue;
        }
      }
      // remove any units that require other units to be consumed on creation (veqryn)
      if (Matches.unitConsumesUnitsOnCreation().test(currentUnit)
          && Matches.unitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTo)
              .negate()
              .test(currentUnit)) {
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
    final boolean moreWithoutFactory =
        Properties.getMoreConstructionsWithoutFactory(getData().getProperties());
    final boolean moreWithFactory =
        Properties.getMoreConstructionsWithFactory(getData().getProperties());
    final boolean unlimitedConstructions =
        Properties.getUnlimitedConstructions(getData().getProperties());
    final boolean wasFactoryThereAtStart =
        wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(to, player);
    // build an integer map of each construction unit in the territory
    final IntegerMap<String> unitMapTo = new IntegerMap<>();
    if (unitsInTo.stream().anyMatch(Matches.unitIsConstruction())) {
      for (final Unit currentUnit :
          CollectionUtils.getMatches(unitsInTo, Matches.unitIsConstruction())) {
        final UnitAttachment ua = currentUnit.getUnitAttachment();
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
        if (wasFactoryThereAtStart
            && !constructionType.equals(Constants.CONSTRUCTION_TYPE_FACTORY)
            && !constructionType.endsWith(Constants.CONSTRUCTION_TYPE_STRUCTURE)) {
          unitMax =
              Math.max(
                  Math.max(unitMax, (moreWithFactory ? toProduction : 0)),
                  (unlimitedConstructions ? 10000 : 0));
        }
        if (!wasFactoryThereAtStart
            && !constructionType.equals(Constants.CONSTRUCTION_TYPE_FACTORY)
            && !constructionType.endsWith(Constants.CONSTRUCTION_TYPE_STRUCTURE)) {
          unitMax =
              Math.max(
                  Math.max(unitMax, (moreWithoutFactory ? toProduction : 0)),
                  (unlimitedConstructions ? 10000 : 0));
        }
        int value =
            Math.min(
                unitMax - unitMapTo.getInt(constructionType), unitMapHeld.getInt(constructionType));
        unitMapHeld.put(constructionType, Math.max(0, value));
      }
    }
    // deal with already placed units
    for (final Unit currentUnit :
        CollectionUtils.getMatches(unitsPlacedAlready, Matches.unitIsConstruction())) {
      final UnitAttachment ua = currentUnit.getUnitAttachment();
      unitMapTypePerTurn.add(ua.getConstructionType(), -1);
    }
    // modify this list based on how many we can place per turn
    final IntegerMap<String> unitsAllowed = new IntegerMap<>();
    for (final String constructionType : unitMapHeld.keySet()) {
      final int unitAllowed =
          Math.max(
              0,
              Math.min(
                  unitMapTypePerTurn.getInt(constructionType),
                  unitMapHeld.getInt(constructionType)));
      if (unitAllowed > 0) {
        unitsAllowed.put(constructionType, unitAllowed);
      }
    }
    // return our integer map
    return unitsAllowed;
  }

  static int howManyOfConstructionUnit(final Unit unit, final IntegerMap<String> constructionsMap) {
    final UnitAttachment ua = unit.getUnitAttachment();
    if (!ua.getIsConstruction()
        || ua.getConstructionsPerTerrPerTypePerTurn() < 1
        || ua.getMaxConstructionsPerTypePerTerr() < 1) {
      return 0;
    }
    return Math.max(0, constructionsMap.getInt(ua.getConstructionType()));
  }

  /**
   * Returns a predicate that indicates whether the territory contains one of the required combos of
   * units (and if 'doNotCountNeighbors' is false, and territory is water, will return true if an
   * adjacent land territory has one of the required combos as well).
   *
   * @param to - Territory we are testing for required units
   * @param doNotCountNeighbors If false, and 'to' is water, then we will test neighboring land
   *     territories to see if they have any of the required units as well.
   */
  private Predicate<Unit> unitWhichRequiresUnitsHasRequiredUnits(
      final Territory to, final boolean doNotCountNeighbors) {
    return unitWhichRequiresUnits -> {
      if (!Matches.unitRequiresUnitsOnCreation().test(unitWhichRequiresUnits)) {
        return true;
      }
      final Collection<Unit> unitsAtStartOfTurnInProducer = unitsAtStartOfStepInTerritory(to);
      // do not need to remove unowned here, as this match will remove unowned units from
      // consideration.
      if (Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInProducer)
          .test(unitWhichRequiresUnits)) {
        return true;
      }
      if (!doNotCountNeighbors && to.isWater()) {
        for (final Territory current :
            getAllProducers(to, player, List.of(unitWhichRequiresUnits), true)) {
          final Collection<Unit> unitsAtStartOfTurnInCurrent =
              unitsAtStartOfStepInTerritory(current);
          if (Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInCurrent)
              .test(unitWhichRequiresUnits)) {
            return true;
          }
        }
      }
      return false;
    };
  }

  private boolean getCanAllUnitsWithRequiresUnitsBePlacedCorrectly(
      final Collection<Unit> units, final Territory to) {
    return getUnitsThatCantBePlacedThatRequireUnits(units, to).isEmpty();
  }

  private Collection<Unit> getUnitsThatCantBePlacedThatRequireUnits(
      final Collection<Unit> units, final Territory to) {
    if (!Properties.getUnitPlacementRestrictions(getData().getProperties())
        || units.stream().noneMatch(Matches.unitRequiresUnitsOnCreation())) {
      return List.of();
    }
    final IntegerMap<Territory> producersMap = getMaxUnitsToBePlacedMap(units, to, player);
    final List<Territory> producers = getAllProducers(to, player, units);
    if (producers.isEmpty()) {
      return units;
    }
    producers.sort(getBestProducerComparator(to, units, player));
    final Collection<Unit> unitsLeftToPlace = new ArrayList<>(units);
    for (final Territory t : producers) {
      if (unitsLeftToPlace.isEmpty()) {
        return List.of();
      }
      final int productionHere = producersMap.getInt(t);
      final List<Unit> canBePlacedHere =
          CollectionUtils.getMatches(
              unitsLeftToPlace, unitWhichRequiresUnitsHasRequiredUnits(t, true));
      if (productionHere == -1 || productionHere >= canBePlacedHere.size()) {
        unitsLeftToPlace.removeAll(canBePlacedHere);
        continue;
      }
      canBePlacedHere.sort(getHardestToPlaceWithRequiresUnitsRestrictions());
      final Collection<Unit> placedHere =
          CollectionUtils.getNMatches(canBePlacedHere, productionHere, it -> true);
      unitsLeftToPlace.removeAll(placedHere);
    }
    return unitsLeftToPlace;
  }

  private Comparator<Territory> getBestProducerComparator(
      final Territory to, final Collection<Unit> units, final GamePlayer player) {
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

  private static Comparator<Unit> getUnitConstructionComparator() {
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

  private static Comparator<Unit> getHardestToPlaceWithRequiresUnitsRestrictions() {
    return (u1, u2) -> {
      if (Objects.equals(u1, u2)) {
        return 0;
      }
      final UnitAttachment ua1 = u1.getUnitAttachment();
      final UnitAttachment ua2 = u2.getUnitAttachment();
      if (ua1 == null && ua2 == null) {
        return 0;
      }
      if (ua1 != null && ua2 == null) {
        return -1;
      }
      if (ua1 == null) {
        return 1;
      }

      // constructions go ahead first
      final int constructionSort = getUnitConstructionComparator().compare(u1, u2);
      if (constructionSort != 0) {
        return constructionSort;
      }
      final List<String[]> ru1 = ua1.getRequiresUnits();
      final List<String[]> ru2 = ua2.getRequiresUnits();
      final int rus1 =
          (ru1 == null ? Integer.MAX_VALUE : (ru1.isEmpty() ? Integer.MAX_VALUE : ru1.size()));
      final int rus2 =
          (ru2 == null ? Integer.MAX_VALUE : (ru2.isEmpty() ? Integer.MAX_VALUE : ru2.size()));
      // fewer means more difficult, and more difficult goes to front of list.
      return Integer.compare(rus1, rus2);
    };
  }

  /**
   * Returns a collection of units that were there in the specified territory at start of turn.
   *
   * @param to referring territory.
   */
  public Collection<Unit> unitsAtStartOfStepInTerritory(final @Nullable Territory to) {
    if (to == null) {
      return new ArrayList<>();
    }
    final Collection<Unit> unitsPlacedAlready = getAlreadyProduced(to);
    if (to.isWater()) {
      for (final Territory current : getAllProducers(to, player, null, true)) {
        unitsPlacedAlready.addAll(getAlreadyProduced(current));
      }
    }
    final Collection<Unit> unitsAtStartOfTurnInTo = new ArrayList<>(to.getUnits());
    unitsAtStartOfTurnInTo.removeAll(unitsPlacedAlready);
    return unitsAtStartOfTurnInTo;
  }

  private Collection<Unit> unitsPlacedInTerritorySoFar(final Territory to) {
    final Collection<Unit> unitsInTo = new ArrayList<>(to.getUnits());
    final Collection<Unit> unitsAtStartOfStep = unitsAtStartOfStepInTerritory(to);
    unitsInTo.removeAll(unitsAtStartOfStep);
    return unitsInTo;
  }

  /**
   * Indicates whether there was an owned unit capable of producing, in this territory at the start
   * of this phase/step.
   *
   * @param to referring territory.
   * @param player PlayerId
   */
  private boolean wasOwnedUnitThatCanProduceUnitsOrIsFactoryInTerritoryAtStartOfStep(
      final Territory to, final GamePlayer player) {
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Predicate<Unit> factoryMatch =
        Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
            .and(Matches.unitIsBeingTransported().negate())
            // land factories in water can't produce, and sea factories in land can't produce.
            // air can produce like land if in land, and like sea if in water.
            .and(to.isWater() ? Matches.unitIsLand().negate() : Matches.unitIsSea().negate());
    return unitsAtStartOfTurnInTo.stream().anyMatch(factoryMatch);
  }

  /**
   * There must be a factory in the territory or an illegal state exception will be thrown. return
   * value may be null.
   */
  private GamePlayer getOriginalFactoryOwner(final Territory territory) {
    final Collection<Unit> factoryUnits =
        territory.getUnitCollection().getMatches(Matches.unitCanProduceUnits());
    if (factoryUnits.isEmpty()) {
      throw new IllegalStateException("No factory in territory:" + territory);
    }
    for (final Unit factory : factoryUnits) {
      if (player.equals(factory.getOriginalOwner())) {
        return factory.getOriginalOwner();
      }
    }
    return CollectionUtils.getAny(factoryUnits).getOriginalOwner();
  }

  /**
   * The rule is that new fighters can be produced on new carriers. This does not allow for fighters
   * to be produced on old carriers. THIS ISN'T CORRECT.
   */
  private static @Nullable String validateNewAirCanLandOnCarriers(
      final Territory to, final Collection<Unit> units) {
    final int cost = AirMovementValidator.carrierCost(units);
    int capacity = AirMovementValidator.carrierCapacity(units, to);
    capacity += AirMovementValidator.carrierCapacity(to.getUnits(), to);
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

  protected boolean wasConquered(final Territory t) {
    final BattleTracker tracker = getData().getBattleDelegate().getBattleTracker();
    return tracker.wasConquered(t);
  }

  private boolean isPlayerAllowedToPlacementAnyTerritoryOwnedLand(final GamePlayer player) {
    if (Properties.getPlaceInAnyTerritory(getData().getProperties())) {
      final RulesAttachment ra = player.getRulesAttachment();
      return ra != null && ra.getPlacementAnyTerritory();
    }
    return false;
  }

  private boolean isPlayerAllowedToPlacementAnySeaZoneByOwnedLand(final GamePlayer player) {
    if (Properties.getPlaceInAnyTerritory(getData().getProperties())) {
      final RulesAttachment ra = player.getRulesAttachment();
      return ra != null && ra.getPlacementAnySeaZone();
    }
    return false;
  }

  private static boolean isPlacementAllowedInCapturedTerritory(final GamePlayer player) {
    final RulesAttachment ra = player.getRulesAttachment();
    return ra != null && ra.getPlacementCapturedTerritory();
  }

  private static boolean isPlacementInCapitalRestricted(final GamePlayer player) {
    final RulesAttachment ra = player.getRulesAttachment();
    return ra != null && ra.getPlacementInCapitalRestricted();
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return IAbstractPlaceDelegate.class;
  }
}
