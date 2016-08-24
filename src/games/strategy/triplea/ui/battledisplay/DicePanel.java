package games.strategy.triplea.ui.battledisplay;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.ui.SwingComponents;

public class DicePanel extends JPanel {
  private static final long serialVersionUID = -7544999867518263506L;
  private final IUIContext m_uiContext;
  private final GameData m_data;

  public DicePanel(final IUIContext uiContext, final GameData data) {
    m_uiContext = uiContext;
    m_data = data;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  }

  public void clear() {
    removeAll();
  }

  public void setDiceRollForBombing(final List<Die> dice, final int cost) {
    removeAll();
    add(create(dice));
    add(Box.createVerticalGlue());
    add(new JLabel("Cost:" + cost));
    invalidate();
  }

  public void setDiceRoll(final DiceRoll diceRoll) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> setDiceRoll(diceRoll));
      return;
    }
    removeAll();
    JPanel contents = new JPanel();
    contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));


    for (int i = 1; i <= m_data.getDiceSides(); i++) {
      final List<Die> dice = diceRoll.getRolls(i);
      if (dice.isEmpty()) {
        continue;
      }
      contents.add(new JLabel("Rolled at " + (i) + ":"));

      List<Die> allDice = diceRoll.getRolls(i);
      final int maxDicePerRow = ClientContext.battleOptionsSettings().maxBattleDicePerRow();
      for(int j = 0; j < allDice.size(); j += maxDicePerRow ) {
        contents.add(create(createSubList(allDice, maxDicePerRow, j)));
        contents.add(Box.createVerticalStrut(5));
      }
    }
    contents.add(Box.createVerticalGlue());
    add(SwingComponents.newJScrollPane(contents));
    add(new JLabel("Total hits:" + diceRoll.getHits()));
    validate();
    invalidate();
    repaint();
  }

  private static List<Die> createSubList(List<Die> allDice, int dicePerRow, int startIndex) {
    List<Die> subList = new ArrayList<>();
    for(int i = 0; i < dicePerRow && i+startIndex < allDice.size(); i ++ ) {
      subList.add(allDice.get(startIndex+i));
    }
    return subList;
  }

  private JComponent create(final List<Die> dice) {
    final JPanel dicePanel = new JPanel();
    dicePanel.setLayout(new BoxLayout(dicePanel, BoxLayout.X_AXIS));
    dicePanel.add(Box.createHorizontalStrut(20));
    for (final Die die : dice) {
      final int roll = die.getValue() + 1;
      dicePanel.add(new JLabel(m_uiContext.getDiceImageFactory().getDieIcon(roll, die.getType())));
      dicePanel.add(Box.createHorizontalStrut(2));
    }
    dicePanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    return dicePanel;
  }
}
