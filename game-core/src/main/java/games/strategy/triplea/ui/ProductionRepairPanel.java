package games.strategy.triplea.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

public class ProductionRepairPanel extends JPanel {
  private static final long serialVersionUID = -6344711064699083729L;
  private final JFrame owner = null;
  private JDialog dialog;
  private final UiContext uiContext;
  private final List<Rule> rules = new ArrayList<>();
  private final JLabel left = new JLabel();
  private JButton done;
  private PlayerID id;
  private boolean bid;
  private Collection<PlayerID> allowedPlayersToRepair;
  private GameData data;

  public static HashMap<Unit, IntegerMap<RepairRule>> getProduction(final PlayerID id,
      final Collection<PlayerID> allowedPlayersToRepair, final JFrame parent, final GameData data, final boolean bid,
      final HashMap<Unit, IntegerMap<RepairRule>> initialPurchase, final UiContext uiContext) {
    return new ProductionRepairPanel(uiContext).show(id, allowedPlayersToRepair, parent, data, bid, initialPurchase);
  }

  private HashMap<Unit, IntegerMap<RepairRule>> getProduction() {
    final HashMap<Unit, IntegerMap<RepairRule>> prod = new HashMap<>();
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

  /**
   * Shows the production panel, and returns a map of selected rules.
   */
  public HashMap<Unit, IntegerMap<RepairRule>> show(final PlayerID id,
      final Collection<PlayerID> allowedPlayersToRepair, final JFrame parent, final GameData data, final boolean bid,
      final HashMap<Unit, IntegerMap<RepairRule>> initialPurchase) {
    if (!(parent == owner)) {
      dialog = null;
    }
    if (dialog == null) {
      initDialog(parent);
    }
    this.bid = bid;
    this.allowedPlayersToRepair = allowedPlayersToRepair;
    this.data = data;
    this.initRules(id, allowedPlayersToRepair, data, initialPurchase);
    this.initLayout();
    this.calculateLimits();
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    SwingUtilities.invokeLater(() -> done.requestFocusInWindow());
    dialog.setVisible(true);
    dialog.dispose();
    return getProduction();
  }

  public List<Rule> getRules() {
    return this.rules;
  }

  private void initDialog(final JFrame root) {
    dialog = new JDialog(root, "Repair", true);
    dialog.getContentPane().add(this);
    SwingComponents.addEscapeKeyListener(dialog, () -> dialog.setVisible(false));
  }

  /** Creates new ProductionRepairPanel. */
  // the constructor can be accessed by subclasses
  public ProductionRepairPanel(final UiContext uiContext) {
    this.uiContext = uiContext;
  }

  private void initRules(final PlayerID player, final Collection<PlayerID> allowedPlayersToRepair, final GameData data,
      final HashMap<Unit, IntegerMap<RepairRule>> initialPurchase) {
    if (!Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
      return;
    }
    this.data.acquireReadLock();
    try {
      this.id = player;
      this.allowedPlayersToRepair = allowedPlayersToRepair;
      final Predicate<Unit> myDamagedUnits = Matches.unitIsOwnedByOfAnyOfThesePlayers(this.allowedPlayersToRepair)
          .and(Matches.unitHasTakenSomeBombingUnitDamage());
      final Collection<Territory> terrsWithPotentiallyDamagedUnits = CollectionUtils
          .getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsThatMatch(myDamagedUnits));
      for (final RepairRule repairRule : player.getRepairFrontier()) {
        for (final Territory terr : terrsWithPotentiallyDamagedUnits) {
          for (final Unit unit : CollectionUtils.getMatches(terr.getUnits().getUnits(), myDamagedUnits)) {
            if (!repairRule.getResults().keySet().iterator().next().equals(unit.getType())) {
              continue;
            }
            final TripleAUnit taUnit = (TripleAUnit) unit;
            final Rule rule = new Rule(repairRule, player, unit, terr);
            int initialQuantity = 0;
            if (initialPurchase.get(unit) != null) {
              initialQuantity = initialPurchase.get(unit).getInt(repairRule);
            }
            rule.setQuantity(initialQuantity);
            rule.setMax(taUnit.getHowMuchCanThisUnitBeRepaired(unit, terr));
            rule.setName(unit.toString());
            rules.add(rule);
          }
        }
      }
    } finally {
      this.data.releaseReadLock();
    }
  }

  private void initLayout() {
    final Insets nullInsets = new Insets(0, 0, 0, 0);
    this.removeAll();
    this.setLayout(new GridBagLayout());
    final JLabel legendLabel = new JLabel("Repair Units");
    add(legendLabel, new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(8, 8, 8, 0), 0, 0));
    for (int x = 0; x < rules.size(); x++) {
      final boolean even = ((x / 2) * 2) == x;
      add(rules.get(x), new GridBagConstraints(x / 2, even ? 1 : 2, 1, 1, 1, 1, GridBagConstraints.EAST,
          GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
    }
    add(left, new GridBagConstraints(0, 3, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(8, 8, 0, 12), 0, 0));
    done = new JButton(doneAction);
    add(done, new GridBagConstraints(0, 4, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 8, 0), 0, 0));
  }

  protected void setLeft(final ResourceCollection left) {
    final ResourceCollection total = getResources();
    this.left.setText("<html>You have " + left + " left.<br>Out of " + total + "</html>");
  }

  Action doneAction = SwingAction.of("Done", e -> dialog.setVisible(false));

  protected void calculateLimits() {
    // final IntegerMap<Resource> cost;
    final ResourceCollection resources = getResources();
    final ResourceCollection spent = new ResourceCollection(data);
    for (final Rule current : rules) {
      spent.add(current.getCost(), current.getQuantity());
    }
    final double discount = TechAbilityAttachment.getRepairDiscount(id, data);
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
      // TODO bid only allows you to add PU's to the bid... maybe upgrading Bids so multiple resources can be given?
      // (actually, bids should
      // not cover repairing at all...)
      final String propertyName = id.getName() + " bid";
      final int bid = data.getProperties().get(propertyName, 0);
      final ResourceCollection bidCollection = new ResourceCollection(data);
      data.acquireReadLock();
      try {
        bidCollection.addResource(data.getResourceList().getResource(Constants.PUS), bid);
      } finally {
        data.releaseReadLock();
      }
      return bidCollection;
    }

    return id.getResources();
  }

  public class Rule extends JPanel {
    private static final long serialVersionUID = -6781214135310064908L;
    private final ScrollableTextField text = new ScrollableTextField(0, Integer.MAX_VALUE);
    private final IntegerMap<Resource> cost;
    private final RepairRule rule;
    private final Unit unit;
    private final int maxRepairAmount;
    private final int repairResults;

    Rule(final RepairRule rule, final PlayerID id, final Unit repairUnit, final Territory territoryUnitIsIn) {
      setLayout(new GridBagLayout());
      this.unit = repairUnit;
      this.rule = rule;
      cost = rule.getCosts();
      final UnitType type = (UnitType) rule.getResults().keySet().iterator().next();
      if (!type.equals(repairUnit.getType())) {
        throw new IllegalStateException("Rule unit type " + type.getName() + " does not match " + repairUnit.toString()
            + ".  Please make sure your maps are up to date!");
      }
      repairResults = rule.getResults().getInt(type);
      final TripleAUnit taUnit = (TripleAUnit) repairUnit;
      final Optional<ImageIcon> icon = uiContext.getUnitImageFactory().getIcon(type, id,
          Matches.unitHasTakenSomeBombingUnitDamage().test(repairUnit), Matches.unitIsDisabled().test(repairUnit));
      final String text = "<html> x " + ResourceCollection.toStringForHtml(cost, data) + "</html>";

      final JLabel label =
          icon.isPresent() ? new JLabel(text, icon.get(), SwingConstants.LEFT) : new JLabel(text, SwingConstants.LEFT);
      final JLabel info = new JLabel(territoryUnitIsIn.getName());
      maxRepairAmount = taUnit.getHowMuchCanThisUnitBeRepaired(repairUnit, territoryUnitIsIn);
      final JLabel remaining = new JLabel("Damage left to repair: " + maxRepairAmount);
      final int space = 8;
      this.add(new JLabel(type.getName()), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(2, 0, 0, 0), 0, 0));
      this.add(label, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(5, space, space, space), 0, 0));
      this.add(info, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(5, space, space, space), 0, 0));
      this.add(remaining, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(5, space, space, space), 0, 0));
      this.add(this.text, new GridBagConstraints(0, 4, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(10, space, space, space), 0, 0));
      this.text.addChangeListener(listener);
      setBorder(new EtchedBorder());
    }

    IntegerMap<Resource> getCost() {
      return cost;
    }

    public int getQuantity() {
      return text.getValue();
    }

    void setQuantity(final int quantity) {
      text.setValue(quantity);
    }

    RepairRule getProductionRule() {
      return rule;
    }

    void setMax(final int max) {
      text.setMax((int) (Math.ceil(((double) Math.min(max, maxRepairAmount) / (double) repairResults))));
    }

    public Unit getUnit() {
      return unit;
    }
  }

  private final ScrollableTextFieldListener listener = stf -> calculateLimits();
}
