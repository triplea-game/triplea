package games.strategy.engine.lobby.server.login;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.example.mockito.MockitoExtension;

import games.strategy.engine.lobby.server.TestUserUtils;
import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.db.AccessLogDao;

@ExtendWith(MockitoExtension.class)
public final class CompositeAccessLogTest {
  @Mock
  private AccessLogDao accessLogDao;

  @InjectMocks
  private CompositeAccessLog compositeAccessLog;

  private final Instant instant = Instant.now();

  private final User user = TestUserUtils.newUser();

  @Test
  public void logFailedAuthentication_ShouldNotAddDatabaseAccessLogRecord() throws Exception {
    for (final AuthenticationType authenticationType : AuthenticationType.values()) {
      compositeAccessLog.logFailedAuthentication(instant, user, authenticationType, "error message");

      verify(accessLogDao, never()).insert(any(Instant.class), any(User.class), anyBoolean());
    }
  }

  @Test
  public void logSuccessfulAuthentication_ShouldAddDatabaseAccessLogRecord() throws Exception {
    for (final AuthenticationType authenticationType : AuthenticationType.values()) {
      compositeAccessLog.logSuccessfulAuthentication(instant, user, authenticationType);

      verify(accessLogDao).insert(instant, user, authenticationType == AuthenticationType.REGISTERED);
    }
  }
}
