package games.strategy.engine.lobby.server.db;

import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import games.strategy.engine.lobby.server.userDB.DBUser;

/**
 * Interface for interacting with database user operations. For example, add user, delete user..
 */
/*
 * TODO: rename to: UserController
 * TODO: Lobby DB Migration - once completed, delete secondary, merge this with the implementation in DbUserController
 */
public class DbUserController implements UserDao {

  /**
   * Stores a convenience reference to the authoritative DAO implementation.
   * We use this datasource for reading, we always write to it, any errors during read/write are considered
   * critical (on the other hand secondary DAO read/write errors can be logged and ignored)
   */
  private final UserDao primaryDao;
  private final UserDao secondaryDao;
  private final MigrationCounter migrationCounter;

  public DbUserController() {
    this(
        new UserController(Database::getDerbyConnection),
        new UserController(Database::getPostgresConnection),
        new MigrationCounter());
  }

  @VisibleForTesting
  DbUserController(
      final UserDao primary,
      final UserDao secondary,
      final MigrationCounter migrationCounter) {
    primaryDao = primary;
    secondaryDao = secondary;
    this.migrationCounter = migrationCounter;
  }

  /**
   * Can be used to override and swap the primary and secondary database DAOs that we would normally use.
   */
  @VisibleForTesting
  DbUserController(final UserDao primary, final UserDao secondary) {
    this(primary, secondary, new MigrationCounter());
  }

  /**
   * @return null if the user does not exist.
   */
  @Override
  public HashedPassword getPassword(final String userName) {
    return Optional.ofNullable(secondaryDao.getPassword(userName))
        .orElse(primaryDao.getPassword(userName));
  }

  @Override
  public boolean doesUserExist(final String userName) {
    return secondaryDao.doesUserExist(userName) || primaryDao.doesUserExist(userName);
  }

  @Override
  public void updateUser(final DBUser user, final HashedPassword password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());
    secondaryDao.updateUser(user, password);

    // continue updating in the primary just in case we have to turn off the 'secondary'
    primaryDao.updateUser(user, password);
  }

  /**
   * Create a user in the database.
   * If an error occured, an IllegalStateException will be thrown with a user displayable error message.
   */
  @Override
  public void createUser(final DBUser user, final HashedPassword password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());
    primaryDao.createUser(user, password);
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
      // migrate the user data to our new (secondary) database
      secondaryDao.createUser(primaryDao.getUserByName(userName), password);
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
        .orElse(primaryDao.getUserByName(userName));
  }
}
