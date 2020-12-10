package org.triplea.generic.xml.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.BodyText;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@SuppressWarnings("UnmatchedTest")
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

    assertThat(library.libraryInventory.attributeThatDoesNotExist, is(nullValue()));
  }

  @Test
  void verifySimpleExample() throws Exception {
    final Library library = xmlMapper.mapXmlToObject(Library.class);

    assertThat(library, is(notNullValue()));
    assertThat(library.mostReadExample, is(notNullValue()));
    assertThat(library.exampleOfListThatIsNotPresent, is(empty()));

    assertThat(library.libraryInventory, is(notNullValue()));
    assertThat(library.libraryInventory.type, is("available"));
    assertThat(library.libraryInventory.books, hasSize(2));
    assertThat(library.libraryInventory.books.get(0), is(notNullValue()));
    assertThat(library.libraryInventory.books.get(0).name, is("Crossing the Atlantic"));
    assertThat(library.libraryInventory.books.get(1), is(notNullValue()));
    assertThat(library.libraryInventory.books.get(1).name, is("The Battle of the Bulge"));

    assertThat(library.libraryInventory.dvds, hasSize(2));
    assertThat(library.libraryInventory.dvds.get(0), is(notNullValue()));
    assertThat(library.libraryInventory.dvds.get(0).name, is("How to Win Revised"));
    assertThat(library.libraryInventory.dvds.get(1), is(notNullValue()));
    assertThat(library.libraryInventory.dvds.get(1).name, is("Game of TripleA"));
  }
}
