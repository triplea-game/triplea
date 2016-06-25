package tools.map.xml.creator;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import tools.image.FileOpen;

public class MapXmlUIHelper {

  /**
   *
   * @param title - the title string for the dialog
   * @param message - the Object to display
   * @param messageType - an integer designating the kind of message this is, primarily used to determine the icon from
   *        the pluggable Look and Feel: ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or
   *        PLAIN_MESSAGE
   * @return an integer indicating the option chosen by the user, or CLOSED_OPTION if the user closed the dialog
   * @throws HeadlessException
   */
  public static int showYesNoOptionDialog(
      final String title, final Object message,
      final int messageType)
      throws HeadlessException {
    return MapXmlUIHelper.showOptionDialog(
        title,
        message,
        JOptionPane.YES_NO_OPTION,
        messageType, JOptionPane.NO_OPTION);
  }


  public static void addNewFocusListenerForTextField(final JTextField textField, final Runnable r) {
    textField.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(final FocusEvent arg0) {
        r.run();
      }

      @Override
      public void focusGained(final FocusEvent arg0) {
        textField.selectAll();
      }
    });
  }

  /**
   *
   * @param title - the title string for the dialog
   * @param message - the Object to display
   * @param messageType - an integer designating the kind of message this is, primarily used to determine the icon from
   *        the pluggable Look and Feel: ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or
   *        PLAIN_MESSAGE
   * @param initialValue - the object that represents the default selection for the dialog; only meaningful if options
   *        is used; can be null
   * @return an integer indicating the option chosen by the user, or CLOSED_OPTION if the user closed the dialog
   * @throws HeadlessException
   */
  public static int showOptionDialog(
      final String title, final Object message,
      final int optionType,
      final int messageType,
      final Object initialValue)
      throws HeadlessException {
    return JOptionPane.showOptionDialog(null,
        message,
        title,
        optionType,
        messageType, null, null, initialValue);
  }

  final public static Font defaultMapXMLCreatorFont = MapXmlUIHelper.getDefaultMapXMLCreatorFont();
  final public static String defaultMapXMLCreatorFontName = MapXmlUIHelper.getDefaultMapXMLCreatorFontName();
  final public static String preferredMapXMLCreatorFontName = "Tahoma";

  public static Font getDefaultMapXMLCreatorFont() {
    return defaultMapXMLCreatorFont;
  }

  /**
   * Tries to find preferredMapXMLCreatorFontName font or takes the first in the list of available fonts.
   *
   * @return default font name for XML Creator
   */
  public static String getDefaultMapXMLCreatorFontName() {
    final String[] availableFontFamilyNames =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    for (final String fontName : availableFontFamilyNames) {
      if (fontName.equals(preferredMapXMLCreatorFontName)) {
        return fontName;
      }
    }
    return availableFontFamilyNames[0];
  }

  /**
   * Creates a button with text, key code and action listener.
   *
   * @param buttonText - the text of the button
   * @param mnemonic - the key code which represents the mnemonic
   * @param actionListener - the ActionListener to be added
   * @return
   */
  public static JButton createButton(final String buttonText, final int mnemonic, final ActionListener actionListener) {
    final JButton newButton = MapXmlUIHelper.createButton(buttonText, mnemonic);
    newButton.addActionListener(actionListener);
    return newButton;
  }

  /**
   * Creates a button with text and key code.
   *
   * @param buttonText - the text of the button
   * @param mnemonic - the key code which represents the mnemonic
   * @return
   */
  public static JButton createButton(final String buttonText, final int mnemonic) {
    final JButton newButton = new JButton(buttonText);
    newButton.setMnemonic(mnemonic);
    newButton.setFont(defaultMapXMLCreatorFont);
    newButton.setMargin(new Insets(2, 5, 2, 5));
    return newButton;
  }

  /**
   * @param gbcToClone base GridBagConstraints object
   * @param gridx gridx value for new GridBagConstraints object
   * @param gridy gridy value for new GridBagConstraints object
   * @param anchor anchor value for new GridBagConstraints object
   * @return cloned GridBagConstraints object with provided gridx and gridy values
   */
  public static GridBagConstraints getGBCCloneWith(final GridBagConstraints gbcToClone, final int gridx,
      final int gridy, final int anchor) {
    final GridBagConstraints gbcNew = MapXmlUIHelper.getGBCCloneWith(gbcToClone, gridx, gridy);
    gbcNew.anchor = anchor;
    return gbcNew;
  }

  /**
   * @param gbcToClone base GridBagConstraints object
   * @param gridx gridx value for new GridBagConstraints object
   * @param gridy gridy value for new GridBagConstraints object
   * @return cloned GridBagConstraints object with provided gridx and gridy values
   */
  public static GridBagConstraints getGBCCloneWith(final GridBagConstraints gbcToClone, final int gridx,
      final int gridy) {
    final GridBagConstraints gbcNew = (GridBagConstraints) gbcToClone.clone();
    gbcNew.gridx = gridx;
    gbcNew.gridy = gridy;
    return gbcNew;
  }

  public static FileOpen selectFile(final String filename, final File currentDirectory, final String... extensions) {
    final FileOpen gameXmlFileOpen =
        new FileOpen("Load " + filename, currentDirectory, MapXmlCreator.FILE_NAME_ENDING_XML);
    if (gameXmlFileOpen.getPathString() != null) {
      return gameXmlFileOpen;
    }
    JOptionPane.showMessageDialog(null,
        "No valid selection for " + filename + "!",
        "Error while selecting " + filename, JOptionPane.ERROR_MESSAGE);
    System.exit(1);//TODO make sure this is being removed, once this method is used other than on startup
    return null;//This is never going to be returned
  }

  /**
   *
   * @param gridx - gridx value for new GridBagConstraints object
   * @param gridy - gridx value for new GridBagConstraints object
   * @return new GridBagConstraints based on gbcTemplate and provided data
   */
  static public GridBagConstraints getGbcDefaultTemplateWith(final int gridx, final int gridy) {
    return getGBCCloneWith(MapXmlUIHelper.gbcTemplate, gridx, gridy);
  }

  /**
   *
   * @return new GridBagConstraints object with:
   *         <ul>
   *         <li>insets = Insets(0, 0, 5, 5)
   *         <li>anchor = GridBagConstraints.WEST
   *         </ul>
   */
  private static GridBagConstraints getNewGbcTemplate() {
    final GridBagConstraints gbcTemplateNew = new GridBagConstraints();
    gbcTemplateNew.insets = new Insets(0, 0, 5, 5);
    gbcTemplateNew.anchor = GridBagConstraints.WEST;
    ;
    return gbcTemplateNew;
  }

  static GridBagConstraints gbcTemplate = getNewGbcTemplate();

}
