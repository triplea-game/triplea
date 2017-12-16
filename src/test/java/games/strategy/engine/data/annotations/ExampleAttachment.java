package games.strategy.engine.data.annotations;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.UnitType;
import games.strategy.util.IntegerMap;

/**
 * Test attachment that demonstrates how @GameProperty is used.
 */
public class ExampleAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -5820318094331518742L;
  private int techCost;
  private boolean heavyBomber;
  private String attribute;
  private IntegerMap<UnitType> givesMovement = new IntegerMap<>();
  @InternalDoNotExport
  private String notAProperty = "str";

  public ExampleAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTechCost(final String techCost) {
    this.techCost = getInt(techCost);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTechCost(final int techCost) {
    this.techCost = techCost;
  }

  public int getTechCost() {
    return techCost;
  }

  public void resetTechCost() {
    techCost = 5;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setHeavyBomber(final String heavyBomber) {
    this.heavyBomber = getBool(heavyBomber);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setHeavyBomber(final boolean heavyBomber) {
    this.heavyBomber = heavyBomber;
  }

  public boolean getHeavyBomber() {
    return heavyBomber;
  }

  public void resetHeavyBomber() {
    heavyBomber = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttribute(final String attribute) {
    this.attribute = attribute;
  }

  public String getAttribute() {
    return attribute;
  }

  public void resetAttribute() {
    attribute = null;
  }

  @InternalDoNotExport
  public void setNotAProperty(final String notAProperty) {
    this.notAProperty = notAProperty;
  }

  public String getNotAProperty() {
    return notAProperty;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setGivesMovement(final String value) {
    final String[] s = value.split(":");
    if (s.length <= 0 || s.length > 2) {
      throw new IllegalStateException("Unit Attachments: givesMovement cannot be empty or have more than two fields");
    }
    final String unitTypeToProduce;
    unitTypeToProduce = s[1];
    // validate that this unit exists in the xml
    final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
    if (ut == null) {
      throw new IllegalStateException("Unit Attachments: No unit called:" + unitTypeToProduce);
    }
    // we should allow positive and negative numbers, since you can give bonuses to units or take away a unit's movement
    final int n = getInt(s[0]);
    givesMovement.add(ut, n);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setGivesMovement(final IntegerMap<UnitType> value) {
    givesMovement = value;
  }

  public IntegerMap<UnitType> getGivesMovement() {
    return givesMovement;
  }

  public void clearGivesMovement() {
    givesMovement.clear();
  }

  public void resetGivesMovement() {
    givesMovement = new IntegerMap<>();
  }

  @Override
  public void validate(final GameData data) {}
}
