package org.triplea.db.dao.user.history;

import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/** DAO to look up a players stats. Intended to be used when getting player information. */
public interface PlayerHistoryDao {

  @SqlQuery(
      "select"
          + "    date_created as date_registered"
          + "  from lobby_user"
          + "  where id = :userId")
  Optional<PlayerHistoryRecord> lookupPlayerHistoryByUserId(@Bind("userId") int userId);
}
