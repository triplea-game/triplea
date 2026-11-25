package games.strategy.triplea.ai.pro;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.chinese;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProPurchaseOptionMap;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PlaceDelegate;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

public class ProPurchaseAiTest {
  private ProAi proAi;
  private GameData gameData;
  private PurchaseDelegate purchaseDelegate;
  private GamePlayer gamePlayer;

  @Test
  public void testShouldSaveUpForAFleet() {
    setupShouldSaveUpForAFleetTest();

    /* Issue 11093: (https://github.com/triplea-game/triplea/issues/11093)
      This issue happens during execution of ProPurchaseAi.shouldSaveUpForAFleet when the IntegerMap<Resource> maxShipCost
      is not reassigned while looping over seaDefenseOptions and an UnsupportedOperationException is thrown later when
      multiplyAllValuesBy is called by the map. This is because the underlying implementation used by maxShipCost is the
      immutable one from the original initialization call to IntegerMap.of(), since it was not reassigned in the loop. To
      remediate this issue the original initialization of the maxShipCost IntegerMap should be done by calling the
      IntegerMap constructor which will initialize the map to be a mutable LinkedHashMap implementation. This way the
      underlying implementation will always be a mutable Map implementation, whether maxShipCost is reassigned or not.
    */
    Assertions.assertDoesNotThrow(
        () -> proAi.purchase(false, 20, purchaseDelegate, gameData, gamePlayer));
  }

  @Test
  void slotsAreConstrainedWhenPusOutrunThem() {
    assertThat(ProPurchaseAi.isSlotConstrained(80, 10, 5)).isTrue();
  }

  @Test
  void slotsAreNotConstrainedWhenTheyCanSpendThePus() {
    assertThat(ProPurchaseAi.isSlotConstrained(39, 10, 5)).isFalse();
  }

  @Test
  void noFreeSlotsConstrainsAnyAffordableSpend() {
    assertThat(ProPurchaseAi.isSlotConstrained(6, 0, 5)).isTrue();
  }

  @Test
  void fewerPusThanOneUnitIsNotConstrained() {
    assertThat(ProPurchaseAi.isSlotConstrained(2, 0, 5)).isFalse();
  }

  @Test
  void unlimitedSlotsAreNeverConstrained() {
    assertThat(ProPurchaseAi.isSlotConstrained(500, 3L * Integer.MAX_VALUE, 5)).isFalse();
  }

  /// Pact of Steel 2 Chinese gain `placementAnySeaZone` once an ally holds Japan, letting them
  /// place ships next to any land they own; Kwangtung has no factory.
  @Test
  void placePutsShipsNextToLandWithoutFactoryUnderPlacementAnySeaZone() throws Exception {
    final GameData pos2GameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
    final GamePlayer chinese = checkNotNull(chinese(pos2GameData));
    chinese.getRulesAttachment().getPropertyOrThrow("placementAnySeaZone").setValue(true);
    final Territory kwangtung = territory("Kwangtung", pos2GameData);
    removeFrom(kwangtung, kwangtung.getUnits());
    pos2GameData.performChange(ChangeFactory.changeOwner(kwangtung, chinese));
    final List<Unit> transports = transport(pos2GameData).create(2, chinese);
    pos2GameData.performChange(ChangeFactory.addUnits(chinese, transports));
    final IDelegateBridge bridge = newDelegateBridge(chinese);
    advanceToStep(bridge, "chinesePlace");
    final PlaceDelegate placeDelegate = new PlaceDelegate();
    placeDelegate.initialize("place");
    placeDelegate.setDelegateBridgeAndPlayer(bridge);
    placeDelegate.start();
    final PlayerBridge playerBridgeMock = Mockito.mock(PlayerBridge.class);
    when(playerBridgeMock.getGameData()).thenReturn(pos2GameData);
    ClientSetting.setPreferences(new MemoryPreferences());
    ClientSetting.aiMovePauseDuration.setValue(0);
    final ProAi chineseAi = new ProAi("Test Name", "Test Player Label");
    chineseAi.initialize(playerBridgeMock, chinese);

    chineseAi.place(false, placeDelegate, pos2GameData, chinese);

    assertThat(territory("59 Sea Zone", pos2GameData).getUnits()).containsAll(transports);
  }

  @Test
  void airIsNotAtCapWhenThereIsNoAir() {
    assertThat(ProPurchaseAi.isAirAtCap(0, 100)).isFalse();
  }

  @Test
  void airIsNotAtCapWhenThereIsNoForceAtAll() {
    assertThat(ProPurchaseAi.isAirAtCap(0, 0)).isFalse();
  }

  @Test
  void airIsNotAtCapBelowHalfOfForceValue() {
    assertThat(ProPurchaseAi.isAirAtCap(49, 100)).isFalse();
  }

  @Test
  void airIsAtCapAtExactlyHalfOfForceValue() {
    assertThat(ProPurchaseAi.isAirAtCap(50, 100)).isTrue();
  }

  @Test
  void airIsAtCapAboveHalfOfForceValue() {
    assertThat(ProPurchaseAi.isAirAtCap(60, 100)).isTrue();
  }

  /// Italy owns far more air than the rest of its force is worth, so Hard Beta's leftover PUs must
  /// go to something else. Without the cap the fallbacks prefer air 10 times over any other unit.
  @Test
  void betaDoesNotSpendLeftoverPusOnAirWhenAirIsAlreadyHalfTheForce() {
    setupShouldSaveUpForAFleetTest(new ProAi("Test Name", "Test Player Label", true));
    final Territory italy = territory("Italy", gameData);
    final List<Unit> fighters =
        gameData.getUnitTypeList().getUnitTypeOrThrow("fighter").create(20, gamePlayer);
    gameData.performChange(ChangeFactory.addUnits(italy, fighters));

    try {
      proAi.purchase(false, 16, purchaseDelegate, gameData, gamePlayer);
    } finally {
      proAi.stopGame();
    }

    assertThat(gamePlayer.getUnits()).isNotEmpty().noneMatch(Matches.unitIsAir());
  }

  @Test
  void leftoverAirKeepsFightersAndDropsBombers() {
    final GameData ww2v3 = TestMapGameData.WW2V3_1941.getGameData();
    final GamePlayer germans = ww2v3.getPlayerList().getPlayerId("Germans");
    final List<ProPurchaseOption> airOptions =
        new ProPurchaseOptionMap(germans, ww2v3).getAirOptions();
    assertThat(airOptions).extracting(o -> o.getUnitType().getName()).contains("fighter", "bomber");

    final List<ProPurchaseOption> leftoverAir = ProPurchaseAi.withoutBomberLikeAir(airOptions);

    assertThat(leftoverAir).extracting(o -> o.getUnitType().getName()).contains("fighter");
    assertThat(leftoverAir).extracting(o -> o.getUnitType().getName()).doesNotContain("bomber");
  }

  /// An overwhelming British stack next to Germany that no purchase can hold sends Beta's PUs to
  /// fighters, which can leave before the capital falls.
  @Test
  void betaBuysFightersWhenCapitalCannotBeHeld() {
    final GameData ww2v3 = TestMapGameData.WW2V3_1941.getGameData();
    final GamePlayer germans = germans(ww2v3);
    final GamePlayer british = british(ww2v3);
    final Territory northwestEurope = territory("Northwestern Europe", ww2v3);
    ww2v3.performChange(ChangeFactory.addUnits(northwestEurope, armour(ww2v3).create(60, british)));
    final PurchaseDelegate purchase = (PurchaseDelegate) ww2v3.getDelegate("purchase");
    purchase.setDelegateBridgeAndPlayer(newDelegateBridge(germans));
    final PlayerBridge playerBridge = Mockito.mock(PlayerBridge.class);
    when(playerBridge.getGameData()).thenReturn(ww2v3);
    ClientSetting.setPreferences(new MemoryPreferences());
    final ProAi beta = new ProAi("Test Name", "Test Player Label", true);
    beta.initialize(playerBridge, germans);

    try {
      beta.purchase(false, 40, purchase, ww2v3, germans);
    } finally {
      // The battle calculator is shared by every ProAi; leave no game data behind for other tests.
      beta.stopGame();
    }

    assertThat(germans.getUnits()).isNotEmpty().allMatch(Matches.unitIsAir());
  }

  @Test
  void waitingFightersFillOnlyTheFreeDeckAndSkipBombers() {
    final GameData ww2v3 = TestMapGameData.WW2V3_1941.getGameData();
    final GamePlayer germans = germans(ww2v3);
    final Territory germany = territory("Germany", ww2v3);
    removeFrom(germany, germany.getUnits());
    final List<Unit> fighters = fighter(ww2v3).create(3, germans);
    ww2v3.performChange(ChangeFactory.addUnits(germany, fighters));
    ww2v3.performChange(
        ChangeFactory.addUnits(germany, GameDataTestUtil.bomber(ww2v3).create(2, germans)));
    final Territory seaZone = territory("5 Sea Zone", ww2v3);

    assertThat(ProPurchaseAi.findWaitingFighters(ww2v3, germans, seaZone, 2)).hasSize(2);
    assertThat(ProPurchaseAi.findWaitingFighters(ww2v3, germans, seaZone, 100))
        .containsAll(fighters)
        .extracting(u -> u.getType().getName())
        .containsOnly("fighter");
    assertThat(ProPurchaseAi.findWaitingFighters(ww2v3, germans, seaZone, 0)).isEmpty();
  }

  @Test
  void stockHardBanksPusWhileSavingUpForAFleet() {
    final GamePlayer british = purchaseForBritainBlockadedByGermanFleet(false);

    assertThat(british.getResources().getQuantity(Constants.PUS)).isGreaterThan(50);
  }

  @Test
  void betaSpendsPusWhileSavingUpForAFleet() {
    final GamePlayer british = purchaseForBritainBlockadedByGermanFleet(true);

    assertThat(british.getResources().getQuantity(Constants.PUS)).isLessThan(50);
  }

  /// German battleships ring the United Kingdom and Eire is British, so Britain can reach the enemy
  /// only by sea, can't defend new ships and saves up for a fleet. It has 100 PUs, more than the
  /// United Kingdom's 8 slots can spend.
  private static GamePlayer purchaseForBritainBlockadedByGermanFleet(final boolean beta) {
    final GameData ww2v3 = TestMapGameData.WW2V3_1941.getGameData();
    final GamePlayer british = british(ww2v3);
    for (final String seaZone :
        List.of("2 Sea Zone", "3 Sea Zone", "6 Sea Zone", "7 Sea Zone", "8 Sea Zone")) {
      ww2v3.performChange(
          ChangeFactory.addUnits(
              territory(seaZone, ww2v3), battleship(ww2v3).create(4, germans(ww2v3))));
    }
    ww2v3.performChange(ChangeFactory.changeOwner(territory("Eire", ww2v3), british));
    final Resource pus = ww2v3.getResourceList().getResourceOrThrow(Constants.PUS);
    ww2v3.performChange(ChangeFactory.changeResourcesChange(british, pus, 57));
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "britishPurchase");
    final PurchaseDelegate purchase = (PurchaseDelegate) ww2v3.getDelegate("purchase");
    purchase.setDelegateBridgeAndPlayer(bridge);
    final PlayerBridge playerBridge = Mockito.mock(PlayerBridge.class);
    when(playerBridge.getGameData()).thenReturn(ww2v3);
    ClientSetting.setPreferences(new MemoryPreferences());
    final ProAi ai = new ProAi("Test Name", "Test Player Label", beta);
    ai.initialize(playerBridge, british);

    try {
      ai.purchase(false, 100, purchase, ww2v3, british);
    } finally {
      ai.stopGame();
    }
    return british;
  }

  private void setupShouldSaveUpForAFleetTest() {
    setupShouldSaveUpForAFleetTest(new ProAi("Test Name", "Test Player Label"));
  }

  private void setupShouldSaveUpForAFleetTest(final ProAi ai) {
    proAi = ai;
    gameData = TestMapGameData.VICTORY_TEST_SHOULD_SAVE_UP_FOR_A_FLEET.getGameData();
    gamePlayer = gameData.getPlayerList().getPlayerId("Italians");
    final IDelegateBridge testBridge = newDelegateBridge(gamePlayer);
    purchaseDelegate = (PurchaseDelegate) gameData.getDelegate("purchase");
    final PlayerBridge playerBridgeMock = Mockito.mock(PlayerBridge.class);

    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    proAi.initialize(playerBridgeMock, gamePlayer);
    ClientSetting.setPreferences(new MemoryPreferences());

    when(playerBridgeMock.getGameData()).thenReturn(gameData);
  }
}
