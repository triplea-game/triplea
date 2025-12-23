package games.strategy.triplea.ui.menubar;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.FindTerritoryAction;
import games.strategy.triplea.ui.FlagDrawMode;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ThreadRunner;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.JMenuItemCheckBoxBuilder;
import org.triplea.swing.key.binding.KeyCode;

@Slf4j
final class ViewMenu extends JMenu {
  private static final long serialVersionUID = -4703734404422047487L;

  private JCheckBoxMenuItem showMapDetails;

  private final List<Territory> gameMapTerritories;
  private final TripleAFrame frame;
  private final UiContext uiContext;

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public enum Mnemonic {
    VIEW_MENU(KeyCode.V),
    FLAG_SUBMENU(KeyCode.N),
    UNIT_SIZE_SUBMENU(KeyCode.S),
    UNIT_SIZE_1(KeyCode.NR_1),
    UNIT_SIZE_83(KeyCode.NR_8),
    UNIT_SIZE_75(KeyCode.NR_7),
    UNIT_SIZE_66(KeyCode.NR_6),
    UNIT_SIZE_5(KeyCode.NR_5),
    MAP_SKINS_SUBMENU(KeyCode.K),
    LOCK_MAP(KeyCode.M),
    ZOOM(KeyCode.Z),
    SHOW_CHAT_TIME(KeyCode.T),
    SHOW_COMMENT_LOG(KeyCode.L),
    SHOW_MAP_DETAILS(KeyCode.D),
    SHOW_MAP_BLENDS(KeyCode.B),
    SHOW_UNIT(KeyCode.U),
    MAP_FONT_OPTIONS(KeyCode.C),
    SHOW_TERRITORY_EFFECTS(KeyCode.T),
    FLAGS_OFF(KeyCode.S),
    FLAGS_LARGE(KeyCode.P),
    FLAGS_SMALL(KeyCode.L),
    FIND_TERRITORY(KeyCode.F);

    private final KeyCode mnemonicCode;

    public int getValue() {
      return mnemonicCode.getInputEventCode();
    }
  }

  ViewMenu(final TripleAFrame frame) {
    super("View");

    this.frame = frame;
    this.uiContext = frame.getUiContext();
    gameMapTerritories = frame.getGame().getData().getMap().getTerritories();

    setMnemonic(Mnemonic.VIEW_MENU.getValue());

    addZoomMenu();
    addUnitSizeMenu();
    addLockMap();
    addShowUnitsMenu();
    addShowUnitsInStatusBarMenu();
    addFlagDisplayModeMenu();

    if (uiContext.getMapData().useTerritoryEffectMarkers()) {
      addShowTerritoryEffects();
    }
    if (ClientSetting.showBetaFeatures.getValueOrThrow()) {
      addMapSkinsMenu();
    }
    addShowMapDetails();
    addShowMapBlends();
    addShowZoomMenu();
    addMapFontAndColorEditorMenu();
    addChatTimeMenu();
    addShowCommentLog();
    addSeparator();
    addFindTerritory();

    showMapDetails.setEnabled(uiContext.getMapData().getHasRelief());
  }

  private void addShowCommentLog() {
    add(
        new JMenuItemCheckBoxBuilder(
                "Show Comment Log", Mnemonic.SHOW_COMMENT_LOG.getMnemonicCode())
            .bindSetting(ClientSetting.showCommentLog)
            .actionListener(
                value -> {
                  if (value) {
                    frame.showCommentLog();
                  } else {
                    frame.hideCommentLog();
                  }
                })
            .build());
  }

  private void addZoomMenu() {
    add(
        new JMenuItemBuilder("Map Zoom", Mnemonic.ZOOM.getMnemonicCode())
            .actionListener(
                () -> {
                  final SpinnerNumberModel model = new SpinnerNumberModel();
                  model.setMaximum(UiContext.MAP_SCALE_MAX_VALUE * 100);
                  model.setMinimum(Math.ceil(frame.getMapPanel().getMinScale() * 100));
                  model.setStepSize(1);
                  setSpinnerValue(model, Math.round(frame.getMapPanel().getScale() * 100));
                  final JSpinner spinner = new JSpinner(model);
                  final JPanel panel = new JPanel();
                  panel.setLayout(new BorderLayout());
                  panel.add(new JLabel("Choose Map Zoom (%)"), BorderLayout.NORTH);
                  panel.add(spinner, BorderLayout.CENTER);
                  final JPanel buttons = new JPanel();
                  final JButton fitWidth = new JButton("Fit Width");
                  buttons.add(fitWidth);
                  final JButton fitHeight = new JButton("Fit Height");
                  buttons.add(fitHeight);
                  final JButton reset = new JButton("Reset");
                  buttons.add(reset);
                  panel.add(buttons, BorderLayout.SOUTH);
                  fitWidth.addActionListener(
                      event -> {
                        final double screenWidth = frame.getMapPanel().getWidth();
                        final double mapWidth = frame.getMapPanel().getImageWidth();
                        double ratio = screenWidth / mapWidth;
                        ratio = Math.max(frame.getMapPanel().getMinScale(), ratio);
                        ratio = Math.min(1, ratio);
                        setSpinnerValue(model, (int) Math.round(ratio * 100));
                      });
                  fitHeight.addActionListener(
                      event -> {
                        final double screenHeight = frame.getMapPanel().getHeight();
                        final double mapHeight = frame.getMapPanel().getImageHeight();
                        double ratio = screenHeight / mapHeight;
                        ratio = Math.max(frame.getMapPanel().getMinScale(), ratio);
                        setSpinnerValue(model, (int) Math.round(ratio * 100));
                      });
                  reset.addActionListener(event -> setSpinnerValue(model, 100));
                  final int result =
                      JOptionPane.showOptionDialog(
                          frame,
                          panel,
                          "Choose Map Zoom",
                          JOptionPane.OK_CANCEL_OPTION,
                          JOptionPane.PLAIN_MESSAGE,
                          null,
                          new String[] {"OK", "Cancel"},
                          0);
                  if (result != 0) {
                    return;
                  }
                  final Number value = (Number) model.getValue();
                  frame.getMapPanel().setScale(value.doubleValue() / 100);
                })
            .build());
  }

  private void setSpinnerValue(SpinnerNumberModel model, double value) {
    // Some L&Fs hit errors when setValue() is called with a non-Double param.
    // This wrapper function ensures that we're always setting it as a double.
    // See: https://github.com/triplea-game/triplea/issues/12126
    model.setValue(value);
  }

  private void addUnitSizeMenu() {
    final NumberFormat decimalFormat = new DecimalFormat("00.##");
    // This is the action listener used
    class UnitSizeAction extends AbstractAction {
      private static final long serialVersionUID = -6280511505686687867L;
      private final double scaleFactor;

      private UnitSizeAction(final double scaleFactor) {
        super(decimalFormat.format(scaleFactor * 100) + "%");
        this.scaleFactor = scaleFactor;
      }

      @Override
      public void actionPerformed(final ActionEvent e) {
        uiContext.setUnitScaleFactor(scaleFactor);
        frame.getMapPanel().resetMap();
      }
    }

    final JMenu unitSizeMenu =
        new JMenuBuilder("Unit Size", Mnemonic.UNIT_SIZE_SUBMENU.getMnemonicCode()).build();
    final ButtonGroup unitSizeGroup = new ButtonGroup();
    final JRadioButtonMenuItem radioItem125 = new JRadioButtonMenuItem(new UnitSizeAction(1.25));
    final JRadioButtonMenuItem radioItem100 = new JRadioButtonMenuItem(new UnitSizeAction(1.0));
    radioItem100.setMnemonic(Mnemonic.UNIT_SIZE_1.getValue());
    final JRadioButtonMenuItem radioItem87 = new JRadioButtonMenuItem(new UnitSizeAction(0.875));
    final JRadioButtonMenuItem radioItem83 = new JRadioButtonMenuItem(new UnitSizeAction(0.8333));
    radioItem83.setMnemonic(Mnemonic.UNIT_SIZE_83.getValue());
    final JRadioButtonMenuItem radioItem75 = new JRadioButtonMenuItem(new UnitSizeAction(0.75));
    radioItem75.setMnemonic(Mnemonic.UNIT_SIZE_75.getValue());
    final JRadioButtonMenuItem radioItem66 = new JRadioButtonMenuItem(new UnitSizeAction(0.6666));
    radioItem66.setMnemonic(Mnemonic.UNIT_SIZE_66.getValue());
    final JRadioButtonMenuItem radioItem56 = new JRadioButtonMenuItem(new UnitSizeAction(0.5625));
    final JRadioButtonMenuItem radioItem50 = new JRadioButtonMenuItem(new UnitSizeAction(0.5));
    radioItem50.setMnemonic(Mnemonic.UNIT_SIZE_5.getValue());
    unitSizeGroup.add(radioItem125);
    unitSizeGroup.add(radioItem100);
    unitSizeGroup.add(radioItem87);
    unitSizeGroup.add(radioItem83);
    unitSizeGroup.add(radioItem75);
    unitSizeGroup.add(radioItem66);
    unitSizeGroup.add(radioItem56);
    unitSizeGroup.add(radioItem50);
    radioItem100.setSelected(true);
    // select the closest to the default size
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
      log.error("default unit size does not match any menu item");
    }
    unitSizeMenu.add(radioItem125);
    unitSizeMenu.add(radioItem100);
    unitSizeMenu.add(radioItem87);
    unitSizeMenu.add(radioItem83);
    unitSizeMenu.add(radioItem75);
    unitSizeMenu.add(radioItem66);
    unitSizeMenu.add(radioItem56);
    unitSizeMenu.add(radioItem50);
    add(unitSizeMenu);
  }

  private void addMapSkinsMenu() {
    final JMenu mapSubMenu =
        new JMenuBuilder("Map Skins", Mnemonic.MAP_SKINS_SUBMENU.getMnemonicCode()).build();
    add(mapSubMenu);
    final ButtonGroup mapButtonGroup = new ButtonGroup();
    final Collection<UiContext.MapSkin> skins =
        uiContext.getSkins(frame.getGame().getData().getMapName());
    mapSubMenu.setEnabled(skins.size() > 1);
    for (final UiContext.MapSkin mapSkin : skins) {
      final JMenuItem mapMenuItem = new JRadioButtonMenuItem(mapSkin.getSkinName());
      mapButtonGroup.add(mapMenuItem);
      mapSubMenu.add(mapMenuItem);
      mapMenuItem.setSelected(mapSkin.isCurrentSkin());
      mapMenuItem.addActionListener(
          e -> {
            try {
              frame.changeMapSkin(mapSkin.getSkinName());
              if (uiContext.getMapData().getHasRelief()) {
                showMapDetails.setSelected(true);
              }
              showMapDetails.setEnabled(uiContext.getMapData().getHasRelief());
            } catch (final Exception exception) {
              log.error("Error Changing Map Skin2", exception);
            }
          });
    }
  }

  private void addShowMapDetails() {
    showMapDetails =
        new JMenuItemCheckBoxBuilder(
                "Show Map Details", Mnemonic.SHOW_MAP_DETAILS.getMnemonicCode())
            .selected(TileImageFactory.getShowReliefImages())
            .actionListener(
                isSelected -> {
                  if (TileImageFactory.getShowReliefImages() == Boolean.TRUE.equals(isSelected)) {
                    return;
                  }
                  TileImageFactory.setShowReliefImages(isSelected);
                  ThreadRunner.runInNewThread(
                      () -> frame.getMapPanel().updateCountries(gameMapTerritories));
                })
            .build();
    add(showMapDetails);
  }

  private void addShowMapBlends() {
    JCheckBoxMenuItem showMapBlends = new JCheckBoxMenuItem("Show Map Blends");
    showMapBlends.setMnemonic(Mnemonic.SHOW_MAP_BLENDS.getValue());
    if (uiContext.getMapData().getHasRelief()
        && showMapDetails.isEnabled()
        && showMapDetails.isSelected()) {
      showMapBlends.setEnabled(true);
      showMapBlends.setSelected(TileImageFactory.getShowMapBlends());
    } else {
      showMapBlends.setSelected(false);
      showMapBlends.setEnabled(false);
    }
    showMapBlends.addActionListener(
        e -> {
          if (TileImageFactory.getShowMapBlends() == showMapBlends.isSelected()) {
            return;
          }
          TileImageFactory.setShowMapBlends(showMapBlends.isSelected());
          TileImageFactory.setShowMapBlendMode(uiContext.getMapData().getMapBlendMode());
          TileImageFactory.setShowMapBlendAlpha(uiContext.getMapData().getMapBlendAlpha());
          new Thread(
                  () -> frame.getMapPanel().updateCountries(gameMapTerritories),
                  "Show map Blends thread")
              .start();
        });
    add(showMapBlends);
  }

  private void addShowUnitsMenu() {
    final JCheckBoxMenuItem showUnitsBox = new JCheckBoxMenuItem("Show Units");
    showUnitsBox.setMnemonic(Mnemonic.SHOW_UNIT.getValue());
    showUnitsBox.setSelected(true);
    showUnitsBox.addActionListener(
        e -> {
          uiContext.setShowUnits(showUnitsBox.isSelected());
          frame.getMapPanel().resetMap();
        });
    add(showUnitsBox);
  }

  private void addShowZoomMenu() {
    final JCheckBoxMenuItem showMapZoomBox = new JCheckBoxMenuItem("Show Zoom Percentage");

    showMapZoomBox.addActionListener(
        e -> this.frame.getBottomBar().setMapZoomEnabled(showMapZoomBox.isSelected()));

    add(showMapZoomBox);
  }

  private void addShowUnitsInStatusBarMenu() {
    JCheckBoxMenuItem checkbox = new JCheckBoxMenuItem("Show Units in Status Bar");
    checkbox.setSelected(true);
    checkbox.addActionListener(
        e -> {
          uiContext.setShowUnitsInStatusBar(checkbox.isSelected());
          // Trigger a bottom bar update.
          frame.getBottomBar().setTerritory(frame.getMapPanel().getCurrentTerritory());
        });
    add(checkbox);
  }

  private void addMapFontAndColorEditorMenu() {
    add(
        new JMenuItemBuilder("Map Font and Color", Mnemonic.MAP_FONT_OPTIONS.getMnemonicCode())
            .actionListener(
                () -> {
                  final List<IEditableProperty<?>> properties = new ArrayList<>();
                  final NumberProperty fontsize =
                      new NumberProperty(
                          "Font Size", null, 60, 0, MapImage.getPropertyMapFont().getSize());
                  final ColorProperty territoryNameColor =
                      new ColorProperty(
                          "Territory Name and PU Color",
                          null,
                          MapImage.getPropertyTerritoryNameAndPuAndCommentColor());
                  final ColorProperty unitCountColor =
                      new ColorProperty(
                          "Unit Count Color", null, MapImage.getPropertyUnitCountColor());
                  final ColorProperty unitCountOutline =
                      new ColorProperty(
                          "Unit Count Outline", null, MapImage.getPropertyUnitCountOutline());
                  final ColorProperty factoryDamageColor =
                      new ColorProperty(
                          "Factory Damage Color",
                          null,
                          MapImage.getPropertyUnitFactoryDamageColor());
                  final ColorProperty factoryDamageOutline =
                      new ColorProperty(
                          "Factory Damage Outline",
                          null,
                          MapImage.getPropertyUnitFactoryDamageOutline());
                  final ColorProperty hitDamageColor =
                      new ColorProperty(
                          "Hit Damage Color", null, MapImage.getPropertyUnitHitDamageColor());
                  final ColorProperty hitDamageOutline =
                      new ColorProperty(
                          "Hit Damage Outline", null, MapImage.getPropertyUnitHitDamageOutline());
                  properties.add(fontsize);
                  properties.add(territoryNameColor);
                  properties.add(unitCountColor);
                  properties.add(unitCountOutline);
                  properties.add(factoryDamageColor);
                  properties.add(factoryDamageOutline);
                  properties.add(hitDamageColor);
                  properties.add(hitDamageOutline);
                  final PropertiesUi pui = new PropertiesUi(properties, true);
                  final JPanel ui = new JPanel();
                  ui.setLayout(new BorderLayout());
                  ui.add(pui, BorderLayout.CENTER);
                  ui.add(
                      new JLabel(
                          "<html>Change the font and color of 'text' (not pictures) on the map. "
                              + "<br /><em>(Some people encounter problems with the color picker, "
                              + "and this "
                              + "<br />is a bug outside of triplea, located in the 'look and feel' "
                              + "that "
                              + "<br />you are using. If you have an error come up, try switching to "
                              + "the "
                              + "<br />basic 'look and feel', then setting the color, then switching "
                              + "back.)</em></html>"),
                      BorderLayout.NORTH);
                  final Object[] options = {"Set Properties", "Reset To Default", "Cancel"};
                  final int result =
                      JOptionPane.showOptionDialog(
                          frame,
                          ui,
                          "Map Font and Color",
                          JOptionPane.YES_NO_CANCEL_OPTION,
                          JOptionPane.PLAIN_MESSAGE,
                          null,
                          options,
                          2);
                  if (result == 1) {
                    MapImage.resetPropertyMapFont();
                    MapImage.resetPropertyTerritoryNameAndPuAndCommentColor();
                    MapImage.resetPropertyUnitCountColor();
                    MapImage.resetPropertyUnitCountOutline();
                    MapImage.resetPropertyUnitFactoryDamageColor();
                    MapImage.resetPropertyUnitFactoryDamageOutline();
                    MapImage.resetPropertyUnitHitDamageColor();
                    MapImage.resetPropertyUnitHitDamageOutline();
                    frame.getMapPanel().resetMap();
                  } else if (result == 0) {
                    MapImage.setPropertyMapFont(
                        new Font(MapImage.FONT_FAMILY_DEFAULT, Font.BOLD, fontsize.getValue()));
                    MapImage.setPropertyTerritoryNameAndPuAndCommentColor(
                        territoryNameColor.getValue());
                    MapImage.setPropertyUnitCountColor(unitCountColor.getValue());
                    MapImage.setPropertyUnitCountOutline(unitCountOutline.getValue());
                    MapImage.setPropertyUnitFactoryDamageColor(factoryDamageColor.getValue());
                    MapImage.setPropertyUnitFactoryDamageOutline(factoryDamageOutline.getValue());
                    MapImage.setPropertyUnitHitDamageColor(hitDamageColor.getValue());
                    MapImage.setPropertyUnitHitDamageOutline(hitDamageOutline.getValue());
                    frame.getMapPanel().resetMap();
                  }
                })
            .build());
  }

  private void addShowTerritoryEffects() {
    final JCheckBoxMenuItem territoryEffectsBox = new JCheckBoxMenuItem("Show TerritoryEffects");
    territoryEffectsBox.setMnemonic(Mnemonic.SHOW_TERRITORY_EFFECTS.getValue());
    territoryEffectsBox.addActionListener(
        e -> {
          uiContext.setShowTerritoryEffects(territoryEffectsBox.isSelected());
          frame.getMapPanel().resetMap();
        });
    add(territoryEffectsBox);
    territoryEffectsBox.setSelected(true);
  }

  private void addLockMap() {
    add(
        new JMenuItemCheckBoxBuilder("Lock Map", Mnemonic.LOCK_MAP.getMnemonicCode())
            .accelerator(KeyCode.L)
            .bindSetting(ClientSetting.lockMap)
            .build());
  }

  private void addFlagDisplayModeMenu() {
    // 2.0 to 1.9 compatibility hack. Can be removed when all players that have played a 2.0
    // prelease have launched a game containing this patch. When going from 2.0 to 1.9,
    // 1.9 will crash due to an enum value not found error when loading 'DRAW_MODE'
    final Preferences prefs = Preferences.userNodeForPackage(getClass());
    if (prefs.get("DRAW_MODE", null) != null) {
      prefs.remove("DRAW_MODE");
      try {
        prefs.flush();
      } catch (final BackingStoreException ignored) {
        // ignore
      }
    }

    final JMenu flagDisplayMenu =
        new JMenuBuilder("Flag Display", Mnemonic.FLAG_SUBMENU.getMnemonicCode()).build();

    final ButtonGroup flagsDisplayGroup = new ButtonGroup();

    final JRadioButtonMenuItem noFlags =
        new JMenuItemBuilder("Off", Mnemonic.FLAGS_OFF.getMnemonicCode())
            .actionListener(
                () ->
                    FlagDrawMode.toggleDrawMode(
                        UnitsDrawer.UnitFlagDrawMode.NONE, frame.getMapPanel()))
            .buildRadio(flagsDisplayGroup);

    final JRadioButtonMenuItem smallFlags =
        new JMenuItemBuilder("Small", Mnemonic.FLAGS_SMALL.getMnemonicCode())
            .actionListener(
                () ->
                    FlagDrawMode.toggleDrawMode(
                        UnitsDrawer.UnitFlagDrawMode.SMALL_FLAG, frame.getMapPanel()))
            .buildRadio(flagsDisplayGroup);

    final JRadioButtonMenuItem largeFlags =
        new JMenuItemBuilder("Large", Mnemonic.FLAGS_LARGE.getMnemonicCode())
            .actionListener(
                () ->
                    FlagDrawMode.toggleDrawMode(
                        UnitsDrawer.UnitFlagDrawMode.LARGE_FLAG, frame.getMapPanel()))
            .buildRadio(flagsDisplayGroup);

    flagDisplayMenu.add(noFlags);
    flagDisplayMenu.add(smallFlags);
    flagDisplayMenu.add(largeFlags);

    // Add a menu listener to update the checked state of the items, as the flag state
    // may change externally (e.g. via UnitScroller UI).
    flagDisplayMenu.addMenuListener(
        new MenuListener() {
          @Override
          public void menuSelected(final MenuEvent e) {
            final var drawModel = ClientSetting.unitFlagDrawMode.getValueOrThrow();
            noFlags.setSelected(drawModel == UnitsDrawer.UnitFlagDrawMode.NONE);
            smallFlags.setSelected(drawModel == UnitsDrawer.UnitFlagDrawMode.SMALL_FLAG);
            largeFlags.setSelected(drawModel == UnitsDrawer.UnitFlagDrawMode.LARGE_FLAG);
          }

          @Override
          public void menuDeselected(final MenuEvent e) {
            // not needed interface method
          }

          @Override
          public void menuCanceled(final MenuEvent e) {
            // not needed interface method
          }
        });
    add(flagDisplayMenu);
  }

  private void addChatTimeMenu() {
    if (frame.hasChat()) {
      add(
          new JMenuItemCheckBoxBuilder("Show Chat Times", Mnemonic.SHOW_CHAT_TIME.getMnemonicCode())
              .bindSetting(ClientSetting.showChatTimeSettings)
              .build());
    }
  }

  private void addFindTerritory() {
    add(
        new JMenuItemBuilder(
                new FindTerritoryAction(frame), Mnemonic.FIND_TERRITORY.getMnemonicCode())
            .accelerator(Mnemonic.FIND_TERRITORY.getMnemonicCode())
            .build());
  }
}
