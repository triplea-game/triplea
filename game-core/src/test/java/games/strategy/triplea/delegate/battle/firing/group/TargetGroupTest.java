package games.strategy.triplea.delegate.battle.firing.group;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractDelegateTestCase;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TargetGroupTest extends AbstractDelegateTestCase {

  final GameData twwGameData = TestMapGameData.TWW.getGameData();

  @Test
  @DisplayName("Verify no target groups if no units and no enemy units")
  void testNewTargetGroupsWithEmptyUnitsAndEnemyUnits() {
    final List<TargetGroup> result = TargetGroup.newTargetGroups(List.of(), List.of());
    assertThat(result, empty());
  }

  @Test
  @DisplayName("Verify no target groups if no units but some enemy units")
  void testNewTargetGroupsWithEmptyUnits() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    final List<TargetGroup> result = TargetGroup.newTargetGroups(List.of(), enemyUnits);
    assertThat(result, empty());
  }

  @Test
  @DisplayName("Verify no target groups if some units but no enemy units")
  void testNewTargetGroupsWithEmptyEnemyUnits() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, List.of());
    assertThat(result, empty());
  }

  @Test
  @DisplayName("Verify 1 target group if an infantry attacks an infantry")
  void testNewTargetGroupsForOneUnitAndOneEnemyUnit() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishInfantry(twwGameData).create(1, britain);
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertThat(result, hasSize(1));
  }

  @Test
  @DisplayName("Verify 1 target group if an infantry attacks infantry and artilleries")
  void testNewTargetGroupsForOneUnitAndMultipleEnemyUnits() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishInfantry(twwGameData).create(1, britain);
    enemyUnits.addAll(GameDataTestUtil.britishArtillery(twwGameData).create(2, britain));
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertThat(result, hasSize(1));
  }

  @Test
  @DisplayName("Verify 1 target group if an infantry and artilleries attack an infantry")
  void testNewTargetGroupsForMultipleUnitsAndOneEnemyUnit() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    units.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(2, germany));
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishInfantry(twwGameData).create(1, britain);
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertThat(result, hasSize(1));
  }

  @Test
  @DisplayName(
      "Verify 1 target group if an infantry and artilleries attack infantry and artilleries")
  void testNewTargetGroupsForMultipleUnitsAndMultipleEnemyUnits() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    units.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(2, germany));
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishInfantry(twwGameData).create(1, britain);
    enemyUnits.addAll(GameDataTestUtil.britishArtillery(twwGameData).create(2, britain));
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertThat(result, hasSize(1));
  }

  @Test
  @DisplayName(
      "Verify 2 target groups if sub and fighters attack sub and fighters as subs can only "
          + "target subs")
  void testNewTargetGroupsForMultipleGroups() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanSubmarine(twwGameData).create(1, germany);
    units.addAll(GameDataTestUtil.germanFighter(twwGameData).create(2, germany));
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishSubmarine(twwGameData).create(1, britain);
    enemyUnits.addAll(GameDataTestUtil.britishFighter(twwGameData).create(2, britain));
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertThat(UnitAttachment.get(units.get(0).getType()).getCanNotTarget(), is(not(empty())));
    assertThat(result, hasSize(2));
  }
}
