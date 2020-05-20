package org.triplea.db.dao.access.log;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * Provides access to the access log table. This is a table that records user data as they enter the
 * lobby. Useful for statistics and for banning.
 */
public interface AccessLogDao {

  @SqlQuery(
      "select"
          + "    access_time,"
          + "    username,"
          + "    ip,"
          + "    system_id,"
          + "    (lobby_user_id is not null) as registered"
          + "  from access_log"
          + "  where username like :username"
          + "     and host(ip) like :ip"
          + "     and system_id like :systemId"
          + "  order by access_time desc"
          + "  offset :rowOffset rows"
          + "  fetch next :rowCount rows only")
  List<AccessLogRecord> fetchAccessLogRows(
      @Bind("rowOffset") int rowOffset,
      @Bind("rowCount") int rowCount,
      @Bind("username") String username,
      @Bind("ip") String ip,
      @Bind("systemId") String systemId);

  @SqlUpdate(
      "insert into access_log(username, ip, system_id, lobby_user_id)\n"
          + "values ("
          + "  :username,"
          + "  :ip::inet,"
          + "  :systemId,"
          + "  (select id from lobby_user where username = :username))")
  int insertUserAccessRecord(
      @Bind("username") String username, @Bind("ip") String ip, @Bind("systemId") String systemId);
}
