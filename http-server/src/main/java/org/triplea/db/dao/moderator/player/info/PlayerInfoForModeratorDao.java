package org.triplea.db.dao.moderator.player.info;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * DAO used to lookup player correlation information for moderators. Answers questions such as
 *
 * <ul>
 *   <li>"how many times and was this player banned?"
 *   <li>"which other names were used by this same IP and system-id? (presumably the same player)"
 * </ul>
 */
public interface PlayerInfoForModeratorDao {
  @SqlQuery(
      "select distinct"
          + "    username as name,"
          + "    ip as ip,"
          + "    system_id as systemId,"
          + "    max(access_time) as accessTime"
          + "  from access_log"
          + "  where "
          + "    access_time > (now() - '14 day'::interval)"
          + "    and ("
          + "      ip = :ip::inet"
          + "      or system_id = :systemId"
          + "    )"
          + "  group by name, ip, systemId"
          + "  order by accessTime desc")
  List<PlayerAliasRecord> lookupPlayerAliasRecords(
      @Bind("systemId") String systemId, @Bind("ip") String ip);

  @SqlQuery(
      "select"
          + "    username as name,"
          + "    ip as ip,"
          + "    system_id as systemId,"
          + "    date_created as banStart,"
          + "    ban_expiry as banEnd"
          + "  from banned_user"
          + "  where "
          + "    ip = :ip::inet"
          + "    or system_id = :systemId"
          + "  order by ban_expiry desc")
  List<PlayerBanRecord> lookupPlayerBanRecords(
      @Bind("systemId") String systemId, @Bind("ip") String ip);
}
