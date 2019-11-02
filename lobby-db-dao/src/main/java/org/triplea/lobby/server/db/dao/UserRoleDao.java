package org.triplea.lobby.server.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface UserRoleDao {
  @SqlQuery("select id from user_role where name = :userRole")
  int lookupRoleId(@Bind("userRole") String userRole);
}
