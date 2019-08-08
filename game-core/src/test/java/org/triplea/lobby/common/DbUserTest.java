package org.triplea.lobby.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

import com.google.common.base.Strings;
import games.strategy.engine.lobby.server.userDB.DBUser;
import java.util.Arrays;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class DbUserTest {

  private static void verifyValid(final DBUser validDbUser) {
    assertThat(
        "Expecting no validation error messages: " + validDbUser.getValidationErrorMessage(),
        validDbUser.getValidationErrorMessage(),
        nullValue());
    assertThat(validDbUser.isValid(), is(true));
  }

  private static void verifyInvalid(final DBUser invalidDbUser) {
    assertThat(
        "DBUser values were expected to be invalid: " + invalidDbUser,
        invalidDbUser.isValid(),
        is(false));
    assertThat(invalidDbUser.getValidationErrorMessage(), not(emptyString()));
  }

  @Test
  void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(DBUser.class).verify();
  }

  @Test
  void valueObjectPropertiesAreSet() {
    final DBUser admin =
        new DBUser(new DBUser.UserName(TestData.name), new DBUser.UserEmail(TestData.email));

    assertThat(admin.getName(), is(TestData.name));
    assertThat(admin.getEmail(), is(TestData.email));
    assertThat("by default not admin", admin.isAdmin(), is(false));
  }

  @Test
  void notAdmin() {
    final DBUser notAdmin =
        new DBUser(
            new DBUser.UserName(TestData.name),
            new DBUser.UserEmail(TestData.email),
            DBUser.Role.NOT_ADMIN);

    assertThat(notAdmin.isAdmin(), is(false));
  }

  @Test
  void admin() {
    final DBUser admin =
        new DBUser(
            new DBUser.UserName(TestData.name),
            new DBUser.UserEmail(TestData.email),
            DBUser.Role.ADMIN);

    assertThat(admin.isAdmin(), is(true));
  }

  @Test
  void validDbUser() {
    verifyValid(
        new DBUser(new DBUser.UserName(TestData.name), new DBUser.UserEmail(TestData.email)));
  }

  @Test
  void inValidDbUser() {
    verifyInvalid(new DBUser(new DBUser.UserName(TestData.name), new DBUser.UserEmail("")));

    verifyInvalid(new DBUser(new DBUser.UserName(""), new DBUser.UserEmail(TestData.email)));
  }

  @Test
  void userNameValidationWithInvalidNames() {
    Arrays.asList(
            null,
            "",
            "a",
            "ab", // still too short
            Strings.repeat("a", DBUser.UserName.MAX_LENGTH + 1),
            "ab*", // no special characters other than '-' and '_'
            "ab$",
            ".ab",
            "a,b",
            "ab?",
            "   ", // no spaces
            "a b")
        .forEach(
            invalidName -> {
              assertThat(
                  "Expected name to be marked as invalid: " + invalidName,
                  DBUser.isValidUserName(invalidName),
                  is(false));
              assertThat(
                  "Expected name to have validation error messages: " + invalidName,
                  DBUser.getUserNameValidationErrorMessage(invalidName),
                  not(emptyString()));
            });

    Arrays.asList(LobbyConstants.LOBBY_WATCHER_NAME, LobbyConstants.ADMIN_USERNAME)
        .forEach(
            invalidNamePart -> {
              assertThat(
                  "user names cannot contain anything from the forbidden name list",
                  DBUser.isValidUserName(invalidNamePart),
                  is(false));
              assertThat(
                  "verify we are doing a contains match to make sure "
                      + "user name does not contain anything forbidden.",
                  DBUser.isValidUserName("xyz" + invalidNamePart + "abc"),
                  is(false));

              assertThat(
                  "case insensitive on our matches.",
                  DBUser.isValidUserName(invalidNamePart.toUpperCase()),
                  is(false));
              assertThat(
                  "case insensitive on our matches.",
                  DBUser.isValidUserName(invalidNamePart.toLowerCase()),
                  is(false));
            });
  }

  @Test
  void userNameValidationWithValidNames() {
    Arrays.asList("abc", Strings.repeat("a", DBUser.UserName.MAX_LENGTH), "123", "---")
        .forEach(
            validName -> {
              assertThat(
                  "Expected name to be marked as valid: " + validName,
                  DBUser.isValidUserName(validName),
                  is(true));

              assertThat(
                  String.format(
                      "Expected name: %s, to have no validation error messages, but had %s",
                      validName, DBUser.getUserNameValidationErrorMessage(validName)),
                  DBUser.getUserNameValidationErrorMessage(validName),
                  nullValue());
            });
  }

  private interface TestData {
    String name = "abc";
    String email = "email@abc.com";
  }
}
