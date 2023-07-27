package games.strategy.triplea.ui;

import games.strategy.engine.data.Unit;
import games.strategy.ui.ScrollableTextFieldListener;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * For when you want multiple individual unit panels, perhaps one for each territory, etc.
 *
 * <p>This lets you create multiple IndividualUnitPanel into a single panel, and have them
 * integrated to use the same MAX. IndividualUnitPanel is a group of units each displayed
 * individually, and you can set an integer up to max for each unit.
 */
public class IndividualUnitPanelGrouped extends JPanel {
  private static final long serialVersionUID = 3573683064535306664L;
  private int max = 0;
  private final boolean showMinAndMax;
  private final JTextArea title;
  private final UiContext uiContext;
  private final Map<String, Collection<Unit>> unitsToChooseFrom;
  private final Collection<Tuple<String, IndividualUnitPanel>> entries = new ArrayList<>();
  private final JLabel leftToSelect = new JLabel();
  private final boolean showSelectAll;
  private final ScrollableTextFieldListener textFieldListener = field -> updateLeft();

  /**
   * For when you want multiple individual unit panels, perhaps one for each territory, etc. This
   * lets you create multiple IndividualUnitPanel into a single panel, and have them integrated to
   * use the same MAX. IndividualUnitPanel is a group of units each displayed individually, and you
   * can set an integer up to max for each unit.
   */
  public IndividualUnitPanelGrouped(
      final Map<String, Collection<Unit>> unitsToChooseFrom,
      final UiContext uiContext,
      final String title,
      final int maxTotal,
      final boolean showMinAndMax,
      final boolean showSelectAll) {
    this.uiContext = uiContext;
    setMaxAndShowMaxButton(maxTotal);
    this.showMinAndMax = showMinAndMax;
    this.title = new JTextArea(title);
    this.title.setBackground(this.getBackground());
    this.title.setEditable(false);
    this.title.setWrapStyleWord(true);
    this.unitsToChooseFrom = unitsToChooseFrom;
    this.showSelectAll = showSelectAll;
    layoutEntries();
  }

  private void setMaxAndShowMaxButton(final int max) {
    this.max = max;
    textFieldListener.changedValue(null);
  }

  private void updateLeft() {
    if (max == -1) {
      return;
    }
    final int selected = getSelectedCount();
    final int newMax = max - selected;
    for (final Tuple<String, IndividualUnitPanel> entry : entries) {
      final int current = entry.getSecond().getSelectedCount();
      final int maxForThis = current + newMax;
      if (entry.getSecond().getMax() != maxForThis) {
        entry.getSecond().setMaxAndUpdate(maxForThis);
      }
    }
    leftToSelect.setText("Left to select: " + newMax);
  }

  protected int getSelectedCount() {
    int selected = 0;
    for (final Tuple<String, IndividualUnitPanel> entry : entries) {
      selected += entry.getSecond().getSelectedCount();
    }
    return selected;
  }

  private void layoutEntries() {
    this.setLayout(new GridBagLayout());
    final Insets nullInsets = new Insets(0, 0, 0, 0);
    final Dimension buttonSize = new Dimension(80, 20);
    final JButton selectNoneButton = new JButton("None");
    selectNoneButton.setPreferredSize(buttonSize);
    final JButton autoSelectButton = new JButton("Max");
    autoSelectButton.setPreferredSize(buttonSize);
    add(
        title,
        new GridBagConstraints(
            0,
            0,
            7,
            1,
            0,
            0.5,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            nullInsets,
            0,
            0));
    selectNoneButton.addActionListener(e -> selectNone());
    autoSelectButton.addActionListener(e -> autoSelect());
    final JPanel entries = new JPanel();
    entries.setLayout(new FlowLayout());
    entries.setBorder(BorderFactory.createEmptyBorder());
    for (final Map.Entry<String, Collection<Unit>> entry : unitsToChooseFrom.entrySet()) {
      final String miniTitle = entry.getKey();
      final Collection<Unit> possibleTargets = entry.getValue();
      final JPanel panelChooser = new JPanel();
      panelChooser.setLayout(new BoxLayout(panelChooser, BoxLayout.Y_AXIS));
      panelChooser.setBorder(BorderFactory.createLineBorder(getBackground()));
      final JLabel chooserTitle = new JLabel("Choose Per Unit");
      chooserTitle.setHorizontalAlignment(JLabel.LEFT);
      chooserTitle.setFont(new Font("Arial", Font.BOLD, 12));
      panelChooser.add(chooserTitle);
      panelChooser.add(new JLabel(" "));
      final IndividualUnitPanel chooser =
          new IndividualUnitPanel(
              possibleTargets,
              miniTitle,
              uiContext,
              max,
              showMinAndMax,
              showSelectAll,
              textFieldListener);
      this.entries.add(Tuple.of(miniTitle, chooser));
      panelChooser.add(chooser);
      final JScrollPane chooserScrollPane = new JScrollPane(panelChooser);
      chooserScrollPane.setMaximumSize(new Dimension(220, 520));
      chooserScrollPane.setPreferredSize(
          new Dimension(
              (chooserScrollPane.getPreferredSize().width > 220
                  ? 220
                  : (chooserScrollPane.getPreferredSize().height > 520
                      ? chooserScrollPane.getPreferredSize().width + 20
                      : chooserScrollPane.getPreferredSize().width)),
              (chooserScrollPane.getPreferredSize().height > 520
                  ? 520
                  : (chooserScrollPane.getPreferredSize().width > 220
                      ? chooserScrollPane.getPreferredSize().height + 20
                      : chooserScrollPane.getPreferredSize().height))));
      entries.add(chooserScrollPane);
    }
    add(
        entries,
        new GridBagConstraints(
            0,
            1,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            nullInsets,
            0,
            0));
    if (showSelectAll) {
      add(
          autoSelectButton,
          new GridBagConstraints(
              0,
              2,
              7,
              1,
              0,
              0.5,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              nullInsets,
              0,
              0));
    }
    add(
        leftToSelect,
        new GridBagConstraints(
            0,
            3,
            5,
            2,
            0,
            0.5,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            nullInsets,
            0,
            0));
  }

  Map<String, IntegerMap<Unit>> getSelected() {
    final Map<String, IntegerMap<Unit>> selectedUnits = new HashMap<>();
    for (final Tuple<String, IndividualUnitPanel> entry : entries) {
      selectedUnits.put(entry.getFirst(), entry.getSecond().getSelected());
    }
    return selectedUnits;
  }

  protected void selectNone() {
    for (final Tuple<String, IndividualUnitPanel> entry : entries) {
      entry.getSecond().selectNone();
    }
  }

  protected void autoSelect() {
    for (final Tuple<String, IndividualUnitPanel> entry : entries) {
      entry.getSecond().autoSelect();
    }
  }
}
