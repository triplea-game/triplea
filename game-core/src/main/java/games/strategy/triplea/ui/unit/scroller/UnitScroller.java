package games.strategy.triplea.ui.unit.scroller;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MapPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.SwingComponents;

/**
 * Unit scroller is a UI component to 'scroll' through units that can be moved. The component is to
 * help players avoid "forgetting to move a unit" by letting them know how many units can be moved
 * and to find them on the map. The scroller provides functionality to center on a territory with
 * movable units, arrows to go to next and previous and a 'sleep' button to skip the current unit.
 * The unit scroller has a center display to show an icon of the units in the current territory.
 *
 * <p>The unit scroller keeps track of state to know which territory is current.
 */
public class UnitScroller {
  /** Enum to indicate combat or non-combat phase. */
  public enum MovePhase {
    COMBAT,
    NON_COMBAT
  }

  private static final String PREVIOUS_UNITS_TOOLTIP =
      "Press 'm' or click this button to center the screen on the 'previous' units with "
          + "movement left";
  private static final String NEXT_UNITS_TOOLTIP =
      "Press 'n' or click this button to center the screen on the 'next' units with movement left";
  private static final String CENTER_UNITS_TOOLTIP =
      "Press 'c' or click this button to center the screen on current units.";
  private static final String SLEEP_UNITS_TOOLTIP =
      "Press 's' or click this button to sleep the current units, they will be automatically "
          + "skipped until you move them.";
  private static final String SKIP_UNITS_TOOLTIP =
      "Press 'space' or click this button to skip the current units and not move them during the "
          + "current move phase";

  private Collection<Unit> skippedUnits = new HashSet<>();
  private Collection<Unit> sleepingUnits = new HashSet<>();
  private Territory lastFocusedTerritory;

  private final GameData gameData;
  private final MapPanel mapPanel;

  private final Supplier<PlayerId> currentPlayerSupplier;
  private final Supplier<MovePhase> movePhaseSupplier;

  private final AvatarPanelFactory avatarPanelFactory;
  private final JLabel movesLeftLabel = new JLabel();
  private final JLabel territoryNameLabel = new JLabel();
  private final JPanel selectUnitImagePanel = new JPanel();

  public UnitScroller(final GameData data, final MapPanel mapPanel) {
    this.gameData = data;
    this.mapPanel = mapPanel;
    this.currentPlayerSupplier = () -> gameData.getSequence().getStep().getPlayerId();
    this.movePhaseSupplier =
        () ->
            gameData.getSequence().getStep().isNonCombat()
                ? MovePhase.NON_COMBAT
                : MovePhase.COMBAT;
    avatarPanelFactory = new AvatarPanelFactory(mapPanel);

    gameData.addGameDataEventListener(GameDataEvent.UNIT_MOVED, this::unitMoved);
    gameData.addGameDataEventListener(GameDataEvent.GAME_STEP_CHANGED, this::gamePhaseChanged);
  }

  private void unitMoved() {
    updateMovesLeftLabel();
    drawUnitAvatarPane(lastFocusedTerritory);

    // remove any moved units from the sleeping units
    sleepingUnits.removeAll(
        CollectionUtils.getMatches(
            gameData.getUnits().getUnits(),
            PredicateBuilder.of(
                    Matches.unitIsOwnedBy(gameData.getSequence().getStep().getPlayerId()))
                .and(Matches.unitHasMoved())
                .build()));
  }

  private void updateMovesLeftLabel() {
    movesLeftLabel.setText("Units left to move: " + movesLeft());
  }

  private int movesLeft() {
    return UnitScrollerModel.computeUnitsToMoveCount(
        gameData.getMap().getTerritories(),
        movePhaseSupplier.get(),
        currentPlayerSupplier.get(),
        getAllSkippedUnits());
  }

  /** Returns both skipped and sleeping units. */
  public Collection<Unit> getAllSkippedUnits() {
    final Collection<Unit> skipped = new HashSet<>();
    skipped.addAll(skippedUnits);
    skipped.addAll(sleepingUnits);
    return skipped;
  }

  private void gamePhaseChanged() {
    skippedUnits = new HashSet<>();
    lastFocusedTerritory = null;
    selectUnitImagePanel.removeAll();
    selectUnitImagePanel.repaint();
    focusCapital();
  }

  private void focusCapital() {
    Optional.ofNullable(currentPlayerSupplier.get())
        .ifPresent(
            player -> {
              lastFocusedTerritory =
                  TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, gameData);
              Optional.ofNullable(lastFocusedTerritory)
                  .ifPresent(
                      t -> {
                        drawUnitAvatarPane(t);
                        territoryNameLabel.setText(t.getName());
                      });
            });
  }

  private void drawUnitAvatarPane(final Territory t) {
    final List<Unit> matchedUnits =
        UnitScrollerModel.getMoveableUnits(
            t, movePhaseSupplier.get(), currentPlayerSupplier.get(), getAllSkippedUnits());

    SwingUtilities.invokeLater(
        () -> {
          selectUnitImagePanel.removeAll();
          selectUnitImagePanel.add(
              avatarPanelFactory.buildPanel(matchedUnits, currentPlayerSupplier.get()));
          selectUnitImagePanel.revalidate();
          selectUnitImagePanel.repaint();
        });
  }

  /** Constructs a UI component for the UnitScroller. */
  public Component build() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    updateMovesLeftLabel();

    final JButton centerOnUnit = new JButton(UnitScrollerIcon.CENTER_ON_UNIT.get());
    centerOnUnit.setToolTipText(CENTER_UNITS_TOOLTIP);
    // disallow focus so that key listeners from MovePanel will continue to function
    centerOnUnit.setFocusable(false);
    centerOnUnit.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    centerOnUnit.addActionListener(e -> centerOnCurrentMovableUnit());
    panel.add(centerOnUnit, BorderLayout.NORTH);
    panel.add(Box.createVerticalStrut(2));

    territoryNameLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    panel.add(territoryNameLabel);

    final JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));

    final JButton prevUnit = new JButton(UnitScrollerIcon.LEFT_ARROW.get());
    prevUnit.setToolTipText(PREVIOUS_UNITS_TOOLTIP);
    prevUnit.setFocusable(false);
    prevUnit.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    prevUnit.addActionListener(e -> centerOnPreviousMovableUnit());
    centerPanel.add(prevUnit);
    centerPanel.add(selectUnitImagePanel);

    final JButton nextUnit = new JButton(UnitScrollerIcon.RIGHT_ARROW.get());
    nextUnit.setToolTipText(NEXT_UNITS_TOOLTIP);
    nextUnit.setFocusable(false);
    nextUnit.addActionListener(e -> centerOnNextMovableUnit());
    centerPanel.add(nextUnit, BorderLayout.EAST);
    centerPanel.add(Box.createHorizontalStrut(10));

    panel.add(centerPanel);
    panel.add(Box.createVerticalStrut(5));

    final JButton skipButton = new JButton(UnitScrollerIcon.UNIT_SKIP.get());
    skipButton.setToolTipText(SKIP_UNITS_TOOLTIP);
    skipButton.setFocusable(false);
    skipButton.addActionListener(e -> skipCurrentUnits());

    final JButton sleepButton = new JButton(UnitScrollerIcon.UNIT_SLEEP.get());
    sleepButton.setToolTipText(SLEEP_UNITS_TOOLTIP);
    sleepButton.setFocusable(false);
    sleepButton.addActionListener(e -> sleepCurrentUnits());

    final JPanel skipAndSleepPanel =
        JPanelBuilder.builder()
            .horizontalBoxLayout()
            .add(skipButton)
            .addHorizontalStrut(30)
            .add(sleepButton)
            .build();
    skipAndSleepPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

    panel.add(skipAndSleepPanel, BorderLayout.SOUTH);
    panel.add(Box.createVerticalStrut(3));

    panel.add(SwingComponents.leftBox(movesLeftLabel));

    panel.add(Box.createVerticalStrut(5));
    return panel;
  }

  /** Centers the map on the current territory shown in the unit scroller. */
  public void centerOnCurrentMovableUnit() {
    if (lastFocusedTerritory != null && !getMovableUnits(lastFocusedTerritory).isEmpty()) {
      mapPanel.setUnitHighlight(
          Collections.singleton(
              UnitScrollerModel.getMoveableUnits(
                  lastFocusedTerritory,
                  movePhaseSupplier.get(),
                  currentPlayerSupplier.get(),
                  getAllSkippedUnits())));
      mapPanel.centerOnTerritoryIgnoringMapLock(lastFocusedTerritory);
    } else {
      centerOnNextMovableUnit();
    }
  }

  private List<Unit> getMovableUnits(final Territory territory) {
    if (territory == null) {
      return Collections.emptyList();
    }
    return UnitScrollerModel.getMoveableUnits(
        territory, movePhaseSupplier.get(), currentPlayerSupplier.get(), getAllSkippedUnits());
  }

  /**
   * Skips the units in the current territory. Scroller will not scroll back to these units and the
   * units will not be highlighted.
   */
  public void skipCurrentUnits() {
    if (lastFocusedTerritory != null) {
      skippedUnits.addAll(getMovableUnits(lastFocusedTerritory));
      updateMovesLeftLabel();
    }
    centerOnNextMovableUnit();
  }

  public void sleepCurrentUnits() {
    if (lastFocusedTerritory != null) {
      sleepingUnits.addAll(getMovableUnits(lastFocusedTerritory));
      updateMovesLeftLabel();
    }
    centerOnNextMovableUnit();
  }

  public void centerOnNextMovableUnit() {
    centerOnMovableUnit(true);

    if (lastFocusedTerritory != null) {
      showAllUnitsMovedIfNeeded();
    }
  }

  private void showAllUnitsMovedIfNeeded() {
    if (movesLeft() == 0 && ClientSetting.notifyAllUnitsMoved.getValueOrThrow()) {
      showAllUnitsMoved();
    }
  }

  private void showAllUnitsMoved() {
    final int result =
        JOptionPane.showOptionDialog(
            mapPanel,
            "All units have moved",
            "All units moved",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            new String[] {"OK", "Do not show again"},
            "OK");
    if (result == 1) {
      ClientSetting.notifyAllUnitsMoved.setValueAndFlush(false);
      DialogBuilder.builder()
          .parent(mapPanel)
          .title("All units moved confirmation turned off")
          .infoMessage("Will not show this message again.\nIt can be turned on from game settings.")
          .showDialog();
    }
  }

  public void centerOnPreviousMovableUnit() {
    centerOnMovableUnit(false);
    showAllUnitsMovedIfNeeded();
  }

  private void centerOnMovableUnit(final boolean selectNext) {
    List<Territory> allTerritories = gameData.getMap().getTerritories();

    if (!selectNext) {
      final List<Territory> territories = new ArrayList<>(allTerritories);
      Collections.reverse(territories);
      allTerritories = territories;
    }
    // new focused index is 1 greater
    int newFocusedIndex =
        lastFocusedTerritory == null ? 0 : allTerritories.indexOf(lastFocusedTerritory) + 1;
    if (newFocusedIndex >= allTerritories.size()) {
      // if we are larger than the number of territories, we must start back at zero
      newFocusedIndex = 0;
    }
    Territory newFocusedTerritory = null;
    // make sure we go through every single territory on the board
    for (int i = 0; i < allTerritories.size(); i++) {
      final Territory t = allTerritories.get(newFocusedIndex);
      final List<Unit> matchedUnits = getMovableUnits(t);

      if (!matchedUnits.isEmpty()) {
        drawUnitAvatarPane(t);
        newFocusedTerritory = t;
        mapPanel.setUnitHighlight(Collections.singleton(matchedUnits));
        break;
      }
      // make sure to cycle through the front half of territories
      if ((newFocusedIndex + 1) >= allTerritories.size()) {
        newFocusedIndex = 0;
      } else {
        newFocusedIndex++;
      }
    }
    if (newFocusedTerritory != null) {
      lastFocusedTerritory = newFocusedTerritory;
      territoryNameLabel.setText(lastFocusedTerritory.getName());
      mapPanel.centerOn(newFocusedTerritory);
    }
  }
}
