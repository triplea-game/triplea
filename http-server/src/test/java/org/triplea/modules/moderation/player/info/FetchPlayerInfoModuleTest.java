package org.triplea.modules.moderation.player.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerIdentifiersByApiKeyLookup;
import org.triplea.db.dao.moderator.player.info.PlayerAliasRecord;
import org.triplea.db.dao.moderator.player.info.PlayerBanRecord;
import org.triplea.db.dao.moderator.player.info.PlayerInfoForModeratorDao;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.lobby.moderator.PlayerSummary.Alias;
import org.triplea.http.client.lobby.moderator.PlayerSummary.BanInformation;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.chat.ChatterSession;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.game.listing.GameListing;

@ExtendWith(MockitoExtension.class)
class FetchPlayerInfoModuleTest {

  private static final PlayerIdentifiersByApiKeyLookup GAME_PLAYER_LOOKUP =
      PlayerIdentifiersByApiKeyLookup.builder()
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

  private static final AuthenticatedUser authenticatedPlayer =
      AuthenticatedUser.builder()
          .name("name")
          .apiKey(ApiKey.of("player-api-key"))
          .userRole(UserRole.ANONYMOUS)
          .build();

  private static final AuthenticatedUser authenticatedModerator =
      AuthenticatedUser.builder()
          .name("name")
          .userId(123)
          .userRole(UserRole.MODERATOR)
          .apiKey(ApiKey.of("moderator-api-key"))
          .build();

  private static final PlayerBanRecord PLAYER_BAN_RECORD =
      PlayerBanRecord.builder()
          .username("banned-name")
          .systemId("id-at-time-of-ban")
          .ip("5.5.5.6")
          .banStart(LocalDateTime.of(2001, 1, 1, 1, 1, 1).toInstant(ZoneOffset.UTC))
          .banEnd(LocalDateTime.of(2100, 1, 1, 1, 1, 1).toInstant(ZoneOffset.UTC))
          .build();

  @Mock private PlayerApiKeyDaoWrapper apiKeyDaoWrapper;
  @Mock private PlayerInfoForModeratorDao playerInfoForModeratorDao;
  @Mock private Chatters chatters;
  @Mock private GameListing gameListing;

  @InjectMocks private FetchPlayerInfoModule fetchPlayerInfoModule;

  private ChatterSession chatterSession;

  @BeforeEach
  void setupChatterSessionData() {
    chatterSession =
        ChatterSession.builder()
            .ip(IpAddressParser.fromString("1.2.3.4"))
            .apiKeyId(-1)
            .chatParticipant(
                ChatParticipant.builder()
                    .userName("user-name")
                    .isModerator(false)
                    .status("AFK")
                    .playerChatId("player-chat-id")
                    .build())
            .session(mock(Session.class))
            .build();
  }

  @Test
  void unableToFindPlayerChatIdInChattersThrows() {
    when(chatters.lookupPlayerByChatId(PlayerChatId.of("id"))).thenReturn(Optional.empty());

    assertThrows(
        IllegalArgumentException.class,
        () -> fetchPlayerInfoModule.apply(authenticatedPlayer, PlayerChatId.of("id")));
  }

  @Test
  void unableToFindPlayerChatIdInApiKeyTableThrows() {
    when(chatters.lookupPlayerByChatId(PlayerChatId.of("id")))
        .thenReturn(Optional.of(chatterSession));
    when(apiKeyDaoWrapper.lookupPlayerByChatId(PlayerChatId.of("id"))) //
        .thenReturn(Optional.empty());
    assertThrows(
        IllegalArgumentException.class,
        () -> fetchPlayerInfoModule.apply(authenticatedModerator, PlayerChatId.of("id")));
  }

  @Test
  @DisplayName("Verify data transformation retrieving info available to any player")
  void playerLookupByPlayer() {
    when(chatters.lookupPlayerByChatId(PlayerChatId.of("id")))
        .thenReturn(Optional.of(chatterSession));

    when(gameListing.getGameNamesPlayerHasJoined(chatterSession.getChatParticipant().getUserName()))
        .thenReturn(Set.of("Host1", "Host2"));

    final var playerSummaryForPlayer =
        fetchPlayerInfoModule.apply(authenticatedPlayer, PlayerChatId.of("id"));

    assertThat(playerSummaryForPlayer.getCurrentGames(), hasItems("Host1", "Host2"));

    // lookup of more information is reserved to moderator players.
    verify(apiKeyDaoWrapper, never()).lookupPlayerByChatId(any());
  }

  @Test
  @DisplayName("Verify data transformation into a player summary object")
  void playerLookupByModerator() {
    when(chatters.lookupPlayerByChatId(PlayerChatId.of("id")))
        .thenReturn(Optional.of(chatterSession));
    when(apiKeyDaoWrapper.lookupPlayerByChatId(PlayerChatId.of("id")))
        .thenReturn(Optional.of(GAME_PLAYER_LOOKUP));
    when(playerInfoForModeratorDao.lookupPlayerAliasRecords(
            GAME_PLAYER_LOOKUP.getSystemId().getValue(), GAME_PLAYER_LOOKUP.getIp()))
        .thenReturn(List.of(PLAYER_ALIAS_RECORD));
    when(playerInfoForModeratorDao.lookupPlayerBanRecords(
            GAME_PLAYER_LOOKUP.getSystemId().getValue(), GAME_PLAYER_LOOKUP.getIp()))
        .thenReturn(List.of(PLAYER_BAN_RECORD));

    final var playerSummaryForModerator =
        fetchPlayerInfoModule.apply(authenticatedModerator, PlayerChatId.of("id"));
    assertThat(playerSummaryForModerator.getIp(), is(chatterSession.getIp().toString()));
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
