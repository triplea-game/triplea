package games.strategy.engine.data.changefactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

class OwnerChangeTest {

  @Test
  void testperform_staticUnitIsTakenOver() {
    final GameState gameData = TestMapGameData.REVISED.getGameData();
    final Territory territory = gameData.getMap().getTerritory("Australia");
    final GamePlayer british = gameData.getPlayerList().getPlayerId("British");
    territory.setOwner(british);
    final GamePlayer americans = gameData.getPlayerList().getPlayerId("Americans");
    final UnitCollection uc = territory.getUnitCollection();
    uc.removeAll(uc.getUnits());
    final UnitType factory = GameDataTestUtil.factory(gameData);
    uc.add(factory.createTemp(1, british).get(0));
    final OwnerChange oc = new OwnerChange(territory, americans);

    oc.perform(gameData);

    assertThat(
        "The (non-mobile) factory should be taken over"
            + " when the allied player takes over the territory",
        CollectionUtils.getAny(territory.getUnits()).isOwnedBy(americans),
        is(true));
  }

  @Test
  void testperform_mobileUnitIsNotTakenOver() {
    final GameState gameData = TestMapGameData.REVISED.getGameData();
    final Territory territory = gameData.getMap().getTerritory("Australia");
    final GamePlayer british = gameData.getPlayerList().getPlayerId("British");
    territory.setOwner(british);
    final GamePlayer americans = gameData.getPlayerList().getPlayerId("Americans");
    final UnitCollection uc = territory.getUnitCollection();
    uc.removeAll(uc.getUnits());
    final UnitType aaGun = GameDataTestUtil.aaGun(gameData);
    uc.add(aaGun.createTemp(1, british).get(0));
    final OwnerChange oc = new OwnerChange(territory, americans);

    oc.perform(gameData);

    assertThat(
        "The (mobile) anti air gun should be taken over"
            + " when the allied player takes over the territory",
        CollectionUtils.getAny(territory.getUnits()).isOwnedBy(british),
        is(true));
  }
}
