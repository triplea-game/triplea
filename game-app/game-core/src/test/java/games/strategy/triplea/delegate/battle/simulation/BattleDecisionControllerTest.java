package games.strategy.triplea.delegate.battle.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BattleDecisionControllerTest {
  @Test
  void casualtyDecisionExposesMaskAndConsumesSelectedUnits() {
    final GameData gameData = new GameData();
    final GamePlayer player = new GamePlayer("defender", gameData);
    gameData.getPlayerList().addPlayerId(player);
    final UnitType infantry = addUnitType(gameData, "infantry", 1);
    final UnitType artillery = addUnitType(gameData, "artillery", 1);
    final Unit first = infantry.create(player);
    final Unit second = artillery.create(player);
    final CasualtyList defaults = new CasualtyList(List.of(first), List.of());
    final BattleDecisionController controller = new BattleDecisionController();

    assertThrows(
        BattleDecisionRequiredException.class,
        () ->
            controller.requestCasualties(
                List.of(first, second), 1, "choose one", player, defaults, false));

    assertEquals(BattleDecisionType.SELECT_CASUALTIES, controller.observation().type());
    assertEquals(2, controller.observation().candidates().size());
    assertEquals(1, controller.legalActions().size());
    assertEquals("select_casualties", controller.legalActions().getFirst().type());

    final BattleAction action =
        new BattleAction(
            "select_casualties",
            Map.of("damagedUnitIds", "", "killedUnitIds", second.getId().toString()));
    assertTrue(controller.isLegalAction(action));
    controller.submit(action);

    final CasualtyDetails result =
        controller.requestCasualties(
            List.of(first, second), 1, "choose one", player, defaults, false);

    assertEquals(List.of(second), result.getKilled());
    assertEquals(List.of(), result.getDamaged());
    assertEquals(BattleDecisionType.NONE, controller.observation().type());
  }

  @Test
  void retreatDecisionOffersContinueAndSortedDestinations() {
    final GameData gameData = new GameData();
    final GamePlayer player = new GamePlayer("attacker", gameData);
    gameData.getPlayerList().addPlayerId(player);
    final Territory alpha = new Territory("Alpha", gameData);
    final Territory bravo = new Territory("Bravo", gameData);
    gameData.getMap().addTerritory(alpha);
    gameData.getMap().addTerritory(bravo);
    final BattleDecisionController controller = new BattleDecisionController();

    assertThrows(
        BattleDecisionRequiredException.class,
        () -> controller.requestRetreat(player, false, List.of(bravo, alpha), "retreat?"));

    assertEquals(BattleDecisionType.RETREAT, controller.observation().type());
    assertEquals(List.of("Alpha", "Bravo"), controller.observation().territories());
    assertEquals(
        List.of(
            new BattleAction("continue", Map.of()),
            new BattleAction("retreat", Map.of("territory", "Alpha")),
            new BattleAction("retreat", Map.of("territory", "Bravo"))),
        controller.legalActions());

    controller.submit(new BattleAction("retreat", Map.of("territory", "Bravo")));
    final Optional<Territory> selected =
        controller.requestRetreat(player, false, List.of(bravo, alpha), "retreat?");

    assertEquals(Optional.of(bravo), selected);
  }

  @Test
  void submergeDecisionUsesSeparateActionType() {
    final GameData gameData = new GameData();
    final GamePlayer player = new GamePlayer("defender", gameData);
    gameData.getPlayerList().addPlayerId(player);
    final Territory sea = new Territory("Sea", true, gameData);
    gameData.getMap().addTerritory(sea);
    final BattleDecisionController controller = new BattleDecisionController();

    assertThrows(
        BattleDecisionRequiredException.class,
        () -> controller.requestRetreat(player, true, List.of(sea), "submerge?"));

    assertEquals(BattleDecisionType.SUBMERGE, controller.observation().type());
    final BattleAction action = new BattleAction("submerge", Map.of("territory", "Sea"));
    assertTrue(controller.isLegalAction(action));
    controller.submit(action);
    assertEquals(
        Optional.of(sea), controller.requestRetreat(player, true, List.of(sea), "submerge?"));
  }

  private static UnitType addUnitType(
      final GameData gameData, final String name, final int hitPoints) {
    final UnitType type = new UnitType(name, gameData);
    final UnitAttachment attachment =
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, type, gameData);
    attachment.setHitPoints(hitPoints);
    type.addAttachment(Constants.UNIT_ATTACHMENT_NAME, attachment);
    gameData.getUnitTypeList().addUnitType(type);
    return type;
  }
}
