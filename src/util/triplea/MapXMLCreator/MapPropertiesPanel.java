package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.image.FileOpen;

/**
 * 
 * @author Erik von der Osten
 * 
 */
public class MapPropertiesPanel {
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel m_stepActionPanel) {

		if (!MapXMLHelper.s_xmlStrings.containsKey("info_@name"))
		{
			for (final String newKey : new String[] { "info_@name", "info_@version" })
			{
				MapXMLHelper.s_xmlStrings.put(newKey, "");
			}
		}
		
		if (MapXMLHelper.s_resourceList.isEmpty())
			MapXMLHelper.s_resourceList.add("");

		final JTextField tMapName = new JTextField(MapXMLHelper.s_xmlStrings.get("info_@name"));
		final JTextField tMapVersion = new JTextField(MapXMLHelper.s_xmlStrings.get("info_@version"));
		final JTextField tResourceName = new JTextField(MapXMLHelper.s_resourceList.get(0));
		final JTextField tMapImageFile = new JTextField((MapXMLCreator.s_mapImageFile==null?"":MapXMLCreator.s_mapImageFile.getAbsolutePath()));
		final JTextField tCentersFile = new JTextField((MapXMLCreator.s_mapCentersFile==null?"":MapXMLCreator.s_mapCentersFile.getAbsolutePath()));
		final JTextField tWaterFilter = new JTextField(MapXMLCreator.s_waterFilterString);
		final JLabel lMapName = new JLabel("Map Name:");
		final JLabel lMapNameExample = new JLabel("e.g. 'Revised'");
		final JLabel lMapVersion = new JLabel("Map Version:");
		final JLabel lMapVersionExample = new JLabel("e.g. '1.2.0.1'");
		final JLabel lResourceName = new JLabel("Resource Name:");
		final JLabel lResourceNameExample = new JLabel("e.g. 'PUs'");
		final JLabel lMapImageFile = new JLabel("Map Image File:");
		final JButton bSelectMapImageFile = new JButton("Browse");
		final JLabel lCentersFile = new JLabel("Map Centers File:");
		final JButton bSelectCentersFile = new JButton("Browse");
		final JLabel lWaterFilter = new JLabel("Water Territory Filter");
		final JLabel lWaterFilterExample = new JLabel("e.g. 'Sea Zone'");		
		
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.rowHeights = new int[] { 32, 32, 32, 32, 32, 32, 32, 0 };
		gbl_panel_1.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		m_stepActionPanel.setLayout(gbl_panel_1);
		
		//Map Name
		GridBagConstraints gbc_lMapName = new GridBagConstraints();
		gbc_lMapName.insets = new Insets(0, 0, 5, 5);
		gbc_lMapName.anchor = GridBagConstraints.NORTHEAST;
		gbc_lMapName.gridheight = 32;
		gbc_lMapName.gridy = 1;
		gbc_lMapName.gridx = 0;
		m_stepActionPanel.add(lMapName, gbc_lMapName);

		tMapName.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				MapXMLHelper.s_xmlStrings.put("info_@name", tMapName.getText());
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		tMapName.setMaximumSize(new Dimension(0, 0));
		tMapName.setColumns(30);
		GridBagConstraints gbc_m_tMapName = (GridBagConstraints) gbc_lMapName.clone();
		gbc_m_tMapName.anchor = GridBagConstraints.NORTH;
		gbc_m_tMapName.gridx = 1;
		tMapName.setMaximumSize(tMapName.getPreferredSize());
		m_stepActionPanel.add(tMapName, gbc_m_tMapName);
		
		GridBagConstraints gbc_lMapNameExample = (GridBagConstraints) gbc_lMapName.clone();
		gbc_lMapNameExample.anchor = GridBagConstraints.NORTHWEST;
		gbc_lMapNameExample.gridx = 2;
		m_stepActionPanel.add(lMapNameExample, gbc_lMapNameExample);
		
		//Map Version
		GridBagConstraints gbc_lMapVersion = (GridBagConstraints) gbc_lMapName.clone();
		gbc_lMapVersion.gridy = 2;
		m_stepActionPanel.add(lMapVersion, gbc_lMapVersion);

		tMapVersion.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				MapXMLHelper.s_xmlStrings.put("info_@version", tMapVersion.getText());
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		tMapVersion.setMaximumSize(new Dimension(0, 0));
		tMapVersion.setColumns(30);
		GridBagConstraints gbc_m_tMapVersion = (GridBagConstraints) gbc_lMapName.clone();
		gbc_m_tMapVersion.gridx = 1;
		gbc_m_tMapVersion.gridy = 2;
		m_stepActionPanel.add(tMapVersion, gbc_m_tMapVersion);
		
		
		GridBagConstraints gbc_lMapVersionExample = (GridBagConstraints) gbc_lMapNameExample.clone();
		gbc_lMapVersionExample.gridx = 2;
		gbc_lMapVersionExample.gridy = 2;
		m_stepActionPanel.add(lMapVersionExample, gbc_lMapVersionExample);
		
		//Resource Name
		GridBagConstraints gbc_lResourceName = (GridBagConstraints) gbc_lMapName.clone();
		gbc_lResourceName.gridx = 0;
		gbc_lResourceName.gridy = 3;
		m_stepActionPanel.add(lResourceName, gbc_lResourceName);
		
		tResourceName.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				MapXMLHelper.s_resourceList.add(0, tResourceName.getText());
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		tResourceName.setMaximumSize(new Dimension(0, 0));
		tResourceName.setColumns(30);
		GridBagConstraints gbc_m_tResourceName = (GridBagConstraints) gbc_lMapName.clone();
		gbc_m_tResourceName.gridx = 1;
		gbc_m_tResourceName.gridy = 3;
		m_stepActionPanel.add(tResourceName, gbc_m_tResourceName);
		
		GridBagConstraints gbc_lResourceNameExample = (GridBagConstraints) gbc_lMapNameExample.clone();
		gbc_lResourceNameExample.gridx = 2;
		gbc_lResourceNameExample.gridy = 3;
		m_stepActionPanel.add(lResourceNameExample, gbc_lResourceNameExample);
		
		//Map Image File
		GridBagConstraints gbc_lMapImageFile = (GridBagConstraints) gbc_lMapName.clone();
		gbc_lMapImageFile.gridx = 0;
		gbc_lMapImageFile.gridy = 4;
		m_stepActionPanel.add(lMapImageFile, gbc_lMapImageFile);
		
		tMapImageFile.setEnabled(false);
		tMapImageFile.setMaximumSize(new Dimension(0, 0));
		tMapImageFile.setColumns(30);
		GridBagConstraints gbc_m_tMapImageFile = (GridBagConstraints) gbc_lMapName.clone();
		gbc_m_tMapImageFile.gridx = 1;
		gbc_m_tMapImageFile.gridy = 4;
		tMapImageFile.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseClicked(MouseEvent e) {
				bSelectMapImageFile.doClick();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
		});
		m_stepActionPanel.add(tMapImageFile, gbc_m_tMapImageFile);
		
		bSelectMapImageFile.addActionListener(new AbstractAction("Select Map Image File")
		{
			private static final long serialVersionUID = 3918797244306320614L;
			
			public void actionPerformed(final ActionEvent e)
			{
				selectMapImageFile();
				if (MapXMLCreator.s_mapImageFile != null)
				{
					tMapImageFile.setText(MapXMLCreator.s_mapImageFile.getAbsolutePath());
					if (MapXMLCreator.s_mapFolderLocation != null && MapXMLCreator.s_mapCentersFile == null)
					{
						final File fileGuess = new File(MapXMLCreator.s_mapFolderLocation, "centers.txt");
						if (fileGuess.exists()
									&& JOptionPane.showConfirmDialog(new JPanel(), "A centers.txt file was found in the map's folder, do you want to use the file to supply the territories names?",
												"File Suggestion", 1) == 0)
						{
							MapXMLCreator.s_mapCentersFile = fileGuess;
							tCentersFile.setText(MapXMLCreator.s_mapCentersFile.getAbsolutePath());
							tWaterFilter.setEnabled(true);
						}
					}
					if (MapXMLCreator.s_mapImageFile != null && tMapName.getText().isEmpty())
					{
						String mapFileName = MapXMLCreator.s_mapImageFile.getName();
						mapFileName = mapFileName.substring(0, mapFileName.lastIndexOf("."));
						tMapName.setText(mapFileName);
						MapXMLHelper.s_xmlStrings.put("info_@name", mapFileName);
					}
				}
			}
		});
		GridBagConstraints gbc_bSelectMapImageFile = (GridBagConstraints) gbc_lMapNameExample.clone();
		gbc_bSelectMapImageFile.gridx = 2;
		gbc_bSelectMapImageFile.gridy = 4;
		m_stepActionPanel.add(bSelectMapImageFile, gbc_bSelectMapImageFile);
		
		//Map Centers File
		GridBagConstraints gbc_lCentersFile = (GridBagConstraints) gbc_lMapName.clone();
		gbc_lCentersFile.gridx = 0;
		gbc_lCentersFile.gridy = 5;
		m_stepActionPanel.add(lCentersFile, gbc_lCentersFile);
		
		tCentersFile.setEnabled(false);
		tCentersFile.setMaximumSize(new Dimension(0, 0));
		tCentersFile.setColumns(30);
		GridBagConstraints gbc_m_tCentersFile = (GridBagConstraints) gbc_lMapName.clone();
		gbc_m_tCentersFile.gridx = 1;
		gbc_m_tCentersFile.gridy = 5;
		tCentersFile.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseClicked(MouseEvent e) {
				bSelectCentersFile.doClick();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
		});
		m_stepActionPanel.add(tCentersFile, gbc_m_tCentersFile);
		
		GridBagConstraints gbc_bSelectCentersFile = (GridBagConstraints) gbc_lMapNameExample.clone();
		gbc_bSelectCentersFile.gridx = 2;
		gbc_bSelectCentersFile.gridy = 5;
		bSelectCentersFile.addActionListener(new AbstractAction("Select Centers File")
		{
			private static final long serialVersionUID = -1634037486288735207L;

			public void actionPerformed(final ActionEvent e)
			{
				selectCentersFile();
				if (MapXMLCreator.s_mapCentersFile != null)
					tWaterFilter.setEnabled(true);
			}
		});
		m_stepActionPanel.add(bSelectCentersFile, gbc_bSelectCentersFile);
		
		//Water Territory Filter
		GridBagConstraints gbc_lWaterFilter = (GridBagConstraints) gbc_lMapName.clone();
		gbc_lWaterFilter.gridy = 6;
		gbc_lWaterFilter.gridx = 0;
		gbc_lWaterFilter.anchor = GridBagConstraints.NORTHWEST;
		m_stepActionPanel.add(lWaterFilter, gbc_lWaterFilter);

		tWaterFilter.setEnabled(MapXMLCreator.s_mapCentersFile != null);
		tWaterFilter.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				MapXMLCreator.s_waterFilterString = tWaterFilter.getText();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		tWaterFilter.setColumns(30);
		GridBagConstraints gbc_m_tWaterFilter = (GridBagConstraints) gbc_lMapName.clone();
		gbc_m_tWaterFilter.gridx = 1;
		gbc_m_tWaterFilter.gridy = 6;
		tWaterFilter.setMaximumSize(tWaterFilter.getPreferredSize());
		m_stepActionPanel.add(tWaterFilter, gbc_m_tWaterFilter);
		
		GridBagConstraints gbc_lWaterFilterExample = (GridBagConstraints) gbc_lMapNameExample.clone();
		gbc_lWaterFilterExample.gridx = 2;
		gbc_lWaterFilterExample.gridy = 6;
		m_stepActionPanel.add(lWaterFilterExample, gbc_lWaterFilterExample);
		
		mapXMLCreator.m_bAuto.setEnabled(false);
	}

	public static void selectCentersFile()
	{
		final FileOpen centersSelection = new FileOpen("Select center.txte File", MapXMLCreator.s_mapImageFile, ".txt");
		if (centersSelection.getFile() != null)
		{
			final File mapCentersFile = centersSelection.getFile();
			if (mapCentersFile.exists())
			{
				if (MapXMLCreator.s_mapFolderLocation == null)
				{
					MapXMLCreator.s_mapFolderLocation = mapCentersFile.getParentFile();
					System.setProperty(MapXMLCreator.TRIPLEA_MAP_FOLDER, MapXMLCreator.s_mapFolderLocation.getPath());
				}
				MapXMLCreator.s_mapCentersFile = mapCentersFile;
				
			}
		}
	}

	public static void selectMapImageFile()
	{
		final FileOpen mapSelection = new FileOpen("Select Map Image File", MapXMLCreator.s_mapImageFile, ".gif", ".png");
		if (mapSelection.getFile() != null)
		{
			
			final File mapImageFile = mapSelection.getFile();
			if (mapImageFile.exists())
			{
				if (MapXMLCreator.s_mapFolderLocation == null)
				{
					MapXMLCreator.s_mapFolderLocation = mapImageFile.getParentFile();
					System.setProperty(MapXMLCreator.TRIPLEA_MAP_FOLDER, MapXMLCreator.s_mapFolderLocation.getPath());
				}
				MapXMLCreator.s_mapImageFile = mapImageFile;
				
			}
		}
	}

}
