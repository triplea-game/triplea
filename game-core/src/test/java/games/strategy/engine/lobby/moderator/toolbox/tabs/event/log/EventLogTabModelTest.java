package games.strategy.engine.lobby.moderator.toolbox.tabs.event.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;

import games.strategy.engine.lobby.moderator.toolbox.tabs.ToolboxTabModelTestUtil;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ModeratorEvent;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxEventLogClient;
import org.triplea.java.DateTimeFormatterUtil;

@ExtendWith(MockitoExtension.class)
class EventLogTabModelTest {
  private static final ModeratorEvent EVENT_1 =
      ModeratorEvent.builder()
          .date(
              LocalDateTime.of(2006, 1, 2, 2, 59) //
                  .toInstant(ZoneOffset.UTC)
                  .toEpochMilli())
          .actionTarget("Malaria is a cloudy pin.")
          .moderatorAction("Jolly roger, real pin. go to puerto rico.")
          .moderatorName("All parrots loot rainy, stormy fish.")
          .build();

  private static final ModeratorEvent EVENT_2 =
      ModeratorEvent.builder()
          .date(
              LocalDateTime.of(2006, 1, 2, 3, 59) //
                  .toInstant(ZoneOffset.UTC)
                  .toEpochMilli())
          .actionTarget("Strength is a gutless tuna.")
          .moderatorAction("Doubloons travel with booty at the stormy madagascar!")
          .moderatorName("The son crushes with life, love the lighthouse.")
          .build();

  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().rowNumber(0).pageSize(10).build();

  @Mock private ToolboxEventLogClient toolboxEventLogClient;

  @InjectMocks private EventLogTabModel eventLogTabModel;

  @BeforeAll
  static void setDateTimeFormattingToUtc() {
    DateTimeFormatterUtil.setDefaultToUtc();
  }

  /**
   * Simple test that fetches log table data and verifies the values. Here we mostly convert the
   * conversion from a list of beans to table data format, a list of lists.
   */
  @Test
  void getEventLogTableData() {
    when(toolboxEventLogClient.lookupModeratorEvents(PAGING_PARAMS))
        .thenReturn(List.of(EVENT_1, EVENT_2));

    final List<List<String>> tableData = eventLogTabModel.fetchTableData(PAGING_PARAMS);

    assertThat(tableData, hasSize(2));

    ToolboxTabModelTestUtil.verifyTableDimensions(tableData, EventLogTabModel.fetchTableHeaders());

    ToolboxTabModelTestUtil.verifyTableDataAtRow(
        tableData,
        0,
        EVENT_1.getDate().toString(),
        EVENT_1.getModeratorName(),
        EVENT_1.getModeratorAction(),
        EVENT_1.getActionTarget());

    ToolboxTabModelTestUtil.verifyTableDataAtRow(
        tableData,
        1,
        EVENT_2.getDate().toString(),
        EVENT_2.getModeratorName(),
        EVENT_2.getModeratorAction(),
        EVENT_2.getActionTarget());
  }
}
