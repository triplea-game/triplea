package org.triplea.generic.xml.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@SuppressWarnings("UnmatchedTest")
public class ExtendedLibraryExampleTest extends AbstractXmlMapperTest {

  ExtendedLibraryExampleTest() {
    super("xml-parser-example.xml");
  }

  @SuppressWarnings("unused")
  @Getter
  static class Library {
    @Tag private Name name;
    @Tag private MostRead mostRead;
    @Tag private Inventory inventory;

    static class Name {
      @Attribute private String libraryName;
    }

    static class MostRead {
      @Tag private Book book;
      @Tag private Magazine magazine;

      static class Book {
        @Attribute private String title;
        @Attribute private String isbn;
      }

      static class Magazine {
        @Attribute private String title;
        @Attribute private String isbn;
      }
    }

    static class Inventory {
      @TagList private List<Book> books;

      @Tag private CdRom cdrom;
      @Tag private Gaming gaming;

      static class Book {
        @Attribute private String name;
      }

      static class CdRom {
        @Attribute private String name;
      }

      static class Gaming {
        @TagList private List<Dvd> dvds;

        @TagList private List<BluRay> bluRays;

        static class Dvd {
          @Attribute private String name;
        }

        static class BluRay {
          @Attribute private String name;
        }
      }
    }
  }

  @Test
  void verifyExtendedExample() throws Exception {

    final Library library = xmlMapper.mapXmlToObject(Library.class);
    assertThat(library, is(notNullValue()));

    assertThat(library.name, is(notNullValue()));
    assertThat(library.name.libraryName, is("Central Library"));

    assertThat(library.mostRead, is(notNullValue()));
    assertThat(library.mostRead.magazine, is(notNullValue()));
    assertThat(library.mostRead.magazine.title, is("War Gaming"));
    assertThat(library.mostRead.magazine.isbn, is("123"));
    assertThat(library.mostRead.book, is(notNullValue()));
    assertThat(library.mostRead.book.title, is("Strategy"));
    assertThat(library.mostRead.book.isbn, is("789"));

    assertThat(library.inventory, is(notNullValue()));
    assertThat(library.inventory.books, hasSize(2));
    assertThat(library.inventory.books.get(0).name, is("Crossing the Atlantic"));
    assertThat(library.inventory.books.get(1).name, is("The Battle of the Bulge"));

    assertThat(library.inventory.cdrom, is(notNullValue()));
    assertThat(library.inventory.cdrom.name, is("Pacific Conflict"));

    assertThat(library.inventory.gaming, is(notNullValue()));
    assertThat(library.inventory.gaming.dvds, is(notNullValue()));
    assertThat(library.inventory.gaming.dvds, hasSize(2));
    assertThat(library.inventory.gaming.dvds.get(0).name, is("How to Win Revised"));
    assertThat(library.inventory.gaming.dvds.get(1).name, is("Game of TripleA"));

    assertThat(library.inventory.gaming.bluRays, is(notNullValue()));
    assertThat(library.inventory.gaming.bluRays, hasSize(1));
    assertThat(library.inventory.gaming.bluRays.get(0).name, is("NWO Lebowski"));
  }
}
