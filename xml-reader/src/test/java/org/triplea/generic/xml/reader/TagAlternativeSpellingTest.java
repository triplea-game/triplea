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

/**
 * Verifies that we can give tags, taglists and attributes alternative names and correctly match
 * XMLs that contain those alternative names.
 */
@SuppressWarnings("UnmatchedTest")
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

    assertThat(library, is(notNullValue()));
    assertThat(library.catalog, is(notNullValue()));
    assertThat(library.catalog.libraryItems, hasSize(2));
    assertThat(library.catalog.libraryItems.get(0).articles, hasSize(2));
    assertThat(
        library.catalog.libraryItems.get(0).articles.get(0).title, is("Crossing the Atlantic"));
    assertThat(
        library.catalog.libraryItems.get(0).articles.get(1).title, is("The Battle of the Bulge"));

    assertThat(library.catalog.libraryItems.get(1).articles, hasSize(2));
    assertThat(library.catalog.libraryItems.get(1).articles.get(0).title, is("How to Win Revised"));
    assertThat(library.catalog.libraryItems.get(1).articles.get(1).title, is("Game of TripleA"));
  }
}
