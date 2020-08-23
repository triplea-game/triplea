package org.triplea.web.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.websocket.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.user.ban.UserBanDao;

@ExtendWith(MockitoExtension.class)
class SessionBannedCheckTest {

  @Mock private UserBanDao userBanDao;

  @InjectMocks private SessionBannedCheck sessionBannedCheck;

  @Mock private Session session;

  @Test
  void notBanned() {
    givenSessionIsBanned(false);

    assertThat(sessionBannedCheck.test(session), is(false));
  }

  @Test
  void banned() {
    givenSessionIsBanned(true);

    assertThat(sessionBannedCheck.test(session), is(true));
  }

  private void givenSessionIsBanned(final boolean isBanned) {
    when(session.getUserProperties())
        .thenReturn(Map.of(InetExtractor.IP_ADDRESS_KEY, "/1.1.1.1:555"));
    when(userBanDao.isBannedByIp("1.1.1.1")).thenReturn(isBanned);
  }
}
