package org.triplea.db.dao.user.ban;

import java.util.List;
import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** DAO for managing user bans (CRUD operations). */
public interface UserBanDao {
  @SqlQuery(
      "select "
          + "    public_id,"
          + "    username,"
          + "    system_id,"
          + "    ip,"
          + "    ban_expiry,"
          + "    date_created"
          + "  from banned_user"
          + "  where ban_expiry > now()"
          + "  order by date_created desc")
  List<UserBanRecord> lookupBans();

  @SqlQuery(
      "select "
          + "    public_id,"
          + "    ban_expiry"
          + "  from banned_user"
          + "  where (ip = :ip::inet or system_id = :systemId)"
          + "    and ban_expiry > now()"
          + "  order by ban_expiry desc "
          + "  limit 1")
  Optional<BanLookupRecord> lookupBan(@Bind("ip") String ip, @Bind("systemId") String systemId);

  @SqlQuery(
      "select exists ("
          + "  select * "
          + "  from banned_user "
          + "  where ip = :ip::inet and ban_expiry > now()"
          + ")")
  boolean isBannedByIp(@Bind("ip") String ip);

  @SqlQuery(
      "select username" //
          + "  from banned_user"
          + "  where public_id = :banId")
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
