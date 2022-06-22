package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users;

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
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanData;
import org.triplea.java.DateTimeFormatterUtil;

@ExtendWith(MockitoExtension.class)
class BannedUsersTabModelTest {

  private static final String BAN_ID = "Avast, clear lagoon. you won't love the freighter.";
  private static final UserBanData BANNED_USER_DATA =
      UserBanData.builder()
          .banDate(
              LocalDateTime.of(2000, 1, 1, 23, 59) //
                  .toInstant(ZoneOffset.UTC)
                  .toEpochMilli())
          .banExpiry(
              LocalDateTime.of(2000, 1, 2, 2, 59) //
                  .toInstant(ZoneOffset.UTC)
                  .toEpochMilli())
          .banId("Pirates hobble with endurance at the old port degas!")
          .hashedMac("Proud, weird scabbards greedily loot a misty, black breeze.")
          .ip("Well, never endure a wave.")
          .username("The anchor fears with treasure, burn the bahamas until it whines.")
          .build();

  @Mock private ToolboxUserBanClient toolboxUserBanClient;

  @InjectMocks private BannedUsersTabModel bannedUsersTabModel;

  @BeforeAll
  static void setDateTimeFormattingToUtc() {
    DateTimeFormatterUtil.setDefaultToUtc();
  }

  @Test
  void fetchTableData() {
    when(toolboxUserBanClient.getUserBans()).thenReturn(List.of(BANNED_USER_DATA));

    final List<List<String>> tableData = bannedUsersTabModel.fetchTableData();

    ToolboxTabModelTestUtil.verifyTableDimensions(
        tableData, BannedUsersTabModel.fetchTableHeaders());

    ToolboxTabModelTestUtil.verifyTableDataAtRow(
        tableData,
        0,
        BANNED_USER_DATA.getBanId(),
        BANNED_USER_DATA.getUsername(),
        "2000-1-1 23:59",
        BANNED_USER_DATA.getIp(),
        BANNED_USER_DATA.getHashedMac(),
        "2000-1-2 2:59",
        BannedUsersTabModel.REMOVE_BUTTON_TEXT);
  }

  @Test
  void removeBan() {
    bannedUsersTabModel.removeBan(BAN_ID);

    verify(toolboxUserBanClient).removeUserBan(BAN_ID);
  }
}
