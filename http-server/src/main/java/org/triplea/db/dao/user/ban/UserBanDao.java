package org.triplea.db.dao.user.ban;

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
          + BanTableColumns.BAN_EXPIRY_COLUMN
          + " > now()\n"
          + "order by "
          + BanTableColumns.DATE_CREATED_COLUMN
          + " desc")
  List<UserBanRecord> lookupBans();

  @SqlQuery(
      "select "
          + BanTableColumns.PUBLIC_ID_COLUMN
          + ", "
          + BanTableColumns.BAN_EXPIRY_COLUMN
          + "\n"
          + "from banned_user\n"
          + "where (\n"
          + BanTableColumns.IP_COLUMN
          + " = :ip::inet"
          + " or "
          + BanTableColumns.SYSTEM_ID_COLUMN
          + " = :systemId"
          + ") and "
          + BanTableColumns.BAN_EXPIRY_COLUMN
          + " > now() "
          + "order by "
          + BanTableColumns.BAN_EXPIRY_COLUMN
          + " desc limit 1")
  Optional<BanLookupRecord> lookupBan(@Bind("ip") String ip, @Bind("systemId") String systemId);

  @SqlQuery(
      "select exists (select * from banned_user "
          + "where \n"
          + BanTableColumns.IP_COLUMN
          + " = :ip::inet"
          + " and "
          + BanTableColumns.BAN_EXPIRY_COLUMN
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
