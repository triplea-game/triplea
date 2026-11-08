package games.strategy.engine.framework.map.download;

import java.awt.BorderLayout;
import java.io.Serial;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.http.client.lobby.maps.listing.MapTag;

/**
 * The tag button next to the search, "Tags" with the number of checked tags and a small arrow; a
 * click opens the tags of the listed maps to check, under their tag name if there are several
 * names, eg "Category". "All" unchecks them. The menu stays open while tags are checked. Only the
 * player's choice reports a change; {@link #showChecked} does not. Hidden if no map has tags.
 */
final class MapTagFilter extends JPanel {
  @Serial private static final long serialVersionUID = -7741695186314036658L;

  /**
   * A tag of a map as the server sends it, eg the value "WW2" of the tag "Category". A map can have
   * several values of one tag, eg the genres "Fantasy" and "WW2"; each is a tag of its own.
   */
  record Tag(String name, String value) {
    /** The tags of the map in their display order, without those that have no value. */
    static List<Tag> of(final MapDownloadItem download) {
      return download.getMapTags().stream()
          .sorted(Comparator.comparing(MapTag::getDisplayOrder))
          .filter(tag -> !tag.getValue().isBlank())
          .map(
              tag -> new Tag(Objects.requireNonNullElse(tag.getName(), ""), tag.getValue().strip()))
          .toList();
    }
  }

  private final JButton button;
  private final JCheckBoxMenuItem all = new JCheckBoxMenuItem("All");
  private final transient Map<Tag, JCheckBoxMenuItem> items = new LinkedHashMap<>();

  /**
   * @param tags The tags to choose from, see {@link #tagsOf}.
   * @param tagsChosen Gets the checked tags after each change by the player.
   */
  MapTagFilter(final List<Tag> tags, final Consumer<Set<Tag>> tagsChosen) {
    super(new BorderLayout());
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    setVisible(!tags.isEmpty());

    final JPopupMenu menu = new JPopupMenu();
    all.addActionListener(
        e -> {
          showChecked(Set.of());
          tagsChosen.accept(Set.of());
        });
    menu.add(all);
    final boolean grouped = tags.stream().map(Tag::name).distinct().count() > 1;
    for (int index = 0; index < tags.size(); index++) {
      final Tag tag = tags.get(index);
      if (index == 0 || !tag.name().equals(tags.get(index - 1).name())) {
        menu.addSeparator();
        if (grouped) {
          final JMenuItem heading = newPlainMenuItem(new JMenuItem(), tag.name());
          heading.setEnabled(false);
          menu.add(heading);
        }
      }
      final JCheckBoxMenuItem item = newPlainMenuItem(new JCheckBoxMenuItem(), tag.value());
      // a click checks the tag and leaves the menu open for the next one
      item.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", Boolean.TRUE);
      item.addActionListener(
          e -> {
            final Set<Tag> checked = getChecked();
            showChecked(checked);
            tagsChosen.accept(checked);
          });
      items.put(tag, item);
      menu.add(item);
    }
    button = MapViewSwitch.newMenuButton(menu);
    button.setToolTipText("Show only maps with the checked tags");
    add(button, BorderLayout.CENTER);
    showChecked(Set.of());
  }

  /**
   * The text of the server as written: a tag like "&lt;html&gt;..." shows as it is. Swing reads the
   * switch when the text is set, so it comes first.
   */
  private static <T extends JMenuItem> T newPlainMenuItem(final T item, final String text) {
    item.putClientProperty("html.disable", Boolean.TRUE);
    item.setText(text);
    return item;
  }

  private Set<Tag> getChecked() {
    return items.entrySet().stream()
        .filter(entry -> entry.getValue().isSelected())
        .map(Entry::getKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Checks the tags, eg after the button of the other tab changed them; none checks "All". */
  void showChecked(final Set<Tag> checked) {
    all.setSelected(checked.isEmpty());
    items.forEach((tag, item) -> item.setSelected(checked.contains(tag)));
    button.setText(checked.isEmpty() ? "Tags ▾" : "Tags (" + checked.size() + ") ▾");
    button
        .getAccessibleContext()
        .setAccessibleName(checked.isEmpty() ? "Tags" : "Tags, " + checked.size() + " checked");
  }

  /**
   * The tags of the maps to choose from, each once: by tag name, then by value, ignoring case, eg
   * "Category: Napoleonic", "Category: WW2".
   */
  static List<Tag> tagsOf(final Collection<ManagedMap> maps) {
    return maps.stream()
        .flatMap(map -> Tag.of(map.getMapDownloadItem()).stream())
        .distinct()
        .sorted(
            Comparator.comparing(Tag::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Tag::value, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  /**
   * Whether the map has one of the checked values of each checked tag name, eg Category "WW2" or
   * "Napoleonic"; with none checked every map passes, else a map without tags does not.
   */
  static boolean matches(final ManagedMap map, final Set<Tag> checked) {
    final List<Tag> own = Tag.of(map.getMapDownloadItem());
    return checked.stream().collect(Collectors.groupingBy(Tag::name)).values().stream()
        .allMatch(values -> values.stream().anyMatch(own::contains));
  }
}
