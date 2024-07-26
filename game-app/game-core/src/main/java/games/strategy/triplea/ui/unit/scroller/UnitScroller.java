package games.strategy.triplea.ui.unit.scroller;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MouseDetails;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.triplea.ui.panels.map.MapSelectionListener;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
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
import org.triplea.swing.CollapsiblePanel;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

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

  private static final int HORIZONTAL_BUTTON_GAP = 2;

  private static final String PREVIOUS_UNITS_TOOLTIP =
      "Press ',' or click to see 'Previous' unmoved units.";
  private static final String NEXT_UNITS_TOOLTIP =
      "Press '.' or click to see 'Next' unmoved units.";
  private static final String SKIP_UNITS_TOOLTIP =
      "Press 'Space' or click to 'Skip' these unmoved units until next move phase.";
  private static final String WAKE_ALL_TOOLTIP =
      "Click to 'Alert' all skipped and sleeping units on the map.";

  private Collection<Unit> skippedUnits = new HashSet<>();
  private final Collection<Unit> sleepingUnits = new HashSet<>();
  private Territory lastFocusedTerritory;

  private final GameData gameData;
  private final MapPanel mapPanel;

  private final Supplier<GamePlayer> currentPlayerSupplier;
  private final Supplier<MovePhase> movePhaseSupplier;
  private final Supplier<Boolean> parentPanelIsVisible;

  private final AvatarPanelFactory avatarPanelFactory;
  private final JLabel territoryNameLabel = new JLabelBuilder().biggerFont().centerAlign().build();
  private final JPanel selectUnitImagePanel = new JPanel();

  private CollapsiblePanel collapsiblePanel;
  private int movesLeft;

  public UnitScroller(
      final GameData data, final MapPanel mapPanel, final Supplier<Boolean> parentPanelIsVisible) {
    this.gameData = data;
    this.mapPanel = mapPanel;
    this.currentPlayerSupplier = () -> gameData.getSequence().getStep().getPlayerId();
    this.movePhaseSupplier =
        () ->
            gameData.getSequence().getStep().isNonCombat()
                ? MovePhase.NON_COMBAT
                : MovePhase.COMBAT;
    movesLeft = movesLeft();
    this.parentPanelIsVisible = parentPanelIsVisible;
    avatarPanelFactory = new AvatarPanelFactory(mapPanel);

    gameData.addGameDataEventListener(GameDataEvent.UNIT_MOVED, this::unitMoved);
    gameData.addGameDataEventListener(GameDataEvent.GAME_STEP_CHANGED, this::gamePhaseChanged);

    mapPanel.addMapSelectionListener(
        new MapSelectionListener() {
          @Override
          public void territorySelected(final Territory territory, final MouseDetails md) {}

          @Override
          public void mouseEntered(@Nullable final Territory territory) {
            if (parentPanelIsVisible.get() && territory != null && movesLeft > 0) {
              lastFocusedTerritory = territory;
              drawUnitAvatarPane(territory);
              territoryNameLabel.setText(territory.getName());
            }
          }

          @Override
          public void mouseMoved(@Nullable final Territory territory, final MouseDetails md) {}
        });
  }

  private void unitMoved() {
    if (!parentPanelIsVisible.get()) {
      return;
    }

    updateMovesLeft();
    if (lastFocusedTerritory == null) {
      focusCapital();
    } else {
      drawUnitAvatarPane(lastFocusedTerritory);
    }

    // remove any moved units from the sleeping units
    sleepingUnits.removeAll(
        CollectionUtils.getMatches(
            gameData.getUnits().getUnits(),
            PredicateBuilder.of(
                    Matches.unitIsOwnedBy(gameData.getSequence().getStep().getPlayerId()))
                .and(Matches.unitHasMoved())
                .build()));
  }

  private void updateMovesLeft() {
    Optional.ofNullable(collapsiblePanel)
        .ifPresent(
            panel -> {
              movesLeft = movesLeft();
              SwingUtilities.invokeLater(
                  () -> {
                    panel.setTitle("Active Units: " + movesLeft);

                    if (movesLeft == 0) {
                      clearUnitAvatarArea();
                    }
                  });
            });
  }

  private void clearUnitAvatarArea() {
    lastFocusedTerritory = null;
    territoryNameLabel.setText("");
    selectUnitImagePanel.removeAll();
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
    updateMovesLeft();
    focusCapital();
  }

  private void focusCapital() {
    Optional.ofNullable(currentPlayerSupplier.get())
        .ifPresent(
            player -> {
              lastFocusedTerritory =
                  TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(
                      player, gameData.getMap());
              Optional.ofNullable(lastFocusedTerritory)
                  .ifPresent(
                      t -> {
                        drawUnitAvatarPane(t);
                        territoryNameLabel.setText(t.getName());
                      });
            });
  }

  private void drawUnitAvatarPane(final Territory t) {
    // use 240 as an approximate default if the containing panel does not yet exist.
    final int panelWidth = selectUnitImagePanel.getWidth();
    final int renderingWidth = panelWidth == 0 ? 240 : panelWidth;

    final GamePlayer player = currentPlayerSupplier.get();
    final List<Unit> moveableUnits =
        player == null
            ? List.of()
            : UnitScrollerModel.getMoveableUnits(
                t, movePhaseSupplier.get(), player, getAllSkippedUnits());

    SwingUtilities.invokeLater(
        () -> {
          selectUnitImagePanel.removeAll();
          if (player != null) {
            selectUnitImagePanel.add(
                avatarPanelFactory.buildPanel(moveableUnits, player, t, renderingWidth));
          }
          SwingComponents.redraw(selectUnitImagePanel);
        });
  }

  /** Constructs a UI component for the UnitScroller. */
  public CollapsiblePanel build() {
    final JPanel panel = new JPanel();
    collapsiblePanel =
        new CollapsiblePanel(panel, "", ClientSetting.unitScrollerCollapsed::setValueAndFlush);
    collapsiblePanel.setCollapsed(ClientSetting.unitScrollerCollapsed.getValueOrThrow());
    updateMovesLeft();

    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    panel.add(selectUnitImagePanel);
    panel.add(territoryNameLabel);
    panel.add(Box.createVerticalStrut(2));

    final JButton prevUnit = new JButton(UnitScrollerIcon.LEFT_ARROW.get());
    prevUnit.setToolTipText(PREVIOUS_UNITS_TOOLTIP);
    prevUnit.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    prevUnit.addActionListener(e -> centerOnPreviousMovableUnit());

    final JButton skipButton = new JButton(UnitScrollerIcon.SKIP.get());
    skipButton.setToolTipText(SKIP_UNITS_TOOLTIP);
    skipButton.addActionListener(e -> skipCurrentUnits());

    final JButton wakeAllButton = new JButton(UnitScrollerIcon.WAKE_ALL.get());
    wakeAllButton.setToolTipText(WAKE_ALL_TOOLTIP);
    wakeAllButton.addActionListener(e -> wakeAllUnits());
    wakeAllButton.setFocusable(false);

    final JButton nextUnit = new JButton(UnitScrollerIcon.RIGHT_ARROW.get());
    nextUnit.setToolTipText(NEXT_UNITS_TOOLTIP);
    nextUnit.addActionListener(e -> centerOnNextMovableUnit());

    final JPanel skipAndSleepPanel =
        new JPanelBuilder()
            .boxLayoutHorizontal()
            .add(prevUnit)
            .addHorizontalStrut(HORIZONTAL_BUTTON_GAP)
            .add(wakeAllButton)
            .addHorizontalStrut(HORIZONTAL_BUTTON_GAP)
            .add(skipButton)
            .addHorizontalStrut(HORIZONTAL_BUTTON_GAP)
            .add(nextUnit)
            .build();
    skipAndSleepPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

    panel.add(skipAndSleepPanel, BorderLayout.SOUTH);
    panel.add(Box.createVerticalStrut(3));
    return collapsiblePanel;
  }

  /**
   * Skips the units in the current territory. Scroller will not scroll back to these units and the
   * units will not be highlighted.
   */
  public void skipCurrentUnits() {
    if (lastFocusedTerritory != null) {
      skippedUnits.addAll(getMovableUnits(lastFocusedTerritory));
      updateMovesLeft();
    }
    centerOnNextMovableUnit();
  }

  private List<Unit> getMovableUnits(final Territory territory) {
    if (territory == null) {
      return List.of();
    }
    return UnitScrollerModel.getMoveableUnits(
        territory, movePhaseSupplier.get(), currentPlayerSupplier.get(), getAllSkippedUnits());
  }

  public void sleepCurrentUnits() {
    if (lastFocusedTerritory != null) {
      sleepingUnits.addAll(getMovableUnits(lastFocusedTerritory));
      updateMovesLeft();
    }
    centerOnNextMovableUnit();
  }

  private void wakeAllUnits() {
    sleepingUnits.clear();
    skippedUnits.clear();
    updateMovesLeft();
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
    final boolean doNotShowAgainClicked = (result == 1);
    if (doNotShowAgainClicked) {
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
        newFocusedTerritory = t;
        mapPanel.setUnitHighlight(Set.of(matchedUnits));
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
      // When the map is moved, the mouse is moved, we will get a territory
      // selected event that will set the lastFocusedTerritory.
      mapPanel.centerOn(newFocusedTerritory);

      // Do an invoke later here so that these actions are after any map UI events.
      final var selectedTerritory = newFocusedTerritory;
      SwingUtilities.invokeLater(() -> updateRenderingToTerritory(selectedTerritory));
    }
  }

  private void updateRenderingToTerritory(final Territory selectedTerritory) {
    lastFocusedTerritory = selectedTerritory;
    territoryNameLabel.setText(lastFocusedTerritory.getName());
    highlightTerritory(selectedTerritory);
    updateMovesLeft();
    drawUnitAvatarPane(selectedTerritory);
  }

  private void highlightTerritory(final Territory territory) {
    if (ClientSetting.unitScrollerHighlightTerritory.getValueOrThrow()) {
      mapPanel.highlightTerritory(
          territory, MapPanel.AnimationDuration.STANDARD, MapPanel.HighlightDelay.SHORT_DELAY);
    }
  }
}
