package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.italians;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9524: when a war breaks out (e.g. via politics) units already sitting
 * on what is now an enemy territory must start a battle there or capture the territory if it is
 * undefended. Without {@link BattleDelegate#setUpBattlesForChangedRelationships(BattleTracker,
 * IDelegateBridge)} those units would just sit idle until a future turn.
 */
class SetUpBattlesAfterRelationshipChangeTest extends AbstractClientSettingTestCase {

  private final GameData gameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
  private final GamePlayer italians = italians(gameData);
  private final GamePlayer russians = russians(gameData);
  // Karelia S.S.R. is owned by Russians and is the territory we'll move Italian units into
  // while the two players are at peace.
  private final Territory karelia = territory("Karelia S.S.R.", gameData);

  @Test
  void newWarCreatesBattleWhereUnitsCoexist() {
    setRelationship(italians, russians, "Neutrality");
    addTo(karelia, armour(gameData).create(2, italians));
    final IDelegateBridge bridge = newDelegateBridge(italians);
    advanceToStep(bridge, "italianPolitics");

    setRelationship(italians, russians, "War");
    BattleDelegate.setUpBattlesForChangedRelationships(getBattleTracker(), bridge);

    final IBattle battle = getBattleTracker().getPendingBattle(karelia, IBattle.BattleType.NORMAL);
    assertThat("a battle should be pending in Karelia S.S.R.", battle, notNullValue());
    assertThat(
        "Italian units should be the attackers",
        battle.getAttackingUnits().stream().allMatch(u -> u.getOwner().equals(italians)),
        equalTo(true));
  }

  @Test
  void newWarCapturesUndefendedEnemyTerritory() {
    setRelationship(italians, russians, "Neutrality");
    karelia.getUnitCollection().clear();
    final List<Unit> italianTanks = armour(gameData).create(1, italians);
    addTo(karelia, italianTanks);
    final IDelegateBridge bridge = newDelegateBridge(italians);
    advanceToStep(bridge, "italianPolitics");

    setRelationship(italians, russians, "War");
    BattleDelegate.setUpBattlesForChangedRelationships(getBattleTracker(), bridge);

    assertThat(
        "Karelia S.S.R. should now be Italian-owned after the war declaration",
        karelia.getOwner(),
        equalTo(italians));
    assertThat(
        "no normal battle should remain pending — territory was captured outright",
        getBattleTracker().getPendingBattle(karelia, IBattle.BattleType.NORMAL),
        nullValue());
  }

  private void setRelationship(
      final GamePlayer p1, final GamePlayer p2, final String relationshipName) {
    final RelationshipType target =
        gameData.getRelationshipTypeList().getRelationshipType(relationshipName);
    final RelationshipType current = gameData.getRelationshipTracker().getRelationshipType(p1, p2);
    if (current.equals(target)) {
      return;
    }
    gameData.performChange(ChangeFactory.relationshipChange(p1, p2, current, target));
  }

  private BattleTracker getBattleTracker() {
    return GameDataTestUtil.battleDelegate(gameData).getBattleTracker();
  }
}
