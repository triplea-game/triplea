package games.strategy.triplea.ui.menubar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.PurchasePanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.ui.SwingAction;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Triple;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;

public class ViewMenu {
  private CheckMenuItem showMapDetails;
  private CheckMenuItem showMapBlends;

  private final GameData gameData;
  private final TripleAFrame frame;
  private final IUIContext uiContext;

  public ViewMenu(final MenuBar menuBar, final TripleAFrame frame) {
    this.frame = frame;
    this.uiContext = frame.getUIContext();
    gameData = frame.getGame().getData();


    final Menu menuView = new Menu("_View");
    menuBar.getMenus().add(menuView);
    addZoomMenu(menuView);
    addUnitSizeMenu(menuView);
    addLockMap(menuView);
    addShowUnits(menuView);
    addUnitNationDrawMenu(menuView);
    if (uiContext.getMapData().useTerritoryEffectMarkers()) {
      addShowTerritoryEffects(menuView);
    }
    addMapSkinsMenu(menuView);
    addShowMapDetails(menuView);
    addShowMapBlends(menuView);
    addDrawTerritoryBordersAgain(menuView);
    addMapFontAndColorEditorMenu(menuView);
    addChatTimeMenu(menuView);
    addShowCommentLog(menuView);
    // The menuItem to turn TabbedProduction on or off
    addTabbedProduction(menuView);
    addShowGameUuid(menuView);

    showMapDetails.setDisable(!uiContext.getMapData().getHasRelief());

  }

  private void addShowCommentLog(final Menu parentMenu) {
    final CheckMenuItem showCommentLog = new CheckMenuItem("Show Comment _Log");
    showCommentLog.setModel(frame.getShowCommentLogButtonModel());
    parentMenu.getItems().add(showCommentLog);
  }

  private static void addTabbedProduction(final Menu parentMenu) {
    final CheckMenuItem tabbedProduction = new CheckMenuItem("Show _Production Tabs");
    tabbedProduction.setMnemonicParsing(true);
    tabbedProduction.setSelected(PurchasePanel.isTabbedProduction());
    tabbedProduction.setOnAction(e -> PurchasePanel.setTabbedProduction(tabbedProduction.isSelected()));
    parentMenu.getItems().add(tabbedProduction);
  }

  private void addShowGameUuid(final Menu menuView) {
    MenuItem gameUUID = new MenuItem("Game _UUID");
    gameUUID.setMnemonicParsing(true);
    gameUUID.setOnAction(e -> {
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setTitle("Game UUID");
      alert.setHeaderText((String) gameData.getProperties().get(GameData.GAME_UUID));
      alert.showAndWait();
    });
    menuView.getItems().add(gameUUID);
  }

  private void addZoomMenu(final Menu menuView) {
    MenuItem mapZoom = new MenuItem("Map _Zoom");
    mapZoom.setOnAction(e -> {
      final SpinnerNumberModel model = new SpinnerNumberModel();
      model.setMaximum(100);
      model.setMinimum(15);
      model.setStepSize(1);
      model.setValue((int) (frame.getMapPanel().getScale() * 100));
      final JSpinner spinner = new JSpinner(model);
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(new JLabel("Choose Map Scale Percentage"), BorderLayout.NORTH);
      panel.add(spinner, BorderLayout.CENTER);
      final JPanel buttons = new JPanel();
      final JButton fitWidth = new JButton("Fit Width");
      buttons.add(fitWidth);
      final JButton fitHeight = new JButton("Fit Height");
      buttons.add(fitHeight);
      final JButton reset = new JButton("Reset");
      buttons.add(reset);
      panel.add(buttons, BorderLayout.SOUTH);
      fitWidth.addActionListener(event -> {
        final double screenWidth = frame.getMapPanel().getWidth();
        final double mapWidth = frame.getMapPanel().getImageWidth();
        double ratio = screenWidth / mapWidth;
        ratio = Math.max(0.15, ratio);
        ratio = Math.min(1, ratio);
        model.setValue((int) (ratio * 100));
      });
      fitHeight.addActionListener(event -> {
        final double screenHeight = frame.getMapPanel().getHeight();
        final double mapHeight = frame.getMapPanel().getImageHeight();
        double ratio = screenHeight / mapHeight;
        ratio = Math.max(0.15, ratio);
        model.setValue((int) (ratio * 100));
      });
      reset.addActionListener(event -> model.setValue(100));
      new Alert(AlertType.CONFIRMATION, "Choose Map Scale", ButtonType.OK, ButtonType.CANCEL).showAndWait()
          .filter(ButtonType.OK::equals).ifPresent(response -> {
            final Number value = (Number) model.getValue();
            frame.setScale(value.doubleValue());
          });

    });
    menuView.getItems().add(mapZoom);
  }

  private RadioMenuItem getScaleRadioButton(double scale, ToggleGroup group, boolean setMnemonic) {
    RadioMenuItem result =
        new RadioMenuItem(setMnemonic ? "_" : "" + new DecimalFormat("00.##").format(scale * 100) + "%");
    result.setOnAction(e -> {
      uiContext.setUnitScaleFactor(scale);
      frame.getMapPanel().resetMap();
    });
    if (setMnemonic) {
      result.setMnemonicParsing(true);
    }
    result.setToggleGroup(group);
    if (Math.abs(scale - uiContext.getUnitImageFactory().getScaleFactor()) < 0.01) {
      result.setSelected(true);
    }
    return result;
  }

  private void addUnitSizeMenu(final Menu menuView) {
    final Menu unitSizeMenu = new Menu("Unit _Size");
    final ToggleGroup unitSizeGroup = new ToggleGroup();
    final RadioMenuItem radioItem125 = getScaleRadioButton(1.25, unitSizeGroup, false);
    final RadioMenuItem radioItem100 = getScaleRadioButton(1.0, unitSizeGroup, true);
    final RadioMenuItem radioItem87 = getScaleRadioButton(0.875, unitSizeGroup, false);
    final RadioMenuItem radioItem83 = getScaleRadioButton(0.8333, unitSizeGroup, true);
    final RadioMenuItem radioItem75 = getScaleRadioButton(0.75, unitSizeGroup, true);
    final RadioMenuItem radioItem66 = getScaleRadioButton(0.6666, unitSizeGroup, true);
    final RadioMenuItem radioItem56 = getScaleRadioButton(0.5625, unitSizeGroup, false);
    final RadioMenuItem radioItem50 = getScaleRadioButton(0.5, unitSizeGroup, true);
    if (unitSizeGroup.getSelectedToggle() == null) {
      System.err.println("default unit size does not match any menu item");
      radioItem100.setSelected(true);
    }
    unitSizeMenu.getItems().add(radioItem125);
    unitSizeMenu.getItems().add(radioItem100);
    unitSizeMenu.getItems().add(radioItem87);
    unitSizeMenu.getItems().add(radioItem83);
    unitSizeMenu.getItems().add(radioItem75);
    unitSizeMenu.getItems().add(radioItem66);
    unitSizeMenu.getItems().add(radioItem56);
    unitSizeMenu.getItems().add(radioItem50);
    menuView.getItems().add(unitSizeMenu);
  }

  private void addMapSkinsMenu(final Menu menuGame) {
    // beagles Mapskin code
    // creates a sub menu of radiobuttons for each available mapdir
    RadioMenuItem mapMenuItem;
    final Menu mapSubMenu = new Menu("Map S_kins");
    mapSubMenu.setMnemonicParsing(true);
    final ToggleGroup mapButtonGroup = new ToggleGroup();
    menuGame.getItems().add(mapSubMenu);
    final Map<String, String> skins = AbstractUIContext.getSkins(frame.getGame().getData());
    for (final String key : skins.keySet()) {
      mapMenuItem = new RadioMenuItem(key);
      // menu key navigation with ALT+first character (multiple hits for same character possible)
      // mapMenuItem.setMnemonic(KeyEvent.getExtendedKeyCodeForChar(key.charAt(0)));
      mapMenuItem.setToggleGroup(mapButtonGroup);
      mapSubMenu.getItems().add(mapMenuItem);
      mapSubMenu.setDisable(skins.size() <= 1);
      if (skins.get(key).equals(AbstractUIContext.getMapDir())) {
        mapMenuItem.setSelected(true);
      }
      mapMenuItem.setOnAction(e -> {
        try {
          frame.updateMap(skins.get(key));
          if (uiContext.getMapData().getHasRelief()) {
            showMapDetails.setSelected(true);
          }
          showMapDetails.setDisable(!uiContext.getMapData().getHasRelief());
        } catch (final Exception exception) {
          ClientLogger.logError("Error Changing Map Skin2", exception);
        }
      });
    }
  }

  private void addShowMapDetails(final Menu menuGame) {
    showMapDetails = new CheckMenuItem("Show Map _Details");
    showMapDetails.setMnemonicParsing(true);
    showMapDetails.setSelected(TileImageFactory.getShowReliefImages());
    showMapDetails.setOnAction(e -> {
      if (TileImageFactory.getShowReliefImages() == showMapDetails.isSelected()) {
        return;
      }
      TileImageFactory.setShowReliefImages(showMapDetails.isSelected());
      new Thread(() -> {
        Thread.yield();
        frame.getMapPanel().updateCountries(gameData.getMap().getTerritories());
      }, "Triplea : Show map details thread").start();
    });
    menuGame.getItems().add(showMapDetails);
  }

  private void addShowMapBlends(final Menu menuGame) {
    showMapBlends = new CheckMenuItem("Show Map _Blends");
    showMapBlends.setMnemonicParsing(true);
    if (uiContext.getMapData().getHasRelief() && !showMapDetails.isDisable() && showMapDetails.isSelected()) {
      showMapBlends.setDisable(false);
      showMapBlends.setSelected(TileImageFactory.getShowMapBlends());
    } else {
      showMapBlends.setSelected(false);
      showMapBlends.setDisable(true);
    }
    showMapBlends.setOnAction(e -> {
      if (TileImageFactory.getShowMapBlends() == showMapBlends.isSelected()) {
        return;
      }
      TileImageFactory.setShowMapBlends(showMapBlends.isSelected());
      TileImageFactory.setShowMapBlendMode(uiContext.getMapData().getMapBlendMode());
      TileImageFactory.setShowMapBlendAlpha(uiContext.getMapData().getMapBlendAlpha());
      new Thread(() -> {
        frame.setScale(uiContext.getScale() * 100);
        Thread.yield();
        frame.getMapPanel().updateCountries(gameData.getMap().getTerritories());
      }, "Triplea : Show map Blends thread").start();
    });
    menuGame.getItems().add(showMapBlends);
  }

  private void addShowUnits(final Menu menuView) {
    final CheckMenuItem showUnitsBox = new CheckMenuItem("Show _Units");
    showUnitsBox.setMnemonicParsing(true);
    showUnitsBox.setSelected(true);
    showUnitsBox.setOnAction(e -> {
      final boolean tfselected = showUnitsBox.isSelected();
      uiContext.setShowUnits(tfselected);
      frame.getMapPanel().resetMap();
    });
    menuView.getItems().add(showUnitsBox);
  }

  private void addDrawTerritoryBordersAgain(final Menu parentMenu) {
    final Menu drawBordersMenu = new Menu("Draw Borders _On Top");
    final RadioMenuItem noneButton = new RadioMenuItem("_Low");
    noneButton.setMnemonicParsing(true);
    final RadioMenuItem mediumButton = new RadioMenuItem("_Medium");
    mediumButton.setMnemonicParsing(true);
    final RadioMenuItem highButton = new RadioMenuItem("_High");
    highButton.setMnemonicParsing(true);
    final ToggleGroup group = new ToggleGroup();
    noneButton.setToggleGroup(group);
    mediumButton.setToggleGroup(group);
    highButton.setToggleGroup(group);
    drawBordersMenu.setOnShowing(e -> {
      final IDrawable.OptionalExtraBorderLevel current = uiContext.getDrawTerritoryBordersAgain();
      if (current == IDrawable.OptionalExtraBorderLevel.LOW) {
        noneButton.setSelected(true);
      } else if (current == IDrawable.OptionalExtraBorderLevel.MEDIUM) {
        mediumButton.setSelected(true);
      } else if (current == IDrawable.OptionalExtraBorderLevel.HIGH) {
        highButton.setSelected(true);
      }
    });
    noneButton.setOnAction(e -> {
      if (noneButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.LOW) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.LOW);
        frame.getMapPanel().resetMap();
      }
    });
    mediumButton.setOnAction(e -> {
      if (mediumButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.MEDIUM) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.MEDIUM);
        frame.getMapPanel().resetMap();
      }
    });
    highButton.setOnAction(e -> {
      if (highButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.HIGH) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.HIGH);
        frame.getMapPanel().resetMap();
      }
    });
    drawBordersMenu.getItems().add(noneButton);
    drawBordersMenu.getItems().add(mediumButton);
    drawBordersMenu.getItems().add(highButton);
    parentMenu.getItems().add(drawBordersMenu);
  }

  private void addMapFontAndColorEditorMenu(final Menu parentMenu) {
    final Action mapFontOptions = SwingAction.of("Edit Map Font and _Color", e -> {
      final List<IEditableProperty> properties = new ArrayList<>();
      final NumberProperty fontsize =
          new NumberProperty("Font Size", null, 60, 0, MapImage.getPropertyMapFont().getSize());
      final ColorProperty territoryNameColor = new ColorProperty("Territory Name and PU Color", null,
          MapImage.getPropertyTerritoryNameAndPUAndCommentcolor());
      final ColorProperty unitCountColor =
          new ColorProperty("Unit Count Color", null, MapImage.getPropertyUnitCountColor());
      final ColorProperty factoryDamageColor =
          new ColorProperty("Factory Damage Color", null, MapImage.getPropertyUnitFactoryDamageColor());
      final ColorProperty hitDamageColor =
          new ColorProperty("Hit Damage Color", null, MapImage.getPropertyUnitHitDamageColor());
      properties.add(fontsize);
      properties.add(territoryNameColor);
      properties.add(unitCountColor);
      properties.add(factoryDamageColor);
      properties.add(hitDamageColor);
      final PropertiesUI pui = new PropertiesUI(properties, true);
      final JPanel ui = new JPanel();
      ui.setLayout(new BorderLayout());
      ui.add(pui, BorderLayout.CENTER);
      ui.add(
          new JLabel("<html>Change the font and color of 'text' (not pictures) on the map. "
              + "<br /><em>(Some people encounter problems with the color picker, and this "
              + "<br />is a bug outside of triplea, located in the 'look and feel' that "
              + "<br />you are using. If you have an error come up, try switching to the "
              + "<br />basic 'look and feel', then setting the color, then switching back.)</em></html>"),
          BorderLayout.NORTH);
      final Object[] options = {"Set Properties", "Reset To Default", "Cancel"};
      Optional<ButtonType> chosenOption =
          new Alert(AlertType.CONFIRMATION, "Edit Map Font and Color", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
              .showAndWait();
      if (chosenOption.isPresent() && chosenOption.get() != ButtonType.CANCEL) {
        if (chosenOption.get() == ButtonType.NO) {
          MapImage.resetPropertyMapFont();
          MapImage.resetPropertyTerritoryNameAndPUAndCommentcolor();
          MapImage.resetPropertyUnitCountColor();
          MapImage.resetPropertyUnitFactoryDamageColor();
          MapImage.resetPropertyUnitHitDamageColor();
          frame.getMapPanel().resetMap();
        } else if (chosenOption.get() == ButtonType.YES) {
          MapImage.setPropertyMapFont(new Font("Ariel", Font.BOLD, fontsize.getValue()));
          MapImage.setPropertyTerritoryNameAndPUAndCommentcolor((Color) territoryNameColor.getValue());
          MapImage.setPropertyUnitCountColor((Color) unitCountColor.getValue());
          MapImage.setPropertyUnitFactoryDamageColor((Color) factoryDamageColor.getValue());
          MapImage.setPropertyUnitHitDamageColor((Color) hitDamageColor.getValue());
          frame.getMapPanel().resetMap();
        }
      }

    });
    parentMenu.getItems().add(mapFontOptions).setMnemonic(KeyEvent.VK_C);
  }

  private void addShowTerritoryEffects(final Menu parentMenu) {
    final CheckMenuItem territoryEffectsBox = new CheckMenuItem("Show _TerritoryEffects");
    territoryEffectsBox.setMnemonicParsing(true);
    territoryEffectsBox.setOnAction(e -> {
      final boolean tfselected = territoryEffectsBox.isSelected();
      uiContext.setShowTerritoryEffects(tfselected);
      frame.getMapPanel().resetMap();
    });
    parentMenu.getItems().add(territoryEffectsBox);
    territoryEffectsBox.setSelected(true);
  }

  private void addLockMap(final Menu menuView) {
    final CheckMenuItem lockMapBox = new CheckMenuItem("Lock _Map");
    lockMapBox.setMnemonicParsing(true);
    lockMapBox.setSelected(uiContext.getLockMap());
    lockMapBox.setOnAction(e -> uiContext.setLockMap(lockMapBox.isSelected()));
    menuView.getItems().add(lockMapBox);
  }

  private void addUnitNationDrawMenu(final Menu menuView) {
    final Menu unitSizeMenu = new Menu("Flag Display Mode");// TODO add Mnemonic

    final Preferences prefs = Preferences.userNodeForPackage(getClass());
    final UnitsDrawer.UnitFlagDrawMode setting = Enum.valueOf(UnitsDrawer.UnitFlagDrawMode.class,
        prefs.get(UnitsDrawer.PreferenceKeys.DRAW_MODE.name(), UnitsDrawer.UnitFlagDrawMode.NEXT_TO.toString()));
    UnitsDrawer.setUnitFlagDrawMode(setting, prefs);
    UnitsDrawer.enabledFlags =
        prefs.getBoolean(UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), UnitsDrawer.enabledFlags);

    final CheckMenuItem toggleFlags = new CheckMenuItem("Show by default");
    toggleFlags.setSelected(UnitsDrawer.enabledFlags);
    toggleFlags.setOnAction(e -> {
      UnitsDrawer.enabledFlags = toggleFlags.isSelected();
      prefs.putBoolean(UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), toggleFlags.isSelected());
      frame.getMapPanel().resetMap();
    });
    unitSizeMenu.getItems().add(toggleFlags);

    final ToggleGroup unitFlagSettingGroup = new ToggleGroup();
    unitSizeMenu.getItems().add(createFlagDrawModeRadionButtonItem("Small", unitFlagSettingGroup,
        UnitsDrawer.UnitFlagDrawMode.NEXT_TO, setting, prefs));
    unitSizeMenu.getItems().add(createFlagDrawModeRadionButtonItem("Large", unitFlagSettingGroup,
        UnitsDrawer.UnitFlagDrawMode.BELOW, setting, prefs));
    menuView.getItems().add(unitSizeMenu);
  }

  private RadioMenuItem createFlagDrawModeRadionButtonItem(final String text, final ToggleGroup group,
      final UnitsDrawer.UnitFlagDrawMode drawMode, final UnitsDrawer.UnitFlagDrawMode setting,
      final Preferences prefs) {
    return createRadioButtonItem(text, group, e -> {
      UnitsDrawer.setUnitFlagDrawMode(drawMode, prefs);
      frame.getMapPanel().resetMap();
    }, setting.equals(drawMode));
  }

  private RadioMenuItem createRadioButtonItem(final String text, final ToggleGroup group,
      final EventHandler<ActionEvent> action,
      final boolean selected) {

    final RadioMenuItem buttonItem = new RadioMenuItem(text);
    buttonItem.setOnAction(action);
    ;
    buttonItem.setSelected(selected);
    buttonItem.setToggleGroup(group);
    return buttonItem;
  }

  private void addChatTimeMenu(final Menu parentMenu) {
    final CheckMenuItem chatTimeBox = new CheckMenuItem("Show Chat _Times");
    chatTimeBox.setMnemonicParsing(true);
    chatTimeBox.setOnAction(e -> frame.setShowChatTime(chatTimeBox.isSelected()));
    chatTimeBox.setSelected(false);
    parentMenu.getItems().add(chatTimeBox);
    chatTimeBox.setDisable(MainFrame.getInstance() != null && MainFrame.getInstance().getChat() != null);
  }
}
