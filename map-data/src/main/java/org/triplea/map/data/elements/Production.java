package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class Production {

  @TagList(ProductionRule.class)
  private List<ProductionRule> productionRules;

  @TagList(RepairRule.class)
  private List<RepairRule> repairRules;

  @TagList(RepairFrontier.class)
  private List<RepairFrontier> repairFrontiers;

  @TagList(ProductionFrontier.class)
  private List<ProductionFrontier> productionFrontiers;

  @TagList(PlayerProduction.class)
  private List<PlayerProduction> playerProductions;

  @TagList(PlayerRepair.class)
  private List<PlayerRepair> playerRepairs;

  @Getter
  public static class ProductionRule {
    @Attribute private String name;

    @TagList(Cost.class)
    private List<Cost> costs;

    @TagList(Result.class)
    private List<Result> results;

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
  public static class RepairRule {
    @Attribute private String name;

    @TagList(ProductionRule.Cost.class)
    private List<ProductionRule.Cost> costs;

    @TagList(ProductionRule.Result.class)
    private List<ProductionRule.Result> results;

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

    @TagList(RepairRules.class)
    private List<RepairRules> repairRules;

    @Getter
    public static class RepairRules {
      @Attribute private String name;
    }
  }

  @Getter
  public static class ProductionFrontier {
    @Attribute private String name;

    @TagList(FrontierRules.class)
    private List<FrontierRules> frontierRules;

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
