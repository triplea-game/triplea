# map-data

Handles parsing and representation of TripleA map files — both game XML data and YAML map metadata.

## Package Structure

### `org.triplea.map.data.elements` (17 classes)
POJOs that mirror game XML file structure, using both JAXB annotations (`jakarta.xml.bind.annotation`) and custom XML reader annotations (`@Tag`, `@Attribute`, `@TagList`):
- **Core**: `Game`, `Info`, `Triplea`
- **Configuration**: `GamePlay`, `DiceSides`, `Initialize`
- **Content**: `UnitList`, `ResourceList`, `PlayerList`, `Production`, `Technology`, `AttachmentList`
- **Map structure**: `Map` (territories and connections)
- **Advanced**: `TerritoryEffectList`, `RelationshipTypes`, `PropertyList`, `VariableList`

These represent "raw" XML data without semantic interpretation. All use Lombok (`@Getter`, `@Builder`).

### `org.triplea.map.description.file`
Handles `map.yml` metadata files:
- `MapDescriptionYaml` — POJO for map.yml contents (map name, game list with file paths)
- `MapDescriptionYamlReader` / `MapDescriptionYamlWriter` — Read/write map.yml files
- `MapDescriptionYamlGenerator` — Generates map.yml from existing legacy maps
- `SkinDescriptionYaml` — Skin description support

### `org.triplea.map.game.notes`
Manages HTML game notes companion files (`.notes.html`) for game XMLs.

## Dependencies

- `lib:java-extras`, `lib:xml-reader`
