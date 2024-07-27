package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Setter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.SwingComponents;

/** The action panel displayed during the purchase action. */
public class PurchasePanel extends ActionPanel {
  private static final long serialVersionUID = -6121756876868623355L;
  // if this is set Purchase will use the tabbedProductionPanel - this is modifiable through the
  // View Menu
  private static final String BUY = "Buy...";
  private static final String CHANGE = "Change...";

  private IntegerMap<ProductionRule> purchase = new IntegerMap<>();
  private boolean bid;
  private final SimpleUnitPanel purchasedPreviousRoundsUnits;
  private final JLabel purchasedPreviousRoundsLabel;
  private final SimpleUnitPanel purchasedUnits;
  private final JLabel purchasedLabel = createIndentedLabel();
  private final JButton buyButton;
  @Setter private boolean keepCurrentPurchase;

  private final AbstractAction purchaseAction =
      new AbstractAction("Buy") {
        private static final long serialVersionUID = -2931438906267249990L;

        @Override
        public void actionPerformed(final ActionEvent e) {
          final GamePlayer player = getCurrentPlayer();
          final GameData data = getData();

          // Restore pending production that was loaded from the save game.
          // Use the delegate from the step, since it may not actually be named 'purchase'.
          final IDelegate delegate = data.getSequence().getStep().getDelegate();
          if (delegate instanceof PurchaseDelegate) {
            final var savedPurchase = ((PurchaseDelegate) delegate).getPendingProductionRules();
            if (savedPurchase != null) {
              purchase = savedPurchase;
            }
          }

          purchase =
              TabbedProductionPanel.getProduction(
                  player,
                  (JFrame) getTopLevelAncestor(),
                  data,
                  bid,
                  purchase,
                  getMap().getUiContext());

          if (delegate instanceof PurchaseDelegate) {
            // Set pending production on the PurchaseDelegate for saving the game.
            ((PurchaseDelegate) delegate).setPendingProductionRules(purchase);
          }

          purchasedUnits.setUnitsFromProductionRuleMap(purchase, player);
          if (purchase.totalValues() == 0) {
            purchasedLabel.setText("");
            buyButton.setText(BUY);
          } else {
            buyButton.setText(CHANGE);
            purchasedLabel.setText(
                totalUnitNumberPurchased(purchase)
                    + MyFormatter.pluralize(" unit", totalUnitNumberPurchased(purchase))
                    + " to be produced:");
          }
        }
      };

  public PurchasePanel(final GameData data, final MapPanel map) {
    super(data, map);
    purchasedPreviousRoundsUnits = new SimpleUnitPanel(map.getUiContext());
    purchasedUnits = new SimpleUnitPanel(map.getUiContext());
    buyButton = new JButton(BUY);
    buyButton.addActionListener(purchaseAction);
    purchasedPreviousRoundsLabel = new JLabel("Unplaced from previous rounds");
  }

  @Override
  public void display(final GamePlayer gamePlayer) {
    super.display(gamePlayer);
    // If keepCurrentPurchase is true, we're trying after showing an error to the user about their
    // current selection. Don't clear everything and let the user correct it instead.
    if (keepCurrentPurchase) {
      keepCurrentPurchase = false;
    } else {
      purchase.clear();
    }
    SwingUtilities.invokeLater(
        () -> {
          removeAll();
          actionLabel.setText(gamePlayer.getName() + " production");
          add(SwingComponents.leftBox(actionLabel));

          buyButton.setText(BUY);
          add(createButtonsPanel(buyButton, createDoneButton()));

          add(Box.createVerticalStrut(9));

          purchasedLabel.setText("");
          add(SwingComponents.leftBox(purchasedLabel));

          add(Box.createVerticalStrut(4));

          purchasedUnits.setUnitsFromProductionRuleMap(new IntegerMap<>(), gamePlayer);
          add(SwingComponents.leftBox(purchasedUnits));

          try (GameData.Unlocker ignored = getData().acquireReadLock()) {
            purchasedPreviousRoundsUnits.setUnits(gamePlayer.getUnits());
            add(Box.createVerticalStrut(4));
            if (!gamePlayer.getUnitCollection().isEmpty()) {
              add(SwingComponents.leftBox(purchasedPreviousRoundsLabel));
            }
            add(SwingComponents.leftBox(purchasedPreviousRoundsUnits));
          }
          add(Box.createVerticalGlue());
          refresh.run();
        });
  }

  @Override
  public void performDone() {
    final boolean hasPurchased = purchase.totalValues() != 0;
    if (!hasPurchased) {
      final int selectedOption =
          JOptionPane.showConfirmDialog(
              JOptionPane.getFrameForComponent(PurchasePanel.this),
              "Are you sure you don't want to buy anything?",
              "End Purchase",
              JOptionPane.YES_NO_OPTION);
      if (selectedOption != JOptionPane.YES_OPTION) {
        return;
      }
    }
    // give a warning if the
    // player tries to produce too much
    final GameData data = getData();
    final GameProperties properties = data.getProperties();
    if (Properties.getWW2V2(properties) || Properties.getPlacementRestrictedByFactory(properties)) {
      final GamePlayer player = getCurrentPlayer();
      int totalProd = 0;
      try (GameData.Unlocker ignored = data.acquireReadLock()) {
        final var predicate = Matches.territoryHasOwnedIsFactoryOrCanProduceUnits(player);
        final var territories =
            CollectionUtils.getMatches(data.getMap().getTerritories(), predicate);
        for (final Territory t : CollectionUtils.getMatches(territories, predicate)) {
          totalProd +=
              UnitUtils.getProductionPotentialOfTerritory(t.getUnits(), t, player, true, true);
        }
      }
      // sum production for all units except factories
      int totalProduced = 0;
      for (final ProductionRule rule : purchase.keySet()) {
        final NamedAttachable resourceOrUnit = rule.getAnyResultKey();
        if (resourceOrUnit instanceof UnitType) {
          final UnitType type = (UnitType) resourceOrUnit;
          if (!Matches.unitTypeIsConstruction().test(type)) {
            totalProduced += purchase.getInt(rule) * rule.getResults().totalValues();
          }
        }
      }
      final Collection<Unit> unitsNeedingFactory =
          CollectionUtils.getMatches(player.getUnits(), Matches.unitIsNotConstruction());
      if (!bid
          && totalProduced + unitsNeedingFactory.size() > totalProd
          && !isUnlimitedProduction(player)) {
        final String text =
            "You have purchased "
                + (totalProduced + unitsNeedingFactory.size())
                + " units, and can only place "
                + totalProd
                + " of them. Continue with purchase?";
        final int selectedOption =
            JOptionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(PurchasePanel.this),
                text,
                "End Purchase",
                JOptionPane.YES_NO_OPTION);
        if (selectedOption != JOptionPane.YES_OPTION) {
          return;
        }
      }
    }
    // When closing the panel, clear the pending production.
    final IDelegate delegate = data.getSequence().getStep().getDelegate();
    if (delegate instanceof PurchaseDelegate) {
      ((PurchaseDelegate) delegate).setPendingProductionRules(null);
    }
    release();
  }

  private void refreshActionLabelText() {
    SwingUtilities.invokeLater(
        () ->
            actionLabel.setText(
                getCurrentPlayer().getName() + " production " + (bid ? " for bid" : "")));
  }

  IntegerMap<ProductionRule> waitForPurchase(final boolean bid) {
    this.bid = bid;
    refreshActionLabelText();
    // automatically "click" the buy button for us!
    SwingUtilities.invokeLater(() -> purchaseAction.actionPerformed(null));
    waitForRelease();
    return purchase;
  }

  private static int totalUnitNumberPurchased(final IntegerMap<ProductionRule> purchase) {
    int totalUnits = 0;
    final Collection<ProductionRule> rules = purchase.keySet();
    for (final ProductionRule current : rules) {
      totalUnits += purchase.getInt(current) * current.getResults().totalValues();
    }
    return totalUnits;
  }

  private static boolean isUnlimitedProduction(final GamePlayer player) {
    final RulesAttachment ra = player.getRulesAttachment();
    return ra != null && ra.getUnlimitedProduction();
  }

  @Override
  public String toString() {
    return "PurchasePanel";
  }
}
