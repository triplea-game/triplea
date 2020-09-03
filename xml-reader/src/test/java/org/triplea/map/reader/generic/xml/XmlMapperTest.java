package org.triplea.map.reader.generic.xml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.InputStream;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;
import org.junit.jupiter.api.Test;

public class XmlMapperTest {

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
      @TagList(Book.class)
      private List<Book> books;

      @Tag private CdRom cdrom;
      @Tag private Gaming gaming;

      static class Book {
        @Attribute private String name;
      }

      static class CdRom {
        @Attribute private String name;
      }

      static class Gaming {
        @TagList(Dvd.class)
        private List<Dvd> dvds;

        @TagList(BluRay.class)
        private List<BluRay> bluRays;

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
  void readAttachmentListTag() throws Exception {
    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    final InputStream inputStream =
        XmlMapperTest.class.getClassLoader().getResourceAsStream("xml-parser-example.xml");
    final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(inputStream);

    try {
      final Library library = new XmlMapper(streamReader).mapXmlToClass(Library.class);
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
    } finally {
      streamReader.close();
    }
  }
}
