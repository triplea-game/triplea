package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.MapRow.Actions;
import games.strategy.engine.framework.map.download.MapTagFilter.Tag;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.swing.JEditorPaneWithClickableLinks;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;

/**
 * The column right of a map list with the selected map: a large preview that slides up out of the
 * way by a click on its lower border, its name and creator, its status, versions, size and tags and
 * its buttons; below them, scrolling, its full description.
 */
final class MapDetails extends JPanel {
  @Serial private static final long serialVersionUID = 6127433920184626413L;
  // the details are never narrower, so the facts and buttons fit; wider windows widen them
  private static final int MIN_WIDTH = 380;
  private static final int PREFERRED_WIDTH = 480;
  private static final int PADDING = 24;

  /** A line of the facts about a map, eg "Size" and "38 MB". */
  record Fact(String label, String value) {}

  private final transient MapPreviews previews;
  private final transient Actions actions;
  private final JLabel status = new JLabel();
  private final PreviewSlider previewSlider = new PreviewSlider();

  /** The preview, name, facts and buttons, which stay at the top. */
  private final JPanel top = new JPanel();

  /** The description, below the buttons, in {@link #bodyScroll}. */
  private final JPanel body = new JPanel();

  private final JScrollPane bodyScroll;

  private transient @Nullable ManagedMap shownMap;
  private @Nullable MapPreviewBox preview;

  MapDetails(final MapPreviews previews, final Actions actions) {
    super(new BorderLayout(0, 16));
    this.previews = previews;
    this.actions = actions;
    setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, LookAndFeel.getSeparatorColor()),
            BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)));
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    top.setOpaque(false);
    add(top, BorderLayout.NORTH);
    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
    body.setOpaque(false);
    // the body keeps its height at the top of a taller viewport instead of stretching its fields
    final JPanel view = new MapScrollPanel();
    view.setLayout(new BorderLayout());
    view.setOpaque(false);
    view.add(body, BorderLayout.NORTH);
    bodyScroll = SwingComponents.newJScrollPane(view);
    bodyScroll.setOpaque(false);
    bodyScroll.getViewport().setOpaque(false);
    bodyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    add(bodyScroll, BorderLayout.CENTER);
    // the preview fills the width of the details
    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            fitPreview();
          }
        });
  }

  private void fitPreview() {
    final int width = getWidth() - getInsets().left - getInsets().right;
    if (preview != null && width > 0 && width != preview.getPreferredSize().width) {
      preview.resize(width);
    }
  }

  /**
   * Shows the map, eg the one selected in the list, or its new status after a change; the same map
   * shown again keeps the scroll position of its description.
   */
  void show(final ManagedMap map) {
    final MapDownloadItem download = map.getMapDownloadItem();
    final MapPreviewBox mapPreview = new MapPreviewBox(previews, download.getPreviewImageUrl());
    if (map.getMapStatus() == ManagedMapStatus.DOWNLOADING) {
      mapPreview.showProgress(0);
    }
    preview = mapPreview;
    fitPreview();
    previewSlider.showPreview(mapPreview);
    status.setText(statusName(map.getMapStatus()));

    top.removeAll();
    addLeftAligned(top, previewSlider);
    top.add(Box.createVerticalStrut(16));
    addLeftAligned(top, new JLabelBuilder(map.getMapName()).bold().adjustFontSize(4).build());
    MapRow.creatorOf(download.getDescription())
        .ifPresent(
            creator ->
                addLeftAligned(
                    top,
                    new JLabelBuilder("by " + creator)
                        .foregroundColor(LookAndFeel.getSecondaryTextColor())
                        .build()));
    top.add(Box.createVerticalStrut(12));
    addLeftAligned(top, newFactsGrid(map));
    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    buttons.setOpaque(false);
    MapRow.newButtons(map, actions).forEach(buttons::add);
    if (buttons.getComponentCount() > 0) {
      top.add(Box.createVerticalStrut(16));
      addLeftAligned(top, buttons);
    }
    final boolean otherMap = shownMap == null || !shownMap.getMapName().equals(map.getMapName());
    shownMap = map;
    if (otherMap) {
      showDescription(map);
    }
    revalidate();
    repaint();
  }

  /** The description, if the map has one, scrolled to the top. */
  private void showDescription(final ManagedMap map) {
    body.removeAll();
    final String description = map.getMapDownloadItem().getDescription();
    if (!MapRow.isMissingDescription(description)) {
      addLeftAligned(body, newDescription(description));
    }
    bodyScroll.getVerticalScrollBar().setValue(0);
  }

  private static void addLeftAligned(final JPanel panel, final JComponent component) {
    component.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(component);
  }

  /** The facts as two columns, the label in the secondary color; the status follows downloads. */
  private JPanel newFactsGrid(final ManagedMap map) {
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setOpaque(false);
    final List<Fact> facts = facts(map);
    for (int row = 0; row < facts.size(); row++) {
      final Fact fact = facts.get(row);
      // tags come from the server, so the facts show their text as written
      final JLabel label = MapRow.newPlainLabel(fact.label());
      label.setForeground(LookAndFeel.getSecondaryTextColor());
      grid.add(
          label,
          new GridBagConstraintsBuilder(0, row)
              .anchor(GridBagConstraintsAnchor.WEST)
              .insets(2, 0, 2, 16)
              .build());
      final JLabel value = row == 0 ? status : MapRow.newPlainLabel(fact.value());
      grid.add(
          value,
          new GridBagConstraintsBuilder(1, row)
              .weightX(1)
              .anchor(GridBagConstraintsAnchor.WEST)
              .insets(2, 0, 2, 0)
              .build());
    }
    return grid;
  }

  /**
   * The facts about the map, the status first, eg "Status: Update available", "Installed version: 3
   * Mar 2024", "Newest version: 9 Jun 2025", "Size: 38 MB" and the tags, eg "Genre: Fantasy, WW2";
   * facts the map lacks are left out.
   */
  static List<Fact> facts(final ManagedMap map) {
    final List<Fact> facts = new ArrayList<>();
    facts.add(new Fact("Status", statusName(map.getMapStatus())));
    if (map.getMapStatus() != ManagedMapStatus.AVAILABLE) {
      // a removed map keeps the installed map that was read at the start
      MapRow.installedDate(map).ifPresent(date -> facts.add(new Fact("Installed version", date)));
    }
    MapRow.newestDate(map.getMapDownloadItem())
        .ifPresent(date -> facts.add(new Fact("Newest version", date)));
    MapRow.sizeText(map.getMapDownloadItem()).ifPresent(size -> facts.add(new Fact("Size", size)));
    // several values of a tag share its line, eg "Genre: Fantasy, WW2"
    Tag.of(map.getMapDownloadItem()).stream()
        .collect(
            Collectors.groupingBy(
                Tag::name,
                LinkedHashMap::new,
                Collectors.mapping(Tag::value, Collectors.joining(", "))))
        .forEach((name, values) -> facts.add(new Fact(name, values)));
    return facts;
  }

  private static String statusName(final ManagedMapStatus mapStatus) {
    return switch (mapStatus) {
      case AVAILABLE -> "Not installed";
      case DOWNLOADING -> "Downloading...";
      case INSTALLED -> "Installed, up to date";
      case UPDATE_AVAILABLE -> "Update available";
      case REMOVING -> "Removing...";
      case ERROR -> "Download failed";
      default -> throw new IllegalStateException("Unknown map status: " + mapStatus);
    };
  }

  /** The full description, which is HTML, in the normal text size; its links open the browser. */
  private static JEditorPane newDescription(final String html) {
    final JEditorPane description = new JEditorPaneWithClickableLinks(html);
    // the text of the app instead of the small default font of HTML
    description.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    description.setFont(UIManager.getFont("Label.font"));
    description.setBorder(BorderFactory.createEmptyBorder());
    description.setCaretPosition(0);
    return description;
  }

  /** Shows the progress of the shown map's download on its preview and in its status. */
  void showProgress(final MapDownloadItem download, final long bytesReceived) {
    if (shownMap == null
        || preview == null
        || !shownMap.getMapName().equals(download.getMapName())) {
      return;
    }
    status.setText(MapRow.progressText(download, bytesReceived));
    preview.showProgress(MapRow.percentOf(download, bytesReceived));
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(PREFERRED_WIDTH, super.getPreferredSize().height);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(MIN_WIDTH, super.getMinimumSize().height);
  }
}
