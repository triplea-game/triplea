package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.image.UnitImageFactory.ImageKey;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.SwingAction;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

class ProductionRepairPanel extends JPanel {
  private static final long serialVersionUID = -6344711064699083729L;
  private JDialog dialog;
  private final UiContext uiContext;
  private final List<Rule> rules = new ArrayList<>();
  private final JLabel left = new JLabel();
  private JButton done;
  private GamePlayer gamePlayer;
  private boolean bid;
  private Collection<GamePlayer> allowedPlayersToRepair;
  private GameData data;
  final Action doneAction = SwingAction.of("Done", e -> dialog.setVisible(false));
  private final ScrollableTextFieldListener listener = stf -> calculateLimits();

  ProductionRepairPanel(final UiContext uiContext) {
    this.uiContext = uiContext;
  }

  static Map<Unit, IntegerMap<RepairRule>> getProduction(
      final GamePlayer gamePlayer,
      final Collection<GamePlayer> allowedPlayersToRepair,
      final JFrame parent,
      final GameData data,
      final boolean bid,
      final Map<Unit, IntegerMap<RepairRule>> initialPurchase,
      final UiContext uiContext) {
    return new ProductionRepairPanel(uiContext)
        .show(gamePlayer, allowedPlayersToRepair, parent, data, bid, initialPurchase);
  }

  private Map<Unit, IntegerMap<RepairRule>> getProduction() {
    final Map<Unit, IntegerMap<RepairRule>> prod = new HashMap<>();
    for (final Rule rule : rules) {
      final int quantity = rule.getQuantity();
      if (quantity != 0) {
        final IntegerMap<RepairRule> repairRule = new IntegerMap<>();
        final Unit unit = rule.getUnit();
        repairRule.put(rule.getProductionRule(), quantity);
        prod.put(unit, repairRule);
      }
    }
    return prod;
  }

  /** Shows the production panel, and returns a map of selected rules. */
  Map<Unit, IntegerMap<RepairRule>> show(
      final GamePlayer gamePlayer,
      final Collection<GamePlayer> allowedPlayersToRepair,
      final JFrame parent,
      final GameData data,
      final boolean bid,
      final Map<Unit, IntegerMap<RepairRule>> initialPurchase) {
    if (parent != null) {
      dialog = null;
    }
    if (dialog == null) {
      initDialog(parent);
    }
    this.bid = bid;
    this.allowedPlayersToRepair = allowedPlayersToRepair;
    this.data = data;
    this.initRules(gamePlayer, allowedPlayersToRepair, data, initialPurchase);
    this.initLayout();
    this.calculateLimits();
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    SwingUtilities.invokeLater(() -> done.requestFocusInWindow());
    dialog.setVisible(true);
    dialog.dispose();
    return getProduction();
  }

  private void initDialog(final JFrame root) {
    dialog = new JDialog(root, "Repair", true);
    dialog.getContentPane().add(this);
    SwingKeyBinding.addKeyBinding(dialog, KeyCode.ESCAPE, () -> dialog.setVisible(false));
  }

  private void initRules(
      final GamePlayer player,
      final Collection<GamePlayer> allowedPlayersToRepair,
      final GameData data,
      final Map<Unit, IntegerMap<RepairRule>> initialPurchase) {
    if (!Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())) {
      return;
    }
    try (GameData.Unlocker ignored = this.data.acquireReadLock()) {
      this.gamePlayer = player;
      this.allowedPlayersToRepair = allowedPlayersToRepair;
      Predicate<Unit> myDamagedUnits =
          Matches.unitIsOwnedByAnyOf(this.allowedPlayersToRepair)
              .and(Matches.unitHasTakenSomeBombingUnitDamage());
      if (GameStepPropertiesHelper.isOnlyRepairIfDisabled(data)) {
        myDamagedUnits = myDamagedUnits.and(Matches.unitIsDisabled());
      }
      final Collection<Territory> terrsWithPotentiallyDamagedUnits =
          CollectionUtils.getMatches(
              data.getMap().getTerritories(), Matches.territoryHasUnitsThatMatch(myDamagedUnits));
      for (final RepairRule repairRule : player.getRepairFrontier()) {
        for (final Territory terr : terrsWithPotentiallyDamagedUnits) {
          for (final Unit unit : CollectionUtils.getMatches(terr.getUnits(), myDamagedUnits)) {
            if (!repairRule.getAnyResultKey().equals(unit.getType())) {
              continue;
            }
            final Rule rule = new Rule(repairRule, unit, terr);
            int initialQuantity = 0;
            if (initialPurchase.get(unit) != null) {
              initialQuantity = initialPurchase.get(unit).getInt(repairRule);
            }
            rule.setQuantity(initialQuantity);
            rule.setMax(unit.getHowMuchCanThisUnitBeRepaired(terr));
            rule.setName(unit.toString());
            rules.add(rule);
          }
        }
      }
    }
  }

  private void initLayout() {
    final Insets nullInsets = new Insets(0, 0, 0, 0);
    this.removeAll();
    this.setLayout(new GridBagLayout());
    final JLabel legendLabel = new JLabel("Repair Units");
    add(
        legendLabel,
        new GridBagConstraints(
            0,
            0,
            30,
            1,
            1,
            1,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            new Insets(8, 8, 8, 0),
            0,
            0));
    for (int x = 0; x < rules.size(); x++) {
      final boolean even = (x / 2) * 2 == x;
      add(
          rules.get(x),
          new GridBagConstraints(
              x / 2,
              even ? 1 : 2,
              1,
              1,
              1,
              1,
              GridBagConstraints.EAST,
              GridBagConstraints.HORIZONTAL,
              nullInsets,
              0,
              0));
    }
    add(
        left,
        new GridBagConstraints(
            0,
            3,
            30,
            1,
            1,
            1,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(8, 8, 0, 12),
            0,
            0));
    done = new JButton(doneAction);
    add(
        done,
        new GridBagConstraints(
            0,
            4,
            30,
            1,
            1,
            1,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(0, 0, 8, 0),
            0,
            0));
  }

  protected void setLeft(final ResourceCollection left) {
    final ResourceCollection total = getResources();
    this.left.setText("<html>You have " + left + " left.<br>Out of " + total + "</html>");
  }

  protected void calculateLimits() {
    // final IntegerMap<Resource> cost;
    final ResourceCollection resources = getResources();
    final ResourceCollection spent = new ResourceCollection(data);
    for (final Rule current : rules) {
      spent.add(current.getCost(), current.getQuantity());
    }

    final double discount =
        TechAbilityAttachment.getRepairDiscount(
            TechTracker.getCurrentTechAdvances(gamePlayer, data.getTechnologyFrontier()));
    if (discount != 1.0D) {
      spent.discount(discount);
    }
    final ResourceCollection leftToSpend = resources.difference(spent);
    setLeft(leftToSpend);
    for (final Rule current : rules) {
      int max = leftToSpend.fitsHowOften(current.getCost());
      if (discount != 1.0F) {
        max = (int) (max / discount);
      }
      max += current.getQuantity();
      current.setMax(max);
    }
  }

  private ResourceCollection getResources() {
    if (bid) {
      // TODO bid only allows you to add PU's to the bid... maybe upgrading Bids so multiple
      // resources can be given?
      // (actually, bids should not cover repairing at all...)
      final String propertyName = gamePlayer.getName() + " bid";
      final int bid = data.getProperties().get(propertyName, 0);
      final ResourceCollection bidCollection = new ResourceCollection(data);
      try (GameData.Unlocker ignored = data.acquireReadLock()) {
        bidCollection.addResource(data.getResourceList().getResourceOrThrow(Constants.PUS), bid);
      }
      return bidCollection;
    }

    return gamePlayer.getResources();
  }

  private class Rule extends JPanel {
    private static final long serialVersionUID = -6781214135310064908L;
    private final ScrollableTextField text = new ScrollableTextField(0, Integer.MAX_VALUE);
    private final IntegerMap<Resource> cost;
    private final RepairRule rule;
    private final Unit unit;
    private final int maxRepairAmount;
    private final int repairResults;

    Rule(final RepairRule rule, final Unit repairUnit, final Territory territoryUnitIsIn) {
      setLayout(new GridBagLayout());
      this.unit = repairUnit;
      this.rule = rule;
      cost = rule.getCosts();
      final UnitType type = (UnitType) rule.getAnyResultKey();
      if (!type.equals(repairUnit.getType())) {
        throw new IllegalStateException(
            "Rule unit type "
                + type.getName()
                + " does not match "
                + repairUnit
                + ".  Please make sure your maps are up to date!");
      }
      repairResults = rule.getResults().getInt(type);
      @NonNls
      final String text = "<html> x " + ResourceCollection.toStringForHtml(cost, data) + "</html>";

      final ImageIcon icon = uiContext.getUnitImageFactory().getIcon(ImageKey.of(repairUnit));
      final JLabel label = new JLabel(text, icon, SwingConstants.LEFT);
      final JLabel info = new JLabel(territoryUnitIsIn.getName());
      maxRepairAmount = repairUnit.getHowMuchCanThisUnitBeRepaired(territoryUnitIsIn);
      final JLabel remaining = new JLabel("Damage left to repair: " + maxRepairAmount);
      final int space = 8;
      this.add(
          new JLabel(type.getName()),
          new GridBagConstraints(
              0,
              0,
              1,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(2, 0, 0, 0),
              0,
              0));
      this.add(
          label,
          new GridBagConstraints(
              0,
              1,
              1,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(5, space, space, space),
              0,
              0));
      this.add(
          info,
          new GridBagConstraints(
              0,
              2,
              1,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(5, space, space, space),
              0,
              0));
      this.add(
          remaining,
          new GridBagConstraints(
              0,
              3,
              1,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(5, space, space, space),
              0,
              0));
      this.add(
          this.text,
          new GridBagConstraints(
              0,
              4,
              1,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(10, space, space, space),
              0,
              0));
      this.text.addChangeListener(listener);
      setBorder(new EtchedBorder());
    }

    IntegerMap<Resource> getCost() {
      return cost;
    }

    int getQuantity() {
      return text.getValue();
    }

    void setQuantity(final int quantity) {
      text.setValue(quantity);
    }

    RepairRule getProductionRule() {
      return rule;
    }

    void setMax(final int max) {
      text.setMax(
          (int) Math.ceil(((double) Math.min(max, maxRepairAmount) / (double) repairResults)));
    }

    Unit getUnit() {
      return unit;
    }
  }
}
