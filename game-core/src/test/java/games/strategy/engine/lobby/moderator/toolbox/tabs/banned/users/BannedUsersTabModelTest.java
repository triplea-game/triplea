package games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.lobby.moderator.toolbox.tabs.ToolboxTabModelTestUtil;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanData;

@ExtendWith(MockitoExtension.class)
class BannedUsersTabModelTest {

  private static final String BAN_ID = "Avast, clear lagoon. you won't love the freighter.";
  private static final UserBanData BANNED_USER_DATA =
      UserBanData.builder()
          .banDate(Instant.now())
          .banExpiry(Instant.now().plusSeconds(100))
          .banId("Pirates hobble with endurance at the old port degas!")
          .hashedMac("Proud, weird scabbards greedily loot a misty, black breeze.")
          .ip("Well, never endure a wave.")
          .username("The anchor fears with treasure, burn the bahamas until it whines.")
          .build();

  @Mock private ToolboxUserBanClient toolboxUserBanClient;

  @InjectMocks private BannedUsersTabModel bannedUsersTabModel;

  @Test
  void fetchTableData() {
    when(toolboxUserBanClient.getUserBans())
        .thenReturn(Collections.singletonList(BANNED_USER_DATA));

    final List<List<String>> tableData = bannedUsersTabModel.fetchTableData();

    ToolboxTabModelTestUtil.verifyTableDimensions(
        tableData, BannedUsersTabModel.fetchTableHeaders());

    ToolboxTabModelTestUtil.verifyTableDataAtRow(
        tableData,
        0,
        BANNED_USER_DATA.getBanId(),
        BANNED_USER_DATA.getUsername(),
        BANNED_USER_DATA.getBanDate().toString(),
        BANNED_USER_DATA.getIp(),
        BANNED_USER_DATA.getHashedMac(),
        BANNED_USER_DATA.getBanExpiry().toString(),
        BannedUsersTabModel.REMOVE_BUTTON_TEXT);
  }

  @Test
  void removeBan() {
    bannedUsersTabModel.removeBan(BAN_ID);

    verify(toolboxUserBanClient).removeUserBan(BAN_ID);
  }
}
