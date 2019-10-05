package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

/** Utility class to create/read/delete banned macs (there is no update). */
@AllArgsConstructor
class BannedMacController implements UserBanDao {

  private final Supplier<Connection> connection;
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  private final UserJdbiDao userDao;

  @Override
  public void banUser(
      final User bannedUser, final @Nullable Instant banTill, final User moderator) {
    checkNotNull(bannedUser);
    checkNotNull(moderator);

    final String sql =
        ""
            + "insert into banned_user "
            + "  (public_id, username, hashed_mac, ip, ban_expiry)"
            + "values (?, ?, ?, ?::inet, ?)";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(
          1, Instant.now().getEpochSecond() + String.valueOf(Math.random()).substring(0, 5));
      ps.setString(2, bannedUser.getUsername());
      ps.setString(3, bannedUser.getHashedMacAddress());
      ps.setString(4, bannedUser.getInetAddress().getHostAddress());
      ps.setTimestamp(
          5,
          banTill != null
              ? Timestamp.from(banTill)
              : Timestamp.from(Instant.now().plus(30, ChronoUnit.DAYS)));
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException(
          "Error inserting banned mac: " + bannedUser.getHashedMacAddress(), e);
    }
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(
                userDao
                    .lookupUserIdByName(moderator.getUsername())
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "Failed to find user: " + moderator.getUsername())))
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_MAC)
            .actionTarget(bannedUser.getHashedMacAddress())
            .build());
  }

  @Override
  public Optional<Timestamp> isBanned(final InetAddress ipAddress, final String mac) {
    final String sql = "select ban_expiry from banned_user where hashed_mac=? or ip =?::inet";

    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, mac);
      ps.setString(2, ipAddress.getHostAddress());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(rs.getTimestamp(1)) : Optional.empty();
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error for testing banned mac existence: " + mac, e);
    }
  }
}
