package org.triplea.generic.xml.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

/**
 * Verifies that we can give tags, taglists and attributes alternative names and correctly match
 * XMLs that contain those alternative names.
 */
public class TagAlternativeSpellingTest extends AbstractXmlMapperTest {

  TagAlternativeSpellingTest() {
    super("library-example-for-alt-spellings.xml");
  }

  /** POJO modelling the XML in our sample dataset. */
  @Getter
  public static class Library {
    @Tag(names = {"inventory"})
    private Catalog catalog;

    @Getter
    public static class Catalog {
      @TagList(names = {"items"})
      private List<LibraryItem> libraryItems;

      public static class LibraryItem {
        @TagList(names = {"Book", "DVD"})
        private List<Article> articles;

        @Getter
        public static class Article {
          @Attribute(names = {"name"})
          private String title;
        }
      }
    }
  }

  @Test
  void verifySimpleExample() throws Exception {
    final Library library = xmlMapper.mapXmlToObject(Library.class);

    assertThat(library).isNotNull();
    assertThat(library.catalog).isNotNull();
    assertThat(library.catalog.libraryItems).hasSize(2);
    assertThat(library.catalog.libraryItems.get(0).articles).hasSize(2);
    assertThat(library.catalog.libraryItems.get(0).articles.get(0).title)
        .isEqualTo("Crossing the Atlantic");
    assertThat(library.catalog.libraryItems.get(0).articles.get(1).title)
        .isEqualTo("The Battle of the Bulge");

    assertThat(library.catalog.libraryItems.get(1).articles).hasSize(2);
    assertThat(library.catalog.libraryItems.get(1).articles.get(0).title)
        .isEqualTo("How to Win Revised");
    assertThat(library.catalog.libraryItems.get(1).articles.get(1).title)
        .isEqualTo("Game of TripleA");
  }
}
