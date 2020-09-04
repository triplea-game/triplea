package org.triplea.map.data.elements;

import lombok.Getter;
import org.triplea.generic.xml.reader.Tag;

/**
 * Represents all of the org.triplea.map.data read from a map. The org.triplea.map.data is in a
 * 'raw' form where we simply represent the org.triplea.map.data as closely as possible as POJOs
 * without semantic meaning.
 */
@Getter
public class Game {
  @Tag private Info info;
  @Tag private Triplea triplea;
  @Tag private AttachmentList attachmentList;
  @Tag private DiceSides diceSides;
  @Tag private GamePlay gamePlay;
  @Tag private Initialize initialize;
  @Tag private Map map;
  @Tag private ResourceList resourceList;
  @Tag private PlayerList playerList;
  @Tag private UnitList unitList;
  @Tag private RelationshipTypes relationshipTypes;
  @Tag private TerritoryEffectList territoryEffectList;
  @Tag private Production production;
  @Tag private Technology technology;
  @Tag private PropertyList propertyList;
  @Tag private VariableList variableList;
}
