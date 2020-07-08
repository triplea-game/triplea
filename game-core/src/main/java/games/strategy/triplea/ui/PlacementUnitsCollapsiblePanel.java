package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.util.UnitSeparator;
import java.util.Collection;
import java.util.List;
import javax.swing.SwingUtilities;
import lombok.Getter;
import org.triplea.swing.CollapsiblePanel;

class PlacementUnitsCollapsiblePanel {
  private final SimpleUnitPanel unitsToPlacePanel;

  @Getter private final CollapsiblePanel panel;
  private final GameData gameData;

  PlacementUnitsCollapsiblePanel(final GameData gameData, final UiContext uiContext) {
    this.gameData = gameData;
    unitsToPlacePanel =
        new SimpleUnitPanel(
            uiContext, SimpleUnitPanel.Style.SMALL_ICONS_WRAPPED_WITH_LABEL_WHEN_EMPTY);
    panel = new CollapsiblePanel(unitsToPlacePanel, "Units To Place");
    panel.setVisible(false);
    gameData.addGameDataEventListener(GameDataEvent.GAME_STEP_CHANGED, this::updateStep);
  }

  private void updateStep() {
    final GameStep step = gameData.getSequence().getStep();

    // Compute data now so that the swing event task is working with unmodifiable data.
    final boolean shouldRenderPanelForThisGameStep =
        !GameStep.isPlaceStep(step.getName())
            && !isInitializationStep(step)
            && stepIsAfterPurchaseAndBeforePlacement(step);

    final Collection<Unit> playerUnits =
        shouldRenderPanelForThisGameStep ? List.copyOf(step.getPlayerId().getUnits()) : List.of();

    SwingUtilities.invokeLater(
        () -> {
          if (shouldRenderPanelForThisGameStep && !playerUnits.isEmpty()) {
            unitsToPlacePanel.setUnitsFromCategories(UnitSeparator.categorize(playerUnits));
            panel.setVisible(true);
          } else {
            panel.setVisible(false);
          }
        });
  }

  private static boolean isInitializationStep(final GameStep gameStep) {
    return gameStep.getPlayerId() == null;
  }

  private boolean stepIsAfterPurchaseAndBeforePlacement(final GameStep step) {
    final GamePlayer currentPlayer = step.getPlayerId();

    for (int i = gameData.getSequence().getStepIndex() - 1; i >= 0; i--) {
      final GameStep previousStep = gameData.getSequence().getStep(i);

      if (isNotPlayersTurn(currentPlayer, previousStep)
          || GameStep.isPlaceStep(previousStep.getName())) {
        return false;
      }

      if (GameStep.isPurchase(previousStep.getName())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNotPlayersTurn(final GamePlayer currentPlayer, final GameStep step) {
    return !currentPlayer.equals(step.getPlayerId());
  }
}
