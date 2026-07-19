package games.strategy.engine.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.player.Player;
import games.strategy.net.LocalNoOpMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.battle.ScrambleLogic;
import games.strategy.triplea.delegate.scoring.SmallFrontScoringService;
import games.strategy.triplea.delegate.supply.SupplyAwareMoveDelegate;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.game.server.HeadlessLaunchAction;

/** Runs an all-Small-Front-AI game and checks both sides actually fight. */
class SmallFrontAiGameTest {
  private static final Path MAP_XML =
      Path.of("src", "test", "resources", "map-xmls", "Small_Front_Meuse.xml");

  @BeforeAll
  static void setUp() throws IOException {
    GameTestUtils.setUp();
  }

  /**
   * Asserts the AI fights, not that it wins. The bar is the Pro AI before it had unit values, which
   * held its seven starting territories for eight rounds and never moved. Who ends up ahead is a
   * balance question and belongs nowhere near this test: both sides here run the same policy, so a
   * good defender pushing the attacker back is a legitimate outcome.
   */
  @Test
  void bothSidesFightRatherThanStandingStill() {
    final GameData data = loadMap();
    final Map<String, String> ownersAtStart = owners(data);

    final ServerGame game = allSmallFrontAi(data);
    game.setStopGameOnDelegateExecutionStop(true);
    while (!game.isGameOver() && data.getSequence().getRound() <= 12) {
      game.runNextStep();
    }

    assertThat(game.isGameOver()).isTrue();
    final Map<String, String> ownersAtEnd = owners(data);
    final List<String> changedHands =
        ownersAtStart.keySet().stream()
            .filter(name -> !ownersAtStart.get(name).equals(ownersAtEnd.get(name)))
            .sorted()
            .toList();
    assertThat(changedHands).as("no territory changed hands in a whole game").isNotEmpty();
    assertThat(named(SmallFrontScoringService.score(data)))
        .containsOnlyKeys("Germans", "Americans");
  }

  @Test
  void terrainStackCapacityChecksDestinationButAllowsFullTransitTerritories() {
    final GameData data = loadMap();

    final Territory vielsalm = data.getMap().getTerritoryOrThrow("Vielsalm");
    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Unit americanInfantry = unitIn(vielsalm, "americanInfantry", "Americans");
    assertThat(
            SupplyAwareMoveDelegate.validateStackCapacity(
                new MoveDescription(List.of(americanInfantry), new Route(vielsalm, stVith)),
                americanInfantry.getOwner()))
        .isEmpty();

    final Territory blankenheim = data.getMap().getTerritoryOrThrow("Blankenheim");
    final Territory losheimGap = data.getMap().getTerritoryOrThrow("Losheim Gap");
    final Territory prum = data.getMap().getTerritoryOrThrow("Prum");
    // Blankenheim's whole German land force (2 infantry + 1 armour, stack cost 4) overflows
    // Losheim Gap's remaining Forest capacity, but Prum (Open) has room for it.
    final List<Unit> assaultForce = landUnitsOf(blankenheim, "Germans");
    final GamePlayer germanPlayer = assaultForce.get(0).getOwner();

    assertThat(
            SupplyAwareMoveDelegate.validateStackCapacity(
                new MoveDescription(assaultForce, new Route(blankenheim, losheimGap, prum)),
                germanPlayer))
        .as("a full intermediate territory must not block transit")
        .isEmpty();

    final Optional<String> overflow =
        SupplyAwareMoveDelegate.validateStackCapacity(
            new MoveDescription(assaultForce, new Route(blankenheim, losheimGap)), germanPlayer);
    assertThat(overflow).isPresent();
    assertThat(overflow.orElseThrow())
        .contains(SupplyAwareMoveDelegate.STACK_CAPACITY_EXCEEDED)
        .contains("Losheim Gap");
  }

  @Test
  void centralSupplyGraphUsesIndirectSecondAxis() {
    final GameData data = loadMap();
    final Territory laRoche = data.getMap().getTerritoryOrThrow("La Roche");
    final Territory marche = data.getMap().getTerritoryOrThrow("Marche");
    final Territory hotton = data.getMap().getTerritoryOrThrow("Hotton");
    final Territory vielsalm = data.getMap().getTerritoryOrThrow("Vielsalm");
    final Territory durbuy = data.getMap().getTerritoryOrThrow("Durbuy");
    final Territory libramont = data.getMap().getTerritoryOrThrow("Libramont");
    final Territory neufchateau = data.getMap().getTerritoryOrThrow("Neufchateau");

    assertThat(SupplyNetworkResolver.getRoadNeighbors(laRoche, data)).contains(marche);
    assertThat(SupplyNetworkResolver.getRoadNeighbors(hotton, data)).doesNotContain(marche);
    assertThat(SupplyNetworkResolver.getRoadNeighbors(vielsalm, data)).doesNotContain(durbuy);
    assertThat(SupplyNetworkResolver.getRoadNeighbors(libramont, data)).doesNotContain(neufchateau);
  }

  @Test
  void mechanizedArmourSupportEncodesCombinedArmsRule() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final UnitAttachment mechanized =
        UnitAttachment.get(
            data.getUnitTypeList().getUnitTypeOrThrow("mechanized"), "unitAttachment");
    assertThat(mechanized.getAttack(germans)).isEqualTo(1);

    final UnitSupportAttachment support =
        UnitSupportAttachment.get(data.getUnitTypeList()).stream()
            .filter(
                rule ->
                    rule.getAttachedTo() == data.getUnitTypeList().getUnitTypeOrThrow("mechanized"))
            .filter(
                rule ->
                    rule.getBonusType() != null
                        && rule.getBonusType().getName().equals("mechanizedArmour"))
            .findFirst()
            .orElseThrow();
    assertThat(support.getUnitType())
        .containsExactly(data.getUnitTypeList().getUnitTypeOrThrow("armour"));
    assertThat(support.getBonus()).isEqualTo(1);
    assertThat(support.getNumber()).isEqualTo(1);
    assertThat(support.getOffence()).isTrue();
    assertThat(support.getDefence()).isFalse();
    assertThat(support.getStrength()).isTrue();
    assertThat(support.getRoll()).isFalse();
    assertThat(support.getPlayers()).containsExactly(germans);
    assertThat(support.getBonusType()).isNotNull();
    assertThat(support.getBonusType().getCount()).isEqualTo(1);
  }

  @Test
  void nativeAirbasesProvideRearAreaScrambleAndRoundScopedAirControl() {
    final GameData data = loadMap();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final GamePlayer americans = data.getPlayerList().getPlayerId("Americans");

    final Territory hotton = data.getMap().getTerritoryOrThrow("Hotton");
    final Territory ciney = data.getMap().getTerritoryOrThrow("Ciney");
    final Unit americanFighter = unitIn(ciney, "fighter", "Americans");
    assertThat(new ScrambleLogic(data, germans, hotton).getUnitsThatCanScramble())
        .contains(americanFighter);

    final Territory stVith = data.getMap().getTerritoryOrThrow("St. Vith");
    final Territory prum = data.getMap().getTerritoryOrThrow("Prum");
    final Unit germanFighter = unitIn(prum, "fighter", "Germans");
    assertThat(new ScrambleLogic(data, americans, stVith).getUnitsThatCanScramble())
        .contains(germanFighter);

    assertThat(AirControlTracker.isPersistent(data)).isFalse();
  }

  private static GameData loadMap() {
    return GameParser.parse(MAP_XML, false)
        .orElseThrow(() -> new AssertionError("map did not parse"));
  }

  private static Unit unitIn(final Territory territory, final String unitType, final String owner) {
    return territory.getUnitCollection().getUnits().stream()
        .filter(unit -> unit.getType().getName().equals(unitType))
        .filter(unit -> unit.getOwner().getName().equals(owner))
        .findFirst()
        .orElseThrow();
  }

  private static List<Unit> landUnitsOf(final Territory territory, final String owner) {
    return territory.getUnitCollection().getUnits().stream()
        .filter(unit -> unit.getOwner().getName().equals(owner))
        .filter(unit -> !unit.getUnitAttachment().isAir())
        .toList();
  }

  private static Map<String, String> owners(final GameData data) {
    final Map<String, String> owners = new HashMap<>();
    data.getMap().getTerritories().forEach(t -> owners.put(t.getName(), t.getOwner().getName()));
    return owners;
  }

  private static ServerGame allSmallFrontAi(final GameData data) {
    final Map<String, PlayerTypes.Type> playerTypes = new HashMap<>();
    for (final GamePlayer player : data.getPlayerList().getPlayers()) {
      playerTypes.put(player.getName(), PlayerTypes.SMALL_FRONT_AI);
    }
    final Set<Player> gamePlayers = data.getGameLoader().newPlayers(playerTypes);
    final HeadlessLaunchAction launchAction =
        new HeadlessLaunchAction(mock(HeadlessGameServer.class));
    final ServerGame game =
        new ServerGame(
            data,
            gamePlayers,
            new HashMap<>(),
            new Messengers(new LocalNoOpMessenger()),
            ClientNetworkBridge.NO_OP_SENDER,
            launchAction);
    game.setDelegateAutosavesEnabled(false);
    data.getGameLoader().startGame(game, gamePlayers, launchAction, null);
    return game;
  }

  private static Map<String, Integer> named(final Map<GamePlayer, Integer> scores) {
    final Map<String, Integer> byName = new HashMap<>();
    scores.forEach((player, score) -> byName.put(player.getName(), score));
    return byName;
  }
}
