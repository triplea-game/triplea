package games.strategy.engine.lobby.server.userDB;

/**
 * TODO: no-op for the moment. Eventually this will have configuration to write to a new datasource in parallel.
 * This will allow existing users to re-encrypt their info and migrate more easily
 */
/*
 * TODO: temp class while we migrate data sources. Eventually we should flip this to be primary, then
 * remove any non-primary sources and then simplify.
 */
public class SqlUserController implements UserDaoPrimarySecondary {
  @Override
  public boolean isPrimary() {
    return false;
  }

  @Override
  public String getPassword(String userName) {
    return "";
  }

  @Override
  public boolean doesUserExist(String userName) {
    return false;
  }

  @Override
  public void updateUser(String name, String email, String hashedPassword, boolean admin) {

  }

  @Override
  public void createUser(String name, String email, String hashedPassword, boolean admin) {

  }

  @Override
  public boolean login(String userName, String hashedPassword) {
    return false;
  }

  @Override
  public DbUser getUser(String userName) {
    return null;
  }
}
