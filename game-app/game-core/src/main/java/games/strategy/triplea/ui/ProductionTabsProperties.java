package games.strategy.triplea.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.ProductionPanel.Rule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.UrlStreams;
import org.triplea.util.Tuple;

@Slf4j
class ProductionTabsProperties {
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
  // The number of rows of units to be used in the panel if rows or columns are "0" the system will
  // calculate based on
  // max units
  private static final String NUMBER_OF_ROWS = "production_tabs.rows";
  // The number of columns of units to be used in the panel if rows or columns are "0" the system
  // will calculate based
  // on max units
  private static final String NUMBER_OF_COLUMNS = "production_tabs.columns";
  private final Properties properties = new Properties();
  private final List<Rule> rules;
  private List<Tuple<String, List<Rule>>> ruleLists;

  protected ProductionTabsProperties(final GamePlayer gamePlayer, final List<Rule> rules) {
    this.rules = rules;
    final ResourceLoader loader = UiContext.getResourceLoader();
    URL url = loader.getResource(PROPERTY_FILE + "." + gamePlayer.getName() + ".properties");
    if (url == null) {
      // no production_tabs.france.properties check for production_tabs.properties
      final String propertyFile = PROPERTY_FILE + ".properties";
      url = loader.getResource(propertyFile);
      if (url != null) {
        final Optional<InputStream> inputStream = UrlStreams.openStream(url);
        if (inputStream.isPresent()) {
          try {
            properties.load(inputStream.get());
          } catch (final IOException e) {
            log.error("Error reading " + propertyFile, e);
          }
        }
      }
    }
  }

  static ProductionTabsProperties getInstance(final GamePlayer gamePlayer, final List<Rule> rules) {
    return new ProductionTabsProperties(gamePlayer, rules);
  }

  List<Tuple<String, List<Rule>>> getRuleLists() {
    if (ruleLists != null) {
      return ruleLists;
    }
    ruleLists = new ArrayList<>();
    final int numberOfTabs = getNumberOfTabs();
    for (int i = 1; i <= numberOfTabs; i++) {
      final String tabName = properties.getProperty(TAB_NAME + "." + i);
      final List<String> tabValues =
          List.of(properties.getProperty(TAB_UNITS + "." + i).split(":"));
      final List<Rule> ruleList = new ArrayList<>();
      for (final Rule rule : rules) {
        if (tabValues.contains(
            rule.getProductionRule().getResults().keySet().iterator().next().getName())) {
          ruleList.add(rule);
        }
      }
      ruleLists.add(Tuple.of(tabName, ruleList));
    }
    return ruleLists;
  }

  private int getNumberOfTabs() {
    return Integer.parseInt(properties.getProperty(NUMBER_OF_TABS, "0"));
  }

  boolean useDefaultTabs() {
    return Boolean.parseBoolean(properties.getProperty(USE_DEFAULT_TABS, "true"));
  }

  int getRows() {
    return Integer.parseInt(properties.getProperty(NUMBER_OF_ROWS, "0"));
  }

  int getColumns() {
    return Integer.parseInt(properties.getProperty(NUMBER_OF_COLUMNS, "0"));
  }
}
