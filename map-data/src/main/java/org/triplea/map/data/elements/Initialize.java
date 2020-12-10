package org.triplea.map.data.elements;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Initialize {
  @XmlElement @Tag private OwnerInitialize ownerInitialize;
  @XmlElement @Tag private UnitInitialize unitInitialize;
  @XmlElement @Tag private ResourceInitialize resourceInitialize;
  @XmlElement @Tag private RelationshipInitialize relationshipInitialize;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OwnerInitialize {
    @XmlElement(name = "territoryOwner")
    @TagList
    private List<TerritoryOwner> territoryOwners;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerritoryOwner {
      @XmlAttribute @Attribute private String territory;
      @XmlAttribute @Attribute private String owner;
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UnitInitialize {
    @XmlElement(name = "unitPlacement")
    @TagList
    private List<UnitPlacement> unitPlacements;

    @XmlElement @TagList private List<HeldUnits> heldUnits;

    @Getter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitPlacement {
      @XmlAttribute @Attribute private String unitType;
      @XmlAttribute @Attribute private String territory;
      @XmlAttribute @Attribute private int quantity;
      @XmlAttribute @Attribute private String owner;
      @XmlAttribute @Attribute private Integer hitsTaken;
      @XmlAttribute @Attribute private Integer unitDamage;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeldUnits {
      @XmlAttribute @Attribute private String unitType;
      @XmlAttribute @Attribute private String player;
      @XmlAttribute @Attribute private int quantity;
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ResourceInitialize {
    @XmlElement(name = "resourceGiven")
    @TagList
    private List<ResourceGiven> resourcesGiven;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceGiven {
      @XmlAttribute @Attribute private String player;
      @XmlAttribute @Attribute private String resource;
      @XmlAttribute @Attribute private int quantity;
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RelationshipInitialize {
    @XmlElement(name = "relationship")
    @TagList
    private List<Relationship> relationships;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Relationship {
      @XmlAttribute @Attribute private String type;
      @XmlAttribute @Attribute private int roundValue;
      @XmlAttribute @Attribute private String player1;
      @XmlAttribute @Attribute private String player2;
    }
  }
}
