package games.strategy.engine.lobby.moderator.toolbox.tabs.maps;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import lombok.Builder;
import org.triplea.http.client.maps.admin.MapTagMetaData;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.java.ThreadRunner;
import org.triplea.swing.JComboBoxBuilder;
import org.triplea.swing.JSplitPaneBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/** The maps tab contains a listing of all maps known to the maps server. */
public class MapsTab implements MapsTabUi, Supplier<Component> {
  private final MapsTabModel mapsTabModel;
  private final Integer parentWindowHeight;
  private final JTextArea messagesTextArea = JTextAreaBuilder.builder().rows(3).readOnly().build();

  private final JButton helpButton =
      SwingComponents.helpButton(
          "Map Tagging Help",
          "You can change map tags by selecting different values in the drop downs.<br>"
              + "The server is updated immediately after selecting a new value.<br>"
              + "Map tags are displayed in the download maps window.");

  @Builder
  public MapsTab(final MapsTabModel mapsTabModel, final Integer parentWindowHeight) {
    this.mapsTabModel = mapsTabModel;
    mapsTabModel.setMapsTabUi(this);
    this.parentWindowHeight = parentWindowHeight;
  }

  @Override
  public Component get() {
    messagesTextArea.setVisible(false);
    return new JPanelBuilder()
        .borderLayout()
        .addCenter(
            JSplitPaneBuilder.builder()
                .dividerLocation(parentWindowHeight - 120)
                .giveExtraSpaceToTopAndLeftPanes()
                .addTop(SwingComponents.newJScrollPane(mapTagTable()))
                .addBottom(SwingComponents.newJScrollPane(messagesTextArea))
                .build())
        .addNorth(
            new JPanelBuilder() //
                .boxLayoutHorizontal()
                .add(helpButton)
                .add(Box.createGlue())
                .build())
        .build();
  }

  /**
   * Returns a table displaying map name on the left and tag values across the row with text boxes
   * and dropdowns to allow those values to be updated. Updates are sent to the backend after being
   * selected from the drop down.
   */
  private JComponent mapTagTable() {
    final List<MapTagMetaData> tagMetaData = mapsTabModel.fetchAllowedMapTagValues();

    final List<MapDownloadItem> maps =
        mapsTabModel.fetchMapsList().stream()
            .sorted(Comparator.comparing(MapDownloadItem::getMapName))
            .collect(Collectors.toList());

    // two columns for every tag (name + value)
    // plus one column for a map name label
    // plus one column for save button
    final int columnCount = (tagMetaData.size() * 2) + 1 + 1;
    return new JPanelBuilder()
        .add(
            new JPanelBuilder()
                .gridLayout(maps.size(), columnCount)
                .addAll(
                    maps.stream()
                        .map(downloadItem -> buildMapRow(downloadItem, tagMetaData))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()))
                .build())
        .build();
  }

  /** Builds a single map row with drop downs to change tag values for that map. */
  private List<Component> buildMapRow(
      final MapDownloadItem mapDownloadItem, final List<MapTagMetaData> mapTagMetaData) {

    final List<Component> components = new ArrayList<>();
    components.add(new JLabel(mapDownloadItem.getMapName()));

    for (final MapTagMetaData mapTagMetaDataItem : mapTagMetaData) {
      components.add(new JLabel(mapTagMetaDataItem.getTagName() + ":", SwingConstants.RIGHT));

      components.add(
          JComboBoxBuilder.builder()
              .selectedItem(mapDownloadItem.getTagValue(mapTagMetaDataItem.getTagName()))
              .items(mapTagMetaDataItem.getAllowedValues())
              .enableAutoComplete()
              .itemSelectedAction(
                  selectedItem ->
                      ThreadRunner.runInNewThread(
                          () ->
                              mapsTabModel.updateMapTag(
                                  mapDownloadItem.getMapName(), mapTagMetaDataItem, selectedItem)))
              .build());
    }
    return components;
  }

  @Override
  public void showMessage(final String message) {
    messagesTextArea.setVisible(true);
    SwingUtilities.invokeLater(() -> messagesTextArea.append(message + "\n"));
  }
}
