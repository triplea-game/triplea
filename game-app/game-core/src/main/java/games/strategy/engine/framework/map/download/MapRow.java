package games.strategy.engine.framework.map.download;

import com.formdev.flatlaf.FlatClientProperties;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.MapPreviews.Caches;
import games.strategy.engine.framework.map.download.MapPreviews.Size;
import games.strategy.engine.framework.map.download.MapTagFilter.Tag;
import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serial;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;

/**
 * One map in a list of the maps window, in the view the player chose: a compact row (name, creator,
 * status, a small button), a row with its preview, a line about it, its tags and its buttons, or a
 * tile with a large preview on top. A click selects it, which shows the map in the details;
 * ctrl-click and shift-click select several maps.
 */
final class MapRow extends JPanel {
  @Serial private static final long serialVersionUID = 4466510936120385542L;
  private static final int ABOUT_LENGTH = 100;
  private static final int PROGRESS_HEIGHT = 3;
  private static final int TILE_PADDING = 12;
  private static final int SELECTION_ARC = 12;
  private static final String SEPARATOR = " · ";
  private static final String NO_DESCRIPTION = "No description available";
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneOffset.UTC);
  private static final Pattern INVISIBLE_HTML =
      Pattern.compile("<(style|script)[^>]*>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
  private static final Pattern LINE_BREAK_TAG =
      Pattern.compile("<\\s*/?\\s*(?:br|p|div|li|tr|h[1-6])\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0]+");
  private static final Pattern SPACES = Pattern.compile("[ \\t\\x0B\\f\\r\\u00A0]+");
  private static final Pattern SENTENCE_END = Pattern.compile("[.!?](\\s|$)");
  // a name ends before "(", ",", ".", "&", " and ", a line break or the like; a second word of it
  // must be capitalized and on the same line
  private static final String NAME_WORD = "[^\\s(),.&;:!?/]+";
  private static final String CAPITALIZED_WORD = "\\p{Lu}[^\\s(),.&;:!?/]*";
  private static final Pattern AUTHOR =
      Pattern.compile(
          "\\b(?i:author|creator)(?:s|\\(s\\))?[ ]*:\\s*("
              + NAME_WORD
              + "(?: +"
              + CAPITALIZED_WORD
              + ")?)");
  private static final Pattern BY =
      Pattern.compile("\\b(?i:by) +(" + CAPITALIZED_WORD + "(?: +" + CAPITALIZED_WORD + ")?)");

  /**
   * What the buttons of a row do; updating a map installs its newest version, cancelling stops the
   * map's download.
   */
  record Actions(
      Consumer<Collection<ManagedMap>> install,
      Consumer<ManagedMap> cancel,
      Consumer<ManagedMap> remove) {}

  /** What the list does on a click, a ctrl-click and a shift-click on the row. */
  record Listener(
      Consumer<MapRow> clicked, Consumer<MapRow> ctrlClicked, Consumer<MapRow> shiftClicked) {}

  @Getter private final transient ManagedMap map;
  private final MapBrowserView view;
  private final JLabel status;
  // the compact list has no preview; it shows the download as a bar at the bottom of the row
  private final @Nullable MapPreviewBox preview;
  // the percent of the download in the compact list, or -1 while the map does not download
  private int downloadPercent = -1;
  private boolean selected;

  MapRow(
      final ManagedMap map,
      final MapBrowserView view,
      final Caches previews,
      final Actions actions,
      final Listener listener) {
    this.map = map;
    this.view = view;
    setOpaque(false);
    setAlignmentX(LEFT_ALIGNMENT);
    status = newStatusLabel(map, view);
    final String previewUrl = map.getMapDownloadItem().getPreviewImageUrl();
    preview =
        switch (view) {
          case LIST -> null;
          case LIST_WITH_PICTURES -> new MapPreviewBox(previews.rows(), previewUrl);
          case TILES -> new MapPreviewBox(previews.tiles(), previewUrl);
          default -> throw new IllegalStateException("Unknown view: " + view);
        };
    if (map.getMapStatus() == ManagedMapStatus.DOWNLOADING) {
      showPercent(0);
    }
    switch (view) {
      case LIST -> layOutCompact(actions);
      case LIST_WITH_PICTURES -> layOutWithPicture(actions);
      case TILES -> layOutTile(actions);
      default -> throw new IllegalStateException("Unknown view: " + view);
    }
    selectOnClick(this, newClickListener(listener));
  }

  private static JLabel newStatusLabel(final ManagedMap map, final MapBrowserView view) {
    final String text = view == MapBrowserView.TILES ? shortStatusText(map) : statusText(map);
    // an empty status, eg a map without a known size, keeps its line, so all rows line up
    final JLabel label = new JLabel(text.isEmpty() ? " " : text);
    if (map.getMapStatus() == ManagedMapStatus.UPDATE_AVAILABLE) {
      label.setFont(label.getFont().deriveFont(Font.BOLD));
    } else {
      label.setForeground(LookAndFeel.getSecondaryTextColor());
    }
    return label;
  }

  private void showPercent(final int percent) {
    if (preview == null) {
      downloadPercent = percent;
      repaint();
    } else {
      preview.showProgress(percent);
    }
  }

  /** Name and creator over the status, a small button on the right; no picture. */
  private void layOutCompact(final Actions actions) {
    setLayout(new BorderLayout(16, 0));
    setBorder(newRowBorder(8));
    final JPanel texts = newTexts();
    texts.add(newTitle(map));
    texts.add(status);
    add(centeredVertically(texts), BorderLayout.CENTER);
    add(centeredVertically(newSmallButtons(map, actions)), BorderLayout.EAST);
  }

  private static Border newRowBorder(final int verticalPadding) {
    return BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, LookAndFeel.getSeparatorColor()),
        BorderFactory.createEmptyBorder(verticalPadding, 12, verticalPadding, 12));
  }

  private static JPanel newTexts() {
    final JPanel texts = new JPanel();
    texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
    texts.setOpaque(false);
    return texts;
  }

  /** The name in bold, followed by its creator, eg "by Ebbe", if the description names one. */
  private static JPanel newTitle(final ManagedMap map) {
    final JPanel title = new JPanel();
    title.setLayout(new BoxLayout(title, BoxLayout.X_AXIS));
    title.setOpaque(false);
    title.setAlignmentX(LEFT_ALIGNMENT);
    title.add(new JLabelBuilder(map.getMapName()).bold().adjustFontSize(1).build());
    creatorOf(map.getMapDownloadItem().getDescription())
        .ifPresent(
            creator -> {
              title.add(Box.createHorizontalStrut(8));
              title.add(newCreatorLabel(creator));
            });
    return title;
  }

  private static JLabel newCreatorLabel(final String creator) {
    return new JLabelBuilder("by " + creator)
        .foregroundColor(LookAndFeel.getSecondaryTextColor())
        .build();
  }

  /** The buttons of the map in the small style, for the compact list and the tiles. */
  private static JPanel newSmallButtons(final ManagedMap map, final Actions actions) {
    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    buttons.setOpaque(false);
    newButtons(map, actions)
        .forEach(
            button -> {
              LookAndFeel.setSmallStyle(button);
              buttons.add(button);
            });
    // as high as a button also without one, eg while the map is removed, so all rows and tiles
    // have the same height
    final JButton sample = newButton("Install", () -> {});
    LookAndFeel.setSmallStyle(sample);
    buttons.setPreferredSize(
        new Dimension(
            buttons.getPreferredSize().width,
            Math.max(buttons.getPreferredSize().height, sample.getPreferredSize().height)));
    return buttons;
  }

  /**
   * The preview on the left, then name, a line about the map, status and tags, then the buttons.
   */
  private void layOutWithPicture(final Actions actions) {
    setLayout(new BorderLayout(20, 0));
    setBorder(newRowBorder(15));
    final MapDownloadItem download = map.getMapDownloadItem();
    final JPanel texts = newTexts();
    texts.add(newTitle(map));
    if (!isMissingDescription(download.getDescription())) {
      final String about = shortDescription(download.getDescription());
      final JLabel aboutLabel =
          new JLabelBuilder(about).foregroundColor(LookAndFeel.getSecondaryTextColor()).build();
      // the line is cut where the row ends; the tooltip shows all of it
      aboutLabel.setToolTipText(about);
      texts.add(aboutLabel);
    }
    texts.add(status);
    texts.add(newTags(map));

    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    buttons.setOpaque(false);
    newButtons(map, actions).forEach(buttons::add);

    add(preview, BorderLayout.WEST);
    add(centeredVertically(texts), BorderLayout.CENTER);
    add(centeredVertically(buttons), BorderLayout.EAST);
  }

  /**
   * The map's tags as small chips, one for each value, eg "Fantasy" and "WW2" for two genres; chips
   * that do not fit the line are left out.
   */
  private static JPanel newTags(final ManagedMap map) {
    // one row: a flow layout puts what does not fit into a second row, outside of the line
    final JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    line.setOpaque(false);
    line.setAlignmentX(LEFT_ALIGNMENT);
    Tag.of(map.getMapDownloadItem())
        .forEach(
            tag -> {
              if (line.getComponentCount() > 0) {
                line.add(Box.createHorizontalStrut(8));
              }
              line.add(newTagChip(tag));
            });
    return line;
  }

  /** A tag as a small pill in the theme's colors (FlatLaf only; else a plain label). */
  private static JLabel newTagChip(final Tag tag) {
    final JLabel chip = newPlainLabel(tag.value());
    LookAndFeel.setSmallStyle(chip);
    // the text color, faded, is a quiet fill in light and dark themes
    chip.putClientProperty(
        FlatClientProperties.STYLE, "arc: 999; background: fade($Label.foreground, 10%)");
    chip.setBorder(BorderFactory.createEmptyBorder(1, 7, 1, 7));
    chip.getAccessibleContext().setAccessibleName(tag.name() + ": " + tag.value());
    return chip;
  }

  /**
   * A label with the text as written, eg the server's: "&lt;html&gt;..." shows as it is. Swing
   * reads the switch when the text is set, so it comes first.
   */
  static JLabel newPlainLabel(final String text) {
    final JLabel label = new JLabel();
    label.putClientProperty("html.disable", Boolean.TRUE);
    label.setText(text);
    return label;
  }

  /** A card: the large preview on top, below it name, creator, status and tags and a button. */
  private void layOutTile(final Actions actions) {
    setLayout(new BorderLayout(0, 8));
    setBorder(
        BorderFactory.createEmptyBorder(TILE_PADDING, TILE_PADDING, TILE_PADDING, TILE_PADDING));
    // one line each, cut with "..." at the width of the tile
    final JPanel texts = new JPanel(new GridLayout(0, 1, 0, 2));
    texts.setOpaque(false);
    texts.add(new JLabelBuilder(map.getMapName()).bold().adjustFontSize(1).build());
    // the creator line is kept empty if the description names none, so all tiles line up
    texts.add(
        creatorOf(map.getMapDownloadItem().getDescription())
            .map(MapRow::newCreatorLabel)
            .orElseGet(() -> new JLabel(" ")));
    texts.add(status);
    texts.add(newTags(map));
    final JPanel buttons = newSmallButtons(map, actions);
    ((FlowLayout) buttons.getLayout()).setAlignment(FlowLayout.LEFT);

    add(preview, BorderLayout.NORTH);
    add(texts, BorderLayout.CENTER);
    add(buttons, BorderLayout.SOUTH);
  }

  /**
   * Whether the map has no description: the map server sends "No description available for:
   * &lt;url&gt;." for those maps.
   */
  static boolean isMissingDescription(final String html) {
    final String text = plainText(html);
    return text.isEmpty() || text.startsWith(NO_DESCRIPTION);
  }

  private static String plainText(final String html) {
    final String withoutTags =
        HTML_TAG.matcher(INVISIBLE_HTML.matcher(html).replaceAll(" ")).replaceAll(" ");
    return WHITESPACE.matcher(StringEscapeUtils.unescapeHtml4(withoutTags)).replaceAll(" ").strip();
  }

  /**
   * Returns the description as plain text, cut after its first sentence or about 100 characters, eg
   * "Fight World War II on a global map." The description of a map is HTML.
   */
  static String shortDescription(final String html) {
    final String text = plainText(html);
    final Matcher sentenceEnd = SENTENCE_END.matcher(text);
    final String sentence = sentenceEnd.find() ? text.substring(0, sentenceEnd.start() + 1) : text;
    if (sentence.length() <= ABOUT_LENGTH) {
      return sentence;
    }
    final int lastSpace = sentence.lastIndexOf(' ', ABOUT_LENGTH);
    return sentence.substring(0, lastSpace > 0 ? lastSpace : ABOUT_LENGTH).strip() + "…";
  }

  /**
   * Returns the creator of the map as its description names it, loosely: the name after the first
   * "Author:" or "Creator:", else the up to two capitalized words after the first "by", eg "Ebbe"
   * from "Steam &amp; Steel by Ebbe (graphics) &amp; TheDog". The map server sends no creator.
   */
  static Optional<String> creatorOf(final String descriptionHtml) {
    if (isMissingDescription(descriptionHtml)) {
      return Optional.empty();
    }
    final String text = textWithLines(descriptionHtml);
    final Matcher author = AUTHOR.matcher(text);
    if (author.find()) {
      return Optional.of(author.group(1));
    }
    final Matcher by = BY.matcher(text);
    return by.find() ? Optional.of(by.group(1)) : Optional.empty();
  }

  /** The description as plain text that keeps its line breaks, so a name ends with its line. */
  private static String textWithLines(final String html) {
    final String withLines =
        LINE_BREAK_TAG.matcher(INVISIBLE_HTML.matcher(html).replaceAll(" ")).replaceAll("\n");
    final String withoutTags = HTML_TAG.matcher(withLines).replaceAll(" ");
    return SPACES.matcher(StringEscapeUtils.unescapeHtml4(withoutTags)).replaceAll(" ");
  }

  /**
   * Returns the status line, eg "Installed", "38 MB" for a map to get, or "Update available · yours
   * from 3 Mar 2024 · newest from 9 Jun 2025".
   */
  static String statusText(final ManagedMap map) {
    return switch (map.getMapStatus()) {
      case AVAILABLE -> sizeText(map.getMapDownloadItem()).orElse("");
      case DOWNLOADING -> "Downloading...";
      case INSTALLED -> "Installed";
      case UPDATE_AVAILABLE -> updateText(map);
      case REMOVING -> "Removing...";
      case ERROR -> "Download failed";
      default -> throw new IllegalStateException("Unknown map status: " + map.getMapStatus());
    };
  }

  static Optional<String> sizeText(final MapDownloadItem download) {
    return download.getDownloadSizeInBytes() > 0
        ? Optional.of(FileUtils.byteCountToDisplaySize(download.getDownloadSizeInBytes()))
        : Optional.empty();
  }

  private static String updateText(final ManagedMap map) {
    final List<String> parts = new ArrayList<>(List.of("Update available"));
    installedDate(map).ifPresent(installed -> parts.add("yours from " + installed));
    newestDate(map.getMapDownloadItem()).ifPresent(newest -> parts.add("newest from " + newest));
    return String.join(SEPARATOR, parts);
  }

  /** The date of the installed version of the map, eg "3 Mar 2024", if it is installed. */
  static Optional<String> installedDate(final ManagedMap map) {
    return Optional.ofNullable(map.getInstalledMap())
        .flatMap(InstalledMap::getLastModifiedDate)
        .map(DATE_FORMAT::format);
  }

  /** The date of the newest version of the map, eg "9 Jun 2025", if the server sends it. */
  static Optional<String> newestDate(final MapDownloadItem download) {
    final long newest = download.getLastCommitDateEpochMilli();
    return newest > 0
        ? Optional.of(DATE_FORMAT.format(Instant.ofEpochMilli(newest)))
        : Optional.empty();
  }

  /**
   * Returns the short status of a tile, eg "Update available" instead of the dates of both
   * versions.
   */
  static String shortStatusText(final ManagedMap map) {
    return map.getMapStatus() == ManagedMapStatus.UPDATE_AVAILABLE
        ? "Update available"
        : statusText(map);
  }

  /**
   * The buttons of the map in every view and the details: Install, Uninstall, Uninstall and Update
   * (primary) for an out-of-date map, or Cancel for a download.
   */
  static List<JButton> newButtons(final ManagedMap map, final Actions actions) {
    return switch (map.getMapStatus()) {
      case AVAILABLE -> List.of(newButton("Install", () -> actions.install().accept(List.of(map))));
      case ERROR -> {
        // a failed update leaves the installed map in place: it can still be uninstalled
        final JButton retry = newButton("Install", () -> actions.install().accept(List.of(map)));
        yield map.getInstalledMap() == null
            ? List.of(retry)
            : List.of(newUninstallButton(map, actions), retry);
      }
      case INSTALLED -> List.of(newUninstallButton(map, actions));
      case UPDATE_AVAILABLE -> {
        final JButton update = newButton("Update", () -> actions.install().accept(List.of(map)));
        LookAndFeel.setPrimaryButtonStyle(update);
        yield List.of(newUninstallButton(map, actions), update);
      }
      // Cancel and Uninstall look like Install: the same kind of button for every map
      case DOWNLOADING -> List.of(newButton("Cancel", () -> actions.cancel().accept(map)));
      case REMOVING -> List.of();
      default -> throw new IllegalStateException("Unknown map status: " + map.getMapStatus());
    };
  }

  private static JButton newUninstallButton(final ManagedMap map, final Actions actions) {
    return newButton("Uninstall", () -> actions.remove().accept(map));
  }

  private static JButton newButton(final String title, final Runnable action) {
    return new JButtonBuilder().title(title).actionListener(action).build();
  }

  /** Whether the map can be installed or updated now, eg with other selected maps. */
  static boolean isInstallable(final ManagedMap map) {
    return switch (map.getMapStatus()) {
      case AVAILABLE, ERROR, UPDATE_AVAILABLE -> true;
      case INSTALLED, DOWNLOADING, REMOVING -> false;
      default -> throw new IllegalStateException("Unknown map status: " + map.getMapStatus());
    };
  }

  /** Centered vertically and at the left edge, so the texts start right after the preview. */
  private static JPanel centeredVertically(final JComponent component) {
    final JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    panel.add(
        component,
        new GridBagConstraintsBuilder(0, 0)
            .weightX(1)
            .anchor(GridBagConstraintsAnchor.WEST)
            .fill(GridBagConstraintsFill.HORIZONTAL)
            .build());
    return panel;
  }

  private MouseListener newClickListener(final Listener listener) {
    return new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        if (e.isShiftDown()) {
          listener.shiftClicked().accept(MapRow.this);
        } else if (e.isControlDown() || e.isMetaDown()) {
          listener.ctrlClicked().accept(MapRow.this);
        } else {
          listener.clicked().accept(MapRow.this);
        }
      }
    };
  }

  /**
   * Selects the row on a click anywhere but on its buttons, also on the line with a tooltip, which
   * takes the clicks from the row.
   */
  private static void selectOnClick(final Component component, final MouseListener listener) {
    if (component instanceof AbstractButton) {
      return;
    }
    component.addMouseListener(listener);
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        selectOnClick(child, listener);
      }
    }
  }

  /** Highlights the row as selected. */
  void select() {
    selected = true;
    repaint();
  }

  void deselect() {
    selected = false;
    repaint();
  }

  /** Shows how much of the map is downloaded, on its preview and in its status line. */
  void showProgress(final long bytesReceived) {
    final MapDownloadItem download = map.getMapDownloadItem();
    status.setText(progressText(download, bytesReceived));
    showPercent(percentOf(download, bytesReceived));
  }

  /** Returns eg "Downloading · 45%", or "Downloading · 12 MB" if the size is unknown. */
  static String progressText(final MapDownloadItem download, final long bytesReceived) {
    final String received =
        download.getDownloadSizeInBytes() > 0
            ? percentOf(download, bytesReceived) + "%"
            : FileUtils.byteCountToDisplaySize(bytesReceived);
    return "Downloading" + SEPARATOR + received;
  }

  /** The percent of the download that has arrived, from 0 to 100; 0 if the size is unknown. */
  static int percentOf(final MapDownloadItem download, final long bytesReceived) {
    final long size = download.getDownloadSizeInBytes();
    return size > 0 ? (int) Math.min(100, bytesReceived * 100 / size) : 0;
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D graphics = (Graphics2D) g.create();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // a list row ends above its separator line; a tile has an outline instead
    final int height = view == MapBrowserView.TILES ? getHeight() : getHeight() - 1;
    if (selected) {
      graphics.setColor(
          Optional.ofNullable(UIManager.getColor("List.selectionInactiveBackground"))
              .orElseGet(LookAndFeel::getSeparatorColor));
      graphics.fillRoundRect(0, 0, getWidth(), height, SELECTION_ARC, SELECTION_ARC);
    }
    if (view == MapBrowserView.TILES) {
      graphics.setColor(LookAndFeel.getSeparatorColor());
      graphics.drawRoundRect(0, 0, getWidth() - 1, height - 1, SELECTION_ARC, SELECTION_ARC);
    }
    if (downloadPercent >= 0) {
      final int barTop = height - PROGRESS_HEIGHT - 2;
      graphics.setColor(LookAndFeel.getSeparatorColor());
      graphics.fillRect(12, barTop, getWidth() - 24, PROGRESS_HEIGHT);
      graphics.setColor(LookAndFeel.getAccentColor());
      graphics.fillRect(12, barTop, (getWidth() - 24) * downloadPercent / 100, PROGRESS_HEIGHT);
    }
    graphics.dispose();
    super.paintComponent(g);
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension size = super.getPreferredSize();
    // all tiles are as wide as their preview, so they line up in the grid
    return view == MapBrowserView.TILES
        ? new Dimension(Size.TILE.getWidth() + 2 * TILE_PADDING, size.height)
        : size;
  }

  @Override
  public Dimension getMaximumSize() {
    return view == MapBrowserView.TILES
        ? getPreferredSize()
        // the rows fill the width of the list, so the buttons sit next to the scroll bar
        : new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
  }
}
