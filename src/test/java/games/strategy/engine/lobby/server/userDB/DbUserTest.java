package games.strategy.engine.lobby.server.userDB;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import games.strategy.engine.lobby.server.userDB.DbUser;

public class DbUserTest {

  @Test
  public void valueObjectPropertiesAreSet() {
    DbUser admin = new DbUser(
        new DbUser.UserName(TestData.name),
        new DbUser.UserEmail(TestData.email));

    assertThat(admin.name, is(TestData.name));
    assertThat(admin.email, is(TestData.email));
    assertThat("by default not admin", admin.admin, is(false));
  }

  @Test
  public void notAdmin() {
    DbUser notAdmin = new DbUser(
        new DbUser.UserName(TestData.name),
        new DbUser.UserEmail(TestData.email),
        DbUser.Role.NOT_ADMIN);

    assertThat(notAdmin.admin, is(false));
  }

  @Test
  public void admin() {
    DbUser admin = new DbUser(
        new DbUser.UserName(TestData.name),
        new DbUser.UserEmail(TestData.email),
        DbUser.Role.ADMIN);

    assertThat(admin.admin, is(true));
  }

  @Test
  public void validDbUser() {
    verifyValid(
        new DbUser(
            new DbUser.UserName(TestData.name),
            new DbUser.UserEmail(TestData.email)));
  }

  private static void verifyValid(DbUser validDbUser) {
    assertThat("Expecting no validation error messages: " + validDbUser.getValidationErrorMessage(),
        validDbUser.getValidationErrorMessage(), nullValue());
    assertThat(validDbUser.isValid(), is(true));
  }

  @Test
  public void inValidDbUser() {
    verifyInvalid(
        new DbUser(
            new DbUser.UserName(TestData.name),
            new DbUser.UserEmail("")));

    verifyInvalid(
        new DbUser(
            new DbUser.UserName(""),
            new DbUser.UserEmail(TestData.email)));

  }

  private static void verifyInvalid(DbUser invalidDbUser) {
    assertThat("DbUser values were expected to be invalid: " + invalidDbUser,
        invalidDbUser.isValid(), is(false));
    assertThat(invalidDbUser.getValidationErrorMessage(), not(emptyString()));
  }
  private interface TestData {
    String name = "abc";
    String email = "email@abc.com";
  }
}
