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

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

public class UnitChooser extends JPanel {
  private static final long serialVersionUID = -4667032237550267682L;
  private final List<ChooserEntry> entries = new ArrayList<>();
  private final Map<Unit, Collection<Unit>> dependents;
  private JTextArea title;
  private int total = -1;
  private final JLabel leftToSelect = new JLabel();
  private final GameData data;
  private boolean allowMultipleHits = false;
  private JButton autoSelectButton;
  private JButton selectNoneButton;
  private final IUIContext uiContext;
  private final Match<Collection<Unit>> match;

  /** Creates new UnitChooser. */
  public UnitChooser(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent, final GameData data,
      final boolean allowTwoHit, final IUIContext uiContext) {
    this(units, Collections.emptyList(), dependent, data, allowTwoHit, uiContext);
  }

  public UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final GameData data, final boolean allowTwoHit,
      final IUIContext uiContext) {
    this(units, defaultSelections, dependent, false, false, data, allowTwoHit, uiContext);
  }

  private UnitChooser(final Map<Unit, Collection<Unit>> dependent, final GameData data, final boolean allowMultipleHits,
      final IUIContext uiContext, final Match<Collection<Unit>> match) {
    dependents = dependent;
    this.data = data;
    this.allowMultipleHits = allowMultipleHits;
    this.uiContext = uiContext;
    this.match = match;
  }

  UnitChooser(final Collection<Unit> units, final CasualtyList defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final GameData data, final boolean allowMultipleHits,
      final IUIContext uiContext) {
    this(dependent, data, allowMultipleHits, uiContext, null);
    final List<Unit> combinedList = defaultSelections.getDamaged();
    // TODO: this adds it to the default selections list, is this intended?
    combinedList.addAll(defaultSelections.getKilled());
    createEntries(units, dependent, false, false, combinedList);
    layoutEntries();
  }

  UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
      final boolean categorizeTransportCost, final GameData data, final boolean allowMultipleHits,
      final IUIContext uiContext) {
    this(dependent, data, allowMultipleHits, uiContext, null);
    createEntries(units, dependent, categorizeMovement, categorizeTransportCost, defaultSelections);
    layoutEntries();
  }

  UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
      final boolean categorizeTransportCost, final GameData data, final boolean allowMultipleHits,
      final IUIContext uiContext, final Match<Collection<Unit>> match) {
    this(dependent, data, allowMultipleHits, uiContext, match);
    createEntries(units, dependent, categorizeMovement, categorizeTransportCost, defaultSelections);
    layoutEntries();
  }

  /**
   * Set the maximum number of units that we can choose.
   */
  public void setMax(final int max) {
    total = max;
    m_textFieldListener.changedValue(null);
    autoSelectButton.setVisible(false);
    selectNoneButton.setVisible(false);
  }

  void setMaxAndShowMaxButton(final int max) {
    total = max;
    m_textFieldListener.changedValue(null);
    autoSelectButton.setText("Max");
  }

  public void setTitle(final String title) {
    this.title.setText(title);
  }

  private void updateLeft() {
    if (total == -1) {
      return;
    }
    Iterator<ChooserEntry> iter;
    final int selected = getSelectedCount();
    leftToSelect.setText("Left to select:" + (total - selected));
    iter = entries.iterator();
    while (iter.hasNext()) {
      final ChooserEntry entry = iter.next();
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
          if (match.match(newSelectedUnits)) {
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
    final ChooserEntry entry = new ChooserEntry(category, total, m_textFieldListener, data, allowMultipleHits,
        defaultValue, uiContext);
    entries.add(entry);
  }

  private void layoutEntries() {
    this.setLayout(new GridBagLayout());
    title = new JTextArea("Choose units");
    title.setBackground(this.getBackground());
    title.setEditable(false);
    title.setWrapStyleWord(true);
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
    int yIndex = 1;
    for (final ChooserEntry entry : entries) {
      entry.createComponents(this, yIndex);
      yIndex++;
    }
    add(autoSelectButton, new GridBagConstraints(0, yIndex, 7, 1, 0, 0.5, GridBagConstraints.EAST,
        GridBagConstraints.NONE, nullInsets, 0, 0));
    yIndex++;
    add(leftToSelect, new GridBagConstraints(0, yIndex, 5, 2, 0, 0.5, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
    if (match != null) {
      autoSelectButton.setVisible(false);
      selectNoneButton.setVisible(false);
      checkMatches();
    }
  }

  public Collection<Unit> getSelected() {
    return getSelected(true);
  }

  /**
   * get the units selected.
   * If units are two hit enabled, returns those with two hits (ie: those killed).
   */
  public List<Unit> getSelected(final boolean selectDependents) {
    final List<Unit> selectedUnits = new ArrayList<>();
    for (final ChooserEntry entry : entries) {
      addToCollection(selectedUnits, entry, entry.getFinalHit(), selectDependents);
    }
    return selectedUnits;
  }

  /**
   * Only applicable if this dialog was constructed using multiple hit points.
   */
  public List<Unit> getSelectedDamagedMultipleHitPointUnits() {
    final List<Unit> selectedUnits = new ArrayList<>();
    for (ChooserEntry chooserEntry : entries) {
      if (chooserEntry.hasMultipleHitPoints()) {
        // there may be some units being given multiple hits, while others get a single or no hits
        for (int i = 0; i < chooserEntry.size() - 1; i++) {
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
          leftToSelect = 0;
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
    final Iterator<Unit> iter = possible.iterator();
    for (int i = 0; i < quantity; i++) {
      final Unit current = iter.next();
      addTo.add(current);
      if (addDependents) {
        final Collection<Unit> dependents = this.dependents.get(current);
        if (dependents != null) {
          addTo.addAll(dependents);
        }
      }
    }
  }

  private final ScrollableTextFieldListener m_textFieldListener = new ScrollableTextFieldListener() {
    @Override
    public void changedValue(final ScrollableTextField field) {
      if (match != null) {
        checkMatches();
      } else {
        updateLeft();
      }
    }
  };
}


class ChooserEntry {
  private final UnitCategory m_category;
  private final ScrollableTextFieldListener m_hitTextFieldListener;
  private final GameData m_data;
  private final boolean m_hasMultipleHits;
  private final List<Integer> m_defaultHits;
  private final List<ScrollableTextField> m_hitTexts;
  private final List<JLabel> m_hitLabel = new ArrayList<>();
  private int m_leftToSelect = 0;
  private static Insets nullInsets = new Insets(0, 0, 0, 0);
  private final IUIContext m_uiContext;

  ChooserEntry(final UnitCategory category, final int leftToSelect, final ScrollableTextFieldListener listener,
      final GameData data, final boolean allowTwoHit, final int defaultValue, final IUIContext uiContext) {
    m_hitTextFieldListener = listener;
    m_data = data;
    m_category = category;
    m_leftToSelect = leftToSelect < 0 ? category.getUnits().size() : leftToSelect;
    m_hasMultipleHits =
        allowTwoHit && category.getHitPoints() > 1 && category.getDamaged() < category.getHitPoints() - 1;
    m_hitTexts = new ArrayList<>(Math.max(1, category.getHitPoints() - category.getDamaged()));
    m_defaultHits = new ArrayList<>(Math.max(1, category.getHitPoints() - category.getDamaged()));
    final int numUnits = category.getUnits().size();
    int hitsUsedSoFar = 0;
    for (int i = 0; i < Math.max(1, category.getHitPoints() - category.getDamaged()); i++) {
      // TODO: check if default value includes damaged points or not
      final int hitsToUse = Math.min(numUnits, (defaultValue - hitsUsedSoFar));
      hitsUsedSoFar += hitsToUse;
      m_defaultHits.add(hitsToUse);
    }
    m_uiContext = uiContext;
  }

  public void createComponents(final JPanel panel, final int rowIndex) {
    int gridx = 0;
    for (int i =
        0; i < (m_hasMultipleHits ? Math.max(1, m_category.getHitPoints() - m_category.getDamaged()) : 1); i++) {
      final ScrollableTextField scroll = new ScrollableTextField(0, m_category.getUnits().size());
      m_hitTexts.add(scroll);
      scroll.setValue(m_defaultHits.get(i));
      scroll.addChangeListener(m_hitTextFieldListener);
      final JLabel label = new JLabel("x" + m_category.getUnits().size());
      m_hitLabel.add(label);
      panel.add(new UnitChooserEntryIcon(i > 0, m_uiContext), new GridBagConstraints(gridx++, rowIndex, 1, 1, 0, 0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, (i == 0 ? 0 : 8), 0, 0), 0, 0));
      if (i == 0) {
        if (m_category.getMovement() != -1) {
          panel.add(new JLabel("mvt " + m_category.getMovement()), new GridBagConstraints(gridx, rowIndex, 1, 1, 0, 0,
              GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 4), 0, 0));
        }
        if (m_category.getTransportCost() != -1) {
          panel.add(new JLabel("cst " + m_category.getTransportCost()), new GridBagConstraints(gridx, rowIndex, 1, 1, 0,
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

  public int getMax() {
    return m_hitTexts.get(0).getMax();
  }

  public void set(final int value) {
    m_hitTexts.get(0).setValue(value);
  }

  public UnitCategory getCategory() {
    return m_category;
  }

  public void selectAll() {
    m_hitTexts.get(0).setValue(m_hitTexts.get(0).getMax());
  }

  public void selectAllMultipleHitPoints() {
    for (final ScrollableTextField t : m_hitTexts) {
      t.setValue(t.getMax());
    }
  }

  public void selectNone() {
    m_hitTexts.get(0).setValue(0);
  }

  public void setLeftToSelect(final int leftToSelect) {
    m_leftToSelect = leftToSelect < 0 ? m_category.getUnits().size() : leftToSelect;
    updateLeftToSelect();
  }

  private void updateLeftToSelect() {
    int previousMax = m_category.getUnits().size();
    for (int i = 0; i < m_hitTexts.size(); i++) {
      final int newMax = m_leftToSelect + getHits(i);
      final ScrollableTextField text = m_hitTexts.get(i);
      if (i > 0 && !m_hasMultipleHits) {
        text.setMax(0);
      } else {
        text.setMax(Math.min(newMax, previousMax));
      }
      if (text.getValue() < 0 || text.getValue() > text.getMax()) {
        text.setValue(Math.max(0, Math.min(text.getMax(), text.getValue())));
      }
      m_hitLabel.get(i).setText("x" + (i == 0 ? m_category.getUnits().size() : text.getMax()));
      previousMax = text.getValue();
    }
  }

  public int getTotalHits() {
    int hits = 0;
    for (int i = 0; i < m_hitTexts.size(); i++) {
      hits += getHits(i);
    }
    return hits;
  }

  public int getHits(final int zeroBasedHitsPosition) {
    if (zeroBasedHitsPosition < 0 || zeroBasedHitsPosition > m_hitTexts.size() - 1) {
      throw new IllegalArgumentException("Index out of range");
    }
    if (!m_hasMultipleHits && zeroBasedHitsPosition > 0) {
      return 0;
    }
    return m_hitTexts.get(zeroBasedHitsPosition).getValue();
  }

  public int getFinalHit() {
    return getHits(m_hitTexts.size() - 1);
  }

  public int getAllButFinalHit() {
    int hits = 0;
    for (int i = 0; i < m_hitTexts.size() - 1; i++) {
      hits += getHits(i);
    }
    return hits;
  }

  public int size() {
    return m_hitTexts.size();
  }

  public boolean hasMultipleHitPoints() {
    return m_hasMultipleHits;
  }

  private class UnitChooserEntryIcon extends JComponent {
    private static final long serialVersionUID = 591598594559651745L;
    private final boolean forceDamaged;
    private final IUIContext uiContext;

    UnitChooserEntryIcon(final boolean forceDamaged, final IUIContext uiContext) {
      this.forceDamaged = forceDamaged;
      this.uiContext = uiContext;
    }

    @Override
    public void paint(final Graphics g) {
      super.paint(g);
      final Optional<Image> image =
          uiContext.getUnitImageFactory().getImage(m_category.getType(), m_category.getOwner(), m_data,
              forceDamaged || m_category.hasDamageOrBombingUnitDamage(), m_category.getDisabled());
      if (image.isPresent()) {
        g.drawImage(image.get(), 0, 0, this);
      }

      final Iterator<UnitOwner> iter = m_category.getDependents().iterator();
      int index = 1;
      while (iter.hasNext()) {
        final UnitOwner holder = iter.next();
        final int x = uiContext.getUnitImageFactory().getUnitImageWidth() * index;
        final Optional<Image> unitImg =
            uiContext.getUnitImageFactory().getImage(holder.getType(), holder.getOwner(), m_data, false, false);
        if (unitImg.isPresent()) {
          g.drawImage(unitImg.get(), x, 0, this);
        }
        index++;
      }
    }

    @Override
    public int getWidth() {
      // we draw a unit symbol for each dependent
      return uiContext.getUnitImageFactory().getUnitImageWidth() * (1 + m_category.getDependents().size());
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

    public Dimension getDimension() {
      return new Dimension(getWidth(), getHeight());
    }
  }
}
