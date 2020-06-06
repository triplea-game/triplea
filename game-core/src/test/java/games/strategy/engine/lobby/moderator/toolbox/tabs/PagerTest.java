package games.strategy.engine.lobby.moderator.toolbox.tabs;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;

@ExtendWith(MockitoExtension.class)
class PagerTest {

  private static final int PAGE_SIZE = 10;
  @Mock private Function<PagingParams, List<List<String>>> dataFetcher;

  private Pager pager;

  @BeforeEach
  void setUp() {
    pager = Pager.builder().dataFetcher(dataFetcher).pageSize(PAGE_SIZE).build();
  }

  @Test
  void getTableData() {
    pager.getTableData();

    verify(dataFetcher).apply(PagingParams.builder().pageSize(PAGE_SIZE).rowNumber(0).build());
  }

  @Test
  void getTableDataRequestsRowZero() {
    pager.getTableData();
    pager.loadMoreData();
    pager.getTableData();
    pager.loadMoreData();

    for (int i = 0; i < 1; i++) {
      verify(dataFetcher, times(2))
          .apply(PagingParams.builder().pageSize(PAGE_SIZE).rowNumber(PAGE_SIZE * i).build());
    }
  }

  @Test
  void loadMoreDataIncrementsRowNumber() {
    pager.loadMoreData();
    pager.loadMoreData();
    pager.loadMoreData();

    for (int i = 0; i < 3; i++) {
      verify(dataFetcher, times(1))
          .apply(PagingParams.builder().pageSize(PAGE_SIZE).rowNumber(PAGE_SIZE * i).build());
    }
  }

  @Test
  void getTableDataIncrementsLoadMoreData() {
    pager.getTableData();
    pager.loadMoreData();

    for (int i = 0; i < 1; i++) {
      verify(dataFetcher, times(1))
          .apply(PagingParams.builder().pageSize(PAGE_SIZE).rowNumber(PAGE_SIZE * i).build());
    }
  }
}
