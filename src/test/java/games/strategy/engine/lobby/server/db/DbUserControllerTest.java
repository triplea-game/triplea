package games.strategy.engine.lobby.server.db;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.core.Is;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.lobby.server.userDB.DBUser;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DbUserControllerTest {

  private DbUserController testObj;

  @Mock
  private UserDao mockPrimary;

  @Mock
  private UserDao mockSecondary;

  @Mock
  private MigrationCounter mockMigrationCounter;

  @Before
  public void setup() {
    testObj = new DbUserController(mockPrimary, mockSecondary, mockMigrationCounter);
  }

  /**
   * Happiest case of login, success on the secondary (new DB), no fallback, we just return true and record stats.
   */
  @Test
  public void login_SecondarySucceeds() {
    Mockito.when(mockSecondary.login(TestData.userName, TestData.password))
        .thenReturn(true);

    final boolean loginResult = testObj.login(TestData.userName, TestData.password);

    assertThat("expect successful login against our new (secondary) shiny DB, yay!",
        loginResult, Is.is(true));

    Mockito.verify(mockMigrationCounter, Mockito.times(1)).secondaryLoginSuccess();
  }


  /**
   * Fallback to primary DB after miss on secondary, fake a success on primary and verify we do
   * a user data migration to secondary DB.
   */
  @Test
  public void login_SecondaryFailed_PrimarySucceeds() {
    Mockito.when(mockSecondary.login(TestData.userName, TestData.password))
        .thenReturn(false);
    Mockito.when(mockPrimary.login(TestData.userName, TestData.password))
        .thenReturn(true);
    Mockito.when(mockPrimary.getUserByName(TestData.userName))
        .thenReturn(TestData.user);

    final boolean loginResult = testObj.login(TestData.userName, TestData.password);

    assertThat("we expect a successful login against the now legacy DB (primary)",
        loginResult, Is.is(true));

    Mockito.verify(mockMigrationCounter, Mockito.times(1)).primaryLoginSuccess();

    Mockito.verify(mockSecondary, Mockito.times(1))
        .createUser(TestData.user, TestData.password);
  }

  /**
   * Login miss on secondary and primary, the unhappy case, we just record stats here.
   */
  @Test
  public void login_NoLookupCase() {
    Mockito.when(mockSecondary.login(TestData.userName, TestData.password))
        .thenReturn(false);
    Mockito.when(mockPrimary.login(TestData.userName, TestData.password))
        .thenReturn(false);

    final boolean loginResult = testObj.login(TestData.userName, TestData.password);

    assertThat("no successful login, not found in primary nor secondary",
        loginResult, Is.is(false));

    Mockito.verify(mockMigrationCounter, Mockito.times(1))
        .loginFailure();
  }


  @Test
  public void doesUserExist_FoundInSecondary() {
    Mockito.when(mockSecondary.doesUserExist(TestData.userName))
        .thenReturn(true);

    assertThat(testObj.doesUserExist(TestData.userName), Is.is(true));
  }

  @Test
  public void doesUserExist_FoundInPrimary() {
    Mockito.when(mockSecondary.doesUserExist(TestData.userName))
        .thenReturn(false);
    Mockito.when(mockPrimary.doesUserExist(TestData.userName))
        .thenReturn(true);

    assertThat(testObj.doesUserExist(TestData.userName), Is.is(true));
  }

  @Test
  public void doesUserExist_NotFound() {
    Mockito.when(mockSecondary.doesUserExist(TestData.userName))
        .thenReturn(false);
    Mockito.when(mockPrimary.doesUserExist(TestData.userName))
        .thenReturn(false);

    assertThat(testObj.doesUserExist(TestData.userName), Is.is(false));
  }

  @Test
  public void updateUser() {
    testObj.updateUser(TestData.user, TestData.password);

    Mockito.verify(mockSecondary, Mockito.times(1))
        .updateUser(TestData.user, TestData.password);
  }


  @Test
  public void getUserByName_secondaryExists() {
    Mockito.when(mockSecondary.getUserByName(TestData.userName))
        .thenReturn(TestData.user);

    final DBUser result = testObj.getUserByName(TestData.userName);

    assertThat(result, sameInstance(TestData.user));
  }

  @Test
  public void getUserByName_primaryExists() {
    Mockito.when(mockSecondary.getUserByName(TestData.userName))
        .thenReturn(null);
    Mockito.when(mockPrimary.getUserByName(TestData.userName))
        .thenReturn(TestData.user);

    final DBUser result = testObj.getUserByName(TestData.userName);

    assertThat(result, sameInstance(TestData.user));
  }

  @Test
  public void getUserByName_doesNotExist() {
    Mockito.when(mockSecondary.getUserByName(TestData.userName))
        .thenReturn(null);
    Mockito.when(mockPrimary.getUserByName(TestData.userName))
        .thenReturn(null);

    final DBUser result = testObj.getUserByName(TestData.userName);

    assertThat(result, nullValue());
  }

  /**
   * User should be created in both DBs for now, just in case we cannot migrate
   * and have to\ stay with Derby for whatever reason.
   */
  @Test
  public void createUser() {
    testObj.createUser(TestData.user, TestData.password);

    Mockito.verify(mockPrimary, Mockito.times(1))
        .createUser(TestData.user, TestData.password);

    Mockito.verify(mockSecondary, Mockito.times(1))
        .createUser(TestData.user, TestData.password);
  }

  @Test
  public void getPassword_secondary() {
    Mockito.when(mockSecondary.getPassword(TestData.userName))
        .thenReturn(TestData.password);

    final HashedPassword result = testObj.getPassword(TestData.userName);

    assertThat(result, sameInstance(TestData.password));
  }

  @Test
  public void getPassword_primary() {
    Mockito.when(mockSecondary.getPassword(TestData.userName))
        .thenReturn(null);
    Mockito.when(mockPrimary.getPassword(TestData.userName))
        .thenReturn(TestData.password);

    final HashedPassword result = testObj.getPassword(TestData.userName);

    assertThat(result, sameInstance(TestData.password));
  }

  @Test
  public void getPassword_notFound() {
    Mockito.when(mockSecondary.getPassword(TestData.userName))
        .thenReturn(null);
    Mockito.when(mockPrimary.getPassword(TestData.userName))
        .thenReturn(null);

    final HashedPassword result = testObj.getPassword(TestData.userName);

    assertThat(result, nullValue());
  }


  private interface TestData {
    String userName = "user";
    HashedPassword password = new HashedPassword("$1$password");
    DBUser user = new DBUser(
        new DBUser.UserName("fake_user_name_return_value"),
        new DBUser.UserEmail("user_fakeemail@fake_email.com"));
  }
}
