package games.strategy.triplea.delegate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
  protected Territory northSea = gameData.getMap().getTerritory("North Sea Zone");
  protected Territory blackSea = gameData.getMap().getTerritory("Black Sea Zone");
  protected Territory uk = gameData.getMap().getTerritory("United Kingdom");
  protected Territory japan = gameData.getMap().getTerritory("Japan");
  protected Territory japanSeaZone = gameData.getMap().getTerritory("Japan Sea Zone");
  protected Territory sfeSeaZone = gameData.getMap().getTerritory("Soviet Far East Sea Zone");
  protected Territory brazil = gameData.getMap().getTerritory("Brazil");
  protected Territory westCanada = gameData.getMap().getTerritory("West Canada");
  protected Territory eastCanada = gameData.getMap().getTerritory("East Canada");
  protected Territory westCanadaSeaZone = gameData.getMap().getTerritory("West Canada Sea Zone");
  protected Territory germany = gameData.getMap().getTerritory("Germany");
  protected Territory syria = gameData.getMap().getTerritory("Syria Jordan");
  protected Territory manchuria = gameData.getMap().getTerritory("Manchuria");
  protected Territory egypt = gameData.getMap().getTerritory("Anglo Sudan Egypt");
  protected Territory congo = gameData.getMap().getTerritory("Congo");
  protected Territory congoSeaZone = gameData.getMap().getTerritory("Congo Sea Zone");
  protected Territory northAtlantic = gameData.getMap().getTerritory("North Atlantic Sea Zone");
  protected Territory westAfricaSea = gameData.getMap().getTerritory("West Africa Sea Zone");
  protected Territory kenya = gameData.getMap().getTerritory("Kenya-Rhodesia");
  protected Territory eastAfrica = gameData.getMap().getTerritory("Italian East Africa");
  protected Territory libya = gameData.getMap().getTerritory("Libya");
  protected Territory algeria = gameData.getMap().getTerritory("Algeria");
  protected Territory equatorialAfrica = gameData.getMap().getTerritory("French Equatorial Africa");
  protected Territory redSea = gameData.getMap().getTerritory("Red Sea Zone");
  protected Territory westAfrica = gameData.getMap().getTerritory("French West Africa");
  protected Territory angola = gameData.getMap().getTerritory("Angola");
  protected Territory angolaSeaZone = gameData.getMap().getTerritory("Angola Sea Zone");
  protected Territory eastCompass = gameData.getMap().getTerritory("East Compass Sea Zone");
  protected Territory westCompass = gameData.getMap().getTerritory("West Compass Sea Zone");
  protected Territory mozambiqueSeaZone = gameData.getMap().getTerritory("Mozambique Sea Zone");
  protected Territory eastMediteranean =
      gameData.getMap().getTerritory("East Mediteranean Sea Zone");
  protected Territory indianOcean = gameData.getMap().getTerritory("Indian Ocean Sea Zone");
  protected Territory westAfricaSeaZone = gameData.getMap().getTerritory("West Africa Sea Zone");
  protected Territory southAfrica = gameData.getMap().getTerritory("South Africa");
  protected Territory saudiArabia = gameData.getMap().getTerritory("Saudi Arabia");
  protected Territory india = gameData.getMap().getTerritory("India");
  protected Territory southAtlantic = gameData.getMap().getTerritory("South Atlantic Sea Zone");
  protected Territory antarticSea = gameData.getMap().getTerritory("Antartic Sea Zone");
  protected Territory southAfricaSeaZone = gameData.getMap().getTerritory("South Africa Sea Zone");
  protected Territory southBrazilSeaZone = gameData.getMap().getTerritory("South Brazil Sea Zone");
  protected Territory russia = gameData.getMap().getTerritory("Russia");
  protected Territory spain = gameData.getMap().getTerritory("Spain");
  protected Territory gibraltar = gameData.getMap().getTerritory("Gibraltar");
  protected Territory balticSeaZone = gameData.getMap().getTerritory("Baltic Sea Zone");
  protected Territory karelia = gameData.getMap().getTerritory("Karelia S.S.R.");
  protected Territory westEurope = gameData.getMap().getTerritory("West Europe");
  protected Territory finlandNorway = gameData.getMap().getTerritory("Finland Norway");
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
  protected Resource pus = gameData.getResourceList().getResource("PUs").orElse(null);

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

  protected static void assertValid(final String string) {
    assertNull(string, string);
  }

  protected static void assertError(final String string) {
    assertNotNull(string, string);
  }
}
