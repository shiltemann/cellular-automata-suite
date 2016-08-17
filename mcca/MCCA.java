/**
 *  Multi Coloured Cellular Automata
 *    @author Saskia Hiltemann
 */

package mcca;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class MCCA extends JPanel implements Runnable{
	private static final long serialVersionUID = 1L;
	
	public CACanvas myCA;
	private volatile Thread runner = null;	
	private int widthCA,heightCA,numCols,radius,threshold;
	private long maxInt = 2147483647;
	private boolean twoDimensional;
	private boolean cyclic;
	private boolean totalistic;
	private boolean life;
	private boolean testing;
	private boolean RD;
	private JComboBox neighbourhood;
	private JComboBox colourScheme;
	private JComboBox lifeFormChoice;
	private JComboBox pictureChoice;
	private JComboBox patternChoice;
	private JSlider radiusScrollbar;
	private JSlider thresholdScrollbar;
	private JSlider numColsScrollbar;
	private JButton restartButton;
	private JButton randomButton;
	private JButton playButton;
	private JButton stopButton;
	private JButton stepButton;
	private JButton reverseButton;
	private JButton snapshotButton;
    private JButton maxColourButton;
    private JButton minColourButton;
    private JButton defaultColoursButton;
    private JButton freezeButton;
    private JButton freezeSetButton;
	private JTextField D1field;
	private JTextField D2field;
	private JTextField sfield;
	private JTextField updateInterval;
    private JRadioButton showAButton;
    private JRadioButton showBButton;
	private JPanel controls;
	private JPanel subpanel1;
	private JPanel subpanel2;
	private JPanel subpanel3;
	private JPanel subpanel4;
	private JPanel subpanel5;
	private JPanel subpanel6;
	private JPanel subpanel7;
	private JPanel subpanel8;
	private JPanel subpanel9;
	private JPanel subpanel10;
	private JLabel nbhLabel;
	private JLabel radiusLabel;
	private JLabel thresholdLabel;
	private JLabel colourSchemeLabel;
	private JLabel numColsLabel;
	private JLabel ruleLabel;
	private JLabel pictureLabel;
	private JSpinner ruleSpinner;
	private SpinnerModel ruleModel;
	private JCheckBox totalisticCheckbox;
	private JCheckBox visibleCheckbox;
	private JCheckBox singlePointCheckbox;
	private boolean visible;
	private boolean reversePossible = false;
	private volatile boolean auto;
	public static MCCA myclone;
	public static CACanvas myclone2;
	public static JFrame f2;
	public static JPanel activepanel;
	public static JTabbedPane tabs;
	public int number;
	
	
	public static void main(String[] args) {
		JFrame f = new JFrame();
		
				
		f.addWindowListener(new java.awt.event.WindowAdapter() {
		public void windowClosing(java.awt.event.WindowEvent e) {
		   System.exit(0);
		 };
		});
		
		tabs = new JTabbedPane();				
		
		JPanel panel1D = new JPanel();
		JPanel panel2D = new JPanel();
		JPanel panel1D_standard =  new JPanel();
		JPanel panel2D_standard = new JPanel();
		JPanel panel_Life = new JPanel();
        JPanel panel_RD = new JPanel();
		
		
		JScrollPane scrollpane = new JScrollPane(panel2D);
		JScrollPane scrollpane2 = new JScrollPane(panel1D);
		JScrollPane scrollpane3 = new JScrollPane(panel1D_standard);
		JScrollPane scrollpane4 = new JScrollPane(panel2D_standard);
		JScrollPane scrollpane5 = new JScrollPane(panel_Life);
		JScrollPane scrollpane6 = new JScrollPane(panel_RD);
		 			
		
		MCCA myMCCA = new MCCA();
		MCCA myMCCA_1D = new MCCA();
		MCCA myMCCA_standard = new MCCA();
		MCCA myMCCA_standard2D = new MCCA();
		MCCA myMCCA_life = new MCCA();
		MCCA myMCCA_RD = new MCCA();

		
		panel2D.add(myMCCA); 
	    tabs.addTab("2D Cyclic",scrollpane);
		
		panel1D.add(myMCCA_1D);
		tabs.addTab("1D Cyclic",scrollpane2);
		
		panel2D_standard.add(myMCCA_standard2D);
		tabs.addTab("2D Parity",scrollpane4);
		
		panel1D_standard.add(myMCCA_standard);
		tabs.addTab("1D Standard",scrollpane3);
		
		panel_Life.add(myMCCA_life);
		tabs.addTab("Game of Life",scrollpane5);
		
		panel_RD.add(myMCCA_RD);
		tabs.addTab("Reaction Diffusion",scrollpane6);
		
		
		myMCCA_1D.init(1,true,4,1,false,false);	
		myMCCA.init(2,true,16,1,false,false);
		myMCCA_standard.init(1,false,2,2,false,false);
		myMCCA_standard2D.init(2,false,2,1,false,false);
		myMCCA_life.init(2,false,2,2,true,false);
        myMCCA_RD.init(2, false, 2, 0, false, true);
		
		f.add(tabs);
		f.pack();		
			
		myMCCA_1D.start();
		myMCCA.start();
		myMCCA_standard.start();
		myMCCA_standard2D.start();
		myMCCA_life.start();
		myMCCA_RD.start();
		
		f.setVisible(true);
		f.setTitle("Cellular Automata Collection");					
		
	}	
	
	
    public void init(int dimension, boolean c, int col, int t, boolean l, boolean RD_){
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

      widthCA = (int)(0.98*dim.width);  //widthCA/=2; //widthCA=192;
      heightCA = (int)(0.9*dim.height); //heightCA/=2; //heightCA=192;
      numCols = col;
      radius = 1;
      threshold = t;
      
      auto = false;      
      cyclic = c; 
      totalistic=false;
      life = l;
      visible = true;
      testing = false;
      RD = RD_;
      
           
      if(dimension == 2) twoDimensional = true;
      else twoDimensional = false;
      
      setLayout(new BorderLayout());
      
      myCA = new CACanvas();     
      myCA.makeSize(widthCA, heightCA, RD);

      
      controls = new JPanel();     
      controls.setLayout(new GridLayout(7, 1));
            
  	  
      
      // buttons
      subpanel1 = new JPanel(); 
      restartButton = new JButton("Restart");
      randomButton = new JButton("Random");
      playButton = new JButton("Play");
      stepButton = new JButton("Single Step");
      stopButton = new JButton("Stop");
      reverseButton = new JButton("Backward Step");
      snapshotButton = new JButton("snapshot");
      subpanel1.add(stopButton);
      subpanel1.add(playButton);
      subpanel1.add(stepButton);
      subpanel1.add(reverseButton);
      subpanel1.add(restartButton);
      subpanel1.add(randomButton);
      if(cyclic)subpanel1.add(snapshotButton);
      controls.add(subpanel1);
      if(auto)playButton.setEnabled(false);
      else stopButton.setEnabled(false);
      
      
          
      // colour scheme
      subpanel5 = new JPanel();      
      colourScheme = new JComboBox();
      colourScheme.addItem("Rainbow"); 
      colourScheme.addItem("Ocean");           
      colourScheme.addItem("Gray");
      if(!(cyclic&&twoDimensional))
    	  colourScheme.setSelectedItem("Ocean");
      colourSchemeLabel = new JLabel("Colour Scheme:");
      subpanel5.add(colourSchemeLabel);
      subpanel5.add(colourScheme);
      if(!RD)controls.add(subpanel5);
   
      // number of colours        
      subpanel6 = new JPanel();
      numColsScrollbar = new JSlider(1,16,16);
      JLabel cLabel = new JLabel("             Colours: ");
      numColsLabel = new JLabel(numCols+"  ");
      subpanel6.add(cLabel);
      subpanel6.add(numColsScrollbar);
      subpanel6.add(numColsLabel);
      if(!life&&!RD) controls.add(subpanel6);
            
      // radius      
      subpanel2 = new JPanel();
      radiusScrollbar = new JSlider(1, 10, 1);
      JLabel label = new JLabel("                 Radius: ");
      subpanel2.add(label);
      subpanel2.add(radiusScrollbar);
      radiusLabel = new JLabel("1    ");
      subpanel2.add(radiusLabel);
      if(!life&&!RD) controls.add(subpanel2);
      
      
      // threshold 
	  JLabel label2 = new JLabel("          Threshold: ");
	  thresholdScrollbar = new JSlider(1, neighbourhoodSize(1), 1);
	  subpanel3 = new JPanel();
	  subpanel3.add(label2);
	  subpanel3.add(thresholdScrollbar);
	  thresholdLabel = new JLabel(threshold+"");
	  subpanel3.add(thresholdLabel);
	  if(cyclic&&!RD) controls.add(subpanel3);  	      

	      
	  // neighbourhood
	  neighbourhood = new JComboBox();
	  neighbourhood.addItem("Von Neumann"); 
	  neighbourhood.addItem("Moore");
	  neighbourhood.addItem("Plus");
	  neighbourhood.addItem("Cross");
	  if(!(cyclic&&twoDimensional))
		 neighbourhood.setSelectedItem("Moore");
	  nbhLabel = new JLabel("Neighbourhood type: ");
	  subpanel4 = new JPanel();
	  subpanel4.add(nbhLabel);
	  subpanel4.add(neighbourhood);
	  if(twoDimensional&&!life&&!RD)controls.add(subpanel4);
	      
	  
	  // pictures 
	  pictureChoice = new JComboBox();
	  pictureChoice.addItem("Dot");
	  pictureChoice.addItem("Hi");
	  pictureChoice.addItem("Stickman");
	  pictureChoice.addItem("Clover");
	  pictureChoice.addItem("beastie");
	  pictureLabel = new JLabel("    Initial Picture: ");	     
	  subpanel7= new JPanel();
	  subpanel7.add(pictureLabel);
	  subpanel7.add(pictureChoice);
	  if(twoDimensional&&!cyclic&&!life&&!RD) controls.add(subpanel7);
	      
	  
	  // testing
	  visibleCheckbox = new JCheckBox("visible",true);
	  if(testing && twoDimensional)subpanel4.add(visibleCheckbox);

	  
	  // single point start in 1D standard case?
	  singlePointCheckbox = new JCheckBox("single point",true);
	  if(!cyclic && !twoDimensional)subpanel4.add(singlePointCheckbox);
	  
	  // life forms
      subpanel9 = new JPanel();
  	  JLabel presetsLabel = new JLabel("Life Form:");
  	  lifeFormChoice = new JComboBox();
  	  lifeFormChoice.addItem("Clear grid");
  	  lifeFormChoice.addItem("Glider");
  	  lifeFormChoice.addItem("Small Exploder");
  	  lifeFormChoice.addItem("Line of Ten");
  	  subpanel9.add(presetsLabel);
	  subpanel9.add(lifeFormChoice);
	  if(life) controls.add(subpanel9);
  	  
  	  
      // rule numbers 
       subpanel8 = new JPanel();	  
       ruleModel = new SpinnerNumberModel(30,0,999999,1); 
       ruleSpinner = new JSpinner(ruleModel);
       ruleLabel = new JLabel("                   Rule: ");
       Font f = new Font(null, Font.BOLD, 12);
       ruleSpinner.setFont(f);	 
       subpanel8.add(ruleLabel);
       subpanel8.add(ruleSpinner);
       //if(!twoDimensional && !cyclic) controls.add(subpanel8); /**/
      
       ChangeListener myListener = new ChangeListener() {
    	 public void stateChanged(ChangeEvent e) {
    	   Object o = ruleModel.getValue();
    	   String temp = o.toString();
    	   int i = Integer.parseInt(temp);
    	   myCA.rule= i;
    	   myCA.setRule();
    	 }
       };
     

       ruleSpinner.addChangeListener(myListener);       
       JSpinner.NumberEditor editor = new JSpinner.NumberEditor(ruleSpinner);
       ruleSpinner.setEditor(editor);    
       ruleLabel = new JLabel(" (0-255)              ");
       subpanel8.add(ruleLabel); 
       
       subpanel4 = new JPanel();
       totalisticCheckbox = new JCheckBox("  totalistic",false);
       subpanel8.add(totalisticCheckbox);
       subpanel8.add(singlePointCheckbox);
       if(!twoDimensional && !cyclic) controls.add(subpanel8); /**/
       
       
       //RD buttons
       
       
       // diffusion constants
       subpanel10 = new JPanel();
       D1field = new JTextField();
       D2field = new JTextField();
       sfield = new JTextField();
       D1field.setColumns(6);
       D2field.setColumns(6);
       sfield.setColumns(6);
       D1field.setText("16.0");
       D2field.setText("3.5");
       sfield.setText("1.0");
       JLabel DaLabel = new JLabel("  Da:  ");
       JLabel DbLabel = new JLabel("  Db:  ");
       JLabel DiffLabel = new JLabel("Diffusion Constants: ");
       JLabel sLabel = new JLabel("                  Reaction rate: ");
       subpanel10.add(DiffLabel);
       subpanel10.add(DaLabel);
       subpanel10.add(D1field);
       subpanel10.add(DbLabel);
       subpanel10.add(D2field);
       subpanel10.add(sLabel);
       subpanel10.add(sfield);
       if(RD)controls.add(subpanel10);
       
       //preset patterns
       JPanel subpanel12= new JPanel();
       patternChoice = new JComboBox();
       patternChoice.addItem("Cheetah");
       patternChoice.addItem("Fingerprint");
       patternChoice.addItem("Colony");
       patternChoice.addItem("Maze");
       JLabel patternLabel = new JLabel("Presets: ");
       subpanel12.add(patternLabel);
       subpanel12.add(patternChoice);
       if(RD) controls.add(subpanel12);
       
       // show which substance?
       JPanel subpanel11 = new JPanel();
       showAButton = new JRadioButton(" A ");
       showBButton = new JRadioButton(" B ");
       showAButton.setSelected(true);       
       ButtonGroup group = new ButtonGroup();
       group.add(showAButton);
       group.add(showBButton);
       JLabel radioLabel = new JLabel("Show Concentration of Substance: \n");       
       subpanel11.add(radioLabel);
       subpanel11.add(showAButton);
       subpanel11.add(showBButton);       
       if(RD) controls.add(subpanel11);
       
       // colour chooser
       JPanel subpanel13 = new JPanel();
       JLabel colLabel = new JLabel("Colours: ");
       defaultColoursButton = new JButton();
       defaultColoursButton.setText("Default Colours");
       maxColourButton = new JButton();
       minColourButton = new JButton();
       minColourButton.setBackground(myCA.DEFAULT_CMIN);
       minColourButton.setText("     ");
       maxColourButton.setBackground(myCA.DEFAULT_CMAX);
       maxColourButton.setText("     ");
       minColourButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent evt) {
               colourButtonActionPerformed(evt);
           }
       });
       maxColourButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent evt) {
               colourButtonActionPerformed(evt);
           }
       });
       subpanel13.add(colLabel);
       subpanel13.add(minColourButton);
       subpanel13.add(maxColourButton);
       subpanel13.add(defaultColoursButton);
       if(RD) controls.add(subpanel13);
       
       //freeze
       JPanel subpanel14 = new JPanel();
       freezeButton = new JButton();
       freezeButton.setText("Freeze");
       freezeSetButton = new JButton();
       freezeSetButton.setText("Freeze & Set");
       subpanel14.add(freezeButton);
       subpanel14.add(freezeSetButton);
       if(RD) controls.add(subpanel14);
       
       //update interval
       JPanel subpanel15 = new JPanel();      
       updateInterval = new JTextField();
       updateInterval.setColumns(4);
       updateInterval.setText("25");
       JLabel intervalLabel = new JLabel("update every ");
       JLabel intervalLabel2= new JLabel(" generations");
       subpanel15.add(intervalLabel);
       subpanel15.add(updateInterval);
       subpanel15.add(intervalLabel2);
       if(RD) controls.add(subpanel15);      
       
       
       
       ChangeListener myListener2 = new ChangeListener() {
	    	 public void stateChanged(ChangeEvent e) {
	    		 number = tabs.getSelectedIndex();
	    	 }
	       };
	      tabs.addChangeListener(myListener2);
       
	      
	      
      add("Center",myCA);
      add("South",controls);  
      addListeners();
       
      
    }   
    
    

    
    
    
    /** 
     * Starts the machine by creating and starting a new thread
     */
    public void start(){ 
      if (runner == null) {
        runner = new Thread(this);
        runner.start();        
      }	
    }
    
    
	public void stop(){ 
	  if (runner != null) {
	    Thread moribund = runner;
	    // Set runner instance to NULL
        runner = null;
        // Interrupt the thread, beceause it might be blocked
        moribund.interrupt();
      }	
    }
	
    //public void update(Graphics g){
    //	paint(g);    	
    //}

	public void run() {
		Thread thisThread = Thread.currentThread();
	    myCA.setParams(numCols,widthCA,heightCA,twoDimensional,cyclic,threshold,life,RD);
	    
		while(thisThread == runner){
			try{				
			  if(auto){
				myCA.makeNextUniverse();		    
			  }
			  else{
				  synchronized(this){
					  while(!(auto) && thisThread == runner){
						  wait();
					  }
				  }
			  }
		  
			if(myCA.visible){  Thread.sleep(1L);			 
				
			}
			}catch(InterruptedException _ex) { 
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
   
	private void addListeners(){
		
	
		defaultColoursButton.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
				myCA.CMIN = myCA.DEFAULT_CMIN;
				myCA.CMAX = myCA.DEFAULT_CMAX;
				myCA.setColorScheme();
			}
		});
		
		showAButton.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
				myCA.showwhich = 0;				
				if(!auto) myCA.drawOffscreen();
			}
		});
		
		showBButton.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){			
				myCA.showwhich = 1;	
				if(!auto) myCA.drawOffscreen();
			}
		});
		
		
		/*if(pictureChoice!=null)*/patternChoice.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int pic = patternChoice.getSelectedIndex();
				
			    switch(pic){
			    	case 0:		myCA.nextDa = 16.0;
			    				myCA.nextDb = 3.5;
			    				myCA.nextRDs = 1.0;
			    				myCA.RDparamschanged= true;
			    				D1field.setText("16.0");
			    				D2field.setText("3.5");
			    				break;
			    				
			    	case 1:		myCA.nextDa = 16.0;
    							myCA.nextDb = 1.0;
    							myCA.nextRDs = 1.0;
    							myCA.RDparamschanged = true;
    							D1field.setText("16.0");
			    				D2field.setText("1.0");    							
			    				break;
			    				
			    	case 2:		myCA.nextDa = 1.6;
								myCA.nextDb = 6.0;
								myCA.nextRDs = 1.0;
								myCA.RDparamschanged = true;
								D1field.setText("1.6");
								D2field.setText("6.0");    							
								break;
								
			    	case 3:		myCA.nextDa = 2.6;
								myCA.nextDb = 24.0;
								myCA.nextRDs = 1.0;
								myCA.RDparamschanged = true;
								D1field.setText("2.6");
								D2field.setText("24.0");    							
								break;			
			    				
			    	default:	break;
			    }
				
				
			}
		});
		
		updateInterval.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
				double val = myCA.times;
				try{				
				val = Double.parseDouble(updateInterval.getText());
				myCA.times = (int) val;									
			}catch(NumberFormatException nfe){}			
						
			}
		});
		
		D1field.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
				double val = myCA.RD_Da;
				try{	
				auto = false;		
				playButton.setEnabled(true);
				val = Double.parseDouble(D1field.getText());
				myCA.nextDa = val;									
				myCA.RDparamschanged = true;				
			}catch(NumberFormatException nfe){}
			
						
			}
		});
		
		D2field.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
			try{
				auto = false;
				playButton.setEnabled(true);
				double val;
				val = Double.parseDouble(D2field.getText());
				myCA.nextDb = val;
				myCA.RDparamschanged = true;
			
			}catch(NumberFormatException nfe){}
			}
		});
		
		
		sfield.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
				double val = myCA.RD_s;
				try{	
				auto = false;		
				playButton.setEnabled(true);
				val = Double.parseDouble(sfield.getText());
				myCA.nextRDs = val;									
				myCA.RDparamschanged = true;
			}catch(NumberFormatException nfe){}
			
						
			}
		});
		

		freezeButton.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
				auto = false;
				myCA.nextfrozen = true;
				myCA.RDparamschanged = true;	
				myCA.frozenSetValue = false;
			}
		}); 
		
		freezeSetButton.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
				auto = false;
				myCA.nextfrozen = true;
				myCA.RDparamschanged = true;
				myCA.frozenSetValue = true;
			}
		}); 
		
		
		/*if(pictureChoice!=null)*/pictureChoice.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int pic = pictureChoice.getSelectedIndex();
				
				if(pic==4){
				  myCA.numCols=15;
				  myCA.beastie = true;
				  myCA.setColorScheme();
				}
				
				myCA.picture = pic;
				myCA.initRandomUniverse();
				
				
			}
		});
		/*if(lifeFormChoice!=null)*/lifeFormChoice.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int shape = lifeFormChoice.getSelectedIndex();		
				myCA.initLifeUniverse(shape);
			}
		});
		/*if(neighbourhood!=null)*/neighbourhood.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(neighbourhood.getSelectedItem().equals("Moore")){
					myCA.moore = true;
					myCA.neumann = false;
					myCA.cross = false;
					myCA.plus = false;
				}else if(neighbourhood.getSelectedItem().equals("Von Neumann")) {
					myCA.moore = false;
					myCA.neumann = true;	
					myCA.cross = false;
					myCA.plus = false;
				}else if(neighbourhood.getSelectedItem().equals("Plus")) {
					myCA.moore = false;
					myCA.neumann = false;
					myCA.plus = true;
					myCA.cross = false;
				}else if(neighbourhood.getSelectedItem().equals("Cross")) {
					myCA.moore = false;
					myCA.neumann = false;	
					myCA.plus = false;
					myCA.cross = true;				
				}
			 	/*if(thresholdScrollbar!=null)*/thresholdScrollbar.setMaximum(neighbourhoodSize(myCA.radius));		
			}
		});
		/*if(colourScheme!=null)*/colourScheme.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(colourScheme.getSelectedItem()=="Rainbow"){
					  myCA.myColorScheme = CACanvas.colorScheme.Rainbow;
					  myCA.setColorScheme();
					  myCA.drawOffscreen();
				 }else if(colourScheme.getSelectedItem()=="Ocean"){
					  myCA.myColorScheme = CACanvas.colorScheme.Ocean;
					  myCA.setColorScheme();		
					  myCA.drawOffscreen();			  
				 }else if(colourScheme.getSelectedItem()=="Gray"){
					  myCA.myColorScheme = CACanvas.colorScheme.Gray;
					  myCA.setColorScheme();		
					  myCA.drawOffscreen();
					  
				  }
			}
		});
		/*if(restartButton!=null)*/restartButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				//auto = false;
				//playButton.setEnabled(true);
				myCA.restart();
	            reversePossible = false;
	            if(!auto) reverseButton.setEnabled(false);
	        }
		});
		/*if(reverseButton!=null)*/reverseButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
			    myCA.reverseStep();
	            reverseButton.setEnabled(false);
	            reversePossible = false;
	        }
		});
		/*if(randomButton!=null)*/randomButton.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent e){
				auto = false;				
				if(RD&&auto){					
					myCA.RDreset=true;					
				}
				else
				myCA.initRandomUniverse();
				
	           	reversePossible = false;
	           	playButton.setEnabled(true);
	            if(!auto) reverseButton.setEnabled(false);
	            
	            
	        }
		});
		/*if(playButton!=null)*/playButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(reversePossible==false){
	              reversePossible = true;
	              reverseButton.setEnabled(true);
	            }
	            playButton.setEnabled(false);
	            stopButton.setEnabled(true);
	            if(!cyclic && twoDimensional && myCA.generation==0){
	              	myCA.saveState();	            	
	            }
	            
	            synchronized(MCCA.this){
		            auto = true;
		            MCCA.this.notify();
	            }
			}
		});
		/*if(stopButton!=null)*/stopButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				auto = false;
	            playButton.setEnabled(true);
	            stopButton.setEnabled(false);
			}
		});
		/*if(stepButton!=null)*/stepButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				auto = false;
	            if(reversePossible==false){
	              reversePossible = true;
	              reverseButton.setEnabled(true);
	            }
	            if(!cyclic && twoDimensional && myCA.generation==0){
	              	myCA.saveState();	            	
	            }
	        	myCA.makeNextUniverse();
	        	playButton.setEnabled(true);
	            stopButton.setEnabled(false);
	        }
		});
		/*if(snapshotButton!=null)*/snapshotButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				myCA.takeSnapshot();
	        }
		});
		///*if(seedButton!=null)*/seedButton.addActionListener(new ActionListener(){
		//	public void actionPerformed(ActionEvent e){
		//		myCA.placeSeed();
	    //    }
		//});
		/*if(visibleCheckbox!=null)*/visibleCheckbox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(visible){
	        		visible=false; 
	        		myCA.visible=false;
	        	}
	        	else{
	        		myCA.setChanged();
	        		visible=true;
	        		myCA.visible=true;
	        		
	        		
	        	}	
			}
		});
		/*if(totalisticCheckbox!=null)*/totalisticCheckbox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(totalistic){
	            	totalistic = false;            	
	            	long t = (long)Math.pow(numCols, 2*radius+1);
	        		long s = (long)Math.pow(numCols,t)-1;
	        		ruleLabel.setText(" (0-"+s+") ");
	        		String temp = ruleModel.getValue().toString();
	        		int i = Integer.parseInt(temp);
	        		ruleModel = new SpinnerNumberModel(Math.min(i,(int)s),0,(int)s,1);
	        		ruleSpinner.setModel(ruleModel);
	        		if(s<i) ruleSpinner.setValue(s);
	            }
	            else{
	              totalistic = true;
	              int maxval = (numCols-1)*(2*radius+1)+1;
	              long s = (long)Math.pow(numCols,maxval)-1;
	             
	              ruleLabel.setText(" (0-"+s+") ");
	              String temp = ruleModel.getValue().toString();
	      		  int i = Integer.parseInt(temp);
	      		  ruleModel = new SpinnerNumberModel(Math.min(i,(int)s),0,(int)s,1);
	      		  ruleSpinner.setModel(ruleModel);
	      		  if(s<i) ruleSpinner.setValue(s);
	            }
	            myCA.totalistic = totalistic;
	            myCA.setRule();
			}
		});
		
		singlePointCheckbox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(myCA.singlepoint)
	            	myCA.singlepoint = false;        
	            else
                    myCA.singlepoint = true;
			}
		});
		
		
		/*if(radiusScrollbar!=null)*/radiusScrollbar.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				int i = radiusScrollbar.getValue();        	
	        	radiusLabel.setText(i+"  ");
	        	if(cyclic)thresholdScrollbar.setMaximum(neighbourhoodSize(i));
	        	myCA.radius = i;   
	        	radius = i;
	        	if(!cyclic && !twoDimensional){
	        		long s;
	        		if(totalistic){
	        		  int maxval = (numCols-1)*(2*radius+1)+1;
	                  s = (long)Math.pow(numCols,maxval)-1;	        			
	        		}
	        		else{
	        		  long t = (long)Math.pow(numCols, 2*radius+1);
	        		  s = (long)Math.pow(numCols,t)-1;
	        		}
	        		ruleLabel.setText(" (0-"+s+") ");
	        		myCA.setRule();
	        		String temp = ruleModel.getValue().toString();
	        		int j = Integer.parseInt(temp);
	        		ruleModel = new SpinnerNumberModel(minimum(j,s),0,minimum(s,maxInt),1);
	        		ruleSpinner.setModel(ruleModel);
	        		if(s<j) ruleSpinner.setValue(s);
	        	}	
			}
		});
		/*if(thresholdScrollbar!=null)*/thresholdScrollbar.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				int i = thresholdScrollbar.getValue();        	
	        	thresholdLabel.setText(i+"  ");        	
	        	myCA.threshold = i;
	        }
		});
		/*if(numColsScrollbar!=null)*/numColsScrollbar.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
	        	int i = numColsScrollbar.getValue();        	
	        	numColsLabel.setText(i+"  ");        	
	        	myCA.numCols = i;
	        	numCols = i;
	        	myCA.initRandomUniverse();
	           	reversePossible = false;
	            if(!auto) reverseButton.setEnabled(false);
	        	if(!cyclic && !twoDimensional){
	        		long s=0;
	        		
	        		if(totalistic){
	        		  int maxval = (numCols-1)*(2*radius+1)+1;
	                  s = (long)Math.pow(numCols,maxval)-1;
	        		}
	        		if(!totalistic){
	        		  long t = (long)Math.pow(numCols, 2*radius+1);
	        		  s = (long)Math.pow(numCols,t)-1;
	        		  
	        		}
	        		ruleLabel.setText(" (0-"+s+") ");        		
	        		myCA.setRule();
	        		String temp = ruleModel.getValue().toString();
	        		int j = Integer.parseInt(temp);
	        		int k = minimum(j, s);
	        
	        		ruleModel = new SpinnerNumberModel(k,0,minimum(s,maxInt),1);
	        		ruleSpinner.setModel(ruleModel);
	        		if(s<j) ruleSpinner.setValue(s);
	        	}
	        }
		});
	}
    
	
	
	private void colourButtonActionPerformed(java.awt.event.ActionEvent evt) {
		boolean isMin = evt.getSource() == minColourButton;

		Color min = minColourButton.getBackground();
		Color max = maxColourButton.getBackground();

		Component parent = isMin ? minColourButton : maxColourButton;

		Color pick = JColorChooser.showDialog( parent, "Choose Colour", isMin ? min : max );
		if( pick != null ) {
			if( isMin ) {
				min = pick;
			} else {
				max = pick;
			}
			myCA.CMIN = min;
			myCA.CMAX = max;
			myCA.setColorScheme();
			minColourButton.setBackground( min );
			maxColourButton.setBackground( max );
		}
	}
	
	
    private int neighbourhoodSize(int r){            
      int size = (int)Math.pow(r*2+1,2)-1;  // Moore
     
      if (myCA.neumann)
    	size -= 4*((r*(r+1))/2);
      else if(myCA.plus || myCA.cross)
    	size = 4*r;  

     return size;  	  
    }
    
    private int minimum(long a, long b){
    	if(b<a&&b<maxInt) return (int)b;
    	else if(a<maxInt) return (int)a;
    	else return (int)maxInt;
    }
    
}


