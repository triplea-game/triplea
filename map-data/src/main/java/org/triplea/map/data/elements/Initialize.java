package org.triplea.map.data.elements;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;
import org.triplea.map.reader.XmlReader;

@Getter
public class Initialize {
  static final String TAG_NAME = "initialize";

  private OwnerInitialize ownerInitialize;
  private UnitInitialize unitInitialize;
  private ResourceInitialize resourceInitialize;
  private RelationshipInitialize relationshipInitialize;

  public Initialize(final XmlReader streamReader) throws XMLStreamException {
    XmlParser.tag(TAG_NAME)
        .childTagHandler(
            OwnerInitialize.TAG_NAME, () -> ownerInitialize = new OwnerInitialize(streamReader))
        .childTagHandler(
            UnitInitialize.TAG_NAME, () -> unitInitialize = new UnitInitialize(streamReader))
        .childTagHandler(
            ResourceInitialize.TAG_NAME,
            () -> resourceInitialize = new ResourceInitialize(streamReader))
        .childTagHandler(
            RelationshipInitialize.TAG_NAME,
            () -> relationshipInitialize = new RelationshipInitialize(streamReader))
        .parse(streamReader);
  }

  @Getter
  public static class OwnerInitialize {
    static final String TAG_NAME = "ownerInitialize";

    private final List<TerritoryOwner> territoryOwners = new ArrayList<>();

    public OwnerInitialize(final XmlReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .childTagHandler(
              TerritoryOwner.TAG_NAME, () -> territoryOwners.add(new TerritoryOwner(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class TerritoryOwner {
      static final String TAG_NAME = "territoryOwner";

      private final String territory;
      private final String owner;

      public TerritoryOwner(final XmlReader streamReader) throws XMLStreamException {
        territory = streamReader.getAttributeValue("territory");
        owner = streamReader.getAttributeValue("owner");
      }
    }
  }

  @Getter
  public static class UnitInitialize {
    static final String TAG_NAME = "unitInitialize";

    private final List<UnitPlacement> unitPlacements = new ArrayList<>();
    private final List<HeldUnits> heldUnits = new ArrayList<>();

    public UnitInitialize(final XmlReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .childTagHandler(
              UnitPlacement.TAG_NAME, () -> unitPlacements.add(new UnitPlacement(streamReader)))
          .childTagHandler(HeldUnits.TAG_NAME, () -> heldUnits.add(new HeldUnits(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class UnitPlacement {
      static final String TAG_NAME = "unitPlacement";

      private final String unitType;
      private final String territory;
      private final String quantity;
      private final String owner;
      private final String hitsTaken;
      private final String unitDamage;

      public UnitPlacement(final XmlReader streamReader) {
        unitType = streamReader.getAttributeValue("unitType");
        territory = streamReader.getAttributeValue("territory");
        quantity = streamReader.getAttributeValue("quantity");
        owner = streamReader.getAttributeValue("owner");
        hitsTaken = streamReader.getAttributeValue("hitsTaken");
        unitDamage = streamReader.getAttributeValue("unitDamage");
      }
    }

    @Getter
    public static class HeldUnits {
      static final String TAG_NAME = "heldUnits";

      private String unitType;
      private String player;
      private String quantity;

      public HeldUnits(final XmlReader streamReader) {
        unitType = streamReader.getAttributeValue("unitType");
        player = streamReader.getAttributeValue("player");
        quantity = streamReader.getAttributeValue("quantity");
      }
    }
  }

  @Getter
  public static class ResourceInitialize {
    static final String TAG_NAME = "resourceInitialize";

    private final List<ResourceGiven> resourcesGiven = new ArrayList<>();

    public ResourceInitialize(final XmlReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .childTagHandler(
              ResourceGiven.TAG_NAME, () -> resourcesGiven.add(new ResourceGiven(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class ResourceGiven {
      static final String TAG_NAME = "resourceGiven";

      private String player;
      private String resource;
      private String quantity;

      public ResourceGiven(final XmlReader streamReader) throws XMLStreamException {
        player = streamReader.getAttributeValue("player");
        resource = streamReader.getAttributeValue("resource");
        quantity = streamReader.getAttributeValue("quantity");
      }
    }
  }

  @Getter
  public static class RelationshipInitialize {
    static final String TAG_NAME = "relationshipInitialize";

    private final List<Relationship> relationships = new ArrayList<>();

    public RelationshipInitialize(final XmlReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .childTagHandler(
              Relationship.TAG_NAME, () -> relationships.add(new Relationship(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class Relationship {
      static final String TAG_NAME = "relationship";

      private String type;
      private String roundValue;
      private String player1;
      private String player2;

      public Relationship(final XmlReader xmlReader) {
        type = xmlReader.getAttributeValue("type");
        roundValue = xmlReader.getAttributeValue("roundValue");
        player1 = xmlReader.getAttributeValue("player1");
        player2 = xmlReader.getAttributeValue("player2");
      }
    }
  }
}
