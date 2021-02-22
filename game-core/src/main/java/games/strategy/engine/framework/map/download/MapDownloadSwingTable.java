package games.strategy.engine.framework.map.download;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.triplea.swing.JTableBuilder;

/**
 * UI component representing a list of maps to download. The table is sortable and displays
 * additional columns with map metadata.
 */
public class MapDownloadSwingTable {
  private final JTable table;

  public MapDownloadSwingTable(final Collection<DownloadFileDescription> maps) {
    table =
        JTableBuilder.<DownloadFileDescription>builder()
            .columnNames("Map", "Category")
            .rowData(
                maps.stream()
                    .sorted(Comparator.comparing(DownloadFileDescription::getMapName))
                    .collect(Collectors.toList()))
            .rowMapper(map -> List.of(map.getMapName(), map.getMapCategory().outputLabel))
            .build();
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
              final List<String> selections = getSelectedMapNames();
              if (!selections.isEmpty()) {
                mapNameSelectionListener.accept(selections);
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

  /** Removes the table row with the given map name. */
  void removeMapRow(final String mapName) {
    for (int i = 0, n = table.getModel().getRowCount(); i < n; i++) {
      final String mapInTable = String.valueOf(table.getModel().getValueAt(i, 0));
      if (mapName.equals(mapInTable)) {
        ((DefaultTableModel) table.getModel()).removeRow(i);
        return;
      }
    }
  }
}
