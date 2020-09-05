package org.triplea.generic.xml.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.annotations.BodyText;
import org.triplea.generic.xml.reader.annotations.Tag;

/** This test focuses on verifying parsing of XML body text content. */
@SuppressWarnings("UnmatchedTest")
public class BodyTextParsingTest extends AbstractXmlMapperTest {

  BodyTextParsingTest() {
    super("xml-body-parsing-example.xml");
  }

  private static class Game {
    @Tag private Description description;
    @Tag private Notes notes;

    private static class Description {
      @BodyText private String value;
    }

    private static class Notes {
      @BodyText private String value;
    }
  }

  @Test
  void readAttachmentListTag() throws Exception {

    final Game game = xmlMapper.mapXmlToObject(Game.class);

    assertThat(game, is(notNullValue()));
    assertThat(game.description, is(notNullValue()));
    assertThat(game.description.value, is(notNullValue()));
    assertThat(game.description.value, is("Single line description"));

    assertThat(game.notes, is(notNullValue()));
    assertThat(game.notes.value, is(notNullValue()));
    assertThat(
        game.notes.value,
        is(
            "<html>\n"
                + "                <title>Html Test</title>\n"
                + "                <body>Body content</body>\n"
                + "            </html>"));
  }
}
