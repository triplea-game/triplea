package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

public class TechPanel extends ActionPanel {
  private static final long serialVersionUID = -6477919141575138007L;
  private final JLabel actionLabel = new JLabel();
  private TechRoll techRoll;
  private int currTokens = 0;
  private int quantity;
  private IntegerMap<PlayerID> whoPaysHowMuch = null;

  /** Creates new TechPanel. */
  public TechPanel(final GameData data, final MapPanel map) {
    super(data, map);
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    SwingUtilities.invokeLater(() -> {
      removeAll();
      actionLabel.setText(id.getName() + " Tech Roll");
      add(actionLabel);
      if (isWW2V3TechModel()) {
        add(new JButton(getTechTokenAction));
        add(new JButton(justRollTech));
      } else {
        add(new JButton(getTechRollsAction));
        add(new JButton(dontBother));
      }
    });
  }

  @Override
  public String toString() {
    return "TechPanel";
  }

  TechRoll waitForTech() {
    if (getAvailableTechs().isEmpty()) {
      return null;
    }
    waitForRelease();
    if (techRoll == null) {
      return null;
    }
    if (techRoll.getRolls() == 0) {
      return null;
    }
    return techRoll;
  }

  private List<TechAdvance> getAvailableTechs() {
    final Collection<TechAdvance> currentAdvances = TechTracker.getCurrentTechAdvances(getCurrentPlayer(), getData());
    final Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(getData(), getCurrentPlayer());
    return CollectionUtils.difference(allAdvances, currentAdvances);
  }

  private List<TechnologyFrontier> getAvailableCategories() {
    final Collection<TechnologyFrontier> currentAdvances =
        TechTracker.getFullyResearchedPlayerTechCategories(getCurrentPlayer());
    final Collection<TechnologyFrontier> allAdvances = TechAdvance.getPlayerTechCategories(getCurrentPlayer());
    return CollectionUtils.difference(allAdvances, currentAdvances);
  }

  private final Action getTechRollsAction = SwingAction.of("Roll Tech...", e -> {
    TechAdvance advance = null;
    if (isWW2V2() || (isSelectableTechRoll() && !isWW2V3TechModel())) {
      final List<TechAdvance> available = getAvailableTechs();
      if (available.isEmpty()) {
        JOptionPane.showMessageDialog(TechPanel.this, "No more available tech advances");
        return;
      }
      final JList<TechAdvance> list = new JList<>(SwingComponents.newListModel(available));
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(list, BorderLayout.CENTER);
      panel.add(new JLabel("Select the tech you want to roll for"), BorderLayout.NORTH);
      list.setSelectedIndex(0);
      final int choice = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(TechPanel.this), panel,
          "Select advance", JOptionPane.PLAIN_MESSAGE);
      if (choice != JOptionPane.OK_OPTION) {
        return;
      }
      advance = list.getSelectedValue();
    }
    final int pus = getCurrentPlayer().getResources().getQuantity(Constants.PUS);
    final String message = "Roll Tech";
    final TechRollPanel techRollPanel = new TechRollPanel(pus, getCurrentPlayer());
    final int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), techRollPanel, message,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
    if (choice != JOptionPane.OK_OPTION) {
      return;
    }
    final int quantity = techRollPanel.getValue();
    if (advance == null) {
      techRoll = new TechRoll(null, quantity);
    } else {
      try {
        getData().acquireReadLock();
        final TechnologyFrontier front = new TechnologyFrontier("", getData());
        front.addAdvance(advance);
        techRoll = new TechRoll(front, quantity);
      } finally {
        getData().releaseReadLock();
      }
    }
    release();
  });

  private final Action dontBother = SwingAction.of("Done", e -> {
    techRoll = null;
    release();
  });

  private final Action getTechTokenAction = SwingAction.of("Buy Tech Tokens...", e -> {
    final PlayerID currentPlayer = getCurrentPlayer();
    currTokens = currentPlayer.getResources().getQuantity(Constants.TECH_TOKENS);
    // Notify user if there are no more techs to acheive
    final List<TechnologyFrontier> techCategories = getAvailableCategories();


    if (techCategories.isEmpty()) {
      JOptionPane.showMessageDialog(TechPanel.this, "No more available tech advances");
      return;
    }
    final JList<TechnologyFrontier> list = new JList<TechnologyFrontier>(SwingComponents.newListModel(techCategories)) {
      private static final long serialVersionUID = 35094445315520702L;

      @Override
      public String getToolTipText(final MouseEvent e) {
        final int index = locationToIndex(e.getPoint());
        return (-1 < index) ? getTechListToolTipText(getModel().getElementAt(index)) : null;
      }
    };
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(list, BorderLayout.CENTER);
    panel.add(new JLabel("Select which tech chart you want to roll for"), BorderLayout.NORTH);
    list.setSelectedIndex(0);
    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(TechPanel.this), panel, "Select chart",
        JOptionPane.PLAIN_MESSAGE);
    final TechnologyFrontier category = list.getSelectedValue();

    final int pus = currentPlayer.getResources().getQuantity(Constants.PUS);
    // see if anyone will help us to pay
    Collection<PlayerID> helpPay;
    final PlayerAttachment pa = PlayerAttachment.get(currentPlayer);
    if (pa != null) {
      helpPay = pa.getHelpPayTechCost();
    } else {
      helpPay = null;
    }
    final TechTokenPanel techTokenPanel = new TechTokenPanel(pus, currTokens, currentPlayer, helpPay);
    final String message = "Purchase Tech Tokens";
    final int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), techTokenPanel, message,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
    if (choice != JOptionPane.OK_OPTION) {
      return;
    }
    quantity = techTokenPanel.getValue();
    whoPaysHowMuch = techTokenPanel.getWhoPaysHowMuch();
    currTokens += quantity;
    techRoll = new TechRoll(category, currTokens, quantity, whoPaysHowMuch);
    techRoll.setNewTokens(quantity);
    release();


  });

  private final Action justRollTech = SwingAction.of("Done/Roll Current Tokens", e -> {
    currTokens = getCurrentPlayer().getResources().getQuantity(Constants.TECH_TOKENS);
    // If this player has tokens, roll them.
    if (currTokens > 0) {
      final List<TechnologyFrontier> techCategories = getAvailableCategories();
      if (techCategories.isEmpty()) {
        return;
      }
      final JList<TechnologyFrontier> list =
          new JList<TechnologyFrontier>(SwingComponents.newListModel(techCategories)) {
            private static final long serialVersionUID = -8415987764855418565L;

            @Override
            public String getToolTipText(final MouseEvent e) {
              final int index = locationToIndex(e.getPoint());
              return (-1 < index) ? getTechListToolTipText(getModel().getElementAt(index)) : null;
            }
          };
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(list, BorderLayout.CENTER);
      panel.add(new JLabel("Select which tech chart you want to roll for"), BorderLayout.NORTH);
      list.setSelectedIndex(0);
      JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(TechPanel.this), panel, "Select chart",
          JOptionPane.PLAIN_MESSAGE);
      final TechnologyFrontier category = list.getSelectedValue();
      techRoll = new TechRoll(category, currTokens);
    } else {
      techRoll = null;
    }
    release();

  });

  private static String getTechListToolTipText(final TechnologyFrontier techCategory) {
    final List<TechAdvance> techList = techCategory.getTechs();
    if (techList.size() <= 1) {
      return null;
    }
    final Collection<TechAdvance> listedAlready = new HashSet<>();
    final StringBuilder strTechCategory = new StringBuilder("Available Techs:  ");
    final Iterator<TechAdvance> iterTechList = techList.iterator();
    while (iterTechList.hasNext()) {
      final TechAdvance advance = iterTechList.next();
      if (listedAlready.contains(advance)) {
        continue;
      }
      listedAlready.add(advance);
      final int freq = Collections.frequency(techList, advance);
      strTechCategory.append(advance.getName()).append((freq > 1) ? (" (" + freq + "/" + techList.size() + ")") : "");
      if (iterTechList.hasNext()) {
        strTechCategory.append(", ");
      }
    }
    return strTechCategory.toString();
  }

  private static final class TechRollPanel extends JPanel {
    private static final long serialVersionUID = -3794742986339086059L;
    int pus;
    PlayerID player;
    JLabel left = new JLabel();
    ScrollableTextField textField;

    TechRollPanel(final int pus, final PlayerID player) {
      setLayout(new GridBagLayout());
      this.pus = pus;
      this.player = player;
      final JLabel title = new JLabel("Select the number of tech rolls:");
      title.setBorder(new EmptyBorder(5, 5, 5, 5));
      textField = new ScrollableTextField(0, pus / TechTracker.getTechCost(player));
      final ScrollableTextFieldListener listener =
          stf -> setLabel(this.pus - (TechTracker.getTechCost(this.player) * textField.getValue()));
      textField.addChangeListener(listener);
      final JLabel costLabel = new JLabel("x" + TechTracker.getTechCost(this.player));
      setLabel(pus);
      final int space = 0;
      add(title, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(5, 5, space, space), 0, 0));
      add(textField, new GridBagConstraints(0, 1, 1, 1, 0.5, 1, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(8, 10, space, space), 0, 0));
      add(costLabel, new GridBagConstraints(1, 1, 1, 1, 0.5, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(8, 5, space, 2), 0, 0));
      add(left, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(10, 5, space, space), 0, 0));
    }

    private void setLabel(final int pus) {
      left.setText("Left to spend:" + pus);
    }

    int getValue() {
      return textField.getValue();
    }
  }

  private static final class TechTokenPanel extends JPanel {
    private static final long serialVersionUID = 332026624893335993L;
    int totalPus;
    int playerPus;
    final ScrollableTextField playerPuField;
    PlayerID player;
    JLabel left = new JLabel();
    JLabel right = new JLabel();
    JLabel totalCost = new JLabel();
    ScrollableTextField textField;
    HashMap<PlayerID, ScrollableTextField> whoPaysTextFields = null;

    TechTokenPanel(final int pus, final int currTokens, final PlayerID player, final Collection<PlayerID> helpPay) {
      playerPus = pus;
      totalPus = pus;
      if ((helpPay != null) && !helpPay.isEmpty()) {
        helpPay.remove(player);
        for (final PlayerID p : helpPay) {
          totalPus += p.getResources().getQuantity(Constants.PUS);
        }
      }
      playerPuField = new ScrollableTextField(0, totalPus);
      playerPuField.setEnabled(false);
      setLayout(new GridBagLayout());
      this.player = player;
      final JLabel title = new JLabel("Select the number of tech tokens to purchase:");
      title.setBorder(new EmptyBorder(5, 5, 5, 5));
      final int techCost = TechTracker.getTechCost(this.player);
      textField = new ScrollableTextField(0, totalPus / techCost);
      final ScrollableTextFieldListener listener = stf -> {
        setLabel(TechTracker.getTechCost(this.player) * textField.getValue());
        setWidgetActivation();
      };
      textField.addChangeListener(listener);
      final JLabel costLabel = new JLabel("x" + techCost + " cost per token");
      setLabel(0);
      setTokens(currTokens);
      final int space = 0;
      add(title, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(5, 5, space, space), 0, 0));
      add(textField, new GridBagConstraints(0, 1, 1, 1, 0.5, 1, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(8, 10, space, space), 0, 0));
      add(costLabel, new GridBagConstraints(1, 1, 1, 1, 0.5, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(8, 5, space, 2), 0, 0));
      add(left, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(10, 5, space, space), 0, 0));
      add(right, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(10, 130, space, space), 0, 0));
      add(totalCost, new GridBagConstraints(0, 3, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(10, 5, space, space), 0, 0));
      if ((helpPay != null) && !helpPay.isEmpty()) {
        if (whoPaysTextFields == null) {
          whoPaysTextFields = new HashMap<>();
        }
        helpPay.remove(player);
        int row = 4;
        add(new JLabel("Nations Paying How Much:"),
            new GridBagConstraints(0, row, 1, 1, 0.5, 1, GridBagConstraints.EAST,
                GridBagConstraints.NONE, new Insets(30, 6, 6, 6), 0, 0));
        row++;
        add(new JLabel(player.getName()), new GridBagConstraints(0, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
            GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
        add(playerPuField, new GridBagConstraints(1, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
            GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
        add(new JLabel("PUs"), new GridBagConstraints(2, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
            GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
        row++;
        for (final PlayerID p : helpPay) {
          final int helperPUs = p.getResources().getQuantity(Constants.PUS);
          if (helperPUs > 0) {
            final ScrollableTextField whoPaysTextField = new ScrollableTextField(0, helperPUs);
            whoPaysTextField.addChangeListener(setWidgetAction());
            whoPaysTextFields.put(p, whoPaysTextField);
            // TODO: force players to pay if it goes above the cost m_player can afford.
            add(new JLabel(p.getName()), new GridBagConstraints(0, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
            add(whoPaysTextField, new GridBagConstraints(1, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
            add(new JLabel("PUs"), new GridBagConstraints(2, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
            row++;
          }
        }
      }
    }

    private void setWidgetActivation() {
      if ((whoPaysTextFields == null) || whoPaysTextFields.isEmpty()) {
        return;
      }
      final int cost = TechTracker.getTechCost(player) * textField.getValue();
      int totalPaidByOthers = 0;
      for (final Entry<PlayerID, ScrollableTextField> entry : whoPaysTextFields.entrySet()) {
        totalPaidByOthers += Math.max(0, entry.getValue().getValue());
      }
      final int totalPaidByPlayer = Math.max(0, cost - totalPaidByOthers);
      int amountOver = -1 * (playerPus - totalPaidByPlayer);
      final Iterator<Entry<PlayerID, ScrollableTextField>> otherPayers = whoPaysTextFields.entrySet().iterator();
      while ((amountOver > 0) && otherPayers.hasNext()) {
        final Entry<PlayerID, ScrollableTextField> entry = otherPayers.next();
        int current = entry.getValue().getValue();
        final int max = entry.getValue().getMax();
        if (current < max) {
          final int canAdd = Math.min(max - current, amountOver);
          amountOver -= canAdd;
          current += canAdd;
          entry.getValue().setValue(current);
        }
      }
      // now check if we are negative
      totalPaidByOthers = 0;
      for (final Entry<PlayerID, ScrollableTextField> entry : whoPaysTextFields.entrySet()) {
        totalPaidByOthers += Math.max(0, entry.getValue().getValue());
      }
      int amountUnder = -1 * (cost - totalPaidByOthers);
      final Iterator<Entry<PlayerID, ScrollableTextField>> otherPayers2 = whoPaysTextFields.entrySet().iterator();
      while ((amountUnder > 0) && otherPayers2.hasNext()) {
        final Entry<PlayerID, ScrollableTextField> entry = otherPayers2.next();
        int current = entry.getValue().getValue();
        if (current > 0) {
          final int canSubtract = Math.min(current, amountUnder);
          amountUnder -= canSubtract;
          current -= canSubtract;
          entry.getValue().setValue(current);
        }
      }
      playerPuField.setValue(Math.max(0, Math.min(playerPus, totalPaidByPlayer)));
    }

    private void setLabel(final int cost) {
      left.setText("Left to Spend:  " + (totalPus - cost));
      totalCost.setText("Total Cost:  " + cost);
    }

    private void setTokens(final int tokens) {
      right.setText("Current token count:  " + tokens);
    }

    private ScrollableTextFieldListener setWidgetAction() {
      return stf -> setWidgetActivation();
    }

    int getValue() {
      return textField.getValue();
    }

    IntegerMap<PlayerID> getWhoPaysHowMuch() {
      final int techCost = TechTracker.getTechCost(player);
      final int numberOfTechRolls = getValue();
      final int totalCost = numberOfTechRolls * techCost;
      final IntegerMap<PlayerID> whoPaysHowMuch = new IntegerMap<>();
      if ((whoPaysTextFields == null) || whoPaysTextFields.isEmpty()) {
        whoPaysHowMuch.put(player, totalCost);
      } else {
        int runningTotal = 0;
        for (final Entry<PlayerID, ScrollableTextField> entry : whoPaysTextFields.entrySet()) {
          final int value = entry.getValue().getValue();
          whoPaysHowMuch.put(entry.getKey(), value);
          runningTotal += value;
        }
        if (!whoPaysTextFields.containsKey(player)) {
          whoPaysHowMuch.put(player, Math.max(0, totalCost - runningTotal));
        }
      }
      return whoPaysHowMuch;
    }
  }
}
