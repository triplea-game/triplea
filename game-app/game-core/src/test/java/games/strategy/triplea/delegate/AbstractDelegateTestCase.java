package games.strategy.triplea.delegate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

/**
 * Superclass for fixtures that test delegates.
 *
 * <p>Pre-loads the {@link TestMapGameData#DELEGATE_TEST} save game and provides fields for the
 * most-commonly-accessed players, territories, and unit types.
 */
public abstract class AbstractDelegateTestCase extends AbstractClientSettingTestCase {
  protected GameData gameData = TestMapGameData.DELEGATE_TEST.getGameData();
  protected GamePlayer british = GameDataTestUtil.british(gameData);
  protected GamePlayer japanese = GameDataTestUtil.japanese(gameData);
  protected GamePlayer russians = GameDataTestUtil.russians(gameData);
  protected GamePlayer germans = GameDataTestUtil.germans(gameData);
  protected Territory northSea = gameData.getMap().getTerritoryOrNull("North Sea Zone");
  protected Territory blackSea = gameData.getMap().getTerritoryOrNull("Black Sea Zone");
  protected Territory uk = gameData.getMap().getTerritoryOrNull("United Kingdom");
  protected Territory japan = gameData.getMap().getTerritoryOrNull("Japan");
  protected Territory japanSeaZone = gameData.getMap().getTerritoryOrNull("Japan Sea Zone");
  protected Territory sfeSeaZone = gameData.getMap().getTerritoryOrNull("Soviet Far East Sea Zone");
  protected Territory brazil = gameData.getMap().getTerritoryOrNull("Brazil");
  protected Territory westCanada = gameData.getMap().getTerritoryOrNull("West Canada");
  protected Territory eastCanada = gameData.getMap().getTerritoryOrNull("East Canada");
  protected Territory westCanadaSeaZone =
      gameData.getMap().getTerritoryOrNull("West Canada Sea Zone");
  protected Territory germany = gameData.getMap().getTerritoryOrNull("Germany");
  protected Territory syria = gameData.getMap().getTerritoryOrNull("Syria Jordan");
  protected Territory manchuria = gameData.getMap().getTerritoryOrNull("Manchuria");
  protected Territory egypt = gameData.getMap().getTerritoryOrNull("Anglo Sudan Egypt");
  protected Territory congo = gameData.getMap().getTerritoryOrNull("Congo");
  protected Territory congoSeaZone = gameData.getMap().getTerritoryOrNull("Congo Sea Zone");
  protected Territory northAtlantic =
      gameData.getMap().getTerritoryOrNull("North Atlantic Sea Zone");
  protected Territory westAfricaSea = gameData.getMap().getTerritoryOrNull("West Africa Sea Zone");
  protected Territory kenya = gameData.getMap().getTerritoryOrNull("Kenya-Rhodesia");
  protected Territory eastAfrica = gameData.getMap().getTerritoryOrNull("Italian East Africa");
  protected Territory libya = gameData.getMap().getTerritoryOrNull("Libya");
  protected Territory algeria = gameData.getMap().getTerritoryOrNull("Algeria");
  protected Territory equatorialAfrica =
      gameData.getMap().getTerritoryOrNull("French Equatorial Africa");
  protected Territory redSea = gameData.getMap().getTerritoryOrNull("Red Sea Zone");
  protected Territory westAfrica = gameData.getMap().getTerritoryOrNull("French West Africa");
  protected Territory angola = gameData.getMap().getTerritoryOrNull("Angola");
  protected Territory angolaSeaZone = gameData.getMap().getTerritoryOrNull("Angola Sea Zone");
  protected Territory eastCompass = gameData.getMap().getTerritoryOrNull("East Compass Sea Zone");
  protected Territory westCompass = gameData.getMap().getTerritoryOrNull("West Compass Sea Zone");
  protected Territory mozambiqueSeaZone =
      gameData.getMap().getTerritoryOrNull("Mozambique Sea Zone");
  protected Territory eastMediteranean =
      gameData.getMap().getTerritoryOrNull("East Mediteranean Sea Zone");
  protected Territory indianOcean = gameData.getMap().getTerritoryOrNull("Indian Ocean Sea Zone");
  protected Territory westAfricaSeaZone =
      gameData.getMap().getTerritoryOrNull("West Africa Sea Zone");
  protected Territory southAfrica = gameData.getMap().getTerritoryOrNull("South Africa");
  protected Territory saudiArabia = gameData.getMap().getTerritoryOrNull("Saudi Arabia");
  protected Territory india = gameData.getMap().getTerritoryOrNull("India");
  protected Territory southAtlantic =
      gameData.getMap().getTerritoryOrNull("South Atlantic Sea Zone");
  protected Territory antarticSea = gameData.getMap().getTerritoryOrNull("Antartic Sea Zone");
  protected Territory southAfricaSeaZone =
      gameData.getMap().getTerritoryOrNull("South Africa Sea Zone");
  protected Territory southBrazilSeaZone =
      gameData.getMap().getTerritoryOrNull("South Brazil Sea Zone");
  protected Territory russia = gameData.getMap().getTerritoryOrNull("Russia");
  protected Territory spain = gameData.getMap().getTerritoryOrNull("Spain");
  protected Territory gibraltar = gameData.getMap().getTerritoryOrNull("Gibraltar");
  protected Territory balticSeaZone = gameData.getMap().getTerritoryOrNull("Baltic Sea Zone");
  protected Territory karelia = gameData.getMap().getTerritoryOrNull("Karelia S.S.R.");
  protected Territory westEurope = gameData.getMap().getTerritoryOrNull("West Europe");
  protected Territory finlandNorway = gameData.getMap().getTerritoryOrNull("Finland Norway");
  protected UnitType armour = GameDataTestUtil.armour(gameData);
  protected UnitType infantry = GameDataTestUtil.infantry(gameData);
  protected UnitType transport = GameDataTestUtil.transport(gameData);
  protected UnitType submarine = GameDataTestUtil.submarine(gameData);
  protected UnitType destroyer = GameDataTestUtil.destroyer(gameData);
  protected UnitType factory = GameDataTestUtil.factory(gameData);
  protected UnitType aaGun = GameDataTestUtil.aaGun(gameData);
  protected UnitType fighter = GameDataTestUtil.fighter(gameData);
  protected UnitType bomber = GameDataTestUtil.bomber(gameData);
  protected UnitType battleship = GameDataTestUtil.battleship(gameData);
  protected UnitType carrier = GameDataTestUtil.carrier(gameData);
  protected Resource pus = gameData.getResourceList().getResourceOrThrow("PUs");

  protected AbstractDelegateTestCase() {}

  @BeforeEach
  public void setUp() {
    addTechAttachment(british);
    addTechAttachment(japanese);
    addTechAttachment(russians);
    addTechAttachment(germans);
  }

  protected List<Unit> create(GamePlayer player, UnitType unitType, int quantity) {
    var units = unitType.create(quantity, player);
    player.getUnitCollection().addAll(units);
    return units;
  }

  private void addTechAttachment(final GamePlayer player) {
    player.addAttachment(
        Constants.TECH_ATTACHMENT_NAME,
        new TechAttachment(Constants.TECH_ATTACHMENT_NAME, player, gameData));
  }

  protected static void assertValid(final Optional<String> string) {
    string.ifPresent(Assertions::fail);
  }

  @Deprecated
  protected static void assertValid(final String string) {
    assertNull(string, string);
  }

  protected static void assertError(final Optional<String> string) {
    assertTrue(string.isPresent());
  }

  @Deprecated
  protected static void assertError(final String string) {
    assertNotNull(string, string);
  }
}
