package games.strategy.triplea.ui;

import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.util.JFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DicePanel extends VBox {
  private final IUIContext m_uiContext;
  private final GameData m_data;

  public DicePanel(final IUIContext uiContext, final GameData data) {
    m_uiContext = uiContext;
    m_data = data;
  }

  public void clear() {
    getChildren().clear();
  }

  public void setDiceRollForBombing(final List<Die> dice, final int cost) {
    clear();
    getChildren().add(create(dice));
    getChildren().add(new Label("Cost:" + cost));
  }

  public void setDiceRoll(final DiceRoll diceRoll) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> setDiceRoll(diceRoll));
      return;
    }
    clear();
    for (int i = 1; i <= m_data.getDiceSides(); i++) {
      final List<Die> dice = diceRoll.getRolls(i);
      if (dice.isEmpty()) {
        continue;
      }
      getChildren().add(new Label("Rolled at " + (i) + ":"));
      getChildren().add(create(diceRoll.getRolls(i)));
    }
    getChildren().add(new Label("Total hits:" + diceRoll.getHits()));
  }

  private Node create(final List<Die> dice) {
    final HBox dicePanel = new HBox();
    for (final Die die : dice) {
      final int roll = die.getValue() + 1;
      Label dieLabel = new Label();
      dieLabel.setGraphic(new ImageView(
          JFXUtils.convertToFx((BufferedImage) m_uiContext.getDiceImageFactory().getDieImage(roll, die.getType()))));
      dicePanel.getChildren().add(new Label());
    }
    final ScrollPane scroll = new ScrollPane(dicePanel);
    scroll.setBorder(null);
    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    // we're adding to a box layout, so to prevent the component from
    // grabbing extra space, set the max height.
    // allow room for a dice and a scrollbar
    scroll.setMinSize(scroll.getMinWidth(), m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17);
    scroll.setMaxSize(scroll.getMaxWidth(), m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17);
    scroll.setPrefSize(scroll.getPrefWidth(), m_uiContext.getDiceImageFactory().DIE_HEIGHT + 17);
    return scroll;
  }
}
