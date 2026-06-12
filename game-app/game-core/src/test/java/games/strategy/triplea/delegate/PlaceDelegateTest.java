package games.strategy.triplea.delegate;

import static games.strategy.triplea.Constants.DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES;
import static games.strategy.triplea.Constants.MOVE_EXISTING_FIGHTERS_TO_NEW_CARRIERS;
import static games.strategy.triplea.Constants.UNIT_PLACEMENT_RESTRICTIONS;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate.BidMode.NOT_BID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

// Note: This inherits from PlaceDelegateTestCommon and the tests defined there are included.
class PlaceDelegateTest extends PlaceDelegateTestCommon {
  @Override
  protected void setupDelegate(GamePlayer player) {
    final IDelegateBridge bridge = newDelegateBridge(player);
    delegate = new PlaceDelegate();
    delegate.initialize("place");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    // Replace the XML-loaded place delegate so undoMove resolves the same instance the test
    // mutates via delegate.placeUnits.
    gameData.addDelegate(delegate);
  }

  @Test
  void testCannotPlaceWithoutFactory() {
    final Optional<String> response =
        delegate.placeUnits(create(british, infantry, 2), egypt, NOT_BID);
    assertError(response);
  }

  @Test
  void testCannotPlaceSeaWithoutFactory() {
    final Optional<String> response =
        delegate.placeUnits(create(british, transport, 2), redSea, NOT_BID);
    assertError(response);
  }

  @Test
  void testCannotProduceThatManyUnits() {
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 3), westCanada);
    assertEquals(2, response.getMaxUnits());
  }

  @Test
  void getPlaceableUnitsThrowsWhenNoPlayerIsActive() {
    // Models the current failure mode: a remote placement query reaches the
    // delegate before ServerGame has assigned the runtime player field.
    final IDelegateBridge bridge = mock(IDelegateBridge.class);
    delegate = new PlaceDelegate();
    delegate.initialize("place");
    delegate.setDelegateBridgeAndPlayer(bridge);

    assertThrows(
        NullPointerException.class,
        () -> delegate.getPlaceableUnits(create(british, infantry, 1), westCanada));
  }

  @Test
  void stepChangeListenerCanObservePlaceDelegateBeforePlayerIsAssigned() {
    // Models the server start-step ordering: GAME_STEP_CHANGED is fired before
    // ServerGame calls setDelegateBridgeAndPlayer on the current delegate.
    // This does not model the client click; it pins down the server-side
    // window where a place delegate can still have no active player.
    final PlaceDelegate placeDelegate = new PlaceDelegate();
    placeDelegate.initialize("place");
    final AtomicReference<GamePlayer> playerAtStepChange = new AtomicReference<>();
    gameData.addGameDataEventListener(
        GameDataEvent.GAME_STEP_CHANGED, () -> playerAtStepChange.set(placeDelegate.player));

    gameData.fireGameDataEvent(GameDataEvent.GAME_STEP_CHANGED);
    placeDelegate.setDelegateBridgeAndPlayer(newDelegateBridge(british));

    assertThat(playerAtStepChange.get(), nullValue());
    assertThat(placeDelegate.player, is(british));
  }

  @Test
  void restoredPlaceDelegateHasNoPlayerUntilBridgeIsAssigned() {
    // Models a save/load boundary: saveState/loadState restores persisted
    // placement state, but runtime fields such as bridge/player are reattached
    // later by ServerGame. A remote call during that interval observes no player.
    final PlaceDelegate runningDelegate = new PlaceDelegate();
    runningDelegate.initialize("place");
    runningDelegate.setDelegateBridgeAndPlayer(newDelegateBridge(british));
    final Serializable savedState = runningDelegate.saveState();

    final PlaceDelegate restoredDelegate = new PlaceDelegate();
    restoredDelegate.initialize("place");
    restoredDelegate.loadState(savedState);

    assertThat(restoredDelegate.player, nullValue());
    assertThrows(
        NullPointerException.class,
        () -> restoredDelegate.getPlaceableUnits(create(british, infantry, 1), westCanada));

    restoredDelegate.setDelegateBridgeAndPlayer(newDelegateBridge(british));

    assertThat(restoredDelegate.player, is(british));
  }

  @Test
  void testCannotProduceThatManyUnitsDueToRequiresUnits() {
    gameData.getProperties().set(UNIT_PLACEMENT_RESTRICTIONS, true);
    // Needed for canProduceXUnits to work. (!)
    gameData.getProperties().set(DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES, true);

    final var threeInfantry2 = create(british, infantry2, 3);
    final var fourInfantry2 = create(british, infantry2, 4);

    uk.getUnitCollection().clear();
    assertError(delegate.canUnitsBePlaced(uk, threeInfantry2, british));
    uk.getUnitCollection().addAll(create(british, factory2, 1));
    assertValid(delegate.canUnitsBePlaced(uk, threeInfantry2, british));
    assertError(delegate.canUnitsBePlaced(uk, fourInfantry2, british));
    final PlaceableUnits response = delegate.getPlaceableUnits(fourInfantry2, uk);
    assertThat(response.getUnits(), hasSize(3));
  }

  @Test
  void testRequiresUnitsSea() {
    gameData.getProperties().set(UNIT_PLACEMENT_RESTRICTIONS, true);
    // Needed for canProduceXUnits to work. (!)
    gameData.getProperties().set(DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES, true);
    final var sub2 = unitType("submarine2", gameData);

    final var threeSub2 = create(british, sub2, 3);
    final var fourSub2 = create(british, sub2, 4);

    uk.getUnitCollection().clear();
    northSea.getUnitCollection().clear();
    assertError(delegate.canUnitsBePlaced(northSea, threeSub2, british));
    uk.getUnitCollection().addAll(create(british, factory2, 1));
    assertValid(delegate.canUnitsBePlaced(northSea, threeSub2, british));
    assertError(delegate.canUnitsBePlaced(northSea, fourSub2, british));
    final PlaceableUnits response = delegate.getPlaceableUnits(fourSub2, northSea);
    assertThat(response.getUnits(), hasSize(3));
    // We also can't place the subs in UK since they're sea units. :)
    assertError(delegate.canUnitsBePlaced(uk, threeSub2, british));
  }

  @Test
  void testAlreadyProducedUnits() {
    delegate.setProduced(Map.of(westCanada, create(british, infantry, 2)));
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 1), westCanada);
    assertEquals(0, response.getMaxUnits());
  }

  // Regression test for https://github.com/triplea-game/triplea/issues/8434.
  // When "Move existing fighters to new carriers" is enabled, placing a carrier may move a
  // previously-placed fighter onto it. Undoing the fighter placement first would otherwise
  // duplicate the unit (it would be removed from the producer territory it no longer occupies
  // while also being added back to the player's hand). The carrier placement now becomes a
  // dependent of the fighter placement, so the fighter can't be undone until the carrier is.
  @Test
  void testCannotUndoFighterPlacementWhileItIsOnANewlyPlacedCarrier() {
    // Advance to the place step so undoSpecific can resolve the current placement delegate.
    advanceToStep(delegate.getBridge(), "britishPlace");
    gameData.getProperties().set(MOVE_EXISTING_FIGHTERS_TO_NEW_CARRIERS, true);
    final var fighters = create(british, fighter, 1);
    final var carriers = create(british, carrier, 1);
    when(delegate.getBridge().getRemotePlayer().getNumberOfFightersToMoveToNewCarrier(any(), any()))
        .thenAnswer(invocation -> invocation.<Collection<Unit>>getArgument(0));

    assertValid(delegate.placeUnits(fighters, uk, NOT_BID));
    assertValid(delegate.placeUnits(carriers, northSea, NOT_BID));

    // Sanity check: the fighter has been moved from UK to the North Sea Zone (onto the carrier).
    assertThat(uk.getMatches(Matches.unitIsOfType(fighter)), is(empty()));
    assertThat(northSea.getMatches(Matches.unitIsOfType(fighter)), contains(fighters.toArray()));

    final List<UndoablePlacement> placements = delegate.getMovesMade();
    assertThat(placements, hasSize(2));
    final UndoablePlacement fighterPlacement = placements.get(0);
    final UndoablePlacement carrierPlacement = placements.get(1);
    assertThat(fighterPlacement.getCanUndo(), is(false));
    assertThat(carrierPlacement.getCanUndo(), is(true));

    // Attempting to undo the fighter placement first must fail with an informative reason rather
    // than corrupt the unit collection.
    final String reason = delegate.undoMove(fighterPlacement.getIndex());
    assertThat(reason, is(notNullValue()));
    assertThat(reason, containsString("must be undone first"));
    assertThat(delegate.getMovesMade(), hasSize(2));

    // Undoing the carrier first puts the fighter back in UK and clears the dependency.
    assertThat(delegate.undoMove(carrierPlacement.getIndex()), is(nullValue()));
    assertThat(uk.getMatches(Matches.unitIsOfType(fighter)), contains(fighters.toArray()));
    assertThat(northSea.getMatches(Matches.unitIsOfType(carrier)), is(empty()));
    assertThat(delegate.getMovesMade(), hasSize(1));
    assertThat(delegate.getMovesMade().get(0).getCanUndo(), is(true));

    // Now the fighter placement undoes cleanly.
    assertThat(delegate.undoMove(fighterPlacement.getIndex()), is(nullValue()));
    assertThat(uk.getMatches(Matches.unitIsOfType(fighter)), is(empty()));
    assertThat(delegate.getMovesMade(), is(empty()));
  }

  // Regression test for https://github.com/triplea-game/triplea/issues/9165.
  // Exercises freePlacementCapacity's split-placements pass: a prior sea-zone placement that
  // exceeds a sibling producer's capacity must be split across producers, freeing up production
  // for a new placement at the original producer.
  @Test
  void testFreePlacementCapacitySplitsPriorPlacementAcrossNeighborProducers() {
    advanceToStep(delegate.getBridge(), "britishPlace");
    final Territory eastCanadaSeaZone =
        gameData.getMap().getTerritoryOrNull("East Canada Sea Zone");
    assertThat(eastCanadaSeaZone, is(notNullValue()));

    eastCanada.getUnitCollection().addAll(create(british, factory, 1));
    // Seed East Canada at 1 of 2 capacity so a 2-transport placement won't fit whole and falls
    // into the split pass. Use a mutable map: subsequent placements update it via put().
    delegate.setProduced(new HashMap<>(Map.of(eastCanada, create(british, infantry, 1))));

    assertValid(delegate.placeUnits(create(british, transport, 2), eastCanadaSeaZone, NOT_BID));
    final List<UndoablePlacement> afterTransports = delegate.getMovesMade();
    assertThat(afterTransports, hasSize(1));
    assertThat(afterTransports.get(0).getProducerTerritory(), is(westCanada));
    assertThat(afterTransports.get(0).getUnits(), hasSize(2));

    final PlaceableUnits placeable =
        delegate.getPlaceableUnits(create(british, infantry, 1), westCanada);
    assertEquals(1, placeable.getMaxUnits());

    assertValid(delegate.placeUnits(create(british, infantry, 1), westCanada, NOT_BID));

    assertThat(westCanada.getMatches(Matches.unitIsOfType(infantry)), hasSize(1));

    final List<UndoablePlacement> seaPlacementsAfter =
        delegate.getMovesMade().stream()
            .filter(p -> p.getPlaceTerritory().equals(eastCanadaSeaZone))
            .collect(Collectors.toList());
    assertThat(seaPlacementsAfter, hasSize(2));
    final Set<Territory> producers =
        seaPlacementsAfter.stream()
            .map(UndoablePlacement::getProducerTerritory)
            .collect(Collectors.toSet());
    assertThat(producers, containsInAnyOrder(westCanada, eastCanada));
  }
}
