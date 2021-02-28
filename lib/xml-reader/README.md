# Xml-Reader

This subproject contains generic code to simplify and wrap a generic
XML parser.

The parser in this sub-rpoject is based on the Streaming API for XML (StAX):
 <https://en.wikipedia.org/wiki/StAX>

## Using the Xml-Reader, an Example

Mapping XML to POJO objects is done by creating annotated model objects.
The parser library will then use reflection to map XML elements encountered
(tags, attributes, and body content) onto the annotated model objects.

### Example

XML:
```xml
<library>
    <mostRead updated="Yesterday">Strategy Guide</mostRead>

    <inventory type="available">
       <book name="Crossing the Atlantic"/>
       <book name="The Battle of the Bulge"/>
       <dvd name="How to Win Revised"/>
       <dvd name="Game of TripleA"/>
    </inventory>
</library>
```

POJO Model:
```java
@Getter
public class Library {

  @Tag private MostRead mostReadExample;
  @Tag private Inventory libraryInventory;

  @Getter
  public static class MostRead {
    @Attribute private String updated;
    @BodyText private String bodyText;
  }

  public static class Inventory {
    @Attribute private String type;
    @TagList(Dvd.class) private List<Book> books;
    @TagList(Dvd.class) private List<Dvd> dvds;

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
```

Usage:
```java
InputStream xmlInputStream = ... < open file input stream >
XmlMapper mapper = new XmlMapper(xmlInputStream);
Library library = mapper.mapXmlToObject(mapper);
```

## General Notes

- XML tags are matched to model object class names (case insensitive)
- XML attributes are matched to model object attributes by variable name (case insensitive)
- Model POJO objects must have a no-arg constructor.
- Variables annotated with `@BodyText` can have any name
- List parsing maintains XML ordering..

### Missing XML element behavior

- If a `@Tag` is not present in XML it is null.
- If a `@TagList` element is not present in XML the list is initialized to empty.
- If an `@Attribute` is not present in XML the value is initialized.
