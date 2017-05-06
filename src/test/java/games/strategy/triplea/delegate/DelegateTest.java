package games.strategy.triplea.delegate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.xml.TestMapGameData;

public class DelegateTest {
  protected GameData gameData;
  protected PlayerID british;
  protected PlayerID japanese;
  protected PlayerID russians;
  protected PlayerID germans;
  protected Territory northSea;
  protected Territory uk;
  protected Territory germany;
  protected Territory japan;
  protected Territory brazil;
  protected Territory westCanada;
  protected Territory egypt;
  protected Territory congo;
  protected Territory kenya;
  protected Territory blackSea;
  protected Territory eastAfrica;
  protected Territory syria;
  protected Territory manchuria;
  protected Territory japanSeaZone;
  protected Territory sfeSeaZone;
  protected Territory libya;
  protected Territory algeria;
  protected Territory equatorialAfrica;
  protected Territory redSea;
  protected Territory westAfrica;
  protected Territory angola;
  protected Territory angolaSeaZone;
  protected Territory eastCompass;
  protected Territory westCompass;
  protected Territory mozambiqueSeaZone;
  protected Territory eastMediteranean;
  protected Territory congoSeaZone;
  protected Territory northAtlantic;
  protected Territory redAtlantic;
  protected Territory westAfricaSea;
  protected Territory indianOcean;
  protected Territory westAfricaSeaZone;
  protected Territory southAfrica;
  protected Territory saudiArabia;
  protected Territory india;
  protected Territory southAtlantic;
  protected Territory southAfricaSeaZone;
  protected Territory antarticSea;
  protected Territory southBrazilSeaZone;
  protected Territory spain;
  protected Territory gibraltar;
  protected Territory russia;
  protected Territory balticSeaZone;
  protected Territory karelia;
  protected Territory westEurope;
  protected Territory finlandNorway;
  protected UnitType armour;
  protected UnitType infantry;
  protected UnitType transport;
  protected UnitType submarine;
  protected UnitType factory;
  protected UnitType aaGun;
  protected UnitType fighter;
  protected UnitType bomber;
  protected UnitType carrier;
  protected Resource pus;

  @Test
  public void setUp() throws Exception {
    gameData = TestMapGameData.DELEGATE_TEST.getGameData();
    british = GameDataTestUtil.british(gameData);
    british.addAttachment(Constants.TECH_ATTACHMENT_NAME, new TechAttachment());
    japanese = GameDataTestUtil.japanese(gameData);
    japanese.addAttachment(Constants.TECH_ATTACHMENT_NAME, new TechAttachment());
    russians = GameDataTestUtil.russians(gameData);
    russians.addAttachment(Constants.TECH_ATTACHMENT_NAME, new TechAttachment());
    germans = GameDataTestUtil.germans(gameData);
    germans.addAttachment(Constants.TECH_ATTACHMENT_NAME, new TechAttachment());
    northSea = gameData.getMap().getTerritory("North Sea Zone");
    blackSea = gameData.getMap().getTerritory("Black Sea Zone");
    uk = gameData.getMap().getTerritory("United Kingdom");
    japan = gameData.getMap().getTerritory("Japan");
    japanSeaZone = gameData.getMap().getTerritory("Japan Sea Zone");
    sfeSeaZone = gameData.getMap().getTerritory("Soviet Far East Sea Zone");
    brazil = gameData.getMap().getTerritory("Brazil");
    westCanada = gameData.getMap().getTerritory("West Canada");
    germany = gameData.getMap().getTerritory("Germany");
    syria = gameData.getMap().getTerritory("Syria Jordan");
    manchuria = gameData.getMap().getTerritory("Manchuria");
    egypt = gameData.getMap().getTerritory("Anglo Sudan Egypt");
    congo = gameData.getMap().getTerritory("Congo");
    congoSeaZone = gameData.getMap().getTerritory("Congo Sea Zone");
    northAtlantic = gameData.getMap().getTerritory("North Atlantic Sea Zone");
    westAfricaSea = gameData.getMap().getTerritory("West Africa Sea Zone");
    kenya = gameData.getMap().getTerritory("Kenya-Rhodesia");
    eastAfrica = gameData.getMap().getTerritory("Italian East Africa");
    libya = gameData.getMap().getTerritory("Libya");
    algeria = gameData.getMap().getTerritory("Algeria");
    equatorialAfrica = gameData.getMap().getTerritory("French Equatorial Africa");
    redSea = gameData.getMap().getTerritory("Red Sea Zone");
    westAfrica = gameData.getMap().getTerritory("French West Africa");
    angola = gameData.getMap().getTerritory("Angola");
    angolaSeaZone = gameData.getMap().getTerritory("Angola Sea Zone");
    eastCompass = gameData.getMap().getTerritory("East Compass Sea Zone");
    westCompass = gameData.getMap().getTerritory("West Compass Sea Zone");
    mozambiqueSeaZone = gameData.getMap().getTerritory("Mozambique Sea Zone");
    eastMediteranean = gameData.getMap().getTerritory("East Mediteranean Sea Zone");
    indianOcean = gameData.getMap().getTerritory("Indian Ocean Sea Zone");
    westAfricaSeaZone = gameData.getMap().getTerritory("West Africa Sea Zone");
    southAfrica = gameData.getMap().getTerritory("South Africa");
    saudiArabia = gameData.getMap().getTerritory("Saudi Arabia");
    india = gameData.getMap().getTerritory("India");
    southAtlantic = gameData.getMap().getTerritory("South Atlantic Sea Zone");
    antarticSea = gameData.getMap().getTerritory("Antartic Sea Zone");
    southAfricaSeaZone = gameData.getMap().getTerritory("South Africa Sea Zone");
    southBrazilSeaZone = gameData.getMap().getTerritory("South Brazil Sea Zone");
    russia = gameData.getMap().getTerritory("Russia");
    spain = gameData.getMap().getTerritory("Spain");
    gibraltar = gameData.getMap().getTerritory("Gibraltar");
    balticSeaZone = gameData.getMap().getTerritory("Baltic Sea Zone");
    karelia = gameData.getMap().getTerritory("Karelia S.S.R.");
    westEurope = gameData.getMap().getTerritory("West Europe");
    finlandNorway = gameData.getMap().getTerritory("Finland Norway");
    armour = GameDataTestUtil.armour(gameData);
    infantry = GameDataTestUtil.infantry(gameData);
    transport = GameDataTestUtil.transport(gameData);
    submarine = GameDataTestUtil.submarine(gameData);
    factory = GameDataTestUtil.factory(gameData);
    aaGun = GameDataTestUtil.aaGun(gameData);
    fighter = GameDataTestUtil.fighter(gameData);
    bomber = GameDataTestUtil.bomber(gameData);
    carrier = GameDataTestUtil.carrier(gameData);
    pus = gameData.getResourceList().getResource("PUs");
  }

  public void assertValid(final String string) {
    assertNull(string, string);
  }

  public void assertError(final String string) {
    assertNotNull(string, string);
  }

  protected ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, gameData);
  }

  @Test
  public void testTest() {
    assertValid(null);
    assertError("Cannot do this");
  }
}
