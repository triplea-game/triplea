package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.engine.posted.game.EndTurnPanel;
import games.strategy.engine.posted.game.MoveForumPosterPanel;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate.MoveType;
import games.strategy.triplea.delegate.data.BattleListing;
import games.strategy.triplea.delegate.data.FightBattleDetails;
import games.strategy.triplea.delegate.data.TechRoll;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.ui.panel.move.MovePanel;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.CardLayout;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.Getter;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.key.binding.ButtonDownMask;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.KeyCombination;
import org.triplea.swing.key.binding.SwingKeyBinding;
import org.triplea.util.Tuple;

/** Root panel for all action buttons in a triplea game. */
public class ActionButtonsPanel extends JPanel {
  public static final String DONE_BUTTON_TOOLTIP =
      "Press ctrl+enter or click this button to end the current turn phase";
  private static final long serialVersionUID = 2175685892863042399L;
  private final CardLayout layout = new CardLayout();

  @Getter private @Nullable BattlePanel battlePanel;

  private @Nullable MovePanel movePanel;

  private @Nullable PurchasePanel purchasePanel;
  private @Nullable RepairPanel repairPanel;

  @Getter private PlacePanel placePanel;

  private @Nullable TechPanel techPanel;
  private @Nullable EndTurnPanel endTurnPanel;
  private @Nullable MoveForumPosterPanel moveForumPosterPanel;
  private @Nullable PoliticsPanel politicsPanel;
  private @Nullable UserActionPanel userActionPanel;
  private @Nullable PickTerritoryAndUnitsPanel pickTerritoryAndUnitsPanel;

  private @Nullable ActionPanel actionPanel;

  public ActionButtonsPanel(
      final GameData data,
      final MapPanel map,
      final MovePanel movePanel,
      final TripleAFrame parent) {
    registerKeyBindings(parent);
    battlePanel = new BattlePanel(data, map, parent);
    this.movePanel = movePanel;
    purchasePanel = new PurchasePanel(data, map);
    repairPanel = new RepairPanel(data, map);
    placePanel = new PlacePanel(data, map, parent);
    techPanel = new TechPanel(data, map);
    endTurnPanel = new EndTurnPanel(data, map);
    moveForumPosterPanel = new MoveForumPosterPanel(data, map);
    politicsPanel = new PoliticsPanel(data, map, parent, parent.getUiContext().getClipPlayer());
    userActionPanel = new UserActionPanel(data, map, parent);
    pickTerritoryAndUnitsPanel = new PickTerritoryAndUnitsPanel(data, map, parent);
    actionPanel = techPanel;
    setLayout(layout);
    add(new JLabel(""), "");
    add(battlePanel, battlePanel.toString());
    add(this.movePanel, this.movePanel.toString());
    add(repairPanel, repairPanel.toString());
    add(purchasePanel, purchasePanel.toString());
    add(placePanel, placePanel.toString());
    add(techPanel, techPanel.toString());
    add(endTurnPanel, endTurnPanel.toString());
    add(moveForumPosterPanel, moveForumPosterPanel.toString());
    add(politicsPanel, politicsPanel.toString());
    add(userActionPanel, userActionPanel.toString());
    add(pickTerritoryAndUnitsPanel, pickTerritoryAndUnitsPanel.toString());
    // this should not be necessary, but it makes tracking down garbage leaks easier in the profiler
    // since it removes a lot of links between objects and if there is a memory leak, this will
    // minimize the damage
    map.getUiContext()
        .addShutdownHook(
            () -> {
              removeAll();
              actionPanel = null;
              battlePanel.removeAll();
              this.movePanel.removeAll();
              repairPanel.removeAll();
              purchasePanel.removeAll();
              placePanel.removeAll();
              techPanel.removeAll();
              endTurnPanel.removeAll();
              moveForumPosterPanel.removeAll();
              politicsPanel.removeAll();
              userActionPanel.removeAll();
              pickTerritoryAndUnitsPanel.removeAll();
              battlePanel = null;
              this.movePanel = null;
              repairPanel = null;
              purchasePanel = null;
              placePanel = null;
              techPanel = null;
              endTurnPanel = null;
              moveForumPosterPanel = null;
              politicsPanel = null;
              userActionPanel = null;
              pickTerritoryAndUnitsPanel = null;
            });
  }

  void changeToMove(final GamePlayer gamePlayer, final boolean nonCombat, final String stepName) {
    if (movePanel != null) {
      movePanel.setNonCombat(nonCombat);
      final boolean airBorne = stepName.endsWith("AirborneCombatMove");
      final String displayText = (airBorne ? " Airborne" : (nonCombat ? " Non" : ""));
      movePanel.setDisplayText(displayText + " Combat Move");
      movePanel.setMoveType(airBorne ? MoveType.SPECIAL : MoveType.DEFAULT);
    }
    changeTo(gamePlayer, movePanel);
  }

  public void changeToRepair(final GamePlayer gamePlayer) {
    changeTo(gamePlayer, repairPanel);
  }

  public void changeToProduce(final GamePlayer gamePlayer, final boolean keepCurrentPurchase) {
    purchasePanel.setKeepCurrentPurchase(keepCurrentPurchase);
    changeTo(gamePlayer, purchasePanel);
  }

  public void changeToPlace(final GamePlayer gamePlayer) {
    changeTo(gamePlayer, placePanel);
  }

  public void changeToBattle(final GamePlayer gamePlayer, final BattleListing battles) {
    if (battlePanel != null) {
      battlePanel.setBattlesAndBombing(battles);
    }
    changeTo(gamePlayer, battlePanel);
  }

  public void changeToPolitics(final GamePlayer gamePlayer) {
    changeTo(gamePlayer, politicsPanel);
  }

  public void changeToUserActions(final GamePlayer gamePlayer) {
    changeTo(gamePlayer, userActionPanel);
  }

  public void changeToTech(final GamePlayer gamePlayer) {
    changeTo(gamePlayer, techPanel);
  }

  public void changeToEndTurn(final GamePlayer gamePlayer) {
    changeTo(gamePlayer, endTurnPanel);
  }

  public void changeToMoveForumPosterPanel(final GamePlayer gamePlayer) {
    changeTo(gamePlayer, moveForumPosterPanel);
  }

  private void changeTo(final GamePlayer gamePlayer, final @Nullable ActionPanel newCurrent) {
    if (actionPanel != null) {
      actionPanel.setActive(false);
    }

    actionPanel = newCurrent;

    // newCurrent might be null if we are shutting down
    if (newCurrent != null) {
      newCurrent.display(gamePlayer);
      SwingUtilities.invokeLater(() -> layout.show(ActionButtonsPanel.this, newCurrent.toString()));
    }
  }

  public void changeToPickTerritoryAndUnits(final GamePlayer gamePlayer) {
    changeTo(gamePlayer, pickTerritoryAndUnitsPanel);
  }

  /**
   * Blocks until the user selects their purchase.
   *
   * @return null if no move was made.
   */
  public IntegerMap<ProductionRule> waitForPurchase(final boolean bid) {
    if (purchasePanel == null) {
      return null;
    }
    return purchasePanel.waitForPurchase(bid);
  }

  /**
   * Blocks until the user selects their purchase.
   *
   * @return null if no move was made.
   */
  public Map<Unit, IntegerMap<RepairRule>> waitForRepair(
      final boolean bid, final Collection<GamePlayer> allowedPlayersToRepair) {
    if (repairPanel == null) {
      return null;
    }
    return repairPanel.waitForRepair(bid, allowedPlayersToRepair);
  }

  /**
   * Blocks until the user moves units.
   *
   * @return null if no move was made.
   */
  public MoveDescription waitForMove(final PlayerBridge bridge) {
    if (movePanel == null) {
      return null;
    }
    return movePanel.waitForMove(bridge);
  }

  /**
   * Blocks until the user selects the number of tech rolls.
   *
   * @return null if no tech roll was made.
   */
  public TechRoll waitForTech() {
    if (techPanel == null) {
      return null;
    }
    return techPanel.waitForTech();
  }

  /**
   * Blocks until the user selects a political action to attempt.
   *
   * @return null if no action was picked.
   */
  public PoliticalActionAttachment waitForPoliticalAction(
      final boolean firstRun, final IPoliticsDelegate politicsDelegate) {
    if (politicsPanel == null) {
      return null;
    }
    return politicsPanel.waitForPoliticalAction(firstRun, politicsDelegate);
  }

  /**
   * Blocks until the user selects a user action to attempt.
   *
   * @return null if no action was picked.
   */
  public UserActionAttachment waitForUserActionAction(
      final boolean firstRun, final IUserActionDelegate userActionDelegate) {
    if (userActionPanel == null) {
      return null;
    }
    return userActionPanel.waitForUserActionAction(firstRun, userActionDelegate);
  }

  /**
   * Blocks until the user selects units to place.
   *
   * @return null if no placement was made.
   */
  public PlaceData waitForPlace(final boolean bid, final PlayerBridge bridge) {
    if (placePanel == null) {
      return null;
    }
    return placePanel.waitForPlace(bid, bridge);
  }

  /** Blocks until the user selects an end-of-turn action. */
  public void waitForEndTurn(final TripleAFrame frame, final PlayerBridge bridge) {
    if (endTurnPanel != null) {
      endTurnPanel.waitForDone(frame, bridge);
    }
  }

  public void waitForMoveForumPosterPanel(final TripleAFrame frame, final PlayerBridge bridge) {
    if (moveForumPosterPanel != null) {
      moveForumPosterPanel.waitForDone(frame, bridge);
    }
  }

  /** Blocks until the user selects a battle to fight. */
  public FightBattleDetails waitForBattleSelection() {
    if (battlePanel == null) {
      return null;
    }
    return battlePanel.waitForBattleSelection();
  }

  public Tuple<Territory, Set<Unit>> waitForPickTerritoryAndUnits(
      final List<Territory> territoryChoices,
      final List<Unit> unitChoices,
      final int unitsPerPick) {
    if (pickTerritoryAndUnitsPanel == null) {
      return Tuple.of(null, Set.of());
    }
    return pickTerritoryAndUnitsPanel.waitForPickTerritoryAndUnits(
        territoryChoices, unitChoices, unitsPerPick);
  }

  public Optional<ActionPanel> getCurrent() {
    return Optional.ofNullable(actionPanel);
  }

  /**
   * Adds a hotkey listener to 'click the done button'. If the current phase has no done button (eg:
   * combat phase), then the hotkey will be a no-op.
   */
  private void registerKeyBindings(final JFrame frame) {
    SwingKeyBinding.addKeyBinding(
        frame,
        KeyCombination.of(KeyCode.ENTER, ButtonDownMask.CTRL),
        () -> Optional.ofNullable(actionPanel).ifPresent(ActionPanel::performDone));
  }
}
