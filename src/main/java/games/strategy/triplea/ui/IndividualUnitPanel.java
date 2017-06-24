package games.strategy.triplea.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.IntegerMap;
import games.strategy.util.Triple;

/**
 * For when you do not want things condensed into categories.
 *
 * <p>
 * This creates a panel which shows a group of units individually, and lets you put points/hits towards each unit
 * individually.
 * It lets you set a max number of points total (though max per unit is not allowed yet). It can return an IntegerMap
 * with the points per
 * unit.
 * </p>
 */
public class IndividualUnitPanel extends JPanel {
  private static final long serialVersionUID = -4222938655315991715L;
  private final List<SingleUnitPanel> entries = new ArrayList<>();
  private final JTextArea title;
  private int max = -1;
  private final JLabel leftToSelect = new JLabel();
  private final GameData gameData;
  private final IUIContext uiContext;
  private ScrollableTextField textFieldPurelyForListening;
  private final ScrollableTextFieldListener countOptionalTextFieldListener;
  private final boolean showSelectAll;
  private final ScrollableTextFieldListener textFieldListener = field -> updateLeft();

  /**
   * For when you do not want things condensed into categories.
   * This creates a panel which shows a group of units individually, and lets you put points/hits towards each unit
   * individually.
   * It lets you set a max number of points total (though max per unit is not allowed yet). It can return an IntegerMap
   * with the points per
   * unit.
   */
  public IndividualUnitPanel(final Collection<Unit> units, final String title, final GameData data,
      final IUIContext uiContext, final int max, final boolean showMinAndMax, final boolean showSelectAll,
      final ScrollableTextFieldListener optionalListener) {
    gameData = data;
    this.uiContext = uiContext;
    this.title = new JTextArea(title);
    this.title.setBackground(this.getBackground());
    this.title.setEditable(false);
    this.title.setWrapStyleWord(true);
    countOptionalTextFieldListener = optionalListener;
    setMaxAndShowMaxButton(max);
    this.showSelectAll = showSelectAll;
    for (final Unit u : units) {
      entries.add(new SingleUnitPanel(u, gameData, this.uiContext, textFieldListener, this.max, 0, showMinAndMax));
    }
    layoutEntries();
  }

  /**
   * For when you do not want things condensed into categories.
   * This creates a panel which shows a group of units individually, and lets you put points/hits towards each unit
   * individually.
   * It lets you set a max number of points total AND per unit. It can return an IntegerMap with the points per unit.
   *
   * @param unitsAndTheirMaxMinAndCurrent
   *        mapped to their individual max, then min, then current values
   */
  public IndividualUnitPanel(final HashMap<Unit, Triple<Integer, Integer, Integer>> unitsAndTheirMaxMinAndCurrent,
      final String title, final GameData data, final IUIContext context, final int max, final boolean showMinAndMax,
      final boolean showSelectAll, final ScrollableTextFieldListener optionalListener) {
    gameData = data;
    uiContext = context;
    this.title = new JTextArea(title);
    this.title.setBackground(this.getBackground());
    this.title.setEditable(false);
    this.title.setWrapStyleWord(true);
    countOptionalTextFieldListener = optionalListener;
    setMaxAndShowMaxButton(max);
    this.showSelectAll = showSelectAll;
    for (final Entry<Unit, Triple<Integer, Integer, Integer>> entry : unitsAndTheirMaxMinAndCurrent.entrySet()) {
      final int unitMax = entry.getValue().getFirst();
      final int thisMax;
      if (this.max < 0 && unitMax < 0) {
        thisMax = -1;
      } else if (unitMax < 0) {
        thisMax = this.max;
      } else if (this.max < 0) {
        thisMax = unitMax;
      } else {
        thisMax = Math.min(this.max, unitMax);
      }
      final int thisMin = Math.max(0, entry.getValue().getSecond());
      final int thisCurrent = Math.max(thisMin, Math.min(thisMax, entry.getValue().getThird()));
      entries.add(new SingleUnitPanel(entry.getKey(), gameData, uiContext, textFieldListener, thisMax, thisMin,
          thisCurrent, showMinAndMax));
    }
    layoutEntries();
  }

  private void setMaxAndShowMaxButton(final int max) {
    this.max = max;
    textFieldPurelyForListening = new ScrollableTextField(0, 0);
    textFieldListener.changedValue(null);
    if (countOptionalTextFieldListener != null) {
      textFieldPurelyForListening.addChangeListener(countOptionalTextFieldListener);
    }
  }

  public void setTitle(final String title) {
    this.title.setText(title);
  }

  public int getMax() {
    return max;
  }

  void setMaxAndUpdate(final int newMax) {
    max = newMax;
    updateLeft();
    textFieldPurelyForListening.setValue(0);
  }

  private void updateLeft() {
    if (max == -1) {
      return;
    }
    final int selected = getSelectedCount();
    final int newMax = max - selected;
    for (final SingleUnitPanel entry : entries) {
      final int current = entry.getCount();
      final int maxForThis = current + newMax;
      entry.setMax(maxForThis);
    }
    leftToSelect.setText("Left to select:" + newMax);
    textFieldPurelyForListening.setValue(0);
  }

  protected int getSelectedCount() {
    int selected = 0;
    for (final SingleUnitPanel entry : entries) {
      selected += entry.getCount();
    }
    return selected;
  }

  private void layoutEntries() {
    this.setLayout(new GridBagLayout());
    final Insets nullInsets = new Insets(0, 0, 0, 0);
    final Dimension buttonSize = new Dimension(80, 20);
    final JButton m_selectNoneButton = new JButton("None");
    m_selectNoneButton.setPreferredSize(buttonSize);
    final JButton m_autoSelectButton = new JButton("Max");
    m_autoSelectButton.setPreferredSize(buttonSize);
    add(title, new GridBagConstraints(0, 0, 7, 1, 0, 0.5, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        nullInsets, 0, 0));
    m_selectNoneButton.addActionListener(e -> selectNone());
    m_autoSelectButton.addActionListener(e -> autoSelect());
    int yIndex = 1;
    for (final SingleUnitPanel entry : entries) {
      entry.createComponents(this, yIndex);
      yIndex++;
    }
    if (showSelectAll) {
      add(m_autoSelectButton, new GridBagConstraints(0, yIndex, 7, 1, 0, 0.5, GridBagConstraints.EAST,
          GridBagConstraints.NONE, nullInsets, 0, 0));
      yIndex++;
    }
    add(leftToSelect, new GridBagConstraints(0, yIndex, 5, 2, 0, 0.5, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
  }

  IntegerMap<Unit> getSelected() {
    final IntegerMap<Unit> selectedUnits = new IntegerMap<>();
    for (final SingleUnitPanel entry : entries) {
      selectedUnits.put(entry.getUnit(), entry.getCount());
    }
    return selectedUnits;
  }

  protected void selectNone() {
    for (final SingleUnitPanel entry : entries) {
      entry.selectNone();
    }
  }

  protected void autoSelect() {
    if (max == -1) {
      for (final SingleUnitPanel entry : entries) {
        entry.selectAll();
      }
    } else {
      int leftToSelect = max - getSelectedCount();
      for (final SingleUnitPanel entry : entries) {
        final int leftToSelectForCurrent = leftToSelect + entry.getCount();
        final int canSelect = entry.getMax();
        if (leftToSelectForCurrent >= canSelect) {
          entry.selectAll();
          leftToSelect -= canSelect;
        } else {
          entry.setCount(leftToSelectForCurrent);
          leftToSelect = 0;
          break;
        }
      }
    }
  }
}


class SingleUnitPanel extends JPanel {
  private static final long serialVersionUID = 5034287842323633030L;
  private final Unit unit;
  private final ScrollableTextField textField;
  private static final Insets nullInsets = new Insets(0, 0, 0, 0);
  private final ScrollableTextFieldListener countTextFieldListener;

  public SingleUnitPanel(final Unit unit, final GameData data, final IUIContext uiContext,
      final ScrollableTextFieldListener textFieldListener, final int max, final int min, final boolean showMaxAndMin) {
    this(unit, data, uiContext, textFieldListener, max, min, 0, showMaxAndMin);
  }

  public SingleUnitPanel(final Unit unit, final GameData data, final IUIContext context,
      final ScrollableTextFieldListener textFieldListener, final int max, final int min, final int currentValue,
      final boolean showMaxAndMin) {
    this.unit = unit;
    final GameData m_data = data;
    final IUIContext m_context = context;
    countTextFieldListener = textFieldListener;
    textField = new ScrollableTextField(0, 512);
    if (max >= 0) {
      setMax(max);
    }
    setMin(min);
    textField.setShowMaxAndMin(showMaxAndMin);
    final TripleAUnit taUnit = TripleAUnit.get(unit);


    setCount(currentValue);
    setLayout(new GridBagLayout());

    final boolean isDamaged = taUnit.getUnitDamage() > 0 || taUnit.getHits() > 0;
    final JLabel label = m_context.createUnitImageJLabel(this.unit.getType(), this.unit.getOwner(), m_data,
        isDamaged ? IUIContext.UnitDamage.DAMAGED : IUIContext.UnitDamage.NOT_DAMAGED,
        taUnit.getDisabled() ? IUIContext.UnitEnable.DISABLED : IUIContext.UnitEnable.ENABLED);

    add(label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 10), 0, 0));
    add(textField, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
  }

  public int getCount() {
    return textField.getValue();
  }

  public void setCount(final int value) {
    textField.setValue(value);
  }

  public void selectAll() {
    textField.setValue(textField.getMax());
  }

  public void selectNone() {
    textField.setValue(0);
  }

  public void setMax(final int value) {
    textField.setMax(value);
  }

  public int getMax() {
    return textField.getMax();
  }

  public void setMin(final int value) {
    textField.setMin(value);
  }

  public Unit getUnit() {
    return unit;
  }

  public void createComponents(final JPanel panel, final int yIndex) {
    panel.add(this, new GridBagConstraints(0, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
    textField.addChangeListener(countTextFieldListener);
  }
}
