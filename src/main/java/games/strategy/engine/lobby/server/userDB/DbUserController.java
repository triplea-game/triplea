package games.strategy.engine.lobby.server.userDB;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;


/**
 * Interface for interacting with database user operations. For example, add user, delete user..
 */
/*
 * TODO: datasource migration note, from derby (java DB) to MySQL
 *  This class is currently set up to write and read from multiple data sources.
 *  This is a migration tool, to go from one data source to another. When we switch over completely
 *  we can simplify and write/read to the primary datasource directly.
 */
public class DbUserController implements UserDao {
  private static final Logger logger = Logger.getLogger(DbUserController.class.getName());

  private static final Collection<UserDaoPrimarySecondary> implementations = Arrays.asList(
      new DerbyUserController(),
      new SqlUserController()
  );

  /**
   * Stores a convenience reference to the authoritative DAO implementation.
   * We use this datasource for reading, we always write to it, any errors during read/write are considered
   * critical (on the other hand secondary DAO read/write errors can be logged and ignored)
   */
  private static final UserDao primaryDao = determinePrimaryDataSource();

  private static UserDao determinePrimaryDataSource() {
    List<UserDao> primaryDataSourceCandidates = implementations.stream()
        .filter(UserDaoPrimarySecondary::isPrimary)
        .collect(Collectors.toList());

    Preconditions.checkState(
        primaryDataSourceCandidates.size() == 1,
        "Startup Assumption - exactly one UserDao implementation is 'primary'");

    return primaryDataSourceCandidates.get(0);
  }


  /**
   * @return null if the user does not exist.
   */
  @Override
  public String getPassword(final String userName) {
    return primaryDao.getPassword(userName);
  }

  @Override
  public boolean doesUserExist(final String userName) {
    return primaryDao.doesUserExist(userName);
  }

  @Override
  public void updateUser(final String name, final String email, final String hashedPassword, final boolean admin) {
    primaryDao.updateUser(name, email, hashedPassword, admin);
    try {
      // TODO : if there is a timeout on failure, this may need to be done on a background thread
      implementations.forEach(dao -> dao.updateUser(name, email, hashedPassword, admin));
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
  public void createUser(final String name, final String email, final String hashedPassword,
      final boolean admin) {
    primaryDao.createUser(name, email, hashedPassword, admin);
    try {
      // TODO : if there is a timeout on failure, this may need to be done on a background thread
      implementations.forEach(dao -> dao.createUser(name, email, hashedPassword, admin));
    } catch (Exception e) {
      logger.warning(
          "Secondary datasource failure, this is okay if we are still setting up a new datasource. " + e.getMessage());
    }
  }

  /**
   * Validate the username password, returning true if the user is able to login.
   * This has the side effect of updating the users last login time.
   */
  @Override
  public boolean login(final String userName, final String hashedPassword) {
    return primaryDao.login(userName, hashedPassword);
  }

  /**
   * @return null if no such user.
   */
  @Override
  public DbUser getUser(final String userName) {
    return primaryDao.getUser(userName);
  }

}
