package games.strategy.triplea.ui;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.JFXUtils;
import games.strategy.util.Match;
import games.strategy.util.Tuple;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

/**
 * For choosing territories and units for them, during RandomStartDelegate.
 */
public class PickTerritoryAndUnitsPanel extends ActionPanel {
  private final Label m_actionLabel = new Label();
  private Button m_doneButton = null;
  private Button m_selectTerritoryButton = null;
  private Button m_selectUnitsButton = null;
  private Territory m_pickedTerritory = null;
  private Set<Unit> m_pickedUnits = new HashSet<>();
  private List<Territory> m_territoryChoices = null;
  private List<Unit> m_unitChoices = null;
  private int m_unitsPerPick = 1;
  private EventHandler<ActionEvent> m_currentAction = null;
  private Territory m_currentHighlightedTerritory = null;

  public PickTerritoryAndUnitsPanel(final GameData data, final MapPanel map) {
    super(data, map);
  }

  @Override
  public String toString() {
    return "Pick Territory and Units";
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    m_pickedTerritory = null;
    m_pickedUnits = new HashSet<>();
    m_currentAction = null;
    m_currentHighlightedTerritory = null;
    Platform.runLater(() -> {
      getChildren().clear();
      m_actionLabel.setText(id.getName() + " Pick Territory and Units");
      getChildren().add(m_actionLabel);
      m_selectTerritoryButton = JFXUtils.getButtonWithAction(selectTerritoryAction);
      getChildren().add(m_selectTerritoryButton);
      m_selectUnitsButton = JFXUtils.getButtonWithAction(selectUnitsAction);
      getChildren().add(m_selectUnitsButton);
      m_doneButton = JFXUtils.getButtonWithAction(doneAction);
      getChildren().add(m_doneButton);
      SwingUtilities.invokeLater(m_selectTerritoryButton::requestFocus);
    });
  }

  public Tuple<Territory, Set<Unit>> waitForPickTerritoryAndUnits(final List<Territory> territoryChoices,
      final List<Unit> unitChoices, final int unitsPerPick) {
    m_territoryChoices = territoryChoices;
    m_unitChoices = unitChoices;
    m_unitsPerPick = unitsPerPick;
    if (m_currentHighlightedTerritory != null) {
      getMap().clearTerritoryOverlay(m_currentHighlightedTerritory);
      m_currentHighlightedTerritory = null;
    }
    if (territoryChoices.size() == 1) {
      m_pickedTerritory = territoryChoices.get(0);
      m_currentHighlightedTerritory = m_pickedTerritory;
      getMap().setTerritoryOverlay(m_currentHighlightedTerritory, Color.WHITE, 200);
    }
    Platform.runLater(() -> {
      if (territoryChoices.size() > 1) {
        selectTerritoryAction.handle(null);
      } else if (unitChoices.size() > 1) {
        selectUnitsAction.handle(null);
      }
    });
    waitForRelease();
    return Tuple.of(this.m_pickedTerritory, this.m_pickedUnits);
  }

  private void setWidgetActivation() {
    // SwingUtilities.invokeLater(() -> {
    // if (!getActive()) {
    // // current turn belongs to remote player or AI player
    // DoneAction.setEnabled(false);
    // SelectUnitsAction.setEnabled(false);
    // selectTerritoryAction.setEnabled(false);
    // } else {
    // DoneAction.setEnabled(m_currentAction == null);
    // SelectUnitsAction.setEnabled(m_currentAction == null);
    // selectTerritoryAction.setEnabled(m_currentAction == null);
    // }
    // });
  }

  private final EventHandler<ActionEvent> doneAction = e -> {
    // m_currentAction = DoneAction;
    setWidgetActivation();
    if (m_pickedTerritory == null || !m_territoryChoices.contains(m_pickedTerritory)) {
      JFXUtils.showWarningDialog("Warning!", "Must Pick An Unowned Territory", "");
      m_currentAction = null;
      if (m_currentHighlightedTerritory != null) {
        getMap().clearTerritoryOverlay(m_currentHighlightedTerritory);
      }
      m_currentHighlightedTerritory = null;
      m_pickedTerritory = null;
      setWidgetActivation();
      return;
    }
    if (!m_pickedUnits.isEmpty() && !m_unitChoices.containsAll(m_pickedUnits)) {
      JFXUtils.showWarningDialog("Invalid Units?!?", "Invalid Units?!?", "");
      m_currentAction = null;
      m_pickedUnits.clear();
      setWidgetActivation();
      return;
    }
    if (m_pickedUnits.size() > Math.max(0, m_unitsPerPick)) {
      JFXUtils.showWarningDialog("Too Many Units?!?", "Too Many Units?!?", "");
      m_currentAction = null;
      m_pickedUnits.clear();
      setWidgetActivation();
      return;
    }
    if (m_pickedUnits.size() < m_unitsPerPick) {
      if (m_unitChoices.size() < m_unitsPerPick) {
        // if we have fewer units than the number we are supposed to pick, set it to all
        m_pickedUnits.addAll(m_unitChoices);
      } else if (Match.allMatch(m_unitChoices, Matches.unitIsOfType(m_unitChoices.get(0).getType()))) {
        // if we have only 1 unit type, set it to that
        m_pickedUnits.clear();
        m_pickedUnits.addAll(Match.getNMatches(m_unitChoices, m_unitsPerPick, Match.getAlwaysMatch()));
      } else {
        JFXUtils.showWarningDialog("Must Choose Units For This Territory", "Must Choose Units For This Territory",
            "");
        m_currentAction = null;
        setWidgetActivation();
        return;
      }
    }
    m_currentAction = null;
    if (m_currentHighlightedTerritory != null) {
      getMap().clearTerritoryOverlay(m_currentHighlightedTerritory);
    }
    m_currentHighlightedTerritory = null;
    setWidgetActivation();
    release();
  };
  private final EventHandler<ActionEvent> selectUnitsAction = e -> {
//     m_currentAction = selectUnitsAction;
    setWidgetActivation();
    final UnitChooser unitChooser = new UnitChooser(m_unitChoices, Collections.emptyMap(),
        getData(), false, getMap().getUIContext());
    unitChooser.setMaxAndShowMaxButton(m_unitsPerPick);
    JFXUtils.getDialogWithContent(unitChooser, AlertType.INFORMATION, "Select Units", "Select Units", "").showAndWait()
        .filter(ButtonType.OK::equals).ifPresent(b -> {
          m_pickedUnits.clear();
          m_pickedUnits.addAll(unitChooser.getSelected());
        });
    m_currentAction = null;
    setWidgetActivation();
  };

  private final MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener() {
    @Override
    public void territorySelected(final Territory territory, final MouseDetails md) {
      if (territory == null) {
        return;
      }
      if (m_currentAction == selectTerritoryAction) {
        if (territory == null || !m_territoryChoices.contains(territory)) {
          JFXUtils.showWarningDialog("Must Pick An Unowned Territory",
              "Must Pick An Unowned Territory (will have a white highlight)", "");
          return;
        }
        m_pickedTerritory = territory;
        SwingUtilities.invokeLater(() -> {
          getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
          m_currentAction = null;
          setWidgetActivation();
        });
      } else {
        System.err.println("Should not be able to select a territory outside of the SelectTerritoryAction.");
      }
    }

    @Override
    public void mouseMoved(final Territory territory, final MouseDetails md) {
      if (!getActive()) {
        System.err.println("Should not be able to select a territory when inactive.");
        return;
      }
      if (territory != null) {
        // highlight territory
        if (m_currentAction == selectTerritoryAction) {
          if (m_currentHighlightedTerritory != territory) {
            if (m_currentHighlightedTerritory != null) {
              getMap().clearTerritoryOverlay(m_currentHighlightedTerritory);
            }
            m_currentHighlightedTerritory = territory;
            if (m_territoryChoices.contains(m_currentHighlightedTerritory)) {
              getMap().setTerritoryOverlay(m_currentHighlightedTerritory, Color.WHITE, 200);
            } else {
              getMap().setTerritoryOverlay(m_currentHighlightedTerritory, Color.RED, 200);
            }
            getMap().repaint();
          }
        }
      }
    }
  };


  private final EventHandler<ActionEvent> selectTerritoryAction = e -> {
    // m_currentAction = selectTerritoryAction;
    setWidgetActivation();
    getMap().addMapSelectionListener(MAP_SELECTION_LISTENER);
  };
}
