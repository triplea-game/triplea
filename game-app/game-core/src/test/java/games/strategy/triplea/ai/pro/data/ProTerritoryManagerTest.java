package games.strategy.triplea.ai.pro.data;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProTerritoryManagerTest {

  @Test
  void airUnitIsNotPlannedIntoTerritoryThatBarsItsType() throws Exception {
    final GameData data = TestMapGameData.DELEGATE_TEST.getGameData();
    final GamePlayer british = british(data);
    final Territory eastCanada = data.getMap().getTerritoryOrThrow("East Canada");
    final Territory westCanada = data.getMap().getTerritoryOrThrow("West Canada");
    final Unit plane = fighter(data).create(1, british).get(0);
    addTo(eastCanada, List.of(plane));
    final var marsh =
        TerritoryEffectAttachment.get(
            TerritoryEffectHelper.getEffects(westCanada).iterator().next());
    marsh.getPropertyOrThrow("unitsNotAllowed").setValue(List.of(fighter(data)));

    final ProTerritoryManager manager = planDefense(data, british);

    assertThat(manager.getDefendOptions().getUnitMoveMap().get(plane)).doesNotContain(westCanada);
    assertThat(manager.getDefendOptions().getUnitMoveMap().get(plane)).contains(eastCanada);
  }

  @Test
  void airUnitIsPlannedIntoTerritoryThatBarsOtherTypes() {
    final GameData data = TestMapGameData.DELEGATE_TEST.getGameData();
    final GamePlayer british = british(data);
    final Territory eastCanada = data.getMap().getTerritoryOrThrow("East Canada");
    final Territory westCanada = data.getMap().getTerritoryOrThrow("West Canada");
    final Unit plane = fighter(data).create(1, british).get(0);
    addTo(eastCanada, List.of(plane));

    final ProTerritoryManager manager = planDefense(data, british);

    assertThat(manager.getDefendOptions().getUnitMoveMap().get(plane)).contains(westCanada);
  }

  @Test
  void trainIsNotPlannedAlongTrackThatLacksRail() {
    final GameData tww = TestMapGameData.TWW.getGameData();
    final GamePlayer germans = GameDataTestUtil.germany(tww);
    final Territory berlin = tww.getMap().getTerritoryOrThrow("Berlin");
    final Territory easternGermany = tww.getMap().getTerritoryOrThrow("Eastern Germany");
    final Unit train = GameDataTestUtil.germanTrain(tww).create(1, germans).get(0);
    addTo(berlin, List.of(train));

    final Set<Territory> moves =
        planDefense(tww, germans).getDefendOptions().getUnitMoveMap().get(train);

    assertThat(moves).doesNotContain(easternGermany);
  }

  @Test
  void trainIsPlannedAlongTrackThatHoldsRailAtBothEnds() {
    final GameData tww = TestMapGameData.TWW.getGameData();
    final GamePlayer germans = GameDataTestUtil.germany(tww);
    final Territory berlin = tww.getMap().getTerritoryOrThrow("Berlin");
    final Territory easternGermany = tww.getMap().getTerritoryOrThrow("Eastern Germany");
    final Unit train = GameDataTestUtil.germanTrain(tww).create(1, germans).get(0);
    addTo(berlin, List.of(train));
    addTo(berlin, GameDataTestUtil.germanRail(tww).create(1, germans));
    addTo(easternGermany, GameDataTestUtil.germanRail(tww).create(1, germans));

    final Set<Territory> moves =
        planDefense(tww, germans).getDefendOptions().getUnitMoveMap().get(train);

    assertThat(moves).contains(easternGermany);
  }

  private static ProTerritoryManager planDefense(final GameData data, final GamePlayer player) {
    final ProData proData = new ProData();
    proData.initializeSimulation(null, data, player);
    final ProTerritoryManager manager = new ProTerritoryManager(null, proData);
    manager.populateDefenseOptions(List.of());
    return manager;
  }

  private final GameData transportData = TestMapGameData.WW2V3_1941.getGameData();
  private final Territory baltic = territory("5 Sea Zone", transportData);
  private final Unit transport = transport(transportData).create(germans(transportData));

  private ProTerritoryManager nonCombatDefendOptions() {
    transportData.getSequence().setRoundAndStep(1, "Non Combat Move", germans(transportData));
    addTo(baltic, List.of(transport));
    addTo(
        territory("Germany", transportData),
        infantry(transportData).create(1, germans(transportData)));
    return planDefense(transportData, germans(transportData));
  }

  private boolean plansToLoad(final ProTerritoryManager manager) {
    return manager.getDefendOptions().getTransportList().stream()
        .filter(t -> t.getTransport().equals(transport))
        .anyMatch(t -> !t.getSeaTransportMap().isEmpty());
  }

  @Test
  void transportPlansToLoadAfterCombatWhenNotBarred() {
    assertThat(plansToLoad(nonCombatDefendOptions())).isTrue();
  }

  @Test
  void transportThatFoughtDoesNotPlanToLoadInNonCombat()
      throws MutableProperty.InvalidValueException {
    transport.getProperty(Unit.PropertyName.WAS_IN_COMBAT).orElseThrow().setValue(true);

    assertThat(plansToLoad(nonCombatDefendOptions())).isFalse();
  }
}
