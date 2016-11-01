package games.strategy.triplea.ui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

public class UnitChooser extends GridPane {
  private final List<ChooserEntry> m_entries = new ArrayList<>();
  private final Map<Unit, Collection<Unit>> m_dependents;
  private TextArea m_title;
  private int m_total = -1;
  private final Label m_leftToSelect = new Label();
  private final GameData m_data;
  private boolean m_allowMultipleHits = false;
  private Button m_autoSelectButton;
  private Button m_selectNoneButton;
  private final IUIContext m_uiContext;
  private final Match<Collection<Unit>> m_match;

  /** Creates new UnitChooser */
  public UnitChooser(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent, final GameData data,
      final boolean allowTwoHit, final IUIContext uiContext) {
    this(units, Collections.emptyList(), dependent, data, allowTwoHit, uiContext);
  }

  public UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final GameData data, final boolean allowTwoHit,
      final IUIContext uiContext) {
    this(units, defaultSelections, dependent, false, false, data, allowTwoHit, uiContext);
  }

  public UnitChooser(final Collection<Unit> units, final CasualtyList defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final GameData data, final boolean allowMultipleHits,
      final IUIContext uiContext) {
    m_dependents = dependent;
    m_data = data;
    m_allowMultipleHits = allowMultipleHits;
    m_uiContext = uiContext;
    m_match = null;
    final List<Unit> combinedList = defaultSelections.getDamaged();
    // TODO: this adds it to the default selections list, is this intended?
    combinedList.addAll(defaultSelections.getKilled());
    createEntries(units, dependent, false, false, combinedList);
    layoutEntries();
  }

  public UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
      final boolean categorizeTransportCost, final GameData data, final boolean allowMultipleHits,
      final IUIContext uiContext) {
    m_dependents = dependent;
    m_data = data;
    m_allowMultipleHits = allowMultipleHits;
    m_uiContext = uiContext;
    m_match = null;
    createEntries(units, dependent, categorizeMovement, categorizeTransportCost, defaultSelections);
    layoutEntries();
  }

  public UnitChooser(final Collection<Unit> units, final Collection<Unit> defaultSelections,
      final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
      final boolean categorizeTransportCost, final GameData data, final boolean allowMultipleHits,
      final IUIContext uiContext, final Match<Collection<Unit>> match) {
    m_dependents = dependent;
    m_data = data;
    m_allowMultipleHits = allowMultipleHits;
    m_uiContext = uiContext;
    m_match = match;
    createEntries(units, dependent, categorizeMovement, categorizeTransportCost, defaultSelections);
    layoutEntries();
  }

  /**
   * Set the maximum number of units that we can choose.
   */
  public void setMax(final int max) {
    m_total = max;
    m_textFieldListener.changedValue(null);
    m_autoSelectButton.setVisible(false);
    m_selectNoneButton.setVisible(false);
  }

  public void setMaxAndShowMaxButton(final int max) {
    m_total = max;
    m_textFieldListener.changedValue(null);
    m_autoSelectButton.setText("Max");
  }

  public void setTitle(final String title) {
    m_title.setText(title);
  }

  private void updateLeft() {
    if (m_total == -1) {
      return;
    }
    Iterator<ChooserEntry> iter;
    final int selected = getSelectedCount();
    m_leftToSelect.setText("Left to select:" + (m_total - selected));
    iter = m_entries.iterator();
    while (iter.hasNext()) {
      final ChooserEntry entry = iter.next();
      entry.setLeftToSelect(m_total - selected);
    }
    m_leftToSelect.setText("Left to select:" + (m_total - selected));
  }

  private void checkMatches() {
    final Collection<Unit> allSelectedUnits = new ArrayList<>();
    for (final ChooserEntry entry : m_entries) {
      addToCollection(allSelectedUnits, entry, entry.getTotalHits(), false);
    }
    // check match against each scroll button
    for (final ChooserEntry entry : m_entries) {
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
          if (m_match.match(newSelectedUnits)) {
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
    for (final ChooserEntry entry : m_entries) {
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

  private IntegerMap<UnitCategory> createDefaultSelectionsMap(final Collection<UnitCategory> categories) {
    final IntegerMap<UnitCategory> defaultValues = new IntegerMap<>();
    for (final UnitCategory category : categories) {
      final int defaultValue = category.getUnits().size();
      defaultValues.put(category, defaultValue);
    }
    return defaultValues;
  }

  private void addCategory(final UnitCategory category, final int defaultValue) {
    final ChooserEntry entry = new ChooserEntry(category, m_total, m_textFieldListener, m_data, m_allowMultipleHits,
        defaultValue, m_uiContext);
    m_entries.add(entry);
  }

  private void layoutEntries() {
    m_title = new TextArea("Choose units");
    m_title.setBackground(this.getBackground());
    m_title.setEditable(false);
    m_title.setWrapText(true);
    int buttonWidth = 80;
    int buttonHeight = 20;
    m_selectNoneButton = new Button("None");
    m_selectNoneButton.setPrefSize(buttonWidth, buttonHeight);
    m_autoSelectButton = new Button("All");
    m_autoSelectButton.setPrefSize(buttonWidth, buttonHeight);
    add(m_title, 0, 0, 7, 1);
    ColumnConstraints cc1 = new ColumnConstraints();
    cc1.setPercentWidth(0);
    RowConstraints rc1 = new RowConstraints();
    rc1.setPercentHeight(0.5);
    getRowConstraints().add(rc1);
    getColumnConstraints().add(cc1);
    setValignment(m_title, VPos.CENTER);
    setHalignment(m_title, HPos.RIGHT);
    setFillHeight(m_title, false);
    m_selectNoneButton.setOnAction(e -> selectNone());
    m_autoSelectButton.setOnAction(e -> autoSelect());
    int yIndex = 1;
    for (final ChooserEntry entry : m_entries) {
      entry.createComponents(this, yIndex);
      yIndex++;
    }
    add(m_autoSelectButton, 0, yIndex, 7, 1);
    setValignment(m_autoSelectButton, VPos.CENTER);
    setHalignment(m_autoSelectButton, HPos.RIGHT);
    setFillHeight(m_autoSelectButton, false);
    setFillWidth(m_autoSelectButton, false);
    yIndex++;
    add(m_leftToSelect, 0, yIndex, 5, 2);
    setValignment(m_leftToSelect, VPos.CENTER);
    setHalignment(m_leftToSelect, HPos.LEFT);
    setFillHeight(m_leftToSelect, false);
    if (m_match != null) {
      m_autoSelectButton.setVisible(false);
      m_selectNoneButton.setVisible(false);
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
    for (final ChooserEntry entry : m_entries) {
      addToCollection(selectedUnits, entry, entry.getFinalHit(), selectDependents);
    }
    return selectedUnits;
  }

  /**
   * Only applicable if this dialog was constructed using multiple hit points
   */
  public List<Unit> getSelectedDamagedMultipleHitPointUnits() {
    final List<Unit> selectedUnits = new ArrayList<>();
    final Iterator<ChooserEntry> entries = m_entries.iterator();
    while (entries.hasNext()) {
      final ChooserEntry chooserEntry = entries.next();
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
    for (final ChooserEntry entry : m_entries) {
      entry.selectNone();
    }
  }

  // does not take into account multiple hit points
  private void autoSelect() {
    if (m_total == -1) {
      for (final ChooserEntry entry : m_entries) {
        entry.selectAll();
      }
    } else {
      int leftToSelect = m_total - getSelectedCount();
      for (final ChooserEntry entry : m_entries) {
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
        final Collection<Unit> dependents = m_dependents.get(current);
        if (dependents != null) {
          addTo.addAll(dependents);
        }
      }
    }
  }

  private final ScrollableTextFieldListener m_textFieldListener = new ScrollableTextFieldListener() {
    @Override
    public void changedValue(final ScrollableTextField field) {
      if (m_match != null) {
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
  private final List<Label> m_hitLabel = new ArrayList<>();
  private int m_leftToSelect = 0;
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

  public void createComponents(final GridPane panel, final int yIndex) {
    int gridx = 0;
    for (int i =
        0; i < (m_hasMultipleHits ? Math.max(1, m_category.getHitPoints() - m_category.getDamaged()) : 1); i++) {
      final ScrollableTextField scroll = new ScrollableTextField(0, m_category.getUnits().size());
      m_hitTexts.add(scroll);
      scroll.setValue(m_defaultHits.get(i));
      scroll.addChangeListener(m_hitTextFieldListener);
      final Label label = new Label("x" + m_category.getUnits().size());
      m_hitLabel.add(label);
      UnitChooserEntryIcon icon = new UnitChooserEntryIcon(i > 0, m_uiContext);
      panel.add(icon, gridx++, yIndex, 1, 1);
      ColumnConstraints cc1 = new ColumnConstraints();
      cc1.setPercentWidth(0);
      RowConstraints rc1 = new RowConstraints();
      rc1.setPercentHeight(0);
      panel.getRowConstraints().add(rc1);
      panel.getColumnConstraints().add(cc1);
      GridPane.setValignment(icon, VPos.CENTER);
      GridPane.setHalignment(icon, HPos.LEFT);
      GridPane.setFillHeight(icon, false);
      GridPane.setMargin(icon, new Insets(0, (i == 0 ? 0 : 8), 0, 0));
      if (i == 0) {
        if (m_category.getMovement() != -1) {
          Label movementLabel = new Label("mvt " + m_category.getMovement());
          panel.add(movementLabel, gridx, yIndex);
          GridPane.setValignment(movementLabel, VPos.CENTER);
          GridPane.setHalignment(movementLabel, HPos.LEFT);
          GridPane.setFillHeight(movementLabel, false);
          GridPane.setMargin(icon, new Insets(0, 4, 0, 4));
        }
        if (m_category.getTransportCost() != -1) {
          Label costLabel = new Label("cst " + m_category.getTransportCost());
          panel.add(costLabel, gridx, yIndex);
          GridPane.setValignment(costLabel, VPos.CENTER);
          GridPane.setHalignment(costLabel, HPos.LEFT);
          GridPane.setFillHeight(costLabel, false);
          GridPane.setMargin(icon, new Insets(0, 4, 0, 4));
        }
        gridx++;
      }
      panel.add(label, gridx++, yIndex);
      GridPane.setValignment(label, VPos.CENTER);
      GridPane.setHalignment(label, HPos.LEFT);
      GridPane.setFillHeight(label, false);
      panel.add(scroll, gridx++, yIndex);
      GridPane.setValignment(scroll, VPos.CENTER);
      GridPane.setHalignment(scroll, HPos.LEFT);
      GridPane.setFillHeight(scroll, false);
      GridPane.setMargin(scroll, new Insets(0, 4, 0, 0));
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

  private class UnitChooserEntryIcon extends ImageView {
    private final boolean m_forceDamaged;
    private final IUIContext uiContext;

    UnitChooserEntryIcon(final boolean forceDamaged, final IUIContext uiContext) {
      this.setImage(getCustomImage());
      m_forceDamaged = forceDamaged;
      this.uiContext = uiContext;
    }

    public javafx.scene.image.Image getCustomImage() {
      Canvas canvas = new Canvas();
      GraphicsContext g = canvas.getGraphicsContext2D();
      final Optional<Image> image =
          uiContext.getUnitImageFactory().getImage(m_category.getType(), m_category.getOwner(), m_data,
              m_forceDamaged || m_category.hasDamageOrBombingUnitDamage(), m_category.getDisabled());
      if (image.isPresent()) {
        g.drawImage(SwingFXUtils.toFXImage((BufferedImage) image.get(), null), 0, 0);
      }

      final Iterator<UnitOwner> iter = m_category.getDependents().iterator();
      int index = 1;
      while (iter.hasNext()) {
        final UnitOwner holder = iter.next();
        final int x = uiContext.getUnitImageFactory().getUnitImageWidth() * index;
        final Optional<Image> unitImg =
            uiContext.getUnitImageFactory().getImage(holder.getType(), holder.getOwner(), m_data, false, false);
        if (unitImg.isPresent()) {
          g.drawImage(SwingFXUtils.toFXImage((BufferedImage) unitImg.get(), null), x, 0);
        }
        index++;
      }
      WritableImage img = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
      canvas.snapshot(null, img);
      return img;
    }
  }
}
