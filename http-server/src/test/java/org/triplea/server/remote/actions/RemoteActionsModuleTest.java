package org.triplea.server.remote.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.IpAddressParser;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao.AuditAction;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao.AuditArgs;
import org.triplea.lobby.server.db.dao.user.ban.UserBanDao;

@ExtendWith(MockitoExtension.class)
class RemoteActionsModuleTest {

  private static final String IP = "99.55.33.11";
  private static final int MODERATOR_ID = 300;

  @Mock private UserBanDao userBanDao;
  @Mock private ModeratorAuditHistoryDao auditHistoryDao;
  @Mock private RemoteActionsEventQueue remoteActionsEventQueue;

  private RemoteActionsModule remoteActionsModule;

  @BeforeEach
  void setup() {
    remoteActionsModule =
        RemoteActionsModule.builder()
            .userBanDao(userBanDao)
            .auditHistoryDao(auditHistoryDao)
            .remoteActionsEventQueue(remoteActionsEventQueue)
            .build();
  }

  @Nested
  class IsUserBanned {
    @Test
    void userIsBanned() {
      when(userBanDao.isBannedByIp(IP)).thenReturn(true);

      final boolean result = remoteActionsModule.isUserBanned(IpAddressParser.fromString(IP));

      assertThat(result, is(true));
    }

    @Test
    void userIsNotBanned() {
      when(userBanDao.isBannedByIp(IP)).thenReturn(false);

      final boolean result = remoteActionsModule.isUserBanned(IpAddressParser.fromString(IP));

      assertThat(result, is(false));
    }
  }

  @Nested
  class AddIpForShutdown {
    @Test
    void addIpforShutdown() {
      remoteActionsModule.addIpForShutdown(MODERATOR_ID, IpAddressParser.fromString(IP));

      verify(remoteActionsEventQueue).addShutdownRequestEvent(IpAddressParser.fromString(IP));
      final ArgumentCaptor<AuditArgs> capture = ArgumentCaptor.forClass(AuditArgs.class);
      verify(auditHistoryDao).addAuditRecord(capture.capture());
      assertThat(
          capture.getValue(),
          is(
              AuditArgs.builder()
                  .actionName(AuditAction.REMOTE_SHUTDOWN)
                  .actionTarget(IP)
                  .moderatorUserId(MODERATOR_ID)
                  .build()));
    }
  }
}
