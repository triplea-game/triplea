package games.strategy.triplea.ui;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.triplea.util.JFXUtils;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.util.Callback;

public class PlayerChooser extends Alert {
  private ListView<PlayerID> m_list;
  private final PlayerList m_players;
  private final PlayerID m_defaultPlayer;
  private final IUIContext m_uiContext;
  private final boolean m_allowNeutral;

  // private JOptionPane m_pane;
  /** Creates new PlayerChooser */
  public PlayerChooser(final PlayerList players, final IUIContext uiContext, final boolean allowNeutral) {
    this(players, null, uiContext, allowNeutral);
  }

  /** Creates new PlayerChooser */
  public PlayerChooser(final PlayerList players, final PlayerID defaultPlayer, final IUIContext uiContext,
      final boolean allowNeutral) {
    super(AlertType.CONFIRMATION, "Choose Player", ButtonType.OK, ButtonType.CANCEL);
    setGraphic(new ImageView(new WritableImage(32, 32)));
    m_players = players;
    m_defaultPlayer = defaultPlayer;
    m_uiContext = uiContext;
    m_allowNeutral = allowNeutral;
    createComponents();
  }

  private void createComponents() {
    final Collection<PlayerID> players = new ArrayList<>(m_players.getPlayers());
    if (m_allowNeutral) {
      players.add(PlayerID.NULL_PLAYERID);
    }
    m_list = new ListView<>();
    m_list.setItems(FXCollections.observableArrayList(players));
    m_list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    m_list.getSelectionModel().select(m_defaultPlayer);
    m_list.setCellFactory(new PlayerChooserRenderer(m_uiContext));
    getDialogPane().setContent(new ScrollPane(m_list));

    final int maxSize = 700;
    final int suggestedSize = m_players.size() * 40;
    final int actualSize = suggestedSize > maxSize ? maxSize : suggestedSize;
    setHeight(300);
    setWidth(actualSize);
  }

  public PlayerID getSelected() {
    if (showAndWait().filter(ButtonType.OK::equals).isPresent()) {
      return m_list.getSelectionModel().getSelectedItem();
    }
    return null;
  }
}


class PlayerChooserRenderer implements Callback<ListView<PlayerID>, ListCell<PlayerID>> {
  private final IUIContext m_uiContext;

  PlayerChooserRenderer(final IUIContext uiContext) {
    m_uiContext = uiContext;
  }


  @Override
  public ListCell<PlayerID> call(ListView<PlayerID> list) {
    return new ListCell<PlayerID>() {

      @Override
      protected void updateItem(PlayerID id, boolean b) {
        setText(id.getName());
        if (m_uiContext == null || id == PlayerID.NULL_PLAYERID) {
          setGraphic(new ImageView(new WritableImage(32, 32)));
        } else {
          setGraphic(
              new ImageView(JFXUtils.convertToFx((BufferedImage) m_uiContext.getFlagImageFactory().getFlag(id))));
        }
      }
    };
  }
}
