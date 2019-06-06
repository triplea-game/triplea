package org.triplea.lobby.server.db;

import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * DAO to verify moderator api keys stored in database.
 */
public interface ApiKeyDao {
  @SqlQuery("select lobby_user_id from moderator_api_key where api_key = :api_key")
  Optional<Integer> lookupModeratorIdByApiKey(@Bind("api_key") String cryptedValue);
}
