package org.triplea.generic.xml.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

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
      @Attribute private String notInTheXml;
    }

    static class MostRead {
      @Tag private MostRead.Book book;
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
      @TagList private List<Inventory.Book> books;

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
    assertThat(library).isNotNull();

    assertThat(library.name).isNotNull();
    assertThat(library.name.libraryName).isEqualTo("Central Library");

    assertThat(library.mostRead).isNotNull();
    assertThat(library.mostRead.magazine).isNotNull();
    assertThat(library.mostRead.magazine.title).isEqualTo("War Gaming");
    assertThat(library.mostRead.magazine.isbn).isEqualTo("123");
    assertThat(library.mostRead.book).isNotNull();
    assertThat(library.mostRead.book.title).isEqualTo("Strategy");
    assertThat(library.mostRead.book.isbn).isEqualTo("789");

    assertThat(library.inventory).isNotNull();
    assertThat(library.inventory.books).hasSize(2);
    assertThat(library.inventory.books.get(0).name).isEqualTo("Crossing the Atlantic");
    assertThat(library.inventory.books.get(1).name).isEqualTo("The Battle of the Bulge");

    assertThat(library.inventory.cdrom).isNotNull();
    assertThat(library.inventory.cdrom.name).isEqualTo("Pacific Conflict");

    assertThat(library.inventory.gaming).isNotNull();
    assertThat(library.inventory.gaming.dvds).isNotNull();
    assertThat(library.inventory.gaming.dvds).hasSize(2);
    assertThat(library.inventory.gaming.dvds.get(0).name).isEqualTo("How to Win Revised");
    assertThat(library.inventory.gaming.dvds.get(1).name).isEqualTo("Game of TripleA");

    assertThat(library.inventory.gaming.bluRays).isNotNull();
    assertThat(library.inventory.gaming.bluRays).hasSize(1);
    assertThat(library.inventory.gaming.bluRays.get(0).name).isEqualTo("NWO Lebowski");
  }
}
