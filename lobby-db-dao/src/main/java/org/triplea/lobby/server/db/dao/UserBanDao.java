package org.triplea.lobby.server.db.dao;

import static org.triplea.lobby.server.db.data.UserBanDaoData.BAN_EXPIRY_COLUMN;
import static org.triplea.lobby.server.db.data.UserBanDaoData.DATE_CREATED_COLUMN;
import static org.triplea.lobby.server.db.data.UserBanDaoData.HASHED_MAC_COLUMN;
import static org.triplea.lobby.server.db.data.UserBanDaoData.IP_COLUMN;
import static org.triplea.lobby.server.db.data.UserBanDaoData.PUBLIC_ID_COLUMN;
import static org.triplea.lobby.server.db.data.UserBanDaoData.USERNAME_COLUMN;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.lobby.server.db.data.UserBanDaoData;

/**
 * DAO for managing user bans (CRUD operations).
 */
public interface UserBanDao {

  @SqlQuery("select "
      + PUBLIC_ID_COLUMN + ", "
      + USERNAME_COLUMN + ", "
      + HASHED_MAC_COLUMN + ", "
      + IP_COLUMN + ", "
      + BAN_EXPIRY_COLUMN + ", "
      + DATE_CREATED_COLUMN + "\n"
      + "from banned_user\n"
      + "order by " + DATE_CREATED_COLUMN + " desc")
  List<UserBanDaoData> lookupBans();

  @SqlQuery("select username\n"
      + "from banned_user\n"
      + "where public_id = :banId")
  String lookupUserNameByBanId(@Bind("banId") String banId);


  @SqlUpdate("delete from banned_user\n"
      + "where public_id = :banId")
  int removeBan(@Bind("banId") String banId);


  @SqlUpdate("insert into banned_user"
      + "(public_id, username, hashed_mac, ip, ban_expiry) values\n"
      + "(:banId, :username, :hashedMac, :ip::inet, now() + :banHours * '1 hour'::interval)")
  int addBan(
      @Bind("banId") String banId,
      @Bind("username") String username,
      @Bind("hashedMac") String hashedMac,
      @Bind("ip") String ip,
      @Bind("banHours") int banHours);
}
