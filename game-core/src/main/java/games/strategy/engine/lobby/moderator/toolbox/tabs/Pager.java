package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.util.List;
import java.util.function.Function;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;

/**
 * Class to take care of paging details. Keeps track of current row, when requesting data will
 * increment the current row to request the next row on the next data fetch.
 */
public class Pager {
  private int rowNumber = 0;
  private final int pageSize;
  private final Function<PagingParams, List<List<String>>> dataFetcher;

  @Builder
  Pager(final int pageSize, final Function<PagingParams, List<List<String>>> dataFetcher) {
    this.pageSize = pageSize;
    this.dataFetcher = dataFetcher;
  }

  /** Resets the current row to zero and (always) loads the first page of data. */
  public List<List<String>> getTableData() {
    rowNumber = 0;
    return loadMoreData();
  }

  /** Fetches the next page of data. */
  public List<List<String>> loadMoreData() {
    final List<List<String>> data = fetchData();
    rowNumber += pageSize;
    return data;
  }

  private List<List<String>> fetchData() {
    return dataFetcher.apply(
        PagingParams.builder().rowNumber(rowNumber).pageSize(pageSize).build());
  }
}
