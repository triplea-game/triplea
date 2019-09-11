package org.triplea.lobby.server.db.dao;

public interface ApiKeyDao {
  // TODO: Project#12 - Convert this interface to use JDBI and have real SQL queries.
  void storeKey(String playerName, String key);
}
