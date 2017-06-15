package games.strategy.triplea.ui;

import java.awt.CardLayout;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate.MoveType;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

/**
 * Root panel for all action buttons in a triplea game. <br>
 */
public class ActionButtons extends JPanel {
  private static final long serialVersionUID = 2175685892863042399L;
  private final CardLayout layout = new CardLayout();
  private BattlePanel battlePanel;
  private MovePanel movePanel;
  private PurchasePanel purchasePanel;
  private RepairPanel repairPanel;
  private PlacePanel placePanel;
  private TechPanel techPanel;
  private EndTurnPanel endTurnPanel;
  private MoveForumPosterPanel moveForumPosterPanel;
  private ActionPanel actionPanel;
  private PoliticsPanel politicsPanel;
  private UserActionPanel userActionPanel;
  private PickTerritoryAndUnitsPanel pickTerritoryAndUnitsPanel;

  /** Creates new ActionButtons. */
  public ActionButtons(final GameData data, final MapPanel map, final MovePanel movePanel, final TripleAFrame parent) {
    battlePanel = new BattlePanel(data, map);
    this.movePanel = movePanel;
    purchasePanel = new PurchasePanel(data, map);
    repairPanel = new RepairPanel(data, map);
    placePanel = new PlacePanel(data, map, parent);
    techPanel = new TechPanel(data, map);
    endTurnPanel = new EndTurnPanel(data, map);
    moveForumPosterPanel = new MoveForumPosterPanel(data, map);
    politicsPanel = new PoliticsPanel(data, map, parent);
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
    // this should not be necceessary
    // but it makes tracking down garbage leaks easier
    // in the profiler
    // since it removes a lot of links
    // between objects
    // and if there is a memory leak
    // this will minimize the damage
    map.getUIContext().addActive(() -> {
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

  void changeToMove(final PlayerID id, final boolean nonCombat, final String stepName) {
    movePanel.setNonCombat(nonCombat);
    final boolean airBorne = stepName.endsWith("AirborneCombatMove");
    final String displayText = (airBorne ? " Airborne" : (nonCombat ? " Non" : ""));
    movePanel.setDisplayText(displayText + " Combat Move");
    movePanel.setMoveType(airBorne ? MoveType.SPECIAL : MoveType.DEFAULT);
    changeTo(id, movePanel);
  }

  public void changeToRepair(final PlayerID id) {
    changeTo(id, repairPanel);
  }

  public void changeToProduce(final PlayerID id) {
    changeTo(id, purchasePanel);
  }

  public void changeToPlace(final PlayerID id) {
    changeTo(id, placePanel);
  }

  public void changeToBattle(final PlayerID id, final Map<BattleType, Collection<Territory>> battles) {
    battlePanel.setBattlesAndBombing(battles);
    changeTo(id, battlePanel);
  }

  public void changeToPolitics(final PlayerID id) {
    changeTo(id, politicsPanel);
  }

  public void changeToUserActions(final PlayerID id) {
    changeTo(id, userActionPanel);
  }

  public void changeToTech(final PlayerID id) {
    changeTo(id, techPanel);
  }

  public void changeToEndTurn(final PlayerID id) {
    changeTo(id, endTurnPanel);
  }

  public void changeToMoveForumPosterPanel(final PlayerID id) {
    changeTo(id, moveForumPosterPanel);
  }

  private void changeTo(final PlayerID id, final ActionPanel newCurrent) {
    actionPanel.setActive(false);
    actionPanel = newCurrent;
    // newCurrent might be null if we are shutting down
    if (actionPanel == null) {
      return;
    }
    actionPanel.display(id);
    final String currentName = actionPanel.toString();
    SwingUtilities.invokeLater(() -> {
      if (layout != null) {
        layout.show(ActionButtons.this, currentName);
      }
    });
  }

  public void changeToPickTerritoryAndUnits(final PlayerID id) {
    changeTo(id, pickTerritoryAndUnitsPanel);
  }

  /**
   * Blocks until the user selects their purchase.
   *
   * @return null if no move was made.
   */
  public IntegerMap<ProductionRule> waitForPurchase(final boolean bid) {
    return purchasePanel.waitForPurchase(bid);
  }

  /**
   * Blocks until the user selects their purchase.
   *
   * @return null if no move was made.
   */
  public HashMap<Unit, IntegerMap<RepairRule>> waitForRepair(final boolean bid,
      final Collection<PlayerID> allowedPlayersToRepair) {
    return repairPanel.waitForRepair(bid, allowedPlayersToRepair);
  }

  /**
   * Blocks until the user moves units.
   *
   * @return null if no move was made.
   */
  public MoveDescription waitForMove(final IPlayerBridge bridge) {
    return movePanel.waitForMove(bridge);
  }

  /**
   * Blocks until the user selects the number of tech rolls.
   *
   * @return null if no tech roll was made.
   */
  public TechRoll waitForTech() {
    return techPanel.waitForTech();
  }

  /**
   * Blocks until the user selects a political action to attempt
   *
   * @return null if no action was picked.
   */
  public PoliticalActionAttachment waitForPoliticalAction(final boolean firstRun,
      final IPoliticsDelegate iPoliticsDelegate) {
    return politicsPanel.waitForPoliticalAction(firstRun, iPoliticsDelegate);
  }

  /**
   * Blocks until the user selects a user action to attempt
   *
   * @return null if no action was picked.
   */
  public UserActionAttachment waitForUserActionAction(final boolean firstRun,
      final IUserActionDelegate iUserActionDelegate) {
    return userActionPanel.waitForUserActionAction(firstRun, iUserActionDelegate);
  }

  /**
   * Blocks until the user selects units to place.
   *
   * @return null if no placement was made.
   */
  public PlaceData waitForPlace(final boolean bid, final IPlayerBridge bridge) {
    return placePanel.waitForPlace(bid, bridge);
  }

  /**
   * Blocks until the user selects an end-of-turn action.
   */
  public void waitForEndTurn(final TripleAFrame frame, final IPlayerBridge bridge) {
    endTurnPanel.waitForEndTurn(frame, bridge);
  }

  public void waitForMoveForumPosterPanel(final TripleAFrame frame, final IPlayerBridge bridge) {
    moveForumPosterPanel.waitForDone(frame, bridge);
  }

  /**
   * Blocks until the user selects a battle to fight.
   */
  public FightBattleDetails waitForBattleSelection() {
    return battlePanel.waitForBattleSelection();
  }

  public Tuple<Territory, Set<Unit>> waitForPickTerritoryAndUnits(final List<Territory> territoryChoices,
      final List<Unit> unitChoices, final int unitsPerPick) {
    return pickTerritoryAndUnitsPanel.waitForPickTerritoryAndUnits(territoryChoices, unitChoices, unitsPerPick);
  }

  public ActionPanel getCurrent() {
    return actionPanel;
  }

  public BattlePanel getBattlePanel() {
    return battlePanel;
  }

  public AbstractMovePanel getMovePanel() {
    return movePanel;
  }

  public PlacePanel getPlacePanel() {
    return placePanel;
  }

  public PurchasePanel getPurchasePanel() {
    return purchasePanel;
  }

  public TechPanel getTechPanel() {
    return techPanel;
  }

  public EndTurnPanel getEndTurnPanel() {
    return endTurnPanel;
  }

  public MoveForumPosterPanel getMoveForumPosterPanel() {
    return moveForumPosterPanel;
  }
}
