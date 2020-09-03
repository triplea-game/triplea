package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.map.reader.generic.xml.Attribute;
import org.triplea.map.reader.generic.xml.Tag;
import org.triplea.map.reader.generic.xml.TagList;

@Getter
public class Initialize {
  @Tag private OwnerInitialize ownerInitialize;
  @Tag private UnitInitialize unitInitialize;
  @Tag private ResourceInitialize resourceInitialize;
  @Tag private RelationshipInitialize relationshipInitialize;

  @Getter
  public static class OwnerInitialize {
    @TagList(TerritoryOwner.class)
    private List<TerritoryOwner> territoryOwners;

    @Getter
    public static class TerritoryOwner {
      @Attribute private String territory;
      @Attribute private String owner;
    }
  }

  @Getter
  public static class UnitInitialize {
    @TagList(UnitPlacement.class)
    private List<UnitPlacement> unitPlacements;

    @TagList(HeldUnits.class)
    private List<HeldUnits> heldUnits;

    @Getter
    public static class UnitPlacement {
      @Attribute private String unitType;
      @Attribute private String territory;
      @Attribute private String quantity;
      @Attribute private String owner;
      @Attribute private String hitsTaken;
      @Attribute private String unitDamage;
    }

    @Getter
    public static class HeldUnits {
      @Attribute private String unitType;
      @Attribute private String player;
      @Attribute private String quantity;
    }
  }

  @Getter
  public static class ResourceInitialize {
    @TagList(ResourceGiven.class)
    private List<ResourceGiven> resourcesGiven;

    @Getter
    public static class ResourceGiven {
      @Attribute private String player;
      @Attribute private String resource;
      @Attribute private String quantity;
    }
  }

  @Getter
  public static class RelationshipInitialize {
    @TagList(Relationship.class)
    private List<Relationship> relationships;

    @Getter
    public static class Relationship {
      @Attribute private String type;
      @Attribute private String roundValue;
      @Attribute private String player1;
      @Attribute private String player2;
    }
  }
}
