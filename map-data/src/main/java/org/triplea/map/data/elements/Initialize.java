package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamReader;

public class Initialize {
  static final String TAG_NAME = "InitializeTag";

  private OwnerInitialize ownerInitialize;
  private UnitInitialize unitInitialize;
  private ResourceInitialize resourceInitialize;
  private RelationshipInitialize relationshipInitialize;

  public Initialize(final XMLStreamReader streamReader) {


  }

  static class OwnerInitialize {
    static final String TAG_NAME = "ownerInitialize";



    static class TerritoryOwnerTag {
      static final String TAG_NAME = "territoryOwner";

      private String territory;
      private String owner;
    }

  }

  static class UnitInitialize {
    static final String TAG_NAME = "unitInitialize";

    static class UnitPlacementTag {
      static final String TAG_NAME = "unitPlacement";

    }

    static class HeldUnitsTag {

    }

  }

  static class ResourceInitialize {

  }

  static class RelationshipInitialize {

  }
}
