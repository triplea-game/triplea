package games.strategy.triplea.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.util.ResourceCollectionUtils;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.ui.SwingAction;
import games.strategy.util.IntegerMap;
import swinglib.JDialogBuilder;

public class ProductionPanel extends JPanel {
  private static final long serialVersionUID = -1539053979479586609L;

  protected final UiContext uiContext;
  protected List<Rule> rules = new ArrayList<>();
  protected JLabel left = new JLabel();
  protected JButton done;
  protected PlayerID id;
  protected GameData data;

  private JDialog dialog;
  private boolean bid;


  public static IntegerMap<ProductionRule> getProduction(final PlayerID id, final JFrame parent, final GameData data,
      final boolean bid, final IntegerMap<ProductionRule> initialPurchase, final UiContext uiContext) {
    return new ProductionPanel(uiContext).show(id, parent, data, bid, initialPurchase);
  }

  private IntegerMap<ProductionRule> getProduction() {
    final IntegerMap<ProductionRule> prod = new IntegerMap<>();
    for (final Rule rule : rules) {
      final int quantity = rule.getQuantity();
      if (quantity != 0) {
        prod.put(rule.getProductionRule(), quantity);
      }
    }
    return prod;
  }

  protected ProductionPanel(final UiContext uiContext) {
    this.uiContext = uiContext;
  }


  /**
   * Shows the production panel, and returns a map of selected rules.
   */
  public IntegerMap<ProductionRule> show(final PlayerID id, final JFrame parent, final GameData data, final boolean bid,
      final IntegerMap<ProductionRule> initialPurchase) {
    final JDialogBuilder dialogBuilder = JDialogBuilder.builder()
        .contents(this)
        .title("Produce");
    if (parent != null) {
      dialogBuilder.parentFrame(parent);
    }
    dialog = dialogBuilder.build();

    this.bid = bid;
    this.data = data;
    this.initRules(id, initialPurchase);
    this.initLayout();
    this.calculateLimits();
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    SwingUtilities.invokeLater(() -> done.requestFocusInWindow());
    // making the dialog visible will block until it is closed
    dialog.setVisible(true);
    dialog.dispose();
    return getProduction();
  }

  // this method can be accessed by subclasses
  protected List<Rule> getRules() {
    return rules;
  }

  protected void initRules(final PlayerID player, final IntegerMap<ProductionRule> initialPurchase) {
    this.data.acquireReadLock();
    try {
      id = player;
      for (final ProductionRule productionRule : player.getProductionFrontier()) {
        final Rule rule = new Rule(productionRule, player);
        final int initialQuantity = initialPurchase.getInt(productionRule);
        rule.setQuantity(initialQuantity);
        rules.add(rule);
      }
    } finally {
      this.data.releaseReadLock();
    }
  }

  protected void initLayout() {
    final Insets nullInsets = new Insets(0, 0, 0, 0);
    this.removeAll();
    this.setLayout(new GridBagLayout());
    final JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    final JLabel legendLabel = new JLabel(String.format(
        "<html>Attack/Defense/Movement. &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; "
            + "(Total Resources: %s)</html>",
        ResourceCollectionUtils.getProductionResources(getResources())));
    this.add(legendLabel, new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST,
        GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));
    int rows = rules.size() / 7;
    rows = Math.max(2, rows);
    for (int x = 0; x < rules.size(); x++) {
      panel.add(rules.get(x).getPanelComponent(), new GridBagConstraints(x / rows, (x % rows), 1, 1, 10, 10,
          GridBagConstraints.EAST, GridBagConstraints.BOTH, nullInsets, 0, 0));
    }
    final JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    final int availHeight = screenResolution.height - 80;
    final int availWidth = screenResolution.width - 30;
    final int availWidthRules = availWidth - 16;
    final int availHeightRules = availHeight - 116;
    scroll
        .setPreferredSize(
            new Dimension(
                ((scroll.getPreferredSize().width > availWidthRules)
                    ?
                    availWidthRules
                    :
                    (scroll.getPreferredSize().width + ((scroll.getPreferredSize().height > availHeightRules)
                        ? 25
                        : 0))),
                ((scroll.getPreferredSize().height > availHeightRules) ? availHeightRules
                    : (scroll.getPreferredSize().height
                    + ((scroll.getPreferredSize().width > availWidthRules) ? 25 : 0)))));
    this.add(scroll, new GridBagConstraints(0, 1, 30, 1, 100, 100, GridBagConstraints.WEST, GridBagConstraints.BOTH,
        new Insets(8, 8, 8, 4), 0, 0));
    // final int startY = m_rules.size() / rows;
    this.add(left, new GridBagConstraints(0, 2, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(8, 8, 0, 12), 0, 0));
    done = new JButton(doneAction);
    this.add(done, new GridBagConstraints(0, 3, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 8, 0), 0, 0));
    this.setMaximumSize(new Dimension(availWidth, availHeight));
  }

  // This method can be overridden by subclasses
  protected void setLeft(final ResourceCollection left, final int totalUnits) {
    this.left.setText(String.format(
        "%d total units purchased.  You have %s left.",
        totalUnits, ResourceCollectionUtils.getProductionResources(left)));
  }

  Action doneAction = SwingAction.of("Done", e -> dialog.setVisible(false));

  // This method can be overridden by subclasses
  protected void calculateLimits() {
    // final IntegerMap<Resource> cost;
    final ResourceCollection resources = getResources();
    final ResourceCollection spent = new ResourceCollection(data);
    int totalUnits = 0;
    for (final Rule current : rules) {
      spent.add(current.getCost(), current.getQuantity());
      totalUnits += current.getQuantity() * current.getProductionRule().getResults().totalValues();
    }
    final ResourceCollection leftToSpend = resources.difference(spent);
    setLeft(leftToSpend, totalUnits);
    for (final Rule current : rules) {
      int max = leftToSpend.fitsHowOften(current.getCost());
      max += current.getQuantity();
      current.setMax(max);
    }
  }

  protected ResourceCollection getResources() {
    if (bid) {
      // TODO bid only allows you to add PU's to the bid... maybe upgrading Bids so multiple resources can be given?
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

    return ((id == null) || id.isNull()) ? new ResourceCollection(data) : id.getResources();
  }

  class Rule {
    private final IntegerMap<Resource> cost;
    private int quantity;
    private final ProductionRule rule;
    private final PlayerID id;
    private final Set<ScrollableTextField> textFields = new HashSet<>();

    protected JPanel getPanelComponent() {
      final JPanel panel = new JPanel();
      final ScrollableTextField textField = new ScrollableTextField(0, Integer.MAX_VALUE);
      textField.setValue(quantity);
      panel.setLayout(new GridBagLayout());
      final JLabel info = new JLabel("  ");
      final JLabel name = new JLabel("  ");
      final Color defaultForegroundLabelColor = name.getForeground();
      Optional<ImageIcon> icon = Optional.empty();
      final StringBuilder tooltip = new StringBuilder();
      final Set<NamedAttachable> results = new HashSet<>(rule.getResults().keySet());
      final Iterator<NamedAttachable> iter = results.iterator();
      while (iter.hasNext()) {
        final NamedAttachable resourceOrUnit = iter.next();
        if (resourceOrUnit instanceof UnitType) {
          final UnitType type = (UnitType) resourceOrUnit;
          icon = uiContext.getUnitImageFactory().getIcon(type, id, false, false);
          final UnitAttachment attach = UnitAttachment.get(type);
          final int attack = attach.getAttack(id);
          final int movement = attach.getMovement(id);
          final int defense = attach.getDefense(id);
          info.setText(attack + "/" + defense + "/" + movement);
          tooltip.append(type.getName()).append(": ").append(type.getTooltip(id));
          name.setText(type.getName());
          if ((attach.getConsumesUnits() != null) && (attach.getConsumesUnits().totalValues() == 1)) {
            name.setForeground(Color.CYAN);
          } else if ((attach.getConsumesUnits() != null) && (attach.getConsumesUnits().totalValues() > 1)) {
            name.setForeground(Color.BLUE);
          } else {
            name.setForeground(defaultForegroundLabelColor);
          }
        } else if (resourceOrUnit instanceof Resource) {
          final Resource resource = (Resource) resourceOrUnit;
          icon = Optional.of(uiContext.getResourceImageFactory().getIcon(resource, true));
          info.setText("resource");
          tooltip.append(resource.getName()).append(": resource");
          name.setText(resource.getName());
          name.setForeground(Color.GREEN);
        }
        if (iter.hasNext()) {
          tooltip.append("<br /><br /><br /><br />");
        }
      }
      final int numberOfUnitsGiven = rule.getResults().totalValues();
      final String text;
      if (numberOfUnitsGiven > 1) {
        text = "<html> x " + ResourceCollection.toStringForHtml(cost, data) + "<br>" + "for " + numberOfUnitsGiven
            + "<br>" + " units</html>";
      } else {
        text = "<html> x " + ResourceCollection.toStringForHtml(cost, data) + "</html>";
      }
      final JLabel label =
          icon.isPresent() ? new JLabel(text, icon.get(), SwingConstants.LEFT) : new JLabel(text, SwingConstants.LEFT);
      final String toolTipText = "<html>" + tooltip + "</html>";
      info.setToolTipText(toolTipText);
      label.setToolTipText(toolTipText);
      final int space = 8;
      // change name color for 'upgrades and consumes' unit types
      panel.add(name, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(2, 0, 0, 0), 0, 0));
      panel.add(label, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(5, space, space, space), 0, 0));
      panel.add(info, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(5, space, space, space), 0, 0));
      panel.add(textField, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(10, space, space, space), 0, 0));
      textField.addChangeListener(listener);
      textFields.add(textField);
      panel.setBorder(new EtchedBorder());
      return panel;
    }

    Rule(final ProductionRule rule, final PlayerID id) {
      this.rule = rule;
      cost = rule.getCosts();
      this.id = id;
    }

    IntegerMap<Resource> getCost() {
      return cost;
    }

    int getQuantity() {
      return quantity;
    }

    void setQuantity(final int quantity) {
      this.quantity = quantity;
      for (final ScrollableTextField textField : textFields) {
        if (textField.getValue() != quantity) {
          textField.setValue(quantity);
        }
      }
    }

    ProductionRule getProductionRule() {
      return rule;
    }

    void setMax(final int max) {
      for (final ScrollableTextField textField : textFields) {
        textField.setMax(max);
      }
    }

    private final ScrollableTextFieldListener listener = new ScrollableTextFieldListener() {
      @Override
      public void changedValue(final ScrollableTextField stf) {
        if (stf.getValue() != quantity) {
          quantity = stf.getValue();
          calculateLimits();
          for (final ScrollableTextField textField : textFields) {
            if (!stf.equals(textField)) {
              textField.setValue(quantity);
            }
          }
        }
      }
    };
  }
}
