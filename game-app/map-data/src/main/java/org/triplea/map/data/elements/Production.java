package org.triplea.map.data.elements;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Production {

  @XmlElement(name = "productionRule")
  @TagList
  private List<ProductionRule> productionRules;

  @XmlElement(name = "repairRule")
  @TagList
  private List<RepairRule> repairRules;

  @XmlElement(name = "repairFrontier")
  @TagList
  private List<RepairFrontier> repairFrontiers;

  @XmlElement(name = "productionFrontier")
  @TagList
  private List<ProductionFrontier> productionFrontiers;

  @XmlElement(name = "playerProduction")
  @TagList
  private List<PlayerProduction> playerProductions;

  @XmlElement(name = "playerRepair")
  @TagList
  private List<PlayerRepair> playerRepairs;

  public interface Rule {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class Cost {
      @XmlAttribute @Attribute private String resource;
      @XmlAttribute @Attribute private Integer quantity;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class Result {
      @XmlAttribute @Attribute private String resourceOrUnit;
      @XmlAttribute @Attribute private Integer quantity;
    }

    List<Result> getRuleResults();
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductionRule implements Rule {
    @XmlAttribute @Attribute private String name;

    @XmlElement(name = "cost")
    @TagList
    private List<Cost> costs;

    @XmlElement(name = "result")
    @TagList
    private List<Result> results;

    @Override
    public List<Result> getRuleResults() {
      return results;
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RepairRule implements Rule {
    @XmlAttribute @Attribute private String name;

    @XmlElement(name = "cost")
    @TagList
    private List<ProductionRule.Cost> costs;

    @XmlElement(name = "result")
    @TagList
    private List<ProductionRule.Result> results;

    @Override
    public List<Result> getRuleResults() {
      return results;
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RepairFrontier {
    @XmlAttribute @Attribute private String name;

    @XmlElement @TagList private List<RepairRules> repairRules;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepairRules {
      @XmlAttribute @Attribute private String name;
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductionFrontier {
    @XmlAttribute @Attribute private String name;

    @XmlElement @TagList private List<FrontierRules> frontierRules;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrontierRules {
      @XmlAttribute @Attribute private String name;
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlayerProduction {
    @XmlAttribute @Attribute private String player;
    @XmlAttribute @Attribute private String frontier;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlayerRepair {
    @XmlAttribute @Attribute private String player;
    @XmlAttribute @Attribute private String frontier;
  }
}
