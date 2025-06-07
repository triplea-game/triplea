package games.strategy.triplea.ui;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.panels.map.MapSelectionListener;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.EventThreadJOptionPane.ConfirmDialogType;
import org.triplea.swing.SwingComponents;
import org.triplea.util.Tuple;

/** For choosing territories and units for them, during RandomStartDelegate. */
@Slf4j
public class PickTerritoryAndUnitsPanel extends ActionPanel {
  private static final long serialVersionUID = -2672163347536778594L;
  private final TripleAFrame frame;
  private JButton doneButton = null;
  private JButton selectTerritoryButton = null;
  private JButton selectUnitsButton = null;
  private Territory pickedTerritory = null;
  private Set<Unit> pickedUnits = new HashSet<>();
  private List<Territory> territoryChoices = null;
  private List<Unit> unitChoices = null;
  private int unitsPerPick = 1;
  private Action currentAction = null;
  private @Nullable Territory currentHighlightedTerritory;

  private final Action selectUnitsAction =
      new AbstractAction("Select Units") {
        private static final long serialVersionUID = 4745335350716395600L;

        @Override
        public void actionPerformed(final ActionEvent event) {
          currentAction = selectUnitsAction;
          setWidgetActivation();
          final UnitChooser unitChooser =
              new UnitChooser(unitChoices, Map.of(), false, getMap().getUiContext());
          unitChooser.setMaxAndShowMaxButton(unitsPerPick);
          if (EventThreadJOptionPane.showConfirmDialog(
              frame, unitChooser, "Select Units", ConfirmDialogType.OK_CANCEL)) {
            pickedUnits.clear();
            pickedUnits.addAll(unitChooser.getSelected());
          }
          currentAction = null;
          setWidgetActivation();
        }
      };

  private final Action selectTerritoryAction =
      new AbstractAction("Select Territory") {
        private static final long serialVersionUID = -8003634505955439651L;

        @Override
        public void actionPerformed(final ActionEvent event) {
          currentAction = selectTerritoryAction;
          setWidgetActivation();
          getMap().addMapSelectionListener(mapSelectionListener);
        }
      };

  private final MapSelectionListener mapSelectionListener =
      new DefaultMapSelectionListener() {
        @Override
        public void territorySelected(final Territory territory, final MouseDetails md) {
          if (currentAction == selectTerritoryAction) {
            if (!territoryChoices.contains(territory)) {
              EventThreadJOptionPane.showMessageDialog(
                  frame,
                  "You must pick an unowned land territory (will have a white highlight)",
                  "Must Pick An Unowned Land Territory",
                  JOptionPane.WARNING_MESSAGE);
              return;
            }
            pickedTerritory = territory;
            SwingUtilities.invokeLater(
                () -> {
                  getMap().removeMapSelectionListener(mapSelectionListener);
                  currentAction = null;
                  setWidgetActivation();
                });
          } else {
            log.error(
                "Should not be able to select a territory outside of the selectTerritoryAction.");
          }
        }

        @Override
        public void mouseMoved(final @Nullable Territory territory, final MouseDetails md) {
          if (!isActive()) {
            // This could happen if the history panel is open.
            return;
          }

          // highlight territory
          if (territory != null
              && currentAction == selectTerritoryAction
              && !territory.equals(currentHighlightedTerritory)) {
            if (currentHighlightedTerritory != null) {
              getMap().clearTerritoryOverlay(currentHighlightedTerritory);
            }
            currentHighlightedTerritory = territory;
            if (territoryChoices.contains(currentHighlightedTerritory)) {
              getMap().setTerritoryOverlay(currentHighlightedTerritory, Color.WHITE, 200);
            } else {
              getMap().setTerritoryOverlay(currentHighlightedTerritory, Color.RED, 200);
            }
            getMap().repaint();
          }
        }
      };

  public PickTerritoryAndUnitsPanel(final TripleAFrame frame) {
    super(frame);
    this.frame = frame;
  }

  @Override
  public String toString() {
    return "Pick Territory and Units";
  }

  @Override
  public void display(final GamePlayer gamePlayer) {
    super.display(gamePlayer);
    pickedTerritory = null;
    pickedUnits = new HashSet<>();
    currentAction = null;
    currentHighlightedTerritory = null;
    SwingUtilities.invokeLater(
        () -> {
          removeAll();
          actionLabel.setText(gamePlayer.getName() + " Pick Territory and Units");
          add(actionLabel);
          selectTerritoryButton = new JButton(selectTerritoryAction);
          add(selectTerritoryButton);
          selectUnitsButton = new JButton(selectUnitsAction);
          add(selectUnitsButton);
          doneButton = createDoneButton();
          add(doneButton);
          SwingComponents.redraw(this);
          SwingUtilities.invokeLater(() -> selectTerritoryButton.requestFocusInWindow());
        });
  }

  @Override
  public void performDone() {
    currentAction = doneButton.getAction();
    setWidgetActivation();
    if (pickedTerritory == null || !territoryChoices.contains(pickedTerritory)) {
      EventThreadJOptionPane.showMessageDialog(
          frame,
          "Must Pick An Unowned Territory",
          "Must Pick An Unowned Territory",
          JOptionPane.WARNING_MESSAGE);
      currentAction = null;
      if (currentHighlightedTerritory != null) {
        getMap().clearTerritoryOverlay(currentHighlightedTerritory);
      }
      currentHighlightedTerritory = null;
      pickedTerritory = null;
      setWidgetActivation();
      return;
    }
    if (!pickedUnits.isEmpty() && !unitChoices.containsAll(pickedUnits)) {
      EventThreadJOptionPane.showMessageDialog(
          frame, "Invalid Units?!?", "Invalid Units?!?", JOptionPane.WARNING_MESSAGE);
      currentAction = null;
      pickedUnits.clear();
      setWidgetActivation();
      return;
    }
    if (pickedUnits.size() > Math.max(0, unitsPerPick)) {
      EventThreadJOptionPane.showMessageDialog(
          frame, "Too Many Units?!?", "Too Many Units?!?", JOptionPane.WARNING_MESSAGE);
      currentAction = null;
      pickedUnits.clear();
      setWidgetActivation();
      return;
    }
    if (pickedUnits.size() < unitsPerPick) {
      if (unitChoices.size() < unitsPerPick) {
        // if we have fewer units than the number we are supposed to pick, set it to all
        pickedUnits.addAll(unitChoices);
      } else if (unitChoices.stream()
          .allMatch(Matches.unitIsOfType(unitChoices.get(0).getType()))) {
        // if we have only 1 unit type, set it to that
        pickedUnits.clear();
        pickedUnits.addAll(CollectionUtils.getNMatches(unitChoices, unitsPerPick, it -> true));
      } else {
        EventThreadJOptionPane.showMessageDialog(
            frame,
            "Must Choose Units For This Territory",
            "Must Choose Units For This Territory",
            JOptionPane.WARNING_MESSAGE);
        currentAction = null;
        setWidgetActivation();
        return;
      }
    }
    currentAction = null;
    if (currentHighlightedTerritory != null) {
      getMap().clearTerritoryOverlay(currentHighlightedTerritory);
    }
    currentHighlightedTerritory = null;
    setWidgetActivation();
    release();
  }

  Tuple<Territory, Set<Unit>> waitForPickTerritoryAndUnits(
      final List<Territory> territoryChoices,
      final List<Unit> unitChoices,
      final int unitsPerPick) {
    Preconditions.checkArgument(unitsPerPick > 0, "unitsPerPick must be greater than 0");
    this.territoryChoices = territoryChoices;
    this.unitChoices = unitChoices;
    this.unitsPerPick = unitsPerPick;
    if (currentHighlightedTerritory != null) {
      getMap().clearTerritoryOverlay(currentHighlightedTerritory);
      currentHighlightedTerritory = null;
    }
    if (territoryChoices.size() == 1) {
      pickedTerritory = territoryChoices.get(0);
      currentHighlightedTerritory = pickedTerritory;
      getMap().setTerritoryOverlay(currentHighlightedTerritory, Color.WHITE, 200);
    }
    SwingUtilities.invokeLater(
        () -> {
          if (territoryChoices.size() > 1) {
            selectTerritoryAction.actionPerformed(null);
          } else if (unitChoices.size() > 1) {
            selectUnitsAction.actionPerformed(null);
          }
        });
    waitForRelease();
    return Tuple.of(this.pickedTerritory, this.pickedUnits);
  }

  private void setWidgetActivation() {
    SwingUtilities.invokeLater(
        () -> {
          if (!isActive()) {
            // current turn belongs to remote player or AI player
            actionLabel.setText("Waiting for other players...");
            doneButton.setEnabled(false);
            selectUnitsAction.setEnabled(false);
            selectTerritoryAction.setEnabled(false);
          } else {
            doneButton.setEnabled(currentAction == null);
            selectUnitsAction.setEnabled(currentAction == null);
            selectTerritoryAction.setEnabled(currentAction == null);
          }
        });
  }
}
