package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.util.ResourceCollectionUtils;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;
import swinglib.JPanelBuilder;

public class TabbedProductionPanel extends ProductionPanel {
  private static final long serialVersionUID = 3481282212500641144L;
  private int rows;
  private int columns;

  protected TabbedProductionPanel(final UiContext uiContext) {
    super(uiContext);
  }

  public static IntegerMap<ProductionRule> getProduction(final PlayerID id, final JFrame parent, final GameData data,
      final boolean bid, final IntegerMap<ProductionRule> initialPurchase, final UiContext uiContext) {
    return new TabbedProductionPanel(uiContext).show(id, parent, data, bid, initialPurchase);
  }

  @Override
  protected void initLayout() {
    this.removeAll();
    this.setLayout(new GridBagLayout());
    add(
        new JLabel(String.format(
            "<html>Attack/Defense/Movement. &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; "
                + "(Total Resources: %s)</html>",
            ResourceCollectionUtils.getProductionResources(getResources()))),
        new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
            new Insets(8, 8, 8, 0), 0, 0));
    final JTabbedPane tabs = new JTabbedPane();
    add(tabs, new GridBagConstraints(0, 1, 1, 1, 100, 100, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(8, 8, 8, 0), 0, 0));
    final ProductionTabsProperties properties = ProductionTabsProperties.getInstance(id, rules);
    final List<Tuple<String, List<Rule>>> ruleLists = getRuleLists(properties);
    calculateRowsAndColumns(properties, largestList(ruleLists));
    for (final Tuple<String, List<Rule>> ruleList : ruleLists) {
      if (ruleList.getSecond().size() > 0) {
        tabs.addTab(ruleList.getFirst(), new JScrollPane(getRulesPanel(ruleList.getSecond())));
      }
    }
    add(left, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(8, 8, 0, 12), 0, 0));
    done = new JButton(doneAction);
    add(done, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 8, 0), 0, 0));
    tabs.validate();
    this.validate();
  }

  private void calculateRowsAndColumns(final ProductionTabsProperties properties, final int largestList) {
    if ((properties == null) || (properties.getRows() == 0) || (properties.getColumns() == 0)
        || ((properties.getRows() * properties.getColumns()) < largestList)) {
      final int maxColumns;
      if (largestList <= 36) {
        maxColumns = Math.max(8,
            Math.min(12, new BigDecimal(largestList).divide(new BigDecimal(3), RoundingMode.UP).intValue()));
      } else if (largestList <= 64) {
        maxColumns = Math.max(8,
            Math.min(16, new BigDecimal(largestList).divide(new BigDecimal(4), RoundingMode.UP).intValue()));
      } else {
        maxColumns = Math.max(8,
            Math.min(16, new BigDecimal(largestList).divide(new BigDecimal(5), RoundingMode.UP).intValue()));
      }
      rows =
          Math.max(2, new BigDecimal(largestList).divide(new BigDecimal(maxColumns), RoundingMode.UP).intValue());
      columns =
          Math.max(3, new BigDecimal(largestList).divide(new BigDecimal(rows), RoundingMode.UP).intValue());
    } else {
      rows = Math.max(2, properties.getRows());
      // There are small display problems if the size is less than 2x3 cells.
      columns = Math.max(3, properties.getColumns());
    }
  }

  private static int largestList(final List<Tuple<String, List<Rule>>> ruleLists) {
    int largestList = 0;
    for (final Tuple<String, List<Rule>> tuple : ruleLists) {
      if (largestList < tuple.getSecond().size()) {
        largestList = tuple.getSecond().size();
      }
    }
    return largestList;
  }

  private void checkLists(final List<Tuple<String, List<Rule>>> ruleLists) {
    final List<Rule> rulesCopy = new ArrayList<>(rules);
    for (final Tuple<String, List<Rule>> tuple : ruleLists) {
      for (final Rule rule : tuple.getSecond()) {
        rulesCopy.remove(rule);
      }
    }
    if (rulesCopy.size() > 0) {
      throw new IllegalStateException("production_tabs: must include all player production rules/units");
    }
  }

  private List<Tuple<String, List<Rule>>> getRuleLists(final ProductionTabsProperties properties) {
    if ((properties != null) && !properties.useDefaultTabs()) {
      final List<Tuple<String, List<Rule>>> ruleLists = properties.getRuleLists();
      checkLists(ruleLists);
      return ruleLists;
    }
    return getDefaultRuleLists();
  }

  private List<Tuple<String, List<Rule>>> getDefaultRuleLists() {
    final List<Tuple<String, List<Rule>>> ruleLists = new ArrayList<>();
    final ArrayList<Rule> allRules = new ArrayList<>();
    final ArrayList<Rule> landRules = new ArrayList<>();
    final ArrayList<Rule> airRules = new ArrayList<>();
    final ArrayList<Rule> seaRules = new ArrayList<>();
    final ArrayList<Rule> constructRules = new ArrayList<>();
    final ArrayList<Rule> upgradeConsumesRules = new ArrayList<>();
    final ArrayList<Rule> resourceRules = new ArrayList<>();
    for (final Rule rule : rules) {
      allRules.add(rule);
      final NamedAttachable resourceOrUnit = rule.getProductionRule().getResults().keySet().iterator().next();
      if (resourceOrUnit instanceof UnitType) {
        final UnitType type = (UnitType) resourceOrUnit;
        final UnitAttachment attach = UnitAttachment.get(type);
        if ((attach.getConsumesUnits() != null) && (attach.getConsumesUnits().totalValues() >= 1)) {
          upgradeConsumesRules.add(rule);
        }
        // canproduceUnits isn't checked on purpose, since this category is for units that can be placed
        // anywhere (placed without needing a factory).
        if (attach.getIsConstruction()) {
          constructRules.add(rule);
        } else if (attach.getIsSea()) {
          seaRules.add(rule);
        } else if (attach.getIsAir()) {
          airRules.add(rule);
        } else {
          landRules.add(rule);
        }
      } else if (resourceOrUnit instanceof Resource) {
        resourceRules.add(rule);
      }
    }
    ruleLists.add(Tuple.of("All", allRules));
    ruleLists.add(Tuple.of("Land", landRules));
    ruleLists.add(Tuple.of("Air", airRules));
    ruleLists.add(Tuple.of("Sea", seaRules));
    ruleLists.add(Tuple.of("Construction", constructRules));
    ruleLists.add(Tuple.of("Upgrades/Consumes", upgradeConsumesRules));
    ruleLists.add(Tuple.of("Resources", resourceRules));
    return ruleLists;
  }

  private JPanel getRulesPanel(final List<Rule> rules) {
    final JPanel panel = JPanelBuilder.builder()
        .gridLayout(rows, columns)
        .build();

    final JPanel[][] panelHolder = new JPanel[rows][columns];
    for (int m = 0; m < rows; m++) {
      for (int n = 0; n < columns; n++) {
        panelHolder[m][n] = new JPanel(new BorderLayout());
        panel.add(panelHolder[m][n]);
      }
    }
    for (int x = 0; x < (columns * rows); x++) {
      if (x < rules.size()) {
        panelHolder[(x % rows)][(x / rows)].add(rules.get(x).getPanelComponent());
      }
    }
    return panel;
  }
}
