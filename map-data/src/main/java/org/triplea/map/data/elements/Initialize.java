package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import lombok.ToString;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class Initialize {
  @Tag private OwnerInitialize ownerInitialize;
  @Tag private UnitInitialize unitInitialize;
  @Tag private ResourceInitialize resourceInitialize;
  @Tag private RelationshipInitialize relationshipInitialize;

  @Getter
  public static class OwnerInitialize {
    @TagList private List<TerritoryOwner> territoryOwners;

    @Getter
    public static class TerritoryOwner {
      @Attribute private String territory;
      @Attribute private String owner;
    }
  }

  @Getter
  public static class UnitInitialize {
    @TagList private List<UnitPlacement> unitPlacements;

    @TagList private List<HeldUnits> heldUnits;

    @Getter
    @ToString
    public static class UnitPlacement {
      @Attribute private String unitType;
      @Attribute private String territory;
      @Attribute private int quantity;
      @Attribute private String owner;
      @Attribute private int hitsTaken;
      @Attribute private int unitDamage;
    }

    @Getter
    public static class HeldUnits {
      @Attribute private String unitType;
      @Attribute private String player;
      @Attribute private int quantity;
    }
  }

  @Getter
  public static class ResourceInitialize {
    @TagList private List<ResourceGiven> resourcesGiven;

    @Getter
    public static class ResourceGiven {
      @Attribute private String player;
      @Attribute private String resource;
      @Attribute private int quantity;
    }
  }

  @Getter
  public static class RelationshipInitialize {
    @TagList private List<Relationship> relationships;

    @Getter
    public static class Relationship {
      @Attribute private String type;
      @Attribute private int roundValue;
      @Attribute private String player1;
      @Attribute private String player2;
    }
  }
}
