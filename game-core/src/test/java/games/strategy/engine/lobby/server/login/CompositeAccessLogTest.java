package games.strategy.engine.lobby.server.login;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.engine.lobby.server.TestUserUtils;
import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.db.AccessLogDao;

@ExtendWith(MockitoExtension.class)
public final class CompositeAccessLogTest {
  @Mock
  private AccessLogDao accessLogDao;

  @InjectMocks
  private CompositeAccessLog compositeAccessLog;

  private final User user = TestUserUtils.newUser();

  @Test
  public void logFailedAuthentication_ShouldNotAddDatabaseAccessLogRecord() throws Exception {
    for (final UserType userType : UserType.values()) {
      compositeAccessLog.logFailedAuthentication(user, userType, "error message");

      verify(accessLogDao, never()).insert(any(User.class), any(UserType.class));
    }
  }

  @Test
  public void logSuccessfulAuthentication_ShouldAddDatabaseAccessLogRecord() throws Exception {
    for (final UserType userType : UserType.values()) {
      compositeAccessLog.logSuccessfulAuthentication(user, userType);

      verify(accessLogDao).insert(user, userType);
    }
  }
}
