package org.triplea.swing;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import lombok.NoArgsConstructor;

/**
 * Example usage: <code><pre>
 *   final JTable panel = JTableBuilder.builder()
 *       .columnNames(columns)
 *       .tableData(rows)
 *       .build();
 * </pre></code> Example usage with row mapper: <code><pre>
 *   final JTable panel = new JTableBuilder&lt;Person&gt;()
 *       .columnNames(columns)
 *       .rowData(personsList)
 *       .rowMapper(person -> List.of(person.getFirstName(), person.getLastName())
 *       .build();
 * </pre></code>
 *
 * @param <T> The value type that can be mapped to each row. If not specified will implicitly be a
 *     list of strings. Otherwise you can specify a list 'rowData' objects and how to map each one
 *     to a row with 'rowMapper'.
 */
@NoArgsConstructor
public class JTableBuilder<T> {

  private Function<T, List<String>> rowMapper;
  private Collection<T> rowData;
  private List<List<String>> tableData;
  private List<String> columnNames;

  public static <X> JTableBuilder<X> builder() {
    return new JTableBuilder<>();
  }

  /** Constructs the JTable swing component. */
  public JTable build() {
    Preconditions.checkNotNull(columnNames);
    Preconditions.checkState(
        (rowData == null && tableData == null) || (rowData == null ^ tableData == null),
        "May only specify one of rowData+rowMapper, tableData, or specify no table data.");
    Preconditions.checkState(
        rowData == null || rowMapper != null,
        "If specifying row data, must specify a mapper for the row data");
    Optional.ofNullable(tableData)
        .ifPresent(data -> verifyRowLengthsMatchHeader(data, columnNames.size()));

    final List<List<String>> data =
        Optional.ofNullable(tableData)
            .orElseGet(
                () ->
                    Optional.ofNullable(rowData)
                        .map(d -> d.stream().map(rowMapper).collect(Collectors.toList()))
                        .orElse(List.of()));

    final DefaultTableModel model =
        new DefaultTableModel() {
          private static final long serialVersionUID = -2814510707475316911L;

          @Override
          public boolean isCellEditable(final int row, final int column) {
            return false;
          }
        };
    columnNames.forEach(model::addColumn);

    data.stream().map(row -> row.toArray(new String[0])).forEach(model::addRow);
    final JTable table = new JTable(model);
    table.setAutoCreateRowSorter(true);
    return table;
  }

  /**
   * Make sure that the table is 'rectangular'. Given a number of headers, each row of data should
   * have the same number of columns.
   */
  private static void verifyRowLengthsMatchHeader(
      final List<List<String>> rowData, final int headerCount) {
    IntStream.range(0, rowData.size())
        .forEach(
            i ->
                checkArgument(
                    rowData.get(i).size() == headerCount,
                    String.format(
                        "Data row number: %s, had incorrect length: %s, needed to match "
                            + "number of column headers: %s, data row: %s",
                        i, rowData.get(i).size(), headerCount, rowData.get(i))));
  }

  /**
   * Convenience method for adding data rows to a given JTable.
   *
   * @param table The table to modify where we will add data rows.
   * @param rows Data rows to be added.
   */
  public static void addRows(final JTable table, final List<List<String>> rows) {
    final DefaultTableModel model = (DefaultTableModel) table.getModel();
    rows.forEach(row -> model.addRow(row.toArray(new String[0])));
  }

  public static void setData(final JTable table, final List<List<String>> rows) {
    final DefaultTableModel model = (DefaultTableModel) table.getModel();
    model.setRowCount(0);
    addRows(table, rows);
  }

  public JTableBuilder<T> columnNames(final String... columnNames) {
    return columnNames(List.of(columnNames));
  }

  public JTableBuilder<T> columnNames(final List<String> columnNames) {
    checkArgument(!columnNames.isEmpty());
    this.columnNames = columnNames;
    return this;
  }

  public JTableBuilder<T> tableData(final List<List<String>> tableData) {
    this.tableData = tableData;
    return this;
  }

  public JTableBuilder<T> rowData(final Collection<T> rowData) {
    this.rowData = rowData;
    return this;
  }

  public JTableBuilder<T> rowMapper(final Function<T, List<String>> rowMapper) {
    this.rowMapper = rowMapper;
    return this;
  }
}
