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
      "select\n"
          + AccessLogRecord.ACCESS_TIME_COLUMN
          + ", "
          + AccessLogRecord.USERNAME_COLUMN
          + ", "
          + AccessLogRecord.IP_COLUMN
          + ", "
          + AccessLogRecord.SYSTEM_ID_COLUMN
          + ", "
          + AccessLogRecord.REGISTERED_COLUMN
          + "\n"
          + "from access_log\n"
          + "order by "
          + AccessLogRecord.ACCESS_TIME_COLUMN
          + " desc\n"
          + "offset :rowOffset rows\n"
          + "fetch next :rowCount rows only")
  List<AccessLogRecord> fetchAccessLogRows(
      @Bind("rowOffset") int rowOffset, @Bind("rowCount") int rowCount);

  @SqlUpdate(
      "insert into access_log(username, ip, system_id, registered)\n"
          + "values(:username, :ip::inet, :systemId, true)")
  int insertRegisteredUserRecord(
      @Bind("username") String username, @Bind("ip") String ip, @Bind("systemId") String systemId);

  @SqlUpdate(
      "insert into access_log(username, ip, system_id, registered)\n"
          + "values(:username, :ip::inet, :systemId, false)")
  int insertAnonymousUserRecord(
      @Bind("username") String username, @Bind("ip") String ip, @Bind("systemId") String systemId);
}
