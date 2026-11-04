package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.MapPreviews.Caches;
import games.strategy.engine.framework.map.download.MapRow.Actions;
import games.strategy.engine.framework.map.download.MapRow.Listener;
import games.strategy.engine.framework.map.download.MapTagFilter.Tag;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import lombok.Getter;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.ScrollableJPanel;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.WrapLayout;

/**
 * The maps of a tab of the maps window by name, in the chosen view (compact rows, rows with
 * pictures or tiles), with the search, the tag and view buttons above them and the details of the
 * selected map on the right. The first shown map is selected unless the player clicked another;
 * ctrl-click and shift-click select several maps, which the bar above the maps installs or updates
 * together. The search shows only the maps whose name or tags, eg the category, contain the
 * searched text, and that have the checked tags.
 */
final class MapList {
  // the share of the width the maps get; the details get the rest
  private static final double LIST_SHARE = 0.58;
  private static final String SELECTION_HINT = "Ctrl- or Shift-click to select several maps";
  private static final Comparator<ManagedMap> BY_NAME =
      Comparator.comparing(map -> map.getMapName().toLowerCase(Locale.ROOT));

  private final Supplier<List<ManagedMap>> maps;
  private final Caches previews;
  private final Actions actions;
  private final JPanel rows = new ScrollableJPanel();
  private final JLabel emptyText = new JLabel();
  private final MapDetails details;
  private final JLabel selectionText = new JLabel();
  private final JButton installSelectedButton =
      new JButtonBuilder().title("Install selected").actionListener(this::installSelected).build();
  private final JButton clearButton =
      new JButtonBuilder().title("Clear").actionListener(this::clearSelection).build();
  @Getter private final JComponent swingComponent;
  private final Map<String, MapRow> rowsByMapName = new HashMap<>();
  private final MapSelection selection = new MapSelection();
  // the bytes received of the running downloads, so rebuilt rows show them at once
  private final Map<String, Long> bytesByMapName = new HashMap<>();
  private MapBrowserView view;
  private String searchedText = "";
  private Set<Tag> checkedTags = Set.of();
  // the row whose map the details show; rows are rebuilt on refresh, then the details are too
  private @Nullable MapRow detailsRow;

  /**
   * @param maps The maps of the list; asked again on {@link #refresh()}.
   * @param toolbar The search and the tag and view buttons, shown above the maps.
   */
  MapList(
      final Supplier<List<ManagedMap>> maps,
      final Caches previews,
      final Actions actions,
      final JComponent toolbar,
      final MapBrowserView view) {
    this.maps = maps;
    this.previews = previews;
    this.actions = actions;
    this.view = view;
    details = new MapDetails(previews.details(), actions);
    emptyText.setAlignmentX(Component.LEFT_ALIGNMENT);
    emptyText.setBorder(BorderFactory.createEmptyBorder(24, 12, 0, 0));
    emptyText.setForeground(LookAndFeel.getSecondaryTextColor());
    final JScrollPane scrollPane = SwingComponents.newJScrollPane(rows);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    final JPanel top = new JPanel();
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
    top.add(toolbar);
    final JPanel selectionBar = newSelectionBar();
    selectionBar.setAlignmentX(Component.LEFT_ALIGNMENT);
    top.add(selectionBar);
    final JPanel listColumn = new JPanel(new BorderLayout());
    listColumn.setMinimumSize(new Dimension(320, 0));
    listColumn.add(top, BorderLayout.NORTH);
    listColumn.add(scrollPane, BorderLayout.CENTER);
    swingComponent = newSplit(listColumn, details);
    refresh();
  }

  /** The bar above the maps: a hint, or the number of selected maps, Install selected and Clear. */
  private JPanel newSelectionBar() {
    LookAndFeel.setPrimaryButtonStyle(installSelectedButton);
    LookAndFeel.setBorderlessButtonStyle(clearButton);
    clearButton.setForeground(LookAndFeel.getAccentColor());
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    bar.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LookAndFeel.getSeparatorColor()),
            BorderFactory.createEmptyBorder(6, 20, 6, 20)));
    bar.add(selectionText);
    bar.add(installSelectedButton);
    bar.add(clearButton);
    return bar;
  }

  /** Installs or updates the selected maps that can be, the same way as their buttons do. */
  @VisibleForTesting
  void installSelected() {
    final List<ManagedMap> installable = getInstallableSelectedMaps();
    selection.clear();
    actions.install().accept(installable);
  }

  private List<ManagedMap> getInstallableSelectedMaps() {
    return getSelectedMaps().stream().filter(MapRow::isInstallable).toList();
  }

  private List<ManagedMap> getSelectedMaps() {
    return selection.getSelected().stream()
        .map(rowsByMapName::get)
        // a refresh may have moved a selected map to the other tab
        .filter(Objects::nonNull)
        .map(MapRow::getMap)
        .sorted(BY_NAME)
        .toList();
  }

  /** Selects only the map the details show. */
  private void clearSelection() {
    selection.clear();
    showSelection();
  }

  /**
   * The maps on the left take their share of the width, the details the rest, also when the window
   * is resized; the player can move the divider.
   */
  private static JSplitPane newSplit(final JComponent listColumn, final JComponent details) {
    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listColumn, details);
    split.setResizeWeight(LIST_SHARE);
    split.setContinuousLayout(true);
    split.setBorder(BorderFactory.createEmptyBorder());
    split.addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            // the first layout splits by the preferred sizes; the share applies once it has a size
            split.setDividerLocation(LIST_SHARE);
            split.removeComponentListener(this);
          }
        });
    return split;
  }

  /** "Update selected" if all maps to install are installed already, else "Install selected". */
  static String selectedActionTitle(final Collection<ManagedMap> installableMaps) {
    return installableMaps.stream()
            .allMatch(map -> map.getMapStatus() == ManagedMapStatus.UPDATE_AVAILABLE)
        ? "Update selected"
        : "Install selected";
  }

  /** Shows the maps in the view, eg after the player switched it. */
  void showView(final MapBrowserView newView) {
    view = newView;
    refresh();
  }

  /** Builds the rows again from the maps, eg after a map was installed or uninstalled. */
  void refresh() {
    rows.removeAll();
    rowsByMapName.clear();
    if (view == MapBrowserView.TILES) {
      rows.setLayout(new WrapLayout(FlowLayout.LEFT, 12, 12));
      rows.setBorder(BorderFactory.createEmptyBorder(4, 8, 24, 4));
    } else {
      rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
      // the rows have room around their highlight; with it, the texts start 32 from the edge and
      // the buttons end a little before the scroll bar
      rows.setBorder(BorderFactory.createEmptyBorder(4, 20, 24, 8));
    }
    rows.add(emptyText);
    final Listener listener = new Listener(this::clicked, this::ctrlClicked, this::shiftClicked);
    maps.get().stream()
        .sorted(BY_NAME)
        .forEach(
            map -> {
              final MapRow row = new MapRow(map, view, previews, actions, listener);
              rowsByMapName.put(map.getMapName(), row);
              rows.add(row);
            });
    bytesByMapName
        .keySet()
        .removeIf(
            name ->
                !rowsByMapName.containsKey(name)
                    || rowsByMapName.get(name).getMap().getMapStatus()
                        != ManagedMapStatus.DOWNLOADING);
    bytesByMapName.forEach((name, bytes) -> rowsByMapName.get(name).showProgress(bytes));
    detailsRow = null;
    showMatchingMaps();
  }

  private void clicked(final MapRow row) {
    selection.select(row.getMap().getMapName());
    showSelection();
  }

  private void ctrlClicked(final MapRow row) {
    selection.toggle(row.getMap().getMapName());
    showSelection();
  }

  private void shiftClicked(final MapRow row) {
    selection.selectRange(getShownMapNames(), row.getMap().getMapName());
    showSelection();
  }

  /** Shows only the maps whose name or tags contain the text, ignoring case; empty shows all. */
  void search(final String text) {
    searchedText = text.strip().toLowerCase(Locale.ROOT);
    showMatchingMaps();
  }

  /** Shows only the maps with the tags that also match the search; none shows all. */
  void filterByTags(final Set<Tag> tags) {
    checkedTags = Set.copyOf(tags);
    showMatchingMaps();
  }

  private void showMatchingMaps() {
    rowsByMapName
        .values()
        .forEach(
            row ->
                row.setVisible(
                    matchesSearch(row.getMap(), searchedText)
                        && MapTagFilter.matches(row.getMap(), checkedTags)));
    emptyText.setText(
        searchedText.isEmpty() && checkedTags.isEmpty()
            ? "No maps here yet."
            : "No maps match your search.");
    emptyText.setVisible(getShownMapNames().isEmpty());
    showSelection();
    rows.revalidate();
    rows.repaint();
  }

  /** The names of the shown maps, top to bottom. */
  List<String> getShownMapNames() {
    return Arrays.stream(rows.getComponents())
        .filter(MapRow.class::isInstance)
        .filter(Component::isVisible)
        .map(row -> ((MapRow) row).getMap().getMapName())
        .toList();
  }

  /** Whether the map's name or one of its tags contains the lower case text; empty matches all. */
  private static boolean matchesSearch(final ManagedMap map, final String lowerCaseText) {
    return lowerCaseText.isEmpty()
        || map.getMapName().toLowerCase(Locale.ROOT).contains(lowerCaseText)
        || Tag.of(map.getMapDownloadItem()).stream()
            .anyMatch(tag -> tag.value().toLowerCase(Locale.ROOT).contains(lowerCaseText));
  }

  /**
   * Highlights the selected maps among the shown ones and shows the lead in the details, so they
   * are never empty while the list shows maps; without maps, the details are hidden.
   */
  private void showSelection() {
    selection.keepShown(getShownMapNames());
    rowsByMapName.forEach(
        (name, row) -> {
          if (selection.isSelected(name)) {
            row.select();
          } else {
            row.deselect();
          }
        });
    selection
        .getLead()
        .map(rowsByMapName::get)
        .ifPresentOrElse(
            this::showDetails,
            () -> {
              detailsRow = null;
              details.setVisible(false);
            });
    showSelectionBar();
  }

  private void showDetails(final MapRow row) {
    if (row == detailsRow) {
      return;
    }
    detailsRow = row;
    details.show(row.getMap());
    Optional.ofNullable(bytesByMapName.get(row.getMap().getMapName()))
        .ifPresent(bytes -> details.showProgress(row.getMap().getMapDownloadItem(), bytes));
    details.setVisible(true);
  }

  /** With several maps selected, their number and the buttons; else the hint how to select. */
  private void showSelectionBar() {
    final int selectedCount = selection.getSelected().size();
    final List<ManagedMap> installable = getInstallableSelectedMaps();
    final boolean several = selectedCount > 1;
    selectionText.setText(several ? selectedCount + " selected" : SELECTION_HINT);
    selectionText.setForeground(several ? null : LookAndFeel.getSecondaryTextColor());
    installSelectedButton.setText(selectedActionTitle(installable));
    installSelectedButton.setVisible(several && !installable.isEmpty());
    clearButton.setVisible(several);
  }

  /** The map the details show, if the list shows maps. */
  @VisibleForTesting
  Optional<String> getSelectedMapName() {
    return selection.getLead();
  }

  /** The selected maps, sorted by name. */
  @VisibleForTesting
  List<String> getSelectedMapNames() {
    return getSelectedMaps().stream().map(ManagedMap::getMapName).toList();
  }

  /** Selects the map as a click on it does. */
  @VisibleForTesting
  void select(final String mapName) {
    Optional.ofNullable(rowsByMapName.get(mapName)).ifPresent(this::clicked);
  }

  /** Adds or removes the map as a ctrl-click on it does. */
  @VisibleForTesting
  void toggle(final String mapName) {
    Optional.ofNullable(rowsByMapName.get(mapName)).ifPresent(this::ctrlClicked);
  }

  /** Shows the progress of the map's download in its row and details, if the list has it. */
  void showProgress(final MapDownloadItem download, final long bytesReceived) {
    Optional.ofNullable(rowsByMapName.get(download.getMapName()))
        // progress that arrives after a cancel is left out
        .filter(row -> row.getMap().getMapStatus() == ManagedMapStatus.DOWNLOADING)
        .ifPresent(
            row -> {
              bytesByMapName.put(download.getMapName(), bytesReceived);
              row.showProgress(bytesReceived);
              details.showProgress(download, bytesReceived);
            });
  }
}
