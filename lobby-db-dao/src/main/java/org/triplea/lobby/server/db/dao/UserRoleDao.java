package org.triplea.lobby.server.db.dao;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.triplea.lobby.server.db.data.UserRole;

public interface UserRoleDao {
  @SqlQuery("select id from user_role where name = '" + UserRole.HOST + "'")
  int lookupHostRoleId();

  @SqlQuery("select id from user_role where name = '" + UserRole.ANONYMOUS + "'")
  int lookupAnonymousRoleId();
}
