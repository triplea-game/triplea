package org.triplea.ai.flowfield;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Resource;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.player.IPlayerBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.menubar.DebugMenu;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import lombok.Getter;
import org.triplea.ai.flowfield.map.DiffusionMap;
import org.triplea.ai.flowfield.map.DiffusionType;
import org.triplea.ai.flowfield.map.TerritoryDebugUiAction;
import org.triplea.ai.flowfield.map.offense.EnemyCapitals;
import org.triplea.ai.flowfield.map.offense.ResourceToGet;
import org.triplea.ai.flowfield.odds.LanchesterDebugUiAction;
import org.triplea.swing.SwingAction;

public class FlowFieldAi extends AbstractAi {

  @Getter private final Map<String, DiffusionMap> diffusions = new HashMap<>();
  private int round = -1;

  public FlowFieldAi(final String name, final PlayerTypes.AiType playerType) {
    super(name, playerType);
  }

  @Override
  public void initialize(final IPlayerBridge playerBridge, final GamePlayer gamePlayer) {
    super.initialize(playerBridge, gamePlayer);
    DebugMenu.registerMenuCallback(getName(), this::addDebugMenuItems);

    final DiffusionType enemyCapitals =
        EnemyCapitals.build(getGamePlayer(), getGameData().getPlayerList(), getGameData().getMap());
    diffusions.put(
        enemyCapitals.getName(),
        new DiffusionMap(
            enemyCapitals, territory -> getGameData().getMap().getNeighbors(territory)));
    final Resource pus = getGameData().getResourceList().getResource(Constants.PUS);
    final DiffusionType resourcesToGet =
        ResourceToGet.build(
            getGamePlayer(), getGameData().getRelationshipTracker(), getGameData().getMap(), pus);
    diffusions.put(
        resourcesToGet.getName(),
        new DiffusionMap(
            resourcesToGet, territory -> getGameData().getMap().getNeighbors(territory)));
  }

  private Collection<JMenuItem> addDebugMenuItems(final TripleAFrame frame) {
    final Map<JRadioButtonMenuItem, TerritoryDebugUiAction> actions = new HashMap<>();
    final JMenu heatmapMenu = new JMenu("HeatMap");
    final ButtonGroup heatmapGroup = new ButtonGroup();

    // add an entry that will clear the heatmaps and other listeners
    final JRadioButtonMenuItem clearHeatmap =
        new JRadioButtonMenuItem(
            SwingAction.of(
                "None",
                new AbstractAction() {
                  @Override
                  public void actionPerformed(final ActionEvent e) {
                    getGameData()
                        .getMap()
                        .getTerritories()
                        .forEach(
                            territory -> {
                              frame.getMapPanel().clearTerritoryOverlay(territory);
                            });
                    frame.getMapPanel().repaint();
                  }
                }));
    clearHeatmap.setSelected(true);
    clearHeatmap.addItemListener(
        e ->
            actions.entrySet().stream()
                .filter(entry -> entry.getKey() != e.getSource())
                .forEach(entry -> entry.getValue().unselect()));
    heatmapGroup.add(clearHeatmap);
    heatmapMenu.add(clearHeatmap);

    // add a button for each of the maps
    diffusions.forEach(
        (name, diffusion) -> {
          final TerritoryDebugUiAction action =
              new TerritoryDebugUiAction(frame, diffusion, getGameData().getMap());
          final JRadioButtonMenuItem menuItem =
              new JRadioButtonMenuItem(SwingAction.of(name, action));
          actions.put(menuItem, action);
          menuItem.addItemListener(
              e ->
                  actions.entrySet().stream()
                      .filter(entry -> entry.getKey() != e.getSource())
                      .forEach(entry -> entry.getValue().unselect()));
          heatmapGroup.add(menuItem);
          heatmapMenu.add(menuItem);
        });

    return List.of(
        heatmapMenu,
        new JMenuItem(SwingAction.of("Lanchester", new LanchesterDebugUiAction(frame, this))));
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

  private void calculateTurn() {}
}
