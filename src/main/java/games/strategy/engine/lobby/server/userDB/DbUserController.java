package games.strategy.engine.lobby.server.userDB;

import static games.strategy.util.PredicateUtils.not;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Interface for interacting with database user operations. For example, add user, delete user..
 */
/*
 * TODO: datasource migration note, from derby (java DB) to MySQL
 *  This class is currently set up to write and read from multiple data sources.
 *  This is a migration tool, to go from one data source to another. When we switch over completely
 *  we can simplify and write/read to the primary datasource directly.
 * TODO: rename to: UserController
 */
public class DbUserController implements UserDao {
  private static final Logger logger = Logger.getLogger(DbUserController.class.getName());

  /**
   * Stores a convenience reference to the authoritative DAO implementation.
   * We use this datasource for reading, we always write to it, any errors during read/write are considered
   * critical (on the other hand secondary DAO read/write errors can be logged and ignored)
   */
  private final UserDao primaryDao;
  private final Collection<UserDao> secondaryDaoSet;


  /**
   * The default constructor uses a production configuration.
   */
  public DbUserController() {
    this(
        new DerbyUserController(
            UserDaoPrimarySecondary.Role.PRIMARY,
            Database::getDerbyConnection),
        new DerbyUserController(
            UserDaoPrimarySecondary.Role.SECONDARY,
            Database::getPostgresConnection));
  }

  /**
   * Finds the primary dao implementation and verifies there is only one.
   * Everything else is stored as secondary, we will parallel write to secondary (ignoring errors),
   * but will only read from the primary.
   */
  @VisibleForTesting DbUserController(UserDaoPrimarySecondary ... userDaos) {
    final List<UserDao> primaryDaoList = Arrays.stream(userDaos)
        .filter(UserDaoPrimarySecondary::isPrimary)
        .collect(Collectors.toList());
    Preconditions.checkState(
        primaryDaoList.size() == 1,
        "Startup Assumption - exactly one UserDao implementation is 'primary'");
    primaryDao = primaryDaoList.get(0);

    secondaryDaoSet = Arrays.stream(userDaos)
        .filter(not(UserDaoPrimarySecondary::isPrimary))
        .collect(Collectors.toSet());
  }


  /**
   * @return null if the user does not exist.
   */
  @Override
  public HashedPassword getPassword(final String userName) {
    return primaryDao.getPassword(userName);
  }

  @Override
  public boolean doesUserExist(final String userName) {
    return primaryDao.doesUserExist(userName);
  }

  @Override
  public void updateUser(final DbUser user, final HashedPassword password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());
    primaryDao.updateUser(user, password);
    try {
      new Thread(() -> secondaryDaoSet.forEach(dao -> dao.updateUser(user, password))).start();
    } catch (Exception e) {
      logger.warning(
          "Secondary datasource failure, this is okay if we are still setting up a new datasource. " + e.getMessage());
    }
  }

  /**
   * Create a user in the database.
   * If an error occured, an IllegalStateException will be thrown with a user displayable error message.
   */
  @Override
  public void createUser(final DbUser user, final HashedPassword password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());
    primaryDao.createUser(user, password);
    try {
      new Thread(() -> secondaryDaoSet.forEach(dao -> dao.createUser(user, password)));
    } catch (Exception e) {
      logger.warning("Secondary datasource failure, this is okay if we are still setting "
          + "up a new datasource, error: " + e.getMessage());
    }
  }

  /**
   * Validate the username password, returning true if the user is able to login.
   * This has the side effect of updating the users last login time.
   */
  @Override
  public boolean login(final String userName, final HashedPassword password) {
    return primaryDao.login(userName, password);
  }

  /**
   * @return null if no such user.
   */
  @Override
  public DbUser getUserByName(final String userName) {
    return primaryDao.getUserByName(userName);
  }

}
