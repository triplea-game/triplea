package games.strategy.engine.framework.startup.ui.editors;

import static games.strategy.engine.framework.startup.ui.pbem.ForumPosterEditor.isInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class ForumPosterEditorTest {

  @Test
  void testIsInt() {
    assertThat(isInt(""), is(false));
    assertThat(isInt("12 34"), is(false));
    assertThat(isInt("12.34"), is(false));
    assertThat(isInt("1234"), is(true));
    assertThat(isInt("0000000000000"), is(true));
    assertThat(isInt("-0"), is(true));
    assertThat(isInt("-4321"), is(true));
  }
}
