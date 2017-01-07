import java.awt.*;

import java.awt.event.*;

import java.awt.image.*;
import java.util.*;
import javax.swing.JFrame;
//import java.util.ArrayList;
//import javax.imageio.*;
//import java.applet.Applet;
import java.io.*;
import java.net.URL;
import java.nio.file.*;

import javax.imageio.ImageIO;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//import java.awt.geom.AffineTransform;

import javax.sound.sampled.*;

public class mazerush extends JFrame {
	static final int 
	FRAME_WIDTH = 640,
	FRAME_HEIGHT = 480,
	maze_zoom = 40,
	KernalSleepTime = 10,
	//maze_pixel_width=64,
	//maze_pixel_height=64,
	pdown = 3,
	pup=1,
	pright=0,
	pleft=2,
	AnimationSpeed = 10, // Higher number = slower
	MaxAnimationFrames = 4 * AnimationSpeed;
	static String goalsplash = "Object: finish the maze as fast as possible",
			keyssplash = "WASD or arrow keys to move\nEnter to skip forward\nBackspace to skip back",
			creditssplash = "Credits";
	static final int
	widthtenth = FRAME_WIDTH / 10,
	heighttenth = FRAME_HEIGHT / 10,
	splashtextwaveheight = 10,
	goalsplashx = 0,
	goalsplashy = 5,
	creditssplashx = 0,
	creditssplashy = 9,
	keyssplashx = 0,
	keyssplashy = 1,
	spritesheeth = 4,
	spritesheetv = 4;
	String hs_chars [] = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
						"-","!",".","\u2B05","\u21B5","?" };
	int hs_chars_xy [][]  = {
			{3,1},{5,1},{7,1},{9,1},{11,1},
			{13,3},{13,5},{13,7},{13,9},{13,11},
			{11,13},{9,13},{7,13},{5,13},{3,13},
			{1,11},{1,9},{1,7},{1,5},{1,3},
			{5,4},{7,4},{9,4},
			{5,10},{7,10},{9,10},
			{10,5},{10,7},{10,9},
			{4,5},{4,7},{4,9},
			};
// UVW up, XYZ down, E up 2
	int
	player_x = maze_zoom,   //FRAME_WIDTH/2 + 10, //this is ugly and not maintainable
	player_dx = maze_zoom /8,
	player_y = maze_zoom, //FRAME_HEIGHT/2,
	player_dy = player_dx,
	player_direction = 0;
	int player_width = 0;
	int player_height = 0;
	int player_center_w = 0;
	int player_center_h = 0;
	int	maze_x = 0, maze_y = 0;
	long starttime = 0;
	long endtime = 0;
	long completedtime = 0;
	boolean maze_completed = false;
//	boolean player_moving = false;
	int AnimationFrame = -1;
	int attracths = 5000;
	int splashtimeonscreen = 20000;
	int mazecount = 0;
	
	static KeyboardInput keyboard = new KeyboardInput(); // Keyboard polling
	Canvas canvas; // Our drawing component

	public mazerush() {
		setIgnoreRepaint( true );
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		canvas = new Canvas();
		canvas.setIgnoreRepaint( true );
		canvas.setSize( FRAME_WIDTH, FRAME_HEIGHT );
		add( canvas );
		pack();
	// Hookup keyboard polling
		addKeyListener( keyboard );
		canvas.addKeyListener( keyboard );
;

	}
	class Player {
		BufferedImage spritesheet;
		int
		player_x = maze_zoom,   //FRAME_WIDTH/2 + 10, //this is ugly and not maintainable
		player_dx = maze_zoom /8,
		player_y = maze_zoom, //FRAME_HEIGHT/2,
		player_dy = player_dx,
		player_direction = 0;
		int player_width = 0;
		int player_height = 0;
		int player_center_w = 0;
		int player_center_h = 0;
		int AnimationFrame = -1;
	}
	
	public void run() {
		
		Player player = new Player();
		
		int current_maze=0;
		File fanfare = new File("fanfare1.wav");
		
		JSONArray mazelist = findmazefiles();
		mazecount = mazelist.size();

                
        
		long hstime = System.currentTimeMillis();
		long splashtime = hstime;
		
		Long[] scorearray = new Long[10];
		String[] initialarray = new String[10];

		
		//System.out.println("CreateBuffers"); //debug only
		canvas.createBufferStrategy( 2 );
		BufferStrategy buffer = canvas.getBufferStrategy();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		BufferedImage bi = gc.createCompatibleImage( FRAME_WIDTH, FRAME_HEIGHT );
		Graphics graphics = null;
		Graphics2D g2d = null;
		Color background = Color.BLACK;
		// Clear the back buffer          
		g2d = bi.createGraphics();
		g2d.setColor( background );
		// .....
		splashscreen(buffer);
		//----------------load IMAGE------------------

		BufferedImage player_img = null;
		BufferedImage player_spritesheet = null;
		int spritesheet_player_width = 0;
		int spritesheet_player_height = 0;	
		try {
			URL player_url = new URL("file:spritesheet6.png");
			player_spritesheet = ImageIO.read(player_url);
			// player_width = player_img.getWidth(null);
			// player_height = player_img.getHeight(null);
			// player_center_w = player_width /2;
			// player_center_h = player_height /2;
			 spritesheet_player_width = player_spritesheet.getWidth(null) / spritesheeth;
			 spritesheet_player_height = player_spritesheet.getHeight(null) / spritesheetv;
			 player_width = spritesheet_player_width * 2;
			 player_height = spritesheet_player_height * 2;
			 player_center_w = player_width /2;
			 player_center_h = player_height /2;
		} catch (IOException e) {
		}
		BufferedImage maze_img = null;

		try {
		//	URL url = new URL("http://boulderhackerspace.com/wp-content/uploads/2012/03/SSDBHS_logo_blueback3.png");
			//URL url = new URL("http://boulderhackerspace.com/wp-content/uploads/2012/03/SSDBHS_logo_blueback3.png");
		//	URL url = new URL("https://upload.wikimedia.org/wikipedia/commons/thumb/6/66/SMPTE_Color_Bars.svg/320px-SMPTE_Color_Bars.svg.png");
		//	URL url = new URL("http://mapy.atari8.info/a-b/brucelee.png");
			URL url = new URL("file:/home/gianni/workspace/maze19.png");
			maze_img = ImageIO.read(url);
		} catch (IOException e) {
		}


		
		// SCALE IMAGE---------------------------------------------
	    //AffineTransform tx1 = new AffineTransform();
	    //tx1.translate(110, 20);
	    //tx1.scale(4.0, 4.0);

	    //g2d.setTransform(tx1);
	    //g2d.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);

		Font myFont = new Font("SansSerif", Font.BOLD, 20);
		starttime = System.currentTimeMillis();
		
		//----------------- GAME LOOP ----------------
		//graphics = buffer.getDrawGraphics();
		//mazeSelect(mazeimages, graphics, buffer, keyboard);

		
		// GAME KERNAL
		// KERNAL
		// KERNAL
		// KERNAL
		// KERNAL
		// KERNAL
		// KERNAL
		player.spritesheet = player_spritesheet;
		player.player_width = spritesheet_player_width * 2;
		player.player_height = spritesheet_player_height * 2;
		player.player_center_w = player_width /2;
		player.player_center_h = player_height /2;
		current_maze = mazeSelect(mazelist, buffer, keyboard, mazecount, player);
		long completed_delay = 0;
		int maze_pixel_width = 0;
		int maze_pixel_height =0;
		if(current_maze >= 0) {
			maze_img = enter_maze(current_maze, mazelist, scorearray, initialarray);
			maze_pixel_width = maze_img.getWidth();
			maze_pixel_height = maze_img.getHeight();
		}
		
		while( current_maze >= 0 ) 
		{
	
		
			try {	//game kernal follows
				// Draw maze and player
				graphics = buffer.getDrawGraphics();
				graphics.drawImage(maze_img, maze_x, maze_y , maze_pixel_width*maze_zoom, maze_pixel_height*maze_zoom, null);
				draw_player (player_x, player_y, player_spritesheet, graphics, spritesheet_player_width, spritesheet_player_height, maze_img, maze_pixel_width, maze_pixel_height, false);
			//	if(player_moving)
			//	AnimationFrame += 1;
			//	if(AnimationFrame > MaxAnimationFrames)
			//	AnimationFrame = 0;
			
			//	int px = (player_x  - maze_x) /maze_zoom;
			//	int py = (player_y  - maze_y) /maze_zoom;
			//	int pixel=maze_img.getRGB(px,py);
				// graphics.drawString(String.format("color: %h    ", pixel), 10, 10);
				if(player_on_green(0,0,maze_img, maze_pixel_width, maze_pixel_height))
					starttime = System.currentTimeMillis();
				if(!maze_completed)
					completedtime = System.currentTimeMillis() - starttime;
				graphics.setFont(myFont);
				graphics.setColor(Color.white);
				graphics.fillRect(5, 20, 185, 25);
				graphics.setColor(Color.black);
				graphics.drawString(String.format("Time: %d.%02d", completedtime / 1000, completedtime % 1000), 10, 40);
				
			//	graphics.drawString(String.format("Time: %l$tM:%l$tS.%l$tL", completedtime), 10, 10);
				// Blit image and flip...
				if(player_on_red(0, 0, maze_img, maze_pixel_width, maze_pixel_height) && !maze_completed) {
					completed_delay = System.currentTimeMillis() + 2 * 500;
					maze_completed = true;
					try {
						sampleplayback(fanfare);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (LineUnavailableException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			//	if(maze_completed) { //<- This line removes delay but freezes player
				if(maze_completed && (System.currentTimeMillis() > completed_delay) && completed_delay > 0) { //<- This line requires you to go on red and then go off of it again
					completed_delay=0;

					try {

						Thread.sleep(2000);

					} 
					catch (InterruptedException e) {

					}
					if (check_if_highscore(completedtime, scorearray)) {
						String initials = enter_highscores(player_spritesheet, graphics, buffer, spritesheet_player_width, spritesheet_player_height, completedtime);
						scorearray[9] = completedtime;
						initialarray[9] = initials;
						sorthighscores(scorearray, initialarray);
						savehighscores(mazelist.get(current_maze).toString(), scorearray, initialarray);
						hstime = System.currentTimeMillis();
					}
						/*int x=530;
 					hstime = System.currentTimeMillis();
					while(System.currentTimeMillis() - hstime < 5000) {
						if (
								(System.currentTimeMillis()/200) % 2==0)
							graphics.drawString(String.format("YOU WIN!"), x, 425);
							x--;
							buffer.show();
					}*/
					maze_completed=false;
			//		current_maze++;
					player.spritesheet = player_spritesheet;
					player.player_width = spritesheet_player_width * 2;
					player.player_height = spritesheet_player_height * 2;
					player.player_center_w = player_width /2;
					player.player_center_h = player_height /2;
					current_maze = mazeSelect(mazelist, buffer, keyboard, mazecount, player);
					if(current_maze >= 0){
						maze_img = enter_maze(current_maze, mazelist, scorearray, initialarray);
						maze_x=0;
						maze_y=0;
						player_x = maze_zoom;   //FRAME_WIDTH/2 + 10, //this is ugly and not maintainable
						player_y = maze_zoom; //F
					}
					else break;
					
				}
				if( keyboard.keyDownOnce( KeyEvent.VK_ENTER ) ) {
					if(current_maze < mazecount - 1) current_maze++;
					hstime = System.currentTimeMillis();
					maze_img = enter_maze(current_maze, mazelist, scorearray, initialarray);
					maze_x=0;
					maze_y=0;
					player_x = maze_zoom;   //FRAME_WIDTH/2 + 10, //this is ugly and not maintainable
					player_y = maze_zoom; //F
				}
				if( keyboard.keyDownOnce( KeyEvent.VK_BACK_SPACE ) ) {
					player.spritesheet = player_spritesheet;
					player.player_width = spritesheet_player_width * 2;
					player.player_height = spritesheet_player_height * 2;
					player.player_center_w = player_width /2;
					player.player_center_h = player_height /2;
					current_maze = mazeSelect(mazelist, buffer, keyboard, mazecount, player);
			//		if(current_maze > 0) current_maze--;
					hstime = System.currentTimeMillis();
					maze_img = enter_maze(current_maze, mazelist, scorearray, initialarray);
					maze_x=0;
					maze_y=0;
					player_x = maze_zoom;   //FRAME_WIDTH/2 + 10, //this is ugly and not maintainable
					player_y = maze_zoom; //F
				}

				//graphics = buffer.getDrawGraphics();

				//graphics.drawImage( bi, 0, 0, null );

				if( !buffer.contentsLost() )

					buffer.show();

				// Poll the keyboard
				keyboard.poll();

				// Should we exit?
				if( keyboard.keyDownOnce( KeyEvent.VK_ESCAPE ) )
					break;

				// originally:   Let the OS have a little time...

				try {

					Thread.sleep(KernalSleepTime);

				} 
				catch (InterruptedException e) {

				}
			}

			//Game win loop *****************
			
				
			finally {
				// Release resources

				if( graphics != null ) 

					graphics.dispose();

				if( g2d != null ) 

					g2d.dispose();

			}

		}

	}
	public boolean switchmaze(int currentmaze) {
		return(true);
	}
	//Checking if touching letters
	public String player_touching_letter(int hs_x_offset, int hs_y_offset){
		for(int check=0; check<32; check++) {
			if ( Math.abs( player_x - (hsletter_x(check) + hs_x_offset)) < touching_distance && 
					Math.abs( player_y - (hsletter_y(check) + hs_y_offset)) < touching_distance  )
				return(hs_chars[check]);
		}
		return(null);
	}
	public int touching_distance = maze_zoom / 3;
	public int hsletter_x(int i) {return ( maze_x + hs_chars_xy[i][0] * maze_zoom);}
	public int hsletter_y(int i) {return ( maze_y + hs_chars_xy[i][1]* maze_zoom);}

	public String enter_highscores(BufferedImage player_spritesheet, Graphics background_graphics, BufferStrategy buffer, int spritesheet_player_width, int spritesheet_player_height, long completedtime){
		int highscore_enter_delay = 3;
		String initials = null;
		Graphics highscore_graphics=null;
		BufferedImage highscore_img=null;
		int hsbackground;
		try {
			URL url = new URL("file:highscore2.png");
	//		URL url = new URL("file:/home/gianni/workspace/png_scanr/highscore2.png");
				highscore_img = ImageIO.read(url);
				hsbackground = highscore_img.getRGB(0, 0);
			} catch (IOException e) {
			}
	//	String hscolor = String.format("#%06X", (0xFFFFFF & hsbackground));
		int maze_pixel_width=highscore_img.getWidth();
		int maze_pixel_height=highscore_img.getHeight();
		maze_x = 0;
		maze_y = 0;
		player_x = 100;
		player_y = 100;
	//	player_x = FRAME_WIDTH/2;
	//	player_y = FRAME_HEIGHT/2;
		Font hsfont = new Font("SansSerif", Font.BOLD, 30);
		int hs_x_offset = 8;
		int hs_y_offset = maze_zoom - 8;
		int box_width = maze_zoom * 3;
		int box_height = maze_zoom + maze_zoom / 2;
		int hsbox_x = (int) (maze_pixel_width / 2 * maze_zoom - box_width / 2 + maze_zoom / 2);
		int hsbox_y = (int) (maze_pixel_height / 2 * maze_zoom - box_height / 2);
		boolean player_touching_letter = false;
		char player_letter_touched;
		boolean letter_in_process = false;
		int touching_distance = maze_zoom / 3;
		Color fillcolor = Color.black; 
		String priorletter = null;
		String hsbuf = null;
		Font timeFont = new Font("SansSerif", Font.BOLD, 15);
		String backspace_char =  "\u2B05";
		String enter_char =  "\u21B5";
		Color hsfontcolor = Color.white;
		Random rnd = new Random();
		rnd.setSeed(333);
		long colorchangedelay = 0;
		long colorchangerate = 50;
		
		// KERNAL
		// HIGHSCORE KERNAL
		// KERNAL
		boolean highscore_entered = false;
		long highscore_delay = System.currentTimeMillis() + 60 * 1000;
		while(!(highscore_entered==true && System.currentTimeMillis() > highscore_delay)){ //Highscore kernal  HS KERNAL
			try{	
				highscore_graphics = buffer.getDrawGraphics();
				highscore_graphics.setColor(fillcolor);
				highscore_graphics.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
				highscore_graphics.setFont(hsfont);
				highscore_graphics.setColor(Color.white);
				highscore_graphics.drawImage(highscore_img, maze_x, maze_y, maze_pixel_width*maze_zoom, maze_pixel_height*maze_zoom, null);
				//draw highscrore letters ontop of maze
				for (int i=0;i<32;i++) {
					
					highscore_graphics.drawString(hs_chars[i],hs_x_offset + hsletter_x(i),hs_y_offset + hsletter_y(i));
							
				}
				
				// drawing highscore box
				highscore_graphics.setColor(Color.black);
				highscore_graphics.fillRect(maze_x + hsbox_x, maze_y + hsbox_y, box_width, box_height);
				highscore_graphics.setColor(hsfontcolor);
				if (hsbuf != null)
					highscore_graphics.drawString(hsbuf, maze_x + hsbox_x + hs_x_offset, maze_y + hsbox_y + hs_y_offset);
				
				// draw time

				highscore_graphics.setFont(timeFont);
				highscore_graphics.setColor(Color.white);
				highscore_graphics.drawString(String.format("Time: %d.%02d", completedtime / 1000, completedtime % 1000), maze_x + hsbox_x + hs_x_offset + 8, maze_y + hsbox_y + hs_y_offset + maze_zoom / 2);

				draw_player (player_x, player_y, player_spritesheet, highscore_graphics, spritesheet_player_width, spritesheet_player_height, highscore_img, maze_pixel_width, maze_pixel_height, true);
				if( !buffer.contentsLost() )

					buffer.show();
				

				String letter = player_touching_letter(hs_x_offset, hs_y_offset);
				if (letter != null && !highscore_entered && letter != priorletter){
					if (letter == backspace_char)  {
						int hssize = hsbuf.length();
						if (hssize>0) {
							hsbuf = hsbuf.substring(0, hssize - 1);
						}
					}	
					else {
						if (hsbuf == null)
							hsbuf = letter;
						else if (hsbuf.length() < 3 && letter != enter_char)
							hsbuf += letter;
					}
					if(letter == enter_char) {
						highscore_entered = true;
						highscore_delay = System.currentTimeMillis() + highscore_enter_delay * 1000;
					}
				}
				if(highscore_entered && System.currentTimeMillis() > colorchangedelay) {
					colorchangedelay = System.currentTimeMillis() + colorchangerate;
					hsfontcolor = new Color(rnd.nextInt(0xff),rnd.nextInt(0xff),rnd.nextInt(0xff));
				}
				priorletter = letter;
				
				// Poll the keyboard
				keyboard.poll();
				try {

					Thread.sleep(KernalSleepTime);

				} 
				catch (InterruptedException e) {

				}
			}


			finally {
				// Release resources
				
				if( highscore_graphics != null ) 

					highscore_graphics.dispose();
			}
		}
		initials = hsbuf;
		return(initials);
	}
	public boolean maze_fits_on_screen(int dx, int dy, int maze_pixel_width, int maze_pixel_height){
		if ((maze_x+dx) > 0) return (false);
		if ((maze_x+dx+maze_pixel_width*maze_zoom) < FRAME_WIDTH) return (false);
		if ((maze_y+dy) > 0) return (false);
		if ((maze_y+dy+maze_pixel_height*maze_zoom) < FRAME_HEIGHT) return (false);
		//if ((maze_y - FRAME_HEIGHT+dy) < maze_pixel_height*maze_zoom) return (false);
		return (true);
	}
	public boolean player_on_screen(int dx, int dy){
		if ((player_x+dx) < 0) return (false);
		if ((player_x+dx) > FRAME_WIDTH) return (false);
		if ((player_y+dy) < 0) return (false);
		if ((player_y+dy) > FRAME_HEIGHT) return (false);
		return (true);
	}
	public void draw_player (int px, int py, BufferedImage img, Graphics screen, int pw, int ph, BufferedImage maze_img, int maze_pixel_width, int maze_pixel_height, boolean scrollonly) {
		if(main_interaction(maze_img, maze_pixel_width, maze_pixel_height, scrollonly)){
			AnimationFrame ++;
			if(AnimationFrame >= MaxAnimationFrames)
				AnimationFrame = 0;

			if((AnimationFrame % (AnimationSpeed*2)) == 0) {
				File footsteps = new File("footstep3.wav");
				try {
					sampleplayback(footsteps);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (UnsupportedAudioFileException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (LineUnavailableException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		BufferedImage player_img = img.getSubimage(AnimationFrame / AnimationSpeed * 16, player_direction * 16,
				pw,ph);
		screen.drawImage(player_img, px - player_center_w, py - player_center_h , player_width, player_height, null);
		//java2s.com/Tutorial/Java/0261__2D-Graphics/
	}
	public boolean player_on_path(int dx, int dy, BufferedImage img, int maze_pixel_width, int maze_pixel_height){
		
		int pxright = (player_x + dx - maze_x + player_center_w) /maze_zoom;
		int pybottom = (player_y + dy - maze_y + player_center_h) /maze_zoom;
		int pxleft = (player_x + dx - maze_x - player_center_w) /maze_zoom;
		int pytop = (player_y + dy - maze_y - player_center_h) /maze_zoom;
		
		if (pxright >= maze_pixel_width || pybottom >= maze_pixel_height) // only bottom and right cause outofbounds exception so we just check those
			return(false);
		
			if (img.getRGB(pxleft, pytop) != 0xff000000 && img.getRGB(pxleft, pytop) != 0xff00ff00 && img.getRGB(pxleft, pytop) != 0xffff0000) return (false);
		
			if (img.getRGB(pxright, pytop) != 0xff000000 && img.getRGB(pxright, pytop) != 0xff00ff00 && img.getRGB(pxright, pytop) != 0xffff0000) return (false);
		
			if (img.getRGB(pxleft, pybottom) != 0xff000000 && img.getRGB(pxleft, pybottom) != 0xff00ff00 && img.getRGB(pxleft, pybottom) != 0xffff0000) return (false);
		
			if (img.getRGB(pxright, pybottom) != 0xff000000 && img.getRGB(pxright, pybottom) != 0xff00ff00 && img.getRGB(pxright, pybottom) != 0xffff0000) return (false);
		
		return (true);
	}
	
	public boolean player_on_red(int dx, int dy, BufferedImage img, int maze_pixel_width, int maze_pixel_height){
		
		int pxright = (player_x + dx - maze_x + player_center_w) /maze_zoom;
		int pybottom = (player_y + dy - maze_y + player_center_h) /maze_zoom;
		int pxleft = (player_x + dx - maze_x - player_center_w) /maze_zoom;
		int pytop = (player_y + dy - maze_y - player_center_h) /maze_zoom;
		
		if (pxright >= maze_pixel_width || pybottom >= maze_pixel_height) // only bottom and right cause outofbounds exception so we just check those
			return(false);
		
			if (img.getRGB(pxleft, pytop) == 0xffff0000) return (true);
		
			if (img.getRGB(pxright, pytop) == 0xffff0000) return (true);
	
			if (img.getRGB(pxleft, pybottom) == 0xffff0000) return (true);
	
			if (img.getRGB(pxright, pybottom) == 0xffff0000) return (true);
	
			return (false);
	}
	public boolean player_on_green(int dx, int dy, BufferedImage img, int maze_pixel_width, int maze_pixel_height){
	
		int pxright = (player_x + dx - maze_x + player_center_w) /maze_zoom;
		int pybottom = (player_y + dy - maze_y + player_center_h) /maze_zoom;
		int pxleft = (player_x + dx - maze_x - player_center_w) /maze_zoom;
		int pytop = (player_y + dy - maze_y - player_center_h) /maze_zoom;
		
		if (pxright >= maze_pixel_width || pybottom >= maze_pixel_height) // only bottom and right cause outofbounds exception so we just check those
			return(false);
		
			if (img.getRGB(pxleft, pytop) == 0xff00ff00) return (true);
		
			if (img.getRGB(pxright, pytop) == 0xff00ff00) return (true);
	
			if (img.getRGB(pxleft, pybottom) == 0xff00ff00) return (true);
	
			if (img.getRGB(pxright, pybottom) == 0xff00ff00) return (true);
	
			return (false);
	}
	
	public boolean main_interaction(BufferedImage maze_img, int maze_pixel_width, int maze_pixel_height, boolean scrollonly){
		boolean moving = false;
		// Check keyboard		
		if(( keyboard.keyDown( KeyEvent.VK_W ) || keyboard.keyDown( KeyEvent.VK_UP )))
		{
			moving = true;
			player_direction = pup;	
			if (player_on_path(0, -player_dy, maze_img, maze_pixel_width, maze_pixel_height)) 
				if ((maze_fits_on_screen(0, player_dy, maze_pixel_width, maze_pixel_height) || scrollonly)  && player_y <= FRAME_HEIGHT /2)
					maze_y += player_dy;
				else
					if (player_on_screen(0, -player_dy) )
						player_y -= player_dy;	
		}
		if(( keyboard.keyDown( KeyEvent.VK_A ) || keyboard.keyDown( KeyEvent.VK_LEFT )))
		{
			moving = true;
			player_direction = pleft;
			if (player_on_path(-player_dx, 0, maze_img, maze_pixel_width, maze_pixel_height)) 
			if ((maze_fits_on_screen(player_dx, 0, maze_pixel_width, maze_pixel_height) || scrollonly) && player_x <= FRAME_WIDTH /2)
				maze_x += player_dx;
			else
				if (player_on_screen(-player_dx, 0) )
					player_x -= player_dx;
		}
		if(( keyboard.keyDown( KeyEvent.VK_S ) || keyboard.keyDown( KeyEvent.VK_DOWN )))
		{
			moving = true;
			player_direction = pdown;	
			if (player_on_path(0, player_dy, maze_img, maze_pixel_width, maze_pixel_height)) 
			if ((maze_fits_on_screen(0, -player_dy, maze_pixel_width, maze_pixel_height) || scrollonly) && player_y >= FRAME_HEIGHT /2)
				maze_y -= player_dy;
			else
				if (player_on_screen(0, player_dy) )
					player_y += player_dy;
		}
		if(( keyboard.keyDown( KeyEvent.VK_D ) || keyboard.keyDown( KeyEvent.VK_RIGHT )))
		{
			moving = true;
			player_direction = pright;
			if (player_on_path(player_dx, 0, maze_img, maze_pixel_width, maze_pixel_height)) 
			if ((maze_fits_on_screen(-player_dx, 0, maze_pixel_width, maze_pixel_height) || scrollonly) && player_x >= FRAME_WIDTH /2)
				maze_x -= player_dx;
			else
				if (player_on_screen(player_dx, 0) )
					player_x += player_dx;
		}
		return(moving);
	}
	public static void savehighscores(String maze, Long[] scorearray, String[] initialarray) {
		JSONArray scores = new JSONArray();
		JSONArray initials = new JSONArray();

		JSONObject mazeshigh = new JSONObject();
		for (int i=0; i<10; i++ ) {
			scores.add(scorearray[i]);
			initials.add(initialarray[i]);
		}
	
			JSONObject mazehigh = new JSONObject();

			mazehigh.put("mazename", maze);
			mazehigh.put("highscores",scores);
			mazehigh.put("initials",initials);
		
		//attempt to write new highscore JSONObject to file highscores.json
		try {
			FileWriter file = new FileWriter(maze+".highscore");
			file.write(mazehigh.toJSONString());
			file.flush();
			file.close();
			
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	public static JSONObject gethighscoretable(String maze){
		JSONParser parser = new JSONParser();
		try {
			
			Object obj = parser.parse(new FileReader(maze+".highscore"));
			JSONObject mazehigh = (JSONObject) obj;
			return(mazehigh);

			/*Iterator<String> iterator = scores.iterator();
			while (iterator.hasNext()) {
				//System.out.println(iterator.next());
				System.out.println("test \n");
			} */
		} catch (IOException | ParseException e) {
			Random rnd = new Random();
			rnd.setSeed(555);
			// if highscore table doesn't exist, create one.

			
			
			JSONArray scores = new JSONArray();
			JSONArray initials = new JSONArray();

			JSONObject mazeshigh = new JSONObject();
			for (int i=0; i<10; i++ ) {
				scores.add(new Long((rnd.nextLong() & 0xffff) + 0x8000));
				int ch = rnd.nextInt(26) + 65;
				char initchar = new Character((char) ch);
				initials.add(Character.toString(initchar));
			}
		
				JSONObject mazehigh = new JSONObject();

				mazehigh.put("mazename", maze);
				mazehigh.put("highscores",scores);
				mazehigh.put("initials",initials);
			
			//attempt to write new highscore JSONObject to file highscores.json
			try {
				FileWriter file = new FileWriter(maze+".highscore");
				file.write(mazehigh.toJSONString());
				file.flush();
				file.close();
				
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			return(mazehigh);

		}
	}
	public static boolean check_if_highscore(long completedtime, Long[] scorearray) {
		if(completedtime < scorearray[9]) return(true);
		return(false);
	}
	public static void converthighscores(JSONObject mazescores, Long[] sortedscores, String[] sortedinitials) {
		JSONArray scores = (JSONArray) mazescores.get("highscores");
		JSONArray initials = (JSONArray) mazescores.get("initials");
		
		for (int index=0; index<10; index++ ) {
			sortedscores[index] = (Long)scores.get(index);	//(Long) type conversion between JSON and java objects
			sortedinitials[index] = (String)initials.get(index);
			
		}
	}
	public static void sorthighscores(Long[] sortedscores, String[] sortedinitials) {
		int left = 0;
		int right = 9;
		scoreQuickSort(sortedscores, sortedinitials, left, right);
	}
	public static void displayhighscores(Long[] sortedscores, String[] sortedinitials, Graphics graphics, int hsx, int hsy) {
		sorthighscores(sortedscores, sortedinitials);
		int left = 0;
		int right = 9;
		scoreQuickSort(sortedscores, sortedinitials, left, right);
		final int ystep = 25;
		final int starty = hsy + ystep;
		graphics.setColor(Color.white);
		graphics.fillRect(hsx - 5, hsy - 20, 206, ystep * 11);
		graphics.setColor(Color.black);
		graphics.drawString(String.format("HIGHSCORES"), hsx, hsy);
		for (int index=0; index<10; index++ ) {
			int printy = starty + ystep * index;
			//String temp =(String)scores.get(index);
			//long time = Long.parseLong(temp);
			long time = sortedscores[index];
			//	graphics.drawString(String.format("%d. %s (%d.%02d)", index + 1, sortedinitials[index], time / 1000, time % 1000 / 10), 430, printy);
			graphics.drawString(String.format("%d. %s", index + 1, sortedinitials[index]), hsx, printy);
			graphics.drawString(String.format("(%d.%02d)", time / 1000, time % 1000 / 10), hsx + 86, printy);
			//graphics.drawString(String.format("Time: %04d.%02d", completedtime / 1000, completedtime % 1000), 10, 40);
		}
	}
	
	static int partition(Long scores[], String initials[], int left, int right)

	{

	      int i = left, j = right;

	      Long tmp;
	      String stmp;

	      Long pivot = scores[(left + right) / 2];

	     

	      while (i <= j) {

	            while (scores[i] < pivot)

	                  i++;

	            while (scores[j] > pivot)

	                  j--;

	            if (i <= j) {

	                  tmp = scores[i];

	                  scores[i] = scores[j];

	                  scores[j] = tmp;
	                  
	                  stmp = initials[i];

	                  initials[i] = initials[j];

	                  initials[j] = stmp;

	                  i++;

	                  j--;

	            }

	      };

	     

	      return i;

	}

	 

	static void scoreQuickSort(Long scores[], String initials[], int left, int right) {
	      int index = partition(scores, initials, left, right);
	      if (left < index - 1)
	            scoreQuickSort(scores, initials, left, index - 1);
	      if (index < right)
	            scoreQuickSort(scores, initials, index, right);
	}
	public static JSONArray findmazefiles() {

		JSONArray mazelist = new JSONArray();

		Path dir = Paths.get("");
		System.out.println(dir.toAbsolutePath().toString());
		try (DirectoryStream<Path> stream =
				Files.newDirectoryStream(dir, "*.png")) {
			for (Path entry: stream) {
				String fname = new String(entry.getFileName().toString());
				//	System.out.println(fname.substring(0, 4) == "maze"); //debug
				//	if(fname.length() > 3)
				if(fname.contains("maze")) {	
					System.out.println(fname); //debug only
					mazelist.add(fname);
				}
			}
		} catch (IOException x) {
			System.err.println(x);
		}
		return(mazelist);
	}
	
	public static void displayMazeThumbs(int topmaze, JSONArray mazelist, Graphics graphics, int mazecount, Player player) {
		Font splashFont = new Font("SansSerif", Font.BOLD, 20);
		int thumbnailzoom = 8;
		int thumbnailheight = 12;
		int thumbnailwidth = 16;
		player.player_direction = pleft;
		player.player_x = thumbnailwidth * thumbnailzoom;
		int spritesheet_player_width = player.spritesheet.getWidth(null) / spritesheeth;
		int spritesheet_player_height = player.spritesheet.getHeight(null) / spritesheetv;
		graphics.setFont(splashFont);
		graphics.setColor(Color.white);
		BufferedImage mazeimage = null;
		int y = 0;
		BufferedImage player_img = null;
		while(y < FRAME_HEIGHT && topmaze < mazecount) {
			try {	
				//System.out.println(mazelist.get(topmaze).toString());
				mazeimage = ImageIO.read(new File(mazelist.get(topmaze).toString()));	       
			} catch (IOException e) {
			}

			player_img = player.spritesheet.getSubimage(player.AnimationFrame / AnimationSpeed * 16, player.player_direction * 16, spritesheet_player_width, spritesheet_player_height);
			graphics.drawString(mazelist.get(topmaze).toString(), thumbnailwidth * thumbnailzoom + player.player_width, y + mazeimage.getHeight() / 2);
			graphics.drawImage(mazeimage.getSubimage(0, 0, thumbnailwidth, thumbnailheight), 0, y, thumbnailwidth * thumbnailzoom, thumbnailheight * thumbnailzoom, null);
			graphics.drawImage(player_img, player.player_x, player.player_y, player.player_width, player.player_height, null);
			y += 16*8;
			topmaze ++;
		}
	}
	
	
	public static int mazeSelect(JSONArray mazelist, BufferStrategy buffer, KeyboardInput keyboard, int mazecount, Player player) {
		int listlocation = 0;
		keyboard.poll();
		player.AnimationFrame = 0;
		player.player_direction = 0;
		player.player_x = 0;
		player.player_y = 0;		
		Long[] scorearray = new Long[10];
		String[] initialarray = new String[10];
		while(!(keyboard.keyDown( KeyEvent.VK_ENTER ) || keyboard.keyDown( KeyEvent.VK_ESCAPE )))
		{
			Graphics graphics = buffer.getDrawGraphics();

			graphics.setColor(Color.BLACK);
			graphics.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
			displayMazeThumbs(listlocation, mazelist, graphics, mazecount, player);
			converthighscores(gethighscoretable(mazelist.get(listlocation).toString()), scorearray, initialarray);
			displayhighscores(scorearray, initialarray, graphics, FRAME_WIDTH * 3/5, 0);
			//highlightmaze(listlocation);
			
			buffer.show();
			while(keyboard.poll()) {
				
			}
			while(!keyboard.poll()) {
				
			}
			if(( keyboard.keyDown( KeyEvent.VK_W ) || keyboard.keyDown( KeyEvent.VK_UP )) && listlocation > 0)
				listlocation--;
			if(( keyboard.keyDown( KeyEvent.VK_S ) || keyboard.keyDown( KeyEvent.VK_DOWN )) && listlocation < mazecount -1)
				listlocation++;
			//System.out.println(listlocation);
		}
		System.out.println("exited mazeselect");
		if(keyboard.keyDown( KeyEvent.VK_ENTER))
			return(listlocation);
		else
			return(-1);
	}
	
	public static int mazeselectkeycheck(KeyboardInput keyboard){
		int keypressed = 0;
		int left = 1;
		int right = 2;
		int enter = 3;
		if(( keyboard.keyDown( KeyEvent.VK_A ) || keyboard.keyDown( KeyEvent.VK_LEFT )))
			keypressed=left;
		
		if(( keyboard.keyDown( KeyEvent.VK_ENTER ) ))
			keypressed=enter;
		if(( keyboard.keyDown( KeyEvent.VK_D ) || keyboard.keyDown( KeyEvent.VK_RIGHT )))
			keypressed=right;
		return(keypressed);
	}

	public static void showmazes(int listlocation, BufferedImage[] mazeimages, Graphics screen){
		int gap = FRAME_WIDTH/7;
		int y = FRAME_HEIGHT/2;
		screen.drawImage(mazeimages[listlocation], gap, y, 64, 64, null);
		screen.drawImage(mazeimages[listlocation + 1], gap * 3, y, 64, 64, null);
		screen.drawImage(mazeimages[listlocation + 2], gap * 5, y, 64, 64, null);
	}

	public static void highlightmaze(int listlocation){
		int x = FRAME_WIDTH/7;
		int y= FRAME_HEIGHT/2;
		int w= FRAME_WIDTH/7;
		int h= FRAME_HEIGHT/7;
		//box(x,y,w,h);
	}
	public static BufferedImage enter_maze(int current_maze, JSONArray mazelist, Long[] scorearray, String[] initialarray) {
		BufferedImage mazeimage = null;
		try {	
			mazeimage = ImageIO.read(new File(mazelist.get(current_maze).toString()));	       
		} catch (IOException e) {
		}
		
		converthighscores(gethighscoretable(mazelist.get(current_maze).toString()), scorearray, initialarray);
		return(mazeimage);
	}
	public static void sampleplayback(final File fileName) throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
		class AudioListener implements LineListener {
		    private boolean done = false;
		    @Override public synchronized void update(LineEvent event) {
		      javax.sound.sampled.LineEvent.Type eventType = event.getType();
		      if (eventType == javax.sound.sampled.LineEvent.Type.STOP) {// || eventType == javax.sound.sampled.LineEvent.Type.CLOSE) {
		        done = true;
		        notifyAll();
		      }
		    }
		    public synchronized void waitUntilDone() throws InterruptedException {
		      while (!done) { wait(); }
		    }
		  }
		
		new Thread(new Runnable() {
			

			public void run() {
				AudioListener listener = new AudioListener();

				try {
					AudioInputStream ais = AudioSystem.getAudioInputStream(fileName);
								
					Clip clip = AudioSystem.getClip();
					clip.addLineListener(listener);
					clip.open(ais);
					clip.start();
					listener.waitUntilDone();
					clip.removeLineListener(listener);
					clip.close();
					ais.close();
				}
				 catch (Exception e) {
					 
				 }
				}
			}).start();
	}
	
/*	
	class AudioListener implements LineListener {
	    private boolean done = false;
	    @Override public synchronized void update(LineEvent event) {
	      javax.sound.sampled.LineEvent.Type eventType = event.getType();
	      if (eventType == javax.sound.sampled.LineEvent.Type.STOP || eventType == javax.sound.sampled.LineEvent.Type.CLOSE) {
	        done = true;
	        notifyAll();
	      }
	    }
	    public synchronized void waitUntilDone() throws InterruptedException {
	      while (!done) { wait(); }
	    }
	  }
	
		AudioListener listener = new AudioListener();
		AudioInputStream ais = AudioSystem.getAudioInputStream(fileName);
		try {
			Clip clip = AudioSystem.getClip();
			clip.addLineListener(listener);
			clip.open(ais);
			try {
				clip.start();
			//	listener.waitUntilDone();
			} finally {
			//	clip.close();
			}
		} finally {
			ais.close();
		}
	}
	*/
	public static void splashtext(Graphics splashgraphics, double yoffset) {
		Font splashFont = new Font("SansSerif", Font.BOLD, 20);

		splashgraphics.setFont(splashFont);
		splashgraphics.setColor(Color.white);
		splashgraphics.drawString(goalsplash, tenthx(goalsplashx), (int) (tenthy(goalsplashy) + yoffset));
		multilinedrawstring(keyssplash, tenthx(keyssplashx), (int) (tenthy(keyssplashy) + yoffset), splashgraphics, 20);
		splashgraphics.drawString(creditssplash, tenthx(creditssplashx), (int) (tenthy(creditssplashy) + yoffset));
	}
	public static int tenthx(int x) {
		return(x * FRAME_WIDTH / 10);
	}
	public static int tenthy(int y) {
		return(y * FRAME_HEIGHT / 10);
	}
	
	public static void multilinedrawstring(String rawstring, int x, int y, Graphics screen, int fontheight) {
		String[] splitstring = rawstring.split("\n");
		for (int i=0; i < splitstring.length; i++) {
			screen.drawString(splitstring[i], x, y);
			y += fontheight;
		}
	}
	public static void splashscreen(BufferStrategy buffer) {
		double yoffset = 0;
		double waveangle = 0;
		double wavespeed = 0.05;
		BufferedImage splashimage = null;
		BufferedImage titleimage = null;
		try {	
			splashimage = ImageIO.read(new File("splash2.png"));	       
		} catch (IOException e) {
		}
		try {	
			titleimage = ImageIO.read(new File("gametitle.png"));	       
		} catch (IOException e) {
		}
		Graphics graphics = buffer.getDrawGraphics();
		while(!keyboard.poll()) {

			graphics.drawImage(splashimage, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, null);
			graphics.drawImage(titleimage, FRAME_WIDTH / 2 - titleimage.getWidth() / 2, FRAME_HEIGHT / 4, null);
			yoffset = Math.sin(waveangle) * splashtextwaveheight;
			waveangle += wavespeed;
			splashtext(graphics, yoffset);
			if( !buffer.contentsLost() )
				buffer.show();
			try {

				Thread.sleep(KernalSleepTime);

			} 
			catch (InterruptedException e) {

			}
		}
	}
	public static void main( String[] args ) {
		mazerush app = new mazerush();
		app.setTitle( "Maze Rush!" );
		app.setVisible( true );
		app.run();
		System.exit( 0 );
	}


}