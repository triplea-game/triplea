package org.triplea.lobby.server.db.dao;

import static org.triplea.lobby.server.db.data.AccessLogDaoData.ACCESS_TIME_COLUMN;
import static org.triplea.lobby.server.db.data.AccessLogDaoData.IP_COLUMN;
import static org.triplea.lobby.server.db.data.AccessLogDaoData.REGISTERED_COLUMN;
import static org.triplea.lobby.server.db.data.AccessLogDaoData.SYSTEM_ID_COLUMN;
import static org.triplea.lobby.server.db.data.AccessLogDaoData.USERNAME_COLUMN;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.triplea.lobby.server.db.data.AccessLogDaoData;

/**
 * Provides access to the access log table. This is a table that records user data as they enter the
 * lobby. Useful for statistics and for banning.
 */
public interface AccessLogDao {

  @SqlQuery(
      "select\n"
          + ACCESS_TIME_COLUMN
          + ", "
          + USERNAME_COLUMN
          + ", "
          + IP_COLUMN
          + ", "
          + SYSTEM_ID_COLUMN
          + ", "
          + REGISTERED_COLUMN
          + "\n"
          + "from access_log\n"
          + "order by "
          + ACCESS_TIME_COLUMN
          + " desc\n"
          + "offset :rowOffset rows\n"
          + "fetch next :rowCount rows only")
  List<AccessLogDaoData> lookupAccessLogData(
      @Bind("rowOffset") int rowOffset, @Bind("rowCount") int rowCount);
}
