package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.verify;
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
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogData;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;
import org.triplea.java.DateTimeFormatterUtil;

@ExtendWith(MockitoExtension.class)
class AccessLogTabModelTest {

  private static final UserBanParams BAN_USER_PARAMS =
      UserBanParams.builder()
          .username("Never sail a captain.")
          .ip("Aye, fine beauty!")
          .systemId("Cockroaches stutter from endurance like jolly scallywags.")
          .minutesToBan(10)
          .build();
  private static final String USERNAME = "Ahoy there's nothing like the stormy death.";
  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().rowNumber(0).pageSize(10).build();

  private static final AccessLogData ACCESS_LOG_DATA_1 =
      AccessLogData.builder()
          .registered(true)
          .systemId("The mainland commands with punishment")
          .ip("Avast, yer not viewing me without a life!")
          .username("Biscuit eaters travel with grace at the dark rummage island!")
          .accessDate(
              LocalDateTime.of(2005, 1, 1, 23, 59) //
                  .toInstant(ZoneOffset.UTC)
                  .toEpochMilli())
          .build();

  private static final AccessLogData ACCESS_LOG_DATA_2 =
      AccessLogData.builder()
          .registered(false)
          .systemId("Command me woodchuck, ye clear wave!")
          .ip("Hoist me rum, ye mighty cockroach!")
          .username("Arg, wow.")
          .accessDate(
              LocalDateTime.of(2007, 1, 1, 23, 59) //
                  .toInstant(ZoneOffset.UTC)
                  .toEpochMilli())
          .build();

  @Mock private ToolboxAccessLogClient toolboxAccessLogClient;
  @Mock private ToolboxUserBanClient toolboxUserBanClient;
  @Mock private ToolboxUsernameBanClient toolboxUsernameBanClient;

  @InjectMocks private AccessLogTabModel accessLogTabModel;

  @BeforeAll
  static void setTimeFormattingToUtc() {
    DateTimeFormatterUtil.setDefaultToUtc();
  }

  @Test
  void fetchData() {
    when(toolboxAccessLogClient.getAccessLog(PAGING_PARAMS))
        .thenReturn(List.of(ACCESS_LOG_DATA_1, ACCESS_LOG_DATA_2));

    final List<List<String>> tableData = accessLogTabModel.fetchTableData(PAGING_PARAMS);

    assertThat(tableData, hasSize(2));

    ToolboxTabModelTestUtil.verifyTableDimensions(tableData, AccessLogTabModel.fetchTableHeaders());

    ToolboxTabModelTestUtil.verifyTableDataAtRow(
        tableData,
        0,
        "2005-1-1 23:59",
        ACCESS_LOG_DATA_1.getUsername(),
        ACCESS_LOG_DATA_1.getIp(),
        ACCESS_LOG_DATA_1.getSystemId(),
        "Y");

    ToolboxTabModelTestUtil.verifyTableDataAtRow(
        tableData,
        1,
        "2007-1-1 23:59",
        ACCESS_LOG_DATA_2.getUsername(),
        ACCESS_LOG_DATA_2.getIp(),
        ACCESS_LOG_DATA_2.getSystemId(),
        "");
  }

  @Test
  void banUserName() {
    accessLogTabModel.banUserName(USERNAME);

    verify(toolboxUsernameBanClient).addUsernameBan(USERNAME);
  }

  @Test
  void banUser() {
    accessLogTabModel.banUser(BAN_USER_PARAMS);

    verify(toolboxUserBanClient).banUser(BAN_USER_PARAMS);
  }
}
