package games.strategy.engine.framework.map.download;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Which tab of the maps window each map is in, decided when the window opens: a map installed in
 * the window stays in "Get more maps", a map uninstalled stays in "Installed", each showing its new
 * status and buttons, until the window opens again. So the maps do not jump away from the player.
 */
final class TabMembership {
  private final Set<String> installedMapNames;

  /**
   * @param maps All maps, as the window opens.
   * @param installed Whether a map belongs to the Installed tab now.
   */
  TabMembership(final Collection<ManagedMap> maps, final Predicate<ManagedMap> installed) {
    installedMapNames =
        maps.stream().filter(installed).map(ManagedMap::getMapName).collect(Collectors.toSet());
  }

  /** The maps of the Installed tab: those that were installed when the window opened. */
  List<ManagedMap> installed(final Collection<ManagedMap> maps) {
    return maps.stream().filter(map -> installedMapNames.contains(map.getMapName())).toList();
  }

  /** The maps of the "Get more maps" tab: all others. */
  List<ManagedMap> toGet(final Collection<ManagedMap> maps) {
    return maps.stream().filter(map -> !installedMapNames.contains(map.getMapName())).toList();
  }
}
