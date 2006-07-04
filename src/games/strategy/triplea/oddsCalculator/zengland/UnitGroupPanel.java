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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UnitGroupPanel extends Panel implements ActionListener {

	private static final long serialVersionUID = 7544343415837468319L;

	private UnitGroup attUnitGroup;
	private UnitGroup defUnitGroup;
	
	Button att10 = new Button("10");
	Button att5 = new Button("5");
	Button att1 = new Button("1");
	Button att0 = new Button("0");
	Button attNeg1 = new Button("-1");
	Button def10 = new Button("10");
	Button def5 = new Button("5");
	Button def1 = new Button("1");
	Button def0 = new Button("0");
	Button defNeg1 = new Button("-1");
	TextField totalAttackers = null;
	TextField totalDefenders = null;
	TextField attOol = null;
	TextField defOol = null;
	
	private Label remAtt;

	private Label remDef;
		
	public UnitGroup getAttUnitGroup() {
		return attUnitGroup;
	}

	public void setAttUnitGroup(UnitGroup attUnitGroup) {
		this.attUnitGroup = attUnitGroup;
	}

	public UnitGroup getDefUnitGroup() {
		return defUnitGroup;
	}

	public void setDefUnitGroup(UnitGroup defUnitGroup) {
		this.defUnitGroup = defUnitGroup;
	}

	public UnitGroupPanel(OCUnit unit) {
		super();
		setLayout(new GridLayout(1, 16));
		setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		setAttUnitGroup(new UnitGroup(unit, 0));
		setDefUnitGroup(new UnitGroup(unit, 0));
		att10.setName("att10");
		att10.addActionListener(this);
		att5.setName("att5");
		att5.addActionListener(this);
		att1.setName("att1");
		att1.addActionListener(this);
		att0.setName("att0");
		att0.addActionListener(this);
		attNeg1.setName("attNeg1");
		attNeg1.addActionListener(this);
		def10.setName("def10");
		def10.addActionListener(this);
		def5.setName("def5");
		def5.addActionListener(this);
		def1.setName("def1");
		def1.addActionListener(this);
		def0.setName("def0");
		def0.addActionListener(this);
		defNeg1.setName("defNeg1");
		defNeg1.addActionListener(this);
		
		attOol = new TextField(2);
		attOol.addActionListener(this);
		add(attOol);
		add(att10);
		add(att5);
		add(att1);
		add(att0);
		add(attNeg1);
		totalAttackers = new TextField(String.valueOf(attUnitGroup.getNumUnits()), 3);
		totalAttackers.addActionListener(this);
		add(totalAttackers);
		remAtt = new Label("0");
		add(remAtt);
		add(new Label(unit.getName(), Label.LEFT));
		defOol = new TextField(2);
		defOol.addActionListener(this);
		add(defOol);
		add(def10);
		add(def5);
		add(def1);
		add(def0);
		add(defNeg1);
		totalDefenders = new TextField(String.valueOf(attUnitGroup.getNumUnits()), 3);
		totalDefenders.addActionListener(this);
		add(totalDefenders);
		remDef = new Label("0");
		add(remDef);
	}

	public void actionPerformed(ActionEvent e) {
//		((RollerApplet)this.getParent().getParent().getParent()).resetStats();
//		((RollerApplet)this.getParent().getParent().getParent()).resetBattle();
		Object o = e.getSource();
		if(o instanceof Button)
		{
			((RollerApplet)this.getParent().getParent().getParent()).processUGPButton(this, (Button)o);
			((RollerApplet)this.getParent().getParent().getParent()).setReset(true);
			try {Thread.sleep(100);} catch (InterruptedException ex) {}
		}
//		if(o instanceof Button)
//		{
//			Button b = (Button) o;
//			String bName = b.getName();
//			int bValue = Integer.valueOf(b.getLabel()).intValue();
//			
//			if(bName.startsWith("att"))
//			{
//				if(bValue==0)
//				{
//					attUnitGroup.setNumUnits(bValue);
//					setRemAtt(bValue);
//				}
//				else
//				{
//					attUnitGroup.setNumUnits(attUnitGroup.getNumUnits()+bValue);
//				}
//				totalAttackers.setText(String.valueOf(attUnitGroup.getNumUnits()));
//			}
//			else if(bName.startsWith("def"))
//			{
//				if(bValue==0)
//				{
//					defUnitGroup.setNumUnits(bValue);
//					setRemDef(bValue);
//				}
//				else
//				{
//					defUnitGroup.setNumUnits(defUnitGroup.getNumUnits()+bValue);
//				}
//				totalDefenders.setText(String.valueOf(defUnitGroup.getNumUnits()));
//			}
//		}

	}

	public Label getRemAtt() {
		return remAtt;
	}

	public void setRemAtt(Label remAtt) {
		this.remAtt = remAtt;
	}
	
	public void setRemAtt(int remVal) {
		remAtt.setText(String.valueOf(remVal));
	}

	public Label getRemDef() {
		return remDef;
	}

	public void setRemDef(Label remDef) {
		this.remDef = remDef;
	}
	
	public void setRemDef(int remVal) {
		remDef.setText(String.valueOf(remVal));
	}
}
