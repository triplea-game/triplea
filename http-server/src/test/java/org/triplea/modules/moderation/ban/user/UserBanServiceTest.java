package org.triplea.modules.moderation.ban.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;
import org.triplea.db.dao.user.ban.UserBanDao;
import org.triplea.db.dao.user.ban.UserBanRecord;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessagingBus;

@ExtendWith(MockitoExtension.class)
class UserBanServiceTest {

  private static final int MODERATOR_ID = 123;
  private static final String BAN_ID = "Parrots grow with pestilence at the sunny madagascar!";
  private static final String USERNAME = "Aye, never taste a bilge rat.";

  private static final UserBanRecord USER_BAN_RECORD_1 =
      UserBanRecord.builder()
          .systemId("Suns stutter from madness like addled comrades.")
          .publicBanId("Ah, never endure a mast.")
          .banExpiry(Instant.now().plus(1, ChronoUnit.DAYS))
          .dateCreated(Instant.now())
          .ip("33.99.99.99")
          .username("How old. You drink like a jolly roger.")
          .build();
  private static final UserBanRecord USER_BAN_RECORD_2 =
      UserBanRecord.builder()
          .systemId("")
          .publicBanId("")
          .banExpiry(Instant.now().plus(2, ChronoUnit.DAYS))
          .dateCreated(Instant.now().minus(2, ChronoUnit.HOURS))
          .ip("55.99.99.99")
          .username("The buccaneer desires with love, break the bahamas until it travels.")
          .build();

  private static final UserBanParams USER_BAN_PARAMS =
      UserBanParams.builder()
          .username(USERNAME)
          .systemId("Love ho! fight to be desired.")
          .ip("99.99.00.99")
          .minutesToBan(20)
          .build();

  @Mock private ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  @Mock private UserBanDao userBanDao;
  @Mock private Supplier<String> publicIdSupplier;
  @Mock private Chatters chatters;

  @SuppressWarnings("unused") // injected into UserBanService
  @Mock
  private PlayerApiKeyDaoWrapper apiKeyDaoWrapper;

  @Mock private WebSocketMessagingBus chatMessagingBus;
  @Mock private WebSocketMessagingBus gameMessagingBus;

  private UserBanService bannedUsersService;

  @BeforeEach
  void setup() {
    bannedUsersService =
        new UserBanService(
            moderatorAuditHistoryDao,
            userBanDao,
            publicIdSupplier,
            chatters,
            apiKeyDaoWrapper,
            chatMessagingBus,
            gameMessagingBus);
  }

  @Test
  void getBannedUsers() {
    when(userBanDao.lookupBans()).thenReturn(List.of(USER_BAN_RECORD_1, USER_BAN_RECORD_2));

    final List<UserBanData> result = bannedUsersService.getBannedUsers();

    assertThat(result, IsCollectionWithSize.hasSize(2));

    assertThat(result.get(0).getBanDate(), is(USER_BAN_RECORD_1.getDateCreated().toEpochMilli()));
    assertThat(result.get(0).getBanExpiry(), is(USER_BAN_RECORD_1.getBanExpiry().toEpochMilli()));
    assertThat(result.get(0).getBanId(), is(USER_BAN_RECORD_1.getPublicBanId()));
    assertThat(result.get(0).getHashedMac(), is(USER_BAN_RECORD_1.getSystemId()));
    assertThat(result.get(0).getIp(), is(USER_BAN_RECORD_1.getIp()));
    assertThat(result.get(0).getUsername(), is(USER_BAN_RECORD_1.getUsername()));

    assertThat(result.get(1).getBanDate(), is(USER_BAN_RECORD_2.getDateCreated().toEpochMilli()));
    assertThat(result.get(1).getBanExpiry(), is(USER_BAN_RECORD_2.getBanExpiry().toEpochMilli()));
    assertThat(result.get(1).getBanId(), is(USER_BAN_RECORD_2.getPublicBanId()));
    assertThat(result.get(1).getHashedMac(), is(USER_BAN_RECORD_2.getSystemId()));
    assertThat(result.get(1).getIp(), is(USER_BAN_RECORD_2.getIp()));
    assertThat(result.get(1).getUsername(), is(USER_BAN_RECORD_2.getUsername()));
  }

  @Nested
  final class RemoveUserBanTest {

    @Test
    void removeUserBanFailureCase() {
      when(userBanDao.removeBan(BAN_ID)).thenReturn(0);

      final boolean result = bannedUsersService.removeUserBan(MODERATOR_ID, BAN_ID);

      assertThat(result, is(false));
      verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
    }

    @Test
    void removeUserBanSuccessCase() {
      when(userBanDao.removeBan(BAN_ID)).thenReturn(1);
      when(userBanDao.lookupUsernameByBanId(BAN_ID)).thenReturn(Optional.of(USERNAME));

      final boolean result = bannedUsersService.removeUserBan(MODERATOR_ID, BAN_ID);

      assertThat(result, is(true));
      verify(moderatorAuditHistoryDao)
          .addAuditRecord(
              ModeratorAuditHistoryDao.AuditArgs.builder()
                  .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_USER_BAN)
                  .actionTarget(USERNAME)
                  .moderatorUserId(MODERATOR_ID)
                  .build());
    }
  }

  @Test
  void banUserFailureCase() {
    givenBanDaoUpdateCount(0);

    assertThrows(
        IllegalStateException.class,
        () -> bannedUsersService.banUser(MODERATOR_ID, USER_BAN_PARAMS));
  }

  private void givenBanDaoUpdateCount(final int updateCount) {
    when(publicIdSupplier.get()).thenReturn(BAN_ID);
    when(userBanDao.addBan(
            BAN_ID,
            USER_BAN_PARAMS.getUsername(),
            USER_BAN_PARAMS.getSystemId(),
            USER_BAN_PARAMS.getIp(),
            USER_BAN_PARAMS.getMinutesToBan()))
        .thenReturn(updateCount);
  }

  @Test
  void banUserSuccessCase() {
    givenBanDaoUpdateCount(1);
    when(chatters.disconnectIp(eq(IpAddressParser.fromString(USER_BAN_PARAMS.getIp())), any()))
        .thenReturn(true);

    bannedUsersService.banUser(MODERATOR_ID, USER_BAN_PARAMS);
    verify(moderatorAuditHistoryDao)
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USER)
                .actionTarget(USERNAME + " " + USER_BAN_PARAMS.getMinutesToBan() + " minutes")
                .moderatorUserId(MODERATOR_ID)
                .build());

    verify(chatMessagingBus).broadcastMessage(any());
    verify(gameMessagingBus).broadcastMessage(any());
  }
}
