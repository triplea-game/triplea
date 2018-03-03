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
  private void setTechCost(final Integer s) {
    techCost = s;
  }

  private void resetTechCost() {
    techCost = 5;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setHeavyBomber(final String s) {
    heavyBomber = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setHeavyBomber(final Boolean s) {
    heavyBomber = s;
  }

  private void resetHeavyBomber() {
    heavyBomber = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setDestroyerBombard(final String s) {
    destroyerBombard = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setDestroyerBombard(final Boolean s) {
    destroyerBombard = s;
  }

  private void resetDestroyerBombard() {
    destroyerBombard = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setLongRangeAir(final String s) {
    longRangeAir = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setLongRangeAir(final Boolean s) {
    longRangeAir = s;
  }

  private void resetLongRangeAir() {
    longRangeAir = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setJetPower(final String s) {
    jetPower = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setJetPower(final Boolean s) {
    jetPower = s;
  }

  private void resetJetPower() {
    jetPower = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setRocket(final String s) {
    rocket = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setRocket(final Boolean s) {
    rocket = s;
  }

  private void resetRocket() {
    rocket = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setIndustrialTechnology(final String s) {
    industrialTechnology = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setIndustrialTechnology(final Boolean s) {
    industrialTechnology = s;
  }

  private void resetIndustrialTechnology() {
    industrialTechnology = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setSuperSub(final String s) {
    superSub = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setSuperSub(final Boolean s) {
    superSub = s;
  }

  private void resetSuperSub() {
    superSub = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setImprovedArtillerySupport(final String s) {
    improvedArtillerySupport = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setImprovedArtillerySupport(final Boolean s) {
    improvedArtillerySupport = s;
  }

  private void resetImprovedArtillerySupport() {
    improvedArtillerySupport = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setParatroopers(final String s) {
    paratroopers = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setParatroopers(final Boolean s) {
    paratroopers = s;
  }

  private void resetParatroopers() {
    paratroopers = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setIncreasedFactoryProduction(final String s) {
    increasedFactoryProduction = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setIncreasedFactoryProduction(final Boolean s) {
    increasedFactoryProduction = s;
  }

  private void resetIncreasedFactoryProduction() {
    increasedFactoryProduction = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setWarBonds(final String s) {
    warBonds = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setWarBonds(final Boolean s) {
    warBonds = s;
  }

  private void resetWarBonds() {
    warBonds = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMechanizedInfantry(final String s) {
    mechanizedInfantry = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setMechanizedInfantry(final Boolean s) {
    mechanizedInfantry = s;
  }

  private void resetMechanizedInfantry() {
    mechanizedInfantry = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAaRadar(final String s) {
    aARadar = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setAaRadar(final Boolean s) {
    aARadar = s;
  }

  private void resetAaRadar() {
    aARadar = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setShipyards(final String s) {
    shipyards = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  private void setShipyards(final Boolean s) {
    shipyards = s;
  }

  private void resetShipyards() {
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

  public boolean getAaRadar() {
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

  @Override
  public void validate(final GameData data) {}

  public static boolean isMechanizedInfantry(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    return ta != null && ta.getMechanizedInfantry();
  }

  public static boolean isAirTransportable(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    return ta != null && ta.getParatroopers();
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("techCost",
            MutableProperty.of(
                this::setTechCost,
                this::setTechCost,
                this::getTechCost,
                this::resetTechCost))
        .put("heavyBomber",
            MutableProperty.of(
                this::setHeavyBomber,
                this::setHeavyBomber,
                this::getHeavyBomber,
                this::resetHeavyBomber))
        .put("longRangeAir",
            MutableProperty.of(
                this::setLongRangeAir,
                this::setLongRangeAir,
                this::getLongRangeAir,
                this::resetLongRangeAir))
        .put("jetPower",
            MutableProperty.of(
                this::setJetPower,
                this::setJetPower,
                this::getJetPower,
                this::resetJetPower))
        .put("rocket",
            MutableProperty.of(
                this::setRocket,
                this::setRocket,
                this::getRocket,
                this::resetRocket))
        .put("industrialTechnology",
            MutableProperty.of(
                this::setIndustrialTechnology,
                this::setIndustrialTechnology,
                this::getIndustrialTechnology,
                this::resetIndustrialTechnology))
        .put("superSub",
            MutableProperty.of(
                this::setSuperSub,
                this::setSuperSub,
                this::getSuperSub,
                this::resetSuperSub))
        .put("destroyerBombard",
            MutableProperty.of(
                this::setDestroyerBombard,
                this::setDestroyerBombard,
                this::getDestroyerBombard,
                this::resetDestroyerBombard))
        .put("improvedArtillerySupport",
            MutableProperty.of(
                this::setImprovedArtillerySupport,
                this::setImprovedArtillerySupport,
                this::getImprovedArtillerySupport,
                this::resetImprovedArtillerySupport))
        .put("paratroopers",
            MutableProperty.of(
                this::setParatroopers,
                this::setParatroopers,
                this::getParatroopers,
                this::resetParatroopers))
        .put("increasedFactoryProduction",
            MutableProperty.of(
                this::setIncreasedFactoryProduction,
                this::setIncreasedFactoryProduction,
                this::getIncreasedFactoryProduction,
                this::resetIncreasedFactoryProduction))
        .put("warBonds",
            MutableProperty.of(
                this::setWarBonds,
                this::setWarBonds,
                this::getWarBonds,
                this::resetWarBonds))
        .put("mechanizedInfantry",
            MutableProperty.of(
                this::setMechanizedInfantry,
                this::setMechanizedInfantry,
                this::getMechanizedInfantry,
                this::resetMechanizedInfantry))
        .put("aARadar",
            MutableProperty.of(
                this::setAaRadar,
                this::setAaRadar,
                this::getAaRadar,
                this::resetAaRadar))
        .put("shipyards",
            MutableProperty.of(
                this::setShipyards,
                this::setShipyards,
                this::getShipyards,
                this::resetShipyards))
        .build();
  }
}
