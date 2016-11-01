package games.strategy.triplea.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.util.JFXUtils;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class TechResultsDisplay extends GridPane {

  public TechResultsDisplay(final TechResults msg, final IUIContext uiContext, final GameData data) {
    final IUIContext m_uiContext = uiContext;
    add(new Label("You got " + msg.getHits() + " hit" + (msg.getHits() != 1 ? "s" : "") + "."), new GridBagConstraints(
        0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
    if (msg.getHits() != 0) {
      add(new Label("Technologies discovered:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      final ListView<String> list = new ListView<>(FXCollections.observableArrayList(msg.getAdvances()));
      add(list, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(0, 0, 5, 0), 0, 0));
      list.setBackground(this.getBackground());
    }
    final HBox dice = new HBox();
    final int remainder = msg.getRemainder();
    for (int i = 0; i < msg.getRolls().length; i++) {
      // add 1 since dice are 0 based
      final int roll = msg.getRolls()[i] + 1;
      Label die = new Label();
      if (remainder > 0) {
        die.setGraphic(new ImageView(JFXUtils.convertToFx((BufferedImage) ((ImageIcon) m_uiContext.getDiceImageFactory()
            .getDieIcon(roll, roll <= remainder ? Die.DieType.HIT : Die.DieType.MISS)).getImage())));
      } else {
        die.setGraphic(new ImageView(
            JFXUtils.convertToFx((BufferedImage) ((ImageIcon) m_uiContext.getDiceImageFactory().getDieIcon(roll,
                roll == data.getDiceSides() ? Die.DieType.HIT : Die.DieType.MISS)).getImage())));
      }
      dice.getChildren().add(die);
      dice.setMaxSize(200, dice.getMaxHeight());
    }
    final ScrollPane diceScroll = new ScrollPane(dice);
    diceScroll.setBorder(null);
    add(diceScroll, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, 5, 0), 0, 0));
  }
}
