package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.image.UnitImageFactory.ImageKey;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.ui.ScrollableTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.triplea.java.collections.IntegerMap;

/** Panel showing a unit image. */
public class UnitPanel extends JPanel {
  private static final long serialVersionUID = 1509643150038705671L;
  private final UnitCategory category;
  private final ScrollableTextField textField;
  private final List<Runnable> listeners = new ArrayList<>();

  UnitPanel(
      final UiContext uiContext, final UnitCategory category, final IntegerMap<UnitType> costs) {
    this.category = category;
    textField = new ScrollableTextField(0, 512);
    textField.setShowMaxAndMin(false);
    textField.addChangeListener(field -> notifyListeners());

    final String toolTipText =
        "<html>"
            + category.getType().getName()
            + ":  "
            + costs.getInt(category.getType())
            + " cost, <br /> &nbsp;&nbsp;&nbsp;&nbsp; "
            + uiContext.getTooltipProperties().getTooltip(category.getType(), category.getOwner())
            + "</html>";
    setCount(category.getUnits().size());
    setLayout(new GridBagLayout());

    final JLabel label = new JLabel(uiContext.getUnitImageFactory().getIcon(ImageKey.of(category)));
    label.setToolTipText(toolTipText);
    add(
        label,
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 10),
            0,
            0));
    add(
        textField,
        new GridBagConstraints(
            1,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
  }

  List<Unit> getUnits() {
    final List<Unit> units =
        category.getType().createTemp(textField.getValue(), category.getOwner());
    if (!units.isEmpty()) {
      // creating the unit just makes it, we want to make sure it is damaged if the category says it
      // is damaged
      if (category.getHitPoints() > 1 && category.getDamaged() > 0) {
        // we do not need to use bridge and change factory here because this is not sent over the
        // network. these are
        // just some temporary units for the battle calc.
        for (final Unit u : units) {
          u.setHits(category.getDamaged());
        }
      }
      if (category.getDisabled() && Matches.unitTypeCanBeDamaged().test(category.getType())) {
        // add 1 because it is the max operational damage and we want to disable it
        final int unitDamage =
            Math.max(0, 1 + category.getUnitAttachment().getMaxOperationalDamage());
        for (final Unit unit : units) {
          unit.setUnitDamage(unitDamage);
        }
      }
    }
    return units;
  }

  void setCount(final int value) {
    textField.setValue(value);
  }

  int getCount() {
    return textField.getValue();
  }

  void addChangeListener(final Runnable listener) {
    listeners.add(listener);
  }

  private void notifyListeners() {
    listeners.forEach(Runnable::run);
  }
}
