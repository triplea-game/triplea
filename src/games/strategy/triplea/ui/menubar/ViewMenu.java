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
import javax.swing.JRadioButtonMenuItem;
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
import javafx.scene.control.RadioMenuItem;
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
    addSetLookAndFeel(menuView);

    showMapDetails.setDisable(!uiContext.getMapData().getHasRelief());

  }

  private void addShowCommentLog(final Menu parentMenu) {
    final CheckMenuItem showCommentLog = new CheckMenuItem("Show Comment _Log");
    showCommentLog.setModel(frame.getShowCommentLogButtonModel());
    parentMenu.add(showCommentLog).setMnemonic(KeyEvent.VK_L);
  }

  private static void addTabbedProduction(final Menu parentMenu) {
    final CheckMenuItem tabbedProduction = new CheckMenuItem("Show Production Tabs");
    tabbedProduction.setMnemonic(KeyEvent.VK_P);
    tabbedProduction.setSelected(PurchasePanel.isTabbedProduction());
    tabbedProduction
        .addActionListener(SwingAction.of(e -> PurchasePanel.setTabbedProduction(tabbedProduction.isSelected())));
    parentMenu.add(tabbedProduction);
  }

  private void addShowGameUuid(final Menu menuView) {
    menuView.add(SwingAction.of("Game UUID", e -> {
      final String id = (String) gameData.getProperties().get(GameData.GAME_UUID);
      final JTextField text = new JTextField();
      text.setText(id);
      final JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      panel.add(new JLabel("Game UUID:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
      panel.add(text, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0));
      JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(menuView), panel, "Game UUID",
          JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] {"OK"}, "OK");
    })).setMnemonic(KeyEvent.VK_U);
  }

  private void addSetLookAndFeel(final Menu menuView) {
    menuView.add(SwingAction.of("Set Look and Feel", e -> {
      final Triple<JList<String>, Map<String, String>, String> lookAndFeel = TripleAMenuBar.getLookAndFeelList();
      final JList<String> list = lookAndFeel.getFirst();
      final String currentKey = lookAndFeel.getThird();
      final Map<String, String> lookAndFeels = lookAndFeel.getSecond();
      if (JOptionPane.showConfirmDialog(frame, list) == JOptionPane.OK_OPTION) {
        final String selectedValue = list.getSelectedValue();
        if (selectedValue == null) {
          return;
        }
        if (selectedValue.equals(currentKey)) {
          return;
        }
        LookAndFeel.setDefaultLookAndFeel(lookAndFeels.get(selectedValue));
        EventThreadJOptionPane.showMessageDialog(frame, "The look and feel will update when you restart TripleA",
            new CountDownLatchHandler(true));
      }
    })).setMnemonic(KeyEvent.VK_F);
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

  private void addUnitSizeMenu(final Menu menuView) {
    final NumberFormat s_decimalFormat = new DecimalFormat("00.##");
    // This is the action listener used
    class UnitSizeAction extends AbstractAction {
      private static final long serialVersionUID = -6280511505686687867L;
      private final double scaleFactor;

      public UnitSizeAction(final double scaleFactor) {
        this.scaleFactor = scaleFactor;
        putValue(Action.NAME, s_decimalFormat.format(scaleFactor * 100) + "%");
      }

      @Override
      public void actionPerformed(final ActionEvent e) {
        uiContext.setUnitScaleFactor(scaleFactor);
        frame.getMapPanel().resetMap();
      }
    }
    final Menu unitSizeMenu = new Menu("Unit _Size");
    final ButtonGroup unitSizeGroup = new ButtonGroup();
    final JRadioButtonMenuItem radioItem125 = new JRadioButtonMenuItem(new UnitSizeAction(1.25));
    final JRadioButtonMenuItem radioItem100 = new JRadioButtonMenuItem(new UnitSizeAction(1.0));
    radioItem100.setMnemonic(KeyEvent.VK_1);
    final JRadioButtonMenuItem radioItem87 = new JRadioButtonMenuItem(new UnitSizeAction(0.875));
    final JRadioButtonMenuItem radioItem83 = new JRadioButtonMenuItem(new UnitSizeAction(0.8333));
    radioItem83.setMnemonic(KeyEvent.VK_8);
    final JRadioButtonMenuItem radioItem75 = new JRadioButtonMenuItem(new UnitSizeAction(0.75));
    radioItem75.setMnemonic(KeyEvent.VK_7);
    final JRadioButtonMenuItem radioItem66 = new JRadioButtonMenuItem(new UnitSizeAction(0.6666));
    radioItem66.setMnemonic(KeyEvent.VK_6);
    final JRadioButtonMenuItem radioItem56 = new JRadioButtonMenuItem(new UnitSizeAction(0.5625));
    final JRadioButtonMenuItem radioItem50 = new JRadioButtonMenuItem(new UnitSizeAction(0.5));
    radioItem50.setMnemonic(KeyEvent.VK_5);
    unitSizeGroup.add(radioItem125);
    unitSizeGroup.add(radioItem100);
    unitSizeGroup.add(radioItem87);
    unitSizeGroup.add(radioItem83);
    unitSizeGroup.add(radioItem75);
    unitSizeGroup.add(radioItem66);
    unitSizeGroup.add(radioItem56);
    unitSizeGroup.add(radioItem50);
    radioItem100.setSelected(true);
    // select the closest to to the default size
    final Enumeration<AbstractButton> enum1 = unitSizeGroup.getElements();
    boolean matchFound = false;
    while (enum1.hasMoreElements()) {
      final JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) enum1.nextElement();
      final UnitSizeAction action = (UnitSizeAction) menuItem.getAction();
      if (Math.abs(action.scaleFactor - uiContext.getUnitImageFactory().getScaleFactor()) < 0.01) {
        menuItem.setSelected(true);
        matchFound = true;
        break;
      }
    }
    if (!matchFound) {
      System.err.println("default unit size does not match any menu item");
    }
    unitSizeMenu.add(radioItem125);
    unitSizeMenu.add(radioItem100);
    unitSizeMenu.add(radioItem87);
    unitSizeMenu.add(radioItem83);
    unitSizeMenu.add(radioItem75);
    unitSizeMenu.add(radioItem66);
    unitSizeMenu.add(radioItem56);
    unitSizeMenu.add(radioItem50);
    menuView.getItems().add(unitSizeMenu);
  }

  private void addMapSkinsMenu(final Menu menuGame) {
    // beagles Mapskin code
    // creates a sub menu of radiobuttons for each available mapdir
    MenuItem mapMenuItem;
    final Menu mapSubMenu = new Menu("Map Skins");
    mapSubMenu.setMnemonic(KeyEvent.VK_K);
    final ButtonGroup mapButtonGroup = new ButtonGroup();
    menuGame.add(mapSubMenu);
    final Map<String, String> skins = AbstractUIContext.getSkins(frame.getGame().getData());
    for (final String key : skins.keySet()) {
      mapMenuItem = new JRadioButtonMenuItem(key);
      // menu key navigation with ALT+first character (multiple hits for same character possible)
      // mapMenuItem.setMnemonic(KeyEvent.getExtendedKeyCodeForChar(key.charAt(0)));
      mapButtonGroup.add(mapMenuItem);
      mapSubMenu.add(mapMenuItem);
      mapSubMenu.setEnabled(skins.size() > 1);
      if (skins.get(key).equals(AbstractUIContext.getMapDir())) {
        mapMenuItem.setSelected(true);
      }
      mapMenuItem.addActionListener(e -> {
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
    final Menu drawBordersMenu = new Menu();
    drawBordersMenu.setMnemonic(KeyEvent.VK_O);
    drawBordersMenu.setText("Draw Borders On Top");
    final JRadioButton noneButton = new JRadioButton("Low");
    noneButton.setMnemonic(KeyEvent.VK_L);
    final JRadioButton mediumButton = new JRadioButton("Medium");
    mediumButton.setMnemonic(KeyEvent.VK_M);
    final JRadioButton highButton = new JRadioButton("High");
    highButton.setMnemonic(KeyEvent.VK_H);
    final ButtonGroup group = new ButtonGroup();
    group.add(noneButton);
    group.add(mediumButton);
    group.add(highButton);
    drawBordersMenu.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected(final MenuEvent e) {
        final IDrawable.OptionalExtraBorderLevel current = uiContext.getDrawTerritoryBordersAgain();
        if (current == IDrawable.OptionalExtraBorderLevel.LOW) {
          noneButton.setSelected(true);
        } else if (current == IDrawable.OptionalExtraBorderLevel.MEDIUM) {
          mediumButton.setSelected(true);
        } else if (current == IDrawable.OptionalExtraBorderLevel.HIGH) {
          highButton.setSelected(true);
        }
      }

      @Override
      public void menuDeselected(final MenuEvent e) {}

      @Override
      public void menuCanceled(final MenuEvent e) {}
    });
    noneButton.addActionListener(e -> {
      if (noneButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.LOW) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.LOW);
        frame.getMapPanel().resetMap();
      }
    });
    mediumButton.addActionListener(e -> {
      if (mediumButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.MEDIUM) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.MEDIUM);
        frame.getMapPanel().resetMap();
      }
    });
    highButton.addActionListener(e -> {
      if (highButton.isSelected()
          && uiContext.getDrawTerritoryBordersAgain() != IDrawable.OptionalExtraBorderLevel.HIGH) {
        uiContext.setDrawTerritoryBordersAgain(IDrawable.OptionalExtraBorderLevel.HIGH);
        frame.getMapPanel().resetMap();
      }
    });
    drawBordersMenu.add(noneButton);
    drawBordersMenu.add(mediumButton);
    drawBordersMenu.add(highButton);
    parentMenu.add(drawBordersMenu);
  }

  private void addMapFontAndColorEditorMenu(final Menu parentMenu) {
    final Action mapFontOptions = SwingAction.of("Edit Map Font and Color", e -> {
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
    parentMenu.add(mapFontOptions).setMnemonic(KeyEvent.VK_C);
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
    final CheckMenuItem chatTimeBox = new CheckMenuItem("Show Chat Times");
    chatTimeBox.setMnemonic(KeyEvent.VK_T);
    chatTimeBox.addActionListener(e -> frame.setShowChatTime(chatTimeBox.isSelected()));
    chatTimeBox.setSelected(false);
    parentMenu.add(chatTimeBox);
    chatTimeBox.setEnabled(MainFrame.getInstance() != null && MainFrame.getInstance().getChat() != null);
  }
}
