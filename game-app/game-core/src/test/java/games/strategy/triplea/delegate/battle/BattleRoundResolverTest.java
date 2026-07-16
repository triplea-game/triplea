package games.strategy.triplea.delegate.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class BattleRoundResolverTest {
  @Test
  void groundBattleFallsBackToGlobalLandRoundsOutsideSmallFront() {
    final GameData gameData = new GameData();
    gameData.getProperties().set(Constants.LAND_BATTLE_ROUNDS, 6);

    assertEquals(
        6,
        BattleRoundResolver.resolveGroundBattleRounds(
            new Territory("Plain", gameData), List.of(), gameData));
  }

  @Test
  void smallFrontGroundBattleReducesGlobalLandRoundsByOne() {
    final GameData gameData = smallFrontGameData();
    gameData.getProperties().set(Constants.LAND_BATTLE_ROUNDS, 6);

    assertEquals(
        5,
        BattleRoundResolver.resolveGroundBattleRounds(
            new Territory("Plain", gameData), List.of(), gameData));
  }

  @Test
  void waterBattleRetainsGlobalSeaRoundsOutsideSmallFront() {
    final GameData gameData = new GameData();
    gameData.getProperties().set(Constants.SEA_BATTLE_ROUNDS, 5);
    final TerritoryEffect terrain = effect(gameData, 2, null);

    assertEquals(
        5,
        BattleRoundResolver.resolveGroundBattleRounds(
            new Territory("Sea", true, gameData), List.of(terrain), gameData));
  }

  @Test
  void smallFrontWaterBattleReducesGlobalSeaRoundsByOne() {
    final GameData gameData = smallFrontGameData();
    gameData.getProperties().set(Constants.SEA_BATTLE_ROUNDS, 5);

    assertEquals(
        4,
        BattleRoundResolver.resolveGroundBattleRounds(
            new Territory("Sea", true, gameData), List.of(), gameData));
  }

  @Test
  void smallFrontUsesShortestFiniteGroundLimitThenReducesItByOne() {
    final GameData gameData = smallFrontGameData();

    assertEquals(
        1,
        BattleRoundResolver.resolveGroundBattleRounds(
            new Territory("Mountain City", gameData),
            List.of(
                effect(gameData, -1, null), effect(gameData, 4, null), effect(gameData, 2, null)),
            gameData));
  }

  @Test
  void smallFrontOneRoundLimitIsNotReducedBelowOne() {
    final GameData gameData = smallFrontGameData();

    assertEquals(
        1,
        BattleRoundResolver.resolveGroundBattleRounds(
            new Territory("Forest", gameData), List.of(effect(gameData, 1, null)), gameData));
  }

  @Test
  void smallFrontUnlimitedGroundEffectsRemainUnlimited() {
    final GameData gameData = smallFrontGameData();

    assertEquals(
        -1,
        BattleRoundResolver.resolveGroundBattleRounds(
            new Territory("Fortress", gameData),
            List.of(effect(gameData, -1, null), effect(gameData, -1, null)),
            gameData));
  }

  @Test
  void smallFrontAirBattleUsesReducedIndependentTerrainLimit() {
    final GameData gameData = smallFrontGameData();
    gameData.getProperties().set(Constants.AIR_BATTLE_ROUNDS, 1);

    assertEquals(
        2, BattleRoundResolver.resolveAirBattleRounds(List.of(effect(gameData, 1, 3)), gameData));
  }

  @Test
  void attachmentPropertiesAcceptPositiveOrUnlimitedRounds() throws Exception {
    final GameData gameData = new GameData();
    final TerritoryEffect effect = new TerritoryEffect("Mountain", gameData);
    final TerritoryEffectAttachment attachment =
        new TerritoryEffectAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, effect, gameData);
    effect.addAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, attachment);

    attachment
        .getPropertyOrEmpty(TerritoryEffectAttachment.MAX_GROUND_BATTLE_ROUNDS)
        .orElseThrow()
        .setValue("4");
    attachment
        .getPropertyOrEmpty(TerritoryEffectAttachment.MAX_AIR_BATTLE_ROUNDS)
        .orElseThrow()
        .setValue("-1");

    assertEquals(OptionalInt.of(4), attachment.getMaxGroundBattleRounds());
    assertEquals(OptionalInt.of(-1), attachment.getMaxAirBattleRounds());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            attachment
                .getPropertyOrEmpty(TerritoryEffectAttachment.MAX_GROUND_BATTLE_ROUNDS)
                .orElseThrow()
                .setValue("0"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            attachment
                .getPropertyOrEmpty(TerritoryEffectAttachment.MAX_AIR_BATTLE_ROUNDS)
                .orElseThrow()
                .setValue("-2"));
  }

  @Test
  void mustFightBattleKeepsTerrainGroundLimitOutsideSmallFront() {
    final GameData gameData = TestMapGameData.TWW.getGameData();
    final Territory sicily = GameDataTestUtil.territory("Sicily", gameData);
    final TerritoryEffect terrain = TerritoryEffectHelper.getEffects(sicily).iterator().next();
    TerritoryEffectAttachment.get(terrain).setMaxGroundBattleRounds(2);

    final MustFightBattle battle =
        new MustFightBattle(sicily, GameDataTestUtil.usa(gameData), gameData, new BattleTracker());

    assertEquals(2, battle.getStatus().getMaxRounds());
    assertFalse(battle.getStatus().isLastRound());
  }

  @Test
  void airBattleKeepsTerrainAirLimitOutsideSmallFront() {
    final GameData gameData = TestMapGameData.TWW.getGameData();
    final Territory sicily = GameDataTestUtil.territory("Sicily", gameData);
    final TerritoryEffect terrain = TerritoryEffectHelper.getEffects(sicily).iterator().next();
    TerritoryEffectAttachment.get(terrain).setMaxAirBattleRounds(3);

    final AirBattle battle =
        new AirBattle(
            sicily,
            IBattle.BattleType.AIR_BATTLE,
            gameData,
            GameDataTestUtil.usa(gameData),
            new BattleTracker());

    battle.round = 2;
    assertFalse(battle.shouldEndBattleDueToMaxRounds());
    battle.round = 3;
    assertTrue(battle.shouldEndBattleDueToMaxRounds());
  }

  private static GameData smallFrontGameData() {
    final GameData gameData = new GameData();
    gameData.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);
    return gameData;
  }

  private static TerritoryEffect effect(
      final GameData gameData, final Integer groundRounds, final Integer airRounds) {
    final TerritoryEffect effect =
        new TerritoryEffect(
            "effect-" + groundRounds + "-" + airRounds + "-" + System.identityHashCode(gameData),
            gameData);
    final TerritoryEffectAttachment attachment =
        new TerritoryEffectAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, effect, gameData);
    if (groundRounds != null) {
      attachment.setMaxGroundBattleRounds(groundRounds);
    }
    if (airRounds != null) {
      attachment.setMaxAirBattleRounds(airRounds);
    }
    effect.addAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, attachment);
    return effect;
  }
}
