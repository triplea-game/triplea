package org.triplea.server.moderator.toolbox.banned.users;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UserBanDao;
import org.triplea.lobby.server.db.data.UserBanDaoData;


@ExtendWith(MockitoExtension.class)
class BannedUsersServiceTest {

  private static final int MODERATOR_ID = 123;
  private static final String BAN_ID = "Parrots grow with pestilence at the sunny madagascar!";
  private static final String USERNAME = "Aye, never taste a bilge rat.";

  private static final UserBanDaoData BANNED_USER_DAO_DATA_1 =
      UserBanDaoData.builder()
          .hashedMac("Suns stutter from madnesses like addled comrades.")
          .publicBanId("Ah, never endure a mast.")
          .banExpiry(Instant.now().plus(1, ChronoUnit.DAYS))
          .dateCreated(Instant.now())
          .ip("The hornpipe sings love like a mighty cannon.")
          .username("How old. You drink like a jolly roger.")
          .build();
  private static final UserBanDaoData BANNED_USER_DAO_DATA_2 =
      UserBanDaoData.builder()
          .hashedMac("")
          .publicBanId("")
          .banExpiry(Instant.now().plus(2, ChronoUnit.DAYS))
          .dateCreated(Instant.now().minus(2, ChronoUnit.HOURS))
          .ip("Riddle, power, and strength.")
          .username("The bucaneer desires with love, break the bahamas until it travels.")
          .build();

  private static final UserBanParams BAN_USER_PARAMS = UserBanParams.builder()
      .username(USERNAME)
      .hashedMac("Love ho! fight to be desired.")
      .ip("Rainy, old pants swiftly desire a warm, lively parrot.")
      .hoursToBan(20)
      .build();


  @Mock
  private ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  @Mock
  private UserBanDao bannedUserDao;
  @Mock
  private Supplier<String> publicIdSupplier;

  @InjectMocks
  private UserBanService bannedUsersService;


  @Test
  void getBannedUsers() {
    when(bannedUserDao.lookupBans()).thenReturn(Arrays.asList(
        BANNED_USER_DAO_DATA_1, BANNED_USER_DAO_DATA_2));

    final List<UserBanData> result = bannedUsersService.getBannedUsers();

    assertThat(result, IsCollectionWithSize.hasSize(2));

    assertThat(result.get(0).getBanDate(), is(BANNED_USER_DAO_DATA_1.getDateCreated()));
    assertThat(result.get(0).getBanExpiry(), is(BANNED_USER_DAO_DATA_1.getBanExpiry()));
    assertThat(result.get(0).getBanId(), is(BANNED_USER_DAO_DATA_1.getPublicBanId()));
    assertThat(result.get(0).getHashedMac(), is(BANNED_USER_DAO_DATA_1.getHashedMac()));
    assertThat(result.get(0).getIp(), is(BANNED_USER_DAO_DATA_1.getIp()));
    assertThat(result.get(0).getUsername(), is(BANNED_USER_DAO_DATA_1.getUsername()));

    assertThat(result.get(1).getBanDate(), is(BANNED_USER_DAO_DATA_2.getDateCreated()));
    assertThat(result.get(1).getBanExpiry(), is(BANNED_USER_DAO_DATA_2.getBanExpiry()));
    assertThat(result.get(1).getBanId(), is(BANNED_USER_DAO_DATA_2.getPublicBanId()));
    assertThat(result.get(1).getHashedMac(), is(BANNED_USER_DAO_DATA_2.getHashedMac()));
    assertThat(result.get(1).getIp(), is(BANNED_USER_DAO_DATA_2.getIp()));
    assertThat(result.get(1).getUsername(), is(BANNED_USER_DAO_DATA_2.getUsername()));
  }

  @Nested
  final class RemoveUserBanTest {

    @Test
    void removeUserBanFailureCase() {
      when(bannedUserDao.removeBan(BAN_ID)).thenReturn(0);

      final boolean result = bannedUsersService.removeUserBan(MODERATOR_ID, BAN_ID);

      assertThat(result, is(false));
      verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
    }

    @Test
    void removeUserBanSuccessCase() {
      when(bannedUserDao.removeBan(BAN_ID)).thenReturn(1);
      when(bannedUserDao.lookupUserNameByBanId(BAN_ID)).thenReturn(USERNAME);

      final boolean result = bannedUsersService.removeUserBan(MODERATOR_ID, BAN_ID);

      assertThat(result, is(true));
      verify(moderatorAuditHistoryDao).addAuditRecord(
          ModeratorAuditHistoryDao.AuditArgs.builder()
              .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_USER_BAN)
              .actionTarget(USERNAME)
              .moderatorUserId(MODERATOR_ID)
              .build());
    }
  }


  @Nested
  final class BanUserTest {
    @Test
    void banUserFailureCase() {
      givenBanDaoUpdateCount(0);

      assertThat(
          bannedUsersService.banUser(MODERATOR_ID, BAN_USER_PARAMS),
          is(false));
      verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
    }

    private void givenBanDaoUpdateCount(final int updateCount) {
      when(publicIdSupplier.get()).thenReturn(BAN_ID);
      when(bannedUserDao.addBan(
          BAN_ID,
          BAN_USER_PARAMS.getUsername(),
          BAN_USER_PARAMS.getHashedMac(),
          BAN_USER_PARAMS.getIp(),
          BAN_USER_PARAMS.getHoursToBan()))
              .thenReturn(updateCount);
    }

    @Test
    void banUserSuccessCase() {
      givenBanDaoUpdateCount(1);

      assertThat(
          bannedUsersService.banUser(MODERATOR_ID, BAN_USER_PARAMS),
          is(true));
      verify(moderatorAuditHistoryDao).addAuditRecord(
          ModeratorAuditHistoryDao.AuditArgs.builder()
              .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USER)
              .actionTarget(USERNAME)
              .moderatorUserId(MODERATOR_ID)
              .build());
    }
  }



}
