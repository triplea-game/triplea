package org.triplea.map.data.elements;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;

@Getter
public class Initialize {
  static final String TAG_NAME = "initialize";

  private OwnerInitialize ownerInitialize;
  private UnitInitialize unitInitialize;
  private ResourceInitialize resourceInitialize;
  private RelationshipInitialize relationshipInitialize;

  public Initialize(final XMLStreamReader streamReader) throws XMLStreamException {
    XmlParser.tag(TAG_NAME)
        .addChildTagHandler(
            OwnerInitialize.TAG_NAME, () -> ownerInitialize = new OwnerInitialize(streamReader))
        .addChildTagHandler(
            UnitInitialize.TAG_NAME, () -> unitInitialize = new UnitInitialize(streamReader))
        .addChildTagHandler(
            ResourceInitialize.TAG_NAME,
            () -> resourceInitialize = new ResourceInitialize(streamReader))
        .addChildTagHandler(
            RelationshipInitialize.TAG_NAME,
            () -> relationshipInitialize = new RelationshipInitialize(streamReader))
        .parse(streamReader);
  }

  @Getter
  public static class OwnerInitialize {
    static final String TAG_NAME = "ownerInitialize";

    private List<TerritoryOwner> territoryOwners = new ArrayList<>();

    public OwnerInitialize(final XMLStreamReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .addChildTagHandler(
              TerritoryOwner.TAG_NAME, () -> territoryOwners.add(new TerritoryOwner(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class TerritoryOwner {
      static final String TAG_NAME = "territoryOwner";

      private String territory;
      private String owner;

      public TerritoryOwner(final XMLStreamReader streamReader) throws XMLStreamException {
        XmlParser.tag(TAG_NAME)
            .addAttributeHandler("territory", value -> territory = value)
            .addAttributeHandler("owner", value -> owner = value)
            .parse(streamReader);
      }
    }
  }

  @Getter
  public static class UnitInitialize {
    static final String TAG_NAME = "unitInitialize";

    private List<UnitPlacement> unitPlacements = new ArrayList<>();
    private List<HeldUnits> heldUnits = new ArrayList<>();

    public UnitInitialize(final XMLStreamReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .addChildTagHandler(
              UnitPlacement.TAG_NAME, () -> unitPlacements.add(new UnitPlacement(streamReader)))
          .addChildTagHandler(HeldUnits.TAG_NAME, () -> heldUnits.add(new HeldUnits(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class UnitPlacement {
      static final String TAG_NAME = "unitPlacement";

      private String unitType;
      private String territory;
      private String quantity;
      private String owner;
      private String hitsTaken;
      private String unitDamage;

      public UnitPlacement(final XMLStreamReader streamReader) throws XMLStreamException {
        XmlParser.tag(TAG_NAME)
            .addAttributeHandler("unitType", value -> unitType = value)
            .addAttributeHandler("territory", value -> territory = value)
            .addAttributeHandler("quantity", value -> quantity = value)
            .addAttributeHandler("owner", value -> owner = value)
            .addAttributeHandler("hitsTaken", value -> hitsTaken = value)
            .addAttributeHandler("unitDamage", value -> unitDamage = value)
            .parse(streamReader);
      }
    }

    @Getter
    public static class HeldUnits {
      static final String TAG_NAME = "heldUnits";

      private String unitType;
      private String player;
      private String quantity;

      public HeldUnits(final XMLStreamReader streamReader) throws XMLStreamException {
        XmlParser.tag(TAG_NAME)
            .addAttributeHandler("unitType", value -> unitType = value)
            .addAttributeHandler("player", value -> player = value)
            .addAttributeHandler("quantity", value -> quantity = value)
            .parse(streamReader);
      }
    }
  }

  @Getter
  public static class ResourceInitialize {
    static final String TAG_NAME = "resourceInitialize";

    private List<ResourceGiven> resourcesGiven = new ArrayList<>();

    public ResourceInitialize(final XMLStreamReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .addChildTagHandler(
              ResourceGiven.TAG_NAME, () -> resourcesGiven.add(new ResourceGiven(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class ResourceGiven {
      static final String TAG_NAME = "resourceGiven";

      private String player;
      private String resource;
      private String quantity;

      public ResourceGiven(final XMLStreamReader streamReader) throws XMLStreamException {
        XmlParser.tag(TAG_NAME)
            .addAttributeHandler("player", value -> player = value)
            .addAttributeHandler("resource", value -> resource = value)
            .addAttributeHandler("quantity", value -> quantity = value)
            .parse(streamReader);
      }
    }
  }

  @Getter
  public static class RelationshipInitialize {
    static final String TAG_NAME = "relationshipInitialize";

    private List<Relationship> relationships = new ArrayList<>();

    public RelationshipInitialize(final XMLStreamReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .addChildTagHandler(
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

      public Relationship(final XMLStreamReader streamReader) throws XMLStreamException {
        XmlParser.tag(TAG_NAME)
            .addAttributeHandler("type", value -> type = value)
            .addAttributeHandler("roundValue", value -> roundValue = value)
            .addAttributeHandler("player1", value -> player1 = value)
            .addAttributeHandler("player2", value -> player2 = value)
            .parse(streamReader);
      }
    }
  }
}
