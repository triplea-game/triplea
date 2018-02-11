package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.lobby.server.login.LoginType;

/**
 * Gateway for the login metrics table.
 */
public final class LoginMetricsController implements LoginMetricsDao {
  private final Clock clock;

  public LoginMetricsController() {
    this(Clock.systemUTC());
  }

  @VisibleForTesting
  LoginMetricsController(final Clock clock) {
    assert ZoneOffset.UTC.equals(clock.getZone());

    this.clock = clock;
  }

  @Override
  public void addSuccessfulLogin(final LoginType loginType) throws SQLException {
    final String sql = ""
        + "insert into login_metrics "
        + "  (login_date, anonymous_logins, registered_logins) values (?, ?, ?) "
        + "on conflict (login_date) do update set "
        + "  anonymous_logins=login_metrics.anonymous_logins + excluded.anonymous_logins, "
        + "  registered_logins=login_metrics.registered_logins + excluded.registered_logins";
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setUtcDate(ps, 1, LocalDate.now(clock));
      ps.setInt(2, (loginType == LoginType.ANONYMOUS) ? 1 : 0);
      ps.setInt(3, (loginType == LoginType.REGISTERED) ? 1 : 0);
      ps.execute();
      conn.commit();
    }
  }

  @VisibleForTesting
  static void setUtcDate(final PreparedStatement ps, final int parameterIndex, final LocalDate localDate)
      throws SQLException {
    ps.setDate(parameterIndex, Date.valueOf(localDate), Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC)));
  }
}
