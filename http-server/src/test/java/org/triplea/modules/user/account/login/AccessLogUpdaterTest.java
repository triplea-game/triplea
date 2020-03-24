package org.triplea.modules.user.account.login;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.access.log.AccessLogDao;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;

@ExtendWith(MockitoExtension.class)
class AccessLogUpdaterTest {

  private static final LoginRecord REGISTEREDLOGIN_RECORD =
      LoginRecord.builder()
          .registered(true)
          .systemId(SystemId.of("system-id"))
          .playerChatId(PlayerChatId.newId())
          .ip("ip")
          .userName(UserName.of("player-name"))
          .build();

  private static final LoginRecord ANONYMOUS_LOGIN_RECORD =
      LoginRecord.builder()
          .registered(false)
          .systemId(SystemId.of("system-id"))
          .playerChatId(PlayerChatId.newId())
          .ip("ip")
          .userName(UserName.of("player-name"))
          .build();

  @Mock private AccessLogDao accessLogDao;

  private AccessLogUpdater accessLogUpdater;

  @BeforeEach
  void setup() {
    accessLogUpdater = AccessLogUpdater.builder().accessLogDao(accessLogDao).build();
  }

  @Test
  void acceptRegistered() {
    when(accessLogDao.insertRegisteredUserRecord(any(), any(), any())).thenReturn(1);

    accessLogUpdater.accept(REGISTEREDLOGIN_RECORD);

    verify(accessLogDao)
        .insertRegisteredUserRecord(
            REGISTEREDLOGIN_RECORD.getUserName().getValue(),
            REGISTEREDLOGIN_RECORD.getIp(),
            REGISTEREDLOGIN_RECORD.getSystemId().getValue());
  }

  @Test
  void acceptAnonymous() {
    when(accessLogDao.insertAnonymousUserRecord(any(), any(), any())).thenReturn(1);

    accessLogUpdater.accept(ANONYMOUS_LOGIN_RECORD);

    verify(accessLogDao)
        .insertAnonymousUserRecord(
            ANONYMOUS_LOGIN_RECORD.getUserName().getValue(),
            ANONYMOUS_LOGIN_RECORD.getIp(),
            ANONYMOUS_LOGIN_RECORD.getSystemId().getValue());
  }
}
