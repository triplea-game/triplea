package games.strategy.engine.lobby.moderator.toolbox.tabs;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.LookupModeratorEventsArgs;
import org.triplea.http.client.moderator.toolbox.ModeratorEvent;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;

@ExtendWith(MockitoExtension.class)
class EventLogTabModelTest extends AbstractClientSettingTestCase {

  private static final String API_KEY = "api-key";

  private static final ModeratorEvent EVENT_1 = ModeratorEvent.builder()
      .date(Instant.now())
      .actionTarget("Malaria is a cloudy pin.")
      .moderatorAction("Jolly roger, real pin. go to puerto rico.")
      .moderatorName("All parrots loot rainy, stormy fish.")
      .build();

  private static final ModeratorEvent EVENT_2 = ModeratorEvent.builder()
      .date(Instant.now().minusSeconds(1000L))
      .actionTarget("Strength is a gutless tuna.")
      .moderatorAction("Doubloons travel with booty at the stormy madagascar!")
      .moderatorName("The son crushes with life, love the lighthouse.")
      .build();
  private static final ModeratorEvent EVENT_3 = ModeratorEvent.builder()
      .date(Instant.now().minusSeconds(3000L))
      .actionTarget("Never burn a grog.")
      .moderatorAction("Jolly roger, haul me anchor, ye dead sail!")
      .moderatorName("Belay, stormy plank. you won't endure the quarter-deck.")
      .build();

  @Mock
  private ModeratorToolboxClient moderatorToolboxClient;

  @InjectMocks
  private EventLogTabModel eventLogTabModel;

  @BeforeEach
  void setupApiKey() {
    ClientSetting.moderatorApiKey.setValue(API_KEY);
  }


  /**
   * Simple test that fetches log table data and verifies the values. Here we mostly convert the conversion
   * from a list of beans to table data format, a list of lists.
   */
  @Test
  void getEventLogTableData() {
    when(
        moderatorToolboxClient.lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(0)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build()))
                .thenReturn(Arrays.asList(EVENT_1, EVENT_2));

    final List<List<String>> tableData = eventLogTabModel.getEventLogTableData();

    assertThat(tableData, hasSize(2));
    assertThat(tableData.get(0), hasSize(4));
    assertThat(tableData.get(0).get(0), is(EVENT_1.getDate().toString()));
    assertThat(tableData.get(0).get(1), is(EVENT_1.getModeratorName()));
    assertThat(tableData.get(0).get(2), is(EVENT_1.getModeratorAction()));
    assertThat(tableData.get(0).get(3), is(EVENT_1.getActionTarget()));

    assertThat(tableData.get(1), hasSize(4));
    assertThat(tableData.get(1).get(0), is(EVENT_2.getDate().toString()));
    assertThat(tableData.get(1).get(1), is(EVENT_2.getModeratorName()));
    assertThat(tableData.get(1).get(2), is(EVENT_2.getModeratorAction()));
    assertThat(tableData.get(1).get(3), is(EVENT_2.getActionTarget()));
  }

  /**
   * Verifies the load more fetches data and we verify the data is as expected.
   */
  @Test
  void loadMore() {
    when(moderatorToolboxClient.lookupModeratorEvents(any()))
        .thenReturn(singletonList(EVENT_1))
        .thenReturn(singletonList(EVENT_2))
        .thenReturn(singletonList(EVENT_3));

    List<List<String>> tableData = eventLogTabModel.getEventLogTableData();
    assertThat(tableData, hasSize(1));

    tableData = eventLogTabModel.loadMore();
    assertThat(tableData, hasSize(1));
    assertThat(tableData.get(0), hasSize(4));
    assertThat(tableData.get(0).get(0), is(EVENT_2.getDate().toString()));
    assertThat(tableData.get(0).get(1), is(EVENT_2.getModeratorName()));
    assertThat(tableData.get(0).get(2), is(EVENT_2.getModeratorAction()));
    assertThat(tableData.get(0).get(3), is(EVENT_2.getActionTarget()));

    tableData = eventLogTabModel.loadMore();
    assertThat(tableData, hasSize(1));
    assertThat(tableData.get(0), hasSize(4));
    assertThat(tableData.get(0).get(0), is(EVENT_3.getDate().toString()));
    assertThat(tableData.get(0).get(1), is(EVENT_3.getModeratorName()));
    assertThat(tableData.get(0).get(2), is(EVENT_3.getModeratorAction()));
    assertThat(tableData.get(0).get(3), is(EVENT_3.getActionTarget()));

    verify(moderatorToolboxClient)
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(0)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());
    verify(moderatorToolboxClient)
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(EventLogTabModel.PAGE_SIZE)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());
    verify(moderatorToolboxClient)
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(EventLogTabModel.PAGE_SIZE * 2)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());
  }

  /**
   * 'getEventLogTableData' should always fetch the first rows of data. Calling the method twice we expect
   * to make the same API call on the backend.
   */
  @Test
  void verifyGetEventLogPageIncrementsDoesNotChange() {
    when(
        moderatorToolboxClient.lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(0)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build()))
                .thenReturn(singletonList(EVENT_1));

    eventLogTabModel.getEventLogTableData();
    eventLogTabModel.getEventLogTableData();

    verify(moderatorToolboxClient, times(2))
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(0)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());
  }

  /**
   * Here we check that calling 'getEventLogTableData' "resets" the table data. When that happens
   * we expect the 'loadMore' to fetch the correct pages, notably after the reset we should then
   * again request the second page when 'loadMore' is called.
   */
  @Test
  void verifyGetEventLogPageIncrementsResetsAfterCallingLoadMore() {
    when(moderatorToolboxClient.lookupModeratorEvents(any()))
        .thenReturn(singletonList(EVENT_1));

    eventLogTabModel.getEventLogTableData();
    eventLogTabModel.loadMore();
    eventLogTabModel.getEventLogTableData();
    eventLogTabModel.loadMore();
    eventLogTabModel.loadMore();

    verify(moderatorToolboxClient, times(2))
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(0)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());

    verify(moderatorToolboxClient, times(2))
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(EventLogTabModel.PAGE_SIZE)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());

    verify(moderatorToolboxClient)
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(EventLogTabModel.PAGE_SIZE * 2)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());
  }

  /**
   * Here we test in detail the page increments when calling 'loadMore' in succession.
   */
  @Test
  void verifyLoadMorePageIncrements() {
    when(moderatorToolboxClient.lookupModeratorEvents(any()))
        .thenReturn(singletonList(EVENT_1));

    eventLogTabModel.getEventLogTableData();
    eventLogTabModel.loadMore();
    eventLogTabModel.loadMore();
    eventLogTabModel.loadMore();

    verify(moderatorToolboxClient)
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(0)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());

    verify(moderatorToolboxClient)
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(EventLogTabModel.PAGE_SIZE)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());

    verify(moderatorToolboxClient)
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(EventLogTabModel.PAGE_SIZE * 2)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());

    verify(moderatorToolboxClient)
        .lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(API_KEY)
            .rowStart(EventLogTabModel.PAGE_SIZE * 3)
            .rowCount(EventLogTabModel.PAGE_SIZE)
            .build());
  }
}
