package org.triplea.client.launch.screens.staging;

import java.util.Map;

import games.strategy.net.INode;

/**
 * A model object to represent the mapping of which players will play as which sides.
 * This model object can be used to send data over network, or to render GUI contents.
 */
public class PlayerMapping {

  private Map<String,INode> mapping;

  public void updateCountry(final String country, final INode player) {

  }

  public Map<String,INode> getCountryToPlayerMap() {
    return null; //return countryToPlayerMap;
  }
}
