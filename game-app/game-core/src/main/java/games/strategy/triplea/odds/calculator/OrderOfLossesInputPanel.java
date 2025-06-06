package games.strategy.triplea.odds.calculator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.image.UnitImageFactory.ImageKey;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.awt.Color;
import java.awt.Component;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.util.Tuple;

/**
 * Order of loss panel, helps user create an order of loss string that is used to choose casualty
 * order.
 */
class OrderOfLossesInputPanel extends JPanel {
  @Serial private static final long serialVersionUID = 8815617685388156219L;
  private static final char OOL_SEPARATOR = ';';
  private static final char OOL_AMOUNT_DESCRIPTOR = '^';
  private static final String OOL_ALL = "*";

  private final GameData data;
  private final UiContext uiContext;
  private final List<UnitCategory> attackerCategories;
  private final List<UnitCategory> defenderCategories;
  private final JTextField attackerTextField;
  private final JTextField defenderTextField;
  private final JLabel attackerLabel = new JLabel("Attacker Units:");
  private final JLabel defenderLabel = new JLabel("Defender Units:");
  private final JButton clear;
  private final boolean land;

  OrderOfLossesInputPanel(
      final String attackerOrder,
      final String defenderOrder,
      final List<UnitCategory> attackerCategories,
      final List<UnitCategory> defenderCategories,
      final boolean land,
      final UiContext uiContext,
      final GameData data) {
    this.data = data;
    this.uiContext = uiContext;
    this.land = land;
    this.attackerCategories = attackerCategories;
    this.defenderCategories = defenderCategories;
    attackerTextField = new JTextField(attackerOrder == null ? "" : attackerOrder);

    new DocumentListenerBuilder(() -> turnToRedIfNotValidOrderOfLoss(attackerTextField))
        .attachTo(attackerTextField);
    defenderTextField = new JTextField(defenderOrder == null ? "" : defenderOrder);

    new DocumentListenerBuilder(() -> turnToRedIfNotValidOrderOfLoss(defenderTextField))
        .attachTo(defenderTextField);

    clear = new JButton("Clear");
    clear.addActionListener(
        e -> {
          attackerTextField.setText("");
          defenderTextField.setText("");
        });
    layoutComponents();
  }

  private void turnToRedIfNotValidOrderOfLoss(final JTextField textField) {
    textField.setForeground(isValidOrderOfLoss(textField.getText(), data) ? null : Color.red);
  }

  /** Validates if a given string can be parsed for order of losses. */
  static boolean isValidOrderOfLoss(final String orderOfLoss, final GameData data) {
    if (orderOfLoss == null || orderOfLoss.isBlank()) {
      return true;
    }
    try {
      final UnitTypeList unitTypes;
      try (GameData.Unlocker ignored = data.acquireReadLock()) {
        unitTypes = data.getUnitTypeList();
      }
      for (final String section : splitOrderOfLoss(orderOfLoss)) {
        if (section.length() == 0) {
          continue;
        }
        final String[] amountThenType = splitOrderOfLossSection(section);
        if (amountThenType.length != 2) {
          return false;
        }
        if (!amountThenType[0].equals(OOL_ALL)) {
          final int amount = Integer.parseInt(amountThenType[0]);
          if (amount <= 0) {
            return false;
          }
        }
        if (unitTypes.getUnitType(amountThenType[1]).isEmpty()) {
          return false;
        }
      }
    } catch (final Exception e) {
      return false;
    }
    return true;
  }

  @VisibleForTesting
  static Iterable<String> splitOrderOfLoss(final String orderOfLoss) {
    return Splitter.on(OOL_SEPARATOR).split(orderOfLoss.trim());
  }

  @VisibleForTesting
  static String[] splitOrderOfLossSection(final String orderOfLossSection) {
    return Iterables.toArray(
        Splitter.on(OOL_AMOUNT_DESCRIPTOR).split(orderOfLossSection), String.class);
  }

  /** Returns units in the same ordering as the 'order of loss' string passed in. */
  static List<Unit> getUnitListByOrderOfLoss(
      final String ool, final Collection<Unit> units, final GameState data) {
    if (ool == null || ool.isBlank()) {
      return null;
    }
    final List<Tuple<Integer, UnitType>> map = new ArrayList<>();
    for (final String section : splitOrderOfLoss(ool)) {
      if (section.length() == 0) {
        continue;
      }
      final String[] amountThenType = splitOrderOfLossSection(section);
      final int amount =
          amountThenType[0].equals(OOL_ALL)
              ? Integer.MAX_VALUE
              : Integer.parseInt(amountThenType[0]);
      final UnitType type = data.getUnitTypeList().getUnitTypeOrThrow(amountThenType[1]);
      map.add(Tuple.of(amount, type));
    }
    Collections.reverse(map);
    final Set<Unit> unitsLeft = new HashSet<>(units);
    final List<Unit> order = new ArrayList<>();
    for (final Tuple<Integer, UnitType> section : map) {
      final List<Unit> unitsOfType =
          CollectionUtils.getNMatches(
              unitsLeft, section.getFirst(), Matches.unitIsOfType(section.getSecond()));
      order.addAll(unitsOfType);
      unitsLeft.removeAll(unitsOfType);
    }
    Collections.reverse(order);
    return order;
  }

  private void layoutComponents() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    final JLabel instructions =
        new JLabel(
            "<html>Here you can specify the 'Order of Losses' (OOL) for each side."
                + "<br />Damageable units will be damaged first always. If the player label is "
                + "red, your OOL is invalid."
                + "<br />The engine will take your input and add all units to a list starting "
                + "on the RIGHT side of your text line."
                + "<br />Then, during combat, casualties will be chosen starting on the LEFT side "
                + "of your OOL."
                + "<br />"
                + OOL_SEPARATOR
                + " separates unit types."
                + "<br />"
                + OOL_AMOUNT_DESCRIPTOR
                + " is in front of the unit type and describes the number of units."
                + "<br />"
                + OOL_ALL
                + " means all units of that type."
                + "<br />Examples:"
                + "<br />"
                + OOL_ALL
                + OOL_AMOUNT_DESCRIPTOR
                + "infantry"
                + OOL_SEPARATOR
                + OOL_ALL
                + OOL_AMOUNT_DESCRIPTOR
                + "artillery"
                + OOL_SEPARATOR
                + OOL_ALL
                + OOL_AMOUNT_DESCRIPTOR
                + "fighter"
                + "<br />The above will take all infantry, then all artillery, then all fighters, "
                + "then all other units as casualty."
                + "<br /><br />1"
                + OOL_AMOUNT_DESCRIPTOR
                + "infantry"
                + OOL_SEPARATOR
                + "2"
                + OOL_AMOUNT_DESCRIPTOR
                + "artillery"
                + OOL_SEPARATOR
                + "6"
                + OOL_AMOUNT_DESCRIPTOR
                + "fighter"
                + "<br />The above will take 1 infantry, then 2 artillery, then 6 fighters, "
                + "then all other units as casualty."
                + "<br /><br />"
                + OOL_ALL
                + OOL_AMOUNT_DESCRIPTOR
                + "infantry"
                + OOL_SEPARATOR
                + OOL_ALL
                + OOL_AMOUNT_DESCRIPTOR
                + "fighter"
                + OOL_SEPARATOR
                + "1"
                + OOL_AMOUNT_DESCRIPTOR
                + "infantry"
                + "<br />The above will take all except 1 infantry casualty, then all fighters, "
                + "then the last infantry, then all other units casualty.</html>");
    instructions.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(instructions);
    add(Box.createVerticalStrut(30));
    attackerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(attackerLabel);
    final JPanel attackerUnits = getUnitButtonPanel(attackerCategories, attackerTextField);
    attackerUnits.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(attackerUnits);
    attackerTextField.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(attackerTextField);
    add(Box.createVerticalStrut(30));
    defenderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(defenderLabel);
    final JPanel defenderUnits = getUnitButtonPanel(defenderCategories, defenderTextField);
    defenderUnits.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(defenderUnits);
    defenderTextField.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(defenderTextField);
    add(Box.createVerticalStrut(10));
    clear.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(clear);
  }

  private JPanel getUnitButtonPanel(
      final List<UnitCategory> unitCategories, final JTextField textField) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    if (unitCategories != null) {
      UnitSeparator.sortUnitCategories(unitCategories, data);
      final Set<UnitType> typesUsed = new HashSet<>();
      for (final UnitCategory category : unitCategories) {
        // no duplicates or infrastructure allowed. no sea if land, no land if sea.
        if (typesUsed.contains(category.getType())
            || Matches.unitTypeIsInfrastructure().test(category.getType())
            || (land && Matches.unitTypeIsSea().test(category.getType()))
            || (!land && Matches.unitTypeIsLand().test(category.getType()))) {
          continue;
        }
        final String unitName = OOL_ALL + OOL_AMOUNT_DESCRIPTOR + category.getType().getName();
        final String toolTipText =
            "<html>"
                + category.getType().getName()
                + ":  "
                + uiContext
                    .getTooltipProperties()
                    .getTooltip(category.getType(), category.getOwner())
                + "</html>";
        final ImageIcon img = uiContext.getUnitImageFactory().getIcon(ImageKey.of(category));
        final JButton button = new JButton(img);
        button.setToolTipText(toolTipText);
        button.addActionListener(
            e ->
                textField.setText(
                    (textField.getText().length() > 0 ? (textField.getText() + OOL_SEPARATOR) : "")
                        + unitName));
        panel.add(button);
        typesUsed.add(category.getType());
      }
    }
    return panel;
  }

  String getAttackerOrder() {
    return attackerTextField.getText();
  }

  String getDefenderOrder() {
    return defenderTextField.getText();
  }
}
