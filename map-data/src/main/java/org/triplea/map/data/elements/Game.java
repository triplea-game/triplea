package org.triplea.map.data.elements;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.triplea.generic.xml.reader.annotations.Tag;

/**
 * Represents all of the org.triplea.map.data read from a map. The org.triplea.map.data is in a
 * 'raw' form where we simply represent the org.triplea.map.data as closely as possible as POJOs
 * without semantic meaning.
 */
@Getter
@XmlRootElement(name = "game")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {
  @XmlElement @Tag private Info info;
  @XmlElement @Tag private Triplea triplea;

  @XmlElement(name = "attachmentList")
  @Tag(names = {"attachmentList", "attatchmentList"})
  private AttachmentList attachmentList;

  @XmlElement @Tag private DiceSides diceSides;
  @XmlElement @Tag private GamePlay gamePlay;
  @XmlElement @Tag private Initialize initialize;
  @XmlElement @Tag private Map map;
  @XmlElement @Tag private ResourceList resourceList;
  @XmlElement @Tag private PlayerList playerList;
  @XmlElement @Tag private UnitList unitList;
  @XmlElement @Tag private RelationshipTypes relationshipTypes;
  @XmlElement @Tag private TerritoryEffectList territoryEffectList;
  @XmlElement @Tag private Production production;
  @XmlElement @Tag private Technology technology;
  @XmlElement @Tag private PropertyList propertyList;
  @XmlElement @Tag private VariableList variableList;
}
