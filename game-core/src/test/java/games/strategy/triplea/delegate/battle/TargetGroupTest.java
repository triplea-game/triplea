package games.strategy.triplea.delegate.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.AbstractDelegateTestCase;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class TargetGroupTest extends AbstractDelegateTestCase {

  final GameData twwGameData = TestMapGameData.TWW.getGameData();

  @Test
  void testNewTargetGroupsWithEmptyUnitsAndEnemyUnits() {
    final List<TargetGroup> result = TargetGroup.newTargetGroups(List.of(), List.of());
    assertEquals(0, result.size());
  }

  @Test
  void testNewTargetGroupsWithEmptyUnits() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    final List<TargetGroup> result = TargetGroup.newTargetGroups(List.of(), enemyUnits);
    assertEquals(0, result.size());
  }

  @Test
  void testNewTargetGroupsWithEmptyEnemyUnits() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, List.of());
    assertEquals(0, result.size());
  }

  @Test
  void testNewTargetGroupsForOneUnitAndOneEnemyUnit() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishInfantry(twwGameData).create(1, britain);
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertEquals(1, result.size());
  }

  @Test
  void testNewTargetGroupsForOneUnitAndMultipleEnemyUnits() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishInfantry(twwGameData).create(1, britain);
    enemyUnits.addAll(GameDataTestUtil.britishArtillery(twwGameData).create(2, britain));
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertEquals(1, result.size());
  }

  @Test
  void testNewTargetGroupsForMultipleUnitsAndOneEnemyUnit() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    units.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(2, germany));
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishInfantry(twwGameData).create(1, britain);
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertEquals(1, result.size());
  }

  @Test
  void testNewTargetGroupsForMultipleUnitsAndMultipleEnemyUnits() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanInfantry(twwGameData).create(1, germany);
    units.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(2, germany));
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishInfantry(twwGameData).create(1, britain);
    enemyUnits.addAll(GameDataTestUtil.britishArtillery(twwGameData).create(2, britain));
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertEquals(1, result.size());
  }

  @Test
  void testNewTargetGroupsForMultipleGroups() {
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final List<Unit> units = GameDataTestUtil.germanSubmarine(twwGameData).create(1, germany);
    units.addAll(GameDataTestUtil.germanFighter(twwGameData).create(2, germany));
    final GamePlayer britain = GameDataTestUtil.britain(twwGameData);
    final List<Unit> enemyUnits = GameDataTestUtil.britishSubmarine(twwGameData).create(1, britain);
    enemyUnits.addAll(GameDataTestUtil.britishFighter(twwGameData).create(2, britain));
    final List<TargetGroup> result = TargetGroup.newTargetGroups(units, enemyUnits);
    assertEquals(2, result.size());
  }
}
