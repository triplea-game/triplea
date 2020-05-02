package org.triplea.modules.moderation.player.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.GamePlayerLookup;
import org.triplea.db.dao.moderator.player.info.PlayerAliasRecord;
import org.triplea.db.dao.moderator.player.info.PlayerBanRecord;
import org.triplea.db.dao.moderator.player.info.PlayerInfoForModeratorDao;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.Alias;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.BanInformation;

@ExtendWith(MockitoExtension.class)
class FetchPlayerInfoModuleTest {

  private static final GamePlayerLookup GAME_PLAYER_LOOKUP =
      GamePlayerLookup.builder()
          .ip("1.1.1.1")
          .systemId(SystemId.of("system-id"))
          .userName(UserName.of("user-name"))
          .build();

  private static final PlayerAliasRecord PLAYER_ALIAS_RECORD =
      PlayerAliasRecord.builder()
          .username("alias-user-name")
          .systemId("system-id2")
          .ip("2.3.2.3")
          .date(LocalDateTime.of(2000, 1, 1, 1, 1, 1).toInstant(ZoneOffset.UTC))
          .build();

  private static final PlayerBanRecord PLAYER_BAN_RECORD =
      PlayerBanRecord.builder()
          .username("banned-name")
          .systemId("id-at-time-of-ban")
          .ip("5.5.5.6")
          .banStart(LocalDateTime.of(2001, 1, 1, 1, 1, 1).toInstant(ZoneOffset.UTC))
          .banEnd(LocalDateTime.of(2100, 1, 1, 1, 1, 1).toInstant(ZoneOffset.UTC))
          .build();

  @Mock private ApiKeyDaoWrapper apiKeyDaoWrapper;
  @Mock private PlayerInfoForModeratorDao playerInfoForModeratorDao;

  @InjectMocks private FetchPlayerInfoModule fetchPlayerInfoModule;

  @Test
  void unableToFindPlayerChatIdThrows() {
    when(apiKeyDaoWrapper.lookupPlayerByChatId(PlayerChatId.of("id"))).thenReturn(Optional.empty());
    assertThrows(
        IllegalArgumentException.class, () -> fetchPlayerInfoModule.apply(PlayerChatId.of("id")));
  }

  @Test
  @DisplayName("Verify data transformation into a player summary object")
  void playerLookup() {
    when(apiKeyDaoWrapper.lookupPlayerByChatId(PlayerChatId.of("id")))
        .thenReturn(Optional.of(GAME_PLAYER_LOOKUP));
    when(playerInfoForModeratorDao.lookupPlayerAliasRecords(
            GAME_PLAYER_LOOKUP.getSystemId().getValue(), GAME_PLAYER_LOOKUP.getIp()))
        .thenReturn(List.of(PLAYER_ALIAS_RECORD));
    when(playerInfoForModeratorDao.lookupPlayerBanRecords(
            GAME_PLAYER_LOOKUP.getSystemId().getValue(), GAME_PLAYER_LOOKUP.getIp()))
        .thenReturn(List.of(PLAYER_BAN_RECORD));

    final var playerSummaryForModerator = fetchPlayerInfoModule.apply(PlayerChatId.of("id"));
    assertThat(
        playerSummaryForModerator.getName(), is(GAME_PLAYER_LOOKUP.getUserName().getValue()));
    assertThat(playerSummaryForModerator.getIp(), is(GAME_PLAYER_LOOKUP.getIp()));
    assertThat(
        playerSummaryForModerator.getSystemId(), is(GAME_PLAYER_LOOKUP.getSystemId().getValue()));

    assertThat(playerSummaryForModerator.getBans(), hasSize(1));
    assertThat(
        playerSummaryForModerator.getBans().iterator().next(),
        is(
            BanInformation.builder()
                .ip(PLAYER_BAN_RECORD.getIp())
                .systemId(PLAYER_BAN_RECORD.getSystemId())
                .name(PLAYER_BAN_RECORD.getUsername())
                .epochMilliStartDate(PLAYER_BAN_RECORD.getBanStart().toEpochMilli())
                .epochMillEndDate(PLAYER_BAN_RECORD.getBanEnd().toEpochMilli())
                .build()));

    assertThat(playerSummaryForModerator.getAliases(), hasSize(1));
    assertThat(
        playerSummaryForModerator.getAliases().iterator().next(),
        is(
            Alias.builder()
                .systemId(PLAYER_ALIAS_RECORD.getSystemId())
                .name(PLAYER_ALIAS_RECORD.getUsername())
                .ip(PLAYER_ALIAS_RECORD.getIp())
                .epochMilliDate(PLAYER_ALIAS_RECORD.getDate().toEpochMilli())
                .build()));
  }
}
