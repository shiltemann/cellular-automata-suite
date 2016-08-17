package mcca;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Random;
import javax.swing.*;

public class CACanvas extends JPanel {  
  private static final long serialVersionUID = 1L;
  
  Graphics bufferGraphics;	
  Image offscreen;
  private int universe[][] = null;		// our automaton (universe)
  private int nextUniverse[][]= null;   // next state of our universe
  private boolean changed[][]=null;		// record which pixels have changed
  private int firstUniverse[][]=null;   // remember what we started with
  private int previousUniverse[][]=null;// previous universe
  private int refUniverse[][]=null;		// universe we hope to find again (snapshot function)
  private int transitions[]=null;		// contains the transition rule for 1D standard
  private volatile double RDUniverse[][][]=null;  // the CA for reaction diffusion systems
  private volatile double RDNextUniverse[][][]=null;
  private double RDFirstUniverse[][][]=null;
  private boolean RDFrozen[][]=null;
  
  private int width;					// width of universe  
  private int height;					// height of universe
  public int numCols;					// how many colours (states) for each cell
  private int maxCols;					// maximum number of colors
  private Color[] myCols = null;		// the colours themselves;
  private Random rand;
  private boolean cyclic;      			// using cyclic CA?
  private boolean RD;
  public boolean moore = false;			// using moore neighbourhood?
  public boolean neumann = true;		// using von Neumann nbh?
  public boolean plus = false;
  public boolean cross = false;
  public boolean totalistic = false;
  public boolean singlepoint = true;
  private boolean life;
  public int radius = 1;
  public int threshold = 1;
  private double tilingdegree;
  private int pixsize;
  public int generation;
  private int lastgen;
  public boolean wrapped = true;		// edges wrapped? 
  private boolean twoDimensional;
  public enum colorScheme{Rainbow,Ocean,Gray,FGSB};
  public colorScheme myColorScheme;
  private Label genCount;
  public boolean converged;				// has automaton converged to stable state?
  //public boolean auto;
  public boolean reset;
  public boolean calcCycle;

  private int cycleStart;
  private int cycleLength;
  public int rule;
  public boolean visible=true; // show on screen? (for debugging purposes mostly)
  public int picture;
  boolean dragging = false;  // This is true when a drag is in progress.
  private int prev_x, prev_y;
  public boolean beastie = false;

  public double RD_s = 1.0;
  public double olrate = 1d;
  public double lrate = 0.01d;
  public volatile double RD_Da=16d;
  public volatile double RD_Db=3.5d;  
  public double RD_beta = 12.0d;
  public double RD_sigma = 0.05d;
  public int numMorphogens = 2;
  public double nextDa=RD_Da;
  public double nextDb=RD_Db;
  public double nextRDs = RD_s;
  public boolean RDreset=false;
  public boolean RDparamschanged = false;
  public boolean frozen = false;
  public boolean nextfrozen = false;
  public boolean frozenSetValue=false;
  public double frozenThreshold = 4.0;
  public double frozenValue = 4.0;
  public int showwhich = 0;
  int times=25;                      //update RD state every how many generations?
  private double RDtilingdegree_x;
  private double RDtilingdegree_y;
  private double tile;  
  public static final Color DEFAULT_CMIN = new Color( 0x3c0009 );
  public static final Color DEFAULT_CMAX = new Color( 0xffff3a );
  public Color CMIN = new Color( 0x3c0009 );
  public Color CMAX = new Color( 0xffff3a );
  private int[] colours;
  int freezewhich = 0;  
  
  public void makeSize(int w, int h,boolean R){
	 pixsize = 2;
	 tilingdegree=1.5;
	 height = h;
     width = w;
     RD = R;
     
     if(RD) pixsize = 3;
	 Dimension d = new Dimension();
	 d.height = (int)(height)+30;
	 d.width = (int)(width)+5;
	 setPreferredSize(d);
	 setMinimumSize(d);
	 
	 height /= pixsize;
	 width /= pixsize;	 
	 
	 if(RD){
		 width = 192;
		 height = 192;
		 RDtilingdegree_x = w/(double)width;
		 RDtilingdegree_y = h/(double)height;
		 System.out.println("RDtilingdegrees: "+RDtilingdegree_x+", y: "+RDtilingdegree_y);
		 
	 }
	 else{
	   height /= tilingdegree;
	   width /= tilingdegree;
	 }
	 
	 
  } 

  // initialize all parameters
  public void setParams(int numCol, int w, int h, boolean dim, boolean c, int t, boolean l, boolean RD_){
	
	 
	  
	addMouseListener(new MouseAdapter(){
		public void mouseClicked(MouseEvent evt) {
			int y = evt.getY();
			int x = evt.getX(); 
			
			if (twoDimensional && !cyclic && evt.isMetaDown()&& !RD ) 
				clearCanvas();		
			else{
			  x/=pixsize;
			  y/=pixsize;
			  prev_y = y;
			  prev_x = x;
			
			  int oldval = universe[x][y];
			  universe[x][y] = (oldval+1)%numCols;
			  changed[x][y] = true;
			  drawOffscreen();
			}
		}

		public void mousePressed(MouseEvent evt) {	
	
		}		
	});  
	addMouseMotionListener(new MouseMotionAdapter(){
		public void mouseDragged(MouseEvent evt) {
			if(twoDimensional && !cyclic){
				int y = evt.getY();
				int x = evt.getX();    
				
		        x /= pixsize;
		        y /= pixsize;	        
		       
		        
		        if(!life && !(prev_x==x&&prev_y==y)){
		          int oldval = universe[x][y];
		          universe[x][y] = (oldval+1)%numCols;
		          changed[x][y] = true;		        
		          drawOffscreen(); 	
		        }
		        else if(life){
		            universe[x][y] = 1;
			        changed[x][y] = true;		        
			        drawOffscreen(); 			        	
		        }
		        
		        prev_x = x;
		        prev_y = y;
			}
		}
	});
	  
	twoDimensional = dim;
	cyclic = c;
	rule = 30;
	threshold = t;
	life = l;
	picture = 0;
	RD = RD_;

	 
	if(tilingdegree!=1 && !(twoDimensional && cyclic)&&!RD){
		width *= tilingdegree;
		height *= tilingdegree;
		tilingdegree=1;		
	}
	
	if(life){
	  width = width/4;
	  height = height/4;
	  pixsize = 8;
	}
	else if(twoDimensional && !cyclic && !RD){
	  width = width*2/3;
	  height = height*2/3;
	  pixsize = 3; 	  
	}
	converged = false;
	reset = false;
	calcCycle = false;
	generation = 0;
	lastgen=0;
	maxCols = 16;	
	numCols = numCol;
	cycleStart = 0;
	cycleLength = -1;

	myCols = new Color[maxCols];
	universe = new int[width][height];
	nextUniverse = new int[width][height];
	firstUniverse = new int[width][height];
	previousUniverse = new int[width][height];
	refUniverse = new int[width][height];
	changed = new boolean[width][height];
    RDUniverse = new double[width][height][numMorphogens+1];
	RDNextUniverse = new double[width][height][numMorphogens+1];
	RDFirstUniverse = new double[width][height][numMorphogens+1];
	RDFrozen = new boolean[width][height];
    
    if(twoDimensional&&cyclic)
    myColorScheme = colorScheme.Rainbow;
    else
	myColorScheme = colorScheme.Ocean;
	setColorScheme();
	
	if(twoDimensional&&!cyclic){
		moore = true;
		neumann = false;
	}
		
	if(RD) offscreen = createImage((int)(width*pixsize*RDtilingdegree_x),(int)(height*pixsize*RDtilingdegree_y));
	else offscreen = createImage((int)(width*pixsize*tilingdegree),(int)(height*pixsize*tilingdegree));	
	bufferGraphics = offscreen.getGraphics();

	rand = new Random();	
	genCount = new Label();
	genCount.setText(generation+" test");
	genCount.setBounds(50, pixsize*height+15, 20, 7);
	genCount.setEnabled(true);
	
	if(life) initLifeUniverse(0);
	else initRandomUniverse();
	//else initSparseUniverse();
	
	setRule();
	if(RD) System.out.println("width and height: "+width+", "+height);
	
  }
  
  private void initSparseUniverse(){
	for(int i=0;i<width;i++)
	  for(int j=0;j<height;j++)
		  universe[i][j]=0;
	
	universe[width/2][height/2]=1;
	
	drawOffscreen();
	  
  }

  
  /**
	 * Calculate an interpolated colour component that is i/255 of the way between lo and hi.
	 */
  private static int scale( int i, int lo, int hi ) {
		return lo + (hi-lo) * i / 255;
  }
  	
  
 public final void setColorScheme(){
	
    if(RD){
    	colours = new int[256];
		int Rlo = CMIN.getRed();
		int Glo = CMIN.getGreen();
		int Blo = CMIN.getBlue();
		int Rhi = CMAX.getRed();
		int Ghi = CMAX.getGreen();
		int Bhi = CMAX.getBlue();

		for( int i=0; i<256; ++i ) {
			colours[i] = scale( i, Rlo, Rhi ) << 16 | scale( i, Glo, Ghi ) << 8  | 	scale( i, Blo, Bhi );
		}   
    
    }	  
	  
    else if(beastie){
		
		myCols[0]= new Color(0xf8f8f8);
		myCols[12]= new Color(0xf8f8a8);
		myCols[10]= new Color(0xa8a8a8);
		myCols[1]= new Color(0x500000);
		myCols[4]= new Color(0x505050);
		myCols[2]= new Color(0x500050);
		myCols[3]= new Color(0xa80000);
		myCols[6]= new Color(0xa85050);
		myCols[13]= new Color(0x000000);
		myCols[5]= new Color(0xa80050);
		myCols[8]= new Color(0xa850a8);
		myCols[11]= new Color(0xf8a8a8);
		myCols[9]= new Color(0xf850a8);
		myCols[7]= new Color(0xa800a8);
		
		myCols[5]= new Color(0xffffff); //white
		myCols[2]= new Color(0xff0000);  // red
		myCols[4]= new Color(0xff8284);  //pink
		myCols[1]= new Color(0x000084);  //blue
		myCols[0]= new Color(0x8482ff);  // light blue
		myCols[3]= new Color(0x848284);  //gray
	}  
	  
	else if(myColorScheme.toString()=="FGSB"){
		  myCols[0]=new Color(0xffffff);  // white
		  myCols[1]=new Color(0x000000);  // black
		  myCols[2]=new Color(0xff0000);  // red
		  myCols[3]=new Color(0x555555);  // green
	      myCols[4]=new Color(0x0000ff);  // blue
	}
	
	else if(myColorScheme.toString()=="Rainbow"){
      myCols[0]=new Color(0xaaff00);  // lime green
	  myCols[1]=new Color(0xffff00);  // yellow
	  myCols[2]=new Color(0xffaa00);  // orange
	  myCols[3]=new Color(0xff5500);  // light red
      myCols[4]=new Color(0xff0000);  // red
      myCols[5]=new Color(0xff0055);  // apple red
      myCols[6]=new Color(0xff00aa);  // pink-red
      myCols[7]=new Color(0xff00ff);  // pink
      myCols[8]=new Color(0xaa00ff);  // purple
      myCols[9]=new Color(0x5500ff);  // dark blue
      myCols[10]=new Color(0x0000ff); // blue
      myCols[11]=new Color(0x0066ff); // bright blue
      myCols[12]=new Color(0x00ffff); // light blue
      myCols[13]=new Color(0x00ff66); // light green
      myCols[14]=new Color(0x00ff00); // green
      myCols[15]=new Color(0x55ff00); // real green      
    }
    else if(myColorScheme.toString()=="Ocean"){
	 myCols[0]=new Color(0x00ffffff);  // white
	 myCols[1]=new Color(0x0000adff);  // light blue 
	 myCols[2]=new Color(0x000000ff);  // blue
	 myCols[3]=new Color(0x004700b1);	   
	 myCols[4]=new Color(0x006f00af);  // purple
     myCols[5]=new Color(0x0000ffff);
     myCols[6]=new Color(0x00f0f000);
     myCols[7]=new Color(0x00f000f0);
     myCols[8]=new Color(0x0000f0f0);
     myCols[9]=new Color(0x00f0f0f0);
     myCols[10]=new Color(0x00ff0000);
     myCols[11]=new Color(0x00ffffff); // white  
     myCols[12]=new Color(0x0000ffff); // light blue
     myCols[13]=new Color(0x0000ff66); // light green
     myCols[14]=new Color(0x0000ff00); // green
     myCols[15]=new Color(0x0055ff00); // real green
    
	 //myCols[1] = new Color(0x006f00af);
     //myCols[2] = new Color(0x016ab0);
     //myCols[3] = new Color(0x0068b7);
    }
    
       
    else{  //grayscale
    	myCols[0]=new Color(0xffffff);  // white
   	    myCols[1]=new Color(0xe2e2e2);  
   	    myCols[2]=new Color(0xc6c6c6);  
   	    myCols[3]=new Color(0xaaaaaa);	   
   	    myCols[4]=new Color(0x8e8e8e);  
        myCols[5]=new Color(0x717171);
        myCols[6]=new Color(0x555555);
        myCols[7]=new Color(0x393939);
        myCols[8]=new Color(0x1c1c1c);
        myCols[9]=new Color(0x000000);
        myCols[10]=new Color(0xff0000);	
    	
    	
    }
    
	for(int i=0;i<width;i++)
	  for(int j=0;j<height;j++)
	    changed[i][j]=true;   
    
  }
   
    
  public synchronized void saveState(){
    for(int i=0;i<width;i++)	 
     for(int j=0;j<height;j++){
    	 firstUniverse[i][j] = universe[i][j];    	 
     }	  
  }
  
  
  synchronized public void setChanged(){
	 for(int i=0;i<width;i++)
	   for(int j=0;j<height;j++)
		   changed[i][j]=true;
  }
  
 public synchronized void takeSnapshot(){

	 for(int i=0;i<width;i++)
      for(int j=0;j<height;j++)
    	  refUniverse[i][j] = universe[i][j];
    
	cycleLength = -1;
    calcCycle = true; 
	cycleStart = generation;
 }
  
  // define our own modulo operation to handle negative numbers in the way we want
  // eg -1 mod 6 = 5 (would be -1 if we use % operator)
  private int mod(int a, int b){
	return (int)(a - Math.floor((double)a/b)*b);  
  }
  
  public void reverseStep(){
	for(int i=0;i<width;i++)
	 for(int j=0;j<height;j++)
	   universe[i][j] = previousUniverse[i][j];
	
	generation--;
	//lastgen--;
	converged = false;
	drawOffscreen();
  }
  
  // implementation of the cyclic CA
  private void cyclicNextUniverse(){
	
	int thisCol,nextCol,numNextCol;
	boolean change = false;
	
	generation++;
	for(int i=0;i<width;i++)     
	  for(int j=0;j<height;j++){  // for each cell
		 thisCol = universe[i][j];
		 nextCol = (thisCol+1)%numCols;
		 numNextCol = 0;
		 for(int k=i-radius;k<=i+radius;k++)
		  for(int l=j-radius;l<=j+radius;l++){	 // for all neighbours
		    
			//for some neighbourhoods exclude some fields from count
			if(plus && k!=i && l!=j){}   								// PLUS
		    else if(neumann && (Math.abs(i-k)+Math.abs(j-l)>radius) ){} // NEUMANN
		    else if(cross && Math.abs(k-i) != Math.abs(l-j) ){}         // CROSS
		    else if(universe[mod(k,width)][mod(l,height)]==nextCol)
		      numNextCol++; 	 
		  }
		 if(numNextCol>=threshold){
		   nextUniverse[i][j] = nextCol;
		   changed[i][j]=true;
		   change = true;
		 }
		 else{
		   nextUniverse[i][j] = thisCol;
		   changed[i][j]=false;
		 }
		 
	  }

	if(change == false) converged = true; 
	
	// copy universe to previousUniverse and nextUniverse to universe
	int[][] temp = universe,temp2 = previousUniverse;
	universe = nextUniverse;
	previousUniverse = temp; 
	nextUniverse = temp2;

	
	//for(int i=0;i<width;i++)     
	//  for(int j=0;j<height;j++){
	//	  previousUniverse[i][j] = universe[i][j];
	//	  universe[i][j] = nextUniverse[i][j];
	//}
	
	// check for a cycle
	if(calcCycle){
	  boolean same = true;
	  for(int i=0;i<width;i++){     
		 for(int j=0;j<height;j++){
			if(universe[i][j]!=refUniverse[i][j]){
			  same=false;
			  break;
			}		 	 
		 }
	     if(!same) break;
	  }
	  if(same){
		 cycleLength = generation - cycleStart;		 
		 calcCycle = false;
	  }
	}
  }  

 // update cyclic CA in 1 dimensional case 
  private void cyclicNextUniverse_1D(){
	int thisCol,nextCol,numNextCol,newRow;	
	generation++;
	
	for(int i=0;i<width;i++)     
	 for(int j=0;j<height;j++)
		previousUniverse[i][j] = universe[i][j];
	
	for(int i=0;i<width;i++)
	  for(int j=0;j<height;j++)
		changed[i][j]=false;
	
	//move rows up if necessary, then add new row
	if(generation>=height){
	  for(int i=0;i<width;i++)
		for(int j=0;j<height-1;j++){
		  if(universe[i][j] != universe[i][j+1]) changed[i][j] = true;	
		  universe[i][j] = universe[i][j+1];	
		}
	}	
	
	// add a row on bottom	  
	newRow = Math.min(generation,height-1);
	for(int i=0;i<width;i++){ //for all cells
	  thisCol = universe[i][newRow-1];
	  nextCol = (thisCol+1)%numCols;
	  numNextCol = 0;
	  for(int k=i-radius; k<=i+radius ;k++){  // for every cell in neighbourhood
		if(universe[mod(k,width)][mod(newRow-1,height)]==nextCol) numNextCol++;			
	  }  
	  if(numNextCol>=threshold){
		nextUniverse[i][newRow]=nextCol;	
	  }
	  else nextUniverse[i][newRow]= thisCol;
	}
	for(int i=0;i<width;i++){
	  universe[i][newRow]=nextUniverse[i][newRow];
	  changed[i][newRow]=true;
	}	  
  }
 
  private void standardNextUniverse(){
	int newRow;
	generation++;
	
	for(int i=0;i<width;i++)     
	 for(int j=0;j<height;j++)
		previousUniverse[i][j] = universe[i][j];
	
	for(int i=0;i<width;i++)
	  for(int j=0;j<height;j++)
		changed[i][j]=false;
	
	//move rows up if necessary
	if(generation>=height){
	  for(int i=0;i<width;i++)
		for(int j=0;j<height-1;j++){
		  if(universe[i][j] != universe[i][j+1]) changed[i][j] = true;	
		  universe[i][j] = universe[i][j+1];	
		}
	}	
	
	// add the new row
	int pow;
	int val,k,val_tot;
	newRow = Math.min(generation,height-1);
	for(int i=0;i<width;i++){ //for all cells
	  pow = 2*radius;
	  val = 0;
	  val_tot = 0;
	  k=i-radius;
	  while(pow>=0){
		val += (Math.pow(numCols,pow)*universe[mod(k,width)][newRow-1]);  
		val_tot += universe[mod(k,width)][newRow-1];
		pow--;
		k++;		  
	  }
	  if(totalistic) val = val_tot;
	  nextUniverse[i][newRow] = transitions[val]; 
	}
	
	//copy calculated rule to universe;
	for(int i=0;i<width;i++){
	  universe[i][newRow]=nextUniverse[i][newRow];
	  changed[i][newRow]=true;
	}	
  }

  private void lifeNextUniverse(){
	
	int val;
	generation++;
	
	for(int i=0;i<width;i++)     
	 for(int j=0;j<height;j++)
		previousUniverse[i][j] = universe[i][j];
	
	for(int i=0;i<width;i++)
	  for(int j=0;j<height;j++){  // for all cells
		 val = 0;
		 for(int k=i-1;k<=i+1;k++)
		   for(int l=j-1;l<=j+1;l++){
			 val += universe[mod(k,width)][mod(l,height)];				
		   }
		 val -= universe[i][j];
		 if(universe[i][j]==1 && (val<2||val>3)) nextUniverse[i][j] = 0;
		 else if(universe[i][j]==0 && val ==3) nextUniverse[i][j] = 1;
		 else nextUniverse[i][j] = universe[i][j];
	  }
	
	//copy calculated rule to universe;	
	for(int i=0;i<width;i++)
		for(int j=0;j<height;j++){
	  universe[i][j]=nextUniverse[i][j];
	  changed[i][j]=true;
	}
	if(!RD)
	try{
        Thread.sleep(1);
       }
       catch(Exception _ex) { }
  }

  private void xorNextUniverse(){
	int val;
	
	for(int i=0;i<width;i++)     
		 for(int j=0;j<height;j++)
			previousUniverse[i][j] = universe[i][j];
	
	generation++;
	
	for(int i=0;i<width;i++)
	  for(int j=0;j<height;j++){  // for all cells
		  val = 0;
		  for(int k=i-radius;k<=i+radius;k++)
			for(int l=j-radius;l<=j+radius;l++){
				
				if(plus && k!=i && l!=j){}   								// PLUS
			    else if(neumann && (Math.abs(i-k)+Math.abs(j-l)>radius) ){} // NEUMANN
			    else if(cross && Math.abs(k-i) != Math.abs(l-j) ){}         // CROSS
			    else val += universe[mod(k,width)][mod(l,height)];				
			}  
		  nextUniverse[i][j] = (val-universe[i][j])%numCols;	
		  
		  nextUniverse[i][j] = (val+universe[i][j])%numCols;  //TODO: remove
	  }	
	
		
	for(int i=0;i<width;i++)
		for(int j=0;j<height;j++){
	  universe[i][j]=nextUniverse[i][j];
	  changed[i][j]=true;
	}	
  }

// set the requested rule
  public void setRule(){    
   int r = rule;
   int div;
   int pow = (int)(Math.pow(numCols,2*radius+1));
   if(totalistic) pow =  (int) (Math.pow(numCols,(numCols-1)*(2*radius+1)));
   transitions = new int[pow];
   while(pow>0){	   
	 div = (int)Math.pow(numCols,pow-1);
	 transitions[pow-1]=r/div;
     r -= (transitions[pow-1]*div);  
     pow--;
   }
  }
    
  //for RD systems, freeze certain cells, restart the rest
  public void frozenReset(){
	
	int fcount=0,nfcount=0;
	double avg=0;
	
	//calculate average
	for(int i=0; i<width; i++)
		for(int j=0; j<height; j++){
			avg += RDUniverse[i][j][freezewhich];			
		}
	avg /= (width*height);
	
	//check which cells to freeze
	for(int i=0; i<width; i++)
		for(int j=0; j<height; j++){
			if(RDUniverse[i][j][freezewhich]>=avg){ RDFrozen[i][j]=true;fcount++; /*RDUniverse[i][j][freezewhich] = avg;*/}
			else{ RDFrozen[i][j]= false; nfcount++;	}	
			
		}
	System.out.println("frozen-unfrozen cells: "+fcount+", "+nfcount);
	  
	//if making leopard spots, set frozen cells to 4.0  
	// reinitialize other cells  
	for(int i=0; i<width; i++)
		for(int j=0; j<height; j++){
			if(!RDFrozen[i][j]){
				RDUniverse[i][j][0] = 4.0 + rand.nextGaussian() * RD_sigma;
				RDUniverse[i][j][1] = 4.0 + rand.nextGaussian() * RD_sigma;	
			}
			else if(frozenSetValue){
				RDUniverse[i][j][0] = 4.0;
				RDUniverse[i][j][1] = 4.0;	
			}			
		}  	
	//frozen = true;
	drawOffscreen();
  }
  
  public void makeRDNextUniverse(){
	  	  
	  
	  double aij,bij,amj,aim,aip,apj,bmj,bim,bip,bpj,da,db,beta;
	  
	  
	  generation += times;
	  for(int time=0; time<times; time++){
		//generation++;
		for(int i=0; i<width; i++)
		 for(int j=0; j<height; j++){
			//beta = RDUniverse[i][j][2];
			beta = RD_beta;
			
			aij = RDUniverse[i][j][0];
			bij = RDUniverse[i][j][1];			
			
			if(frozen && RDFrozen[i][j] ){  
				RDNextUniverse[i][j][0] = aij;
				RDNextUniverse[i][j][1] = bij;
			}
			else{
			amj = RDUniverse[mod(i-1,width)][j][0]; 
			aim = RDUniverse[i][mod(j-1,height)][0];
			apj = RDUniverse[mod(i+1,width)][j][0];
			aip = RDUniverse[i][mod(j+1,height)][0];
			
			bmj = RDUniverse[mod(i-1,width)][j][1]; 
			bim = RDUniverse[i][mod(j-1,height)][1];
			bpj = RDUniverse[mod(i+1,width)][j][1];
			bip = RDUniverse[i][mod(j+1,height)][1];
	        
			
			da = RD_s* (16.0-aij*bij) + RD_Da*(apj+amj+aim+aip-4.0*aij);
			db = RD_s* (aij*bij-bij-beta) +  RD_Db*(bpj+bmj+bim+bip-4.0*bij);
			RDNextUniverse[i][j][0] = aij + (lrate*da);	        	
	        RDNextUniverse[i][j][1] = bij + (lrate*db);	
			 
	        if(RDNextUniverse[i][j][1]<0) RDNextUniverse[i][j][1]=0;
	        if(RDNextUniverse[i][j][0]<0) RDNextUniverse[i][j][0]=0;
			}//else
		 }
		
		double[][][] temp = RDUniverse;
		RDUniverse = RDNextUniverse;
		RDNextUniverse = temp;
		
	  /*
	  for(int i=0; i<width; i++)
	  	  for(int j=0; j<height; j++){
			  RDUniverse[i][j][0] = RDNextUniverse[i][j][0];
			  RDUniverse[i][j][1] = RDNextUniverse[i][j][1];
	     } */
	  }	  

  }
  
 // get next state of CA 
 public synchronized  void makeNextUniverse(){
	 
   if(!converged){
	 if(RD){
		if(RDreset){
			initRandomUniverse();
			drawOffscreen();
			RDreset = false;
		} 
		if(RDparamschanged){
		RD_Da = nextDa;
	    RD_Db = nextDb;
	    RD_s = nextRDs;
	    RDparamschanged = false;
	    frozen = nextfrozen;
	    if(nextfrozen){ 
	    	frozenReset(); 
	    	nextfrozen = false;
	    	}
		}
		makeRDNextUniverse();
	 }
     else if (cyclic && twoDimensional)
	    cyclicNextUniverse();
	 else if(cyclic && !twoDimensional)
	    cyclicNextUniverse_1D();
	 else if(life)
	    lifeNextUniverse();	
	 else if(!twoDimensional && !cyclic)
		standardNextUniverse();
	 else if(twoDimensional&& !cyclic)
		xorNextUniverse();
	}
	drawOffscreen();

  }
  
 // restart this particular CA (same initial config)
  public synchronized void restart(){	  	  
	  
	for(int i=0;i<width;i++)
	  for(int j=0;j<height;j++){
	     if(RD){
	        RDUniverse[i][j][0] = RDFirstUniverse[i][j][0];	 
	        RDUniverse[i][j][1] = RDFirstUniverse[i][j][1];	
	        //RDNextUniver
	     }
	     else{ 
		   universe[i][j]=firstUniverse[i][j]; 
	       changed[i][j]=true;
	     }
	  }
	
	generation = 0;
	//auto = true;
	reset = true;
	calcCycle = false;
	cycleLength = -1;
	if(RD) frozen = false;
	drawOffscreen();
  }
  
  public void setBMP1(){
	  
      int i= width/2 -25;
      int j = height/2 -25;


/*  flag
      universe[i+0][j+0]= 2; 
      universe[i+1][j+0]= 4; 
      universe[i+2][j+0]= 5; 
      universe[i+3][j+0]= 5; 
      universe[i+4][j+0]= 5; 
      universe[i+5][j+0]= 5; 
      universe[i+6][j+0]= 5; 
      universe[i+7][j+0]= 5; 
      universe[i+8][j+0]= 5; 
      universe[i+9][j+0]= 3; 
      universe[i+10][j+0]= 1; 
      universe[i+11][j+0]= 1; 
      universe[i+12][j+0]= 1; 
      universe[i+13][j+0]= 1; 
      universe[i+14][j+0]= 1; 
      universe[i+15][j+0]= 1; 
      universe[i+16][j+0]= 1; 
      universe[i+17][j+0]= 1; 
      universe[i+18][j+0]= 1; 
      universe[i+19][j+0]= 1; 
      universe[i+20][j+0]= 1; 
      universe[i+21][j+0]= 1; 
      universe[i+22][j+0]= 1; 
      universe[i+23][j+0]= 1; 
      universe[i+24][j+0]= 1; 
      universe[i+25][j+0]= 1; 
      universe[i+26][j+0]= 1; 
      universe[i+27][j+0]= 1; 
      universe[i+28][j+0]= 1; 
      universe[i+29][j+0]= 1; 
      universe[i+30][j+0]= 1; 
      universe[i+31][j+0]= 4; 
      universe[i+32][j+0]= 5; 
      universe[i+33][j+0]= 4; 
      universe[i+34][j+0]= 2; 
      universe[i+35][j+0]= 2; 
      universe[i+36][j+0]= 2; 
      universe[i+37][j+0]= 2; 
      universe[i+38][j+0]= 2; 
      universe[i+39][j+0]= 2; 
      universe[i+40][j+0]= 2; 
      universe[i+41][j+0]= 4; 
      universe[i+42][j+0]= 5; 
      universe[i+43][j+0]= 4; 
      universe[i+44][j+0]= 1; 
      universe[i+45][j+0]= 1; 
      universe[i+46][j+0]= 1; 
      universe[i+47][j+0]= 1; 
      universe[i+48][j+0]= 1; 
      universe[i+49][j+0]= 1; 
      universe[i+50][j+0]= 1; 
      universe[i+51][j+0]= 1; 
      universe[i+52][j+0]= 1; 
      universe[i+53][j+0]= 1; 
      universe[i+54][j+0]= 1; 
      universe[i+55][j+0]= 1; 
      universe[i+56][j+0]= 1; 
      universe[i+57][j+0]= 1; 
      universe[i+58][j+0]= 1; 
      universe[i+59][j+0]= 1; 
      universe[i+60][j+0]= 1; 
      universe[i+61][j+0]= 1; 
      universe[i+62][j+0]= 1; 
      universe[i+63][j+0]= 1; 
      universe[i+64][j+0]= 1; 
      universe[i+65][j+0]= 3; 
      universe[i+66][j+0]= 5; 
      universe[i+67][j+0]= 5; 
      universe[i+68][j+0]= 4; 
      universe[i+69][j+0]= 2; 
      universe[i+70][j+0]= 2; 
      universe[i+71][j+0]= 2; 
      universe[i+72][j+0]= 2; 
      universe[i+73][j+0]= 2; 
      universe[i+74][j+0]= 4; 
      universe[i+0][j+1]= 2; 
      universe[i+1][j+1]= 2; 
      universe[i+2][j+1]= 2; 
      universe[i+3][j+1]= 4; 
      universe[i+4][j+1]= 5; 
      universe[i+5][j+1]= 5; 
      universe[i+6][j+1]= 5; 
      universe[i+7][j+1]= 5; 
      universe[i+8][j+1]= 5; 
      universe[i+9][j+1]= 5; 
      universe[i+10][j+1]= 5; 
      universe[i+11][j+1]= 3; 
      universe[i+12][j+1]= 1; 
      universe[i+13][j+1]= 1; 
      universe[i+14][j+1]= 1; 
      universe[i+15][j+1]= 1; 
      universe[i+16][j+1]= 1; 
      universe[i+17][j+1]= 1; 
      universe[i+18][j+1]= 1; 
      universe[i+19][j+1]= 1; 
      universe[i+20][j+1]= 1; 
      universe[i+21][j+1]= 1; 
      universe[i+22][j+1]= 1; 
      universe[i+23][j+1]= 1; 
      universe[i+24][j+1]= 1; 
      universe[i+25][j+1]= 1; 
      universe[i+26][j+1]= 1; 
      universe[i+27][j+1]= 1; 
      universe[i+28][j+1]= 1; 
      universe[i+29][j+1]= 1; 
      universe[i+30][j+1]= 1; 
      universe[i+31][j+1]= 4; 
      universe[i+32][j+1]= 5; 
      universe[i+33][j+1]= 4; 
      universe[i+34][j+1]= 2; 
      universe[i+35][j+1]= 2; 
      universe[i+36][j+1]= 2; 
      universe[i+37][j+1]= 2; 
      universe[i+38][j+1]= 2; 
      universe[i+39][j+1]= 2; 
      universe[i+40][j+1]= 2; 
      universe[i+41][j+1]= 4; 
      universe[i+42][j+1]= 5; 
      universe[i+43][j+1]= 4; 
      universe[i+44][j+1]= 1; 
      universe[i+45][j+1]= 1; 
      universe[i+46][j+1]= 1; 
      universe[i+47][j+1]= 1; 
      universe[i+48][j+1]= 1; 
      universe[i+49][j+1]= 1; 
      universe[i+50][j+1]= 1; 
      universe[i+51][j+1]= 1; 
      universe[i+52][j+1]= 1; 
      universe[i+53][j+1]= 1; 
      universe[i+54][j+1]= 1; 
      universe[i+55][j+1]= 1; 
      universe[i+56][j+1]= 1; 
      universe[i+57][j+1]= 1; 
      universe[i+58][j+1]= 1; 
      universe[i+59][j+1]= 1; 
      universe[i+60][j+1]= 1; 
      universe[i+61][j+1]= 1; 
      universe[i+62][j+1]= 1; 
      universe[i+63][j+1]= 3; 
      universe[i+64][j+1]= 5; 
      universe[i+65][j+1]= 5; 
      universe[i+66][j+1]= 4; 
      universe[i+67][j+1]= 2; 
      universe[i+68][j+1]= 2; 
      universe[i+69][j+1]= 2; 
      universe[i+70][j+1]= 2; 
      universe[i+71][j+1]= 2; 
      universe[i+72][j+1]= 4; 
      universe[i+73][j+1]= 5; 
      universe[i+74][j+1]= 5; 
      universe[i+0][j+2]= 2; 
      universe[i+1][j+2]= 2; 
      universe[i+2][j+2]= 2; 
      universe[i+3][j+2]= 2; 
      universe[i+4][j+2]= 2; 
      universe[i+5][j+2]= 4; 
      universe[i+6][j+2]= 5; 
      universe[i+7][j+2]= 5; 
      universe[i+8][j+2]= 5; 
      universe[i+9][j+2]= 5; 
      universe[i+10][j+2]= 5; 
      universe[i+11][j+2]= 5; 
      universe[i+12][j+2]= 5; 
      universe[i+13][j+2]= 3; 
      universe[i+14][j+2]= 1; 
      universe[i+15][j+2]= 1; 
      universe[i+16][j+2]= 1; 
      universe[i+17][j+2]= 1; 
      universe[i+18][j+2]= 1; 
      universe[i+19][j+2]= 1; 
      universe[i+20][j+2]= 1; 
      universe[i+21][j+2]= 1; 
      universe[i+22][j+2]= 1; 
      universe[i+23][j+2]= 1; 
      universe[i+24][j+2]= 1; 
      universe[i+25][j+2]= 1; 
      universe[i+26][j+2]= 1; 
      universe[i+27][j+2]= 1; 
      universe[i+28][j+2]= 1; 
      universe[i+29][j+2]= 1; 
      universe[i+30][j+2]= 1; 
      universe[i+31][j+2]= 4; 
      universe[i+32][j+2]= 5; 
      universe[i+33][j+2]= 4; 
      universe[i+34][j+2]= 2; 
      universe[i+35][j+2]= 2; 
      universe[i+36][j+2]= 2; 
      universe[i+37][j+2]= 2; 
      universe[i+38][j+2]= 2; 
      universe[i+39][j+2]= 2; 
      universe[i+40][j+2]= 2; 
      universe[i+41][j+2]= 4; 
      universe[i+42][j+2]= 5; 
      universe[i+43][j+2]= 4; 
      universe[i+44][j+2]= 1; 
      universe[i+45][j+2]= 1; 
      universe[i+46][j+2]= 1; 
      universe[i+47][j+2]= 1; 
      universe[i+48][j+2]= 1; 
      universe[i+49][j+2]= 1; 
      universe[i+50][j+2]= 1; 
      universe[i+51][j+2]= 1; 
      universe[i+52][j+2]= 1; 
      universe[i+53][j+2]= 1; 
      universe[i+54][j+2]= 1; 
      universe[i+55][j+2]= 1; 
      universe[i+56][j+2]= 1; 
      universe[i+57][j+2]= 1; 
      universe[i+58][j+2]= 1; 
      universe[i+59][j+2]= 1; 
      universe[i+60][j+2]= 1; 
      universe[i+61][j+2]= 3; 
      universe[i+62][j+2]= 5; 
      universe[i+63][j+2]= 5; 
      universe[i+64][j+2]= 4; 
      universe[i+65][j+2]= 2; 
      universe[i+66][j+2]= 2; 
      universe[i+67][j+2]= 2; 
      universe[i+68][j+2]= 2; 
      universe[i+69][j+2]= 2; 
      universe[i+70][j+2]= 4; 
      universe[i+71][j+2]= 5; 
      universe[i+72][j+2]= 5; 
      universe[i+73][j+2]= 5; 
      universe[i+74][j+2]= 5; 
      universe[i+0][j+3]= 5; 
      universe[i+1][j+3]= 4; 
      universe[i+2][j+3]= 2; 
      universe[i+3][j+3]= 2; 
      universe[i+4][j+3]= 2; 
      universe[i+5][j+3]= 2; 
      universe[i+6][j+3]= 2; 
      universe[i+7][j+3]= 4; 
      universe[i+8][j+3]= 5; 
      universe[i+9][j+3]= 5; 
      universe[i+10][j+3]= 5; 
      universe[i+11][j+3]= 5; 
      universe[i+12][j+3]= 5; 
      universe[i+13][j+3]= 5; 
      universe[i+14][j+3]= 5; 
      universe[i+15][j+3]= 3; 
      universe[i+16][j+3]= 1; 
      universe[i+17][j+3]= 1; 
      universe[i+18][j+3]= 1; 
      universe[i+19][j+3]= 1; 
      universe[i+20][j+3]= 1; 
      universe[i+21][j+3]= 1; 
      universe[i+22][j+3]= 1; 
      universe[i+23][j+3]= 1; 
      universe[i+24][j+3]= 1; 
      universe[i+25][j+3]= 1; 
      universe[i+26][j+3]= 1; 
      universe[i+27][j+3]= 1; 
      universe[i+28][j+3]= 1; 
      universe[i+29][j+3]= 1; 
      universe[i+30][j+3]= 1; 
      universe[i+31][j+3]= 4; 
      universe[i+32][j+3]= 5; 
      universe[i+33][j+3]= 4; 
      universe[i+34][j+3]= 2; 
      universe[i+35][j+3]= 2; 
      universe[i+36][j+3]= 2; 
      universe[i+37][j+3]= 2; 
      universe[i+38][j+3]= 2; 
      universe[i+39][j+3]= 2; 
      universe[i+40][j+3]= 2; 
      universe[i+41][j+3]= 4; 
      universe[i+42][j+3]= 5; 
      universe[i+43][j+3]= 4; 
      universe[i+44][j+3]= 1; 
      universe[i+45][j+3]= 1; 
      universe[i+46][j+3]= 1; 
      universe[i+47][j+3]= 1; 
      universe[i+48][j+3]= 1; 
      universe[i+49][j+3]= 1; 
      universe[i+50][j+3]= 1; 
      universe[i+51][j+3]= 1; 
      universe[i+52][j+3]= 1; 
      universe[i+53][j+3]= 1; 
      universe[i+54][j+3]= 1; 
      universe[i+55][j+3]= 1; 
      universe[i+56][j+3]= 1; 
      universe[i+57][j+3]= 1; 
      universe[i+58][j+3]= 1; 
      universe[i+59][j+3]= 3; 
      universe[i+60][j+3]= 5; 
      universe[i+61][j+3]= 5; 
      universe[i+62][j+3]= 4; 
      universe[i+63][j+3]= 2; 
      universe[i+64][j+3]= 2; 
      universe[i+65][j+3]= 2; 
      universe[i+66][j+3]= 2; 
      universe[i+67][j+3]= 2; 
      universe[i+68][j+3]= 4; 
      universe[i+69][j+3]= 5; 
      universe[i+70][j+3]= 5; 
      universe[i+71][j+3]= 5; 
      universe[i+72][j+3]= 5; 
      universe[i+73][j+3]= 5; 
      universe[i+74][j+3]= 5; 
      universe[i+0][j+4]= 4; 
      universe[i+1][j+4]= 5; 
      universe[i+2][j+4]= 5; 
      universe[i+3][j+4]= 4; 
      universe[i+4][j+4]= 2; 
      universe[i+5][j+4]= 2; 
      universe[i+6][j+4]= 2; 
      universe[i+7][j+4]= 2; 
      universe[i+8][j+4]= 2; 
      universe[i+9][j+4]= 4; 
      universe[i+10][j+4]= 5; 
      universe[i+11][j+4]= 5; 
      universe[i+12][j+4]= 5; 
      universe[i+13][j+4]= 5; 
      universe[i+14][j+4]= 5; 
      universe[i+15][j+4]= 5; 
      universe[i+16][j+4]= 5; 
      universe[i+17][j+4]= 3; 
      universe[i+18][j+4]= 1; 
      universe[i+19][j+4]= 1; 
      universe[i+20][j+4]= 1; 
      universe[i+21][j+4]= 1; 
      universe[i+22][j+4]= 1; 
      universe[i+23][j+4]= 1; 
      universe[i+24][j+4]= 1; 
      universe[i+25][j+4]= 1; 
      universe[i+26][j+4]= 1; 
      universe[i+27][j+4]= 1; 
      universe[i+28][j+4]= 1; 
      universe[i+29][j+4]= 1; 
      universe[i+30][j+4]= 1; 
      universe[i+31][j+4]= 4; 
      universe[i+32][j+4]= 5; 
      universe[i+33][j+4]= 4; 
      universe[i+34][j+4]= 2; 
      universe[i+35][j+4]= 2; 
      universe[i+36][j+4]= 2; 
      universe[i+37][j+4]= 2; 
      universe[i+38][j+4]= 2; 
      universe[i+39][j+4]= 2; 
      universe[i+40][j+4]= 2; 
      universe[i+41][j+4]= 4; 
      universe[i+42][j+4]= 5; 
      universe[i+43][j+4]= 4; 
      universe[i+44][j+4]= 1; 
      universe[i+45][j+4]= 1; 
      universe[i+46][j+4]= 1; 
      universe[i+47][j+4]= 1; 
      universe[i+48][j+4]= 1; 
      universe[i+49][j+4]= 1; 
      universe[i+50][j+4]= 1; 
      universe[i+51][j+4]= 1; 
      universe[i+52][j+4]= 1; 
      universe[i+53][j+4]= 1; 
      universe[i+54][j+4]= 1; 
      universe[i+55][j+4]= 1; 
      universe[i+56][j+4]= 1; 
      universe[i+57][j+4]= 3; 
      universe[i+58][j+4]= 5; 
      universe[i+59][j+4]= 5; 
      universe[i+60][j+4]= 4; 
      universe[i+61][j+4]= 2; 
      universe[i+62][j+4]= 2; 
      universe[i+63][j+4]= 2; 
      universe[i+64][j+4]= 2; 
      universe[i+65][j+4]= 2; 
      universe[i+66][j+4]= 4; 
      universe[i+67][j+4]= 5; 
      universe[i+68][j+4]= 5; 
      universe[i+69][j+4]= 5; 
      universe[i+70][j+4]= 5; 
      universe[i+71][j+4]= 5; 
      universe[i+72][j+4]= 5; 
      universe[i+73][j+4]= 5; 
      universe[i+74][j+4]= 4; 
      universe[i+0][j+5]= 1; 
      universe[i+1][j+5]= 1; 
      universe[i+2][j+5]= 4; 
      universe[i+3][j+5]= 5; 
      universe[i+4][j+5]= 5; 
      universe[i+5][j+5]= 4; 
      universe[i+6][j+5]= 2; 
      universe[i+7][j+5]= 2; 
      universe[i+8][j+5]= 2; 
      universe[i+9][j+5]= 2; 
      universe[i+10][j+5]= 2; 
      universe[i+11][j+5]= 4; 
      universe[i+12][j+5]= 5; 
      universe[i+13][j+5]= 5; 
      universe[i+14][j+5]= 5; 
      universe[i+15][j+5]= 5; 
      universe[i+16][j+5]= 5; 
      universe[i+17][j+5]= 5; 
      universe[i+18][j+5]= 5; 
      universe[i+19][j+5]= 3; 
      universe[i+20][j+5]= 1; 
      universe[i+21][j+5]= 1; 
      universe[i+22][j+5]= 1; 
      universe[i+23][j+5]= 1; 
      universe[i+24][j+5]= 1; 
      universe[i+25][j+5]= 1; 
      universe[i+26][j+5]= 1; 
      universe[i+27][j+5]= 1; 
      universe[i+28][j+5]= 1; 
      universe[i+29][j+5]= 1; 
      universe[i+30][j+5]= 1; 
      universe[i+31][j+5]= 4; 
      universe[i+32][j+5]= 5; 
      universe[i+33][j+5]= 4; 
      universe[i+34][j+5]= 2; 
      universe[i+35][j+5]= 2; 
      universe[i+36][j+5]= 2; 
      universe[i+37][j+5]= 2; 
      universe[i+38][j+5]= 2; 
      universe[i+39][j+5]= 2; 
      universe[i+40][j+5]= 2; 
      universe[i+41][j+5]= 4; 
      universe[i+42][j+5]= 5; 
      universe[i+43][j+5]= 4; 
      universe[i+44][j+5]= 1; 
      universe[i+45][j+5]= 1; 
      universe[i+46][j+5]= 1; 
      universe[i+47][j+5]= 1; 
      universe[i+48][j+5]= 1; 
      universe[i+49][j+5]= 1; 
      universe[i+50][j+5]= 1; 
      universe[i+51][j+5]= 1; 
      universe[i+52][j+5]= 1; 
      universe[i+53][j+5]= 1; 
      universe[i+54][j+5]= 1; 
      universe[i+55][j+5]= 3; 
      universe[i+56][j+5]= 5; 
      universe[i+57][j+5]= 5; 
      universe[i+58][j+5]= 4; 
      universe[i+59][j+5]= 2; 
      universe[i+60][j+5]= 2; 
      universe[i+61][j+5]= 2; 
      universe[i+62][j+5]= 2; 
      universe[i+63][j+5]= 2; 
      universe[i+64][j+5]= 4; 
      universe[i+65][j+5]= 5; 
      universe[i+66][j+5]= 5; 
      universe[i+67][j+5]= 5; 
      universe[i+68][j+5]= 5; 
      universe[i+69][j+5]= 5; 
      universe[i+70][j+5]= 5; 
      universe[i+71][j+5]= 5; 
      universe[i+72][j+5]= 4; 
      universe[i+73][j+5]= 1; 
      universe[i+74][j+5]= 1; 
      universe[i+0][j+6]= 1; 
      universe[i+1][j+6]= 1; 
      universe[i+2][j+6]= 1; 
      universe[i+3][j+6]= 1; 
      universe[i+4][j+6]= 4; 
      universe[i+5][j+6]= 5; 
      universe[i+6][j+6]= 5; 
      universe[i+7][j+6]= 4; 
      universe[i+8][j+6]= 2; 
      universe[i+9][j+6]= 2; 
      universe[i+10][j+6]= 2; 
      universe[i+11][j+6]= 2; 
      universe[i+12][j+6]= 2; 
      universe[i+13][j+6]= 4; 
      universe[i+14][j+6]= 5; 
      universe[i+15][j+6]= 5; 
      universe[i+16][j+6]= 5; 
      universe[i+17][j+6]= 5; 
      universe[i+18][j+6]= 5; 
      universe[i+19][j+6]= 5; 
      universe[i+20][j+6]= 5; 
      universe[i+21][j+6]= 3; 
      universe[i+22][j+6]= 1; 
      universe[i+23][j+6]= 1; 
      universe[i+24][j+6]= 1; 
      universe[i+25][j+6]= 1; 
      universe[i+26][j+6]= 1; 
      universe[i+27][j+6]= 1; 
      universe[i+28][j+6]= 1; 
      universe[i+29][j+6]= 1; 
      universe[i+30][j+6]= 1; 
      universe[i+31][j+6]= 4; 
      universe[i+32][j+6]= 5; 
      universe[i+33][j+6]= 4; 
      universe[i+34][j+6]= 2; 
      universe[i+35][j+6]= 2; 
      universe[i+36][j+6]= 2; 
      universe[i+37][j+6]= 2; 
      universe[i+38][j+6]= 2; 
      universe[i+39][j+6]= 2; 
      universe[i+40][j+6]= 2; 
      universe[i+41][j+6]= 4; 
      universe[i+42][j+6]= 5; 
      universe[i+43][j+6]= 4; 
      universe[i+44][j+6]= 1; 
      universe[i+45][j+6]= 1; 
      universe[i+46][j+6]= 1; 
      universe[i+47][j+6]= 1; 
      universe[i+48][j+6]= 1; 
      universe[i+49][j+6]= 1; 
      universe[i+50][j+6]= 1; 
      universe[i+51][j+6]= 1; 
      universe[i+52][j+6]= 1; 
      universe[i+53][j+6]= 3; 
      universe[i+54][j+6]= 5; 
      universe[i+55][j+6]= 5; 
      universe[i+56][j+6]= 4; 
      universe[i+57][j+6]= 2; 
      universe[i+58][j+6]= 2; 
      universe[i+59][j+6]= 2; 
      universe[i+60][j+6]= 2; 
      universe[i+61][j+6]= 2; 
      universe[i+62][j+6]= 4; 
      universe[i+63][j+6]= 5; 
      universe[i+64][j+6]= 5; 
      universe[i+65][j+6]= 5; 
      universe[i+66][j+6]= 5; 
      universe[i+67][j+6]= 5; 
      universe[i+68][j+6]= 5; 
      universe[i+69][j+6]= 5; 
      universe[i+70][j+6]= 4; 
      universe[i+71][j+6]= 1; 
      universe[i+72][j+6]= 1; 
      universe[i+73][j+6]= 1; 
      universe[i+74][j+6]= 1; 
      universe[i+0][j+7]= 1; 
      universe[i+1][j+7]= 1; 
      universe[i+2][j+7]= 1; 
      universe[i+3][j+7]= 1; 
      universe[i+4][j+7]= 1; 
      universe[i+5][j+7]= 1; 
      universe[i+6][j+7]= 4; 
      universe[i+7][j+7]= 5; 
      universe[i+8][j+7]= 5; 
      universe[i+9][j+7]= 4; 
      universe[i+10][j+7]= 2; 
      universe[i+11][j+7]= 2; 
      universe[i+12][j+7]= 2; 
      universe[i+13][j+7]= 2; 
      universe[i+14][j+7]= 4; 
      universe[i+15][j+7]= 5; 
      universe[i+16][j+7]= 5; 
      universe[i+17][j+7]= 5; 
      universe[i+18][j+7]= 5; 
      universe[i+19][j+7]= 5; 
      universe[i+20][j+7]= 5; 
      universe[i+21][j+7]= 5; 
      universe[i+22][j+7]= 5; 
      universe[i+23][j+7]= 3; 
      universe[i+24][j+7]= 1; 
      universe[i+25][j+7]= 1; 
      universe[i+26][j+7]= 1; 
      universe[i+27][j+7]= 1; 
      universe[i+28][j+7]= 1; 
      universe[i+29][j+7]= 1; 
      universe[i+30][j+7]= 1; 
      universe[i+31][j+7]= 4; 
      universe[i+32][j+7]= 5; 
      universe[i+33][j+7]= 4; 
      universe[i+34][j+7]= 2; 
      universe[i+35][j+7]= 2; 
      universe[i+36][j+7]= 2; 
      universe[i+37][j+7]= 2; 
      universe[i+38][j+7]= 2; 
      universe[i+39][j+7]= 2; 
      universe[i+40][j+7]= 2; 
      universe[i+41][j+7]= 4; 
      universe[i+42][j+7]= 5; 
      universe[i+43][j+7]= 4; 
      universe[i+44][j+7]= 1; 
      universe[i+45][j+7]= 1; 
      universe[i+46][j+7]= 1; 
      universe[i+47][j+7]= 1; 
      universe[i+48][j+7]= 1; 
      universe[i+49][j+7]= 1; 
      universe[i+50][j+7]= 1; 
      universe[i+51][j+7]= 3; 
      universe[i+52][j+7]= 5; 
      universe[i+53][j+7]= 5; 
      universe[i+54][j+7]= 4; 
      universe[i+55][j+7]= 2; 
      universe[i+56][j+7]= 2; 
      universe[i+57][j+7]= 2; 
      universe[i+58][j+7]= 2; 
      universe[i+59][j+7]= 2; 
      universe[i+60][j+7]= 4; 
      universe[i+61][j+7]= 5; 
      universe[i+62][j+7]= 5; 
      universe[i+63][j+7]= 5; 
      universe[i+64][j+7]= 5; 
      universe[i+65][j+7]= 5; 
      universe[i+66][j+7]= 5; 
      universe[i+67][j+7]= 5; 
      universe[i+68][j+7]= 4; 
      universe[i+69][j+7]= 1; 
      universe[i+70][j+7]= 1; 
      universe[i+71][j+7]= 1; 
      universe[i+72][j+7]= 1; 
      universe[i+73][j+7]= 1; 
      universe[i+74][j+7]= 1; 
      universe[i+0][j+8]= 1; 
      universe[i+1][j+8]= 1; 
      universe[i+2][j+8]= 1; 
      universe[i+3][j+8]= 1; 
      universe[i+4][j+8]= 1; 
      universe[i+5][j+8]= 1; 
      universe[i+6][j+8]= 1; 
      universe[i+7][j+8]= 1; 
      universe[i+8][j+8]= 4; 
      universe[i+9][j+8]= 5; 
      universe[i+10][j+8]= 5; 
      universe[i+11][j+8]= 4; 
      universe[i+12][j+8]= 2; 
      universe[i+13][j+8]= 2; 
      universe[i+14][j+8]= 2; 
      universe[i+15][j+8]= 2; 
      universe[i+16][j+8]= 4; 
      universe[i+17][j+8]= 5; 
      universe[i+18][j+8]= 5; 
      universe[i+19][j+8]= 5; 
      universe[i+20][j+8]= 5; 
      universe[i+21][j+8]= 5; 
      universe[i+22][j+8]= 5; 
      universe[i+23][j+8]= 5; 
      universe[i+24][j+8]= 5; 
      universe[i+25][j+8]= 3; 
      universe[i+26][j+8]= 1; 
      universe[i+27][j+8]= 1; 
      universe[i+28][j+8]= 1; 
      universe[i+29][j+8]= 1; 
      universe[i+30][j+8]= 1; 
      universe[i+31][j+8]= 4; 
      universe[i+32][j+8]= 5; 
      universe[i+33][j+8]= 4; 
      universe[i+34][j+8]= 2; 
      universe[i+35][j+8]= 2; 
      universe[i+36][j+8]= 2; 
      universe[i+37][j+8]= 2; 
      universe[i+38][j+8]= 2; 
      universe[i+39][j+8]= 2; 
      universe[i+40][j+8]= 2; 
      universe[i+41][j+8]= 4; 
      universe[i+42][j+8]= 5; 
      universe[i+43][j+8]= 4; 
      universe[i+44][j+8]= 1; 
      universe[i+45][j+8]= 1; 
      universe[i+46][j+8]= 1; 
      universe[i+47][j+8]= 1; 
      universe[i+48][j+8]= 1; 
      universe[i+49][j+8]= 3; 
      universe[i+50][j+8]= 5; 
      universe[i+51][j+8]= 5; 
      universe[i+52][j+8]= 4; 
      universe[i+53][j+8]= 2; 
      universe[i+54][j+8]= 2; 
      universe[i+55][j+8]= 2; 
      universe[i+56][j+8]= 2; 
      universe[i+57][j+8]= 2; 
      universe[i+58][j+8]= 4; 
      universe[i+59][j+8]= 5; 
      universe[i+60][j+8]= 5; 
      universe[i+61][j+8]= 5; 
      universe[i+62][j+8]= 5; 
      universe[i+63][j+8]= 5; 
      universe[i+64][j+8]= 5; 
      universe[i+65][j+8]= 5; 
      universe[i+66][j+8]= 4; 
      universe[i+67][j+8]= 1; 
      universe[i+68][j+8]= 1; 
      universe[i+69][j+8]= 1; 
      universe[i+70][j+8]= 1; 
      universe[i+71][j+8]= 1; 
      universe[i+72][j+8]= 1; 
      universe[i+73][j+8]= 1; 
      universe[i+74][j+8]= 1; 
      universe[i+0][j+9]= 1; 
      universe[i+1][j+9]= 1; 
      universe[i+2][j+9]= 1; 
      universe[i+3][j+9]= 1; 
      universe[i+4][j+9]= 1; 
      universe[i+5][j+9]= 1; 
      universe[i+6][j+9]= 1; 
      universe[i+7][j+9]= 1; 
      universe[i+8][j+9]= 1; 
      universe[i+9][j+9]= 1; 
      universe[i+10][j+9]= 4; 
      universe[i+11][j+9]= 5; 
      universe[i+12][j+9]= 5; 
      universe[i+13][j+9]= 4; 
      universe[i+14][j+9]= 2; 
      universe[i+15][j+9]= 2; 
      universe[i+16][j+9]= 2; 
      universe[i+17][j+9]= 2; 
      universe[i+18][j+9]= 4; 
      universe[i+19][j+9]= 5; 
      universe[i+20][j+9]= 5; 
      universe[i+21][j+9]= 5; 
      universe[i+22][j+9]= 5; 
      universe[i+23][j+9]= 5; 
      universe[i+24][j+9]= 5; 
      universe[i+25][j+9]= 5; 
      universe[i+26][j+9]= 5; 
      universe[i+27][j+9]= 3; 
      universe[i+28][j+9]= 1; 
      universe[i+29][j+9]= 1; 
      universe[i+30][j+9]= 1; 
      universe[i+31][j+9]= 4; 
      universe[i+32][j+9]= 5; 
      universe[i+33][j+9]= 4; 
      universe[i+34][j+9]= 2; 
      universe[i+35][j+9]= 2; 
      universe[i+36][j+9]= 2; 
      universe[i+37][j+9]= 2; 
      universe[i+38][j+9]= 2; 
      universe[i+39][j+9]= 2; 
      universe[i+40][j+9]= 2; 
      universe[i+41][j+9]= 4; 
      universe[i+42][j+9]= 5; 
      universe[i+43][j+9]= 4; 
      universe[i+44][j+9]= 1; 
      universe[i+45][j+9]= 1; 
      universe[i+46][j+9]= 1; 
      universe[i+47][j+9]= 3; 
      universe[i+48][j+9]= 5; 
      universe[i+49][j+9]= 5; 
      universe[i+50][j+9]= 4; 
      universe[i+51][j+9]= 2; 
      universe[i+52][j+9]= 2; 
      universe[i+53][j+9]= 2; 
      universe[i+54][j+9]= 2; 
      universe[i+55][j+9]= 2; 
      universe[i+56][j+9]= 4; 
      universe[i+57][j+9]= 5; 
      universe[i+58][j+9]= 5; 
      universe[i+59][j+9]= 5; 
      universe[i+60][j+9]= 5; 
      universe[i+61][j+9]= 5; 
      universe[i+62][j+9]= 5; 
      universe[i+63][j+9]= 5; 
      universe[i+64][j+9]= 4; 
      universe[i+65][j+9]= 1; 
      universe[i+66][j+9]= 1; 
      universe[i+67][j+9]= 1; 
      universe[i+68][j+9]= 1; 
      universe[i+69][j+9]= 1; 
      universe[i+70][j+9]= 1; 
      universe[i+71][j+9]= 1; 
      universe[i+72][j+9]= 1; 
      universe[i+73][j+9]= 1; 
      universe[i+74][j+9]= 1; 
      universe[i+0][j+10]= 1; 
      universe[i+1][j+10]= 1; 
      universe[i+2][j+10]= 1; 
      universe[i+3][j+10]= 1; 
      universe[i+4][j+10]= 1; 
      universe[i+5][j+10]= 1; 
      universe[i+6][j+10]= 1; 
      universe[i+7][j+10]= 1; 
      universe[i+8][j+10]= 1; 
      universe[i+9][j+10]= 1; 
      universe[i+10][j+10]= 1; 
      universe[i+11][j+10]= 1; 
      universe[i+12][j+10]= 4; 
      universe[i+13][j+10]= 5; 
      universe[i+14][j+10]= 5; 
      universe[i+15][j+10]= 2; 
      universe[i+16][j+10]= 2; 
      universe[i+17][j+10]= 2; 
      universe[i+18][j+10]= 2; 
      universe[i+19][j+10]= 2; 
      universe[i+20][j+10]= 4; 
      universe[i+21][j+10]= 5; 
      universe[i+22][j+10]= 5; 
      universe[i+23][j+10]= 5; 
      universe[i+24][j+10]= 5; 
      universe[i+25][j+10]= 5; 
      universe[i+26][j+10]= 5; 
      universe[i+27][j+10]= 5; 
      universe[i+28][j+10]= 5; 
      universe[i+29][j+10]= 3; 
      universe[i+30][j+10]= 1; 
      universe[i+31][j+10]= 4; 
      universe[i+32][j+10]= 5; 
      universe[i+33][j+10]= 4; 
      universe[i+34][j+10]= 2; 
      universe[i+35][j+10]= 2; 
      universe[i+36][j+10]= 2; 
      universe[i+37][j+10]= 2; 
      universe[i+38][j+10]= 2; 
      universe[i+39][j+10]= 2; 
      universe[i+40][j+10]= 2; 
      universe[i+41][j+10]= 4; 
      universe[i+42][j+10]= 5; 
      universe[i+43][j+10]= 4; 
      universe[i+44][j+10]= 1; 
      universe[i+45][j+10]= 3; 
      universe[i+46][j+10]= 5; 
      universe[i+47][j+10]= 5; 
      universe[i+48][j+10]= 4; 
      universe[i+49][j+10]= 2; 
      universe[i+50][j+10]= 2; 
      universe[i+51][j+10]= 2; 
      universe[i+52][j+10]= 2; 
      universe[i+53][j+10]= 2; 
      universe[i+54][j+10]= 4; 
      universe[i+55][j+10]= 5; 
      universe[i+56][j+10]= 5; 
      universe[i+57][j+10]= 5; 
      universe[i+58][j+10]= 5; 
      universe[i+59][j+10]= 5; 
      universe[i+60][j+10]= 5; 
      universe[i+61][j+10]= 5; 
      universe[i+62][j+10]= 4; 
      universe[i+63][j+10]= 1; 
      universe[i+64][j+10]= 1; 
      universe[i+65][j+10]= 1; 
      universe[i+66][j+10]= 1; 
      universe[i+67][j+10]= 1; 
      universe[i+68][j+10]= 1; 
      universe[i+69][j+10]= 1; 
      universe[i+70][j+10]= 1; 
      universe[i+71][j+10]= 1; 
      universe[i+72][j+10]= 1; 
      universe[i+73][j+10]= 1; 
      universe[i+74][j+10]= 1; 
      universe[i+0][j+11]= 1; 
      universe[i+1][j+11]= 1; 
      universe[i+2][j+11]= 1; 
      universe[i+3][j+11]= 1; 
      universe[i+4][j+11]= 1; 
      universe[i+5][j+11]= 1; 
      universe[i+6][j+11]= 1; 
      universe[i+7][j+11]= 1; 
      universe[i+8][j+11]= 1; 
      universe[i+9][j+11]= 1; 
      universe[i+10][j+11]= 1; 
      universe[i+11][j+11]= 1; 
      universe[i+12][j+11]= 1; 
      universe[i+13][j+11]= 1; 
      universe[i+14][j+11]= 4; 
      universe[i+15][j+11]= 5; 
      universe[i+16][j+11]= 4; 
      universe[i+17][j+11]= 2; 
      universe[i+18][j+11]= 2; 
      universe[i+19][j+11]= 2; 
      universe[i+20][j+11]= 2; 
      universe[i+21][j+11]= 2; 
      universe[i+22][j+11]= 4; 
      universe[i+23][j+11]= 5; 
      universe[i+24][j+11]= 5; 
      universe[i+25][j+11]= 5; 
      universe[i+26][j+11]= 5; 
      universe[i+27][j+11]= 5; 
      universe[i+28][j+11]= 5; 
      universe[i+29][j+11]= 5; 
      universe[i+30][j+11]= 5; 
      universe[i+31][j+11]= 5; 
      universe[i+32][j+11]= 5; 
      universe[i+33][j+11]= 4; 
      universe[i+34][j+11]= 2; 
      universe[i+35][j+11]= 2; 
      universe[i+36][j+11]= 2; 
      universe[i+37][j+11]= 2; 
      universe[i+38][j+11]= 2; 
      universe[i+39][j+11]= 2; 
      universe[i+40][j+11]= 2; 
      universe[i+41][j+11]= 4; 
      universe[i+42][j+11]= 5; 
      universe[i+43][j+11]= 5; 
      universe[i+44][j+11]= 5; 
      universe[i+45][j+11]= 5; 
      universe[i+46][j+11]= 4; 
      universe[i+47][j+11]= 2; 
      universe[i+48][j+11]= 2; 
      universe[i+49][j+11]= 2; 
      universe[i+50][j+11]= 2; 
      universe[i+51][j+11]= 2; 
      universe[i+52][j+11]= 4; 
      universe[i+53][j+11]= 5; 
      universe[i+54][j+11]= 5; 
      universe[i+55][j+11]= 5; 
      universe[i+56][j+11]= 5; 
      universe[i+57][j+11]= 5; 
      universe[i+58][j+11]= 5; 
      universe[i+59][j+11]= 5; 
      universe[i+60][j+11]= 4; 
      universe[i+61][j+11]= 1; 
      universe[i+62][j+11]= 1; 
      universe[i+63][j+11]= 1; 
      universe[i+64][j+11]= 1; 
      universe[i+65][j+11]= 1; 
      universe[i+66][j+11]= 1; 
      universe[i+67][j+11]= 1; 
      universe[i+68][j+11]= 1; 
      universe[i+69][j+11]= 1; 
      universe[i+70][j+11]= 1; 
      universe[i+71][j+11]= 1; 
      universe[i+72][j+11]= 1; 
      universe[i+73][j+11]= 1; 
      universe[i+74][j+11]= 1; 
      universe[i+0][j+12]= 3; 
      universe[i+1][j+12]= 3; 
      universe[i+2][j+12]= 3; 
      universe[i+3][j+12]= 3; 
      universe[i+4][j+12]= 3; 
      universe[i+5][j+12]= 3; 
      universe[i+6][j+12]= 3; 
      universe[i+7][j+12]= 3; 
      universe[i+8][j+12]= 3; 
      universe[i+9][j+12]= 3; 
      universe[i+10][j+12]= 3; 
      universe[i+11][j+12]= 3; 
      universe[i+12][j+12]= 3; 
      universe[i+13][j+12]= 3; 
      universe[i+14][j+12]= 3; 
      universe[i+15][j+12]= 3; 
      universe[i+16][j+12]= 5; 
      universe[i+17][j+12]= 5; 
      universe[i+18][j+12]= 4; 
      universe[i+19][j+12]= 4; 
      universe[i+20][j+12]= 2; 
      universe[i+21][j+12]= 2; 
      universe[i+22][j+12]= 2; 
      universe[i+23][j+12]= 2; 
      universe[i+24][j+12]= 4; 
      universe[i+25][j+12]= 5; 
      universe[i+26][j+12]= 5; 
      universe[i+27][j+12]= 5; 
      universe[i+28][j+12]= 5; 
      universe[i+29][j+12]= 5; 
      universe[i+30][j+12]= 5; 
      universe[i+31][j+12]= 5; 
      universe[i+32][j+12]= 5; 
      universe[i+33][j+12]= 4; 
      universe[i+34][j+12]= 2; 
      universe[i+35][j+12]= 2; 
      universe[i+36][j+12]= 2; 
      universe[i+37][j+12]= 2; 
      universe[i+38][j+12]= 2; 
      universe[i+39][j+12]= 2; 
      universe[i+40][j+12]= 2; 
      universe[i+41][j+12]= 4; 
      universe[i+42][j+12]= 5; 
      universe[i+43][j+12]= 5; 
      universe[i+44][j+12]= 4; 
      universe[i+45][j+12]= 4; 
      universe[i+46][j+12]= 2; 
      universe[i+47][j+12]= 2; 
      universe[i+48][j+12]= 2; 
      universe[i+49][j+12]= 2; 
      universe[i+50][j+12]= 4; 
      universe[i+51][j+12]= 5; 
      universe[i+52][j+12]= 5; 
      universe[i+53][j+12]= 5; 
      universe[i+54][j+12]= 5; 
      universe[i+55][j+12]= 5; 
      universe[i+56][j+12]= 5; 
      universe[i+57][j+12]= 5; 
      universe[i+58][j+12]= 5; 
      universe[i+59][j+12]= 3; 
      universe[i+60][j+12]= 3; 
      universe[i+61][j+12]= 3; 
      universe[i+62][j+12]= 3; 
      universe[i+63][j+12]= 3; 
      universe[i+64][j+12]= 3; 
      universe[i+65][j+12]= 3; 
      universe[i+66][j+12]= 3; 
      universe[i+67][j+12]= 3; 
      universe[i+68][j+12]= 3; 
      universe[i+69][j+12]= 3; 
      universe[i+70][j+12]= 3; 
      universe[i+71][j+12]= 3; 
      universe[i+72][j+12]= 3; 
      universe[i+73][j+12]= 3; 
      universe[i+74][j+12]= 3; 
      universe[i+0][j+13]= 5; 
      universe[i+1][j+13]= 5; 
      universe[i+2][j+13]= 5; 
      universe[i+3][j+13]= 5; 
      universe[i+4][j+13]= 5; 
      universe[i+5][j+13]= 5; 
      universe[i+6][j+13]= 5; 
      universe[i+7][j+13]= 5; 
      universe[i+8][j+13]= 5; 
      universe[i+9][j+13]= 5; 
      universe[i+10][j+13]= 5; 
      universe[i+11][j+13]= 5; 
      universe[i+12][j+13]= 5; 
      universe[i+13][j+13]= 5; 
      universe[i+14][j+13]= 5; 
      universe[i+15][j+13]= 5; 
      universe[i+16][j+13]= 5; 
      universe[i+17][j+13]= 5; 
      universe[i+18][j+13]= 5; 
      universe[i+19][j+13]= 5; 
      universe[i+20][j+13]= 5; 
      universe[i+21][j+13]= 5; 
      universe[i+22][j+13]= 5; 
      universe[i+23][j+13]= 5; 
      universe[i+24][j+13]= 5; 
      universe[i+25][j+13]= 5; 
      universe[i+26][j+13]= 5; 
      universe[i+27][j+13]= 5; 
      universe[i+28][j+13]= 5; 
      universe[i+29][j+13]= 5; 
      universe[i+30][j+13]= 5; 
      universe[i+31][j+13]= 5; 
      universe[i+32][j+13]= 5; 
      universe[i+33][j+13]= 4; 
      universe[i+34][j+13]= 2; 
      universe[i+35][j+13]= 2; 
      universe[i+36][j+13]= 2; 
      universe[i+37][j+13]= 2; 
      universe[i+38][j+13]= 2; 
      universe[i+39][j+13]= 2; 
      universe[i+40][j+13]= 2; 
      universe[i+41][j+13]= 4; 
      universe[i+42][j+13]= 5; 
      universe[i+43][j+13]= 5; 
      universe[i+44][j+13]= 5; 
      universe[i+45][j+13]= 5; 
      universe[i+46][j+13]= 5; 
      universe[i+47][j+13]= 5; 
      universe[i+48][j+13]= 5; 
      universe[i+49][j+13]= 5; 
      universe[i+50][j+13]= 5; 
      universe[i+51][j+13]= 5; 
      universe[i+52][j+13]= 5; 
      universe[i+53][j+13]= 5; 
      universe[i+54][j+13]= 5; 
      universe[i+55][j+13]= 5; 
      universe[i+56][j+13]= 5; 
      universe[i+57][j+13]= 5; 
      universe[i+58][j+13]= 5; 
      universe[i+59][j+13]= 5; 
      universe[i+60][j+13]= 5; 
      universe[i+61][j+13]= 5; 
      universe[i+62][j+13]= 5; 
      universe[i+63][j+13]= 5; 
      universe[i+64][j+13]= 5; 
      universe[i+65][j+13]= 5; 
      universe[i+66][j+13]= 5; 
      universe[i+67][j+13]= 5; 
      universe[i+68][j+13]= 5; 
      universe[i+69][j+13]= 5; 
      universe[i+70][j+13]= 5; 
      universe[i+71][j+13]= 5; 
      universe[i+72][j+13]= 5; 
      universe[i+73][j+13]= 5; 
      universe[i+74][j+13]= 5; 
      universe[i+0][j+14]= 5; 
      universe[i+1][j+14]= 5; 
      universe[i+2][j+14]= 5; 
      universe[i+3][j+14]= 5; 
      universe[i+4][j+14]= 5; 
      universe[i+5][j+14]= 5; 
      universe[i+6][j+14]= 5; 
      universe[i+7][j+14]= 5; 
      universe[i+8][j+14]= 5; 
      universe[i+9][j+14]= 5; 
      universe[i+10][j+14]= 5; 
      universe[i+11][j+14]= 5; 
      universe[i+12][j+14]= 5; 
      universe[i+13][j+14]= 5; 
      universe[i+14][j+14]= 5; 
      universe[i+15][j+14]= 5; 
      universe[i+16][j+14]= 5; 
      universe[i+17][j+14]= 5; 
      universe[i+18][j+14]= 5; 
      universe[i+19][j+14]= 5; 
      universe[i+20][j+14]= 5; 
      universe[i+21][j+14]= 5; 
      universe[i+22][j+14]= 5; 
      universe[i+23][j+14]= 5; 
      universe[i+24][j+14]= 5; 
      universe[i+25][j+14]= 5; 
      universe[i+26][j+14]= 5; 
      universe[i+27][j+14]= 5; 
      universe[i+28][j+14]= 5; 
      universe[i+29][j+14]= 5; 
      universe[i+30][j+14]= 5; 
      universe[i+31][j+14]= 5; 
      universe[i+32][j+14]= 5; 
      universe[i+33][j+14]= 4; 
      universe[i+34][j+14]= 2; 
      universe[i+35][j+14]= 2; 
      universe[i+36][j+14]= 2; 
      universe[i+37][j+14]= 2; 
      universe[i+38][j+14]= 2; 
      universe[i+39][j+14]= 2; 
      universe[i+40][j+14]= 2; 
      universe[i+41][j+14]= 4; 
      universe[i+42][j+14]= 5; 
      universe[i+43][j+14]= 5; 
      universe[i+44][j+14]= 5; 
      universe[i+45][j+14]= 5; 
      universe[i+46][j+14]= 5; 
      universe[i+47][j+14]= 5; 
      universe[i+48][j+14]= 5; 
      universe[i+49][j+14]= 5; 
      universe[i+50][j+14]= 5; 
      universe[i+51][j+14]= 5; 
      universe[i+52][j+14]= 5; 
      universe[i+53][j+14]= 5; 
      universe[i+54][j+14]= 5; 
      universe[i+55][j+14]= 5; 
      universe[i+56][j+14]= 5; 
      universe[i+57][j+14]= 5; 
      universe[i+58][j+14]= 5; 
      universe[i+59][j+14]= 5; 
      universe[i+60][j+14]= 5; 
      universe[i+61][j+14]= 5; 
      universe[i+62][j+14]= 5; 
      universe[i+63][j+14]= 5; 
      universe[i+64][j+14]= 5; 
      universe[i+65][j+14]= 5; 
      universe[i+66][j+14]= 5; 
      universe[i+67][j+14]= 5; 
      universe[i+68][j+14]= 5; 
      universe[i+69][j+14]= 5; 
      universe[i+70][j+14]= 5; 
      universe[i+71][j+14]= 5; 
      universe[i+72][j+14]= 5; 
      universe[i+73][j+14]= 5; 
      universe[i+74][j+14]= 5; 
      universe[i+0][j+15]= 2; 
      universe[i+1][j+15]= 2; 
      universe[i+2][j+15]= 2; 
      universe[i+3][j+15]= 2; 
      universe[i+4][j+15]= 2; 
      universe[i+5][j+15]= 2; 
      universe[i+6][j+15]= 2; 
      universe[i+7][j+15]= 2; 
      universe[i+8][j+15]= 2; 
      universe[i+9][j+15]= 2; 
      universe[i+10][j+15]= 2; 
      universe[i+11][j+15]= 2; 
      universe[i+12][j+15]= 2; 
      universe[i+13][j+15]= 2; 
      universe[i+14][j+15]= 2; 
      universe[i+15][j+15]= 2; 
      universe[i+16][j+15]= 2; 
      universe[i+17][j+15]= 2; 
      universe[i+18][j+15]= 2; 
      universe[i+19][j+15]= 2; 
      universe[i+20][j+15]= 2; 
      universe[i+21][j+15]= 2; 
      universe[i+22][j+15]= 2; 
      universe[i+23][j+15]= 2; 
      universe[i+24][j+15]= 2; 
      universe[i+25][j+15]= 2; 
      universe[i+26][j+15]= 2; 
      universe[i+27][j+15]= 2; 
      universe[i+28][j+15]= 2; 
      universe[i+29][j+15]= 2; 
      universe[i+30][j+15]= 2; 
      universe[i+31][j+15]= 2; 
      universe[i+32][j+15]= 2; 
      universe[i+33][j+15]= 2; 
      universe[i+34][j+15]= 2; 
      universe[i+35][j+15]= 2; 
      universe[i+36][j+15]= 2; 
      universe[i+37][j+15]= 2; 
      universe[i+38][j+15]= 2; 
      universe[i+39][j+15]= 2; 
      universe[i+40][j+15]= 2; 
      universe[i+41][j+15]= 2; 
      universe[i+42][j+15]= 2; 
      universe[i+43][j+15]= 2; 
      universe[i+44][j+15]= 2; 
      universe[i+45][j+15]= 2; 
      universe[i+46][j+15]= 2; 
      universe[i+47][j+15]= 2; 
      universe[i+48][j+15]= 2; 
      universe[i+49][j+15]= 2; 
      universe[i+50][j+15]= 2; 
      universe[i+51][j+15]= 2; 
      universe[i+52][j+15]= 2; 
      universe[i+53][j+15]= 2; 
      universe[i+54][j+15]= 2; 
      universe[i+55][j+15]= 2; 
      universe[i+56][j+15]= 2; 
      universe[i+57][j+15]= 2; 
      universe[i+58][j+15]= 2; 
      universe[i+59][j+15]= 2; 
      universe[i+60][j+15]= 2; 
      universe[i+61][j+15]= 2; 
      universe[i+62][j+15]= 2; 
      universe[i+63][j+15]= 2; 
      universe[i+64][j+15]= 2; 
      universe[i+65][j+15]= 2; 
      universe[i+66][j+15]= 2; 
      universe[i+67][j+15]= 2; 
      universe[i+68][j+15]= 2; 
      universe[i+69][j+15]= 2; 
      universe[i+70][j+15]= 2; 
      universe[i+71][j+15]= 2; 
      universe[i+72][j+15]= 2; 
      universe[i+73][j+15]= 2; 
      universe[i+74][j+15]= 2; 
      universe[i+0][j+16]= 2; 
      universe[i+1][j+16]= 2; 
      universe[i+2][j+16]= 2; 
      universe[i+3][j+16]= 2; 
      universe[i+4][j+16]= 2; 
      universe[i+5][j+16]= 2; 
      universe[i+6][j+16]= 2; 
      universe[i+7][j+16]= 2; 
      universe[i+8][j+16]= 2; 
      universe[i+9][j+16]= 2; 
      universe[i+10][j+16]= 2; 
      universe[i+11][j+16]= 2; 
      universe[i+12][j+16]= 2; 
      universe[i+13][j+16]= 2; 
      universe[i+14][j+16]= 2; 
      universe[i+15][j+16]= 2; 
      universe[i+16][j+16]= 2; 
      universe[i+17][j+16]= 2; 
      universe[i+18][j+16]= 2; 
      universe[i+19][j+16]= 2; 
      universe[i+20][j+16]= 2; 
      universe[i+21][j+16]= 2; 
      universe[i+22][j+16]= 2; 
      universe[i+23][j+16]= 2; 
      universe[i+24][j+16]= 2; 
      universe[i+25][j+16]= 2; 
      universe[i+26][j+16]= 2; 
      universe[i+27][j+16]= 2; 
      universe[i+28][j+16]= 2; 
      universe[i+29][j+16]= 2; 
      universe[i+30][j+16]= 2; 
      universe[i+31][j+16]= 2; 
      universe[i+32][j+16]= 2; 
      universe[i+33][j+16]= 2; 
      universe[i+34][j+16]= 2; 
      universe[i+35][j+16]= 2; 
      universe[i+36][j+16]= 2; 
      universe[i+37][j+16]= 2; 
      universe[i+38][j+16]= 2; 
      universe[i+39][j+16]= 2; 
      universe[i+40][j+16]= 2; 
      universe[i+41][j+16]= 2; 
      universe[i+42][j+16]= 2; 
      universe[i+43][j+16]= 2; 
      universe[i+44][j+16]= 2; 
      universe[i+45][j+16]= 2; 
      universe[i+46][j+16]= 2; 
      universe[i+47][j+16]= 2; 
      universe[i+48][j+16]= 2; 
      universe[i+49][j+16]= 2; 
      universe[i+50][j+16]= 2; 
      universe[i+51][j+16]= 2; 
      universe[i+52][j+16]= 2; 
      universe[i+53][j+16]= 2; 
      universe[i+54][j+16]= 2; 
      universe[i+55][j+16]= 2; 
      universe[i+56][j+16]= 2; 
      universe[i+57][j+16]= 2; 
      universe[i+58][j+16]= 2; 
      universe[i+59][j+16]= 2; 
      universe[i+60][j+16]= 2; 
      universe[i+61][j+16]= 2; 
      universe[i+62][j+16]= 2; 
      universe[i+63][j+16]= 2; 
      universe[i+64][j+16]= 2; 
      universe[i+65][j+16]= 2; 
      universe[i+66][j+16]= 2; 
      universe[i+67][j+16]= 2; 
      universe[i+68][j+16]= 2; 
      universe[i+69][j+16]= 2; 
      universe[i+70][j+16]= 2; 
      universe[i+71][j+16]= 2; 
      universe[i+72][j+16]= 2; 
      universe[i+73][j+16]= 2; 
      universe[i+74][j+16]= 2; 
      universe[i+0][j+17]= 2; 
      universe[i+1][j+17]= 2; 
      universe[i+2][j+17]= 2; 
      universe[i+3][j+17]= 2; 
      universe[i+4][j+17]= 2; 
      universe[i+5][j+17]= 2; 
      universe[i+6][j+17]= 2; 
      universe[i+7][j+17]= 2; 
      universe[i+8][j+17]= 2; 
      universe[i+9][j+17]= 2; 
      universe[i+10][j+17]= 2; 
      universe[i+11][j+17]= 2; 
      universe[i+12][j+17]= 2; 
      universe[i+13][j+17]= 2; 
      universe[i+14][j+17]= 2; 
      universe[i+15][j+17]= 2; 
      universe[i+16][j+17]= 2; 
      universe[i+17][j+17]= 2; 
      universe[i+18][j+17]= 2; 
      universe[i+19][j+17]= 2; 
      universe[i+20][j+17]= 2; 
      universe[i+21][j+17]= 2; 
      universe[i+22][j+17]= 2; 
      universe[i+23][j+17]= 2; 
      universe[i+24][j+17]= 2; 
      universe[i+25][j+17]= 2; 
      universe[i+26][j+17]= 2; 
      universe[i+27][j+17]= 2; 
      universe[i+28][j+17]= 2; 
      universe[i+29][j+17]= 2; 
      universe[i+30][j+17]= 2; 
      universe[i+31][j+17]= 2; 
      universe[i+32][j+17]= 2; 
      universe[i+33][j+17]= 2; 
      universe[i+34][j+17]= 2; 
      universe[i+35][j+17]= 2; 
      universe[i+36][j+17]= 2; 
      universe[i+37][j+17]= 2; 
      universe[i+38][j+17]= 2; 
      universe[i+39][j+17]= 2; 
      universe[i+40][j+17]= 2; 
      universe[i+41][j+17]= 2; 
      universe[i+42][j+17]= 2; 
      universe[i+43][j+17]= 2; 
      universe[i+44][j+17]= 2; 
      universe[i+45][j+17]= 2; 
      universe[i+46][j+17]= 2; 
      universe[i+47][j+17]= 2; 
      universe[i+48][j+17]= 2; 
      universe[i+49][j+17]= 2; 
      universe[i+50][j+17]= 2; 
      universe[i+51][j+17]= 2; 
      universe[i+52][j+17]= 2; 
      universe[i+53][j+17]= 2; 
      universe[i+54][j+17]= 2; 
      universe[i+55][j+17]= 2; 
      universe[i+56][j+17]= 2; 
      universe[i+57][j+17]= 2; 
      universe[i+58][j+17]= 2; 
      universe[i+59][j+17]= 2; 
      universe[i+60][j+17]= 2; 
      universe[i+61][j+17]= 2; 
      universe[i+62][j+17]= 2; 
      universe[i+63][j+17]= 2; 
      universe[i+64][j+17]= 2; 
      universe[i+65][j+17]= 2; 
      universe[i+66][j+17]= 2; 
      universe[i+67][j+17]= 2; 
      universe[i+68][j+17]= 2; 
      universe[i+69][j+17]= 2; 
      universe[i+70][j+17]= 2; 
      universe[i+71][j+17]= 2; 
      universe[i+72][j+17]= 2; 
      universe[i+73][j+17]= 2; 
      universe[i+74][j+17]= 2; 
      universe[i+0][j+18]= 2; 
      universe[i+1][j+18]= 2; 
      universe[i+2][j+18]= 2; 
      universe[i+3][j+18]= 2; 
      universe[i+4][j+18]= 2; 
      universe[i+5][j+18]= 2; 
      universe[i+6][j+18]= 2; 
      universe[i+7][j+18]= 2; 
      universe[i+8][j+18]= 2; 
      universe[i+9][j+18]= 2; 
      universe[i+10][j+18]= 2; 
      universe[i+11][j+18]= 2; 
      universe[i+12][j+18]= 2; 
      universe[i+13][j+18]= 2; 
      universe[i+14][j+18]= 2; 
      universe[i+15][j+18]= 2; 
      universe[i+16][j+18]= 2; 
      universe[i+17][j+18]= 2; 
      universe[i+18][j+18]= 2; 
      universe[i+19][j+18]= 2; 
      universe[i+20][j+18]= 2; 
      universe[i+21][j+18]= 2; 
      universe[i+22][j+18]= 2; 
      universe[i+23][j+18]= 2; 
      universe[i+24][j+18]= 2; 
      universe[i+25][j+18]= 2; 
      universe[i+26][j+18]= 2; 
      universe[i+27][j+18]= 2; 
      universe[i+28][j+18]= 2; 
      universe[i+29][j+18]= 2; 
      universe[i+30][j+18]= 2; 
      universe[i+31][j+18]= 2; 
      universe[i+32][j+18]= 2; 
      universe[i+33][j+18]= 2; 
      universe[i+34][j+18]= 2; 
      universe[i+35][j+18]= 2; 
      universe[i+36][j+18]= 2; 
      universe[i+37][j+18]= 2; 
      universe[i+38][j+18]= 2; 
      universe[i+39][j+18]= 2; 
      universe[i+40][j+18]= 2; 
      universe[i+41][j+18]= 2; 
      universe[i+42][j+18]= 2; 
      universe[i+43][j+18]= 2; 
      universe[i+44][j+18]= 2; 
      universe[i+45][j+18]= 2; 
      universe[i+46][j+18]= 2; 
      universe[i+47][j+18]= 2; 
      universe[i+48][j+18]= 2; 
      universe[i+49][j+18]= 2; 
      universe[i+50][j+18]= 2; 
      universe[i+51][j+18]= 2; 
      universe[i+52][j+18]= 2; 
      universe[i+53][j+18]= 2; 
      universe[i+54][j+18]= 2; 
      universe[i+55][j+18]= 2; 
      universe[i+56][j+18]= 2; 
      universe[i+57][j+18]= 2; 
      universe[i+58][j+18]= 2; 
      universe[i+59][j+18]= 2; 
      universe[i+60][j+18]= 2; 
      universe[i+61][j+18]= 2; 
      universe[i+62][j+18]= 2; 
      universe[i+63][j+18]= 2; 
      universe[i+64][j+18]= 2; 
      universe[i+65][j+18]= 2; 
      universe[i+66][j+18]= 2; 
      universe[i+67][j+18]= 2; 
      universe[i+68][j+18]= 2; 
      universe[i+69][j+18]= 2; 
      universe[i+70][j+18]= 2; 
      universe[i+71][j+18]= 2; 
      universe[i+72][j+18]= 2; 
      universe[i+73][j+18]= 2; 
      universe[i+74][j+18]= 2; 
      universe[i+0][j+19]= 2; 
      universe[i+1][j+19]= 2; 
      universe[i+2][j+19]= 2; 
      universe[i+3][j+19]= 2; 
      universe[i+4][j+19]= 2; 
      universe[i+5][j+19]= 2; 
      universe[i+6][j+19]= 2; 
      universe[i+7][j+19]= 2; 
      universe[i+8][j+19]= 2; 
      universe[i+9][j+19]= 2; 
      universe[i+10][j+19]= 2; 
      universe[i+11][j+19]= 2; 
      universe[i+12][j+19]= 2; 
      universe[i+13][j+19]= 2; 
      universe[i+14][j+19]= 2; 
      universe[i+15][j+19]= 2; 
      universe[i+16][j+19]= 2; 
      universe[i+17][j+19]= 2; 
      universe[i+18][j+19]= 2; 
      universe[i+19][j+19]= 2; 
      universe[i+20][j+19]= 2; 
      universe[i+21][j+19]= 2; 
      universe[i+22][j+19]= 2; 
      universe[i+23][j+19]= 2; 
      universe[i+24][j+19]= 2; 
      universe[i+25][j+19]= 2; 
      universe[i+26][j+19]= 2; 
      universe[i+27][j+19]= 2; 
      universe[i+28][j+19]= 2; 
      universe[i+29][j+19]= 2; 
      universe[i+30][j+19]= 2; 
      universe[i+31][j+19]= 2; 
      universe[i+32][j+19]= 2; 
      universe[i+33][j+19]= 2; 
      universe[i+34][j+19]= 2; 
      universe[i+35][j+19]= 2; 
      universe[i+36][j+19]= 2; 
      universe[i+37][j+19]= 2; 
      universe[i+38][j+19]= 2; 
      universe[i+39][j+19]= 2; 
      universe[i+40][j+19]= 2; 
      universe[i+41][j+19]= 2; 
      universe[i+42][j+19]= 2; 
      universe[i+43][j+19]= 2; 
      universe[i+44][j+19]= 2; 
      universe[i+45][j+19]= 2; 
      universe[i+46][j+19]= 2; 
      universe[i+47][j+19]= 2; 
      universe[i+48][j+19]= 2; 
      universe[i+49][j+19]= 2; 
      universe[i+50][j+19]= 2; 
      universe[i+51][j+19]= 2; 
      universe[i+52][j+19]= 2; 
      universe[i+53][j+19]= 2; 
      universe[i+54][j+19]= 2; 
      universe[i+55][j+19]= 2; 
      universe[i+56][j+19]= 2; 
      universe[i+57][j+19]= 2; 
      universe[i+58][j+19]= 2; 
      universe[i+59][j+19]= 2; 
      universe[i+60][j+19]= 2; 
      universe[i+61][j+19]= 2; 
      universe[i+62][j+19]= 2; 
      universe[i+63][j+19]= 2; 
      universe[i+64][j+19]= 2; 
      universe[i+65][j+19]= 2; 
      universe[i+66][j+19]= 2; 
      universe[i+67][j+19]= 2; 
      universe[i+68][j+19]= 2; 
      universe[i+69][j+19]= 2; 
      universe[i+70][j+19]= 2; 
      universe[i+71][j+19]= 2; 
      universe[i+72][j+19]= 2; 
      universe[i+73][j+19]= 2; 
      universe[i+74][j+19]= 2; 
      universe[i+0][j+20]= 2; 
      universe[i+1][j+20]= 2; 
      universe[i+2][j+20]= 2; 
      universe[i+3][j+20]= 2; 
      universe[i+4][j+20]= 2; 
      universe[i+5][j+20]= 2; 
      universe[i+6][j+20]= 2; 
      universe[i+7][j+20]= 2; 
      universe[i+8][j+20]= 2; 
      universe[i+9][j+20]= 2; 
      universe[i+10][j+20]= 2; 
      universe[i+11][j+20]= 2; 
      universe[i+12][j+20]= 2; 
      universe[i+13][j+20]= 2; 
      universe[i+14][j+20]= 2; 
      universe[i+15][j+20]= 2; 
      universe[i+16][j+20]= 2; 
      universe[i+17][j+20]= 2; 
      universe[i+18][j+20]= 2; 
      universe[i+19][j+20]= 2; 
      universe[i+20][j+20]= 2; 
      universe[i+21][j+20]= 2; 
      universe[i+22][j+20]= 2; 
      universe[i+23][j+20]= 2; 
      universe[i+24][j+20]= 2; 
      universe[i+25][j+20]= 2; 
      universe[i+26][j+20]= 2; 
      universe[i+27][j+20]= 2; 
      universe[i+28][j+20]= 2; 
      universe[i+29][j+20]= 2; 
      universe[i+30][j+20]= 2; 
      universe[i+31][j+20]= 2; 
      universe[i+32][j+20]= 2; 
      universe[i+33][j+20]= 2; 
      universe[i+34][j+20]= 2; 
      universe[i+35][j+20]= 2; 
      universe[i+36][j+20]= 2; 
      universe[i+37][j+20]= 2; 
      universe[i+38][j+20]= 2; 
      universe[i+39][j+20]= 2; 
      universe[i+40][j+20]= 2; 
      universe[i+41][j+20]= 2; 
      universe[i+42][j+20]= 2; 
      universe[i+43][j+20]= 2; 
      universe[i+44][j+20]= 2; 
      universe[i+45][j+20]= 2; 
      universe[i+46][j+20]= 2; 
      universe[i+47][j+20]= 2; 
      universe[i+48][j+20]= 2; 
      universe[i+49][j+20]= 2; 
      universe[i+50][j+20]= 2; 
      universe[i+51][j+20]= 2; 
      universe[i+52][j+20]= 2; 
      universe[i+53][j+20]= 2; 
      universe[i+54][j+20]= 2; 
      universe[i+55][j+20]= 2; 
      universe[i+56][j+20]= 2; 
      universe[i+57][j+20]= 2; 
      universe[i+58][j+20]= 2; 
      universe[i+59][j+20]= 2; 
      universe[i+60][j+20]= 2; 
      universe[i+61][j+20]= 2; 
      universe[i+62][j+20]= 2; 
      universe[i+63][j+20]= 2; 
      universe[i+64][j+20]= 2; 
      universe[i+65][j+20]= 2; 
      universe[i+66][j+20]= 2; 
      universe[i+67][j+20]= 2; 
      universe[i+68][j+20]= 2; 
      universe[i+69][j+20]= 2; 
      universe[i+70][j+20]= 2; 
      universe[i+71][j+20]= 2; 
      universe[i+72][j+20]= 2; 
      universe[i+73][j+20]= 2; 
      universe[i+74][j+20]= 2; 
      universe[i+0][j+21]= 2; 
      universe[i+1][j+21]= 2; 
      universe[i+2][j+21]= 2; 
      universe[i+3][j+21]= 2; 
      universe[i+4][j+21]= 2; 
      universe[i+5][j+21]= 2; 
      universe[i+6][j+21]= 2; 
      universe[i+7][j+21]= 2; 
      universe[i+8][j+21]= 2; 
      universe[i+9][j+21]= 2; 
      universe[i+10][j+21]= 2; 
      universe[i+11][j+21]= 2; 
      universe[i+12][j+21]= 2; 
      universe[i+13][j+21]= 2; 
      universe[i+14][j+21]= 2; 
      universe[i+15][j+21]= 2; 
      universe[i+16][j+21]= 2; 
      universe[i+17][j+21]= 2; 
      universe[i+18][j+21]= 2; 
      universe[i+19][j+21]= 2; 
      universe[i+20][j+21]= 2; 
      universe[i+21][j+21]= 2; 
      universe[i+22][j+21]= 2; 
      universe[i+23][j+21]= 2; 
      universe[i+24][j+21]= 2; 
      universe[i+25][j+21]= 2; 
      universe[i+26][j+21]= 2; 
      universe[i+27][j+21]= 2; 
      universe[i+28][j+21]= 2; 
      universe[i+29][j+21]= 2; 
      universe[i+30][j+21]= 2; 
      universe[i+31][j+21]= 2; 
      universe[i+32][j+21]= 2; 
      universe[i+33][j+21]= 2; 
      universe[i+34][j+21]= 2; 
      universe[i+35][j+21]= 2; 
      universe[i+36][j+21]= 2; 
      universe[i+37][j+21]= 2; 
      universe[i+38][j+21]= 2; 
      universe[i+39][j+21]= 2; 
      universe[i+40][j+21]= 2; 
      universe[i+41][j+21]= 2; 
      universe[i+42][j+21]= 2; 
      universe[i+43][j+21]= 2; 
      universe[i+44][j+21]= 2; 
      universe[i+45][j+21]= 2; 
      universe[i+46][j+21]= 2; 
      universe[i+47][j+21]= 2; 
      universe[i+48][j+21]= 2; 
      universe[i+49][j+21]= 2; 
      universe[i+50][j+21]= 2; 
      universe[i+51][j+21]= 2; 
      universe[i+52][j+21]= 2; 
      universe[i+53][j+21]= 2; 
      universe[i+54][j+21]= 2; 
      universe[i+55][j+21]= 2; 
      universe[i+56][j+21]= 2; 
      universe[i+57][j+21]= 2; 
      universe[i+58][j+21]= 2; 
      universe[i+59][j+21]= 2; 
      universe[i+60][j+21]= 2; 
      universe[i+61][j+21]= 2; 
      universe[i+62][j+21]= 2; 
      universe[i+63][j+21]= 2; 
      universe[i+64][j+21]= 2; 
      universe[i+65][j+21]= 2; 
      universe[i+66][j+21]= 2; 
      universe[i+67][j+21]= 2; 
      universe[i+68][j+21]= 2; 
      universe[i+69][j+21]= 2; 
      universe[i+70][j+21]= 2; 
      universe[i+71][j+21]= 2; 
      universe[i+72][j+21]= 2; 
      universe[i+73][j+21]= 2; 
      universe[i+74][j+21]= 2; 
      universe[i+0][j+22]= 2; 
      universe[i+1][j+22]= 2; 
      universe[i+2][j+22]= 2; 
      universe[i+3][j+22]= 2; 
      universe[i+4][j+22]= 2; 
      universe[i+5][j+22]= 2; 
      universe[i+6][j+22]= 2; 
      universe[i+7][j+22]= 2; 
      universe[i+8][j+22]= 2; 
      universe[i+9][j+22]= 2; 
      universe[i+10][j+22]= 2; 
      universe[i+11][j+22]= 2; 
      universe[i+12][j+22]= 2; 
      universe[i+13][j+22]= 2; 
      universe[i+14][j+22]= 2; 
      universe[i+15][j+22]= 2; 
      universe[i+16][j+22]= 2; 
      universe[i+17][j+22]= 2; 
      universe[i+18][j+22]= 2; 
      universe[i+19][j+22]= 2; 
      universe[i+20][j+22]= 2; 
      universe[i+21][j+22]= 2; 
      universe[i+22][j+22]= 2; 
      universe[i+23][j+22]= 2; 
      universe[i+24][j+22]= 2; 
      universe[i+25][j+22]= 2; 
      universe[i+26][j+22]= 2; 
      universe[i+27][j+22]= 2; 
      universe[i+28][j+22]= 2; 
      universe[i+29][j+22]= 2; 
      universe[i+30][j+22]= 2; 
      universe[i+31][j+22]= 2; 
      universe[i+32][j+22]= 2; 
      universe[i+33][j+22]= 2; 
      universe[i+34][j+22]= 2; 
      universe[i+35][j+22]= 2; 
      universe[i+36][j+22]= 2; 
      universe[i+37][j+22]= 2; 
      universe[i+38][j+22]= 2; 
      universe[i+39][j+22]= 2; 
      universe[i+40][j+22]= 2; 
      universe[i+41][j+22]= 2; 
      universe[i+42][j+22]= 2; 
      universe[i+43][j+22]= 2; 
      universe[i+44][j+22]= 2; 
      universe[i+45][j+22]= 2; 
      universe[i+46][j+22]= 2; 
      universe[i+47][j+22]= 2; 
      universe[i+48][j+22]= 2; 
      universe[i+49][j+22]= 2; 
      universe[i+50][j+22]= 2; 
      universe[i+51][j+22]= 2; 
      universe[i+52][j+22]= 2; 
      universe[i+53][j+22]= 2; 
      universe[i+54][j+22]= 2; 
      universe[i+55][j+22]= 2; 
      universe[i+56][j+22]= 2; 
      universe[i+57][j+22]= 2; 
      universe[i+58][j+22]= 2; 
      universe[i+59][j+22]= 2; 
      universe[i+60][j+22]= 2; 
      universe[i+61][j+22]= 2; 
      universe[i+62][j+22]= 2; 
      universe[i+63][j+22]= 2; 
      universe[i+64][j+22]= 2; 
      universe[i+65][j+22]= 2; 
      universe[i+66][j+22]= 2; 
      universe[i+67][j+22]= 2; 
      universe[i+68][j+22]= 2; 
      universe[i+69][j+22]= 2; 
      universe[i+70][j+22]= 2; 
      universe[i+71][j+22]= 2; 
      universe[i+72][j+22]= 2; 
      universe[i+73][j+22]= 2; 
      universe[i+74][j+22]= 2; 
      universe[i+0][j+23]= 5; 
      universe[i+1][j+23]= 5; 
      universe[i+2][j+23]= 5; 
      universe[i+3][j+23]= 5; 
      universe[i+4][j+23]= 5; 
      universe[i+5][j+23]= 5; 
      universe[i+6][j+23]= 5; 
      universe[i+7][j+23]= 5; 
      universe[i+8][j+23]= 5; 
      universe[i+9][j+23]= 5; 
      universe[i+10][j+23]= 5; 
      universe[i+11][j+23]= 5; 
      universe[i+12][j+23]= 5; 
      universe[i+13][j+23]= 5; 
      universe[i+14][j+23]= 5; 
      universe[i+15][j+23]= 5; 
      universe[i+16][j+23]= 5; 
      universe[i+17][j+23]= 5; 
      universe[i+18][j+23]= 5; 
      universe[i+19][j+23]= 5; 
      universe[i+20][j+23]= 5; 
      universe[i+21][j+23]= 5; 
      universe[i+22][j+23]= 5; 
      universe[i+23][j+23]= 5; 
      universe[i+24][j+23]= 5; 
      universe[i+25][j+23]= 5; 
      universe[i+26][j+23]= 5; 
      universe[i+27][j+23]= 5; 
      universe[i+28][j+23]= 5; 
      universe[i+29][j+23]= 5; 
      universe[i+30][j+23]= 5; 
      universe[i+31][j+23]= 5; 
      universe[i+32][j+23]= 5; 
      universe[i+33][j+23]= 4; 
      universe[i+34][j+23]= 2; 
      universe[i+35][j+23]= 2; 
      universe[i+36][j+23]= 2; 
      universe[i+37][j+23]= 2; 
      universe[i+38][j+23]= 2; 
      universe[i+39][j+23]= 2; 
      universe[i+40][j+23]= 2; 
      universe[i+41][j+23]= 4; 
      universe[i+42][j+23]= 5; 
      universe[i+43][j+23]= 5; 
      universe[i+44][j+23]= 5; 
      universe[i+45][j+23]= 5; 
      universe[i+46][j+23]= 5; 
      universe[i+47][j+23]= 5; 
      universe[i+48][j+23]= 5; 
      universe[i+49][j+23]= 5; 
      universe[i+50][j+23]= 5; 
      universe[i+51][j+23]= 5; 
      universe[i+52][j+23]= 5; 
      universe[i+53][j+23]= 5; 
      universe[i+54][j+23]= 5; 
      universe[i+55][j+23]= 5; 
      universe[i+56][j+23]= 5; 
      universe[i+57][j+23]= 5; 
      universe[i+58][j+23]= 5; 
      universe[i+59][j+23]= 5; 
      universe[i+60][j+23]= 5; 
      universe[i+61][j+23]= 5; 
      universe[i+62][j+23]= 5; 
      universe[i+63][j+23]= 5; 
      universe[i+64][j+23]= 5; 
      universe[i+65][j+23]= 5; 
      universe[i+66][j+23]= 5; 
      universe[i+67][j+23]= 5; 
      universe[i+68][j+23]= 5; 
      universe[i+69][j+23]= 5; 
      universe[i+70][j+23]= 5; 
      universe[i+71][j+23]= 5; 
      universe[i+72][j+23]= 5; 
      universe[i+73][j+23]= 5; 
      universe[i+74][j+23]= 5; 
      universe[i+0][j+24]= 5; 
      universe[i+1][j+24]= 5; 
      universe[i+2][j+24]= 5; 
      universe[i+3][j+24]= 5; 
      universe[i+4][j+24]= 5; 
      universe[i+5][j+24]= 5; 
      universe[i+6][j+24]= 5; 
      universe[i+7][j+24]= 5; 
      universe[i+8][j+24]= 5; 
      universe[i+9][j+24]= 5; 
      universe[i+10][j+24]= 5; 
      universe[i+11][j+24]= 5; 
      universe[i+12][j+24]= 5; 
      universe[i+13][j+24]= 5; 
      universe[i+14][j+24]= 5; 
      universe[i+15][j+24]= 5; 
      universe[i+16][j+24]= 5; 
      universe[i+17][j+24]= 5; 
      universe[i+18][j+24]= 5; 
      universe[i+19][j+24]= 5; 
      universe[i+20][j+24]= 5; 
      universe[i+21][j+24]= 5; 
      universe[i+22][j+24]= 5; 
      universe[i+23][j+24]= 5; 
      universe[i+24][j+24]= 5; 
      universe[i+25][j+24]= 5; 
      universe[i+26][j+24]= 5; 
      universe[i+27][j+24]= 5; 
      universe[i+28][j+24]= 5; 
      universe[i+29][j+24]= 5; 
      universe[i+30][j+24]= 5; 
      universe[i+31][j+24]= 5; 
      universe[i+32][j+24]= 5; 
      universe[i+33][j+24]= 4; 
      universe[i+34][j+24]= 2; 
      universe[i+35][j+24]= 2; 
      universe[i+36][j+24]= 2; 
      universe[i+37][j+24]= 2; 
      universe[i+38][j+24]= 2; 
      universe[i+39][j+24]= 2; 
      universe[i+40][j+24]= 2; 
      universe[i+41][j+24]= 4; 
      universe[i+42][j+24]= 5; 
      universe[i+43][j+24]= 5; 
      universe[i+44][j+24]= 5; 
      universe[i+45][j+24]= 5; 
      universe[i+46][j+24]= 5; 
      universe[i+47][j+24]= 5; 
      universe[i+48][j+24]= 5; 
      universe[i+49][j+24]= 5; 
      universe[i+50][j+24]= 5; 
      universe[i+51][j+24]= 5; 
      universe[i+52][j+24]= 5; 
      universe[i+53][j+24]= 5; 
      universe[i+54][j+24]= 5; 
      universe[i+55][j+24]= 5; 
      universe[i+56][j+24]= 5; 
      universe[i+57][j+24]= 5; 
      universe[i+58][j+24]= 5; 
      universe[i+59][j+24]= 5; 
      universe[i+60][j+24]= 5; 
      universe[i+61][j+24]= 5; 
      universe[i+62][j+24]= 5; 
      universe[i+63][j+24]= 5; 
      universe[i+64][j+24]= 5; 
      universe[i+65][j+24]= 5; 
      universe[i+66][j+24]= 5; 
      universe[i+67][j+24]= 5; 
      universe[i+68][j+24]= 5; 
      universe[i+69][j+24]= 5; 
      universe[i+70][j+24]= 5; 
      universe[i+71][j+24]= 5; 
      universe[i+72][j+24]= 5; 
      universe[i+73][j+24]= 5; 
      universe[i+74][j+24]= 5; 
      universe[i+0][j+25]= 3; 
      universe[i+1][j+25]= 3; 
      universe[i+2][j+25]= 3; 
      universe[i+3][j+25]= 3; 
      universe[i+4][j+25]= 3; 
      universe[i+5][j+25]= 3; 
      universe[i+6][j+25]= 3; 
      universe[i+7][j+25]= 3; 
      universe[i+8][j+25]= 3; 
      universe[i+9][j+25]= 3; 
      universe[i+10][j+25]= 3; 
      universe[i+11][j+25]= 3; 
      universe[i+12][j+25]= 3; 
      universe[i+13][j+25]= 3; 
      universe[i+14][j+25]= 3; 
      universe[i+15][j+25]= 3; 
      universe[i+16][j+25]= 5; 
      universe[i+17][j+25]= 5; 
      universe[i+18][j+25]= 5; 
      universe[i+19][j+25]= 5; 
      universe[i+20][j+25]= 5; 
      universe[i+21][j+25]= 5; 
      universe[i+22][j+25]= 5; 
      universe[i+23][j+25]= 5; 
      universe[i+24][j+25]= 4; 
      universe[i+25][j+25]= 4; 
      universe[i+26][j+25]= 4; 
      universe[i+27][j+25]= 4; 
      universe[i+28][j+25]= 4; 
      universe[i+29][j+25]= 4; 
      universe[i+30][j+25]= 5; 
      universe[i+31][j+25]= 5; 
      universe[i+32][j+25]= 5; 
      universe[i+33][j+25]= 4; 
      universe[i+34][j+25]= 2; 
      universe[i+35][j+25]= 2; 
      universe[i+36][j+25]= 2; 
      universe[i+37][j+25]= 2; 
      universe[i+38][j+25]= 2; 
      universe[i+39][j+25]= 2; 
      universe[i+40][j+25]= 2; 
      universe[i+41][j+25]= 4; 
      universe[i+42][j+25]= 5; 
      universe[i+43][j+25]= 5; 
      universe[i+44][j+25]= 5; 
      universe[i+45][j+25]= 5; 
      universe[i+46][j+25]= 5; 
      universe[i+47][j+25]= 5; 
      universe[i+48][j+25]= 5; 
      universe[i+49][j+25]= 5; 
      universe[i+50][j+25]= 4; 
      universe[i+51][j+25]= 4; 
      universe[i+52][j+25]= 4; 
      universe[i+53][j+25]= 4; 
      universe[i+54][j+25]= 4; 
      universe[i+55][j+25]= 4; 
      universe[i+56][j+25]= 4; 
      universe[i+57][j+25]= 5; 
      universe[i+58][j+25]= 5; 
      universe[i+59][j+25]= 3; 
      universe[i+60][j+25]= 3; 
      universe[i+61][j+25]= 3; 
      universe[i+62][j+25]= 3; 
      universe[i+63][j+25]= 3; 
      universe[i+64][j+25]= 3; 
      universe[i+65][j+25]= 3; 
      universe[i+66][j+25]= 3; 
      universe[i+67][j+25]= 3; 
      universe[i+68][j+25]= 3; 
      universe[i+69][j+25]= 3; 
      universe[i+70][j+25]= 3; 
      universe[i+71][j+25]= 3; 
      universe[i+72][j+25]= 3; 
      universe[i+73][j+25]= 3; 
      universe[i+74][j+25]= 3; 
      universe[i+0][j+26]= 1; 
      universe[i+1][j+26]= 1; 
      universe[i+2][j+26]= 1; 
      universe[i+3][j+26]= 1; 
      universe[i+4][j+26]= 1; 
      universe[i+5][j+26]= 1; 
      universe[i+6][j+26]= 1; 
      universe[i+7][j+26]= 1; 
      universe[i+8][j+26]= 1; 
      universe[i+9][j+26]= 1; 
      universe[i+10][j+26]= 1; 
      universe[i+11][j+26]= 1; 
      universe[i+12][j+26]= 1; 
      universe[i+13][j+26]= 1; 
      universe[i+14][j+26]= 4; 
      universe[i+15][j+26]= 5; 
      universe[i+16][j+26]= 5; 
      universe[i+17][j+26]= 5; 
      universe[i+18][j+26]= 5; 
      universe[i+19][j+26]= 5; 
      universe[i+20][j+26]= 5; 
      universe[i+21][j+26]= 5; 
      universe[i+22][j+26]= 4; 
      universe[i+23][j+26]= 2; 
      universe[i+24][j+26]= 2; 
      universe[i+25][j+26]= 2; 
      universe[i+26][j+26]= 2; 
      universe[i+27][j+26]= 2; 
      universe[i+28][j+26]= 4; 
      universe[i+29][j+26]= 5; 
      universe[i+30][j+26]= 5; 
      universe[i+31][j+26]= 5; 
      universe[i+32][j+26]= 5; 
      universe[i+33][j+26]= 4; 
      universe[i+34][j+26]= 2; 
      universe[i+35][j+26]= 2; 
      universe[i+36][j+26]= 2; 
      universe[i+37][j+26]= 2; 
      universe[i+38][j+26]= 2; 
      universe[i+39][j+26]= 2; 
      universe[i+40][j+26]= 2; 
      universe[i+41][j+26]= 4; 
      universe[i+42][j+26]= 5; 
      universe[i+43][j+26]= 5; 
      universe[i+44][j+26]= 5; 
      universe[i+45][j+26]= 5; 
      universe[i+46][j+26]= 5; 
      universe[i+47][j+26]= 5; 
      universe[i+48][j+26]= 5; 
      universe[i+49][j+26]= 5; 
      universe[i+50][j+26]= 5; 
      universe[i+51][j+26]= 5; 
      universe[i+52][j+26]= 4; 
      universe[i+53][j+26]= 2; 
      universe[i+54][j+26]= 2; 
      universe[i+55][j+26]= 2; 
      universe[i+56][j+26]= 2; 
      universe[i+57][j+26]= 2; 
      universe[i+58][j+26]= 4; 
      universe[i+59][j+26]= 5; 
      universe[i+60][j+26]= 4; 
      universe[i+61][j+26]= 1; 
      universe[i+62][j+26]= 1; 
      universe[i+63][j+26]= 1; 
      universe[i+64][j+26]= 1; 
      universe[i+65][j+26]= 1; 
      universe[i+66][j+26]= 1; 
      universe[i+67][j+26]= 1; 
      universe[i+68][j+26]= 1; 
      universe[i+69][j+26]= 1; 
      universe[i+70][j+26]= 1; 
      universe[i+71][j+26]= 1; 
      universe[i+72][j+26]= 1; 
      universe[i+73][j+26]= 1; 
      universe[i+74][j+26]= 1; 
      universe[i+0][j+27]= 1; 
      universe[i+1][j+27]= 1; 
      universe[i+2][j+27]= 1; 
      universe[i+3][j+27]= 1; 
      universe[i+4][j+27]= 1; 
      universe[i+5][j+27]= 1; 
      universe[i+6][j+27]= 1; 
      universe[i+7][j+27]= 1; 
      universe[i+8][j+27]= 1; 
      universe[i+9][j+27]= 1; 
      universe[i+10][j+27]= 1; 
      universe[i+11][j+27]= 1; 
      universe[i+12][j+27]= 4; 
      universe[i+13][j+27]= 5; 
      universe[i+14][j+27]= 5; 
      universe[i+15][j+27]= 5; 
      universe[i+16][j+27]= 5; 
      universe[i+17][j+27]= 5; 
      universe[i+18][j+27]= 5; 
      universe[i+19][j+27]= 5; 
      universe[i+20][j+27]= 4; 
      universe[i+21][j+27]= 2; 
      universe[i+22][j+27]= 2; 
      universe[i+23][j+27]= 2; 
      universe[i+24][j+27]= 2; 
      universe[i+25][j+27]= 2; 
      universe[i+26][j+27]= 4; 
      universe[i+27][j+27]= 5; 
      universe[i+28][j+27]= 5; 
      universe[i+29][j+27]= 3; 
      universe[i+30][j+27]= 1; 
      universe[i+31][j+27]= 4; 
      universe[i+32][j+27]= 5; 
      universe[i+33][j+27]= 4; 
      universe[i+34][j+27]= 2; 
      universe[i+35][j+27]= 2; 
      universe[i+36][j+27]= 2; 
      universe[i+37][j+27]= 2; 
      universe[i+38][j+27]= 2; 
      universe[i+39][j+27]= 2; 
      universe[i+40][j+27]= 2; 
      universe[i+41][j+27]= 4; 
      universe[i+42][j+27]= 5; 
      universe[i+43][j+27]= 4; 
      universe[i+44][j+27]= 1; 
      universe[i+45][j+27]= 3; 
      universe[i+46][j+27]= 5; 
      universe[i+47][j+27]= 5; 
      universe[i+48][j+27]= 5; 
      universe[i+49][j+27]= 5; 
      universe[i+50][j+27]= 5; 
      universe[i+51][j+27]= 5; 
      universe[i+52][j+27]= 5; 
      universe[i+53][j+27]= 5; 
      universe[i+54][j+27]= 4; 
      universe[i+55][j+27]= 2; 
      universe[i+56][j+27]= 2; 
      universe[i+57][j+27]= 2; 
      universe[i+58][j+27]= 2; 
      universe[i+59][j+27]= 2; 
      universe[i+60][j+27]= 5; 
      universe[i+61][j+27]= 5; 
      universe[i+62][j+27]= 4; 
      universe[i+63][j+27]= 1; 
      universe[i+64][j+27]= 1; 
      universe[i+65][j+27]= 1; 
      universe[i+66][j+27]= 1; 
      universe[i+67][j+27]= 1; 
      universe[i+68][j+27]= 1; 
      universe[i+69][j+27]= 1; 
      universe[i+70][j+27]= 1; 
      universe[i+71][j+27]= 1; 
      universe[i+72][j+27]= 1; 
      universe[i+73][j+27]= 1; 
      universe[i+74][j+27]= 1; 
      universe[i+0][j+28]= 1; 
      universe[i+1][j+28]= 1; 
      universe[i+2][j+28]= 1; 
      universe[i+3][j+28]= 1; 
      universe[i+4][j+28]= 1; 
      universe[i+5][j+28]= 1; 
      universe[i+6][j+28]= 1; 
      universe[i+7][j+28]= 1; 
      universe[i+8][j+28]= 1; 
      universe[i+9][j+28]= 1; 
      universe[i+10][j+28]= 4; 
      universe[i+11][j+28]= 5; 
      universe[i+12][j+28]= 5; 
      universe[i+13][j+28]= 5; 
      universe[i+14][j+28]= 5; 
      universe[i+15][j+28]= 5; 
      universe[i+16][j+28]= 5; 
      universe[i+17][j+28]= 5; 
      universe[i+18][j+28]= 4; 
      universe[i+19][j+28]= 2; 
      universe[i+20][j+28]= 2; 
      universe[i+21][j+28]= 2; 
      universe[i+22][j+28]= 2; 
      universe[i+23][j+28]= 2; 
      universe[i+24][j+28]= 4; 
      universe[i+25][j+28]= 5; 
      universe[i+26][j+28]= 5; 
      universe[i+27][j+28]= 3; 
      universe[i+28][j+28]= 1; 
      universe[i+29][j+28]= 1; 
      universe[i+30][j+28]= 1; 
      universe[i+31][j+28]= 4; 
      universe[i+32][j+28]= 5; 
      universe[i+33][j+28]= 4; 
      universe[i+34][j+28]= 2; 
      universe[i+35][j+28]= 2; 
      universe[i+36][j+28]= 2; 
      universe[i+37][j+28]= 2; 
      universe[i+38][j+28]= 2; 
      universe[i+39][j+28]= 2; 
      universe[i+40][j+28]= 2; 
      universe[i+41][j+28]= 4; 
      universe[i+42][j+28]= 5; 
      universe[i+43][j+28]= 4; 
      universe[i+44][j+28]= 1; 
      universe[i+45][j+28]= 1; 
      universe[i+46][j+28]= 1; 
      universe[i+47][j+28]= 3; 
      universe[i+48][j+28]= 5; 
      universe[i+49][j+28]= 5; 
      universe[i+50][j+28]= 5; 
      universe[i+51][j+28]= 5; 
      universe[i+52][j+28]= 5; 
      universe[i+53][j+28]= 5; 
      universe[i+54][j+28]= 5; 
      universe[i+55][j+28]= 5; 
      universe[i+56][j+28]= 4; 
      universe[i+57][j+28]= 2; 
      universe[i+58][j+28]= 2; 
      universe[i+59][j+28]= 2; 
      universe[i+60][j+28]= 2; 
      universe[i+61][j+28]= 4; 
      universe[i+62][j+28]= 5; 
      universe[i+63][j+28]= 5; 
      universe[i+64][j+28]= 4; 
      universe[i+65][j+28]= 1; 
      universe[i+66][j+28]= 1; 
      universe[i+67][j+28]= 1; 
      universe[i+68][j+28]= 1; 
      universe[i+69][j+28]= 1; 
      universe[i+70][j+28]= 1; 
      universe[i+71][j+28]= 1; 
      universe[i+72][j+28]= 1; 
      universe[i+73][j+28]= 1; 
      universe[i+74][j+28]= 1; 
      universe[i+0][j+29]= 1; 
      universe[i+1][j+29]= 1; 
      universe[i+2][j+29]= 1; 
      universe[i+3][j+29]= 1; 
      universe[i+4][j+29]= 1; 
      universe[i+5][j+29]= 1; 
      universe[i+6][j+29]= 1; 
      universe[i+7][j+29]= 1; 
      universe[i+8][j+29]= 4; 
      universe[i+9][j+29]= 5; 
      universe[i+10][j+29]= 5; 
      universe[i+11][j+29]= 5; 
      universe[i+12][j+29]= 5; 
      universe[i+13][j+29]= 5; 
      universe[i+14][j+29]= 5; 
      universe[i+15][j+29]= 5; 
      universe[i+16][j+29]= 4; 
      universe[i+17][j+29]= 2; 
      universe[i+18][j+29]= 2; 
      universe[i+19][j+29]= 2; 
      universe[i+20][j+29]= 2; 
      universe[i+21][j+29]= 2; 
      universe[i+22][j+29]= 4; 
      universe[i+23][j+29]= 5; 
      universe[i+24][j+29]= 5; 
      universe[i+25][j+29]= 3; 
      universe[i+26][j+29]= 1; 
      universe[i+27][j+29]= 1; 
      universe[i+28][j+29]= 1; 
      universe[i+29][j+29]= 1; 
      universe[i+30][j+29]= 1; 
      universe[i+31][j+29]= 4; 
      universe[i+32][j+29]= 5; 
      universe[i+33][j+29]= 4; 
      universe[i+34][j+29]= 2; 
      universe[i+35][j+29]= 2; 
      universe[i+36][j+29]= 2; 
      universe[i+37][j+29]= 2; 
      universe[i+38][j+29]= 2; 
      universe[i+39][j+29]= 2; 
      universe[i+40][j+29]= 2; 
      universe[i+41][j+29]= 4; 
      universe[i+42][j+29]= 5; 
      universe[i+43][j+29]= 4; 
      universe[i+44][j+29]= 1; 
      universe[i+45][j+29]= 1; 
      universe[i+46][j+29]= 1; 
      universe[i+47][j+29]= 1; 
      universe[i+48][j+29]= 1; 
      universe[i+49][j+29]= 3; 
      universe[i+50][j+29]= 5; 
      universe[i+51][j+29]= 5; 
      universe[i+52][j+29]= 5; 
      universe[i+53][j+29]= 5; 
      universe[i+54][j+29]= 5; 
      universe[i+55][j+29]= 5; 
      universe[i+56][j+29]= 5; 
      universe[i+57][j+29]= 5; 
      universe[i+58][j+29]= 4; 
      universe[i+59][j+29]= 2; 
      universe[i+60][j+29]= 2; 
      universe[i+61][j+29]= 2; 
      universe[i+62][j+29]= 2; 
      universe[i+63][j+29]= 4; 
      universe[i+64][j+29]= 5; 
      universe[i+65][j+29]= 5; 
      universe[i+66][j+29]= 4; 
      universe[i+67][j+29]= 1; 
      universe[i+68][j+29]= 1; 
      universe[i+69][j+29]= 1; 
      universe[i+70][j+29]= 1; 
      universe[i+71][j+29]= 1; 
      universe[i+72][j+29]= 1; 
      universe[i+73][j+29]= 1; 
      universe[i+74][j+29]= 1; 
      universe[i+0][j+30]= 1; 
      universe[i+1][j+30]= 1; 
      universe[i+2][j+30]= 1; 
      universe[i+3][j+30]= 1; 
      universe[i+4][j+30]= 1; 
      universe[i+5][j+30]= 1; 
      universe[i+6][j+30]= 4; 
      universe[i+7][j+30]= 5; 
      universe[i+8][j+30]= 5; 
      universe[i+9][j+30]= 5; 
      universe[i+10][j+30]= 5; 
      universe[i+11][j+30]= 5; 
      universe[i+12][j+30]= 5; 
      universe[i+13][j+30]= 5; 
      universe[i+14][j+30]= 4; 
      universe[i+15][j+30]= 2; 
      universe[i+16][j+30]= 2; 
      universe[i+17][j+30]= 2; 
      universe[i+18][j+30]= 2; 
      universe[i+19][j+30]= 2; 
      universe[i+20][j+30]= 4; 
      universe[i+21][j+30]= 5; 
      universe[i+22][j+30]= 5; 
      universe[i+23][j+30]= 3; 
      universe[i+24][j+30]= 1; 
      universe[i+25][j+30]= 1; 
      universe[i+26][j+30]= 1; 
      universe[i+27][j+30]= 1; 
      universe[i+28][j+30]= 1; 
      universe[i+29][j+30]= 1; 
      universe[i+30][j+30]= 1; 
      universe[i+31][j+30]= 4; 
      universe[i+32][j+30]= 5; 
      universe[i+33][j+30]= 4; 
      universe[i+34][j+30]= 2; 
      universe[i+35][j+30]= 2; 
      universe[i+36][j+30]= 2; 
      universe[i+37][j+30]= 2; 
      universe[i+38][j+30]= 2; 
      universe[i+39][j+30]= 2; 
      universe[i+40][j+30]= 2; 
      universe[i+41][j+30]= 4; 
      universe[i+42][j+30]= 5; 
      universe[i+43][j+30]= 4; 
      universe[i+44][j+30]= 1; 
      universe[i+45][j+30]= 1; 
      universe[i+46][j+30]= 1; 
      universe[i+47][j+30]= 1; 
      universe[i+48][j+30]= 1; 
      universe[i+49][j+30]= 1; 
      universe[i+50][j+30]= 1; 
      universe[i+51][j+30]= 3; 
      universe[i+52][j+30]= 5; 
      universe[i+53][j+30]= 5; 
      universe[i+54][j+30]= 5; 
      universe[i+55][j+30]= 5; 
      universe[i+56][j+30]= 5; 
      universe[i+57][j+30]= 5; 
      universe[i+58][j+30]= 5; 
      universe[i+59][j+30]= 5; 
      universe[i+60][j+30]= 4; 
      universe[i+61][j+30]= 2; 
      universe[i+62][j+30]= 2; 
      universe[i+63][j+30]= 2; 
      universe[i+64][j+30]= 2; 
      universe[i+65][j+30]= 4; 
      universe[i+66][j+30]= 5; 
      universe[i+67][j+30]= 5; 
      universe[i+68][j+30]= 4; 
      universe[i+69][j+30]= 1; 
      universe[i+70][j+30]= 1; 
      universe[i+71][j+30]= 1; 
      universe[i+72][j+30]= 1; 
      universe[i+73][j+30]= 1; 
      universe[i+74][j+30]= 1; 
      universe[i+0][j+31]= 1; 
      universe[i+1][j+31]= 1; 
      universe[i+2][j+31]= 1; 
      universe[i+3][j+31]= 1; 
      universe[i+4][j+31]= 4; 
      universe[i+5][j+31]= 5; 
      universe[i+6][j+31]= 5; 
      universe[i+7][j+31]= 5; 
      universe[i+8][j+31]= 5; 
      universe[i+9][j+31]= 5; 
      universe[i+10][j+31]= 5; 
      universe[i+11][j+31]= 5; 
      universe[i+12][j+31]= 4; 
      universe[i+13][j+31]= 2; 
      universe[i+14][j+31]= 2; 
      universe[i+15][j+31]= 2; 
      universe[i+16][j+31]= 2; 
      universe[i+17][j+31]= 2; 
      universe[i+18][j+31]= 4; 
      universe[i+19][j+31]= 5; 
      universe[i+20][j+31]= 5; 
      universe[i+21][j+31]= 3; 
      universe[i+22][j+31]= 1; 
      universe[i+23][j+31]= 1; 
      universe[i+24][j+31]= 1; 
      universe[i+25][j+31]= 1; 
      universe[i+26][j+31]= 1; 
      universe[i+27][j+31]= 1; 
      universe[i+28][j+31]= 1; 
      universe[i+29][j+31]= 1; 
      universe[i+30][j+31]= 1; 
      universe[i+31][j+31]= 4; 
      universe[i+32][j+31]= 5; 
      universe[i+33][j+31]= 4; 
      universe[i+34][j+31]= 2; 
      universe[i+35][j+31]= 2; 
      universe[i+36][j+31]= 2; 
      universe[i+37][j+31]= 2; 
      universe[i+38][j+31]= 2; 
      universe[i+39][j+31]= 2; 
      universe[i+40][j+31]= 2; 
      universe[i+41][j+31]= 4; 
      universe[i+42][j+31]= 5; 
      universe[i+43][j+31]= 4; 
      universe[i+44][j+31]= 1; 
      universe[i+45][j+31]= 1; 
      universe[i+46][j+31]= 1; 
      universe[i+47][j+31]= 1; 
      universe[i+48][j+31]= 1; 
      universe[i+49][j+31]= 1; 
      universe[i+50][j+31]= 1; 
      universe[i+51][j+31]= 1; 
      universe[i+52][j+31]= 1; 
      universe[i+53][j+31]= 3; 
      universe[i+54][j+31]= 5; 
      universe[i+55][j+31]= 5; 
      universe[i+56][j+31]= 5; 
      universe[i+57][j+31]= 5; 
      universe[i+58][j+31]= 5; 
      universe[i+59][j+31]= 5; 
      universe[i+60][j+31]= 5; 
      universe[i+61][j+31]= 4; 
      universe[i+62][j+31]= 2; 
      universe[i+63][j+31]= 2; 
      universe[i+64][j+31]= 2; 
      universe[i+65][j+31]= 2; 
      universe[i+66][j+31]= 2; 
      universe[i+67][j+31]= 4; 
      universe[i+68][j+31]= 5; 
      universe[i+69][j+31]= 5; 
      universe[i+70][j+31]= 4; 
      universe[i+71][j+31]= 1; 
      universe[i+72][j+31]= 1; 
      universe[i+73][j+31]= 1; 
      universe[i+74][j+31]= 1; 
      universe[i+0][j+32]= 1; 
      universe[i+1][j+32]= 1; 
      universe[i+2][j+32]= 4; 
      universe[i+3][j+32]= 5; 
      universe[i+4][j+32]= 5; 
      universe[i+5][j+32]= 5; 
      universe[i+6][j+32]= 5; 
      universe[i+7][j+32]= 5; 
      universe[i+8][j+32]= 5; 
      universe[i+9][j+32]= 5; 
      universe[i+10][j+32]= 4; 
      universe[i+11][j+32]= 2; 
      universe[i+12][j+32]= 2; 
      universe[i+13][j+32]= 2; 
      universe[i+14][j+32]= 2; 
      universe[i+15][j+32]= 2; 
      universe[i+16][j+32]= 4; 
      universe[i+17][j+32]= 5; 
      universe[i+18][j+32]= 5; 
      universe[i+19][j+32]= 3; 
      universe[i+20][j+32]= 1; 
      universe[i+21][j+32]= 1; 
      universe[i+22][j+32]= 1; 
      universe[i+23][j+32]= 1; 
      universe[i+24][j+32]= 1; 
      universe[i+25][j+32]= 1; 
      universe[i+26][j+32]= 1; 
      universe[i+27][j+32]= 1; 
      universe[i+28][j+32]= 1; 
      universe[i+29][j+32]= 1; 
      universe[i+30][j+32]= 1; 
      universe[i+31][j+32]= 4; 
      universe[i+32][j+32]= 5; 
      universe[i+33][j+32]= 4; 
      universe[i+34][j+32]= 2; 
      universe[i+35][j+32]= 2; 
      universe[i+36][j+32]= 2; 
      universe[i+37][j+32]= 2; 
      universe[i+38][j+32]= 2; 
      universe[i+39][j+32]= 2; 
      universe[i+40][j+32]= 2; 
      universe[i+41][j+32]= 4; 
      universe[i+42][j+32]= 5; 
      universe[i+43][j+32]= 4; 
      universe[i+44][j+32]= 1; 
      universe[i+45][j+32]= 1; 
      universe[i+46][j+32]= 1; 
      universe[i+47][j+32]= 1; 
      universe[i+48][j+32]= 1; 
      universe[i+49][j+32]= 1; 
      universe[i+50][j+32]= 1; 
      universe[i+51][j+32]= 1; 
      universe[i+52][j+32]= 1; 
      universe[i+53][j+32]= 1; 
      universe[i+54][j+32]= 1; 
      universe[i+55][j+32]= 3; 
      universe[i+56][j+32]= 5; 
      universe[i+57][j+32]= 5; 
      universe[i+58][j+32]= 5; 
      universe[i+59][j+32]= 5; 
      universe[i+60][j+32]= 5; 
      universe[i+61][j+32]= 5; 
      universe[i+62][j+32]= 5; 
      universe[i+63][j+32]= 4; 
      universe[i+64][j+32]= 2; 
      universe[i+65][j+32]= 2; 
      universe[i+66][j+32]= 2; 
      universe[i+67][j+32]= 2; 
      universe[i+68][j+32]= 2; 
      universe[i+69][j+32]= 4; 
      universe[i+70][j+32]= 5; 
      universe[i+71][j+32]= 5; 
      universe[i+72][j+32]= 4; 
      universe[i+73][j+32]= 1; 
      universe[i+74][j+32]= 1; 
      universe[i+0][j+33]= 4; 
      universe[i+1][j+33]= 5; 
      universe[i+2][j+33]= 5; 
      universe[i+3][j+33]= 5; 
      universe[i+4][j+33]= 5; 
      universe[i+5][j+33]= 5; 
      universe[i+6][j+33]= 5; 
      universe[i+7][j+33]= 5; 
      universe[i+8][j+33]= 4; 
      universe[i+9][j+33]= 2; 
      universe[i+10][j+33]= 2; 
      universe[i+11][j+33]= 2; 
      universe[i+12][j+33]= 2; 
      universe[i+13][j+33]= 2; 
      universe[i+14][j+33]= 4; 
      universe[i+15][j+33]= 5; 
      universe[i+16][j+33]= 5; 
      universe[i+17][j+33]= 3; 
      universe[i+18][j+33]= 1; 
      universe[i+19][j+33]= 1; 
      universe[i+20][j+33]= 1; 
      universe[i+21][j+33]= 1; 
      universe[i+22][j+33]= 1; 
      universe[i+23][j+33]= 1; 
      universe[i+24][j+33]= 1; 
      universe[i+25][j+33]= 1; 
      universe[i+26][j+33]= 1; 
      universe[i+27][j+33]= 1; 
      universe[i+28][j+33]= 1; 
      universe[i+29][j+33]= 1; 
      universe[i+30][j+33]= 1; 
      universe[i+31][j+33]= 4; 
      universe[i+32][j+33]= 5; 
      universe[i+33][j+33]= 4; 
      universe[i+34][j+33]= 2; 
      universe[i+35][j+33]= 2; 
      universe[i+36][j+33]= 2; 
      universe[i+37][j+33]= 2; 
      universe[i+38][j+33]= 2; 
      universe[i+39][j+33]= 2; 
      universe[i+40][j+33]= 2; 
      universe[i+41][j+33]= 4; 
      universe[i+42][j+33]= 5; 
      universe[i+43][j+33]= 4; 
      universe[i+44][j+33]= 1; 
      universe[i+45][j+33]= 1; 
      universe[i+46][j+33]= 1; 
      universe[i+47][j+33]= 1; 
      universe[i+48][j+33]= 1; 
      universe[i+49][j+33]= 1; 
      universe[i+50][j+33]= 1; 
      universe[i+51][j+33]= 1; 
      universe[i+52][j+33]= 1; 
      universe[i+53][j+33]= 1; 
      universe[i+54][j+33]= 1; 
      universe[i+55][j+33]= 1; 
      universe[i+56][j+33]= 1; 
      universe[i+57][j+33]= 3; 
      universe[i+58][j+33]= 5; 
      universe[i+59][j+33]= 5; 
      universe[i+60][j+33]= 5; 
      universe[i+61][j+33]= 5; 
      universe[i+62][j+33]= 5; 
      universe[i+63][j+33]= 5; 
      universe[i+64][j+33]= 5; 
      universe[i+65][j+33]= 4; 
      universe[i+66][j+33]= 2; 
      universe[i+67][j+33]= 2; 
      universe[i+68][j+33]= 2; 
      universe[i+69][j+33]= 2; 
      universe[i+70][j+33]= 2; 
      universe[i+71][j+33]= 4; 
      universe[i+72][j+33]= 5; 
      universe[i+73][j+33]= 5; 
      universe[i+74][j+33]= 4; 
      universe[i+0][j+34]= 5; 
      universe[i+1][j+34]= 5; 
      universe[i+2][j+34]= 5; 
      universe[i+3][j+34]= 5; 
      universe[i+4][j+34]= 5; 
      universe[i+5][j+34]= 5; 
      universe[i+6][j+34]= 4; 
      universe[i+7][j+34]= 2; 
      universe[i+8][j+34]= 2; 
      universe[i+9][j+34]= 2; 
      universe[i+10][j+34]= 2; 
      universe[i+11][j+34]= 2; 
      universe[i+12][j+34]= 4; 
      universe[i+13][j+34]= 5; 
      universe[i+14][j+34]= 5; 
      universe[i+15][j+34]= 3; 
      universe[i+16][j+34]= 1; 
      universe[i+17][j+34]= 1; 
      universe[i+18][j+34]= 1; 
      universe[i+19][j+34]= 1; 
      universe[i+20][j+34]= 1; 
      universe[i+21][j+34]= 1; 
      universe[i+22][j+34]= 1; 
      universe[i+23][j+34]= 1; 
      universe[i+24][j+34]= 1; 
      universe[i+25][j+34]= 1; 
      universe[i+26][j+34]= 1; 
      universe[i+27][j+34]= 1; 
      universe[i+28][j+34]= 1; 
      universe[i+29][j+34]= 1; 
      universe[i+30][j+34]= 1; 
      universe[i+31][j+34]= 4; 
      universe[i+32][j+34]= 5; 
      universe[i+33][j+34]= 4; 
      universe[i+34][j+34]= 2; 
      universe[i+35][j+34]= 2; 
      universe[i+36][j+34]= 2; 
      universe[i+37][j+34]= 2; 
      universe[i+38][j+34]= 2; 
      universe[i+39][j+34]= 2; 
      universe[i+40][j+34]= 2; 
      universe[i+41][j+34]= 4; 
      universe[i+42][j+34]= 5; 
      universe[i+43][j+34]= 4; 
      universe[i+44][j+34]= 1; 
      universe[i+45][j+34]= 1; 
      universe[i+46][j+34]= 1; 
      universe[i+47][j+34]= 1; 
      universe[i+48][j+34]= 1; 
      universe[i+49][j+34]= 1; 
      universe[i+50][j+34]= 1; 
      universe[i+51][j+34]= 1; 
      universe[i+52][j+34]= 1; 
      universe[i+53][j+34]= 1; 
      universe[i+54][j+34]= 1; 
      universe[i+55][j+34]= 1; 
      universe[i+56][j+34]= 1; 
      universe[i+57][j+34]= 1; 
      universe[i+58][j+34]= 1; 
      universe[i+59][j+34]= 3; 
      universe[i+60][j+34]= 5; 
      universe[i+61][j+34]= 5; 
      universe[i+62][j+34]= 5; 
      universe[i+63][j+34]= 5; 
      universe[i+64][j+34]= 5; 
      universe[i+65][j+34]= 5; 
      universe[i+66][j+34]= 5; 
      universe[i+67][j+34]= 4; 
      universe[i+68][j+34]= 2; 
      universe[i+69][j+34]= 2; 
      universe[i+70][j+34]= 2; 
      universe[i+71][j+34]= 2; 
      universe[i+72][j+34]= 2; 
      universe[i+73][j+34]= 4; 
      universe[i+74][j+34]= 5; 
      universe[i+0][j+35]= 5; 
      universe[i+1][j+35]= 5; 
      universe[i+2][j+35]= 5; 
      universe[i+3][j+35]= 5; 
      universe[i+4][j+35]= 4; 
      universe[i+5][j+35]= 2; 
      universe[i+6][j+35]= 2; 
      universe[i+7][j+35]= 2; 
      universe[i+8][j+35]= 2; 
      universe[i+9][j+35]= 2; 
      universe[i+10][j+35]= 4; 
      universe[i+11][j+35]= 5; 
      universe[i+12][j+35]= 5; 
      universe[i+13][j+35]= 3; 
      universe[i+14][j+35]= 1; 
      universe[i+15][j+35]= 1; 
      universe[i+16][j+35]= 1; 
      universe[i+17][j+35]= 1; 
      universe[i+18][j+35]= 1; 
      universe[i+19][j+35]= 1; 
      universe[i+20][j+35]= 1; 
      universe[i+21][j+35]= 1; 
      universe[i+22][j+35]= 1; 
      universe[i+23][j+35]= 1; 
      universe[i+24][j+35]= 1; 
      universe[i+25][j+35]= 1; 
      universe[i+26][j+35]= 1; 
      universe[i+27][j+35]= 1; 
      universe[i+28][j+35]= 1; 
      universe[i+29][j+35]= 1; 
      universe[i+30][j+35]= 1; 
      universe[i+31][j+35]= 4; 
      universe[i+32][j+35]= 5; 
      universe[i+33][j+35]= 4; 
      universe[i+34][j+35]= 2; 
      universe[i+35][j+35]= 2; 
      universe[i+36][j+35]= 2; 
      universe[i+37][j+35]= 2; 
      universe[i+38][j+35]= 2; 
      universe[i+39][j+35]= 2; 
      universe[i+40][j+35]= 2; 
      universe[i+41][j+35]= 4; 
      universe[i+42][j+35]= 5; 
      universe[i+43][j+35]= 4; 
      universe[i+44][j+35]= 1; 
      universe[i+45][j+35]= 1; 
      universe[i+46][j+35]= 1; 
      universe[i+47][j+35]= 1; 
      universe[i+48][j+35]= 1; 
      universe[i+49][j+35]= 1; 
      universe[i+50][j+35]= 1; 
      universe[i+51][j+35]= 1; 
      universe[i+52][j+35]= 1; 
      universe[i+53][j+35]= 1; 
      universe[i+54][j+35]= 1; 
      universe[i+55][j+35]= 1; 
      universe[i+56][j+35]= 1; 
      universe[i+57][j+35]= 1; 
      universe[i+58][j+35]= 1; 
      universe[i+59][j+35]= 1; 
      universe[i+60][j+35]= 1; 
      universe[i+61][j+35]= 3; 
      universe[i+62][j+35]= 5; 
      universe[i+63][j+35]= 5; 
      universe[i+64][j+35]= 5; 
      universe[i+65][j+35]= 5; 
      universe[i+66][j+35]= 5; 
      universe[i+67][j+35]= 5; 
      universe[i+68][j+35]= 5; 
      universe[i+69][j+35]= 4; 
      universe[i+70][j+35]= 2; 
      universe[i+71][j+35]= 2; 
      universe[i+72][j+35]= 2; 
      universe[i+73][j+35]= 2; 
      universe[i+74][j+35]= 2; 
      universe[i+0][j+36]= 5; 
      universe[i+1][j+36]= 5; 
      universe[i+2][j+36]= 4; 
      universe[i+3][j+36]= 2; 
      universe[i+4][j+36]= 2; 
      universe[i+5][j+36]= 2; 
      universe[i+6][j+36]= 2; 
      universe[i+7][j+36]= 2; 
      universe[i+8][j+36]= 4; 
      universe[i+9][j+36]= 5; 
      universe[i+10][j+36]= 5; 
      universe[i+11][j+36]= 3; 
      universe[i+12][j+36]= 1; 
      universe[i+13][j+36]= 1; 
      universe[i+14][j+36]= 1; 
      universe[i+15][j+36]= 1; 
      universe[i+16][j+36]= 1; 
      universe[i+17][j+36]= 1; 
      universe[i+18][j+36]= 1; 
      universe[i+19][j+36]= 1; 
      universe[i+20][j+36]= 1; 
      universe[i+21][j+36]= 1; 
      universe[i+22][j+36]= 1; 
      universe[i+23][j+36]= 1; 
      universe[i+24][j+36]= 1; 
      universe[i+25][j+36]= 1; 
      universe[i+26][j+36]= 1; 
      universe[i+27][j+36]= 1; 
      universe[i+28][j+36]= 1; 
      universe[i+29][j+36]= 1; 
      universe[i+30][j+36]= 1; 
      universe[i+31][j+36]= 4; 
      universe[i+32][j+36]= 5; 
      universe[i+33][j+36]= 4; 
      universe[i+34][j+36]= 2; 
      universe[i+35][j+36]= 2; 
      universe[i+36][j+36]= 2; 
      universe[i+37][j+36]= 2; 
      universe[i+38][j+36]= 2; 
      universe[i+39][j+36]= 2; 
      universe[i+40][j+36]= 2; 
      universe[i+41][j+36]= 4; 
      universe[i+42][j+36]= 5; 
      universe[i+43][j+36]= 4; 
      universe[i+44][j+36]= 1; 
      universe[i+45][j+36]= 1; 
      universe[i+46][j+36]= 1; 
      universe[i+47][j+36]= 1; 
      universe[i+48][j+36]= 1; 
      universe[i+49][j+36]= 1; 
      universe[i+50][j+36]= 1; 
      universe[i+51][j+36]= 1; 
      universe[i+52][j+36]= 1; 
      universe[i+53][j+36]= 1; 
      universe[i+54][j+36]= 1; 
      universe[i+55][j+36]= 1; 
      universe[i+56][j+36]= 1; 
      universe[i+57][j+36]= 1; 
      universe[i+58][j+36]= 1; 
      universe[i+59][j+36]= 1; 
      universe[i+60][j+36]= 1; 
      universe[i+61][j+36]= 1; 
      universe[i+62][j+36]= 1; 
      universe[i+63][j+36]= 3; 
      universe[i+64][j+36]= 5; 
      universe[i+65][j+36]= 5; 
      universe[i+66][j+36]= 5; 
      universe[i+67][j+36]= 5; 
      universe[i+68][j+36]= 5; 
      universe[i+69][j+36]= 5; 
      universe[i+70][j+36]= 5; 
      universe[i+71][j+36]= 4; 
      universe[i+72][j+36]= 2; 
      universe[i+73][j+36]= 2; 
      universe[i+74][j+36]= 2; 
      universe[i+0][j+37]= 4; 
      universe[i+1][j+37]= 2; 
      universe[i+2][j+37]= 2; 
      universe[i+3][j+37]= 2; 
      universe[i+4][j+37]= 2; 
      universe[i+5][j+37]= 2; 
      universe[i+6][j+37]= 4; 
      universe[i+7][j+37]= 5; 
      universe[i+8][j+37]= 5; 
      universe[i+9][j+37]= 3; 
      universe[i+10][j+37]= 1; 
      universe[i+11][j+37]= 1; 
      universe[i+12][j+37]= 1; 
      universe[i+13][j+37]= 1; 
      universe[i+14][j+37]= 1; 
      universe[i+15][j+37]= 1; 
      universe[i+16][j+37]= 1; 
      universe[i+17][j+37]= 1; 
      universe[i+18][j+37]= 1; 
      universe[i+19][j+37]= 1; 
      universe[i+20][j+37]= 1; 
      universe[i+21][j+37]= 1; 
      universe[i+22][j+37]= 1; 
      universe[i+23][j+37]= 1; 
      universe[i+24][j+37]= 1; 
      universe[i+25][j+37]= 1; 
      universe[i+26][j+37]= 1; 
      universe[i+27][j+37]= 1; 
      universe[i+28][j+37]= 1; 
      universe[i+29][j+37]= 1; 
      universe[i+30][j+37]= 1; 
      universe[i+31][j+37]= 4; 
      universe[i+32][j+37]= 5; 
      universe[i+33][j+37]= 4; 
      universe[i+34][j+37]= 2; 
      universe[i+35][j+37]= 2; 
      universe[i+36][j+37]= 2; 
      universe[i+37][j+37]= 2; 
      universe[i+38][j+37]= 2; 
      universe[i+39][j+37]= 2; 
      universe[i+40][j+37]= 2; 
      universe[i+41][j+37]= 4; 
      universe[i+42][j+37]= 5; 
      universe[i+43][j+37]= 4; 
      universe[i+44][j+37]= 1; 
      universe[i+45][j+37]= 1; 
      universe[i+46][j+37]= 1; 
      universe[i+47][j+37]= 1; 
      universe[i+48][j+37]= 1; 
      universe[i+49][j+37]= 1; 
      universe[i+50][j+37]= 1; 
      universe[i+51][j+37]= 1; 
      universe[i+52][j+37]= 1; 
      universe[i+53][j+37]= 1; 
      universe[i+54][j+37]= 1; 
      universe[i+55][j+37]= 1; 
      universe[i+56][j+37]= 1; 
      universe[i+57][j+37]= 1; 
      universe[i+58][j+37]= 1; 
      universe[i+59][j+37]= 1; 
      universe[i+60][j+37]= 1; 
      universe[i+61][j+37]= 1; 
      universe[i+62][j+37]= 1; 
      universe[i+63][j+37]= 1; 
      universe[i+64][j+37]= 1; 
      universe[i+65][j+37]= 3; 
      universe[i+66][j+37]= 5; 
      universe[i+67][j+37]= 5; 
      universe[i+68][j+37]= 5; 
      universe[i+69][j+37]= 5; 
      universe[i+70][j+37]= 5; 
      universe[i+71][j+37]= 5; 
      universe[i+72][j+37]= 5; 
      universe[i+73][j+37]= 4; 
      universe[i+74][j+37]= 2; 
*/


      /*
      universe[i+14][j+1]= 1; 
      universe[i+15][j+1]= 1; 
      universe[i+16][j+1]= 1; 
      universe[i+13][j+2]= 1; 
      universe[i+14][j+2]= 3; 
      universe[i+15][j+2]= 3; 
      universe[i+16][j+2]= 4; 
      universe[i+12][j+3]= 1; 
      universe[i+13][j+3]= 3; 
      universe[i+14][j+3]= 3; 
      universe[i+15][j+3]= 3; 
      universe[i+16][j+3]= 3; 
      universe[i+17][j+3]= 4; 
      universe[i+12][j+4]= 1; 
      universe[i+13][j+4]= 3; 
      universe[i+14][j+4]= 3; 
      universe[i+15][j+4]= 3; 
      universe[i+16][j+4]= 3; 
      universe[i+17][j+4]= 3; 
      universe[i+19][j+4]= 2; 
      universe[i+11][j+5]= 1; 
      universe[i+12][j+5]= 3; 
      universe[i+13][j+5]= 3; 
      universe[i+14][j+5]= 3; 
      universe[i+15][j+5]= 3; 
      universe[i+16][j+5]= 3; 
      universe[i+17][j+5]= 3; 
      universe[i+18][j+5]= 4; 
      universe[i+20][j+5]= 2; 
      universe[i+11][j+6]= 1; 
      universe[i+12][j+6]= 3; 
      universe[i+13][j+6]= 3; 
      universe[i+14][j+6]= 3; 
      universe[i+15][j+6]= 3; 
      universe[i+16][j+6]= 3; 
      universe[i+17][j+6]= 3; 
      universe[i+18][j+6]= 3; 
      universe[i+20][j+6]= 2; 
      universe[i+21][j+6]= 2; 
      universe[i+10][j+7]= 1; 
      universe[i+11][j+7]= 3; 
      universe[i+12][j+7]= 3; 
      universe[i+13][j+7]= 3; 
      universe[i+14][j+7]= 3; 
      universe[i+15][j+7]= 3; 
      universe[i+16][j+7]= 3; 
      universe[i+17][j+7]= 3; 
      universe[i+18][j+7]= 3; 
      universe[i+19][j+7]= 4; 
      universe[i+21][j+7]= 2; 
      universe[i+10][j+8]= 1; 
      universe[i+11][j+8]= 3; 
      universe[i+12][j+8]= 3; 
      universe[i+13][j+8]= 3; 
      universe[i+14][j+8]= 3; 
      universe[i+15][j+8]= 3; 
      universe[i+16][j+8]= 3; 
      universe[i+17][j+8]= 3; 
      universe[i+18][j+8]= 3; 
      universe[i+19][j+8]= 3; 
      universe[i+21][j+8]= 2; 
      universe[i+22][j+8]= 2; 
      universe[i+9][j+9]= 1; 
      universe[i+10][j+9]= 3; 
      universe[i+11][j+9]= 3; 
      universe[i+12][j+9]= 3; 
      universe[i+13][j+9]= 3; 
      universe[i+14][j+9]= 3; 
      universe[i+15][j+9]= 3; 
      universe[i+16][j+9]= 3; 
      universe[i+17][j+9]= 3; 
      universe[i+18][j+9]= 3; 
      universe[i+19][j+9]= 3; 
      universe[i+20][j+9]= 4; 
      universe[i+22][j+9]= 2; 
      universe[i+9][j+10]= 1; 
      universe[i+10][j+10]= 3; 
      universe[i+11][j+10]= 3; 
      universe[i+12][j+10]= 3; 
      universe[i+13][j+10]= 4; 
      universe[i+17][j+10]= 4; 
      universe[i+18][j+10]= 3; 
      universe[i+19][j+10]= 3; 
      universe[i+20][j+10]= 3; 
      universe[i+22][j+10]= 2; 
      universe[i+23][j+10]= 2; 
      universe[i+8][j+11]= 1; 
      universe[i+9][j+11]= 3; 
      universe[i+10][j+11]= 3; 
      universe[i+11][j+11]= 3; 
      universe[i+12][j+11]= 3; 
      universe[i+18][j+11]= 3; 
      universe[i+19][j+11]= 3; 
      universe[i+20][j+11]= 3; 
      universe[i+21][j+11]= 4; 
      universe[i+23][j+11]= 2; 
      universe[i+8][j+12]= 1; 
      universe[i+9][j+12]= 3; 
      universe[i+10][j+12]= 3; 
      universe[i+11][j+12]= 3; 
      universe[i+12][j+12]= 3; 
      universe[i+18][j+12]= 3; 
      universe[i+19][j+12]= 3; 
      universe[i+20][j+12]= 3; 
      universe[i+21][j+12]= 3; 
      universe[i+23][j+12]= 2; 
      universe[i+24][j+12]= 2; 
      universe[i+7][j+13]= 1; 
      universe[i+8][j+13]= 3; 
      universe[i+9][j+13]= 3; 
      universe[i+10][j+13]= 3; 
      universe[i+11][j+13]= 3; 
      universe[i+12][j+13]= 3; 
      universe[i+18][j+13]= 3; 
      universe[i+19][j+13]= 3; 
      universe[i+20][j+13]= 3; 
      universe[i+21][j+13]= 3; 
      universe[i+22][j+13]= 4; 
      universe[i+24][j+13]= 2; 
      universe[i+7][j+14]= 1; 
      universe[i+8][j+14]= 3; 
      universe[i+9][j+14]= 3; 
      universe[i+10][j+14]= 3; 
      universe[i+11][j+14]= 3; 
      universe[i+12][j+14]= 3; 
      universe[i+18][j+14]= 3; 
      universe[i+19][j+14]= 3; 
      universe[i+20][j+14]= 3; 
      universe[i+21][j+14]= 3; 
      universe[i+22][j+14]= 3; 
      universe[i+24][j+14]= 2; 
      universe[i+25][j+14]= 2; 
      universe[i+6][j+15]= 1; 
      universe[i+7][j+15]= 3; 
      universe[i+8][j+15]= 3; 
      universe[i+9][j+15]= 3; 
      universe[i+10][j+15]= 3; 
      universe[i+11][j+15]= 3; 
      universe[i+12][j+15]= 3; 
      universe[i+18][j+15]= 3; 
      universe[i+19][j+15]= 3; 
      universe[i+20][j+15]= 3; 
      universe[i+21][j+15]= 3; 
      universe[i+22][j+15]= 3; 
      universe[i+23][j+15]= 4; 
      universe[i+25][j+15]= 2; 
      universe[i+6][j+16]= 1; 
      universe[i+7][j+16]= 3; 
      universe[i+8][j+16]= 3; 
      universe[i+9][j+16]= 3; 
      universe[i+10][j+16]= 3; 
      universe[i+11][j+16]= 3; 
      universe[i+12][j+16]= 3; 
      universe[i+13][j+16]= 1; 
      universe[i+17][j+16]= 1; 
      universe[i+18][j+16]= 3; 
      universe[i+19][j+16]= 3; 
      universe[i+20][j+16]= 3; 
      universe[i+21][j+16]= 3; 
      universe[i+22][j+16]= 3; 
      universe[i+23][j+16]= 3; 
      universe[i+25][j+16]= 2; 
      universe[i+26][j+16]= 2; 
      universe[i+5][j+17]= 1; 
      universe[i+6][j+17]= 3; 
      universe[i+7][j+17]= 3; 
      universe[i+8][j+17]= 3; 
      universe[i+9][j+17]= 3; 
      universe[i+10][j+17]= 3; 
      universe[i+11][j+17]= 3; 
      universe[i+12][j+17]= 3; 
      universe[i+13][j+17]= 4; 
      universe[i+17][j+17]= 4; 
      universe[i+18][j+17]= 3; 
      universe[i+19][j+17]= 3; 
      universe[i+20][j+17]= 3; 
      universe[i+21][j+17]= 3; 
      universe[i+22][j+17]= 3; 
      universe[i+23][j+17]= 3; 
      universe[i+24][j+17]= 4; 
      universe[i+26][j+17]= 2; 
      universe[i+5][j+18]= 1; 
      universe[i+6][j+18]= 3; 
      universe[i+7][j+18]= 3; 
      universe[i+8][j+18]= 3; 
      universe[i+9][j+18]= 3; 
      universe[i+10][j+18]= 3; 
      universe[i+11][j+18]= 3; 
      universe[i+12][j+18]= 3; 
      universe[i+13][j+18]= 3; 
      universe[i+17][j+18]= 3; 
      universe[i+18][j+18]= 3; 
      universe[i+19][j+18]= 3; 
      universe[i+20][j+18]= 3; 
      universe[i+21][j+18]= 3; 
      universe[i+22][j+18]= 3; 
      universe[i+23][j+18]= 3; 
      universe[i+24][j+18]= 3; 
      universe[i+26][j+18]= 2; 
      universe[i+27][j+18]= 2; 
      universe[i+4][j+19]= 1; 
      universe[i+5][j+19]= 3; 
      universe[i+6][j+19]= 3; 
      universe[i+7][j+19]= 3; 
      universe[i+8][j+19]= 3; 
      universe[i+9][j+19]= 3; 
      universe[i+10][j+19]= 3; 
      universe[i+11][j+19]= 3; 
      universe[i+12][j+19]= 3; 
      universe[i+13][j+19]= 3; 
      universe[i+14][j+19]= 1; 
      universe[i+16][j+19]= 1; 
      universe[i+17][j+19]= 3; 
      universe[i+18][j+19]= 3; 
      universe[i+19][j+19]= 3; 
      universe[i+20][j+19]= 3; 
      universe[i+21][j+19]= 3; 
      universe[i+22][j+19]= 3; 
      universe[i+23][j+19]= 3; 
      universe[i+24][j+19]= 3; 
      universe[i+25][j+19]= 4; 
      universe[i+27][j+19]= 2; 
      universe[i+4][j+20]= 1; 
      universe[i+5][j+20]= 3; 
      universe[i+6][j+20]= 3; 
      universe[i+7][j+20]= 3; 
      universe[i+8][j+20]= 3; 
      universe[i+9][j+20]= 3; 
      universe[i+10][j+20]= 3; 
      universe[i+11][j+20]= 3; 
      universe[i+12][j+20]= 3; 
      universe[i+13][j+20]= 3; 
      universe[i+14][j+20]= 4; 
      universe[i+16][j+20]= 4; 
      universe[i+17][j+20]= 3; 
      universe[i+18][j+20]= 3; 
      universe[i+19][j+20]= 3; 
      universe[i+20][j+20]= 3; 
      universe[i+21][j+20]= 3; 
      universe[i+22][j+20]= 3; 
      universe[i+23][j+20]= 3; 
      universe[i+24][j+20]= 3; 
      universe[i+25][j+20]= 3; 
      universe[i+27][j+20]= 2; 
      universe[i+28][j+20]= 2; 
      universe[i+3][j+21]= 1; 
      universe[i+4][j+21]= 3; 
      universe[i+5][j+21]= 3; 
      universe[i+6][j+21]= 3; 
      universe[i+7][j+21]= 3; 
      universe[i+8][j+21]= 3; 
      universe[i+9][j+21]= 3; 
      universe[i+10][j+21]= 3; 
      universe[i+11][j+21]= 3; 
      universe[i+12][j+21]= 3; 
      universe[i+13][j+21]= 3; 
      universe[i+14][j+21]= 3; 
      universe[i+16][j+21]= 3; 
      universe[i+17][j+21]= 3; 
      universe[i+18][j+21]= 3; 
      universe[i+19][j+21]= 3; 
      universe[i+20][j+21]= 3; 
      universe[i+21][j+21]= 3; 
      universe[i+22][j+21]= 3; 
      universe[i+23][j+21]= 3; 
      universe[i+24][j+21]= 3; 
      universe[i+25][j+21]= 3; 
      universe[i+26][j+21]= 4; 
      universe[i+28][j+21]= 2; 
      universe[i+3][j+22]= 1; 
      universe[i+4][j+22]= 3; 
      universe[i+5][j+22]= 3; 
      universe[i+6][j+22]= 3; 
      universe[i+7][j+22]= 3; 
      universe[i+8][j+22]= 3; 
      universe[i+9][j+22]= 3; 
      universe[i+10][j+22]= 3; 
      universe[i+11][j+22]= 3; 
      universe[i+12][j+22]= 3; 
      universe[i+13][j+22]= 3; 
      universe[i+14][j+22]= 3; 
      universe[i+15][j+22]= 3; 
      universe[i+16][j+22]= 3; 
      universe[i+17][j+22]= 3; 
      universe[i+18][j+22]= 3; 
      universe[i+19][j+22]= 3; 
      universe[i+20][j+22]= 3; 
      universe[i+21][j+22]= 3; 
      universe[i+22][j+22]= 3; 
      universe[i+23][j+22]= 3; 
      universe[i+24][j+22]= 3; 
      universe[i+25][j+22]= 3; 
      universe[i+26][j+22]= 3; 
      universe[i+28][j+22]= 2; 
      universe[i+29][j+22]= 2; 
      universe[i+2][j+23]= 1; 
      universe[i+3][j+23]= 3; 
      universe[i+4][j+23]= 3; 
      universe[i+5][j+23]= 3; 
      universe[i+6][j+23]= 3; 
      universe[i+7][j+23]= 3; 
      universe[i+8][j+23]= 3; 
      universe[i+9][j+23]= 3; 
      universe[i+10][j+23]= 3; 
      universe[i+11][j+23]= 3; 
      universe[i+12][j+23]= 3; 
      universe[i+13][j+23]= 3; 
      universe[i+14][j+23]= 4; 
      universe[i+17][j+23]= 4; 
      universe[i+18][j+23]= 3; 
      universe[i+19][j+23]= 3; 
      universe[i+20][j+23]= 3; 
      universe[i+21][j+23]= 3; 
      universe[i+22][j+23]= 3; 
      universe[i+23][j+23]= 3; 
      universe[i+24][j+23]= 3; 
      universe[i+25][j+23]= 3; 
      universe[i+26][j+23]= 3; 
      universe[i+27][j+23]= 4; 
      universe[i+29][j+23]= 2; 
      universe[i+2][j+24]= 1; 
      universe[i+3][j+24]= 3; 
      universe[i+4][j+24]= 3; 
      universe[i+5][j+24]= 3; 
      universe[i+6][j+24]= 3; 
      universe[i+7][j+24]= 3; 
      universe[i+8][j+24]= 3; 
      universe[i+9][j+24]= 3; 
      universe[i+10][j+24]= 3; 
      universe[i+11][j+24]= 3; 
      universe[i+12][j+24]= 3; 
      universe[i+13][j+24]= 3; 
      universe[i+18][j+24]= 3; 
      universe[i+19][j+24]= 3; 
      universe[i+20][j+24]= 3; 
      universe[i+21][j+24]= 3; 
      universe[i+22][j+24]= 3; 
      universe[i+23][j+24]= 3; 
      universe[i+24][j+24]= 3; 
      universe[i+25][j+24]= 3; 
      universe[i+26][j+24]= 3; 
      universe[i+27][j+24]= 3; 
      universe[i+29][j+24]= 2; 
      universe[i+30][j+24]= 2; 
      universe[i+1][j+25]= 1; 
      universe[i+2][j+25]= 3; 
      universe[i+3][j+25]= 3; 
      universe[i+4][j+25]= 3; 
      universe[i+5][j+25]= 3; 
      universe[i+6][j+25]= 3; 
      universe[i+7][j+25]= 3; 
      universe[i+8][j+25]= 3; 
      universe[i+9][j+25]= 3; 
      universe[i+10][j+25]= 3; 
      universe[i+11][j+25]= 3; 
      universe[i+12][j+25]= 3; 
      universe[i+13][j+25]= 3; 
      universe[i+18][j+25]= 3; 
      universe[i+19][j+25]= 3; 
      universe[i+20][j+25]= 3; 
      universe[i+21][j+25]= 3; 
      universe[i+22][j+25]= 3; 
      universe[i+23][j+25]= 3; 
      universe[i+24][j+25]= 3; 
      universe[i+25][j+25]= 3; 
      universe[i+26][j+25]= 3; 
      universe[i+27][j+25]= 3; 
      universe[i+28][j+25]= 4; 
      universe[i+30][j+25]= 2; 
      universe[i+1][j+26]= 1; 
      universe[i+2][j+26]= 3; 
      universe[i+3][j+26]= 3; 
      universe[i+4][j+26]= 3; 
      universe[i+5][j+26]= 3; 
      universe[i+6][j+26]= 3; 
      universe[i+7][j+26]= 3; 
      universe[i+8][j+26]= 3; 
      universe[i+9][j+26]= 3; 
      universe[i+10][j+26]= 3; 
      universe[i+11][j+26]= 3; 
      universe[i+12][j+26]= 3; 
      universe[i+13][j+26]= 3; 
      universe[i+14][j+26]= 4; 
      universe[i+17][j+26]= 4; 
      universe[i+18][j+26]= 3; 
      universe[i+19][j+26]= 3; 
      universe[i+20][j+26]= 3; 
      universe[i+21][j+26]= 3; 
      universe[i+22][j+26]= 3; 
      universe[i+23][j+26]= 3; 
      universe[i+24][j+26]= 3; 
      universe[i+25][j+26]= 3; 
      universe[i+26][j+26]= 3; 
      universe[i+27][j+26]= 3; 
      universe[i+28][j+26]= 3; 
      universe[i+30][j+26]= 2; 
      universe[i+31][j+26]= 2; 
      universe[i+1][j+27]= 1; 
      universe[i+2][j+27]= 3; 
      universe[i+3][j+27]= 3; 
      universe[i+4][j+27]= 3; 
      universe[i+5][j+27]= 3; 
      universe[i+6][j+27]= 3; 
      universe[i+7][j+27]= 3; 
      universe[i+8][j+27]= 3; 
      universe[i+9][j+27]= 3; 
      universe[i+10][j+27]= 3; 
      universe[i+11][j+27]= 3; 
      universe[i+12][j+27]= 3; 
      universe[i+13][j+27]= 3; 
      universe[i+14][j+27]= 3; 
      universe[i+15][j+27]= 3; 
      universe[i+16][j+27]= 3; 
      universe[i+17][j+27]= 3; 
      universe[i+18][j+27]= 3; 
      universe[i+19][j+27]= 3; 
      universe[i+20][j+27]= 3; 
      universe[i+21][j+27]= 3; 
      universe[i+22][j+27]= 3; 
      universe[i+23][j+27]= 3; 
      universe[i+24][j+27]= 3; 
      universe[i+25][j+27]= 3; 
      universe[i+26][j+27]= 3; 
      universe[i+27][j+27]= 3; 
      universe[i+28][j+27]= 3; 
      universe[i+30][j+27]= 2; 
      universe[i+31][j+27]= 2; 
      universe[i+1][j+28]= 1; 
      universe[i+2][j+28]= 3; 
      universe[i+3][j+28]= 3; 
      universe[i+4][j+28]= 3; 
      universe[i+5][j+28]= 3; 
      universe[i+6][j+28]= 3; 
      universe[i+7][j+28]= 3; 
      universe[i+8][j+28]= 3; 
      universe[i+9][j+28]= 3; 
      universe[i+10][j+28]= 3; 
      universe[i+11][j+28]= 3; 
      universe[i+12][j+28]= 3; 
      universe[i+13][j+28]= 3; 
      universe[i+14][j+28]= 3; 
      universe[i+15][j+28]= 3; 
      universe[i+16][j+28]= 3; 
      universe[i+17][j+28]= 3; 
      universe[i+18][j+28]= 3; 
      universe[i+19][j+28]= 3; 
      universe[i+20][j+28]= 3; 
      universe[i+21][j+28]= 3; 
      universe[i+22][j+28]= 3; 
      universe[i+23][j+28]= 3; 
      universe[i+24][j+28]= 3; 
      universe[i+25][j+28]= 3; 
      universe[i+26][j+28]= 3; 
      universe[i+27][j+28]= 3; 
      universe[i+28][j+28]= 4; 
      universe[i+30][j+28]= 2; 
      universe[i+31][j+28]= 2; 
      universe[i+2][j+29]= 1; 
      universe[i+3][j+29]= 3; 
      universe[i+4][j+29]= 3; 
      universe[i+5][j+29]= 3; 
      universe[i+6][j+29]= 3; 
      universe[i+7][j+29]= 3; 
      universe[i+8][j+29]= 3; 
      universe[i+9][j+29]= 3; 
      universe[i+10][j+29]= 3; 
      universe[i+11][j+29]= 3; 
      universe[i+12][j+29]= 3; 
      universe[i+13][j+29]= 3; 
      universe[i+14][j+29]= 3; 
      universe[i+15][j+29]= 3; 
      universe[i+16][j+29]= 3; 
      universe[i+17][j+29]= 3; 
      universe[i+18][j+29]= 3; 
      universe[i+19][j+29]= 3; 
      universe[i+20][j+29]= 3; 
      universe[i+21][j+29]= 3; 
      universe[i+22][j+29]= 3; 
      universe[i+23][j+29]= 3; 
      universe[i+24][j+29]= 3; 
      universe[i+25][j+29]= 3; 
      universe[i+26][j+29]= 3; 
      universe[i+27][j+29]= 4; 
      universe[i+29][j+29]= 2; 
      universe[i+30][j+29]= 2; 
      universe[i+31][j+29]= 2; 
      universe[i+3][j+30]= 1; 
      universe[i+28][j+30]= 2; 
      universe[i+29][j+30]= 2; 
      universe[i+30][j+30]= 2; 
      universe[i+5][j+31]= 2; 
      universe[i+6][j+31]= 2; 
      universe[i+7][j+31]= 2; 
      universe[i+8][j+31]= 2; 
      universe[i+9][j+31]= 2; 
      universe[i+10][j+31]= 2; 
      universe[i+11][j+31]= 2; 
      universe[i+12][j+31]= 2; 
      universe[i+13][j+31]= 2; 
      universe[i+14][j+31]= 2; 
      universe[i+15][j+31]= 2; 
      universe[i+16][j+31]= 2; 
      universe[i+17][j+31]= 2; 
      universe[i+18][j+31]= 2; 
      universe[i+19][j+31]= 2; 
      universe[i+20][j+31]= 2; 
      universe[i+21][j+31]= 2; 
      universe[i+22][j+31]= 2; 
      universe[i+23][j+31]= 2; 
      universe[i+24][j+31]= 2; 
      universe[i+25][j+31]= 2; 
      universe[i+26][j+31]= 2; 
      universe[i+27][j+31]= 2; 
      universe[i+28][j+31]= 2; 
      universe[i+29][j+31]= 2; 

*/
      universe[i+28][j+0]= 12; 
      universe[i+29][j+0]= 10; 
      universe[i+28][j+1]= 12; 
      universe[i+29][j+1]= 1; 
      universe[i+30][j+1]= 4; 
      universe[i+31][j+1]= 10; 
      universe[i+15][j+2]= 4; 
      universe[i+16][j+2]= 2; 
      universe[i+29][j+2]= 12; 
      universe[i+30][j+2]= 2; 
      universe[i+31][j+2]= 3; 
      universe[i+32][j+2]= 6; 
      universe[i+14][j+3]= 1; 
      universe[i+15][j+3]= 3; 
      universe[i+16][j+3]= 10; 
      universe[i+30][j+3]= 10; 
      universe[i+31][j+3]= 1; 
      universe[i+32][j+3]= 3; 
      universe[i+33][j+3]= 6; 
      universe[i+13][j+4]= 4; 
      universe[i+14][j+4]= 13; 
      universe[i+15][j+4]= 5; 
      universe[i+24][j+4]= 12; 
      universe[i+25][j+4]= 10; 
      universe[i+26][j+4]= 10; 
      universe[i+27][j+4]= 12; 
      universe[i+31][j+4]= 2; 
      universe[i+32][j+4]= 5; 
      universe[i+33][j+4]= 3; 
      universe[i+34][j+4]= 6; 
      universe[i+12][j+5]= 10; 
      universe[i+13][j+5]= 13; 
      universe[i+14][j+5]= 1; 
      universe[i+15][j+5]= 3; 
      universe[i+21][j+5]= 10; 
      universe[i+22][j+5]= 4; 
      universe[i+23][j+5]= 13; 
      universe[i+24][j+5]= 13; 
      universe[i+25][j+5]= 1; 
      universe[i+26][j+5]= 1; 
      universe[i+27][j+5]= 3; 
      universe[i+28][j+5]= 5; 
      universe[i+29][j+5]= 8; 
      universe[i+30][j+5]= 10; 
      universe[i+31][j+5]= 13; 
      universe[i+32][j+5]= 5; 
      universe[i+33][j+5]= 3; 
      universe[i+34][j+5]= 5; 
      universe[i+35][j+5]= 10; 
      universe[i+12][j+6]= 4; 
      universe[i+13][j+6]= 13; 
      universe[i+14][j+6]= 3; 
      universe[i+15][j+6]= 3; 
      universe[i+20][j+6]= 4; 
      universe[i+21][j+6]= 13; 
      universe[i+22][j+6]= 13; 
      universe[i+23][j+6]= 1; 
      universe[i+24][j+6]= 1; 
      universe[i+25][j+6]= 1; 
      universe[i+26][j+6]= 1; 
      universe[i+27][j+6]= 1; 
      universe[i+28][j+6]= 3; 
      universe[i+29][j+6]= 3; 
      universe[i+30][j+6]= 1; 
      universe[i+31][j+6]= 1; 
      universe[i+32][j+6]= 1; 
      universe[i+33][j+6]= 3; 
      universe[i+34][j+6]= 5; 
      universe[i+35][j+6]= 5; 
      universe[i+12][j+7]= 13; 
      universe[i+13][j+7]= 13; 
      universe[i+14][j+7]= 1; 
      universe[i+15][j+7]= 5; 
      universe[i+16][j+7]= 2; 
      universe[i+17][j+7]= 10; 
      universe[i+18][j+7]= 12; 
      universe[i+19][j+7]= 2; 
      universe[i+20][j+7]= 13; 
      universe[i+21][j+7]= 13; 
      universe[i+22][j+7]= 1; 
      universe[i+23][j+7]= 1; 
      universe[i+24][j+7]= 1; 
      universe[i+25][j+7]= 1; 
      universe[i+26][j+7]= 5; 
      universe[i+27][j+7]= 5; 
      universe[i+28][j+7]= 3; 
      universe[i+29][j+7]= 5; 
      universe[i+30][j+7]= 3; 
      universe[i+31][j+7]= 3; 
      universe[i+32][j+7]= 3; 
      universe[i+33][j+7]= 3; 
      universe[i+34][j+7]= 5; 
      universe[i+35][j+7]= 5; 
      universe[i+12][j+8]= 13; 
      universe[i+13][j+8]= 13; 
      universe[i+14][j+8]= 1; 
      universe[i+15][j+8]= 3; 
      universe[i+16][j+8]= 5; 
      universe[i+17][j+8]= 3; 
      universe[i+18][j+8]= 3; 
      universe[i+19][j+8]= 3; 
      universe[i+20][j+8]= 1; 
      universe[i+21][j+8]= 1; 
      universe[i+22][j+8]= 1; 
      universe[i+23][j+8]= 1; 
      universe[i+24][j+8]= 1; 
      universe[i+25][j+8]= 3; 
      universe[i+26][j+8]= 5; 
      universe[i+27][j+8]= 5; 
      universe[i+28][j+8]= 5; 
      universe[i+29][j+8]= 3; 
      universe[i+30][j+8]= 6; 
      universe[i+31][j+8]= 10; 
      universe[i+32][j+8]= 5; 
      universe[i+33][j+8]= 3; 
      universe[i+34][j+8]= 5; 
      universe[i+35][j+8]= 5; 
      universe[i+12][j+9]= 13; 
      universe[i+13][j+9]= 13; 
      universe[i+14][j+9]= 13; 
      universe[i+15][j+9]= 1; 
      universe[i+16][j+9]= 3; 
      universe[i+17][j+9]= 3; 
      universe[i+18][j+9]= 5; 
      universe[i+19][j+9]= 5; 
      universe[i+20][j+9]= 1; 
      universe[i+21][j+9]= 1; 
      universe[i+22][j+9]= 1; 
      universe[i+23][j+9]= 1; 
      universe[i+24][j+9]= 2; 
      universe[i+25][j+9]= 5; 
      universe[i+26][j+9]= 6; 
      universe[i+27][j+9]= 11; 
      universe[i+28][j+9]= 12; 
      universe[i+29][j+9]= 5; 
      universe[i+30][j+9]= 5; 
      universe[i+31][j+9]= 12; 
      universe[i+32][j+9]= 11; 
      universe[i+33][j+9]= 5; 
      universe[i+34][j+9]= 5; 
      universe[i+35][j+9]= 6; 
      universe[i+12][j+10]= 10; 
      universe[i+13][j+10]= 13; 
      universe[i+14][j+10]= 13; 
      universe[i+15][j+10]= 13; 
      universe[i+16][j+10]= 1; 
      universe[i+17][j+10]= 3; 
      universe[i+18][j+10]= 3; 
      universe[i+19][j+10]= 1; 
      universe[i+20][j+10]= 1; 
      universe[i+21][j+10]= 1; 
      universe[i+22][j+10]= 1; 
      universe[i+23][j+10]= 1; 
      universe[i+24][j+10]= 3; 
      universe[i+25][j+10]= 3; 
      universe[i+26][j+10]= 10; 
      universe[i+27][j+10]= 11; 
      universe[i+28][j+10]= 12; 
      universe[i+29][j+10]= 11; 
      universe[i+30][j+10]= 3; 
      universe[i+31][j+10]= 11; 
      universe[i+32][j+10]= 12; 
      universe[i+33][j+10]= 11; 
      universe[i+34][j+10]= 3; 
      universe[i+35][j+10]= 10; 
      universe[i+13][j+11]= 13; 
      universe[i+14][j+11]= 13; 
      universe[i+15][j+11]= 13; 
      universe[i+16][j+11]= 13; 
      universe[i+17][j+11]= 1; 
      universe[i+18][j+11]= 1; 
      universe[i+19][j+11]= 1; 
      universe[i+20][j+11]= 1; 
      universe[i+21][j+11]= 1; 
      universe[i+22][j+11]= 1; 
      universe[i+23][j+11]= 3; 
      universe[i+24][j+11]= 3; 
      universe[i+25][j+11]= 3; 
      universe[i+26][j+11]= 11; 
      universe[i+27][j+11]= 12; 
      universe[i+28][j+11]= 12; 
      universe[i+29][j+11]= 12; 
      universe[i+30][j+11]= 8; 
      universe[i+31][j+11]= 9; 
      universe[i+32][j+11]= 12; 
      universe[i+33][j+11]= 12; 
      universe[i+34][j+11]= 5; 
      universe[i+35][j+11]= 6; 
      universe[i+14][j+12]= 13; 
      universe[i+15][j+12]= 13; 
      universe[i+16][j+12]= 13; 
      universe[i+17][j+12]= 13; 
      universe[i+18][j+12]= 13; 
      universe[i+19][j+12]= 13; 
      universe[i+20][j+12]= 1; 
      universe[i+21][j+12]= 1; 
      universe[i+22][j+12]= 1; 
      universe[i+23][j+12]= 3; 
      universe[i+24][j+12]= 3; 
      universe[i+25][j+12]= 5; 
      universe[i+26][j+12]= 11; 
      universe[i+27][j+12]= 12; 
      universe[i+28][j+12]= 12; 
      universe[i+29][j+12]= 12; 
      universe[i+30][j+12]= 9; 
      universe[i+31][j+12]= 5; 
      universe[i+32][j+12]= 10; 
      universe[i+33][j+12]= 8; 
      universe[i+34][j+12]= 8; 
      universe[i+35][j+12]= 6; 
      universe[i+15][j+13]= 13; 
      universe[i+16][j+13]= 13; 
      universe[i+17][j+13]= 13; 
      universe[i+18][j+13]= 13; 
      universe[i+19][j+13]= 13; 
      universe[i+20][j+13]= 13; 
      universe[i+21][j+13]= 1; 
      universe[i+22][j+13]= 1; 
      universe[i+23][j+13]= 3; 
      universe[i+24][j+13]= 3; 
      universe[i+25][j+13]= 5; 
      universe[i+26][j+13]= 11; 
      universe[i+27][j+13]= 12; 
      universe[i+28][j+13]= 12; 
      universe[i+29][j+13]= 1; 
      universe[i+30][j+13]= 8; 
      universe[i+31][j+13]= 1; 
      universe[i+32][j+13]= 8; 
      universe[i+33][j+13]= 2; 
      universe[i+34][j+13]= 4; 
      universe[i+35][j+13]= 5; 
      universe[i+16][j+14]= 4; 
      universe[i+17][j+14]= 13; 
      universe[i+18][j+14]= 13; 
      universe[i+19][j+14]= 13; 
      universe[i+20][j+14]= 13; 
      universe[i+21][j+14]= 1; 
      universe[i+22][j+14]= 3; 
      universe[i+23][j+14]= 3; 
      universe[i+24][j+14]= 3; 
      universe[i+25][j+14]= 3; 
      universe[i+26][j+14]= 11; 
      universe[i+27][j+14]= 12; 
      universe[i+28][j+14]= 11; 
      universe[i+29][j+14]= 13; 
      universe[i+30][j+14]= 2; 
      universe[i+31][j+14]= 13; 
      universe[i+32][j+14]= 3; 
      universe[i+33][j+14]= 13; 
      universe[i+34][j+14]= 13; 
      universe[i+35][j+14]= 3; 
      universe[i+36][j+14]= 11; 
      universe[i+17][j+15]= 4; 
      universe[i+18][j+15]= 13; 
      universe[i+19][j+15]= 13; 
      universe[i+20][j+15]= 13; 
      universe[i+21][j+15]= 1; 
      universe[i+22][j+15]= 3; 
      universe[i+23][j+15]= 5; 
      universe[i+24][j+15]= 3; 
      universe[i+25][j+15]= 5; 
      universe[i+26][j+15]= 10; 
      universe[i+27][j+15]= 12; 
      universe[i+28][j+15]= 11; 
      universe[i+29][j+15]= 13; 
      universe[i+30][j+15]= 13; 
      universe[i+31][j+15]= 13; 
      universe[i+32][j+15]= 3; 
      universe[i+33][j+15]= 13; 
      universe[i+34][j+15]= 1; 
      universe[i+35][j+15]= 5; 
      universe[i+36][j+15]= 6; 
      universe[i+17][j+16]= 13; 
      universe[i+18][j+16]= 13; 
      universe[i+19][j+16]= 13; 
      universe[i+20][j+16]= 13; 
      universe[i+21][j+16]= 1; 
      universe[i+22][j+16]= 1; 
      universe[i+23][j+16]= 3; 
      universe[i+24][j+16]= 3; 
      universe[i+25][j+16]= 3; 
      universe[i+26][j+16]= 4; 
      universe[i+27][j+16]= 11; 
      universe[i+28][j+16]= 12; 
      universe[i+29][j+16]= 13; 
      universe[i+30][j+16]= 13; 
      universe[i+31][j+16]= 1; 
      universe[i+32][j+16]= 3; 
      universe[i+33][j+16]= 3; 
      universe[i+34][j+16]= 3; 
      universe[i+35][j+16]= 5; 
      universe[i+36][j+16]= 5; 
      universe[i+37][j+16]= 6; 
      universe[i+17][j+17]= 4; 
      universe[i+18][j+17]= 13; 
      universe[i+19][j+17]= 13; 
      universe[i+20][j+17]= 13; 
      universe[i+21][j+17]= 1; 
      universe[i+22][j+17]= 1; 
      universe[i+23][j+17]= 3; 
      universe[i+24][j+17]= 3; 
      universe[i+25][j+17]= 3; 
      universe[i+26][j+17]= 2; 
      universe[i+27][j+17]= 8; 
      universe[i+28][j+17]= 11; 
      universe[i+29][j+17]= 10; 
      universe[i+30][j+17]= 13; 
      universe[i+31][j+17]= 3; 
      universe[i+32][j+17]= 5; 
      universe[i+33][j+17]= 6; 
      universe[i+34][j+17]= 5; 
      universe[i+35][j+17]= 3; 
      universe[i+36][j+17]= 5; 
      universe[i+37][j+17]= 5; 
      universe[i+40][j+17]= 11; 
      universe[i+41][j+17]= 10; 
      universe[i+17][j+18]= 10; 
      universe[i+18][j+18]= 13; 
      universe[i+19][j+18]= 13; 
      universe[i+20][j+18]= 13; 
      universe[i+21][j+18]= 1; 
      universe[i+22][j+18]= 1; 
      universe[i+23][j+18]= 1; 
      universe[i+24][j+18]= 3; 
      universe[i+25][j+18]= 3; 
      universe[i+26][j+18]= 3; 
      universe[i+27][j+18]= 5; 
      universe[i+28][j+18]= 3; 
      universe[i+29][j+18]= 3; 
      universe[i+30][j+18]= 1; 
      universe[i+31][j+18]= 1; 
      universe[i+32][j+18]= 3; 
      universe[i+33][j+18]= 5; 
      universe[i+34][j+18]= 5; 
      universe[i+35][j+18]= 5; 
      universe[i+36][j+18]= 5; 
      universe[i+37][j+18]= 5; 
      universe[i+39][j+18]= 8; 
      universe[i+40][j+18]= 9; 
      universe[i+41][j+18]= 11; 
      universe[i+18][j+19]= 13; 
      universe[i+19][j+19]= 13; 
      universe[i+20][j+19]= 13; 
      universe[i+21][j+19]= 13; 
      universe[i+22][j+19]= 1; 
      universe[i+23][j+19]= 3; 
      universe[i+24][j+19]= 5; 
      universe[i+25][j+19]= 5; 
      universe[i+26][j+19]= 5; 
      universe[i+27][j+19]= 5; 
      universe[i+28][j+19]= 5; 
      universe[i+29][j+19]= 3; 
      universe[i+30][j+19]= 3; 
      universe[i+31][j+19]= 3; 
      universe[i+32][j+19]= 5; 
      universe[i+33][j+19]= 3; 
      universe[i+34][j+19]= 5; 
      universe[i+35][j+19]= 3; 
      universe[i+36][j+19]= 5; 
      universe[i+37][j+19]= 10; 
      universe[i+38][j+19]= 10; 
      universe[i+39][j+19]= 8; 
      universe[i+40][j+19]= 8; 
      universe[i+18][j+20]= 10; 
      universe[i+19][j+20]= 13; 
      universe[i+20][j+20]= 13; 
      universe[i+21][j+20]= 13; 
      universe[i+22][j+20]= 1; 
      universe[i+23][j+20]= 1; 
      universe[i+24][j+20]= 3; 
      universe[i+25][j+20]= 3; 
      universe[i+26][j+20]= 3; 
      universe[i+27][j+20]= 3; 
      universe[i+28][j+20]= 5; 
      universe[i+29][j+20]= 5; 
      universe[i+30][j+20]= 3; 
      universe[i+31][j+20]= 3; 
      universe[i+32][j+20]= 3; 
      universe[i+33][j+20]= 3; 
      universe[i+34][j+20]= 1; 
      universe[i+35][j+20]= 6; 
      universe[i+36][j+20]= 6; 
      universe[i+37][j+20]= 12; 
      universe[i+38][j+20]= 8; 
      universe[i+41][j+20]= 10; 
      universe[i+42][j+20]= 9; 
      universe[i+43][j+20]= 11;  
      universe[i+19][j+21]= 4; 
      universe[i+20][j+21]= 13; 
      universe[i+21][j+21]= 13; 
      universe[i+22][j+21]= 13; 
      universe[i+23][j+21]= 1; 
      universe[i+24][j+21]= 1; 
      universe[i+25][j+21]= 1; 
      universe[i+26][j+21]= 1; 
      universe[i+27][j+21]= 3; 
      universe[i+28][j+21]= 3; 
      universe[i+29][j+21]= 3; 
      universe[i+30][j+21]= 3; 
      universe[i+31][j+21]= 3; 
      universe[i+32][j+21]= 3; 
      universe[i+33][j+21]= 1; 
      universe[i+34][j+21]= 4; 
      universe[i+35][j+21]= 10; 
      universe[i+37][j+21]= 11; 
      universe[i+38][j+21]= 9; 
      universe[i+40][j+21]= 9; 
      universe[i+41][j+21]= 8; 
      universe[i+42][j+21]= 8; 
      universe[i+20][j+22]= 4; 
      universe[i+21][j+22]= 13; 
      universe[i+22][j+22]= 13; 
      universe[i+23][j+22]= 1; 
      universe[i+24][j+22]= 5; 
      universe[i+25][j+22]= 5; 
      universe[i+26][j+22]= 3; 
      universe[i+27][j+22]= 1; 
      universe[i+28][j+22]= 1; 
      universe[i+29][j+22]= 3; 
      universe[i+30][j+22]= 3; 
      universe[i+31][j+22]= 3; 
      universe[i+32][j+22]= 1; 
      universe[i+33][j+22]= 10; 
      universe[i+38][j+22]= 8; 
      universe[i+39][j+22]= 9; 
      universe[i+40][j+22]= 8; 
      universe[i+41][j+22]= 12; 
      universe[i+42][j+22]= 12; 
      universe[i+45][j+22]= 12; 
      universe[i+46][j+22]= 11; 
      universe[i+21][j+23]= 4; 
      universe[i+22][j+23]= 13; 
      universe[i+23][j+23]= 1; 
      universe[i+24][j+23]= 1; 
      universe[i+25][j+23]= 1; 
      universe[i+26][j+23]= 1; 
      universe[i+27][j+23]= 5; 
      universe[i+28][j+23]= 3; 
      universe[i+29][j+23]= 3; 
      universe[i+30][j+23]= 1; 
      universe[i+31][j+23]= 4; 
      universe[i+32][j+23]= 11; 
      universe[i+33][j+23]= 12; 
      universe[i+34][j+23]= 5; 
      universe[i+35][j+23]= 5; 
      universe[i+36][j+23]= 8; 
      universe[i+37][j+23]= 10; 
      universe[i+38][j+23]= 8; 
      universe[i+39][j+23]= 8; 
      universe[i+40][j+23]= 8; 
      universe[i+43][j+23]= 10; 
      universe[i+44][j+23]= 7; 
      universe[i+45][j+23]= 8; 
      universe[i+21][j+24]= 4; 
      universe[i+22][j+24]= 13; 
      universe[i+23][j+24]= 1; 
      universe[i+24][j+24]= 3; 
      universe[i+25][j+24]= 3; 
      universe[i+26][j+24]= 3; 
      universe[i+27][j+24]= 1; 
      universe[i+28][j+24]= 1; 
      universe[i+29][j+24]= 1; 
      universe[i+30][j+24]= 10; 
      universe[i+33][j+24]= 10; 
      universe[i+34][j+24]= 5; 
      universe[i+35][j+24]= 5; 
      universe[i+36][j+24]= 5; 
      universe[i+37][j+24]= 8; 
      universe[i+38][j+24]= 8; 
      universe[i+39][j+24]= 10; 
      universe[i+40][j+24]= 8; 
      universe[i+41][j+24]= 8; 
      universe[i+42][j+24]= 9; 
      universe[i+43][j+24]= 7; 
      universe[i+44][j+24]= 8; 
      universe[i+21][j+25]= 13; 
      universe[i+22][j+25]= 13; 
      universe[i+23][j+25]= 5; 
      universe[i+24][j+25]= 3; 
      universe[i+25][j+25]= 3; 
      universe[i+26][j+25]= 3; 
      universe[i+27][j+25]= 3; 
      universe[i+28][j+25]= 3; 
      universe[i+29][j+25]= 3; 
      universe[i+30][j+25]= 10; 
      universe[i+32][j+25]= 12; 
      universe[i+33][j+25]= 10; 
      universe[i+34][j+25]= 1; 
      universe[i+35][j+25]= 6; 
      universe[i+36][j+25]= 3; 
      universe[i+37][j+25]= 8; 
      universe[i+41][j+25]= 11; 
      universe[i+42][j+25]= 8; 
      universe[i+43][j+25]= 11; 
      universe[i+20][j+26]= 10; 
      universe[i+21][j+26]= 13; 
      universe[i+22][j+26]= 13; 
      universe[i+23][j+26]= 3; 
      universe[i+24][j+26]= 3; 
      universe[i+25][j+26]= 5; 
      universe[i+26][j+26]= 5; 
      universe[i+27][j+26]= 5; 
      universe[i+28][j+26]= 5; 
      universe[i+29][j+26]= 5; 
      universe[i+30][j+26]= 5; 
      universe[i+31][j+26]= 4; 
      universe[i+32][j+26]= 13; 
      universe[i+33][j+26]= 2; 
      universe[i+34][j+26]= 1; 
      universe[i+35][j+26]= 5; 
      universe[i+36][j+26]= 6; 
      universe[i+37][j+26]= 3; 
      universe[i+38][j+26]= 11; 
      universe[i+20][j+27]= 4; 
      universe[i+21][j+27]= 13; 
      universe[i+22][j+27]= 13; 
      universe[i+23][j+27]= 5; 
      universe[i+24][j+27]= 5; 
      universe[i+25][j+27]= 3; 
      universe[i+26][j+27]= 3; 
      universe[i+27][j+27]= 3; 
      universe[i+28][j+27]= 5; 
      universe[i+29][j+27]= 5; 
      universe[i+30][j+27]= 5; 
      universe[i+31][j+27]= 1; 
      universe[i+32][j+27]= 4; 
      universe[i+33][j+27]= 2; 
      universe[i+34][j+27]= 1; 
      universe[i+35][j+27]= 6; 
      universe[i+36][j+27]= 5; 
      universe[i+37][j+27]= 5; 
      universe[i+38][j+27]= 11; 
      universe[i+20][j+28]= 13; 
      universe[i+21][j+28]= 13; 
      universe[i+22][j+28]= 13; 
      universe[i+23][j+28]= 2; 
      universe[i+24][j+28]= 1; 
      universe[i+25][j+28]= 1; 
      universe[i+26][j+28]= 1; 
      universe[i+27][j+28]= 5; 
      universe[i+28][j+28]= 5; 
      universe[i+29][j+28]= 5; 
      universe[i+30][j+28]= 5; 
      universe[i+31][j+28]= 1; 
      universe[i+32][j+28]= 13; 
      universe[i+33][j+28]= 4; 
      universe[i+34][j+28]= 4; 
      universe[i+35][j+28]= 1; 
      universe[i+36][j+28]= 3; 
      universe[i+37][j+28]= 6; 
      universe[i+20][j+29]= 13; 
      universe[i+21][j+29]= 13; 
      universe[i+22][j+29]= 13; 
      universe[i+23][j+29]= 2; 
      universe[i+24][j+29]= 4; 
      universe[i+25][j+29]= 2; 
      universe[i+26][j+29]= 5; 
      universe[i+27][j+29]= 5; 
      universe[i+28][j+29]= 5; 
      universe[i+29][j+29]= 5; 
      universe[i+30][j+29]= 5; 
      universe[i+31][j+29]= 2; 
      universe[i+32][j+29]= 4; 
      universe[i+33][j+29]= 1; 
      universe[i+34][j+29]= 3; 
      universe[i+35][j+29]= 3; 
      universe[i+36][j+29]= 6; 
      universe[i+20][j+30]= 13; 
      universe[i+21][j+30]= 13; 
      universe[i+22][j+30]= 13; 
      universe[i+23][j+30]= 13; 
      universe[i+24][j+30]= 4; 
      universe[i+25][j+30]= 2; 
      universe[i+26][j+30]= 4; 
      universe[i+27][j+30]= 5; 
      universe[i+28][j+30]= 5; 
      universe[i+29][j+30]= 6; 
      universe[i+30][j+30]= 5; 
      universe[i+31][j+30]= 3; 
      universe[i+32][j+30]= 1; 
      universe[i+33][j+30]= 2; 
      universe[i+34][j+30]= 10; 
      universe[i+35][j+30]= 12; 
      universe[i+20][j+31]= 13; 
      universe[i+21][j+31]= 13; 
      universe[i+22][j+31]= 13; 
      universe[i+23][j+31]= 13; 
      universe[i+24][j+31]= 1; 
      universe[i+25][j+31]= 2; 
      universe[i+26][j+31]= 1; 
      universe[i+27][j+31]= 4; 
      universe[i+28][j+31]= 5; 
      universe[i+29][j+31]= 5; 
      universe[i+30][j+31]= 3; 
      universe[i+31][j+31]= 5; 
      universe[i+32][j+31]= 2; 
      universe[i+33][j+31]= 10; 
      universe[i+20][j+32]= 4; 
      universe[i+21][j+32]= 13; 
      universe[i+22][j+32]= 13; 
      universe[i+23][j+32]= 13; 
      universe[i+24][j+32]= 13; 
      universe[i+25][j+32]= 2; 
      universe[i+26][j+32]= 2; 
      universe[i+27][j+32]= 2; 
      universe[i+28][j+32]= 2; 
      universe[i+29][j+32]= 4; 
      universe[i+30][j+32]= 2; 
      universe[i+31][j+32]= 2; 
      universe[i+32][j+32]= 1; 
      universe[i+20][j+33]= 4; 
      universe[i+21][j+33]= 13; 
      universe[i+22][j+33]= 13; 
      universe[i+23][j+33]= 13; 
      universe[i+24][j+33]= 13; 
      universe[i+25][j+33]= 13; 
      universe[i+26][j+33]= 13; 
      universe[i+27][j+33]= 2; 
      universe[i+28][j+33]= 1; 
      universe[i+29][j+33]= 1; 
      universe[i+30][j+33]= 1; 
      universe[i+31][j+33]= 1; 
      universe[i+32][j+33]= 3; 
      universe[i+33][j+33]= 12; 
      universe[i+20][j+34]= 13; 
      universe[i+21][j+34]= 13; 
      universe[i+22][j+34]= 13; 
      universe[i+23][j+34]= 5; 
      universe[i+24][j+34]= 7; 
      universe[i+25][j+34]= 2; 
      universe[i+26][j+34]= 13; 
      universe[i+27][j+34]= 13; 
      universe[i+28][j+34]= 13; 
      universe[i+29][j+34]= 1; 
      universe[i+30][j+34]= 3; 
      universe[i+31][j+34]= 5; 
      universe[i+32][j+34]= 3; 
      universe[i+33][j+34]= 12; 
      universe[i+19][j+35]= 10; 
      universe[i+20][j+35]= 13; 
      universe[i+21][j+35]= 2; 
      universe[i+22][j+35]= 8; 
      universe[i+23][j+35]= 7; 
      universe[i+24][j+35]= 2; 
      universe[i+25][j+35]= 2; 
      universe[i+26][j+35]= 2; 
      universe[i+27][j+35]= 1; 
      universe[i+28][j+35]= 3; 
      universe[i+29][j+35]= 3; 
      universe[i+30][j+35]= 5; 
      universe[i+31][j+35]= 5; 
      universe[i+32][j+35]= 3; 
      universe[i+19][j+36]= 4; 
      universe[i+20][j+36]= 13; 
      universe[i+21][j+36]= 2; 
      universe[i+22][j+36]= 2; 
      universe[i+23][j+36]= 4; 
      universe[i+24][j+36]= 1; 
      universe[i+25][j+36]= 1; 
      universe[i+26][j+36]= 1; 
      universe[i+27][j+36]= 3; 
      universe[i+28][j+36]= 3; 
      universe[i+29][j+36]= 5; 
      universe[i+30][j+36]= 5; 
      universe[i+31][j+36]= 5; 
      universe[i+32][j+36]= 6; 
      universe[i+18][j+37]= 10; 
      universe[i+19][j+37]= 13; 
      universe[i+20][j+37]= 13; 
      universe[i+21][j+37]= 13; 
      universe[i+22][j+37]= 13; 
      universe[i+23][j+37]= 13; 
      universe[i+24][j+37]= 13; 
      universe[i+25][j+37]= 1; 
      universe[i+26][j+37]= 1; 
      universe[i+27][j+37]= 1; 
      universe[i+28][j+37]= 3; 
      universe[i+29][j+37]= 3; 
      universe[i+30][j+37]= 3; 
      universe[i+31][j+37]= 1; 
      universe[i+17][j+38]= 10; 
      universe[i+18][j+38]= 13; 
      universe[i+19][j+38]= 13; 
      universe[i+20][j+38]= 13; 
      universe[i+21][j+38]= 13; 
      universe[i+22][j+38]= 13; 
      universe[i+23][j+38]= 13; 
      universe[i+24][j+38]= 13; 
      universe[i+25][j+38]= 13; 
      universe[i+26][j+38]= 13; 
      universe[i+27][j+38]= 1; 
      universe[i+28][j+38]= 13; 
      universe[i+29][j+38]= 1; 
      universe[i+30][j+38]= 1; 
      universe[i+31][j+38]= 2; 
      universe[i+15][j+39]= 10; 
      universe[i+16][j+39]= 4; 
      universe[i+17][j+39]= 13; 
      universe[i+18][j+39]= 13; 
      universe[i+19][j+39]= 13; 
      universe[i+20][j+39]= 4; 
      universe[i+21][j+39]= 2; 
      universe[i+22][j+39]= 1; 
      universe[i+23][j+39]= 2; 
      universe[i+24][j+39]= 1; 
      universe[i+25][j+39]= 1; 
      universe[i+26][j+39]= 2; 
      universe[i+27][j+39]= 1; 
      universe[i+28][j+39]= 1; 
      universe[i+29][j+39]= 5; 
      universe[i+30][j+39]= 6; 
      universe[i+31][j+39]= 6; 
      universe[i+12][j+40]= 10; 
      universe[i+13][j+40]= 10; 
      universe[i+14][j+40]= 2; 
      universe[i+15][j+40]= 13; 
      universe[i+16][j+40]= 13; 
      universe[i+17][j+40]= 13; 
      universe[i+18][j+40]= 13; 
      universe[i+19][j+40]= 10; 
      universe[i+20][j+40]= 10; 
      universe[i+21][j+40]= 13; 
      universe[i+22][j+40]= 6; 
      universe[i+23][j+40]= 2; 
      universe[i+24][j+40]= 1; 
      universe[i+25][j+40]= 6; 
      universe[i+26][j+40]= 6; 
      universe[i+27][j+40]= 6; 
      universe[i+28][j+40]= 2; 
      universe[i+29][j+40]= 6; 
      universe[i+30][j+40]= 6; 
      universe[i+31][j+40]= 4; 
      universe[i+32][j+40]= 8; 
      universe[i+33][j+40]= 12; 
      universe[i+7][j+41]= 10; 
      universe[i+8][j+41]= 10; 
      universe[i+9][j+41]= 8; 
      universe[i+10][j+41]= 2; 
      universe[i+11][j+41]= 13; 
      universe[i+12][j+41]= 1; 
      universe[i+13][j+41]= 2; 
      universe[i+14][j+41]= 13; 
      universe[i+15][j+41]= 13; 
      universe[i+16][j+41]= 8; 
      universe[i+17][j+41]= 10; 
      universe[i+20][j+41]= 4; 
      universe[i+21][j+41]= 1; 
      universe[i+22][j+41]= 4; 
      universe[i+23][j+41]= 2; 
      universe[i+24][j+41]= 2; 
      universe[i+25][j+41]= 3; 
      universe[i+26][j+41]= 1; 
      universe[i+27][j+41]= 4; 
      universe[i+28][j+41]= 1; 
      universe[i+29][j+41]= 2; 
      universe[i+30][j+41]= 5; 
      universe[i+31][j+41]= 6; 
      universe[i+32][j+41]= 6; 
      universe[i+33][j+41]= 6; 
      universe[i+34][j+41]= 6; 
      universe[i+35][j+41]= 2; 
      universe[i+36][j+41]= 4; 
      universe[i+37][j+41]= 10; 
      universe[i+38][j+41]= 10; 
      universe[i+39][j+41]= 10; 
      universe[i+40][j+41]= 12; 
      universe[i+4][j+42]= 10; 
      universe[i+5][j+42]= 4; 
      universe[i+6][j+42]= 1; 
      universe[i+7][j+42]= 1; 
      universe[i+8][j+42]= 1; 
      universe[i+9][j+42]= 13; 
      universe[i+10][j+42]= 13; 
      universe[i+11][j+42]= 1; 
      universe[i+12][j+42]= 4; 
      universe[i+13][j+42]= 10; 
      universe[i+14][j+42]= 10; 
      universe[i+20][j+42]= 1; 
      universe[i+21][j+42]= 2; 
      universe[i+22][j+42]= 2; 
      universe[i+23][j+42]= 2; 
      universe[i+24][j+42]= 2; 
      universe[i+25][j+42]= 2; 
      universe[i+26][j+42]= 2; 
      universe[i+27][j+42]= 1; 
      universe[i+28][j+42]= 5; 
      universe[i+29][j+42]= 2; 
      universe[i+30][j+42]= 4; 
      universe[i+31][j+42]= 8; 
      universe[i+32][j+42]= 11; 
      universe[i+33][j+42]= 11; 
      universe[i+34][j+42]= 8; 
      universe[i+35][j+42]= 4; 
      universe[i+36][j+42]= 10; 
      universe[i+37][j+42]= 12; 
      universe[i+38][j+42]= 12; 
      universe[i+39][j+42]= 12; 
      universe[i+40][j+42]= 12; 
      universe[i+41][j+42]= 11; 
      universe[i+42][j+42]= 10; 
      universe[i+3][j+43]= 4; 
      universe[i+4][j+43]= 1; 
      universe[i+5][j+43]= 1; 
      universe[i+6][j+43]= 6; 
      universe[i+7][j+43]= 10; 
      universe[i+8][j+43]= 11; 
      universe[i+9][j+43]= 12; 
      universe[i+19][j+43]= 12; 
      universe[i+20][j+43]= 4; 
      universe[i+21][j+43]= 4; 
      universe[i+22][j+43]= 4; 
      universe[i+23][j+43]= 2; 
      universe[i+24][j+43]= 2; 
      universe[i+25][j+43]= 2; 
      universe[i+26][j+43]= 2; 
      universe[i+27][j+43]= 5; 
      universe[i+28][j+43]= 5; 
      universe[i+29][j+43]= 4; 
      universe[i+30][j+43]= 10; 
      universe[i+31][j+43]= 12; 
      universe[i+32][j+43]= 12; 
      universe[i+33][j+43]= 12; 
      universe[i+34][j+43]= 12; 
      universe[i+35][j+43]= 12; 
      universe[i+36][j+43]= 6; 
      universe[i+37][j+43]= 10; 
      universe[i+38][j+43]= 11; 
      universe[i+39][j+43]= 11; 
      universe[i+40][j+43]= 11; 
      universe[i+41][j+43]= 11; 
      universe[i+42][j+43]= 11; 
      universe[i+43][j+43]= 12; 
      universe[i+2][j+44]= 4; 
      universe[i+3][j+44]= 1; 
      universe[i+4][j+44]= 5; 
      universe[i+20][j+44]= 10; 
      universe[i+21][j+44]= 4; 
      universe[i+22][j+44]= 4; 
      universe[i+23][j+44]= 2; 
      universe[i+24][j+44]= 4; 
      universe[i+25][j+44]= 2; 
      universe[i+26][j+44]= 1; 
      universe[i+27][j+44]= 2; 
      universe[i+28][j+44]= 2; 
      universe[i+29][j+44]= 4; 
      universe[i+30][j+44]= 8; 
      universe[i+31][j+44]= 11; 
      universe[i+32][j+44]= 11; 
      universe[i+33][j+44]= 11; 
      universe[i+34][j+44]= 11; 
      universe[i+35][j+44]= 11; 
      universe[i+36][j+44]= 11; 
      universe[i+37][j+44]= 4; 
      universe[i+38][j+44]= 11; 
      universe[i+39][j+44]= 12; 
      universe[i+40][j+44]= 12; 
      universe[i+41][j+44]= 11; 
      universe[i+42][j+44]= 10; 
      universe[i+2][j+45]= 4; 
      universe[i+3][j+45]= 13; 
      universe[i+4][j+45]= 1; 
      universe[i+5][j+45]= 11; 
      universe[i+8][j+45]= 6; 
      universe[i+9][j+45]= 3; 
      universe[i+10][j+45]= 10; 
      universe[i+22][j+45]= 10; 
      universe[i+23][j+45]= 4; 
      universe[i+24][j+45]= 4; 
      universe[i+25][j+45]= 4; 
      universe[i+26][j+45]= 4; 
      universe[i+27][j+45]= 4; 
      universe[i+28][j+45]= 4; 
      universe[i+29][j+45]= 4; 
      universe[i+30][j+45]= 4; 
      universe[i+31][j+45]= 10; 
      universe[i+32][j+45]= 11; 
      universe[i+33][j+45]= 11; 
      universe[i+34][j+45]= 11; 
      universe[i+35][j+45]= 12; 
      universe[i+36][j+45]= 10; 
      universe[i+37][j+45]= 10; 
      universe[i+38][j+45]= 12; 
      universe[i+39][j+45]= 12; 
      universe[i+40][j+45]= 11; 
      universe[i+41][j+45]= 11; 
      universe[i+3][j+46]= 13; 
      universe[i+4][j+46]= 13; 
      universe[i+5][j+46]= 1; 
      universe[i+6][j+46]= 1; 
      universe[i+7][j+46]= 6; 
      universe[i+8][j+46]= 3; 
      universe[i+9][j+46]= 3; 
      universe[i+10][j+46]= 3; 
      universe[i+11][j+46]= 6; 
      universe[i+12][j+46]= 11; 
      universe[i+24][j+46]= 12; 
      universe[i+25][j+46]= 10; 
      universe[i+26][j+46]= 10; 
      universe[i+27][j+46]= 10; 
      universe[i+28][j+46]= 4; 
      universe[i+29][j+46]= 4; 
      universe[i+30][j+46]= 4; 
      universe[i+31][j+46]= 10; 
      universe[i+32][j+46]= 11; 
      universe[i+33][j+46]= 11; 
      universe[i+34][j+46]= 11; 
      universe[i+35][j+46]= 12; 
      universe[i+4][j+47]= 10; 
      universe[i+5][j+47]= 4; 
      universe[i+6][j+47]= 4; 
      universe[i+7][j+47]= 1; 
      universe[i+8][j+47]= 1; 
      universe[i+9][j+47]= 3; 
      universe[i+10][j+47]= 3; 
      universe[i+11][j+47]= 3; 
      universe[i+12][j+47]= 3; 
      universe[i+13][j+47]= 6; 
      universe[i+14][j+47]= 11; 
      universe[i+7][j+48]= 10; 
      universe[i+8][j+48]= 4; 
      universe[i+9][j+48]= 6; 
      universe[i+10][j+48]= 6; 
      universe[i+11][j+48]= 6; 
      universe[i+12][j+48]= 6; 
      universe[i+13][j+48]= 6; 
      universe[i+14][j+48]= 1; 
      universe[i+15][j+48]= 4;  

      
  } 
  
 // generate a random initial state for the CA
  public synchronized void initRandomUniverse(){
	
	//reaction diffussion  
	if(RD){  
		frozen = false;
	    for(int i=0;i<width;i++)
	    	for(int j=0; j<height; j++){
	    		//RDUniverse[i][j][0] = 4;
	    		//RDUniverse[i][j][1] = 4;
	    		//RDUniverse[i][j][2] = RD_beta-RD_sigma+2*RD_sigma *rand.nextDouble();
	    		RDUniverse[i][j][2] = RD_beta + RD_sigma * rand.nextGaussian();
	    		RDUniverse[i][j][0] = /*rand.nextDouble() */ 4.0 + rand.nextGaussian() * RD_sigma;
	    		RDUniverse[i][j][1] = /*rand.nextDouble() */ 4.0 + rand.nextGaussian() * RD_sigma;	 
	    		RDFrozen[i][j] = false;
	    		
	    		//RDNextUniverse = new double[width][height][3];
	    	//}
	    //makeRDNextUniverse();	    		
	    //for(int i=0;i<width;i++)
	    //	for(int j=0; j<height; j++){
	    		RDFirstUniverse[i][j][0] = RDUniverse[i][j][0];
	    		RDFirstUniverse[i][j][1] = RDUniverse[i][j][1];
	    		RDFirstUniverse[i][j][2] = RDUniverse[i][j][2];
	    	}
	    //drawOffscreen();
	} //if RD 
	  
	// 2D Cyclic  
    else if(twoDimensional && cyclic){	  	  
	  for(int i=0;i<width;i++)
        for(int j=0;j<height;j++){
    	  universe[i][j] = rand.nextInt(numCols);
    	  firstUniverse[i][j] = universe[i][j];
    	  changed[i][j]=true;
        }  		  
	}
	// 2D standard
	else if(twoDimensional){
		for(int i=0;i<width;i++)
	        for(int j=0;j<height;j++)
	    	  universe[i][j] = 0;
	    	  
		int i = width/2;
		int j = height/2;
		
		if(picture==0){   // Dot
			universe[i][j]=1;
		}
		else if(picture==1){  // HI
		  i-=5;	
			
		  universe[i][j] = 1;
		  universe[i][j+1] = 1;
		  universe[i][j+2] = 1;
		  universe[i][j+3] = 1;
		  universe[i][j-1] = 1;
		  universe[i][j-2] = 1;
		  universe[i][j-3] = 1;
		  universe[i+1][j] = 1;
		  universe[i+2][j] = 1;
		  universe[i+3][j] = 1;
		  universe[i+4][j] = 1;
		  universe[i+4][j+1] = 1;
		  universe[i+4][j+2] = 1;
		  universe[i+4][j+3] = 1;
		  universe[i+4][j-1] = 1;
		  universe[i+4][j-2] = 1;
		  universe[i+4][j-3] = 1;
		  
		  i+=9;
		  universe[i][j-3] = 1;
		  universe[i][j-2] = 1;
		  universe[i][j-1] = 1;
		  universe[i][j] = 1;
		  universe[i][j+1] = 1;
		  universe[i][j+2] = 1;
		  universe[i][j+3] = 1;
		  universe[i+1][j-3] = 1;
		  universe[i-1][j-3] = 1;
		  universe[i+1][j+3] = 1;
		  universe[i-1][j+3] = 1;
		  
		}
		
		else if(picture==2){  // stickman
		  j-=10;
			
		  universe[i][j]=1;	
		  universe[i-1][j]=1;
		  universe[i+1][j]=1;
		  universe[i-2][j+1]=1;
		  universe[i-2][j+2]=1;
		  universe[i-2][j+3]=1;
		  universe[i+2][j+1]=1;
		  universe[i+2][j+2]=1;
		  universe[i+2][j+3]=1;
		  universe[i-1][j+4]=1;
		  universe[i][j+4]=1;
		  universe[i+1][j+4]=1;  // head
		  
		  j+=5;
		  universe[i][j]=1;
		  universe[i][j+1]=1;
		  universe[i][j+2]=1;
		  universe[i][j+3]=1;
		  universe[i][j+4]=1;
		  universe[i][j+5]=1;
		  universe[i][j+6]=1;
		  universe[i][j+7]=1;
		  universe[i][j+8]=1; //body
		  
		  universe[i-1][j+2]=1;
		  universe[i-2][j+2]=1;
		  universe[i-2][j+1]=1;
		  universe[i-3][j+1]=1;
		  universe[i-3][j]=1;
		  universe[i-4][j]=1;
		  universe[i-5][j]=1;
		  universe[i-5][j-1]=1;  // left arm
		  
		  universe[i+1][j+1]=1;
		  universe[i+1][j+2]=1;
		  universe[i+2][j+1]=1;
		  universe[i+2][j]=1;
		  universe[i+3][j]=1;
		  universe[i+4][j]=1;
		  universe[i+4][j-1]=1;
		  universe[i+5][j-1]=1;
		  universe[i+6][j-1]=1;
		  universe[i+6][j-2]=1; // right arm
		  
		  j+=7;
		  universe[i-1][j]=1;
		  universe[i-1][j+1]=1;
		  universe[i-2][j+1]=1;
		  universe[i-2][j+2]=1;
		  universe[i-2][j+3]=1;
		  universe[i-3][j+3]=1;
		  universe[i-3][j+4]=1;
		  universe[i-4][j+4]=1;
		  universe[i-4][j+5]=1; //left leg
		  
		  universe[i+1][j]=1;
		  universe[i+1][j+1]=1;
		  universe[i+2][j+1]=1;
		  universe[i+2][j+2]=1;
		  universe[i+2][j+3]=1;
		  universe[i+3][j+3]=1;
		  universe[i+3][j+4]=1;
		  universe[i+4][j+4]=1;
		  universe[i+4][j+5]=1; //right leg
		  
		}
		else if(picture==3){ //from example
		  j-=4;
		  i-=4;
		  for(int k=0;k<9;k++)
			if(k!=4) universe[i+k][j]=1;
		  j+=1;
		  for(int k=0;k<9;k++)
			if(k==0||k==3||k==5||k==8) universe[i+k][j]=1;
		  j+=1;
		  for(int k=0;k<9;k++)
			if(k==0||k==3||k==4||k==5||k==8) universe[i+k][j]=1;
		  j+=1;
		  for(int k=0;k<9;k++)
			if(k<=2||k>=6) universe[i+k][j]=1;
		  j+=1;
		  universe[i+2][j]=1;
		  universe[i+6][j]=1;
		  j+=1;
		  for(int k=0;k<9;k++)
			if(k<=2||k>=6) universe[i+k][j]=1;
		  j+=1;
		  for(int k=0;k<9;k++)
			if(k==0||k==3||k==4||k==5||k==8) universe[i+k][j]=1;
		  j+=1;
		  for(int k=0;k<9;k++)
			if(k==0||k==3||k==5||k==8) universe[i+k][j]=1;
		  j+=1;
		  for(int k=0;k<9;k++)
			if(k!=4) universe[i+k][j]=1;
		}
		else if(picture==4){
			setBMP1();
		}
		
		for(i=0;i<width;i++)
	      for(j=0;j<height;j++)
	    	firstUniverse[i][j] = universe[i][j];
		
	}
	
	else if(cyclic){  // 1D cyclic CA
	  for(int i=0;i<width;i++)
		for(int j=0;j<height;j++){  
	      if(j==0) universe[i][j] = rand.nextInt(numCols);	  
	      else universe[i][j] = 0;
	      firstUniverse[i][j] = universe[i][j];
	      changed[i][j]=true;
	  }
	}
	else{  // 1D standard CA
      
		
      for(int i=0;i<width;i++)
    	for(int j=0;j<height;j++){    	    
    		universe[i][j]=0;
    	    changed[i][j]=true;
    	    firstUniverse[i][j] = universe[i][j];
    	}
    if(singlepoint){
      universe[width/2][0] = rand.nextInt(numCols-1)+1;
      firstUniverse[width/2][0]=universe[width/2][0];
	}
	else{
		for(int i=0;i<width;i++)
			for(int j=0;j<height;j++){  
		      if(j==0) universe[i][j] = rand.nextInt(numCols);	  
		      else universe[i][j] = 0;
		      firstUniverse[i][j] = universe[i][j];
		      changed[i][j]=true;
		  }
		
	}
	}
	
	generation = 0; 
    reset = true;
	calcCycle = false;
	cycleLength = -1;
	
    drawOffscreen();
    
  }
  
  
  public void initLifeUniverse(int which){
   	  
   for(int i=0;i<width;i++)
	 for(int j=0;j<height;j++)
		 universe[i][j]=0;   
   
   
   if(which==1){ //spaceship
     universe[width/2][height/2] = 1;
     universe[width/2+1][height/2] = 1;
     universe[width/2+2][height/2] = 1;
     universe[width/2][height/2+1] = 1;
     universe[width/2+1][height/2+2] = 1;
   }
   else if(which==2){
	 universe[width/2][height/2]=1;  
	 universe[width/2][height/2-1]=1;  
	 universe[width/2-1][height/2]=1;  
	 universe[width/2+1][height/2]=1;  
	 universe[width/2-1][height/2+1]=1;  
	 universe[width/2+1][height/2+1]=1;  
	 universe[width/2][height/2+2]=1;  
   }
   else if(which==3){
	 universe[width/2][height/2]=1;
	 universe[width/2-1][height/2]=1;
	 universe[width/2-2][height/2]=1;
	 universe[width/2-3][height/2]=1;
	 universe[width/2-4][height/2]=1; 
	 universe[width/2-5][height/2]=1;
	 universe[width/2+1][height/2]=1;
	 universe[width/2+2][height/2]=1;
	 universe[width/2+3][height/2]=1;
	 universe[width/2+4][height/2]=1;
   }
   
   for(int i=0;i<width;i++)
	 for(int j=0;j<height;j++)
		firstUniverse[i][j]= universe[i][j];
   
	generation = 0; 
    //auto = true;
    reset = true;
	calcCycle = false;
	cycleLength = -1;
	
	
    drawOffscreen();
  }
  
 
  
  public synchronized void drawOffscreen(){
   
	if(RD){
	   
	   double mina=Double.POSITIVE_INFINITY,
	          maxa=Double.NEGATIVE_INFINITY,
	          minb=Double.POSITIVE_INFINITY,
	          maxb=Double.NEGATIVE_INFINITY,
	          output,output2;
	   
	   /* find min and max concentrations of morphogens */
	   for(int j=0;j<height;j++)
		  for(int i=0;i<width;i++){
		    if(RDUniverse[i][j][0]<mina) mina = RDUniverse[i][j][0];
		    if(RDUniverse[i][j][0]>maxa) maxa = RDUniverse[i][j][0];
		    if(RDUniverse[i][j][1]<minb) minb = RDUniverse[i][j][1];
		    if(RDUniverse[i][j][1]>maxb) maxb = RDUniverse[i][j][1];
		    
		  }
	   
	   if (mina == maxa) {
		    mina = maxa - 0.01;
		    maxa = mina + 0.02;
	   }
	   if (minb == maxb) {
		    minb = maxb - 0.01;
		    maxb = minb + 0.02;
	   }
	   
	   
	   for(int j=0;j<height;j++)
		  for(int i=0;i<width;i++){
			  output = (RDUniverse[i][j][0] - mina)/ (maxa - mina);
		      output = output * 255.0;  
		      		           
		      output2 = (RDUniverse[i][j][1] - minb)/ (maxb - minb);
		      output2 = output2 * 255.0;  

		      
		    if(showwhich==1) output = output2;
  
			if(output<0||output>255) System.out.println("output warning");
			//bufferGraphics.setColor(new Color((int)output,(int)output,(int)output));	
			bufferGraphics.setColor(new Color(colours[255-(int)output]));	
			
				for(int k=0;k<=(RDtilingdegree_x);k++)
				    for(int l=0; l<=(RDtilingdegree_y); l++){
				    	int xoffset = width*l*pixsize;
				    	int yoffset = height*k*pixsize;
				    	bufferGraphics.fillRect(i*pixsize+xoffset,j*pixsize+yoffset, pixsize, pixsize);						    	
				    }				
			//}				
		  }//for i
   }//if RD	  
   
   else if(!converged){
	for(int j=0;j<height;j++)
	  for(int i=0;i<width;i++){
	    if(changed[i][j]){
		  bufferGraphics.setColor(myCols[universe[i][j]]);
		  if(tilingdegree==1){
			  bufferGraphics.fillRect(i*pixsize,j*pixsize, pixsize, pixsize);
		  }
		  else
		  for(int k=0;k<tilingdegree;k++){
		    //for(int l=0; l<tilingdegree; l++){
		    bufferGraphics.fillRect((k*width*pixsize)+i*pixsize, (k*height*pixsize)+j*pixsize, pixsize, pixsize);
		    bufferGraphics.fillRect((k*width*pixsize)+i*pixsize, j*pixsize, pixsize, pixsize);
		    bufferGraphics.fillRect(i*pixsize, (k*height*pixsize)+j*pixsize, pixsize, pixsize);		
		    
		  }
	    }
	  }	
	}//if converged
	
	repaint();
	
	
	
  }
  
  
  
  public synchronized void update(Graphics g){	  
	 paint(g);
  }
  
  public synchronized void paintComponent(Graphics g) {	  
	
	
	g.drawImage(offscreen,0,0,this);
    int R;
    if(RD) R=1;
    else R=0;
    R=0;
	
	if(RD) tile = RDtilingdegree_y/pixsize;
	else tile = tilingdegree;
	
    if(lastgen!=generation)g.clearRect(5,(int)(pixsize*height*tile)+2,120,15);
    g.setColor(Color.BLACK);
    Font f = new Font(null, Font.BOLD, 12);
    g.setFont(f);
    g.drawString("Generation:  "+generation , 10, (int)(pixsize*height*tile)+15+R*25);    
	lastgen = generation;
        
    if(reset && !RD){
      g.clearRect(300, (int)(pixsize*height*tile)+2, 340, 20);	
      reset = false;
      converged = false;
    }
    
    if(calcCycle && !converged && cycleLength==-1 && !RD){
      g.clearRect(300, (int)(pixsize*height*tile)+2, 340, 20);	
      g.drawString("Snapshot taken at time "+cycleStart,350,(int)(pixsize*height*tile)+15+R*25);         	
    }
    	
    if(cycleLength>0 && !RD ){
    	g.clearRect(300, (int)(pixsize*height*tile)+2, 340, 20);	
    	g.drawString("Cyclic behaviour found, cycle length: "+cycleLength, 350, (int)(pixsize*height*tile)+15+R*25);	 
    }
    
    if(converged){
     g.drawString("stable state reached after "+generation+" generations", 350, (int)(pixsize*height*tile)+15+R*25);

    }
  }



 public void clearCanvas(){
  for(int i=0;i<width;i++)
	for(int j=0;j<height;j++){
	  universe[i][j] = 0;
	  changed[i][j] = true;
		
	}  
  drawOffscreen();
	
  }


  

}
