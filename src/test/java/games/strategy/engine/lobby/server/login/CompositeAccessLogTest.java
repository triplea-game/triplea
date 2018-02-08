package games.strategy.engine.lobby.server.login;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.example.mockito.MockitoExtension;

import games.strategy.engine.lobby.server.db.LoginMetricsDao;

@ExtendWith(MockitoExtension.class)
public final class CompositeAccessLogTest {
  @Mock
  private LoginMetricsDao loginMetricsDao;

  @InjectMocks
  private CompositeAccessLog compositeAccessLog;

  @Test
  public void logFailedLogin_ShouldNotAddLoginRecord() {
    Arrays.stream(LoginType.values()).forEach(loginType -> {
      compositeAccessLog.logFailedLogin(loginType, "username", InetAddress.getLoopbackAddress(), "error");

      thenShouldNotAddLoginRecord();
    });
  }

  @Test
  public void logSuccessfulLogin_ShouldAddLoginRecord() {
    Arrays.stream(LoginType.values()).forEach(loginType -> {
      compositeAccessLog.logSuccessfulLogin(loginType, "username", InetAddress.getLoopbackAddress());

      thenShouldAddLoginRecordForType(loginType);
    });
  }

  private void thenShouldNotAddLoginRecord() {
    try {
      verify(loginMetricsDao, never()).addSuccessfulLogin(any(LoginType.class));
    } catch (final SQLException e) {
      fail("unexpected exception", e);
    }
  }

  private void thenShouldAddLoginRecordForType(final LoginType loginType) {
    try {
      verify(loginMetricsDao).addSuccessfulLogin(loginType);
    } catch (final SQLException e) {
      fail("unexpected exception", e);
    }
  }
}
