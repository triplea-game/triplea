package games.strategy.triplea.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StackCapacityResolverTest {
  private static final String SMALL_FRONT_MEUSE_GAME = "Small Front: Meuse Corridor";

  private final GameData gameData = new GameData();
  private final AtomicInteger names = new AtomicInteger();

  @Test
  void shortestFiniteCapacityWinsAcrossEffects() {
    assertEquals(
        2, StackCapacityResolver.resolveCapacity(List.of(effect(4), effect(2))).orElseThrow());
  }

  @Test
  void finiteCapacityWinsOverUnlimitedEffect() {
    assertEquals(
        3, StackCapacityResolver.resolveCapacity(List.of(effect(-1), effect(3))).orElseThrow());
  }

  @Test
  void allUnlimitedEffectsResolveToUnlimited() {
    assertEquals(
        -1, StackCapacityResolver.resolveCapacity(List.of(effect(-1), effect(-1))).orElseThrow());
  }

  @Test
  void missingCapacityRemainsUnconfigured() {
    final TerritoryEffect effect = new TerritoryEffect("plain", gameData);
    effect.addAttachment(
        Constants.TERRITORYEFFECT_ATTACHMENT_NAME,
        new TerritoryEffectAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, effect, gameData));
    assertFalse(StackCapacityResolver.resolveCapacity(List.of(effect)).isPresent());
  }

  @Test
  void smallFrontMeuseUsesRelaxedTerrainCapacities() {
    assertEquals(
        7,
        StackCapacityResolver.resolveCapacity(
                List.of(namedEffect("Open", 6)), SMALL_FRONT_MEUSE_GAME)
            .orElseThrow());
    assertEquals(
        6,
        StackCapacityResolver.resolveCapacity(
                List.of(namedEffect("Town", 5)), SMALL_FRONT_MEUSE_GAME)
            .orElseThrow());
    assertEquals(
        5,
        StackCapacityResolver.resolveCapacity(
                List.of(namedEffect("Forest", 3)), SMALL_FRONT_MEUSE_GAME)
            .orElseThrow());
  }

  @Test
  void otherGamesKeepConfiguredTerrainCapacity() {
    assertEquals(
        3,
        StackCapacityResolver.resolveCapacity(List.of(namedEffect("Forest", 3)), "Another Game")
            .orElseThrow());
  }

  @Test
  void acceptsCandidatesInStableOrderUntilCapacityIsFull() {
    final GamePlayer owner = mock(GamePlayer.class);
    final Unit first = unit(owner, 2);
    final Unit second = unit(owner, 1);
    final Unit third = unit(owner, 2);
    assertEquals(
        List.of(first, second),
        StackCapacityResolver.filterUnitsToFit(
            List.of(first, second, third), owner, List.of(effect(3)), List.of(), List.of()));
  }

  @Test
  void existingOverCapacityStackAllowsOnlyZeroCostEntries() {
    final GamePlayer owner = mock(GamePlayer.class);
    final Unit existing = unit(owner, 4);
    final Unit free = unit(owner, 0);
    final Unit costly = unit(owner, 1);
    assertEquals(
        List.of(free),
        StackCapacityResolver.filterUnitsToFit(
            List.of(free, costly), owner, List.of(effect(3)), List.of(existing), List.of()));
  }

  @Test
  void pendingUnitsConsumeCapacityForPlacementAndReinforcementBatches() {
    final GamePlayer owner = mock(GamePlayer.class);
    final Unit pending = unit(owner, 2);
    final Unit candidate = unit(owner, 2);
    assertEquals(
        List.of(),
        StackCapacityResolver.filterUnitsToFit(
            List.of(candidate), owner, List.of(effect(3)), List.of(), List.of(pending)));
  }

  @Test
  void invalidAttachmentValuesAreRejected() {
    final TerritoryEffect effect = new TerritoryEffect("invalid", gameData);
    final TerritoryEffectAttachment attachment =
        new TerritoryEffectAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, effect, gameData);
    assertThrows(IllegalArgumentException.class, () -> attachment.setStackCapacity(-2));
    final UnitType type = new UnitType("invalid-cost", gameData);
    final UnitAttachment unitAttachment =
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, type, gameData);
    assertThrows(IllegalArgumentException.class, () -> unitAttachment.setStackCost(-1));
  }

  private TerritoryEffect effect(final int capacity) {
    return namedEffect("effect-" + names.incrementAndGet(), capacity);
  }

  private TerritoryEffect namedEffect(final String name, final int capacity) {
    final TerritoryEffect effect = new TerritoryEffect(name, gameData);
    final TerritoryEffectAttachment attachment =
        new TerritoryEffectAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, effect, gameData)
            .setStackCapacity(capacity);
    effect.addAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, attachment);
    return effect;
  }

  private static Unit unit(final GamePlayer owner, final int cost) {
    final Unit unit = mock(Unit.class);
    final UnitAttachment attachment = mock(UnitAttachment.class);
    when(unit.getOwner()).thenReturn(owner);
    when(unit.getUnitAttachment()).thenReturn(attachment);
    when(attachment.getStackCost()).thenReturn(cost);
    return unit;
  }
}
