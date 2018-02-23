package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.SwingAction;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

public class PurchasePanel extends ActionPanel {
  private static final long serialVersionUID = -6121756876868623355L;
  private final JLabel actionLabel = new JLabel();
  private IntegerMap<ProductionRule> purchase;
  private boolean bid;
  private final SimpleUnitPanel purchasedPreviousRoundsUnits;
  private final JLabel purchasedPreviousRoundsLabel;
  private final SimpleUnitPanel purhcasedUnits;
  private final JLabel purchasedLabel = new JLabel();
  private final JButton buyButton;
  // if this is set Purchase will use the tabbedProductionPanel - this is modifyable through the View Menu
  private static boolean tabbedProduction = true;
  private static final String BUY = "Buy...";
  private static final String CHANGE = "Change...";

  /** Creates new PurchasePanel. */
  public PurchasePanel(final GameData data, final MapPanel map) {
    super(data, map);
    purchasedPreviousRoundsUnits = new SimpleUnitPanel(map.getUiContext());
    purhcasedUnits = new SimpleUnitPanel(map.getUiContext());
    buyButton = new JButton(BUY);
    buyButton.addActionListener(purchaseAction);
    purchasedPreviousRoundsLabel = new JLabel("Unplaced from previous rounds");
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    purchase = new IntegerMap<>();
    SwingUtilities.invokeLater(() -> {
      removeAll();
      actionLabel.setText(id.getName() + " production");
      add(actionLabel);

      buyButton.setText(BUY);
      add(buyButton);

      add(new JButton(doneAction));

      add(Box.createVerticalStrut(9));

      purchasedLabel.setText("");
      add(purchasedLabel);

      add(Box.createVerticalStrut(4));

      purhcasedUnits.setUnitsFromProductionRuleMap(new IntegerMap<>(), id);
      add(purhcasedUnits);

      getData().acquireReadLock();
      try {
        purchasedPreviousRoundsUnits.setUnitsFromCategories(UnitSeperator.categorize(id.getUnits().getUnits()));
        add(Box.createVerticalStrut(4));
        if (!id.getUnits().isEmpty()) {
          add(purchasedPreviousRoundsLabel);
        }
        add(purchasedPreviousRoundsUnits);
      } finally {
        getData().releaseReadLock();
      }
      add(Box.createVerticalGlue());
      SwingUtilities.invokeLater(refresh);
    });
  }

  private void refreshActionLabelText() {
    SwingUtilities.invokeLater(
        () -> actionLabel.setText(getCurrentPlayer().getName() + " production " + (bid ? " for bid" : "")));
  }

  IntegerMap<ProductionRule> waitForPurchase(final boolean bid) {
    this.bid = bid;
    refreshActionLabelText();
    // automatically "click" the buy button for us!
    SwingUtilities.invokeLater(() -> purchaseAction.actionPerformed(null));
    waitForRelease();
    return purchase;
  }

  private final AbstractAction purchaseAction = new AbstractAction("Buy") {
    private static final long serialVersionUID = -2931438906267249990L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      final PlayerID player = getCurrentPlayer();
      final GameData data = getData();
      if (isTabbedProduction()) {
        purchase = TabbedProductionPanel.getProduction(player, (JFrame) getTopLevelAncestor(), data, bid,
            purchase, getMap().getUiContext());
      } else {
        purchase = ProductionPanel.getProduction(player, (JFrame) getTopLevelAncestor(), data, bid, purchase,
            getMap().getUiContext());
      }
      purhcasedUnits.setUnitsFromProductionRuleMap(purchase, player);
      if (purchase.totalValues() == 0) {
        purchasedLabel.setText("");
        buyButton.setText(BUY);
      } else {
        buyButton.setText(CHANGE);
        purchasedLabel.setText(totalUnitNumberPurchased(purchase)
            + MyFormatter.pluralize(" unit", totalUnitNumberPurchased(purchase)) + " to be produced:");
      }
    }
  };

  private static int totalUnitNumberPurchased(final IntegerMap<ProductionRule> purchase) {
    int totalUnits = 0;
    final Collection<ProductionRule> rules = purchase.keySet();
    for (final ProductionRule current : rules) {
      totalUnits += purchase.getInt(current) * current.getResults().totalValues();
    }
    return totalUnits;
  }

  private final Action doneAction = SwingAction.of("Done", e -> {
    final boolean hasPurchased = purchase.totalValues() != 0;
    if (!hasPurchased) {
      final int selectedOption = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(PurchasePanel.this),
          "Are you sure you dont want to buy anything?", "End Purchase", JOptionPane.YES_NO_OPTION);
      if (selectedOption != JOptionPane.YES_OPTION) {
        return;
      }
    }
    // give a warning if the
    // player tries to produce too much
    if (isWW2V2() || isRestrictedPurchase()) {
      getData().acquireReadLock();
      int totalProd = 0;
      try {
        for (final Territory t : CollectionUtils.getMatches(getData().getMap().getTerritories(),
            Matches.territoryHasOwnedIsFactoryOrCanProduceUnits(getCurrentPlayer()))) {
          totalProd += TripleAUnit.getProductionPotentialOfTerritory(t.getUnits().getUnits(), t, getCurrentPlayer(),
              getData(), true, true);
        }
      } finally {
        getData().releaseReadLock();
      }
      // sum production for all units except factories
      int totalProduced = 0;
      for (final ProductionRule rule : purchase.keySet()) {
        final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
        if (resourceOrUnit instanceof UnitType) {
          final UnitType type = (UnitType) resourceOrUnit;
          if (!Matches.unitTypeIsConstruction().test(type)) {
            totalProduced += purchase.getInt(rule) * rule.getResults().totalValues();
          }
        }
      }
      final PlayerID player = getCurrentPlayer();
      final Collection<Unit> unitsNeedingFactory =
          CollectionUtils.getMatches(player.getUnits().getUnits(), Matches.unitIsNotConstruction());
      if (!bid && ((totalProduced + unitsNeedingFactory.size()) > totalProd) && !isUnlimitedProduction(player)) {
        final String text = "You have purchased " + (totalProduced + unitsNeedingFactory.size())
            + " units, and can only place " + totalProd + " of them. Continue with purchase?";
        final int selectedOption = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(PurchasePanel.this),
            text, "End Purchase", JOptionPane.YES_NO_OPTION);
        if (selectedOption != JOptionPane.YES_OPTION) {
          return;
        }
      }
    }
    release();
  });

  private static boolean isUnlimitedProduction(final PlayerID player) {
    final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
    if (ra == null) {
      return false;
    }
    return ra.getUnlimitedProduction();
  }

  @Override
  public String toString() {
    return "PurchasePanel";
  }

  public static void setTabbedProduction(final boolean tabbedProduction) {
    PurchasePanel.tabbedProduction = tabbedProduction;
  }

  public static boolean isTabbedProduction() {
    return tabbedProduction;
  }
}
