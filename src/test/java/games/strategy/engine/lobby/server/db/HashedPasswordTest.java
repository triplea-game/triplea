package games.strategy.engine.lobby.server.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class HashedPasswordTest {
  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(HashedPassword.class).verify();
  }

  @Test
  public void isValidSyntax() {
    final String md5CryptMagic = games.strategy.util.MD5Crypt.MAGIC;

    Arrays.asList(
        md5CryptMagic,
        md5CryptMagic + " ",
        md5CryptMagic + "_",
        md5CryptMagic + "abc").forEach(
            valid -> assertThat(
                "Expecting this to look valid, starts with magic: " + valid,
                new HashedPassword(valid).isHashedWithSalt(), is(true)));
  }

  @Test
  public void isValidSyntaxInvalidCases() {
    Arrays.asList(
        "",
        "abc",
        "  ",
        "\n",
        "#00000").forEach(
            invalid -> assertThat(
                "Expecting this to look invalid, does not start with magic: " + invalid,
                new HashedPassword(invalid).isHashedWithSalt(), is(false)));
  }
}
