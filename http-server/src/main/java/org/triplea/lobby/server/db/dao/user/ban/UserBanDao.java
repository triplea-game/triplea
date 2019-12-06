package org.triplea.lobby.server.db.dao.user.ban;

import static org.triplea.lobby.server.db.dao.user.ban.BanTableColumns.BAN_EXPIRY_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.BanTableColumns.DATE_CREATED_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.BanTableColumns.IP_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.BanTableColumns.PUBLIC_ID_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.BanTableColumns.SYSTEM_ID_COLUMN;

import java.util.List;
import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** DAO for managing user bans (CRUD operations). */
public interface UserBanDao {
  // TODO: Project#11 use paging
  @SqlQuery(
      "select "
          + UserBanRecord.SELECT_CLAUSE
          + "\n"
          + "from banned_user\n"
          + "where "
          + BAN_EXPIRY_COLUMN
          + " > now()\n"
          + "order by "
          + DATE_CREATED_COLUMN
          + " desc")
  List<UserBanRecord> lookupBans();

  // TODO: Project#12 convert select to be an update to count matches against system id vs IP
  // TODO: test that expired bans are not returned when IP address or system id does match.
  @SqlQuery(
      "select "
          + PUBLIC_ID_COLUMN
          + ", "
          + BAN_EXPIRY_COLUMN
          + "\n"
          + "from banned_user\n"
          + "where (\n"
          + IP_COLUMN
          + " = :ip::inet"
          + " or "
          + SYSTEM_ID_COLUMN
          + " = :systemId"
          + ") and "
          + BAN_EXPIRY_COLUMN
          + " > now() "
          + "order by "
          + BAN_EXPIRY_COLUMN
          + " desc limit 1")
  Optional<BanLookupRecord> lookupBan(@Bind("ip") String ip, @Bind("systemId") String systemId);

  // TODO: test-me
  @SqlQuery(
      "select exists (select * from banned_user "
          + "where \n"
          + IP_COLUMN
          + " = :ip::inet"
          + " and "
          + BAN_EXPIRY_COLUMN
          + " > now())")
  boolean isBannedByIp(@Bind("ip") String ip);

  @SqlQuery(
      "select username\n" //
          + "from banned_user\n" //
          + "where public_id = :banId")
  Optional<String> lookupUsernameByBanId(@Bind("banId") String banId);

  @SqlUpdate("delete from banned_user where public_id = :banId")
  int removeBan(@Bind("banId") String banId);

  @SqlUpdate(
      "insert into banned_user"
          + "(public_id, username, system_id, ip, ban_expiry) values\n"
          + "(:banId, :username, :systemId, :ip::inet, now() + :banMinutes * '1 minute'::interval)")
  int addBan(
      @Bind("banId") String banId,
      @Bind("username") String username,
      @Bind("systemId") String systemId,
      @Bind("ip") String ip,
      @Bind("banMinutes") long banMinutes);
}
