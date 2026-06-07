package games.strategy.engine.framework.map.download;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.http.client.lobby.maps.listing.MapTag;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.JTableTypeAheadListener;

/**
 * UI component representing a list of maps to download. The table is sortable and displays
 * additional columns with map metadata.
 */
public class MapDownloadSwingTable {
  private final JTable table;
  private final List<String> tagNames;
  private boolean blockMapSelectionListeners;

  MapDownloadSwingTable(Collection<ManagedMap> managedMaps) {
    // Build a JTable that has n+1 columns, the first column (+1) is the map name,
    // the 'n' columns are one for each tag.
    // Note, not all maps have all tags (potentially sparse), we will display a blank
    // value if a map does not have a given tag.

    final List<String> columnNames = new ArrayList<>();
    columnNames.add("Map");

    // Get the full set of all tag names in display order
    this.tagNames =
        managedMaps.stream()
            .map(ManagedMap::getMapDownloadItem)
            .map(MapDownloadItem::getMapTags)
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(MapTag::getDisplayOrder))
            .map(MapTag::getName)
            .distinct()
            .collect(Collectors.toList());

    columnNames.addAll(tagNames);

    table =
        JTableBuilder.<ManagedMap>builder()
            .columnNames(columnNames)
            .rowData(managedMaps)
            .rowMapper(managedMap -> rowMapper(managedMap, tagNames))
            .build();
    table.getRowSorter().toggleSortOrder(0);
    table.addKeyListener(new JTableTypeAheadListener(table, 0));
  }

  /**
   * Given a download item and a set of tags (in correct display order), returns values for a table
   * row, map name and then each of the map's tag values.
   */
  private List<String> rowMapper(final ManagedMap managedMap, final List<String> mapTags) {
    final MapDownloadItem mapDownloadItem = managedMap.getMapDownloadItem();
    final List<String> rowValues = new ArrayList<>();
    rowValues.add(mapDownloadItem.getMapName());

    for (final String tag : mapTags) {
      final String tagValue = mapDownloadItem.getTagValue(tag);
      rowValues.add(tagValue);
    }
    return rowValues;
  }

  public JTable getSwingComponent() {
    // select first row, will trigger any selection listeners
    table.setRowSelectionInterval(0, 0);
    return table;
  }

  /**
   * Adds a listener that is called when a table selection occurs.
   *
   * @param mapNameSelectionListener Consumer handling map selection name, input to the consumer is
   *     the list of selected maps.
   */
  public void addMapSelectionListener(final Consumer<List<String>> mapNameSelectionListener) {
    table
        .getSelectionModel()
        .addListSelectionListener(
            listener -> {
              if (!blockMapSelectionListeners) {
                mapNameSelectionListener.accept(getSelectedMapNames());
              }
            });
  }

  /** Returns list of currently selected maps. */
  public List<String> getSelectedMapNames() {
    if (table.getSelectedRows() == null || table.getSelectedRows().length == 0) {
      return List.of();
    }

    return Arrays.stream(table.getSelectedRows())
        .mapToObj(selectedIndex -> table.getValueAt(selectedIndex, 0))
        .map(String::valueOf)
        .collect(Collectors.toList());
  }

  void removeMapRows(final List<ManagedMap> removeMaps) {
    final Set<String> removeMapNames =
        removeMaps.stream().map(ManagedMap::getMapName).collect(Collectors.toSet());

    final DefaultTableModel model = (DefaultTableModel) table.getModel();

    // remove iterating backwards to avoid indices shifting
    blockMapSelectionListeners = true;
    for (int row = model.getRowCount() - 1; row >= 0; row--) {
      final String mapNameInTable = String.valueOf(model.getValueAt(row, 0));
      if (removeMapNames.contains(mapNameInTable)) {
        model.removeRow(row);
      }
    }
    blockMapSelectionListeners = false;
  }

  void setMaps(final List<ManagedMap> newMapsList) {
    final DefaultTableModel model = (DefaultTableModel) table.getModel();
    model.setRowCount(0);

    newMapsList.stream()
        .map(map -> rowMapper(map, tagNames))
        .forEach(row -> model.addRow(row.toArray()));
    table.repaint();
  }
}
