package games.strategy.engine.lobby.server.userDB;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DbUserTest {

  private static void verifyValid(final DBUser validDbUser) {
    assertThat("Expecting no validation error messages: " + validDbUser.getValidationErrorMessage(),
        validDbUser.getValidationErrorMessage(), nullValue());
    assertThat(validDbUser.isValid(), is(true));
  }

  private static void verifyInvalid(final DBUser invalidDbUser) {
    assertThat("DBUser values were expected to be invalid: " + invalidDbUser,
        invalidDbUser.isValid(), is(false));
    assertThat(invalidDbUser.getValidationErrorMessage(), not(emptyString()));
  }

  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(DBUser.class).verify();
  }

  @Test
  public void valueObjectPropertiesAreSet() {
    final DBUser admin = new DBUser(
        new DBUser.UserName(TestData.name),
        new DBUser.UserEmail(TestData.email));

    assertThat(admin.getName(), is(TestData.name));
    assertThat(admin.getEmail(), is(TestData.email));
    assertThat("by default not admin", admin.isAdmin(), is(false));
  }

  @Test
  public void notAdmin() {
    final DBUser notAdmin = new DBUser(
        new DBUser.UserName(TestData.name),
        new DBUser.UserEmail(TestData.email),
        DBUser.Role.NOT_ADMIN);

    assertThat(notAdmin.isAdmin(), is(false));
  }

  @Test
  public void admin() {
    final DBUser admin = new DBUser(
        new DBUser.UserName(TestData.name),
        new DBUser.UserEmail(TestData.email),
        DBUser.Role.ADMIN);

    assertThat(admin.isAdmin(), is(true));
  }

  @Test
  public void validDbUser() {
    verifyValid(
        new DBUser(
            new DBUser.UserName(TestData.name),
            new DBUser.UserEmail(TestData.email)));
  }

  @Test
  public void inValidDbUser() {
    verifyInvalid(
        new DBUser(
            new DBUser.UserName(TestData.name),
            new DBUser.UserEmail("")));

    verifyInvalid(
        new DBUser(
            new DBUser.UserName(""),
            new DBUser.UserEmail(TestData.email)));

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
          DBUser.isValidUserName(invalidName), is(false));
      assertThat("Expected name to have validation error messages: " + invalidName,
          DBUser.getUserNameValidationErrorMessage(invalidName), not(emptyString()));
    });

    DBUser.forbiddenNameParts.forEach(
        invalidNamePart -> {
          assertThat("user names cannot contain anything from the forbidden name list",
              DBUser.isValidUserName(invalidNamePart), is(false));
          assertThat("verify we are doing a contains match to make sure "
                  + "user name does not contain anything forbidden.",
              DBUser.isValidUserName("xyz" + invalidNamePart + "abc"), is(false));

          assertThat("case insensitive on our matches.",
              DBUser.isValidUserName(invalidNamePart.toUpperCase()), is(false));
          assertThat("case insensitive on our matches.",
              DBUser.isValidUserName(invalidNamePart.toLowerCase()), is(false));
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
          DBUser.isValidUserName(validName), is(true));

      assertThat(
          String.format("Expected name: %s, to have no validation error messages, but had %s",
              validName, DBUser.getUserNameValidationErrorMessage(validName)),
          DBUser.getUserNameValidationErrorMessage(validName), nullValue());
    });
  }

  private interface TestData {
    String name = "abc";
    String email = "email@abc.com";
  }
}
