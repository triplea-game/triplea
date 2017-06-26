package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;

public class RepairPanel extends ActionPanel {
  private static final long serialVersionUID = 3045997038627313714L;
  private static final String CHANGE = "Change...";
  private static final String BUY = "Repair...";
  private final JLabel actionLabel = new JLabel();
  private HashMap<Unit, IntegerMap<RepairRule>> repair;
  private boolean bid;
  private Collection<PlayerID> allowedPlayersToRepair;
  private final SimpleUnitPanel unitsPanel;
  private final JLabel repairdSoFar = new JLabel();
  private final JButton buyButton;

  /** Creates new RepairPanel. */
  public RepairPanel(final GameData data, final MapPanel map) {
    super(data, map);
    unitsPanel = new SimpleUnitPanel(map.getUIContext());
    buyButton = new JButton(BUY);
    buyButton.addActionListener(PURCHASE_ACTION);
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    repair = new HashMap<>();
    SwingUtilities.invokeLater(() -> {
      removeAll();
      actionLabel.setText(id.getName() + " repair");
      buyButton.setText(BUY);
      add(actionLabel);
      add(buyButton);
      add(new JButton(DoneAction));
      repairdSoFar.setText("");
      add(Box.createVerticalStrut(9));
      add(repairdSoFar);
      add(Box.createVerticalStrut(4));
      unitsPanel.setUnitsFromRepairRuleMap(new HashMap<>(), id, getData());
      add(unitsPanel);
      add(Box.createVerticalGlue());
      SwingUtilities.invokeLater(REFRESH);
    });
  }

  private void refreshActionLabelText() {
    SwingUtilities.invokeLater(
        () -> actionLabel.setText(getCurrentPlayer().getName() + " repair " + (bid ? " for bid" : "")));
  }

  HashMap<Unit, IntegerMap<RepairRule>> waitForRepair(final boolean bid,
      final Collection<PlayerID> allowedPlayersToRepair) {
    this.bid = bid;
    this.allowedPlayersToRepair = allowedPlayersToRepair;
    refreshActionLabelText();
    // automatically "click" the buy button for us!
    SwingUtilities.invokeLater(() -> PURCHASE_ACTION.actionPerformed(null));
    waitForRelease();
    return repair;
  }

  private final AbstractAction PURCHASE_ACTION = new AbstractAction("Buy") {
    private static final long serialVersionUID = 5572043262815077402L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      final PlayerID player = getCurrentPlayer();
      final GameData data = getData();
      repair = ProductionRepairPanel.getProduction(player, allowedPlayersToRepair, (JFrame) getTopLevelAncestor(),
          data, bid, repair, getMap().getUIContext());
      unitsPanel.setUnitsFromRepairRuleMap(repair, player, data);
      final int totalValues = getTotalValues(repair);
      if (totalValues == 0) {
        repairdSoFar.setText("");
        buyButton.setText(BUY);
      } else {
        buyButton.setText(CHANGE);
        repairdSoFar.setText(totalValues + MyFormatter.pluralize(" unit", totalValues) + " to be repaired:");
      }
    }
  };

  // Spin through the territories to get this.
  private static int getTotalValues(final HashMap<Unit, IntegerMap<RepairRule>> repair) {
    final Collection<Unit> units = repair.keySet();
    final Iterator<Unit> iter = units.iterator();
    int totalValues = 0;
    while (iter.hasNext()) {
      final Unit unit = iter.next();
      totalValues += repair.get(unit).totalValues();
    }
    return totalValues;
  }

  private final Action DoneAction = new AbstractAction("Done") {
    private static final long serialVersionUID = -2002286381161651398L;

    @Override
    public void actionPerformed(final ActionEvent event) {
      final boolean hasPurchased = getTotalValues(repair) != 0;
      if (!hasPurchased) {
        final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(RepairPanel.this),
            "Are you sure you dont want to repair anything?", "End Purchase", JOptionPane.YES_NO_OPTION);
        if (rVal != JOptionPane.YES_OPTION) {
          return;
        }
      }
      release();
    }
  };

  @Override
  public String toString() {
    return "RepairPanel";
  }
}
