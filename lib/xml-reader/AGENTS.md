# xml-reader

Annotation-driven XML-to-POJO mapping library using StAX streaming. The primary
consumer is `game-app/map-data` (17+ POJO classes for TripleA game XML), with
additional use in `game-core` (`GameParser`) and map utilities.

## Entry Points

- **`XmlMapper`** — main public API. Wraps an `InputStream`, call
  `mapXmlToObject(MyClass.class)` to parse the full document. Implements
  `Closeable`.
- **`XmlScanner`** — lightweight partial-read API. Use
  `scanForAttributeValue(AttributeScannerParameters)` to extract a single
  attribute value without parsing the whole file.

## Annotation System

Annotate POJO fields to declare the XML mapping. All name matching is
**case-insensitive**. Every annotation accepts an optional `names` array for
alternative spellings (backward compatibility).

| Annotation   | Valid field types                              | XML source          |
|--------------|-----------------------------------------------|---------------------|
| `@Tag`       | Any non-primitive, non-collection object       | Single child element |
| `@TagList`   | `List<T>` only                                 | Repeated child elements |
| `@Attribute` | `String`, `int/Integer`, `double/Double`, `boolean/Boolean` | Element attribute |
| `@BodyText`  | `String` only                                  | Text content inside element |
| `@LegacyXml` | Any (marker only)                             | —                   |

### Rules enforced at parse time (`AnnotatedFields` validation)

- One annotation per field.
- At most one `@BodyText` per class.
- `@BodyText` cannot coexist with `@Tag`/`@TagList` on the same class (but can
  coexist with `@Attribute`).
- Default-value type must match field type (e.g., `defaultInt` only on
  `int`/`Integer`).
- POJOs must have a no-arg constructor (can be private).

### Attribute defaults and nullability

| Field type | Missing attribute, no default | Missing attribute, with default |
|------------|-------------------------------|--------------------------------|
| `String`   | `null`                        | default value                  |
| `int`      | `0`                           | `defaultInt` value             |
| `Integer`  | `null`                        | `defaultInt` value             |
| `double`   | `0.0`                         | `defaultDouble` value          |
| `Double`   | `null`                        | `defaultDouble` value          |
| `boolean`  | `false`                       | `defaultBoolean` value         |
| `Boolean`  | `null`                        | `defaultBoolean` value         |

Missing `@Tag` fields → `null`. Missing `@TagList` fields → empty list.

## Parsing Flow

1. `XmlMapper.mapXmlToObject(Class)` instantiates the POJO via reflection.
2. `AnnotatedFields` validates annotations and categorizes fields.
3. `@Attribute` fields are populated from the current element's attributes via
   `AttributeValueCasting`.
4. If only attributes exist, returns immediately.
5. Otherwise `XmlParser` (package-private, StAX event loop) registers callbacks
   for each `@Tag`/`@TagList`/`@BodyText` field and processes child elements
   recursively.

## Exception Hierarchy

| Exception                | Type      | When                                    |
|--------------------------|-----------|-----------------------------------------|
| `XmlReadException`       | Runtime   | IO / system-level errors                |
| `XmlParsingException`    | Checked   | Structural XML errors (includes line/col) |
| `XmlDataException`       | Checked   | Type mismatch (e.g., `"abc"` for int)   |
| `JavaDataModelException` | Runtime   | Invalid POJO annotations or constructor |

## Package Layout

```
org.triplea.generic.xml.reader            XmlMapper, XmlParser, support classes
org.triplea.generic.xml.reader.annotations  @Tag, @Attribute, @TagList, @BodyText, @LegacyXml
org.triplea.generic.xml.reader.exceptions   All 4 exception types
org.triplea.generic.xml.scanner             XmlScanner, AttributeScanner
```

## Dependencies

Only `lib:java-extras` and the Java standard library (`javax.xml.stream` StAX).
