package games.strategy.engine.lobby.server.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;

import org.junit.Test;

import games.strategy.util.MD5Crypt;
import nl.jqno.equalsverifier.EqualsVerifier;

public class HashedPasswordTest {
  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(HashedPassword.class).verify();
  }

  @Test
  public void isValidSyntax() {
    Arrays.asList(
        MD5Crypt.MAGIC,
        MD5Crypt.MAGIC + " ",
        MD5Crypt.MAGIC + "_",
        MD5Crypt.MAGIC + "abc"
    ).forEach(valid ->
        assertThat(
            "Expecting this to look valid, starts with magic: " + valid,
            new HashedPassword(valid).isValidSyntax(), is(true)));
  }

  @Test
  public void isValidSyntaxInvalidCases() {
    Arrays.asList(
        "",
        "abc",
        "  ",
        "\n",
        "#00000"
    ).forEach(invalid ->
        assertThat(
            "Expecting this to look invalid, does not start with magic: " + invalid,
            new HashedPassword(invalid).isValidSyntax(), is(false)));
  }
}
