package games.strategy.triplea.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.IntegerMap;

final class UnitChooser extends JPanel {
  private static final long serialVersionUID = -4667032237550267682L;
  private final List<ChooserEntry> entries = new ArrayList<>();
  private final Map<Unit, Collection<Unit>> dependents;
  private JTextArea title;
  private int total = -1;
  private final JLabel leftToSelect = new JLabel();
  private boolean allowMultipleHits = false;
  private JButton autoSelectButton;
  private JButton selectNoneButton;
  private final UiContext uiContext;
  private final Predicate<Collection<Unit>> match;

  UnitChooser(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent,
      final boolean allowTwoHit, final UiContext uiContext) {
    this(units, Collections.emptyList(), dependent, allowTwoHit, uiContext);
  }

  private UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final boolean allowTwoHit, final UiContext uiContext) {
    this(units, defaultSelections, dependent, false, false, allowTwoHit, uiContext);
  }

  private UnitChooser(final Map<Unit, Collection<Unit>> dependent, final boolean allowMultipleHits,
      final UiContext uiContext, final Predicate<Collection<Unit>> match) {
    dependents = dependent;
    this.allowMultipleHits = allowMultipleHits;
    this.uiContext = uiContext;
    this.match = match;
  }

  UnitChooser(final Collection<Unit> units, final CasualtyList defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final boolean allowMultipleHits, final UiContext uiContext) {
    this(dependent, allowMultipleHits, uiContext, null);
    final List<Unit> combinedList = defaultSelections.getDamaged();
    // TODO: this adds it to the default selections list, is this intended?
    combinedList.addAll(defaultSelections.getKilled());
    createEntries(units, dependent, false, false, combinedList);
    layoutEntries();
  }

  UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
      final boolean categorizeTransportCost, final boolean allowMultipleHits, final UiContext uiContext) {
    this(dependent, allowMultipleHits, uiContext, null);
    createEntries(units, dependent, categorizeMovement, categorizeTransportCost, defaultSelections);
    layoutEntries();
  }

  UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
      final boolean categorizeTransportCost, final boolean allowMultipleHits,
      final UiContext uiContext, final Predicate<Collection<Unit>> match) {
    this(dependent, allowMultipleHits, uiContext, match);
    createEntries(units, dependent, categorizeMovement, categorizeTransportCost, defaultSelections);
    layoutEntries();
  }

  /**
   * Set the maximum number of units that we can choose.
   */
  void setMax(final int max) {
    total = max;
    textFieldListener.changedValue(null);
    autoSelectButton.setVisible(false);
    selectNoneButton.setVisible(false);
  }

  void setMaxAndShowMaxButton(final int max) {
    total = max;
    textFieldListener.changedValue(null);
    autoSelectButton.setText("Max");
  }

  void setTitle(final String title) {
    this.title.setText(title);
    this.title.setVisible(true);
  }

  private void updateLeft() {
    if (total == -1) {
      return;
    }
    final int selected = getSelectedCount();
    leftToSelect.setText("Left to select:" + (total - selected));
    for (final ChooserEntry entry : entries) {
      entry.setLeftToSelect(total - selected);
    }
    leftToSelect.setText("Left to select:" + (total - selected));
  }

  private void checkMatches() {
    final Collection<Unit> allSelectedUnits = new ArrayList<>();
    for (final ChooserEntry entry : entries) {
      addToCollection(allSelectedUnits, entry, entry.getTotalHits(), false);
    }
    // check match against each scroll button
    for (final ChooserEntry entry : entries) {
      final Collection<Unit> newSelectedUnits = new ArrayList<>(allSelectedUnits);
      final int totalHits = entry.getTotalHits();
      final int totalUnits = entry.getCategory().getUnits().size();
      int leftToSelect = 0;
      final Iterator<Unit> unitIter = entry.getCategory().getUnits().iterator();
      for (int i = 1; i <= totalUnits; i++) {
        final Unit unit = unitIter.next();
        if (i > totalHits) {
          newSelectedUnits.add(unit);
        }
        if (i >= totalHits) {
          if (match.test(newSelectedUnits)) {
            leftToSelect = i - totalHits;
          } else {
            break;
          }
        }
      }
      entry.setLeftToSelect(leftToSelect);
    }
  }

  private int getSelectedCount() {
    int selected = 0;
    for (final ChooserEntry entry : entries) {
      selected += entry.getTotalHits();
    }
    return selected;
  }

  private void createEntries(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent,
      final boolean categorizeMovement, final boolean categorizeTransportCost,
      final Collection<Unit> defaultSelections) {
    final Collection<UnitCategory> categories =
        UnitSeperator.categorize(units, dependent, categorizeMovement, categorizeTransportCost);
    final Collection<UnitCategory> defaultSelectionsCategorized =
        UnitSeperator.categorize(defaultSelections, dependent, categorizeMovement, categorizeTransportCost);
    final IntegerMap<UnitCategory> defaultValues = createDefaultSelectionsMap(defaultSelectionsCategorized);
    for (final UnitCategory category : categories) {
      addCategory(category, defaultValues.getInt(category));
    }
  }

  private static IntegerMap<UnitCategory> createDefaultSelectionsMap(final Collection<UnitCategory> categories) {
    final IntegerMap<UnitCategory> defaultValues = new IntegerMap<>();
    for (final UnitCategory category : categories) {
      final int defaultValue = category.getUnits().size();
      defaultValues.put(category, defaultValue);
    }
    return defaultValues;
  }

  private void addCategory(final UnitCategory category, final int defaultValue) {
    final ChooserEntry entry = new ChooserEntry(category, total, textFieldListener, allowMultipleHits,
        defaultValue, uiContext);
    entries.add(entry);
  }

  private void layoutEntries() {
    this.setLayout(new GridBagLayout());
    title = new JTextArea();
    title.setBackground(this.getBackground());
    title.setEditable(false);
    title.setWrapStyleWord(true);
    title.setVisible(false);
    final Insets nullInsets = new Insets(0, 0, 0, 0);
    final Dimension buttonSize = new Dimension(80, 20);
    selectNoneButton = new JButton("None");
    selectNoneButton.setPreferredSize(buttonSize);
    autoSelectButton = new JButton("All");
    autoSelectButton.setPreferredSize(buttonSize);
    add(title, new GridBagConstraints(0, 0, 7, 1, 0, 0.5, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        nullInsets, 0, 0));
    selectNoneButton.addActionListener(e -> selectNone());
    autoSelectButton.addActionListener(e -> autoSelect());
    int rowIndex = 1;
    for (final ChooserEntry entry : entries) {
      entry.createComponents(this, rowIndex);
      rowIndex++;
    }
    add(autoSelectButton, new GridBagConstraints(0, rowIndex, 7, 1, 0, 0.5, GridBagConstraints.EAST,
        GridBagConstraints.NONE, nullInsets, 0, 0));
    rowIndex++;
    add(leftToSelect, new GridBagConstraints(0, rowIndex, 5, 2, 0, 0.5, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
    if (match != null) {
      autoSelectButton.setVisible(false);
      selectNoneButton.setVisible(false);
      checkMatches();
    }
  }

  Collection<Unit> getSelected() {
    return getSelected(true);
  }

  /**
   * get the units selected.
   * If units are two hit enabled, returns those with two hits (ie: those killed).
   */
  List<Unit> getSelected(final boolean selectDependents) {
    final List<Unit> selectedUnits = new ArrayList<>();
    for (final ChooserEntry entry : entries) {
      addToCollection(selectedUnits, entry, entry.getFinalHit(), selectDependents);
    }
    return selectedUnits;
  }

  /**
   * Only applicable if this dialog was constructed using multiple hit points.
   */
  List<Unit> getSelectedDamagedMultipleHitPointUnits() {
    final List<Unit> selectedUnits = new ArrayList<>();
    for (final ChooserEntry chooserEntry : entries) {
      if (chooserEntry.hasMultipleHitPoints()) {
        // there may be some units being given multiple hits, while others get a single or no hits
        for (int i = 0; i < (chooserEntry.size() - 1); i++) {
          // here we are counting on the fact that unit category stores the units in a list, so the order is the same
          // every time we access
          // it.
          // this means that in the loop we may select the first 2 units in the list to receive 1 hit, then select the
          // first unit the list
          // to receive 1 more hit
          addToCollection(selectedUnits, chooserEntry, chooserEntry.getHits(i), false);
        }
      }
    }
    return selectedUnits;
  }

  private void selectNone() {
    for (final ChooserEntry entry : entries) {
      entry.selectNone();
    }
  }

  // does not take into account multiple hit points
  private void autoSelect() {
    if (total == -1) {
      for (final ChooserEntry entry : entries) {
        entry.selectAll();
      }
    } else {
      int leftToSelect = total - getSelectedCount();
      for (final ChooserEntry entry : entries) {
        final int canSelect = entry.getMax() - entry.getHits(0);
        if (leftToSelect >= canSelect) {
          entry.selectAll();
          leftToSelect -= canSelect;
        } else {
          entry.set(entry.getHits(0) + canSelect);
          break;
        }
      }
    }
  }

  private void addToCollection(final Collection<Unit> addTo, final ChooserEntry entry, final int quantity,
      final boolean addDependents) {
    final Collection<Unit> possible = entry.getCategory().getUnits();
    if (possible.size() < quantity) {
      throw new IllegalStateException("Not enough units");
    }
    possible.stream().limit(quantity).forEach(current -> {
      addTo.add(current);
      if (addDependents) {
        final Collection<Unit> dependents = this.dependents.get(current);
        if (dependents != null) {
          addTo.addAll(dependents);
        }
      }
    });
  }

  private final ScrollableTextFieldListener textFieldListener = new ScrollableTextFieldListener() {
    @Override
    public void changedValue(final ScrollableTextField field) {
      if (match != null) {
        checkMatches();
      } else {
        updateLeft();
      }
    }
  };

  private static final class ChooserEntry {
    private final UnitCategory category;
    private final ScrollableTextFieldListener hitTextFieldListener;
    private final boolean hasMultipleHits;
    private final List<Integer> defaultHits;
    private final List<ScrollableTextField> hitTexts;
    private final List<JLabel> hitLabel = new ArrayList<>();
    private int leftToSelect = 0;
    private static final Insets nullInsets = new Insets(0, 0, 0, 0);
    private final UiContext uiContext;

    ChooserEntry(final UnitCategory category, final int leftToSelect, final ScrollableTextFieldListener listener,
        final boolean allowTwoHit, final int defaultValue, final UiContext uiContext) {
      hitTextFieldListener = listener;
      this.category = category;
      this.leftToSelect = (leftToSelect < 0) ? category.getUnits().size() : leftToSelect;
      hasMultipleHits =
          allowTwoHit && (category.getHitPoints() > 1) && (category.getDamaged() < (category.getHitPoints() - 1));
      hitTexts = new ArrayList<>(Math.max(1, category.getHitPoints() - category.getDamaged()));
      defaultHits = new ArrayList<>(Math.max(1, category.getHitPoints() - category.getDamaged()));
      final int numUnits = category.getUnits().size();
      int hitsUsedSoFar = 0;
      for (int i = 0; i < Math.max(1, category.getHitPoints() - category.getDamaged()); i++) {
        // TODO: check if default value includes damaged points or not
        final int hitsToUse = Math.min(numUnits, (defaultValue - hitsUsedSoFar));
        hitsUsedSoFar += hitsToUse;
        defaultHits.add(hitsToUse);
      }
      this.uiContext = uiContext;
    }

    void createComponents(final JPanel panel, final int rowIndex) {
      int gridx = 0;
      for (int i =
          0; i < (hasMultipleHits ? Math.max(1, category.getHitPoints() - category.getDamaged()) : 1); i++) {
        final ScrollableTextField scroll = new ScrollableTextField(0, category.getUnits().size());
        hitTexts.add(scroll);
        scroll.setValue(defaultHits.get(i));
        scroll.addChangeListener(hitTextFieldListener);
        final JLabel label = new JLabel("x" + category.getUnits().size());
        hitLabel.add(label);
        panel.add(new UnitChooserEntryIcon(i > 0, uiContext), new GridBagConstraints(gridx++, rowIndex, 1, 1, 0, 0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, ((i == 0) ? 0 : 8), 0, 0), 0, 0));
        if (i == 0) {
          if (category.getMovement() != -1) {
            panel.add(new JLabel("mvt " + category.getMovement()), new GridBagConstraints(gridx, rowIndex, 1, 1, 0, 0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 4), 0, 0));
          }
          if (category.getTransportCost() != -1) {
            panel.add(new JLabel("cst " + category.getTransportCost()),
                new GridBagConstraints(gridx, rowIndex, 1, 1, 0,
                    0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 4), 0, 0));
          }
          gridx++;
        }
        panel.add(label, new GridBagConstraints(gridx++, rowIndex, 1, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
        panel.add(scroll, new GridBagConstraints(gridx++, rowIndex, 1, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0));
        scroll.addChangeListener(field -> updateLeftToSelect());
      }
      updateLeftToSelect();
    }

    int getMax() {
      return hitTexts.get(0).getMax();
    }

    void set(final int value) {
      hitTexts.get(0).setValue(value);
    }

    UnitCategory getCategory() {
      return category;
    }

    void selectAll() {
      hitTexts.get(0).setValue(hitTexts.get(0).getMax());
    }

    void selectNone() {
      hitTexts.get(0).setValue(0);
    }

    void setLeftToSelect(final int leftToSelect) {
      this.leftToSelect = (leftToSelect < 0) ? category.getUnits().size() : leftToSelect;
      updateLeftToSelect();
    }

    private void updateLeftToSelect() {
      int previousMax = category.getUnits().size();
      for (int i = 0; i < hitTexts.size(); i++) {
        final int newMax = leftToSelect + getHits(i);
        final ScrollableTextField text = hitTexts.get(i);
        if ((i > 0) && !hasMultipleHits) {
          text.setMax(0);
        } else {
          text.setMax(Math.min(newMax, previousMax));
        }
        if ((text.getValue() < 0) || (text.getValue() > text.getMax())) {
          text.setValue(Math.max(0, Math.min(text.getMax(), text.getValue())));
        }
        hitLabel.get(i).setText("x" + (i == 0 ? category.getUnits().size() : text.getMax()));
        previousMax = text.getValue();
      }
    }

    int getTotalHits() {
      int hits = 0;
      for (int i = 0; i < hitTexts.size(); i++) {
        hits += getHits(i);
      }
      return hits;
    }

    int getHits(final int zeroBasedHitsPosition) {
      if ((zeroBasedHitsPosition < 0) || (zeroBasedHitsPosition > (hitTexts.size() - 1))) {
        throw new IllegalArgumentException("Index out of range");
      }
      if (!hasMultipleHits && (zeroBasedHitsPosition > 0)) {
        return 0;
      }
      return hitTexts.get(zeroBasedHitsPosition).getValue();
    }

    int getFinalHit() {
      return getHits(hitTexts.size() - 1);
    }

    int size() {
      return hitTexts.size();
    }

    boolean hasMultipleHitPoints() {
      return hasMultipleHits;
    }

    private class UnitChooserEntryIcon extends JComponent {
      private static final long serialVersionUID = 591598594559651745L;
      private final boolean forceDamaged;
      private final UiContext uiContext;

      UnitChooserEntryIcon(final boolean forceDamaged, final UiContext uiContext) {
        this.forceDamaged = forceDamaged;
        this.uiContext = uiContext;
      }

      @Override
      public void paint(final Graphics g) {
        super.paint(g);
        final Optional<Image> image =
            uiContext.getUnitImageFactory().getImage(category.getType(), category.getOwner(),
                forceDamaged || category.hasDamageOrBombingUnitDamage(), category.getDisabled());
        if (image.isPresent()) {
          g.drawImage(image.get(), 0, 0, this);
        }

        int index = 1;
        for (final UnitOwner holder : category.getDependents()) {
          final int x = uiContext.getUnitImageFactory().getUnitImageWidth() * index;
          final Optional<Image> unitImg =
              uiContext.getUnitImageFactory().getImage(holder.getType(), holder.getOwner(), false, false);
          if (unitImg.isPresent()) {
            g.drawImage(unitImg.get(), x, 0, this);
          }
          index++;
        }
      }

      @Override
      public int getWidth() {
        // we draw a unit symbol for each dependent
        return uiContext.getUnitImageFactory().getUnitImageWidth() * (1 + category.getDependents().size());
      }

      @Override
      public int getHeight() {
        return uiContext.getUnitImageFactory().getUnitImageHeight();
      }

      @Override
      public Dimension getMaximumSize() {
        return getDimension();
      }

      @Override
      public Dimension getMinimumSize() {
        return getDimension();
      }

      @Override
      public Dimension getPreferredSize() {
        return getDimension();
      }

      Dimension getDimension() {
        return new Dimension(getWidth(), getHeight());
      }
    }
  }
}
