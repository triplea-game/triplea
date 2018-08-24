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
    Arrays.asList(
        "$1$wwmV2glD$J5dZUS3L8DAMUim4wdL/11",
        "$2a$10$7v.RGs4Aw.cW8Hg1LGINQO/EKf47TAQDClHXkDAzx6HCuakrjBF7.")
        .forEach(
            value -> assertThat(
                "Expecting this to look valid: " + value,
                new HashedPassword(value).isHashedWithSalt(), is(true)));
  }

  @Test
  public void isValidSyntaxInvalidCases() {
    Arrays.asList(
        "",
        "abc",
        "  ",
        "\n",
        "#00000")
        .forEach(
            value -> assertThat(
                "Expecting this to look invalid: " + value,
                new HashedPassword(value).isHashedWithSalt(), is(false)));
  }
}
