package games.strategy.triplea.ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.ui.SwingAction;

class DiceChooser extends JPanel {
  private static final long serialVersionUID = -3658408802544268998L;
  private final UiContext uiContext;
  private JPanel dicePanel;
  private final int[] random;
  private int diceCount = 0;
  private int numRolls = 0;
  private int hitAt = 0;
  private boolean hitOnlyIfEquals = false;
  private final Collection<JButton> buttons;
  private JButton undoButton;
  private JLabel diceCountLabel;
  // private final GameData m_data;
  private int diceSides = 6;

  DiceChooser(final UiContext uiContext, final int numRolls, final int hitAt, final boolean hitOnlyIfEquals,
      final int diceSides) {
    this.uiContext = uiContext;
    this.numRolls = numRolls;
    this.diceSides = diceSides;
    this.hitAt = hitAt;
    this.hitOnlyIfEquals = hitOnlyIfEquals;
    // m_data = data;
    buttons = new ArrayList<>(diceSides);
    random = new int[numRolls];
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    createComponents();
  }

  int[] getDice() {
    if (diceCount < numRolls) {
      return null;
    }
    return random;
  }

  private void addDie(final int roll) {
    final boolean hit = ((roll == hitAt) || (!hitOnlyIfEquals && (hitAt > 0) && (roll > hitAt)));
    final DieType dieType = hit ? DieType.HIT : DieType.MISS;
    dicePanel.add(new JLabel(uiContext.getDiceImageFactory().getDieIcon(roll, dieType)));
    dicePanel.add(Box.createHorizontalStrut(2));
    random[diceCount++] = roll - 1;
    updateDiceCount();
    validate();
    invalidate();
    repaint();
  }

  private void removeLastDie() {
    // remove the strut and the component
    final int lastIndex = dicePanel.getComponentCount() - 1;
    dicePanel.remove(lastIndex);
    dicePanel.remove(lastIndex - 1);
    diceCount--;
    updateDiceCount();
    validate();
    invalidate();
    repaint();
  }

  private void updateDiceCount() {
    final boolean showButtons = (diceCount < numRolls);
    for (final JButton button : buttons) {
      button.setEnabled(showButtons);
    }
    undoButton.setEnabled((diceCount > 0));
    diceCountLabel.setText("Dice remaining: " + (numRolls - diceCount));
  }

  private void createComponents() {
    final JPanel diceButtonPanel = new JPanel();
    diceButtonPanel.setLayout(new BoxLayout(diceButtonPanel, BoxLayout.X_AXIS));
    diceButtonPanel.add(Box.createHorizontalStrut(40));
    for (int roll = 1; roll <= diceSides; roll++) {
      final boolean hit = ((roll == hitAt) || (!hitOnlyIfEquals && (hitAt > 0) && (roll > hitAt)));
      diceButtonPanel.add(Box.createHorizontalStrut(4));
      final int dieNum = roll;
      final DieType dieType = hit ? DieType.HIT : DieType.MISS;
      final JButton button = new JButton(uiContext.getDiceImageFactory().getDieIcon(roll, dieType));
      button.addActionListener(e -> addDie(dieNum));
      buttons.add(button);
      button.setPreferredSize(new Dimension(DiceImageFactory.DIE_WIDTH + 4, DiceImageFactory.DIE_HEIGHT + 4));
      diceButtonPanel.add(button);
    }
    diceButtonPanel.add(Box.createHorizontalStrut(4));
    undoButton = new JButton(SwingAction.of("Undo", e -> removeLastDie()));
    diceButtonPanel.add(undoButton);
    diceButtonPanel.add(Box.createHorizontalStrut(40));
    diceCountLabel = new JLabel("Dice remaining:   ");
    final JPanel labelPanel = new JPanel();
    labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
    labelPanel.add(diceCountLabel);
    dicePanel = new JPanel();
    dicePanel.setBorder(BorderFactory.createLoweredBevelBorder());
    dicePanel.setLayout(new BoxLayout(dicePanel, BoxLayout.X_AXIS));
    final JScrollPane scroll = new JScrollPane(dicePanel);
    scroll.setBorder(null);
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    // we're adding to a box layout, so to prevent the component from
    // grabbing extra space, set the max height.
    // allow room for a dice and a scrollbar
    scroll.setMinimumSize(new Dimension(scroll.getMinimumSize().width, DiceImageFactory.DIE_HEIGHT + 17));
    scroll.setMaximumSize(new Dimension(scroll.getMaximumSize().width, DiceImageFactory.DIE_HEIGHT + 17));
    scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, DiceImageFactory.DIE_HEIGHT + 17));
    add(scroll);
    add(Box.createVerticalStrut(8));
    add(labelPanel);
    add(Box.createVerticalStrut(8));
    add(diceButtonPanel);
    updateDiceCount();
  }
}
