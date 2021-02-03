package org.triplea.maps.indexing;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

interface MapIndexDao {

  @SqlUpdate(
      "insert into map_index(map_name, repo_url, category_id, version)\n"
          + "values(:mapName, :mapRepoUri, 1, :mapVersion)\n"
          + "on conflict(repo_url)\n"
          + "do update set map_name = :mapName, version = :mapVersion")
  void upsert(@BindBean MapIndexResult mapIndexResult);

  @SqlUpdate("delete from map_index where repo_url not in(<mapUriList>)")
  int removeMapsNotIn(@BindList("mapUriList") List<String> mapUriList);
}

