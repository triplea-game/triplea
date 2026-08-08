package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Test;

class MapDownloadSwingTableTest {

  @Test
  void getSwingComponentWithNoMapsReturnsEmptyTable() {
    final var table = new MapDownloadSwingTable(List.of()).getSwingComponent();

    assertThat(table.getRowCount(), is(0));
  }
}
