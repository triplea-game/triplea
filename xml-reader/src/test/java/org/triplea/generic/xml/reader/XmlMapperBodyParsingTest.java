package org.triplea.generic.xml.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.junit.jupiter.api.Test;

public class XmlMapperBodyParsingTest {

  private static class Game {

    @Tag private Description description;

    @Tag private Notes notes;

    private static class Description {
      @TextElement private String value;
    }

    private static class Notes {
      @TextElement private String value;
    }
  }

  @Test
  void readAttachmentListTag() throws Exception {
    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    try (InputStream inputStream =
        XmlMapperTest.class.getClassLoader().getResourceAsStream("xml-parser-example.xml")) {
      final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(inputStream);

      try {
        final Game game = new XmlMapper(streamReader).mapXmlToClass(Game.class);
        assertThat(game, is(notNullValue()));
        assertThat(game.description, is(notNullValue()));
        assertThat(game.description.value, is(notNullValue()));
        assertThat(game.description.value, is("Single line description"));

        assertThat(game.notes, is(notNullValue()));
        assertThat(game.notes.value, is(notNullValue()));
        assertThat(game.description.value, is("<html>Html Test</html>"));
      } finally {
        streamReader.close();
      }
    }
  }
}
