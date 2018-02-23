package games.strategy.triplea.attachments;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.TechAdvance;

@MapSupport
public class TechAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -8780929085456199961L;

  // attaches to a PlayerID
  public static TechAttachment get(final PlayerID id) {
    final TechAttachment attachment = id.getTechAttachment();
    // dont crash, as a map xml may not set the tech attachment for all players, so just create a new tech attachment
    // for them
    if (attachment == null) {
      return new TechAttachment();
    }
    return attachment;
  }

  static TechAttachment get(final PlayerID id, final String nameOfAttachment) {
    if (!nameOfAttachment.equals(Constants.TECH_ATTACHMENT_NAME)) {
      throw new IllegalStateException(
          "TechAttachment may not yet get attachments not named:" + Constants.TECH_ATTACHMENT_NAME);
    }
    final TechAttachment attachment = (TechAttachment) id.getAttachment(nameOfAttachment);
    // dont crash, as a map xml may not set the tech attachment for all players, so just create a new tech attachment
    // for them
    if (attachment == null) {
      return new TechAttachment();
    }
    return attachment;
  }

  private int techCost = 5;
  private boolean heavyBomber = false;
  private boolean longRangeAir = false;
  private boolean jetPower = false;
  private boolean rocket = false;
  private boolean industrialTechnology = false;
  private boolean superSub = false;
  private boolean destroyerBombard = false;
  private boolean improvedArtillerySupport = false;
  private boolean paratroopers = false;
  private boolean increasedFactoryProduction = false;
  private boolean warBonds = false;
  private boolean mechanizedInfantry = false;
  private boolean aARadar = false;
  private boolean shipyards = false;
  // do not export at this point. currently map xml cannot
  // define a player having a custom tech at start of game
  @InternalDoNotExport
  private Map<String, Boolean> genericTech = new HashMap<>();

  public TechAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
    setGenericTechs();
  }

  /**
   * @deprecated Since many maps do not include a tech attachment for each player (and no maps include tech attachments
   *             for the Null
   *             Player),
   *             we must ensure a default tech attachment is available for all these players. It is preferred to use the
   *             full
   *             constructor. Do not delete
   *             this.
   *             TODO: create tech attachments all players that don't have one, as the map is initialized.
   */
  @Deprecated
  public TechAttachment() {
    super(Constants.TECH_ATTACHMENT_NAME, null, null);
    // TODO: not having game data, and not having generic techs, causes problems. Fix by creating real tech attachments
    // for all players who
    // are missing them, at the beginning of the game.
  }

  // setters
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTechCost(final String s) {
    techCost = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTechCost(final Integer s) {
    techCost = s;
  }

  public void resetTechCost() {
    techCost = 5;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setHeavyBomber(final String s) {
    heavyBomber = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setHeavyBomber(final Boolean s) {
    heavyBomber = s;
  }

  public void resetHeavyBomber() {
    heavyBomber = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDestroyerBombard(final String s) {
    destroyerBombard = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDestroyerBombard(final Boolean s) {
    destroyerBombard = s;
  }

  public void resetDestroyerBombard() {
    destroyerBombard = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setLongRangeAir(final String s) {
    longRangeAir = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setLongRangeAir(final Boolean s) {
    longRangeAir = s;
  }

  public void resetLongRangeAir() {
    longRangeAir = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setJetPower(final String s) {
    jetPower = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setJetPower(final Boolean s) {
    jetPower = s;
  }

  public void resetJetPower() {
    jetPower = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRocket(final String s) {
    rocket = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRocket(final Boolean s) {
    rocket = s;
  }

  public void resetRocket() {
    rocket = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIndustrialTechnology(final String s) {
    industrialTechnology = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIndustrialTechnology(final Boolean s) {
    industrialTechnology = s;
  }

  public void resetIndustrialTechnology() {
    industrialTechnology = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setSuperSub(final String s) {
    superSub = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setSuperSub(final Boolean s) {
    superSub = s;
  }

  public void resetSuperSub() {
    superSub = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setImprovedArtillerySupport(final String s) {
    improvedArtillerySupport = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setImprovedArtillerySupport(final Boolean s) {
    improvedArtillerySupport = s;
  }

  public void resetImprovedArtillerySupport() {
    improvedArtillerySupport = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setParatroopers(final String s) {
    paratroopers = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setParatroopers(final Boolean s) {
    paratroopers = s;
  }

  public void resetParatroopers() {
    paratroopers = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIncreasedFactoryProduction(final String s) {
    increasedFactoryProduction = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIncreasedFactoryProduction(final Boolean s) {
    increasedFactoryProduction = s;
  }

  public void resetIncreasedFactoryProduction() {
    increasedFactoryProduction = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWarBonds(final String s) {
    warBonds = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWarBonds(final Boolean s) {
    warBonds = s;
  }

  public void resetWarBonds() {
    warBonds = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMechanizedInfantry(final String s) {
    mechanizedInfantry = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMechanizedInfantry(final Boolean s) {
    mechanizedInfantry = s;
  }

  public void resetMechanizedInfantry() {
    mechanizedInfantry = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAARadar(final String s) {
    aARadar = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAARadar(final Boolean s) {
    aARadar = s;
  }

  public void resetAARadar() {
    aARadar = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setShipyards(final String s) {
    shipyards = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setShipyards(final Boolean s) {
    shipyards = s;
  }

  public void resetShipyards() {
    shipyards = false;
  }

  // getters
  public int getTechCost() {
    return techCost;
  }

  public boolean getHeavyBomber() {
    return heavyBomber;
  }

  public boolean getLongRangeAir() {
    return longRangeAir;
  }

  public boolean getJetPower() {
    return jetPower;
  }

  public boolean getRocket() {
    return rocket;
  }

  public boolean getIndustrialTechnology() {
    return industrialTechnology;
  }

  public boolean getSuperSub() {
    return superSub;
  }

  public boolean getDestroyerBombard() {
    return destroyerBombard;
  }

  public boolean getImprovedArtillerySupport() {
    return improvedArtillerySupport;
  }

  public boolean getParatroopers() {
    return paratroopers;
  }

  public boolean getIncreasedFactoryProduction() {
    return increasedFactoryProduction;
  }

  public boolean getWarBonds() {
    return warBonds;
  }

  public boolean getMechanizedInfantry() {
    return mechanizedInfantry;
  }

  public boolean getAARadar() {
    return aARadar;
  }

  public boolean getShipyards() {
    return shipyards;
  }

  // custom techs
  /**
   * Internal use only, is not set by xml or property utils.
   */
  @InternalDoNotExport
  private void setGenericTechs() {
    for (final TechAdvance ta : getData().getTechnologyFrontier()) {
      if (ta instanceof GenericTechAdvance) {
        if (((GenericTechAdvance) ta).getAdvance() == null) {
          genericTech.put(ta.getProperty(), Boolean.FALSE);
        }
      }
    }
  }

  public Boolean hasGenericTech(final String name) {
    return genericTech.get(name);
  }

  /**
   * Internal use only, is not set by xml or property utils.
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @InternalDoNotExport
  public void setGenericTech(final String name, final boolean value) {
    genericTech.put(name, value);
  }

  @InternalDoNotExport
  public void setGenericTech(final HashMap<String, Boolean> value) {
    genericTech = value;
  }

  public Map<String, Boolean> getGenericTech() {
    return genericTech;
  }

  public void clearGenericTech() {
    genericTech.clear();
  }

  @Override
  public void validate(final GameData data) {}

  public static boolean isMechanizedInfantry(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    if (ta == null) {
      return false;
    }
    return ta.getMechanizedInfantry();
  }

  public static boolean isAirTransportable(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    if (ta == null) {
      return false;
    }
    return ta.getParatroopers();
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("techCost",
            MutableProperty.of(
                Integer.class,
                this::setTechCost,
                this::setTechCost,
                this::getTechCost,
                this::resetTechCost))
        .put("heavyBomber",
            MutableProperty.of(
                Boolean.class,
                this::setHeavyBomber,
                this::setHeavyBomber,
                this::getHeavyBomber,
                this::resetHeavyBomber))
        .put("longRangeAir",
            MutableProperty.of(
                Boolean.class,
                this::setLongRangeAir,
                this::setLongRangeAir,
                this::getLongRangeAir,
                this::resetLongRangeAir))
        .put("jetPower",
            MutableProperty.of(
                Boolean.class,
                this::setJetPower,
                this::setJetPower,
                this::getJetPower,
                this::resetJetPower))
        .put("rocket",
            MutableProperty.of(
                Boolean.class,
                this::setRocket,
                this::setRocket,
                this::getRocket,
                this::resetRocket))
        .put("industrialTechnology",
            MutableProperty.of(
                Boolean.class,
                this::setIndustrialTechnology,
                this::setIndustrialTechnology,
                this::getIndustrialTechnology,
                this::resetIndustrialTechnology))
        .put("superSub",
            MutableProperty.of(
                Boolean.class,
                this::setSuperSub,
                this::setSuperSub,
                this::getSuperSub,
                this::resetSuperSub))
        .put("destroyerBombard",
            MutableProperty.of(
                Boolean.class,
                this::setDestroyerBombard,
                this::setDestroyerBombard,
                this::getDestroyerBombard,
                this::resetDestroyerBombard))
        .put("improvedArtillerySupport",
            MutableProperty.of(
                Boolean.class,
                this::setImprovedArtillerySupport,
                this::setImprovedArtillerySupport,
                this::getImprovedArtillerySupport,
                this::resetImprovedArtillerySupport))
        .put("paratroopers",
            MutableProperty.of(
                Boolean.class,
                this::setParatroopers,
                this::setParatroopers,
                this::getParatroopers,
                this::resetParatroopers))
        .put("increasedFactoryProduction",
            MutableProperty.of(
                Boolean.class,
                this::setIncreasedFactoryProduction,
                this::setIncreasedFactoryProduction,
                this::getIncreasedFactoryProduction,
                this::resetIncreasedFactoryProduction))
        .put("warBonds",
            MutableProperty.of(
                Boolean.class,
                this::setWarBonds,
                this::setWarBonds,
                this::getWarBonds,
                this::resetWarBonds))
        .put("mechanizedInfantry",
            MutableProperty.of(
                Boolean.class,
                this::setMechanizedInfantry,
                this::setMechanizedInfantry,
                this::getMechanizedInfantry,
                this::resetMechanizedInfantry))
        .put("aARadar",
            MutableProperty.of(
                Boolean.class,
                this::setAARadar,
                this::setAARadar,
                this::getAARadar,
                this::resetAARadar))
        .put("shipyards",
            MutableProperty.of(
                Boolean.class,
                this::setShipyards,
                this::setShipyards,
                this::getShipyards,
                this::resetShipyards))
        .build();
  }
}
