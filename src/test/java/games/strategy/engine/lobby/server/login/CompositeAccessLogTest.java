package games.strategy.engine.lobby.server.login;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;

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
  public void logFailedAccess_ShouldNotAddDatabaseAccessLogRecord() {
    Arrays.stream(AccessMethod.values()).forEach(accessMethod -> {
      compositeAccessLog.logFailedAccess(instant, user, accessMethod, "error message");

      thenShouldNotAddDatabaseAccessLogRecord();
    });
  }

  @Test
  public void logSuccessfulAccess_ShouldAddDatabaseAccessLogRecord() {
    Arrays.stream(AccessMethod.values()).forEach(accessMethod -> {
      compositeAccessLog.logSuccessfulAccess(instant, user, accessMethod);

      thenShouldAddDatabaseAccessLogRecordFor(accessMethod);
    });
  }

  private void thenShouldNotAddDatabaseAccessLogRecord() {
    try {
      verify(accessLogDao, never()).insert(any(Instant.class), any(User.class), anyBoolean());
    } catch (final SQLException e) {
      fail("unexpected exception", e);
    }
  }

  private void thenShouldAddDatabaseAccessLogRecordFor(final AccessMethod accessMethod) {
    try {
      verify(accessLogDao).insert(instant, user, accessMethod == AccessMethod.AUTHENTICATION);
    } catch (final SQLException e) {
      fail("unexpected exception", e);
    }
  }
}
