/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.triplea.oddsCalculator.zengland;
import java.applet.Applet;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.awt.*;

import javax.swing.BoxLayout;
import java.awt.event.*;

public class RollerApplet extends Applet implements Runnable, ItemListener, KeyListener, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6537455000443362041L;
	private static String AA2nd = "Axis and Allies";
	private static String AAR = "Axis and Allies Revised";
	
	private static String landBattle = "Land Zone";
	private static String seaBattle = "Sea Zone";
	
	private boolean reset = false;
	private String currentGameType = RollerApplet.AA2nd;
	private Panel unitsPanel = null;
	private Panel optionsPanel = null;
	private Vector<Panel> unitGroupPanels = new Vector<Panel>();
	private Vector<String> ool = null;
	
	private Thread battleThread = null;
	private OCBattle b = null;
	private int battles = 0;
	private float controlPercent =0.00f;
	private int controleds = 0;
	private float airWinPercent = 0.00f;
	private int airWins = 0;
	private float clearedPercent = 0.00f;
	private int cleareds = 0;
	private float indecisivePercent = 0.00f;
	private int indecisives = 0;
	private float lossPercent = 0.00f;
	private int losses = 0;
	private Label controlPercentLabel;
	private Label airWinPercentLabel;
	private Label clearedPercentLabel;
	private Label indecisivePercentLabel;
	private Label lossPercentLabel;
	private TextField rounds;
	private Checkbox conL;
	private Checkbox hasAA;
	private Choice battleType;
	private Checkbox oolCheckbox;
	private Checkbox rollRounds;
	private Label battlesLabel;
	private Hashtable<String, Integer> totRemAtt;
	private Hashtable<String, Integer> totRemDef;
	
	private static String sSubsLabel = "Super Subs";
	private static String jetPLabel = "Jet Power";
	private static String hBLabel = "Heavy Bobmers";
	private static String cBLabel = "Combined Bombardment";
	
	private Vector<Button> buttonsToProc = new Vector<Button>();
	private Vector<UnitGroupPanel> ugpsToProc = new Vector<UnitGroupPanel>();
	private Vector<ItemSelectable> selectables = new Vector<ItemSelectable>();
	private boolean clearButton;

	public void init() {
		setSize(850,550);
		
		
		setBackground(Color.WHITE);
		setLayout(new BorderLayout(10,10));

		unitsPanel = createUnitsPanel(currentGameType);
		add(unitsPanel, BorderLayout.WEST);
		
		optionsPanel = createOptionsPanel();
		add(optionsPanel, BorderLayout.EAST);

	}
	
	public void run() {
		Thread myThread = Thread.currentThread();
		while(battleThread == myThread) {
			// do thread work
			if(!reset)
			{
				resetBattle();
				if(b.getNumberOfUnits(b.getAttackers())!=0 && b.getNumberOfUnits(b.getDefenders())!=0)
				{
					b.rollBattle();
					updateBattleStats();
				}
				else
				{
					try {
					Thread.sleep(100);
					} catch (InterruptedException ie) {}
				}
				if(clearButton||buttonsToProc.size()>0||ugpsToProc.size()>0||selectables.size()>0)
				{
					setReset(true);
				}
			}
			else
			{
				processActions();
				resetBattle();
				resetStats();
				setReset(false);
			}
		}

	}
	
	private void processActions() {
		//buttonsToProc
		//ugpsToProc
		int bSize = buttonsToProc.size();
		int ugpsSize = ugpsToProc.size();
		if(bSize!=0&&ugpsSize!=0)
		{
			for(int i=bSize-1;i>=0;i--)
			{
				Button b = buttonsToProc.elementAt(i);
				UnitGroupPanel ugp = ugpsToProc.elementAt(i);
				
				String bName = b.getName();
				int bValue = Integer.valueOf(b.getLabel()).intValue();
				
				if(bName.startsWith("att"))
				{
					if(bValue==0)
					{
						ugp.getAttUnitGroup().setNumUnits(bValue);
						ugp.setRemAtt(bValue);
					}
					else
					{
						ugp.getAttUnitGroup().setNumUnits(ugp.getAttUnitGroup().getNumUnits()+bValue);
					}
					ugp.totalAttackers.setText(String.valueOf(ugp.getAttUnitGroup().getNumUnits()));
				}
				else if(bName.startsWith("def"))
				{
					if(bValue==0)
					{
						ugp.getDefUnitGroup().setNumUnits(bValue);
						ugp.setRemDef(bValue);
					}
					else
					{
						ugp.getDefUnitGroup().setNumUnits(ugp.getDefUnitGroup().getNumUnits()+bValue);
					}
					ugp.totalDefenders.setText(String.valueOf(ugp.getDefUnitGroup().getNumUnits()));
				}
				buttonsToProc.removeElementAt(i);
				ugpsToProc.removeElementAt(i);
			}
		}
		
		//selectables
		int selSize = selectables.size();
		for(int i=selSize-1;i>=0;i--)
		{
			ItemSelectable item = selectables.elementAt(i);
			
			if(item instanceof Choice)
			{
				Choice c = (Choice)item;
				String cName = c.getName();
				String sel = c.getSelectedItem();
				if(cName.equals("gameChoice"))
				{
					if(!sel.equals(currentGameType))
					{
						currentGameType = sel;
						remove(unitsPanel);
						unitsPanel = createUnitsPanel(sel);
						add(unitsPanel, BorderLayout.WEST);
						this.validate();
						resetStats();
						resetBattle();
					}
				}
				else if(cName.equals("battleType"))
				{
					resetStats();
				}
	
			}
			else if(item instanceof Checkbox)
			{
				Checkbox c = (Checkbox)item;
				String sel = c.getLabel();
				boolean selected = c.getState();
				if(sel.equals(sSubsLabel))
				{
					setSuperSubs(selected);
				}
				else if(sel.equals(hBLabel))
				{
					setHeavyBombers(selected);
				}
				else if(sel.equals(cBLabel))
				{
					setCombinedBombardment(selected);
				}
				else if(sel.equals(jetPLabel))
				{
					setJetPower(selected);
				}
				resetStats();
			}
			selectables.removeElementAt(i);
		}
		//clearButton
		if(clearButton)
		{
			int size = unitGroupPanels.size();
			for(int i=0;i<size;i++)
			{
				Panel p = unitGroupPanels.elementAt(i);
				UnitGroupPanel ugp = (UnitGroupPanel) p.getComponent(0);
				ugp.totalAttackers.setText("0");
				ugp.totalDefenders.setText("0");
				ugp.getAttUnitGroup().setNumUnits(0);
				ugp.getDefUnitGroup().setNumUnits(0);
				ugp.setRemAtt(0);
				ugp.setRemDef(0);
				resetStats();
			}
			clearButton = false;
		}
		
	}

	private void updateBattleStats() {
		battles++;
		if(b.getResultStatus() == OCBattle.CLEARED)
		{
			cleareds++;
		}
		else if(b.getResultStatus() == OCBattle.TAKEN)
		{
			controleds++;
		}
		else if(b.getResultStatus() == OCBattle.DEFENDED)
		{
			losses++;
		}
		else if(b.getResultStatus() == OCBattle.INDECISIVE)
		{
			indecisives++;
		}
		else if(b.getResultStatus() == OCBattle.CLEAREDAIR)
		{
			airWins++;
		}
		
		controlPercent = ((float)controleds/(float)battles)*100;
		airWinPercent = ((float)airWins/(float)battles)*100;
		clearedPercent = ((float)cleareds/(float)battles)*100;
		indecisivePercent = ((float)indecisives/(float)battles)*100;
		lossPercent = ((float)losses/(float)battles)*100;
		
		controlPercentLabel.setText(formattedPercent(controlPercent));
		airWinPercentLabel.setText(formattedPercent(airWinPercent));
		clearedPercentLabel.setText(formattedPercent(clearedPercent));
		indecisivePercentLabel.setText(formattedPercent(indecisivePercent));
		lossPercentLabel.setText(formattedPercent(lossPercent));
		battlesLabel.setText(String.valueOf(battles));
		
		
		Vector atts = b.getAttackers();
		int attSize = atts.size();
		for(int i=0;i<attSize;i++)
		{
			UnitGroup curG = (UnitGroup)atts.elementAt(i);
			String name = curG.getUnit().getName();
			Integer totAtt = totRemAtt.get(name);
			totRemAtt.remove(name);
			int newTot = 0;
			newTot = totAtt.intValue()+curG.getNumUnits();
			totRemAtt.put(name, new Integer(newTot));
		}

		int size = unitGroupPanels.size();
		for(int j=0;j<size;j++)
		{
			Panel p = unitGroupPanels.elementAt(j);
			UnitGroupPanel ugp = (UnitGroupPanel) p.getComponent(0);
			int avgTot = 0;
			if(battles>0)
				avgTot = Math.round((float)totRemAtt.get(ugp.getAttUnitGroup().getUnit().getName()).intValue()/(float)battles);
			if(lossPercent<50)
				ugp.setRemAtt(avgTot);
			else
				ugp.setRemAtt(0);
		}
		
		
		Vector defs = b.getDefenders();
		int defSize = defs.size();
		for(int i=0;i<defSize;i++)
		{
			UnitGroup curG = (UnitGroup)defs.elementAt(i);
			String name = curG.getUnit().getName();
			Integer totDef = totRemDef.get(name);
			totRemDef.remove(name);
			int newTot = 0;
			newTot = totDef.intValue()+curG.getNumUnits();
			totRemDef.put(name, new Integer(newTot));
		}
		
		int defPSize = unitGroupPanels.size();
		for(int j=0;j<defPSize;j++)
		{
			Panel p = unitGroupPanels.elementAt(j);
			UnitGroupPanel ugp = (UnitGroupPanel) p.getComponent(0);
			int avgTot = 0;
			if(battles>0)
				avgTot = Math.round((float)totRemDef.get(ugp.getDefUnitGroup().getUnit().getName()).intValue()/(float)battles);
			if(lossPercent>50)
				ugp.setRemDef(avgTot);
			else
				ugp.setRemDef(0);
		}
		
		
	}

	public void start() {
		if(battleThread == null) {
			battleThread = new Thread(this, "Battle");
			battleThread.start();
		}
	}
	
	public void stop() {
		reset = true;
	}
	
	public Panel createUnitGroupPanel(OCUnit u) {
		Panel p = new Panel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.add(new UnitGroupPanel(u));
		return p;
	}
	
	public Panel createUnitsPanel(String gameType) {
		Panel unitsPanel = new Panel();
		unitsPanel.setLayout(new BoxLayout(unitsPanel, BoxLayout.Y_AXIS));
		Panel topPanel = new Panel();
		topPanel.setLayout(new GridLayout(1,4));
		//topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(new Label("Attackers"));
		Button clearButton = new Button("Clear");
		clearButton.setName("Clear");
		clearButton.addActionListener(this);
		topPanel.add(clearButton);
		topPanel.add(new Label());
		topPanel.add(new Label("Defense"));
		unitsPanel.add(topPanel);
		
		unitGroupPanels = new Vector<Panel>();
		totRemAtt = new Hashtable<String, Integer>();
		totRemDef = new Hashtable<String, Integer>();
		if(gameType.equals(RollerApplet.AA2nd))
		{
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newInf()));
			totRemAtt.put(StandardUnits.InfName, new Integer(0));
			totRemDef.put(StandardUnits.InfName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newArm()));
			totRemAtt.put(StandardUnits.ArmName, new Integer(0));
			totRemDef.put(StandardUnits.ArmName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newBomber()));
			totRemAtt.put(StandardUnits.BmbName, new Integer(0));
			totRemDef.put(StandardUnits.BmbName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newFighter()));
			totRemAtt.put(StandardUnits.FtrName, new Integer(0));
			totRemDef.put(StandardUnits.FtrName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newAC()));
			totRemAtt.put(StandardUnits.ACName, new Integer(0));
			totRemDef.put(StandardUnits.ACName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newSub()));
			totRemAtt.put(StandardUnits.SubName, new Integer(0));
			totRemDef.put(StandardUnits.SubName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newTrn()));
			totRemAtt.put(StandardUnits.TrnName, new Integer(0));
			totRemDef.put(StandardUnits.TrnName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newBB()));
			totRemAtt.put(StandardUnits.BBName, new Integer(0));
			totRemDef.put(StandardUnits.BBName, new Integer(0));
		}
		else if(gameType.equals(RollerApplet.AAR))
		{
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newInf()));
			totRemAtt.put(StandardUnits.InfName, new Integer(0));
			totRemDef.put(StandardUnits.InfName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newArm()));
			totRemAtt.put(StandardUnits.ArmName, new Integer(0));
			totRemDef.put(StandardUnits.ArmName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newRtl()));
			totRemAtt.put(StandardUnits.RtlName, new Integer(0));
			totRemDef.put(StandardUnits.RtlName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newBomber()));
			totRemAtt.put(StandardUnits.BmbName, new Integer(0));
			totRemDef.put(StandardUnits.BmbName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newFighter()));
			totRemAtt.put(StandardUnits.FtrName, new Integer(0));
			totRemDef.put(StandardUnits.FtrName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newAC()));
			totRemAtt.put(StandardUnits.ACName, new Integer(0));
			totRemDef.put(StandardUnits.ACName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newSub()));
			totRemAtt.put(StandardUnits.SubName, new Integer(0));
			totRemDef.put(StandardUnits.SubName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newTrn()));
			totRemAtt.put(StandardUnits.TrnName, new Integer(0));
			totRemDef.put(StandardUnits.TrnName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newBB()));
			totRemAtt.put(StandardUnits.BBName, new Integer(0));
			totRemDef.put(StandardUnits.BBName, new Integer(0));
			unitGroupPanels.addElement(createUnitGroupPanel(OCUnit.newDestroyer()));
			totRemAtt.put(StandardUnits.DesName, new Integer(0));
			totRemDef.put(StandardUnits.DesName, new Integer(0));
		}
		int size = unitGroupPanels.size();
		for(int i=0;i<size;i++)
		{
			unitsPanel.add(unitGroupPanels.elementAt(i));
		}
		
		return unitsPanel;
	}
	
	public Panel createOptionsPanel() {
		Panel optionsPanel = new Panel();
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
		//optionsPanel.setLayout(new GridLayout(4,1));
		
		Panel gamePanel = new Panel();
		gamePanel.setLayout(new GridLayout(2,1));
		//gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
		Label gameLabel = new Label("Game type");
		gamePanel.add(gameLabel);
		
		Choice gameChoice = new Choice();
		gameChoice.add(AA2nd);
		gameChoice.add(AAR);
		gameChoice.addItemListener(this);
		gameChoice.setName("gameChoice");
		gamePanel.add(gameChoice);

		Panel techPanel = new Panel();
		techPanel.setLayout(new GridLayout(6,1));
		techPanel.add(new Label("Techs"));
		Checkbox sSubs = new Checkbox(sSubsLabel);
		sSubs.addItemListener(this);
		Checkbox jetP = new Checkbox(jetPLabel);
		jetP.addItemListener(this);
		Checkbox hBmbs = new Checkbox(hBLabel);
		hBmbs.addItemListener(this);
		Checkbox comB = new Checkbox(cBLabel);
		comB.addItemListener(this);
		techPanel.add(sSubs);
		techPanel.add(hBmbs);
		techPanel.add(comB);
		techPanel.add(jetP);
		
		Panel battleInfoPanel = new Panel();
		battleInfoPanel.setLayout(new GridLayout(7,1));
				//new BoxLayout(battleInfoPanel, BoxLayout.Y_AXIS));
		battleInfoPanel.add(new Label("Battle options"));
		battleType = new Choice();
		battleType.add(RollerApplet.landBattle);
		battleType.add(RollerApplet.seaBattle);
		battleType.addItemListener(this);
		battleType.setName("battleType");
		battleInfoPanel.add(battleType);
		conL = new Checkbox("Conserve one land");
		conL.addItemListener(this);
		battleInfoPanel.add(conL);
		hasAA = new Checkbox("Anti-Air present");
		hasAA.addItemListener(this);
		battleInfoPanel.add(hasAA);
		oolCheckbox = new Checkbox("Use Custom OOL");
		oolCheckbox.addItemListener(this);
		battleInfoPanel.add(oolCheckbox);	
		rollRounds = new Checkbox("Roll rounds");
		rollRounds.addItemListener(this);
		battleInfoPanel.add(rollRounds);
		rounds = new TextField("1");
		rounds.addKeyListener(this);
		battleInfoPanel.add(rounds);
		
		Panel resultsPanel = new Panel();
		resultsPanel.setLayout(new GridLayout(6,2));
		Label controlLabel = new Label("Control");
		resultsPanel.add(controlLabel);
		controlPercentLabel = new Label(formattedPercent(controlPercent));
		resultsPanel.add(controlPercentLabel);
		Label airLabel = new Label("Air win");
		resultsPanel.add(airLabel);
		airWinPercentLabel = new Label(formattedPercent(airWinPercent));
		resultsPanel.add(airWinPercentLabel);
		Label clearedLabel = new Label("Cleared");
		resultsPanel.add(clearedLabel);
		clearedPercentLabel = new Label(formattedPercent(clearedPercent));
		resultsPanel.add(clearedPercentLabel);
		Label indecisiveLabel = new Label("Indecisive");
		resultsPanel.add(indecisiveLabel);
		indecisivePercentLabel = new Label(formattedPercent(indecisivePercent));
		resultsPanel.add(indecisivePercentLabel);
		Label lossLabel = new Label("Loss");
		resultsPanel.add(lossLabel);
		lossPercentLabel = new Label(formattedPercent(lossPercent));
		resultsPanel.add(lossPercentLabel);
		Label battlesTitleLabel = new Label("Battles rolled");
		resultsPanel.add(battlesTitleLabel);
		battlesLabel = new Label(String.valueOf(battles));
		resultsPanel.add(battlesLabel);
		
		
		optionsPanel.add(gamePanel);
		optionsPanel.add(techPanel);
		optionsPanel.add(battleInfoPanel);
		optionsPanel.add(resultsPanel);

		return optionsPanel;
	}

	public void itemStateChanged(ItemEvent e) {
		ItemSelectable item = e.getItemSelectable();
		selectables.add(item);
		setReset(true);
		try {Thread.sleep(100);} catch (InterruptedException ex) {}

	}
	
	private void setJetPower(boolean state) {
		int size = unitGroupPanels.size();
		int change = 1;
		if(state)
			change = 1;
		else
			change = -1;
		for(int i=0;i<size;i++)
		{
			Panel p = unitGroupPanels.elementAt(i);
			UnitGroupPanel ugp = (UnitGroupPanel)p.getComponent(0);
			UnitGroup ug = ugp.getAttUnitGroup();
			OCUnit cur = ug.getUnit();
			if(cur.getName().equals(StandardUnits.FtrName))
			{
				cur.setDefendValue(cur.getDefendValue()+change);
			}
		}
		
	}

	private void setCombinedBombardment(boolean state) {
		int size = unitGroupPanels.size();
		for(int i=0;i<size;i++)
		{
			Panel p = unitGroupPanels.elementAt(i);
			UnitGroupPanel ugp = (UnitGroupPanel)p.getComponent(0);
			UnitGroup ug = ugp.getAttUnitGroup();
			OCUnit cur = ug.getUnit();
			if(cur.getName().equals(StandardUnits.DesName))
			{
				cur.setSupportShot(state);
			}
		}
	}

	private void setHeavyBombers(boolean state) {
		int size = unitGroupPanels.size();
		for(int i=0;i<size;i++)
		{
			Panel p = unitGroupPanels.elementAt(i);
			UnitGroupPanel ugp = (UnitGroupPanel)p.getComponent(0);
			UnitGroup ug = ugp.getAttUnitGroup();
			OCUnit cur = ug.getUnit();
			if(cur.getName().equals(StandardUnits.BmbName))
			{
				if(state)
				{
					if(currentGameType.equals(AAR))
						cur.setMaxRolls(2);
					else
						cur.setMaxHits(3);
				}
				else
				{
					if(currentGameType.equals(AAR))
						cur.setMaxRolls(1);
					else
						cur.setMaxHits(1);					
				}
			}
		}
		
	}

	private void setSuperSubs(boolean state) {
		int size = unitGroupPanels.size();
		int change = 1;
		if(state)
			change = 1;
		else
			change = -1;
		for(int i=0;i<size;i++)
		{
			Panel p = unitGroupPanels.elementAt(i);
			UnitGroupPanel ugp = (UnitGroupPanel)p.getComponent(0);
			UnitGroup ug = ugp.getAttUnitGroup();
			OCUnit cur = ug.getUnit();
			if(cur.getName().equals(StandardUnits.SubName))
			{
				cur.setAttackValue(cur.getAttackValue()+change);
			}
		}
		
	}

	public void resetStats() {
		reset = true;
		battles = 0;
		controleds = 0;
		airWins = 0;
		cleareds = 0;
		indecisives = 0;
		losses = 0;
		controlPercent = 0.00f;
		airWinPercent = 0.00f;
		clearedPercent = 0.00f;
		indecisivePercent = 0.00f;
		lossPercent = 0.00f;
		reset = false;
		controlPercentLabel.setText(formattedPercent(controlPercent));
		airWinPercentLabel.setText(formattedPercent(airWinPercent));
		clearedPercentLabel.setText(formattedPercent(clearedPercent));
		indecisivePercentLabel.setText(formattedPercent(indecisivePercent));
		lossPercentLabel.setText(formattedPercent(lossPercent));
		battlesLabel.setText(String.valueOf(battles));
		
		Enumeration<String> atts = totRemAtt.keys();
		while(atts.hasMoreElements())
		{
			String name = atts.nextElement();
			totRemAtt.remove(name);
			totRemAtt.put(name, new Integer(0));
		}
		
		Enumeration<String> defs = totRemDef.keys();
		while(defs.hasMoreElements())
		{
			String name = defs.nextElement();
			totRemDef.remove(name);
			totRemDef.put(name, new Integer(0));
		}
		
	}
	
	public void resetBattle() {
		reset = true;
		Vector<UnitGroup> attackers = null;
		Vector<UnitGroup> defenders = null;
		attackers = processAttUnitGroups();
		defenders = processDefUnitGroups();
		int roundCount = 0;
		String roundsText = rounds.getText();
		try {
			roundCount = Integer.parseInt(roundsText);
		} catch (NumberFormatException nfe)
		{
			roundCount = 0;
		}
		if(!rollRounds.getState())
			roundCount = 0;
		boolean landB = true;
		if(battleType.getSelectedItem().equals(RollerApplet.landBattle))
			landB = true;
		else
			landB = false;
		boolean rollAASep = false;
		if(currentGameType.equals(RollerApplet.AAR))
			rollAASep = true;
		resetOOL();
		b = new OCBattle(attackers, defenders, roundCount, conL.getState(), hasAA.getState(), landB, rollAASep, false, ool);
		reset = false;
	}
	
	private void resetOOL() {
		ool = null;
	}

	private Vector<UnitGroup> processDefUnitGroups() {
		Vector<UnitGroup> defenders = new Vector<UnitGroup>();
		int size = unitGroupPanels.size();
		for(int i=0;i<size;i++)
		{
			Panel p = unitGroupPanels.elementAt(i);
			UnitGroupPanel ugp = (UnitGroupPanel)p.getComponent(0);
			UnitGroup ug = ugp.getDefUnitGroup();
			defenders.addElement((UnitGroup) ug.clone());
		}
		return defenders;
	}

	private Vector<UnitGroup> processAttUnitGroups() {
		Vector<UnitGroup> attackers = new Vector<UnitGroup>();
		int size = unitGroupPanels.size();
		for(int i=0;i<size;i++)
		{
			Panel p = unitGroupPanels.elementAt(i);
			UnitGroupPanel ugp = (UnitGroupPanel)p.getComponent(0);
			UnitGroup ug = ugp.getAttUnitGroup();
			attackers.addElement((UnitGroup) ug.clone());
		}
		return attackers;
	}

	public String formattedPercent(float per) {
		BigDecimal bd = new BigDecimal(per);
		String res = bd.toString();
		int endSpace = 0;
		if(res.indexOf(".")+3>=res.length()||res.indexOf(".") == -1)
		{
			endSpace = res.length();
		}
		else
			endSpace = res.indexOf(".")+3;
		res = res.substring(0, endSpace);
		if(res.indexOf(".")==-1)
			res += ".00";
		res+="%";
		return res;
	}

	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if(o instanceof Button)
		{
			if(((Button)o).getName().equals("Clear"))
			{
				clearButton = true;
				setReset(true);
				try {Thread.sleep(100);} catch (InterruptedException ex) {}
			}
		}
		
	}

	public boolean isReset() {
		return reset;
	}

	public void setReset(boolean reset) {
		this.reset = reset;
	}

	public void processUGPButton(UnitGroupPanel panel, Button button) {
		buttonsToProc.add(button);
		ugpsToProc.add(panel);
		
	}

	


}
