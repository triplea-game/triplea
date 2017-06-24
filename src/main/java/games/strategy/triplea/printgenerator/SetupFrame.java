package games.strategy.triplea.printgenerator;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import games.strategy.engine.data.GameData;
import games.strategy.ui.SwingComponents;

public class SetupFrame extends JPanel {
  private static final long serialVersionUID = 7308943603423170303L;
  private final JTextField outField;
  private final JFileChooser outChooser;
  private final JRadioButton originalState;
  private final GameData data;
  private File outDir;

  /**
   * Creates a new SetupFrame.
   */
  public SetupFrame(final GameData data) {
    super(new BorderLayout());
    final JLabel m_info1 = new JLabel();
    final JLabel m_info2 = new JLabel();
    final JLabel m_info3 = new JLabel();
    this.data = data;
    final JButton m_outDirButton = new JButton();
    final JButton m_runButton = new JButton();
    outField = new JTextField(15);
    outChooser = new JFileChooser();
    outChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    final JRadioButton m_currentState = new JRadioButton();
    originalState = new JRadioButton();
    final ButtonGroup m_radioButtonGroup = new ButtonGroup();
    m_info1.setText("This utility will export the map's either current or ");
    m_info2.setText("beginning state exactly like the boardgame, so you ");
    m_info3.setText("will get Setup Charts, Unit Information, etc.");
    m_currentState.setText("Current Position/State");
    originalState.setText("Starting Position/State");
    m_radioButtonGroup.add(m_currentState);
    m_radioButtonGroup.add(originalState);
    originalState.setSelected(true);
    m_outDirButton.setText("Choose the Output Directory");
    m_outDirButton.addActionListener(e -> {
      final int returnVal = outChooser.showOpenDialog(null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File outDir = outChooser.getSelectedFile();
        outField.setText(outDir.getAbsolutePath());
      }
    });
    m_runButton.setText("Generate the Files");
    m_runButton.addActionListener(e -> {
      if (!outField.getText().equals("")) {
        outDir = new File(outField.getText());
        final PrintGenerationData printData = new PrintGenerationData();
        printData.setOutDir(outDir);
        printData.setData(this.data);
        new InitialSetup().run(printData, originalState.isSelected());
        JOptionPane.showMessageDialog(null, "Done!", "Done!", JOptionPane.INFORMATION_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(null, "You need to select an Output Directory.", "Select an Output Directory!",
            JOptionPane.ERROR_MESSAGE);
      }
    });
    final JPanel m_infoPanel = SwingComponents.gridPanel(3, 1);
    final JPanel m_textButtonRadioPanel = new JPanel(new BorderLayout());
    m_infoPanel.add(m_info1);
    m_infoPanel.add(m_info2);
    m_infoPanel.add(m_info3);
    super.add(m_infoPanel, BorderLayout.NORTH);
    m_textButtonRadioPanel.add(outField, BorderLayout.WEST);
    m_textButtonRadioPanel.add(m_outDirButton, BorderLayout.EAST);
    final JPanel panel = SwingComponents.gridPanel(1, 2);
    panel.add(originalState);
    panel.add(m_currentState);
    m_textButtonRadioPanel.add(panel, BorderLayout.SOUTH);
    super.add(m_textButtonRadioPanel, BorderLayout.CENTER);
    super.add(m_runButton, BorderLayout.SOUTH);
  }
}
