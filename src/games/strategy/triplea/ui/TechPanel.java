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
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.common.swing.SwingAction;
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
import games.strategy.util.IntegerMap;
import games.strategy.util.Util;

public class TechPanel extends ActionPanel {
  private static final long serialVersionUID = -6477919141575138007L;
  private final JLabel m_actionLabel = new JLabel();
  private TechRoll m_techRoll;
  private int m_currTokens = 0;
  private int m_quantity;
  private IntegerMap<PlayerID> m_whoPaysHowMuch = null;

  /** Creates new BattlePanel */
  public TechPanel(final GameData data, final MapPanel map) {
    super(data, map);
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        removeAll();
        m_actionLabel.setText(id.getName() + " Tech Roll");
        add(m_actionLabel);
        if (isWW2V3TechModel()) {
          add(new JButton(GetTechTokenAction));
          add(new JButton(JustRollTech));
        } else {
          add(new JButton(GetTechRollsAction));
          add(new JButton(DontBother));
        }
      }
    });
  }

  @Override
  public String toString() {
    return "TechPanel";
  }

  public TechRoll waitForTech() {
    if (getAvailableTechs().isEmpty()) {
      return null;
    }
    waitForRelease();
    if (m_techRoll == null) {
      return null;
    }
    if (m_techRoll.getRolls() == 0) {
      return null;
    }
    return m_techRoll;
  }

  private List<TechAdvance> getAvailableTechs() {
    final Collection<TechAdvance> currentAdvances = TechTracker.getCurrentTechAdvances(getCurrentPlayer(), getData());
    final Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(getData(), getCurrentPlayer());
    return Util.difference(allAdvances, currentAdvances);
  }

  private List<TechnologyFrontier> getAvailableCategories() {
    final Collection<TechnologyFrontier> currentAdvances =
        TechTracker.getFullyResearchedPlayerTechCategories(getData(), getCurrentPlayer());
    final Collection<TechnologyFrontier> allAdvances =
        TechAdvance.getPlayerTechCategories(getData(), getCurrentPlayer());
    return Util.difference(allAdvances, currentAdvances);
  }

  private final Action GetTechRollsAction = SwingAction.of("Roll Tech...", e -> {
    TechAdvance advance = null;
    if (isWW2V2() || (isSelectableTechRoll() && !isWW2V3TechModel())) {
      final List<TechAdvance> available = getAvailableTechs();
      if (available.isEmpty()) {
        JOptionPane.showMessageDialog(TechPanel.this, "No more available tech advances");
        return;
      }
      final JList<TechAdvance> list = new JList<>(new Vector<>(available));
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
    final int PUs = getCurrentPlayer().getResources().getQuantity(Constants.PUS);
    final String message = "Roll Tech";
    final TechRollPanel techRollPanel = new TechRollPanel(PUs, getCurrentPlayer());
    final int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), techRollPanel, message,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
    if (choice != JOptionPane.OK_OPTION) {
      return;
    }
    final int quantity = techRollPanel.getValue();
    if (advance == null) {
      m_techRoll = new TechRoll(null, quantity);
    } else {
      try {
        getData().acquireReadLock();
        final TechnologyFrontier front = new TechnologyFrontier("", getData());
        front.addAdvance(advance);
        m_techRoll = new TechRoll(front, quantity);
      } finally {
        getData().releaseReadLock();
      }
    }
    release();
  });
  private final Action DontBother = SwingAction.of("Done", e -> {
    m_techRoll = null;
    release();
  });
  private final Action GetTechTokenAction = SwingAction.of("Buy Tech Tokens...", e -> {
    final PlayerID currentPlayer = getCurrentPlayer();
    m_currTokens = currentPlayer.getResources().getQuantity(Constants.TECH_TOKENS);
    // Notify user if there are no more techs to acheive
    final List<TechnologyFrontier> techCategories = getAvailableCategories();


    if (techCategories.isEmpty()) {
      JOptionPane.showMessageDialog(TechPanel.this, "No more available tech advances");
      return;
    }
    TechnologyFrontier category = null;
    final JList<TechnologyFrontier> list = new JList<TechnologyFrontier>(new Vector<>(techCategories)) {
      private static final long serialVersionUID = 35094445315520702L;

      @Override
      public String getToolTipText(final MouseEvent e) {
        final int index = locationToIndex(e.getPoint());
        if (-1 < index) {
          return getTechListToolTipText(getModel().getElementAt(index));
        } else {
          return null;
        }
      }
    };
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(list, BorderLayout.CENTER);
    panel.add(new JLabel("Select which tech chart you want to roll for"), BorderLayout.NORTH);
    list.setSelectedIndex(0);
    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(TechPanel.this), panel, "Select chart",
        JOptionPane.PLAIN_MESSAGE);
    category = list.getSelectedValue();

    final int PUs = currentPlayer.getResources().getQuantity(Constants.PUS);
    final String message = "Purchase Tech Tokens";
    // see if anyone will help us to pay
    Collection<PlayerID> helpPay;
    final PlayerAttachment pa = PlayerAttachment.get(currentPlayer);
    if (pa != null) {
      helpPay = pa.getHelpPayTechCost();
    } else {
      helpPay = null;
    }
    final TechTokenPanel techTokenPanel = new TechTokenPanel(PUs, m_currTokens, currentPlayer, helpPay);
    final int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), techTokenPanel, message,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
    if (choice != JOptionPane.OK_OPTION) {
      return;
    }
    m_quantity = techTokenPanel.getValue();
    m_whoPaysHowMuch = techTokenPanel.getWhoPaysHowMuch();
    m_currTokens += m_quantity;
    m_techRoll = new TechRoll(category, m_currTokens, m_quantity, m_whoPaysHowMuch);
    m_techRoll.setNewTokens(m_quantity);
    release();


  });
  private final Action JustRollTech = SwingAction.of("Done/Roll Current Tokens", e -> {
    m_currTokens = getCurrentPlayer().getResources().getQuantity(Constants.TECH_TOKENS);
    // If this player has tokens, roll them.
    if (m_currTokens > 0) {
      final List<TechnologyFrontier> techCategories = getAvailableCategories();
      if (techCategories.isEmpty()) {
        return;
      }
      TechnologyFrontier category = null;
      final JList<TechnologyFrontier> list = new JList<TechnologyFrontier>(new Vector<>(techCategories)) {
        private static final long serialVersionUID = -8415987764855418565L;

        @Override
        public String getToolTipText(final MouseEvent e) {
          final int index = locationToIndex(e.getPoint());
          if (-1 < index) {
            return getTechListToolTipText(getModel().getElementAt(index));
          } else {
            return null;
          }
        }
      };
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(list, BorderLayout.CENTER);
      panel.add(new JLabel("Select which tech chart you want to roll for"), BorderLayout.NORTH);
      list.setSelectedIndex(0);
      JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(TechPanel.this), panel, "Select chart",
          JOptionPane.PLAIN_MESSAGE);
      category = list.getSelectedValue();
      m_techRoll = new TechRoll(category, m_currTokens);
    } else {
      m_techRoll = null;
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
      } else {
        listedAlready.add(advance);
      }
      final int freq = Collections.frequency(techList, advance);
      strTechCategory.append(advance.getName()).append(freq > 1 ? " (" + freq + "/" + techList.size() + ")" : "");
      if (iterTechList.hasNext()) {
        strTechCategory.append(", ");
      }
    }
    return strTechCategory.toString();
  }
}


class TechRollPanel extends JPanel {
  private static final long serialVersionUID = -3794742986339086059L;
  int m_PUs;
  PlayerID m_player;
  JLabel m_left = new JLabel();
  ScrollableTextField m_textField;

  TechRollPanel(final int PUs, final PlayerID player) {
    setLayout(new GridBagLayout());
    m_PUs = PUs;
    m_player = player;
    final JLabel title = new JLabel("Select the number of tech rolls:");
    title.setBorder(new javax.swing.border.EmptyBorder(5, 5, 5, 5));
    m_textField = new ScrollableTextField(0, PUs / TechTracker.getTechCost(player));
    m_textField.addChangeListener(m_listener);
    final JLabel costLabel = new JLabel("x" + TechTracker.getTechCost(m_player));
    setLabel(PUs);
    final int space = 0;
    add(title, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(5, 5, space, space), 0, 0));
    add(m_textField, new GridBagConstraints(0, 1, 1, 1, 0.5, 1, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(8, 10, space, space), 0, 0));
    add(costLabel, new GridBagConstraints(1, 1, 1, 1, 0.5, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(8, 5, space, 2), 0, 0));
    add(m_left, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 5, space, space), 0, 0));
  }

  private void setLabel(final int PUs) {
    m_left.setText("Left to spend:" + PUs);
  }

  private final ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener() {
    @Override
    public void changedValue(final ScrollableTextField stf) {
      setLabel(m_PUs - (TechTracker.getTechCost(m_player) * m_textField.getValue()));
    }
  };

  public int getValue() {
    return m_textField.getValue();
  }
}


class TechTokenPanel extends JPanel {
  private static final long serialVersionUID = 332026624893335993L;
  int m_totalPUs;
  int m_playerPUs;
  final ScrollableTextField m_playerPUField;
  PlayerID m_player;
  JLabel m_left = new JLabel();
  JLabel m_right = new JLabel();
  JLabel m_totalCost = new JLabel();
  ScrollableTextField m_textField;
  HashMap<PlayerID, ScrollableTextField> m_whoPaysTextFields = null;

  TechTokenPanel(final int PUs, final int currTokens, final PlayerID player, final Collection<PlayerID> helpPay) {
    m_playerPUs = PUs;
    m_totalPUs = PUs;
    if (helpPay != null && !helpPay.isEmpty()) {
      helpPay.remove(player);
      for (final PlayerID p : helpPay) {
        m_totalPUs += p.getResources().getQuantity(Constants.PUS);
      }
    }
    m_playerPUField = new ScrollableTextField(0, m_totalPUs);
    m_playerPUField.setEnabled(false);
    setLayout(new GridBagLayout());
    m_player = player;
    final JLabel title = new JLabel("Select the number of tech tokens to purchase:");
    title.setBorder(new javax.swing.border.EmptyBorder(5, 5, 5, 5));
    final int techCost = TechTracker.getTechCost(m_player);
    m_textField = new ScrollableTextField(0, m_totalPUs / techCost);
    m_textField.addChangeListener(m_listener);
    final JLabel costLabel = new JLabel("x" + techCost + " cost per token");
    setLabel(0);
    setTokens(currTokens);
    final int space = 0;
    add(title, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(5, 5, space, space), 0, 0));
    add(m_textField, new GridBagConstraints(0, 1, 1, 1, 0.5, 1, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(8, 10, space, space), 0, 0));
    add(costLabel, new GridBagConstraints(1, 1, 1, 1, 0.5, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(8, 5, space, 2), 0, 0));
    add(m_left, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 5, space, space), 0, 0));
    add(m_right, new GridBagConstraints(0, 2, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 130, space, space), 0, 0));
    add(m_totalCost, new GridBagConstraints(0, 3, 3, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 5, space, space), 0, 0));
    if (helpPay != null && !helpPay.isEmpty()) {
      if (m_whoPaysTextFields == null) {
        m_whoPaysTextFields = new HashMap<>();
      }
      helpPay.remove(player);
      int row = 4;
      add(new JLabel("Nations Paying How Much:"), new GridBagConstraints(0, row, 1, 1, 0.5, 1, GridBagConstraints.EAST,
          GridBagConstraints.NONE, new Insets(30, 6, 6, 6), 0, 0));
      row++;
      add(new JLabel(player.getName()), new GridBagConstraints(0, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
      add(m_playerPUField, new GridBagConstraints(1, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
      add(new JLabel("PUs"), new GridBagConstraints(2, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
      row++;
      for (final PlayerID p : helpPay) {
        final int helperPUs = p.getResources().getQuantity(Constants.PUS);
        if (helperPUs > 0) {
          final ScrollableTextField whoPaysTextField = new ScrollableTextField(0, helperPUs);
          whoPaysTextField.addChangeListener(setWidgetAction());
          m_whoPaysTextFields.put(p, whoPaysTextField);
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
    if (m_whoPaysTextFields == null || m_whoPaysTextFields.isEmpty()) {
      return;
    }
    final int cost = TechTracker.getTechCost(m_player) * m_textField.getValue();
    int totalPaidByOthers = 0;
    for (final Entry<PlayerID, ScrollableTextField> entry : m_whoPaysTextFields.entrySet()) {
      totalPaidByOthers += Math.max(0, entry.getValue().getValue());
    }
    final int totalPaidByPlayer = Math.max(0, cost - totalPaidByOthers);
    int amountOver = -1 * (m_playerPUs - totalPaidByPlayer);
    final Iterator<Entry<PlayerID, ScrollableTextField>> otherPayers = m_whoPaysTextFields.entrySet().iterator();
    while (amountOver > 0 && otherPayers.hasNext()) {
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
    for (final Entry<PlayerID, ScrollableTextField> entry : m_whoPaysTextFields.entrySet()) {
      totalPaidByOthers += Math.max(0, entry.getValue().getValue());
    }
    int amountUnder = -1 * (cost - totalPaidByOthers);
    final Iterator<Entry<PlayerID, ScrollableTextField>> otherPayers2 = m_whoPaysTextFields.entrySet().iterator();
    while (amountUnder > 0 && otherPayers2.hasNext()) {
      final Entry<PlayerID, ScrollableTextField> entry = otherPayers2.next();
      int current = entry.getValue().getValue();
      if (current > 0) {
        final int canSubtract = Math.min(current, amountUnder);
        amountUnder -= canSubtract;
        current -= canSubtract;
        entry.getValue().setValue(current);
      }
    }
    m_playerPUField.setValue(Math.max(0, Math.min(m_playerPUs, totalPaidByPlayer)));
  }

  private void setLabel(final int cost) {
    m_left.setText("Left to Spend:  " + (m_totalPUs - cost));
    m_totalCost.setText("Total Cost:  " + cost);
  }

  private void setTokens(final int tokens) {
    m_right.setText("Current token count:  " + tokens);
  }

  private final ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener() {
    @Override
    public void changedValue(final ScrollableTextField stf) {
      setLabel(TechTracker.getTechCost(m_player) * m_textField.getValue());
      setWidgetActivation();
    }
  };

  private final ScrollableTextFieldListener setWidgetAction() {
    return new ScrollableTextFieldListener() {
      @Override
      public void changedValue(final ScrollableTextField stf) {
        setWidgetActivation();
      }
    };
  }

  public int getValue() {
    return m_textField.getValue();
  }

  public IntegerMap<PlayerID> getWhoPaysHowMuch() {
    final int techCost = TechTracker.getTechCost(m_player);
    final int numberOfTechRolls = getValue();
    final int totalCost = numberOfTechRolls * techCost;
    final IntegerMap<PlayerID> whoPaysHowMuch = new IntegerMap<>();
    if (m_whoPaysTextFields == null || m_whoPaysTextFields.isEmpty()) {
      whoPaysHowMuch.put(m_player, totalCost);
    } else {
      int runningTotal = 0;
      for (final Entry<PlayerID, ScrollableTextField> entry : m_whoPaysTextFields.entrySet()) {
        final int value = entry.getValue().getValue();
        whoPaysHowMuch.put(entry.getKey(), value);
        runningTotal += value;
      }
      if (!m_whoPaysTextFields.containsKey(m_player)) {
        whoPaysHowMuch.put(m_player, Math.max(0, totalCost - runningTotal));
      }
    }
    return whoPaysHowMuch;
  }
}
