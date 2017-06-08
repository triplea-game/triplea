package games.strategy.engine.lobby.server.userDB;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

public class DbUserTest {

  private static void verifyValid(DbUser validDbUser) {
    assertThat("Expecting no validation error messages: " + validDbUser.getValidationErrorMessage(),
        validDbUser.getValidationErrorMessage(), nullValue());
    assertThat(validDbUser.isValid(), is(true));
  }

  private static void verifyInvalid(DbUser invalidDbUser) {
    assertThat("DbUser values were expected to be invalid: " + invalidDbUser,
        invalidDbUser.isValid(), is(false));
    assertThat(invalidDbUser.getValidationErrorMessage(), not(emptyString()));
  }

  @Test
  public void valueObjectPropertiesAreSet() {
    DbUser admin = new DbUser(
        new DbUser.UserName(TestData.name),
        new DbUser.UserEmail(TestData.email));

    assertThat(admin.getName(), is(TestData.name));
    assertThat(admin.getEmail(), is(TestData.email));
    assertThat("by default not admin", admin.isAdmin(), is(false));
  }

  @Test
  public void notAdmin() {
    DbUser notAdmin = new DbUser(
        new DbUser.UserName(TestData.name),
        new DbUser.UserEmail(TestData.email),
        DbUser.Role.NOT_ADMIN);

    assertThat(notAdmin.isAdmin(), is(false));
  }

  @Test
  public void admin() {
    DbUser admin = new DbUser(
        new DbUser.UserName(TestData.name),
        new DbUser.UserEmail(TestData.email),
        DbUser.Role.ADMIN);

    assertThat(admin.isAdmin(), is(true));
  }

  @Test
  public void validDbUser() {
    verifyValid(
        new DbUser(
            new DbUser.UserName(TestData.name),
            new DbUser.UserEmail(TestData.email)));
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

  @Test
  public void userNameValidationWithInvalidNames() {
    Arrays.asList(
        null,
        "",
        "a",
        "ab", // still too short
        "ab*", // no special characters other than '-' and '_'
        "ab$",
        ".ab",
        "a,b",
        "ab?",
        "   ", // no spaces
        "a b"
    ).forEach(invalidName -> {
      assertThat("Expected name to be marked as invalid: " + invalidName,
          DbUser.isValidUserName(invalidName), is(false));
      assertThat("Expected name to have validation error messages: " + invalidName,
          DbUser.getUserNameValidationErrorMessage(invalidName), not(emptyString()));
    });

    DbUser.forbiddenNameParts.forEach(
        invalidNamePart -> {
          assertThat("user names cannot contain anything from the forbidden name list",
              DbUser.isValidUserName(invalidNamePart), is(false));
          assertThat("verify we are doing a contains match to make sure "
                  + "user name does not contain anything forbidden.",
              DbUser.isValidUserName("xyz" + invalidNamePart + "abc"), is(false));

          assertThat("case insensitive on our matches.",
              DbUser.isValidUserName(invalidNamePart.toUpperCase()), is(false));
          assertThat("case insensitive on our matches.",
              DbUser.isValidUserName(invalidNamePart.toLowerCase()), is(false));
        });
  }

  @Test
  public void userNameValidationWithValidNames() {
    Arrays.asList(
        "abc",
        "123",
        "---",
        // TODO: should we add a max length rule to user name validation?
        "test_case_with_something_that_is_a_bit_longer_and_perhaps_even_should_be_considered_invalid"
    ).forEach(validName -> {
      assertThat(
          "Expected name to be marked as valid: " + validName,
          DbUser.isValidUserName(validName), is(true));

      assertThat(
          String.format("Expected name: %s, to have no validation error messages, but had %s",
              validName, DbUser.getUserNameValidationErrorMessage(validName)),
          DbUser.getUserNameValidationErrorMessage(validName), nullValue());
    });
  }

  private interface TestData {
    String name = "abc";
    String email = "email@abc.com";
  }
}
