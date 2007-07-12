/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * DelegateTest.java
 *
 * Created on November 9, 2001, 3:29 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TechAttachment;

import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class DelegateTest extends TestCase
{

	protected GameData m_data;
	
	protected PlayerID british;
	protected PlayerID japanese;
	
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
	protected Territory japanSeaZone;
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
	
	protected UnitType armour;
	protected UnitType infantry;
	protected UnitType transport;
	protected UnitType factory;
	protected UnitType aaGun;
	protected UnitType fighter;
	protected UnitType bomber;
	protected UnitType carrier;

	
	
	protected Resource ipcs;
	
	
	/** Creates new PlaceDelegateTest */
    public DelegateTest(String name) 
	{
		super(name);
    }


	public void setUp() throws Exception
	{
		
		//get the xml file
		URL url = this.getClass().getResource("DelegateTest.xml");
		
		InputStream input= url.openStream();
		m_data = (new GameParser()).parse(input);
        input.close();
		
		british = m_data.getPlayerList().getPlayerID("British");
		british.addAttachment(Constants.TECH_ATTATCHMENT_NAME, new TechAttachment());
		japanese = m_data.getPlayerList().getPlayerID("Japanese");
		japanese.addAttachment(Constants.TECH_ATTATCHMENT_NAME, new TechAttachment());		
		
		northSea = m_data.getMap().getTerritory("North Sea Zone");
		blackSea = m_data.getMap().getTerritory("Black Sea Zone");
		uk = m_data.getMap().getTerritory("United Kingdom");
		japan = m_data.getMap().getTerritory("Japan");
		japanSeaZone = m_data.getMap().getTerritory("Japan Sea Zone");
		brazil = m_data.getMap().getTerritory("Brazil");
		westCanada = m_data.getMap().getTerritory("West Canada");
		germany = m_data.getMap().getTerritory("Germany");
		syria = m_data.getMap().getTerritory("Syria Jordan");
		egypt= m_data.getMap().getTerritory("Anglo Sudan Egypt");
		congo= m_data.getMap().getTerritory("Congo");
		congoSeaZone = m_data.getMap().getTerritory("Congo Sea Zone");
		kenya= m_data.getMap().getTerritory("Kenya-Rhodesia");
		eastAfrica = m_data.getMap().getTerritory("Italian East Africa");
		libya = m_data.getMap().getTerritory("Libya");
		algeria = m_data.getMap().getTerritory("Algeria");
		equatorialAfrica = m_data.getMap().getTerritory("French Equatorial Africa");
		redSea = m_data.getMap().getTerritory("Red Sea Zone");
		westAfrica = m_data.getMap().getTerritory("French West Africa");
		angola = m_data.getMap().getTerritory("Angola");
		angolaSeaZone = m_data.getMap().getTerritory("Angola Sea Zone");
		eastCompass = m_data.getMap().getTerritory("East Compass Sea Zone");
		westCompass = m_data.getMap().getTerritory("West Compass Sea Zone");
		mozambiqueSeaZone  = m_data.getMap().getTerritory("Mozambique Sea Zone");
		eastMediteranean  = m_data.getMap().getTerritory("East Mediteranean Sea Zone");
		indianOcean  = m_data.getMap().getTerritory("Indian Ocean Sea Zone");
		westAfricaSeaZone = m_data.getMap().getTerritory("West Africa Sea Zone");
		southAfrica = m_data.getMap().getTerritory("South Africa");
		saudiArabia = m_data.getMap().getTerritory("Saudi Arabia");
		india = m_data.getMap().getTerritory("India");
		southAtlantic = m_data.getMap().getTerritory("South Atlantic Sea Zone");
		antarticSea = m_data.getMap().getTerritory("Antartic Sea Zone");
		southAfricaSeaZone = m_data.getMap().getTerritory("South Africa Sea Zone");
		southBrazilSeaZone = m_data.getMap().getTerritory("South Brazil Sea Zone");
		russia = m_data.getMap().getTerritory("Russia");
		spain= m_data.getMap().getTerritory("Spain");
		gibraltar = m_data.getMap().getTerritory("Gibraltar");
		
		armour = m_data.getUnitTypeList().getUnitType("armour");
		infantry = m_data.getUnitTypeList().getUnitType("infantry");
		transport = m_data.getUnitTypeList().getUnitType("transport");
		factory = m_data.getUnitTypeList().getUnitType("factory");
		aaGun = m_data.getUnitTypeList().getUnitType("aaGun");
		fighter = m_data.getUnitTypeList().getUnitType("fighter");
		bomber = m_data.getUnitTypeList().getUnitType("bomber");
		carrier = m_data.getUnitTypeList().getUnitType("carrier");
		
		ipcs = m_data.getResourceList().getResource("IPCs");
	}


	public void assertValid(String string)
	{
	    assertNull(string,string);
	}
	
	public void assertError(String string)
	{
	    assertNotNull(string,string);
	}
	
}