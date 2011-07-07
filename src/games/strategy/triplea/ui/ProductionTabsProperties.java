package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.ProductionPanel.Rule;
import games.strategy.util.Tuple;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class ProductionTabsProperties {
	// Filename
	private static final String PROPERTY_FILE = "production_tabs";
	// Properties
	private static final String USE_DEFAULT_TABS = "production_tabs.use_default_tabs";
	
	// The number of tabs that should be drawn
	private static final String NUMBER_OF_TABS = "production_tabs.number_of_tabs";
	
	// don't use production_tabs.tab_name=Air but use: 
	// production_tabs.tab_name.1=Air 
	// production_tabs.tab_name.2=Land
	private static final String TAB_NAME = "production_tabs.tab_name";
	
	// don't use production_tabs.tab_units=Infantry:Panzer:Transport but use:
	// production_tabs.tab_units.1=Infantry:Panzer:Transport
	// production_tabs.tab_units.2=Artillery:Fighter:Bomber
	private static final String TAB_UNITS = "production_tabs.tab_units";
	
	// The number of rows of units to be used in the panel if rows or columns are "0" the system will calculate based on max units
	private static final String NUMBER_OF_ROWS = "production_tabs.rows";
	
	// The number of columns of units to be used in the panel if rows or columns are "0" the system will calculate based on max units
	private static final String NUMBER_OF_COLUMNS = "production_tabs.columns";
	
	
	private Properties m_properties = new Properties();
	private List<Rule> m_rules;
	private List<Tuple<String, List<Rule>>> m_ruleLists;

	protected ProductionTabsProperties(PlayerID playerId, List<Rule> mRules, String mapDir) {
		m_rules = mRules;
		ResourceLoader loader = ResourceLoader.getMapresourceLoader(mapDir);
		String propertyFile = PROPERTY_FILE + "." + playerId.getName() + ".properties";
		
        URL url = loader.getResource(propertyFile);
        if(url == null) {
        	// no production_tabs.france.properties check for  production_tabs.properties
        	propertyFile = PROPERTY_FILE + ".properties";
        	url = loader.getResource(propertyFile);
        	if(url == null) {
        	} else {
                try {
					m_properties.load(url.openStream());
				} catch (IOException e) {
	                System.out.println("Error reading " + propertyFile + e);
				}
        	}
        }	
	}

	public static ProductionTabsProperties getInstance(PlayerID playerId, List<Rule> mRules, String mapDir) {
			 return new ProductionTabsProperties(playerId,mRules,mapDir);
	}

	public List<Tuple<String, List<Rule>>> getRuleLists() {
		if(m_ruleLists !=null)
			return m_ruleLists;
		
		m_ruleLists = new ArrayList<Tuple<String,List<Rule>>>();
		
		int iTabs = getNumberOfTabs();
		for (int i = 1; i <= iTabs ; i++) {
			String tabName = m_properties.getProperty(TAB_NAME+"."+i);
			List<String> tabValues = Arrays.asList(m_properties.getProperty(TAB_UNITS+"."+i).split(":"));
			List<Rule> ruleList = new ArrayList<Rule>();
			for(Rule rule:m_rules) {
				if(tabValues.contains(rule.getProductionRule().getResults().keySet().iterator().next().getName())) {
					ruleList.add(rule);
				}
			}
			m_ruleLists.add(new Tuple<String, List<Rule>>(tabName,ruleList));
		}
		return m_ruleLists;
	}


	private int getNumberOfTabs() {
		return Integer.valueOf(m_properties.getProperty(NUMBER_OF_TABS,"0")).intValue();
	}

	public boolean useDefaultTabs() {
		return Boolean.valueOf(m_properties.getProperty(USE_DEFAULT_TABS,"true")).booleanValue();
	}

	public int getRows() {
 		return Integer.valueOf(m_properties.getProperty(NUMBER_OF_ROWS,"0")).intValue();
	}

	public int getColumns() {
		return Integer.valueOf(m_properties.getProperty(NUMBER_OF_COLUMNS,"0")).intValue();
	}

}
