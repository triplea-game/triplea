package games.strategy.engine.lobby.server.db;

import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import games.strategy.engine.lobby.server.userDB.DBUser;

/**
 * Interface for interacting with database user operations. For example, add user, delete user..
 */
/*
 * TODO: datasource migration note, from derby (java DB) to MySQL
 * This class is currently set up to write and read from multiple data sources.
 * This is a migration tool, to go from one data source to another. When we switch over completely
 * we can simplify and write/read to the primary datasource directly.
 * TODO: rename to: UserController
 */
public class DbUserController implements UserDao {

  private final UserDao primaryDao;
  private final UserDao secondaryDao;
  private final MigrationCounter migrationCounter;

  public DbUserController() {
    this(
        new UserController(Database::getDerbyConnection),
        new UserController(Database::getPostgresConnection),
        new MigrationCounter());
  }

  /**
   * Finds the primary dao implementation and verifies there is only one.
   * Everything else is stored as secondary, we will parallel write to secondary (ignoring errors),
   * but will only read from the primary.
   */
  @VisibleForTesting
  DbUserController(
      final UserDao primaryDao,
      final UserDao secondaryDao,
      final MigrationCounter migrationCounter) {
    this.primaryDao = primaryDao;
    this.secondaryDao = secondaryDao;
    this.migrationCounter = migrationCounter;
  }


  /**
   * @return null if the user does not exist.
   */
  @Override
  public HashedPassword getPassword(final String userName) {
    return Optional.ofNullable(secondaryDao.getPassword(userName))
        .orElseGet(() -> primaryDao.getPassword(userName));
  }

  /**
   * Similar to getPassword but with the difference that this method always
   * returns a password which was hashed using the legacy MD5Crypt algorithm.
   */
  public HashedPassword getLegacyPassword(final String userName) {
    final HashedPassword password = secondaryDao.getPassword(userName);
    if (password != null && !password.isBcrypted()) {
      return password;
    }
    return primaryDao.getPassword(userName);
  }

  @Override
  public boolean doesUserExist(final String userName) {
    return secondaryDao.doesUserExist(userName) || primaryDao.doesUserExist(userName);
  }

  @Override
  public void updateUser(final DBUser user, final HashedPassword password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());
    Preconditions.checkArgument(password.isValidSyntax());
    primaryDao.updateUser(user, password);
    secondaryDao.updateUser(user, password);
  }

  @Override
  public void updateUser(final DBUser user, final String password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());
    secondaryDao.updateUser(user, password);
  }

  /**
   * Create a user in the database.
   * If an error occured, an IllegalStateException will be thrown with a user displayable error message.
   */
  @Override
  public void createUser(final DBUser user, final HashedPassword password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());
    Preconditions.checkArgument(password.isValidSyntax());
    primaryDao.createUser(user, password);
    secondaryDao.createUser(user, password);
  }

  /**
   * Create a user in the database.
   * If an error occured, an IllegalStateException will be thrown with a user displayable error message.
   */
  @Override
  public void createUser(final DBUser user, final String password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());
    secondaryDao.createUser(user, password);
  }

  /**
   * Validate the username password, returning true if the user is able to login.
   * This has the side effect of updating the users last login time.
   */
  @Override
  public boolean login(final String userName, final HashedPassword password) {
    if (secondaryDao.login(userName, password)) {
      migrationCounter.secondaryLoginSuccess();
      return true;
    }

    if (primaryDao.login(userName, password)) {
      migrationCounter.primaryLoginSuccess();
      if (!secondaryDao.doesUserExist(userName)) {
        secondaryDao.createUser(primaryDao.getUserByName(userName), password);
      }
      return true;
    }

    migrationCounter.loginFailure();
    return false;
  }

  @Override
  public boolean login(String userName, String password) {
    if (secondaryDao.login(userName, password)) {
      migrationCounter.secondaryLoginSuccess();
      return true;
    }

    migrationCounter.loginFailure();
    return false;
  }

  /**
   * @return null if no such user.
   */
  @Override
  public DBUser getUserByName(final String userName) {
    return Optional.ofNullable(secondaryDao.getUserByName(userName))
        .orElseGet(() -> primaryDao.getUserByName(userName));
  }
}
