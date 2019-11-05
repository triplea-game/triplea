package org.triplea.lobby.server.db.dao.user.ban;

import static org.triplea.lobby.server.db.dao.user.ban.UserBanRecord.BAN_EXPIRY_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.UserBanRecord.DATE_CREATED_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.UserBanRecord.IP_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.UserBanRecord.PUBLIC_ID_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.UserBanRecord.SYSTEM_ID_COLUMN;
import static org.triplea.lobby.server.db.dao.user.ban.UserBanRecord.USERNAME_COLUMN;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** DAO for managing user bans (CRUD operations). */
public interface UserBanDao {

  @SqlQuery(
      "select "
          + PUBLIC_ID_COLUMN
          + ", "
          + USERNAME_COLUMN
          + ", "
          + SYSTEM_ID_COLUMN
          + ", "
          + IP_COLUMN
          + ", "
          + BAN_EXPIRY_COLUMN
          + ", "
          + DATE_CREATED_COLUMN
          + "\n"
          + "from banned_user\n"
          + "order by "
          + DATE_CREATED_COLUMN
          + " desc")
  List<UserBanRecord> lookupBans();

  @SqlQuery("select username\n" + "from banned_user\n" + "where public_id = :banId")
  String lookupUserNameByBanId(@Bind("banId") String banId);

  @SqlUpdate("delete from banned_user\n" + "where public_id = :banId")
  int removeBan(@Bind("banId") String banId);

  @SqlUpdate(
      "insert into banned_user"
          + "(public_id, username, system_id, ip, ban_expiry) values\n"
          + "(:banId, :username, :systemId, :ip::inet, now() + :banHours * '1 hour'::interval)")
  int addBan(
      @Bind("banId") String banId,
      @Bind("username") String username,
      @Bind("systemId") String systemId,
      @Bind("ip") String ip,
      @Bind("banHours") int banHours);
}
