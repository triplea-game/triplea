package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names;

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
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.UsernameBanData;
import org.triplea.java.DateTimeFormatterUtil;

@ExtendWith(MockitoExtension.class)
class BannedUsernamesTabModelTest {

  private static final String USERNAME = "Belay, yer not desiring me without a pestilence!";
  private static final UsernameBanData BANNED_USERNAME_DATA =
      UsernameBanData.builder()
          .banDate(
              LocalDateTime.of(2010, 1, 1, 23, 59) //
                  .toInstant(ZoneOffset.UTC)
                  .toEpochMilli())
          .bannedName("Fear the pacific ocean until it falls.")
          .build();

  @Mock private ToolboxUsernameBanClient toolboxUsernameBanClient;

  @InjectMocks private BannedUsernamesTabModel bannedUsernamesTabModel;

  @BeforeAll
  static void setDateTimeFormattingToUtc() {
    DateTimeFormatterUtil.setDefaultToUtc();
  }

  @Test
  void fetchTableData() {
    when(toolboxUsernameBanClient.getUsernameBans()).thenReturn(List.of(BANNED_USERNAME_DATA));

    final List<List<String>> tableData = bannedUsernamesTabModel.fetchTableData();

    ToolboxTabModelTestUtil.verifyTableDimensions(
        tableData, BannedUsernamesTabModel.fetchTableHeaders());
    ToolboxTabModelTestUtil.verifyTableDataAtRow(
        tableData,
        0,
        BANNED_USERNAME_DATA.getBannedName(),
        "2010-1-1 23:59",
        BannedUsernamesTabModel.REMOVE_BUTTON_TEXT);
  }

  @Test
  void removeUsernameBan() {
    bannedUsernamesTabModel.removeUsernameBan(USERNAME);

    verify(toolboxUsernameBanClient).removeUsernameBan(USERNAME);
  }

  @Test
  void addUsernameBan() {
    bannedUsernamesTabModel.addUsernameBan(USERNAME);

    verify(toolboxUsernameBanClient).addUsernameBan(USERNAME);
  }
}
