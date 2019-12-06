package org.triplea.lobby.server.db.dao.api.key;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

interface GameHostingApiKeyDao {
  @SqlUpdate("insert into game_hosting_api_key(key, ip) values(:key, :ip::inet)")
  int insertKey(@Bind("key") String key, @Bind("ip") String ip);

  @SqlQuery("select exists (select * from game_hosting_api_key where key = :apiKey)")
  boolean keyExists(@Bind("apiKey") String apiKey);
}
