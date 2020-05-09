package games.strategy.engine.lobby.moderator.toolbox.tabs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for verifying toolbox tab data. Notably most tabs have table-like views and this
 * class provides utility support for it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolboxTabModelTestUtil {
  /**
   * Given a set of table data, verifies that the values at a given row match a set of expected
   * values.
   *
   * @param data The set of table data.
   * @param row The row index, zero based, of the table data to verify.
   * @param expectedData Values that we expect to find at the specified row of data.
   */
  public static void verifyTableDataAtRow(
      final List<List<String>> data, final int row, final String... expectedData) {

    assertThat(
        String.format("Data table size: %s, row requested: %s", data.size(), row),
        data.size() > row,
        is(true));
    final List<String> rowData = data.get(row);

    for (int i = 0; i < expectedData.length; i++) {
      assertThat(
          String.format(
              "Mismatch at column %s, rowData: %s, expectedData: %s",
              i, rowData, List.of(expectedData)),
          rowData.get(i),
          is(expectedData[i]));
    }
  }

  /**
   * Verifies that a table data is 'square', that each row has the same length as a set of given
   * headers.
   */
  public static void verifyTableDimensions(
      final List<List<String>> data, final List<String> headers) {
    for (int i = 0; i < data.size(); i++) {
      assertThat(
          String.format(
              "Row: %s expected to have length %s, had length %s.\n"
                  + "Row data = %s\n"
                  + "Expected to match column headers: %s",
              i, headers.size(), data.get(i).size(), data.get(i), headers),
          data.get(i).size(),
          is(headers.size()));
    }
  }
}
