package org.triplea.lobby.server.db.dao;

import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** Data access object for the users table. */
public interface UserJdbiDao {
  @SqlQuery("select id from lobby_user where username = :username")
  Optional<Integer> lookupUserIdByName(@Bind("username") String username);

  @SqlQuery("select bcrypt_password from lobby_user where username = :username")
  Optional<String> getPassword(@Bind("username") String username);

  @SqlUpdate("update lobby_user set last_login = now() where username = :username")
  int updateLastLoginTime(@Bind("username") String username);
}
