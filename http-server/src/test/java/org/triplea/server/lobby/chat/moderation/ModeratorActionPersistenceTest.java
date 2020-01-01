package org.triplea.server.lobby.chat.moderation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao.AuditAction;
import org.triplea.lobby.server.db.dao.api.key.GamePlayerLookup;
import org.triplea.lobby.server.db.dao.user.ban.UserBanDao;

@ExtendWith(MockitoExtension.class)
class ModeratorActionPersistenceTest {

  private static final int MODERATOR_ID = 10;
  private static final GamePlayerLookup PLAYER_ID_LOOKUP =
      GamePlayerLookup.builder()
          .systemId(SystemId.of("system-id"))
          .userName(UserName.of("player-name"))
          .ip("ip")
          .build();
  private static final BanPlayerRequest BAN_PLAYER_REQUEST =
      BanPlayerRequest.builder().banMinutes(30).playerChatId("player-chat-id").build();

  @Mock private UserBanDao userBanDao;
  @Mock private ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  @InjectMocks private ModeratorActionPersistence moderatorActionPersistence;

  @Test
  void recordBan() {
    moderatorActionPersistence.recordBan(MODERATOR_ID, PLAYER_ID_LOOKUP, BAN_PLAYER_REQUEST);

    verify(userBanDao)
        .addBan(
            anyString(),
            eq(PLAYER_ID_LOOKUP.getUserName().getValue()),
            eq(PLAYER_ID_LOOKUP.getSystemId().getValue()),
            eq(PLAYER_ID_LOOKUP.getIp()),
            eq(BAN_PLAYER_REQUEST.getBanMinutes()));

    verify(moderatorAuditHistoryDao)
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(MODERATOR_ID)
                .actionName(AuditAction.BAN_USER)
                .actionTarget("player-name 30 minutes")
                .build());
  }

  @Test
  void recordPlayerDisconnect() {
    moderatorActionPersistence.recordPlayerDisconnect(MODERATOR_ID, PLAYER_ID_LOOKUP);

    verify(moderatorAuditHistoryDao)
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(MODERATOR_ID)
                .actionName(AuditAction.DISCONNECT_USER)
                .actionTarget(PLAYER_ID_LOOKUP.getUserName().getValue())
                .build());
  }
}
