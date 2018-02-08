package games.strategy.engine.lobby.server.db;

import static games.strategy.engine.lobby.server.db.LoginMetricsController.setUtcDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import javax.annotation.concurrent.Immutable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.example.mockito.MockitoExtension;

import games.strategy.engine.lobby.server.login.LoginType;

@ExtendWith(MockitoExtension.class)
public final class LoginMetricsControllerIntegrationTest {
  // NB: use an instant that will cross dates when converted to UTC to detect implementation errors
  private static final Instant LOGIN_INSTANT =
      DateTimeFormatter.ISO_DATE_TIME.parse("2011-12-03T23:59:00.00-05:00", Instant::from);

  @Mock
  private Clock clock;

  @InjectMocks
  private LoginMetricsController loginMetricsController;

  @BeforeEach
  public void setupClock() {
    when(clock.instant()).thenReturn(LOGIN_INSTANT);
  }

  @Nested
  public final class WhenLoginTypeIsAnonymousTest {
    @Test
    public void shouldRecordOneAnonymousLoginWhenLoginDateDoesNotExist() throws Exception {
      givenNoLoginMetrics();

      whenAddSuccessfulLogin();

      thenLoginMetricsShouldBe(LoginMetrics.empty().withAnonymousLogins(1));
    }

    @Test
    public void shouldIncrementAnonymousLoginCountWhenLoginDateExists() throws Exception {
      final LoginMetrics loginMetrics = LoginMetrics.empty().withAnonymousLogins(100).withRegisteredLogins(200);
      givenLoginMetrics(loginMetrics);

      whenAddSuccessfulLogin();

      thenLoginMetricsShouldBe(loginMetrics.withAnonymousLogins(loginMetrics.anonymousLogins + 1));
    }

    private void whenAddSuccessfulLogin() throws Exception {
      loginMetricsController.addSuccessfulLogin(LoginType.ANONYMOUS);
    }
  }

  @Nested
  public final class WhenLoginTypeIsRegisteredTest {
    @Test
    public void shouldRecordOneRegisteredLoginWhenLoginDateDoesNotExist() throws Exception {
      givenNoLoginMetrics();

      whenAddSuccessfulLogin();

      thenLoginMetricsShouldBe(LoginMetrics.empty().withRegisteredLogins(1));
    }

    @Test
    public void shouldIncrementRegisteredLoginCountWhenLoginDateExists() throws Exception {
      final LoginMetrics loginMetrics = LoginMetrics.empty().withAnonymousLogins(300).withRegisteredLogins(400);
      givenLoginMetrics(loginMetrics);

      whenAddSuccessfulLogin();

      thenLoginMetricsShouldBe(loginMetrics.withRegisteredLogins(loginMetrics.registeredLogins + 1));
    }

    private void whenAddSuccessfulLogin() throws Exception {
      loginMetricsController.addSuccessfulLogin(LoginType.REGISTERED);
    }
  }

  private static void givenNoLoginMetrics() throws Exception {
    final String sql = "delete from login_metrics where login_date = ?";
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setUtcDate(ps, 1, LOGIN_INSTANT);
      ps.executeUpdate();
      conn.commit();
    }
  }

  private static void givenLoginMetrics(final LoginMetrics loginMetrics) throws Exception {
    final String sql = ""
        + "insert into login_metrics "
        + "  (login_date, anonymous_logins, registered_logins) values (?, ?, ?) "
        + "on conflict (login_date) do update set "
        + "  anonymous_logins=excluded.anonymous_logins, "
        + "  registered_logins=excluded.registered_logins";
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setUtcDate(ps, 1, LOGIN_INSTANT);
      ps.setInt(2, loginMetrics.anonymousLogins);
      ps.setInt(3, loginMetrics.registeredLogins);
      ps.executeUpdate();
      conn.commit();
    }
  }

  private static void thenLoginMetricsShouldBe(final LoginMetrics loginMetrics) throws Exception {
    final String sql = "select anonymous_logins, registered_logins from login_metrics where login_date = ?";
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setUtcDate(ps, 1, LOGIN_INSTANT);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertThat(rs.getInt(1), is(loginMetrics.anonymousLogins));
          assertThat(rs.getInt(2), is(loginMetrics.registeredLogins));
        } else {
          fail("no login metrics for " + LOGIN_INSTANT);
        }
      }
    }
  }

  @Immutable
  private static final class LoginMetrics {
    final int anonymousLogins;
    final int registeredLogins;

    private LoginMetrics(final int anonymousLogins, final int registeredLogins) {
      this.anonymousLogins = anonymousLogins;
      this.registeredLogins = registeredLogins;
    }

    static LoginMetrics empty() {
      return new LoginMetrics(0, 0);
    }

    LoginMetrics withAnonymousLogins(final int anonymousLogins) {
      return new LoginMetrics(anonymousLogins, registeredLogins);
    }

    LoginMetrics withRegisteredLogins(final int registeredLogins) {
      return new LoginMetrics(anonymousLogins, registeredLogins);
    }
  }
}
