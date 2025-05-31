package games.strategy.triplea.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.TechAdvance;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

/**
 * An attachment for instances of {@link GamePlayer} that defines properties related to technology
 * advances. Note: Empty collection fields default to null to minimize memory use and serialization
 * size.
 */
public class TechAttachment extends DefaultAttachment {
  @Serial private static final long serialVersionUID = -8780929085456199961L;

  // getters
  @Getter private int techCost = 5;
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
  private boolean aaRadar = false;
  private boolean shipyards = false;
  @Getter private final Map<String, Boolean> genericTech = new HashMap<>();

  public TechAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
    setGenericTechs();
  }

  static TechAttachment get(final GamePlayer gamePlayer, final String nameOfAttachment) {
    if (!nameOfAttachment.equals(Constants.TECH_ATTACHMENT_NAME)) {
      throw new IllegalStateException(
          "TechAttachment may not yet get attachments not named: "
              + Constants.TECH_ATTACHMENT_NAME);
    }
    return gamePlayer.getTechAttachment();
  }

  // setters
  public void setTechCost(final String s) {
    techCost = getInt(s);
  }

  private void setTechCost(final Integer s) {
    techCost = s;
  }

  private void resetTechCost() {
    techCost = 5;
  }

  private void setHeavyBomber(final String s) {
    heavyBomber = getBool(s);
  }

  private void setHeavyBomber(final Boolean s) {
    heavyBomber = s;
  }

  private void resetHeavyBomber() {
    heavyBomber = false;
  }

  private void setDestroyerBombard(final String s) {
    destroyerBombard = getBool(s);
  }

  private void setDestroyerBombard(final Boolean s) {
    destroyerBombard = s;
  }

  private void resetDestroyerBombard() {
    destroyerBombard = false;
  }

  private void setLongRangeAir(final String s) {
    longRangeAir = getBool(s);
  }

  private void setLongRangeAir(final Boolean s) {
    longRangeAir = s;
  }

  private void resetLongRangeAir() {
    longRangeAir = false;
  }

  private void setJetPower(final String s) {
    jetPower = getBool(s);
  }

  private void setJetPower(final Boolean s) {
    jetPower = s;
  }

  private void resetJetPower() {
    jetPower = false;
  }

  private void setRocket(final String s) {
    rocket = getBool(s);
  }

  private void setRocket(final Boolean s) {
    rocket = s;
  }

  private void resetRocket() {
    rocket = false;
  }

  private void setIndustrialTechnology(final String s) {
    industrialTechnology = getBool(s);
  }

  private void setIndustrialTechnology(final Boolean s) {
    industrialTechnology = s;
  }

  private void resetIndustrialTechnology() {
    industrialTechnology = false;
  }

  private void setSuperSub(final String s) {
    superSub = getBool(s);
  }

  private void setSuperSub(final Boolean s) {
    superSub = s;
  }

  private void resetSuperSub() {
    superSub = false;
  }

  private void setImprovedArtillerySupport(final String s) {
    improvedArtillerySupport = getBool(s);
  }

  private void setImprovedArtillerySupport(final Boolean s) {
    improvedArtillerySupport = s;
  }

  private void resetImprovedArtillerySupport() {
    improvedArtillerySupport = false;
  }

  public void setParatroopers(final String s) {
    paratroopers = getBool(s);
  }

  private void setParatroopers(final Boolean s) {
    paratroopers = s;
  }

  private void resetParatroopers() {
    paratroopers = false;
  }

  private void setIncreasedFactoryProduction(final String s) {
    increasedFactoryProduction = getBool(s);
  }

  private void setIncreasedFactoryProduction(final Boolean s) {
    increasedFactoryProduction = s;
  }

  private void resetIncreasedFactoryProduction() {
    increasedFactoryProduction = false;
  }

  private void setWarBonds(final String s) {
    warBonds = getBool(s);
  }

  private void setWarBonds(final Boolean s) {
    warBonds = s;
  }

  private void resetWarBonds() {
    warBonds = false;
  }

  public void setMechanizedInfantry(final String s) {
    mechanizedInfantry = getBool(s);
  }

  private void setMechanizedInfantry(final Boolean s) {
    mechanizedInfantry = s;
  }

  private void resetMechanizedInfantry() {
    mechanizedInfantry = false;
  }

  public void setAaRadar(final String s) {
    aaRadar = getBool(s);
  }

  private void setAaRadar(final Boolean s) {
    aaRadar = s;
  }

  private void resetAaRadar() {
    aaRadar = false;
  }

  private void setShipyards(final String s) {
    shipyards = getBool(s);
  }

  private void setShipyards(final Boolean s) {
    shipyards = s;
  }

  private void resetShipyards() {
    shipyards = false;
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
    return aaRadar;
  }

  public boolean getShipyards() {
    return shipyards;
  }

  // custom techs

  private void setGenericTechs() {
    for (final TechAdvance ta : getData().getTechnologyFrontier()) {
      if (ta instanceof GenericTechAdvance && ((GenericTechAdvance) ta).getAdvance() == null) {
        genericTech.put(ta.getProperty().intern(), Boolean.FALSE);
      }
    }
  }

  public Boolean hasGenericTech(final String name) {
    return genericTech.get(name);
  }

  public void setGenericTech(final String name, final boolean value) {
    genericTech.put(name, value);
    getData().getTechTracker().clearCache();
  }

  @Override
  public void validate(final GameState data) {}

  @Override
  public Optional<MutableProperty<?>> getPropertyOrEmpty(final @NonNls String propertyName) {
    return switch (propertyName) {
      case "techCost" ->
          Optional.of(
              MutableProperty.of(
                  this::setTechCost, this::setTechCost, this::getTechCost, this::resetTechCost));
      case "heavyBomber" ->
          Optional.of(
              MutableProperty.of(
                  this::setHeavyBomber,
                  this::setHeavyBomber,
                  this::getHeavyBomber,
                  this::resetHeavyBomber));
      case "longRangeAir" ->
          Optional.of(
              MutableProperty.of(
                  this::setLongRangeAir,
                  this::setLongRangeAir,
                  this::getLongRangeAir,
                  this::resetLongRangeAir));
      case "jetPower" ->
          Optional.of(
              MutableProperty.of(
                  this::setJetPower, this::setJetPower, this::getJetPower, this::resetJetPower));
      case "rocket" ->
          Optional.of(
              MutableProperty.of(
                  this::setRocket, this::setRocket, this::getRocket, this::resetRocket));
      case "industrialTechnology" ->
          Optional.of(
              MutableProperty.of(
                  this::setIndustrialTechnology,
                  this::setIndustrialTechnology,
                  this::getIndustrialTechnology,
                  this::resetIndustrialTechnology));
      case "superSub" ->
          Optional.of(
              MutableProperty.of(
                  this::setSuperSub, this::setSuperSub, this::getSuperSub, this::resetSuperSub));
      case "destroyerBombard" ->
          Optional.of(
              MutableProperty.of(
                  this::setDestroyerBombard,
                  this::setDestroyerBombard,
                  this::getDestroyerBombard,
                  this::resetDestroyerBombard));
      case "improvedArtillerySupport" ->
          Optional.of(
              MutableProperty.of(
                  this::setImprovedArtillerySupport,
                  this::setImprovedArtillerySupport,
                  this::getImprovedArtillerySupport,
                  this::resetImprovedArtillerySupport));
      case "paratroopers" ->
          Optional.of(
              MutableProperty.of(
                  this::setParatroopers,
                  this::setParatroopers,
                  this::getParatroopers,
                  this::resetParatroopers));
      case "increasedFactoryProduction" ->
          Optional.of(
              MutableProperty.of(
                  this::setIncreasedFactoryProduction,
                  this::setIncreasedFactoryProduction,
                  this::getIncreasedFactoryProduction,
                  this::resetIncreasedFactoryProduction));
      case "warBonds" ->
          Optional.of(
              MutableProperty.of(
                  this::setWarBonds, this::setWarBonds, this::getWarBonds, this::resetWarBonds));
      case "mechanizedInfantry" ->
          Optional.of(
              MutableProperty.of(
                  this::setMechanizedInfantry,
                  this::setMechanizedInfantry,
                  this::getMechanizedInfantry,
                  this::resetMechanizedInfantry));
      case "aARadar" ->
          Optional.of(
              MutableProperty.of(
                  this::setAaRadar, this::setAaRadar, this::getAaRadar, this::resetAaRadar));
      case "shipyards" ->
          Optional.of(
              MutableProperty.of(
                  this::setShipyards,
                  this::setShipyards,
                  this::getShipyards,
                  this::resetShipyards));
      default -> Optional.empty();
    };
  }
}
