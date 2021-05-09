package org.triplea.maps.indexing;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

interface MapIndexDao {

  /** Upserts a map indexing result into the map_index table. */
  @SqlUpdate(
      "insert into map_index(map_name, repo_url, category_id, last_commit_date)\n"
          + "values(:mapName, :mapRepoUri, 1, :lastCommitDate)\n"
          + "on conflict(repo_url)\n"
          + "do update set map_name = :mapName, last_commit_date = :lastCommitDate")
  void upsert(@BindBean MapIndexResult mapIndexResult);

  /** Deletes maps that are not in the parameter list from the map_index table. */
  @SqlUpdate("delete from map_index where repo_url not in(<mapUriList>)")
  int removeMapsNotIn(@BindList("mapUriList") List<String> mapUriList);
}
