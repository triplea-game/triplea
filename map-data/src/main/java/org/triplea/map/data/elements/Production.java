package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class Production {

  @TagList private List<ProductionRule> productionRules;

  @TagList private List<RepairRule> repairRules;

  @TagList private List<RepairFrontier> repairFrontiers;

  @TagList private List<ProductionFrontier> productionFrontiers;

  @TagList private List<PlayerProduction> playerProductions;

  @TagList private List<PlayerRepair> playerRepairs;

  @Getter
  public static class ProductionRule {
    @Attribute private String name;

    @TagList private List<Cost> costs;

    @TagList private List<Result> results;

    @Getter
    public static class Cost {
      @Attribute private String resource;
      @Attribute private int quantity;
    }

    @Getter
    public static class Result {
      @Attribute private String resourceOrUnit;
      @Attribute private int quantity;
    }
  }

  @Getter
  public static class RepairRule {
    @Attribute private String name;

    @TagList private List<ProductionRule.Cost> costs;

    @TagList private List<ProductionRule.Result> results;

    @Getter
    public static class Cost {
      @Attribute private String resource;
      @Attribute private String quantity;
    }

    @Getter
    public static class Result {
      @Attribute private String resourceOrUnit;
      @Attribute private String quantity;
    }
  }

  @Getter
  public static class RepairFrontier {
    @Attribute private String name;

    @TagList private List<RepairRules> repairRules;

    @Getter
    public static class RepairRules {
      @Attribute private String name;
    }
  }

  @Getter
  public static class ProductionFrontier {
    @Attribute private String name;

    @TagList private List<FrontierRules> frontierRules;

    @Getter
    public static class FrontierRules {
      @Attribute private String name;
    }
  }

  @Getter
  public static class PlayerProduction {
    @Attribute private String player;
    @Attribute private String frontier;
  }

  @Getter
  public static class PlayerRepair {
    @Attribute private String player;
    @Attribute private String frontier;
  }
}
