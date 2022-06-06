package org.triplea.ai.flowfield;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.player.IPlayerBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.ui.menubar.DebugMenu;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.ai.flowfield.influence.InfluenceMap;
import org.triplea.ai.flowfield.influence.InfluenceMapBuilder;
import org.triplea.ai.flowfield.influence.TerritoryDebugAction;
import org.triplea.ai.flowfield.neighbors.MapWithNeighbors;
import org.triplea.ai.flowfield.neighbors.NeighborGetter;
import org.triplea.ai.flowfield.odds.LanchesterDebugAction;

public class FlowFieldAi extends AbstractAi {

  @Getter private Collection<InfluenceMap> diffusions = new ArrayList<>();
  private int round = -1;

  public FlowFieldAi(final String name, final PlayerTypes.AiType playerType) {
    super(name, playerType);
  }

  @Override
  public void initialize(final IPlayerBridge playerBridge, final GamePlayer gamePlayer) {
    super.initialize(playerBridge, gamePlayer);
    setupDiffusionMaps();
    DebugMenu.registerDebugOptions(this, buildDebugOptions());
  }

  private List<AiPlayerDebugOption> buildDebugOptions() {
    return List.of(
        AiPlayerDebugOption.builder().title("HeatMap").subOptions(buildHeatmapOptions()).build(),
        AiPlayerDebugOption.builder()
            .title("Calculate Attrition Factor")
            .actionListener(new LanchesterDebugAction(this, getGameData().getRelationshipTracker()))
            .build());
  }

  private List<AiPlayerDebugOption> buildHeatmapOptions() {
    final List<AiPlayerDebugOption> options = new ArrayList<>();

    options.add(
        AiPlayerDebugOption.builder()
            .title("None")
            .optionType(AiPlayerDebugOption.OptionType.ON_OFF_EXCLUSIVE)
            .exclusiveGroup("heatmap")
            .build());

    options.addAll(
        diffusions.stream()
            .map(
                diffusion ->
                    AiPlayerDebugOption.builder()
                        .title(diffusion.getName())
                        .optionType(AiPlayerDebugOption.OptionType.ON_OFF_EXCLUSIVE)
                        .exclusiveGroup("heatmap")
                        .actionListener(new TerritoryDebugAction(diffusion, getGameData().getMap()))
                        .build())
            .collect(Collectors.toList()));

    return options;
  }

  @Override
  public void stopGame() {
    super.stopGame();
  }

  @Override
  protected void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {
    if (isNewRound(getGameData().getCurrentRound())) {
      calculateTurn();
    }
  }

  private boolean isNewRound(final int round) {
    if (round != this.round) {
      this.round = round;
      return true;
    }
    return false;
  }

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {
    if (isNewRound(getGameData().getCurrentRound())) {
      calculateTurn();
    }
  }

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {
    if (isNewRound(getGameData().getCurrentRound())) {
      calculateTurn();
    }
  }

  @Override
  protected void place(
      final boolean placeForBid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameState data,
      final GamePlayer player) {
    if (isNewRound(getGameData().getCurrentRound())) {
      calculateTurn();
    }
  }

  private void calculateTurn() {
    setupDiffusionMaps();
  }

  private void setupDiffusionMaps() {
    final InfluenceMapBuilder influenceMapBuilder =
        InfluenceMapBuilder.setup()
            .gamePlayer(getGamePlayer())
            .playerList(getGameData().getPlayerList())
            .relationshipTracker(getGameData().getRelationshipTracker())
            .resourceList(getGameData().getResourceList())
            .gameMap(getGameData().getMap())
            .offenseCombatBuilder(
                CombatValueBuilder.mainCombatValue()
                    .side(BattleState.Side.OFFENSE)
                    .gameDiceSides(getGameData().getDiceSides())
                    .gameSequence(getGameData().getSequence())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(getGameData().getProperties()))
                    .supportAttachments(getGameData().getUnitTypeList().getSupportRules()))
            .defenseCombatBuilder(
                CombatValueBuilder.mainCombatValue()
                    .side(BattleState.Side.DEFENSE)
                    .gameDiceSides(getGameData().getDiceSides())
                    .gameSequence(getGameData().getSequence())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(getGameData().getProperties()))
                    .supportAttachments(getGameData().getUnitTypeList().getSupportRules()))
            .build();

    diffusions =
        getGamePlayer().getProductionFrontier().getRules().stream()
            .map(rule -> rule.getResults().entrySet())
            .flatMap(Collection::stream)
            .map(Map.Entry::getKey)
            .filter(UnitType.class::isInstance)
            .map(UnitType.class::cast)
            .filter(unitType -> unitType.getUnitAttachment().getMovement(getGamePlayer()) > 0)
            .map(
                unitType ->
                    influenceMapBuilder.buildMaps(
                        unitType.getName(),
                        new MapWithNeighbors(
                            getGameData().getMap().getTerritories(),
                            new NeighborGetter(unitType, getGameData().getMap()))))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
  }
}
