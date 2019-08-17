package org.triplea.server.moderator.toolbox.banned.names;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.banned.name.UsernameBanData;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UsernameBanDao;
import org.triplea.lobby.server.db.data.UsernameBanDaoData;

@ExtendWith(MockitoExtension.class)
class BannedUsernameServiceTest {

  private static final String USERNAME = "You haul like an ale.";
  private static final int MODERATOR_ID = 42352;
  private static final UsernameBanDaoData bannedUserNameDaoData =
      UsernameBanDaoData.builder()
          .dateCreated(Instant.now())
          .username("Golden, big mainlands quietly trade a stormy, warm skull.")
          .build();

  @Mock private UsernameBanDao bannedUsernamesDao;
  @Mock private ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  @InjectMocks private UsernameBanService usernameBanService;

  @Nested
  final class RemoveNameBanTest {

    @Test
    void removeFailureCase() {
      when(bannedUsernamesDao.removeBannedUserName(USERNAME)).thenReturn(0);

      assertThat(usernameBanService.removeUsernameBan(MODERATOR_ID, USERNAME), is(false));

      verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
    }

    @Test
    void removeSuccessCase() {
      when(bannedUsernamesDao.removeBannedUserName(USERNAME)).thenReturn(1);

      assertThat(usernameBanService.removeUsernameBan(MODERATOR_ID, USERNAME), is(true));

      verify(moderatorAuditHistoryDao)
          .addAuditRecord(
              ModeratorAuditHistoryDao.AuditArgs.builder()
                  .moderatorUserId(MODERATOR_ID)
                  .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_USERNAME_BAN)
                  .actionTarget(USERNAME)
                  .build());
    }
  }

  @Nested
  final class AddNameBanTest {
    @Test
    void addFailureCase() {
      when(bannedUsernamesDao.addBannedUserName(USERNAME)).thenReturn(0);

      assertThat(usernameBanService.addBannedUserName(MODERATOR_ID, USERNAME), is(false));

      verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
    }

    @Test
    void addSuccessCase() {
      when(bannedUsernamesDao.addBannedUserName(USERNAME)).thenReturn(1);

      assertThat(usernameBanService.addBannedUserName(MODERATOR_ID, USERNAME), is(true));

      verify(moderatorAuditHistoryDao)
          .addAuditRecord(
              ModeratorAuditHistoryDao.AuditArgs.builder()
                  .moderatorUserId(MODERATOR_ID)
                  .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
                  .actionTarget(USERNAME)
                  .build());
    }
  }

  @Test
  void getBannedUserNames() {
    when(bannedUsernamesDao.getBannedUserNames())
        .thenReturn(Collections.singletonList(bannedUserNameDaoData));

    final List<UsernameBanData> results = usernameBanService.getBannedUserNames();
    assertThat(results, hasSize(1));
    assertThat(results.get(0).getBanDate(), is(bannedUserNameDaoData.getDateCreated()));
    assertThat(results.get(0).getBannedName(), is(bannedUserNameDaoData.getUsername()));
  }
}
