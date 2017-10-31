package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class VersionTest {
  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(Version.class).withIgnoredFields("exactVersion").verify();
  }

  @Test
  public void testCompareTo() {
    assertThrows(NullPointerException.class, () -> new Version(1, 0).compareTo(null));

    assertThat(new Version(1, 0, 0, 0).compareTo(new Version(2, 0, 0, 0)), is(lessThan(0)));
    assertThat(new Version(0, 1, 0, 0).compareTo(new Version(0, 2, 0, 0)), is(lessThan(0)));
    assertThat(new Version(0, 0, 1, 0).compareTo(new Version(0, 0, 2, 0)), is(lessThan(0)));
    assertThat(new Version(0, 0, 0, 1).compareTo(new Version(0, 0, 0, 2)), is(lessThan(0)));
    assertThat(new Version(0, 0, 0, 0).compareTo(new Version("0.0.0.dev")), is(lessThan(0)));

    assertThat(new Version(1, 0, 0, 0).compareTo(new Version(1, 0, 0, 0)), is(0));
    assertThat(new Version(0, 1, 0, 0).compareTo(new Version(0, 1, 0, 0)), is(0));
    assertThat(new Version(0, 0, 1, 0).compareTo(new Version(0, 0, 1, 0)), is(0));
    assertThat(new Version(0, 0, 0, 1).compareTo(new Version(0, 0, 0, 1)), is(0));
    assertThat(new Version("0.0.0.dev").compareTo(new Version("0.0.0.dev")), is(0));

    assertThat(new Version(2, 0, 0, 0).compareTo(new Version(1, 0, 0, 0)), is(greaterThan(0)));
    assertThat(new Version(0, 2, 0, 0).compareTo(new Version(0, 1, 0, 0)), is(greaterThan(0)));
    assertThat(new Version(0, 0, 2, 0).compareTo(new Version(0, 0, 1, 0)), is(greaterThan(0)));
    assertThat(new Version(0, 0, 0, 2).compareTo(new Version(0, 0, 0, 1)), is(greaterThan(0)));
    assertThat(new Version("0.0.0.dev").compareTo(new Version(0, 0, 0, 0)), is(greaterThan(0)));
  }

  @Test
  public void testToString() {
    assertEquals("1.2.3", new Version("1.2.3").toString());
    assertEquals("1.2", new Version("1.2").toString());
    assertEquals("1.2", new Version("1.2.0").toString());
    assertEquals("1.2.3.dev", new Version("1.2.3.dev").toString());
  }

  @Test
  public void testToStringFull() {
    assertEquals("1_2_3_dev", new Version("1.2.3.dev").toStringFull('_'));
  }

  @Test
  public void testGetExactVersion() {
    assertEquals("1.2.3.4", new Version(1, 2, 3, 4).getExactVersion());
    assertEquals("1.2.3.4.5", new Version("1.2.3.4.5").getExactVersion());
  }
}
