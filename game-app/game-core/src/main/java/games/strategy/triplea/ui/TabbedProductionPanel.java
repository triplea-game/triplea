package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.util.Tuple;

class TabbedProductionPanel extends ProductionPanel {
  private static final long serialVersionUID = 3481282212500641144L;
  private int rows;
  private int columns;
  private final Map<Rule, JPanel> rulesComponents = new HashMap<>();
  private Dimension cellSize;

  protected TabbedProductionPanel(final UiContext uiContext) {
    super(uiContext);
  }

  static IntegerMap<ProductionRule> getProduction(
      final GamePlayer gamePlayer,
      final JFrame parent,
      final GameData data,
      final boolean bid,
      final IntegerMap<ProductionRule> initialPurchase,
      final UiContext uiContext) {
    return new TabbedProductionPanel(uiContext)
        .show(gamePlayer, parent, data, bid, initialPurchase);
  }

  @Override
  protected void initLayout() {
    this.removeAll();
    this.setLayout(new BorderLayout());
    final JTabbedPane tabs = new JTabbedPane();
    add(tabs, BorderLayout.CENTER);
    final ProductionTabsProperties properties =
        new ProductionTabsProperties(gamePlayer, rules, uiContext.getResourceLoader());
    final List<Tuple<String, List<Rule>>> ruleLists = getRuleLists(properties);
    cellSize = new Dimension();
    for (Rule rule : rules) {
      JPanel component = rule.getPanelComponent();
      rulesComponents.put(rule, component);
      // Use the same size for all cells by getting the max size for both dimensions.
      cellSize.width = Math.max(cellSize.width, component.getPreferredSize().width);
      cellSize.height = Math.max(cellSize.height, component.getPreferredSize().height);
    }
    calculateRowsAndColumns(properties, largestList(ruleLists));

    for (final Tuple<String, List<Rule>> ruleList : ruleLists) {
      JScrollPane scrollPane = new JScrollPane();
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      tabs.addTab(ruleList.getFirst(), scrollPane);
    }
    updateTabContent(tabs, ruleLists);
    tabs.addChangeListener(e -> updateTabContent(tabs, ruleLists));

    final JPanel totals = new JPanel();
    totals.add(left);
    totals.add(remainingResources);

    final JPanel bottom = new JPanel();
    bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
    bottom.setLayout(new BorderLayout());
    bottom.add(totals, BorderLayout.WEST);
    bottom.add(new JLabel("Attack | Defense | Movement"), BorderLayout.EAST);
    JPanel donePanel = new JPanel();
    donePanel.setLayout(new BoxLayout(donePanel, BoxLayout.Y_AXIS));
    donePanel.add(Box.createVerticalGlue());
    donePanel.add(done);
    done.setAlignmentX(Component.CENTER_ALIGNMENT);
    donePanel.add(Box.createVerticalGlue());
    bottom.add(donePanel, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);
  }

  private void calculateRowsAndColumns(
      final ProductionTabsProperties properties, final int largestList) {
    if (properties.getRows() == 0
        || properties.getColumns() == 0
        || properties.getRows() * properties.getColumns() < largestList) {
      final int desiredColumns;
      if (largestList <= 36) {
        desiredColumns = Math.max(8, Math.min(12, divideRoundUp(largestList, 3)));
      } else if (largestList <= 64) {
        desiredColumns = Math.max(8, Math.min(16, divideRoundUp(largestList, 4)));
      } else {
        desiredColumns = Math.max(8, Math.min(16, divideRoundUp(largestList, 5)));
      }
      // Limit number of columns to the available width. Reserve 64px for borders & scroll bar.
      final int availableScreenWidth = Util.getScreenSize(dialog).width - 64;
      final int maxColumns = Math.min(desiredColumns, availableScreenWidth / cellSize.width);
      rows = Math.max(2, divideRoundUp(largestList, maxColumns));
      columns = Math.max(3, divideRoundUp(largestList, rows));
    } else {
      rows = Math.max(2, properties.getRows());
      // There are small display problems if the size is less than 2x3 cells.
      columns = Math.max(3, properties.getColumns());
    }
  }

  private static int divideRoundUp(int numerator, int denominator) {
    return (int) Math.ceil((double) numerator / denominator);
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
    if (!rulesCopy.isEmpty()) {
      final String missing =
          rulesCopy.stream()
              .map(rule -> rule.getProductionRule().getName())
              .collect(Collectors.joining(", "));
      throw new IllegalStateException(
          "production_tabs: must include all player production rules/units; missing: " + missing);
    }
  }

  private List<Tuple<String, List<Rule>>> getRuleLists(final ProductionTabsProperties properties) {
    final List<Tuple<String, List<Rule>>> ruleLists;
    if (properties.useDefaultTabs()) {
      ruleLists = getDefaultRuleLists();
    } else {
      ruleLists = properties.getRuleLists();
      checkLists(ruleLists);
    }
    // Return only non-empty rule lists.
    return ruleLists.stream().filter(e -> !e.getSecond().isEmpty()).collect(Collectors.toList());
  }

  private List<Tuple<String, List<Rule>>> getDefaultRuleLists() {
    final List<Tuple<String, List<Rule>>> ruleLists = new ArrayList<>();
    final List<Rule> allRules = new ArrayList<>();
    final List<Rule> landRules = new ArrayList<>();
    final List<Rule> airRules = new ArrayList<>();
    final List<Rule> seaRules = new ArrayList<>();
    final List<Rule> constructRules = new ArrayList<>();
    final List<Rule> upgradeConsumesRules = new ArrayList<>();
    final List<Rule> resourceRules = new ArrayList<>();
    for (final Rule rule : rules) {
      allRules.add(rule);
      final NamedAttachable resourceOrUnit = rule.getProductionRule().getAnyResultKey();
      if (resourceOrUnit instanceof UnitType) {
        final UnitType type = (UnitType) resourceOrUnit;
        final UnitAttachment attach = type.getUnitAttachment();
        if (attach.getConsumesUnits().totalValues() >= 1) {
          upgradeConsumesRules.add(rule);
        }
        // canProduceUnits isn't checked on purpose, since this category is for units that can be
        // placed anywhere (placed without needing a factory).
        if (attach.isConstruction()) {
          constructRules.add(rule);
        } else if (attach.isSea()) {
          seaRules.add(rule);
        } else if (attach.isAir()) {
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

  private JPanel createRulesGrid(List<Rule> rules) {
    final JPanel panel = new JPanelBuilder().gridLayout(0, columns).build();
    for (Rule rule : rules) {
      JPanel cell = rulesComponents.get(rule);
      cell.setPreferredSize(cellSize);
      panel.add(cell);
    }
    // Add the panel to an outer panel to absorb any extra height if there's too few cells.
    panel.setOpaque(false);
    JPanel outer = new JPanel();
    outer.add(panel);
    return outer;
  }

  private void updateTabContent(JTabbedPane tabs, List<Tuple<String, List<Rule>>> ruleLists) {
    final List<Rule> rules = ruleLists.get(tabs.getSelectedIndex()).getSecond();
    final JScrollPane scrollPane = (JScrollPane) tabs.getSelectedComponent();
    scrollPane.setViewportView(createRulesGrid(rules));
  }
}
