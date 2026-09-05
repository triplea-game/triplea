package org.triplea.generic.xml.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.BodyText;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

public class SimpleLibraryExampleTest extends AbstractXmlMapperTest {

  SimpleLibraryExampleTest() {
    super("simple-library-example.xml");
  }

  /** POJO modelling the XML in our sample dataset. */
  @Getter
  public static class Library {

    @Tag private MostRead mostReadExample;
    @Tag private Inventory libraryInventory;

    @TagList private List<NotPresentListElement> exampleOfListThatIsNotPresent;

    @Getter
    public static class MostRead {
      @Attribute private String updated;
      @BodyText private String bodyText;
    }

    public static class NotPresentListElement {}

    @Getter
    public static class Inventory {
      @Attribute private String attributeThatDoesNotExist;

      @Attribute private String type;

      @TagList private List<Book> books;

      @TagList private List<Dvd> dvds;

      @Getter
      public static class Book {
        @Attribute private String name;
      }

      @Getter
      public static class Dvd {
        @Attribute private String name;
      }
    }
  }

  @Test
  void attributesNotPresentInXmlAreNull() throws Exception {
    final Library library = xmlMapper.mapXmlToObject(Library.class);

    assertThat(library.libraryInventory.attributeThatDoesNotExist).isNull();
  }

  @Test
  void verifySimpleExample() throws Exception {
    final Library library = xmlMapper.mapXmlToObject(Library.class);

    assertThat(library).isNotNull();
    assertThat(library.mostReadExample).isNotNull();
    assertThat(library.exampleOfListThatIsNotPresent).isEmpty();

    assertThat(library.libraryInventory).isNotNull();
    assertThat(library.libraryInventory.type).isEqualTo("available");
    assertThat(library.libraryInventory.books).hasSize(2);
    assertThat(library.libraryInventory.books.get(0)).isNotNull();
    assertThat(library.libraryInventory.books.get(0).name).isEqualTo("Crossing the Atlantic");
    assertThat(library.libraryInventory.books.get(1)).isNotNull();
    assertThat(library.libraryInventory.books.get(1).name).isEqualTo("The Battle of the Bulge");

    assertThat(library.libraryInventory.dvds).hasSize(2);
    assertThat(library.libraryInventory.dvds.get(0)).isNotNull();
    assertThat(library.libraryInventory.dvds.get(0).name).isEqualTo("How to Win Revised");
    assertThat(library.libraryInventory.dvds.get(1)).isNotNull();
    assertThat(library.libraryInventory.dvds.get(1).name).isEqualTo("Game of TripleA");
  }
}
