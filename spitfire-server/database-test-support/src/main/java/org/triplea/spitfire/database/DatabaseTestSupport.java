package org.triplea.spitfire.database;

public abstract class DatabaseTestSupport extends DbRiderTestExtension {
  @Override
  protected String getDatabaseUser() {
    return "lobby_user";
  }

  @Override
  protected String getDatabasePassword() {
    return "lobby";
  }

  @Override
  protected String getDatabaseUrl() {
    return "jdbc:postgresql://localhost:5432/lobby_db";
  }
}
