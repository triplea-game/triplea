package org.triplea.lobby.server.db.dao;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** DAO interface for interacting with the badword table. Essentially provides CRUD operations. */
public interface BadWordsDao {

  @SqlQuery("select word from bad_word order by word")
  List<String> getBadWords();

  @SqlUpdate("insert into bad_word (word) values (:word)")
  int addBadWord(@Bind("word") String badWordToAdd);

  @SqlUpdate("delete from bad_word where word = :word")
  int removeBadWord(@Bind("word") String badWordToRemove);

  @SqlQuery(
      "select exists (select * from bad_word where lower(:word) like '%' || lower(word) || '%')")
  boolean containsBadWord(@Bind("word") String word);
}
