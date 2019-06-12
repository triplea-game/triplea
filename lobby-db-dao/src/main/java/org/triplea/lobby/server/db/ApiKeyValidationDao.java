package org.triplea.lobby.server.db;

import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * DAO to verify moderator api keys stored in database.
 */
public interface ApiKeyValidationDao {
  @SqlQuery("select lobby_user_id from moderator_api_key where api_key = :api_key")
  Optional<Integer> lookupModeratorIdByApiKey(@Bind("api_key") String cryptedValue);

  // TODO: test-me
  @SqlUpdate("update moderator_api_key set date_last_used = now() where api_key = :api_key")
  int recordKeyUsage(@Bind("api_key") String cryptedValue);
}
