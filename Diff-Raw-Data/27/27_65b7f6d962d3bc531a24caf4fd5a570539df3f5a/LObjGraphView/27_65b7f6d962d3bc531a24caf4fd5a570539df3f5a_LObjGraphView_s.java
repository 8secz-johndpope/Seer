 package org.concord.CCProbe;
 
 import graph.*;
 import waba.ui.*;
 import waba.util.*;
 import waba.fx.*;
 import waba.sys.*;
 import org.concord.waba.extra.event.*;
 import org.concord.waba.extra.ui.*;
 import org.concord.waba.extra.util.*;
 import org.concord.waba.extra.probware.*;
 import org.concord.waba.extra.probware.probs.*;
 import extra.util.*;
 import org.concord.LabBook.*;
 
 class TimeBin implements DecoratedValue
 {
     float time = 0f;
 	CCUnit unit = CCUnit.getUnit(CCUnit.UNIT_CODE_S);
 
     public String getLabel(){return "Time";}
     public float getValue(){return time;}
     public void setValue(float t){time = t;}
     public Color getColor(){return null;}
     public float getTime(){return 0f;}
 	public CCUnit getUnit(){return unit;}
 }
 
 public class LObjGraphView extends LabObjectView
     implements ActionListener
 {
     LObjGraph graph;
     AnnotView av = null;
 
 	Choice viewChoice = null;
 	LineGraphMode lgm = null;
 	Button addMark = null;
 	Choice toolsChoice = null;
 	Button notes = null;
 	Button clear = null;
 
     Button doneButton = null;
     Label titleLabel = null;
 
     Menu menu = new Menu("Graph");
 	int curViewIndex = 1;
 
     TimeBin timeBin = new TimeBin();
 
     DigitalDisplay dd;
 	DecoratedValue curAnnot = null;
 
 	int dd_height = 20;
 
 	LObjDictionary dataDict;
 	boolean instant = false;
 
 	String [] fileStrings = {"Save Data..", "Export Data.."};
 	String [] palmFileStrings = {"Save Data.."};
 
     public LObjGraphView(ViewContainer vc, LObjGraph g, LObjDictionary curDict)
     {
 		super(vc);
 
 		dataDict = curDict;
 
 		menu.add("Properties..");
 		menu.addActionListener(this);
 
 		graph = g;
 		lObj = g;	
 
     }
 
 	public void addMenus()
 	{
 		if(container == null) return;
 
 		container.getMainView().addMenu(this, menu);
 		if(waba.sys.Vm.getPlatform().equals("PalmOS")){
 			fileStrings = palmFileStrings;
 		}
 		container.getMainView().addFileMenuItems(fileStrings, this);
 	}
 
 	public void delMenus()
 	{
 		if(container != null){
 			container.getMainView().delMenu(this,menu);
 			container.getMainView().removeFileMenuItems(fileStrings, this);
 		}
 	}
 
 	public void doInstantCollection()
 	{
 		instant = true;
 	}		
 
 	public void updateProp()
 	{
 		if(graph.curGS == null) return;
 		if(graph.curDS != null){
 			graph.curGS.setYUnit(graph.curDS.getUnit());
 			if(graph.autoTitle){
 				graph.curGS.setYLabel(graph.curDS.getLabel());
 				graph.title = graph.curDS.getSummary();
 			}
 		}
 		av.update();
 		postEvent(new ControlEvent(1001, this));
 	}
 
     public void actionPerformed(ActionEvent e)
     {
 		String command;
 		Debug.println("Got action: " + e.getActionCommand());
 
 		if(e.getSource() == menu){
 			if(e.getActionCommand().equals("Properties..")){
 				graph.showAxisProp();
 			}
 		} else {
 			if(e.getActionCommand().equals("Save Data..")){
 				LObjDataSet dSet = DataObjFactory.createDataSet();
 
 				LObjGraph dsGraph = (LObjGraph)graph.copy();
 				dsGraph.name = "Graph";
 
 				dSet.setDataViewer(dsGraph);
 				graph.curGS.saveData(dSet);
 
 				if(dataDict != null){
 					dataDict.add(dSet);
 					dSet.store();
 				} else {
 					// for now it is an error
 					// latter it should ask the user for the name
 				}
 			} else if(e.getActionCommand().equals("Export Data..")){
 				Bin curBin = graph.curGS.getBin();
 				if(curBin != null){
 					curBin.description = graph.title;
 					DataExport.export(curBin, av.lGraph.annots);
 				}
 			}
 		}
 	}
 
     boolean sTitle;
 
     public void showTitle(boolean doIt)
     {
 		sTitle = doIt;
     }
 
 	public void setTitle(String title)
 	{
 		if(graph != null){
 			graph.title = title;
 		}
 
 		if(titleLabel != null){
 			titleLabel.setText(title);
 		}
 	}
 
     public void layout(boolean sDone)
     {
 		if(didLayout) return;
 		didLayout = true;
 
 		showDone = sDone;
 
 		if(sTitle){
 			titleLabel = new Label(graph.name, Label.CENTER);
 			add(titleLabel);
 		}
 
 		if(showDone){
 			doneButton = new Button("Done");
 			add(doneButton);
 		} 
 
 		lgm = new LineGraphMode();
 		add(lgm);
 
 		String [] viewChoices = {"Bar Graph", "Line Graph"};
 		viewChoice = new Choice(viewChoices);
 		viewChoice.setName("View");
 		viewChoice.setSelectedIndex(1);
 		//		viewChoice.setRect(0, butStart, 40, 13);
 		add(viewChoice);
 
 		addMark = new Button("Mark");
 
 		String [] toolsChoices = {"Delete Mark", "Toggle Scrolling", "Auto Resize", "Zoom"};
 		toolsChoice = new Choice(toolsChoices);
 		toolsChoice.setName("Tools");
 		add(toolsChoice);
 		
 		notes = new Button("Notes");
 		add(notes);
 
 		clear = new Button("Clear");
 		add(clear);
     }
 
 
 	SplitAxis xaxis = null;
 	ColorAxis yaxis = null;
 	
     public void setRect(int x, int y, int width, int height)
     {
 		super.setRect(x,y,width,height);
 		if(!didLayout) layout(false);
 
 		int curY = 0;
 		int gHeight = height;
 		if(gHeight <= 160){
 			dd_height = 10;
 		}
 
 		if(sTitle){
 			titleLabel.setRect(x, curY, width, 16);
 			curY += 16;
 			gHeight -= 16;
 		} 
 
 		// Trying to get 10pt at dd_height = 10 
 		//  and 16pt at dd_height = 20
 		dd = new DigitalDisplay(new Font("Helvetica", 
 										 Font.BOLD, 3*dd_height/5 + 4));		
 		gHeight -= dd_height;
 		if(showDone) dd.setRect(0,curY, width - 30, dd_height);
 		else dd.setRect(0,curY, width, dd_height);
 		dd.addBin(timeBin);
 		add(dd);
 		curY += dd_height;
 
 		if(showDone){
 			doneButton.setRect(width-30,0,30,15);
 			gHeight -= 16;
 		}
 
 		if(width <= 160){
 			gHeight -= 12;
 			viewChoice.setRect(0, height-12, 33, 12);
 			lgm.setRect(32, height -12, 30, 12);
 			addMark.setRect(32, height-12, 30, 12);
 			toolsChoice.setRect(63, height-12, 35, 12);
 			notes.setRect(100, height-12, 30, 12); 
 			clear.setRect(130, height-12, 30, 12);
 		} else {
 			gHeight -= 16;
 			viewChoice.setRect(0, height-16, 50, 16);
 			lgm.setRect(50, height -16, 42, 16);
 			addMark.setRect(50, height-16, 42, 16);
 			toolsChoice.setRect(100, height-16, 55, 16);
 			notes.setRect(160, height-16, 30, 16);
 			clear.setRect(200, height-16, 30, 16);
 		}
 
 
 		if(av != null){ remove(av); }
 	
 		if(graph.graphSettings == null){
 			return;
 		}
 		
 		// This is just a hack
 		xaxis = new SplitAxis(Axis.BOTTOM);
 		yaxis = new ColorAxis(Axis.LEFT);
 		yaxis.setMaxDigits(6);
 
 		// This is just a hack
 		if(gHeight < 1) gHeight = 1;
 			
 		av = new AnnotView(width, gHeight, xaxis, yaxis);
 		av.setPos(0,curY);
 		Bin curBin = new Bin(xaxis.lastAxis, yaxis);
 		av.addBin(curBin);
 
 		GraphSettings gs = (GraphSettings)graph.graphSettings.get(0);
 		gs.init(this, (Object)av, curBin, xaxis, yaxis);
 
 		for(int i=1; i<graph.graphSettings.getCount(); i++){
 			xaxis = new SplitAxis(Axis.BOTTOM);
 			yaxis = new ColorAxis(Axis.LEFT);
 			yaxis.setMaxDigits(6);
 
 			av.setAxis(xaxis, yaxis);
 
 			curBin = new Bin(xaxis.lastAxis, yaxis);
 			av.addBin(curBin);
 
 			gs = (GraphSettings)graph.graphSettings.get(i);
 			gs.init(this, (Object)av, curBin, xaxis, yaxis);
 		}
 
 		add(av);
 
 		if(instant){
 			graph.startDataDelivery();
 			instant = false;
 		}
     }
 
     public void close()
     {
 		Debug.println("Got close in graph");
 
 		av.free();
 		if(yaxis != null)yaxis.free();
 		if(xaxis != null)xaxis.free();
 		graph.removeGV();
 		graph.store();
 		super.close();
     }
 
 	int numStarted = 0;
 	public void startGraph(Object cookie, Bin curBin)
 	{
 		dd.addBin(curBin);
 		numStarted++;
 	}
 
 	public void update(Object cookie, float time){
 		av.update();			
 		timeBin.setValue(time);
 		dd.update();
 	}
 
 	public void stopGraph()
 	{
 		if(graph.graphSettings == null){
 			return;
 		}
 
 		for(int i=0; i<graph.graphSettings.getCount(); i++){
 			GraphSettings gs = (GraphSettings)graph.graphSettings.get(i);
 			gs.stopGraph();
 		}
 	}
 
 	// Right this requires the caller to call repaint()
 	public Bin stopGraph(Object cookie, Bin curBin, 
 						 boolean newBin, SplitAxis xaxis)
 	{
 		numStarted--;
 
 		dd.removeBin(curBin);
 
 		if(numStarted == 0 && (curBin.maxX < 0 || curBin.getNumVals() < 3)){
 			curBin.reset();
 			curBin.xaxis = xaxis.lastAxis;
 			curBin.label = "Probe";
 		} else {
 			av.removeBin(curBin);
 
 			if(newBin){
 				xaxis.addAxis(curBin.maxX);
 				curBin = new Bin(xaxis.lastAxis, curBin.yaxis);
 				curBin.label = "Probe";
 				av.addBin(curBin);
 			} else {
 				curBin = null;
 			}
 		}
 		av.update();
 
		if(numStarted == 0) postEvent(new ControlEvent(1000, this));	
 		return curBin;
 	}
 
     public void onEvent(Event e)
     {
 		if(e.target == doneButton &&
 		   e.type == ControlEvent.PRESSED){
 			if(container != null){
 				container.done(this);
 			}	    
 		} else if(e.target == lgm &&
 				  e.type == ControlEvent.PRESSED){
 			switch(lgm.getSelectedIndex()){
 			case 0:
 				av.setViewMode('D');
 				break;
 			case 1:
 				av.setViewMode('Z');
 				break;
 			case 2:
 				av.setViewMode('A');
 				break;
 			}
 		} else if(e.target == addMark &&
 				  e.type == ControlEvent.PRESSED){
 			av.addAnnot();
 		} else if(e.type == 1003){
 			//	    System.out.println("Got 1003");
 			if(av.lgView.selAnnot != null){
 				timeBin.setValue(av.lgView.selAnnot.time);
 				// need to make sure lg.lgView.selAnnot has been added to dd
 				if(curAnnot == null){
 					// might need to remove the curBin to save space
 					curAnnot = av.lgView.selAnnot;
 					dd.addBin(curAnnot);
 				} else if(curAnnot != av.lgView.selAnnot){
 					dd.removeBin(curAnnot);
 					curAnnot = av.lgView.selAnnot;
 					dd.addBin(curAnnot);
 				}
 
 				dd.update();
 
 				// curVal.setText(convertor.fToString(lg.lgView.selAnnot.value) + units);
 				// curTime.setText(convertor.fToString() + "s");
 			} else {
 				// hack
 				// Need to remove annot bin from dd
 				// set value of timeBin to last time
 				if(curAnnot != null){
 					dd.removeBin(curAnnot);
 					curAnnot = null;
 				}
 				dd.update();
 		
 				// curVal.setText("");
 				// curTime.setText("");
 			}		
 		} else if(e.type == 1004){
 			graph.showAxisProp(2);
 		} else if(e.type == 1005){
 			graph.showAxisProp(1);
 		} else if(e.type == 1006){
 			graph.curGS.updateGS();
 		} else if(e.target == viewChoice){
 			if(e.type == ControlEvent.PRESSED){
 				int index = viewChoice.getSelectedIndex();
 				av.setViewType(index);
 				if(curViewIndex != index){
 					curViewIndex = index;
 					switch(index){
 					case 0:
 						remove(lgm);
 						add(addMark);
 						break;
 					case 1:
 						remove(addMark);
 						add(lgm);
 						break;
 					}
 				}
 			}
 		} else if(e.target == toolsChoice){
 			if(e.type == ControlEvent.PRESSED){
 				int index = toolsChoice.getSelectedIndex();
 				switch(index){
 				case 0:
 				case 1:
 					break;
 				case 2:
 					GraphSettings gs = graph.curGS;
 					if(gs.calcVisibleRange()){
 						float margin = (gs.maxVisY - gs.minVisY)*0.1f;
 						int count=0;
 						float ymin, ymax;
 						while(margin == 0f && count < 4){
 							ymin = gs.minVisY - (-1f)/gs.yaxis.scale;
 							ymax = gs.maxVisY + (-1f)/gs.yaxis.scale;
 							gs.setYValues(ymin, ymax);
 							av.update();
 
 							if(!gs.calcVisibleRange()) return;
 							margin = (gs.maxVisY - gs.minVisY)*0.1f;
 							count++;
 						}
 						if(margin < 1.0E-8f) margin = 1.0E-8f; 
 						ymin = gs.minVisY - margin;
 						ymax = gs.maxVisY + margin;
 						gs.setYValues(ymin, ymax);
 						av.update();
 					}
 					break;
 				case 3:
 					av.lgView.zoomSelect();
 					break;
 				}
 			}
 		}else if(e.target == clear &&
 				 e.type == ControlEvent.PRESSED){
 			clear();
 		}
 
     }
 
     public void clear()
     {
 		// ask for confirmation????
 		postEvent(new ControlEvent(1000, this));
 		av.reset();
 	
 		// Clear curBin and set time to 0
 		timeBin.setValue(0f);
 
 		if(graph.graphSettings == null){
 			return;
 		}
 
 		for(int i=0; i<graph.graphSettings.getCount(); i++){
 			GraphSettings gs = (GraphSettings)graph.graphSettings.get(i);
 			gs.clear();
 		}
 		numStarted = 0;
 
 		dd.update();
     }
 
 	public void clear(Object cookie, Bin curBin)
 	{
 		dd.removeBin(curBin);		
 	}
 }
