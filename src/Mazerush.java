//TO DO: coins to unlock
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.TimerTask;
import java.util.Timer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.ImageReader;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.NodeList;

import de.quippy.javamod.mixer.Mixer;
import de.quippy.javamod.multimedia.MultimediaContainer;
import de.quippy.javamod.multimedia.MultimediaContainerManager;
import de.quippy.javamod.multimedia.mod.ModContainer;
import de.quippy.javamod.system.Helpers;

import java.util.zip.CRC32;
import java.io.ByteArrayOutputStream;

public class Mazerush extends JFrame {
	static final long serialVersionUID = 1L;
	static final int FRAME_WIDTH = 640, 
			FRAME_HEIGHT = 480, 
			maze_zoom = 40, 
			player_speed = 8, // actual movement = mazezoom / player_speed

			objectupdate_bandwidth = 14, // Was 14, //time in milliseconds
			// between object updates
			mazeselect_bandwidth = 200,
			coinprobability = 25, // was 10
			coinreward = 1, //in seconds per coin
			maze_subimage_width = FRAME_WIDTH / maze_zoom, maze_subimage_height = FRAME_HEIGHT / maze_zoom,
			KernalSleepTime = 10, pup = 0b0001, pdown = 0b0010, pleft = 0b0100, pright = 0b1000, pstill = 0,
			AnimationSpeed = 10, // was 10,Higher number = slower
			MaxanimationFrames = 4 * AnimationSpeed;
	static String goalsplash = "Run the maze and collect coins",
			keyssplash = "WASD or arrow keys to move\nESC to exit",
			creditssplash = "by Nicky & Specter", anykeysplash = "Press A Key";
	static final int widthtenth = FRAME_WIDTH / 10, heighttenth = FRAME_HEIGHT / 10, spritesheeth = 4, // how
			// many
			// sprite
			// frames
			// in
			// spritesheet
			// horizontally
			spritesheetv = 5, // how many sprite frames in spritesheet
			// vertically
			mazepathcolor = 0xff000000, mazeorigincolor = 0xff00ff00, mazegoalcolor = 0xffff0000;

	String hs_chars[] = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
			"T", "U", "V", "W", "X", "Y", "Z", "-", "!", ".", "\u2408", "\u21B5", "?", "Highscore!",
	"Enter your initials" };
	int hs_chars_xy[][] = { { 3, 1 }, { 5, 1 }, { 7, 1 }, { 9, 1 }, { 11, 1 }, { 13, 3 }, { 13, 5 }, { 13, 7 },
			{ 13, 9 }, { 13, 11 }, { 11, 13 }, { 9, 13 }, { 7, 13 }, { 5, 13 }, { 3, 13 }, { 1, 11 }, { 1, 9 },
			{ 1, 7 }, { 1, 5 }, { 1, 3 }, { 5, 4 }, { 7, 4 }, { 9, 4 }, { 5, 10 }, { 7, 10 }, { 9, 10 }, { 10, 5 },
			{ 10, 7 }, { 10, 9 }, { 4, 5 }, { 4, 7 }, { 4, 9 }, { 5, 2 }, { 3, 3 } };
	long endtime = 0;
	int attracths = 5000;
	int splashtimeonscreen = 20000;
	int mazecount = 0;
	int totalFrameCount = 0;
	int currentFPS = 0;
	static int objectupdatetick = 0;
	static KeyboardInput keyboard = new KeyboardInput(); // Keyboard polling
	Canvas canvas; // Our drawing component
	static Font gameFont = null;
	
	public Mazerush() {
		setIgnoreRepaint(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		canvas = new Canvas();
		canvas.setIgnoreRepaint(true);
		canvas.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		add(canvas);
		pack();
		addKeyListener(keyboard); // Hookup keyboard polling
		canvas.addKeyListener(keyboard);
	}

	class Player {
		BufferedImage spritesheet;
		int player_x = maze_zoom, // FRAME_WIDTH/2 + 10, //this is ugly and not
				// maintainable
				player_dx = maze_zoom / player_speed, // this means player moves
				// maze_zoom /
				// player_speed pixels
				// each frame
				player_y = maze_zoom, // FRAME_HEIGHT/2,
				player_dy = player_dx, player_moving_direction = 0, player_facing_direction = 0;
		boolean moving = false;
		int player_width = 0;
		int player_height = 0;
		int player_center_w = 0;
		int player_center_h = 0;
		int animationFrame = -1;
		boolean maze_completed = false;
		long completedtime = 0;
		String rle = null;
		int cmove = 0;
		int powtick;
		int powcount = 0;
		boolean powplayback_enabled = false;
		int spritesheet_player_height, spritesheet_player_width;
		int coinsCollected = 0;
		int coinCollisions = 0;
		boolean clockstarted = false;
		int rushian = 0;
		int currentMaze = 1;
	}
	static class Rusher {
		BufferedImage spritesheet = null;
		String titleTag = null;
		int price = 0;
		String name = "none";
		{}
	}
	class AnimatedSprite {
		BufferedImage spritesheet;
		int x,y;
		int width = 0;
		int height = 0;
		int center_w = 0;
		int center_h = 0;
		int animationFrame = -1;
		int numFrames = 1;
	}
	class Coin {
		BufferedImage spritesheet;

		{
			try {
				spritesheet = ImageIO.read(new URL("file:resources/coin.png"));
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	
		int x = -1, y = -1, center_w = spritesheet.getWidth(null) / spritesheeth,
				center_h = spritesheet.getHeight(null) / spritesheetv, width = center_w * 2, height = center_h * 2,

				animationFrame = -1;
		boolean collected = false;

	}
	static class Saying {
		long expires;
		String text;
		int x;
		int y;
		Color fgcol = Color.black;
		Color bkcol = Color.white;
		long startTime;
		long flash;
		public Saying (long ttl, String sayText, int sayX, int sayY, Color sayFgcol, Color sayBkcol) {
			expires=System.currentTimeMillis() + ttl;
			text=sayText;
			x=sayX;
			y=sayY;
			fgcol=sayFgcol;
			bkcol=sayBkcol;
			startTime=0;
			}
		public Saying (long ttl, String sayText, int sayX, int sayY, Color sayFgcol, Color sayBkcol, long sayFlash) {
			expires=System.currentTimeMillis() + ttl;
			text=sayText;
			x=sayX;
			y=sayY;
			fgcol=sayFgcol;
			bkcol=sayBkcol;
			flash=sayFlash;
			startTime=System.currentTimeMillis();
		}
	}
	static class ScoreArrays {
		Long[] times = new Long[10];
		int[] coins = new int[10];
		String[] initials = new String[10];
		String[] pows = new String[10];
		/*
		 * Long[] scorearray = new Long[10]; String[] initialArray = new
		 * String[10];
		 */
	}

	class Maze {
		BufferedImage maze_img;
		int maze_pixel_width;
		int maze_pixel_height;
		int maze_x;
		int maze_y;
		int powplaypos = 0;
		String pow;
		boolean powenabled = false;
	}

	public void startFrameTimers() {
		TimerTask updateFPS = new TimerTask() {
			public void run() {
				currentFPS = totalFrameCount;
				totalFrameCount = 0;
			}
		};
		Timer t = new Timer();
		t.scheduleAtFixedRate(updateFPS, 1000, 1000);

		TimerTask objectupdatetimer = new TimerTask() {
			public void run() {
				objectupdatetick++;

			}
		};
		Timer obt = new Timer();
		obt.scheduleAtFixedRate(objectupdatetimer, objectupdate_bandwidth, objectupdate_bandwidth);

	}

	public void run() {
		Rusher rusher = new Rusher();
		List rusherList = getRushers("rushians/");
		Player player = new Player();
		//register FONT
		//Font gameFont = null;
		try {
		     //Returned font is of pt size 1
		     gameFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/prstartk.ttf"));
		
		} catch (IOException|FontFormatException e) {
			System.out.println(e);//Handle exception
		}
	    
		// TODO File fanfare = new File("resources/fanfare1.wav");

		// initialize maze database
		JSONArray mazelist = findmazefiles();
		mazecount = mazelist.size();

		ScoreArrays scoreArrays = new ScoreArrays();

		canvas.createBufferStrategy(2);
		BufferStrategy buffer = canvas.getBufferStrategy();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		BufferedImage bi = gc.createCompatibleImage(FRAME_WIDTH, FRAME_HEIGHT);
		Graphics backbuffer = null;
		Graphics2D g2d = null;
		Color background = Color.BLACK;
		// Clear the back buffer
		g2d = bi.createGraphics();
		g2d.setColor(background);
		// .....
		startFrameTimers();
		splashScreen(buffer);
		player.spritesheet = null;
		player.spritesheet_player_width = 0;
		player.spritesheet_player_height = 0;
		try {
			URL player_url = new URL("file:rushians/zombie.png");
			player.spritesheet = ImageIO.read(player_url);
			player.spritesheet_player_width = player.spritesheet.getWidth(null) / spritesheeth;
			player.spritesheet_player_height = player.spritesheet.getHeight(null) / spritesheetv;
			player.player_width = player.spritesheet_player_width * 2;
			player.player_height = player.spritesheet_player_height * 2;
			player.player_center_w = player.player_width / 2;
			player.player_center_h = player.player_height / 2;
		} catch (IOException e) {
			System.out.println(e);
		}
		AnimatedSprite bouncylock = new AnimatedSprite();
		bouncylock.numFrames = 8;
		try{
			URL lockURL = new URL("file:resources/BouncyLock.png");
			bouncylock.spritesheet = ImageIO.read(lockURL);

		}
		catch (IOException e){
			System.out.println(e);
		}
		AnimatedSprite arrow = new AnimatedSprite();
		arrow.numFrames = 8;
		try{
			URL lockURL = new URL("file:resources/arrow.png");
			arrow.spritesheet = ImageIO.read(lockURL);

		}
		catch (IOException e){
			System.out.println(e);
		}

		Maze maze = new Maze();
		// TODO boolean fanfareplaying = false;
		int lasthighscoreidx = -1;
		List coins = new ArrayList();

		// OUTER LOOP GAME KERNAL
		// OUTER LOOP GAME KERNAL
		// OUTER LOOP GAME KERNAL
		while (player.currentMaze > 0) {
			player.currentMaze = mazeSelect(mazelist, buffer, keyboard, mazecount, player, lasthighscoreidx,
					scoreArrays, bouncylock, arrow, rusherList);
			
			
			if (player.currentMaze > 0) {
				scoreArrays = converthighscores(gethighscoretable(mazelist.get(player.currentMaze).toString()));
					
				Mixer mixer = getmixer("resources/Waiting for loaders.mod");
				playsong(mixer);
				doMazeRun(player, mazelist, maze, scoreArrays, backbuffer, buffer, g2d, coins, rusherList);
				mixer.stopPlayback();
			} else if (player.currentMaze < 0) {
				// ------------------------------------POW TEST CODE
				// ----------------
				player.currentMaze *= -1;
				String maze_name = mazelist.get(player.currentMaze).toString();
				JSONObject hstable = (JSONObject) gethighscoretable(maze_name);

				JSONArray maze_pow = getpow(hstable);

				if (maze_pow != null) {
					if (maze_pow.get(0) != null) {
						String mazepowstring = (String) maze_pow.get(0);
						// getmazeruntime(mazepowstring, player.currentMaze, player,
						// mazelist, maze, scoreArrays);
						System.out.println(
								getmazeruntime(mazepowstring, player, mazelist, maze, scoreArrays));
						player.maze_completed = false;
					}
				}
			}
		}
	}

	public void initmazerun(Player player, JSONArray mazelist, Maze maze, ScoreArrays scoreArrays) {
		player.player_x = maze_zoom;
		player.player_y = maze_zoom;
		player.player_moving_direction = 0;
		player.player_dx = player.player_dy = maze_zoom / player_speed;
		player.animationFrame = 0;
		player.maze_completed = false;
		maze.maze_x = 0; // all mazes start at 0,0
		maze.maze_y = 0; // could change so that we look for the mazeorigincolor
		maze.maze_img = enter_maze(player, mazelist, scoreArrays);
		maze.maze_pixel_width = maze.maze_img.getWidth();
		maze.maze_pixel_height = maze.maze_img.getHeight();
		player.clockstarted = false;
	}

	public long getmazeruntime(String maze_pow, Player player, JSONArray mazelist, Maze maze,
			ScoreArrays scoreArrays) {

		if (maze_pow.length() > 0) {
			player.powplayback_enabled = true;
			player.powtick = 0;
			player.rle = maze_pow;
		} else
			return (-1);
		initmazerun(player, mazelist, maze, scoreArrays);
		while (!player.maze_completed) { // TODO find a condition
			player = update_objects(maze, player);
		}
		return (player.completedtime);
	}

	public void doMazeRun(Player player, JSONArray mazelist, Maze maze, ScoreArrays scoreArrays,
			Graphics backbuffer, BufferStrategy buffer, Graphics2D g2d, List coins, List rusherList) {

		boolean fanfareplaying = false;
		long completed_delay = 0;
		File fanfare = new File("resources/fanfare1.wav");
		int lasthighscoreidx = -1;

		if (player.currentMaze != 0) {
			player.powplayback_enabled = false; // default
			if (player.currentMaze < 0) {
				player.currentMaze *= -1;
			}
			initmazerun(player, mazelist, maze, scoreArrays);
			coins.clear();
			placeCoins(coins, maze);
			objectupdatetick = 0;
		}
		Font timeFont = gameFont.deriveFont(16f);
		int maze_overscan_x = 0;
		int maze_overscan_y = 0;
		boolean FrameValid = false;
		File footsteps = new File("resources/footstep3.wav");
		File coin_collected_sound = new File("resources/135936__bradwesson__collectcoin.wav");
		AnimatedSprite coin = new AnimatedSprite();
		coin.numFrames = 4;
		try{
			URL coinURL = new URL("file:resources/coin.png");
			coin.spritesheet = ImageIO.read(coinURL);

		}
		catch (IOException e){
			System.out.println(e);
		}
		player.coinsCollected = 0;
		player.coinCollisions = 0;
		while (player.currentMaze > 0) {
			// inner kernel loop starts here

			if (objectupdatetick > 0) {
				objectupdatetick--;
				player = update_objects(maze, player);

				player.coinCollisions = coinCollision(player, coins, maze);
				player.coinsCollected += player.coinCollisions;
				if (player.coinCollisions > 0) {
					try {
						sampleplayback(coin_collected_sound);
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						e1.printStackTrace();
					} catch (LineUnavailableException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				if ((player.animationFrame % (AnimationSpeed * 2)) == 0 && player.moving) {
					try {
						sampleplayback(footsteps);
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						e1.printStackTrace();
					} catch (LineUnavailableException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				// Poll the keyboard - may want to move into update object while
				// loop
				keyboard.poll();
				if (objectupdatetick == 0)
					FrameValid = false;
				
			}
			if (player.maze_completed && !fanfareplaying) {
				completed_delay = System.currentTimeMillis() + 2 * 500;
				fanfareplaying = true;
				try {
					sampleplayback(fanfare);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (UnsupportedAudioFileException e1) {
					e1.printStackTrace();
				} catch (LineUnavailableException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			if (player.maze_completed && (System.currentTimeMillis() > completed_delay) && completed_delay > 0) { 
				completed_delay = 0;
				try {
					Thread.sleep(2000); // transitional pause between maze run screen and highscore screen
				} catch (InterruptedException e) {
					System.out.println(e);

				}
				if (checkIfHighscore(getScore(player.completedtime,player.coinsCollected), scoreArrays)) {
					String initials = enter_highscores(backbuffer, buffer, player, rusherList);
					if (initials != null) {
						scoreArrays.times[9] = player.completedtime;
						scoreArrays.coins[9] = player.coinsCollected; 
						scoreArrays.initials[9] = initials;
						scoreArrays.pows[9] = player.rle;
						sorthighscores(scoreArrays);
						savehighscores(mazelist.get(player.currentMaze).toString(), scoreArrays);
						lasthighscoreidx = findhighscore(scoreArrays, player.completedtime, initials);
					}
				}
				player.maze_completed = false;
				break; // break out of while loop for inner game kernal
			}
			keyboard.poll();

			if (keyboard.keyDownOnce(KeyEvent.VK_BACK_SPACE))
				break;
			// Should we exit?
			if (keyboard.keyDownOnce(KeyEvent.VK_ESCAPE)) {
				player.currentMaze = 0;
				break;
			}

			// ------------------------------------------------------------------------------------------------
			// UPDATE GRAPHICS
			// ------------------------------------------------------------------------------------------------
//	was		if (objectupdatetick == 0 && FrameValid == false) {
			if (!FrameValid) {

				try {
					totalFrameCount++;
					backbuffer = buffer.getDrawGraphics(); // DRAW
					// backbuffer.drawImage(maze.maze_img, maze.maze_x,
					// maze.maze_y , maze.maze_pixel_width*maze_zoom,
					// maze.maze_pixel_height*maze_zoom, null);

					if ((-(maze.maze_x / maze_zoom) + maze_subimage_width + 1) <= maze.maze_pixel_width)
						maze_overscan_x = 1;
					else
						maze_overscan_x = 0;

					if ((-(maze.maze_y / maze_zoom) + maze_subimage_height + 1) <= maze.maze_pixel_height)
						maze_overscan_y = 1;
					else
						maze_overscan_y = 0;
					backbuffer.drawImage(
							maze.maze_img.getSubimage(-maze.maze_x / maze_zoom, -maze.maze_y / maze_zoom,
									maze_subimage_width + maze_overscan_x, maze_subimage_height + maze_overscan_y),
									maze.maze_x % maze_zoom, maze.maze_y % maze_zoom, FRAME_WIDTH + maze_zoom * maze_overscan_x,
									FRAME_HEIGHT + maze_zoom * maze_overscan_y, null);
					draw_coins(backbuffer, coins, maze);
					draw_player(backbuffer, maze, player, rusherList);
					backbuffer.setFont(timeFont);
					drawStatus(backbuffer, player, coin);
					if (!buffer.contentsLost())
						buffer.show();
					FrameValid = true; // Frame limiting

				} finally {
					// Release resources

					if (backbuffer != null)

						backbuffer.dispose();

					if (g2d != null)

						g2d.dispose();

				}
			}
		}

	}
	public void drawStatus(Graphics backbuffer, Player player, AnimatedSprite coin){
		final int statusypos = 25, timexpos = 10, coinsxpos = 210, fpsxpos = 320;
	//	backbuffer.setColor(Color.white);
	//	backbuffer.fillRect(5, 20, 185, 25);
		String timeString = String.format("Time: %d.%02d", player.completedtime / 1000, player.completedtime % 1000);
		backbuffer.setColor(Color.white);
		
		for(int fy=-1;fy<=1;fy++)
			for(int fx=-1;fx<=1;fx++)
				backbuffer.drawString(timeString, timexpos+fx, statusypos+fy);
		
			
		backbuffer.setColor(Color.black);
				
		backbuffer.drawString(timeString, timexpos, statusypos); // TODO move to other side of screen if
		// player is on top of it
		
	      
		backbuffer.drawString(String.format("FPS: %d", currentFPS), fpsxpos, statusypos);
		coin.x=coinsxpos;
		coin.y=3;
		coin.animationFrame ++;
		drawAnimatedSprite(coin, backbuffer);
		backbuffer.setColor(Color.yellow);
		backbuffer.drawString(String.format(": %d", player.coinsCollected), coinsxpos+32, statusypos);
		
	}
	public Player powrecord(Player player) {
		char powdir = (char) (65 + player.player_moving_direction);
		char coindir = (char) 'Z';
		char lastmove;
		if (player.rle == null)
			player.rle = "";
		if (player.rle.length() > 0) {
			// System.out.println(player.pow.rle);
			lastmove = player.rle.charAt(player.rle.length() - 1);

			if (lastmove == powdir && player.coinCollisions == 0) {
				player.powcount++;
			} else {

				player.rle += Integer.toString(player.powcount);
				player.powcount = 0;
				if(player.coinCollisions > 0) {
					player.rle += coindir;
					player.rle += Long.toString(player.completedtime);
				}
				player.rle += powdir;
			}
		} else {
			player.rle = "" + powdir;
		}
		// System.out.println(player.rle);
		return (player);
	}

	/*
	 * OLD 4 directional version public Player powrecord(Player player) { char
	 * powdir; switch (player.player_moving_direction) { case pup: powdir = 'U';
	 * break; case pdown: powdir = 'D'; break; case pleft: powdir = 'L'; break;
	 * case pright: powdir = 'R'; break; case pstill: powdir = 'S'; break;
	 * default: powdir = '.'; }
	 * 
	 * char lastmove; if(player.rle == null) player.rle = "";
	 * if(player.rle.length() > 0){ //System.out.println(player.pow.rle);
	 * lastmove = player.rle.charAt(player.rle.length() - 1);
	 * 
	 * if(lastmove == powdir) { player.powcount ++; } else {
	 * 
	 * player.rle += Integer.toString(player.powcount); player.powcount = 0;
	 * player.rle += powdir; } } else { player.rle = "" + powdir; }
	 * 
	 * return(player); }
	 */

	public List placeCoins(List coins, Maze maze) {
		
		CRC32 c32 = new CRC32();
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			ImageIO.write(maze.maze_img, "png", baos);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
			} catch (Exception e) {
			}
		}
		Random rnd = new Random();
		rnd.setSeed(c32.getValue());
		for (int my = 0; my < maze.maze_pixel_height; my += 2)
			for (int mx = 0; mx < maze.maze_pixel_width; mx += 2) {
				if (maze.maze_img.getRGB(mx, my) == mazepathcolor) {
					if (rnd.nextInt(0xff) < coinprobability) {
						placeCoin(coins, mx, my);
					}
				}
			}
		return coins;
	}

	public void placeCoin(List coins, int x, int y) {
		Coin coin = new Coin();
		coin.x = x;
		coin.y = y;
		coin.collected = false;
		coin.animationFrame = 0;
		coins.add(coin);
		coin.width = 32;
		coin.height = 32; //this is for double size
	}

	public Player update_objects(Maze maze, Player player) {

		player = update_player(false, maze, player);
		if (!player.powplayback_enabled)
			player = powrecord(player);
		if (player_on_color(mazegoalcolor, 0, 0, maze, player) && !player.maze_completed)
			player.maze_completed = true;
		if (!player.maze_completed)
			player.completedtime += objectupdate_bandwidth;
		if (player_on_color(mazeorigincolor, 0, 0, maze, player) && !player.clockstarted)
			player.completedtime = 0;
		else if (!player.clockstarted) player.clockstarted=true;
		return (player);
	}

	public boolean switchmaze(int currentmaze) {
		return (true);
	}

	// Checking if touching letters
	public String player_touching_letter(int hs_x_offset, int hs_y_offset, Maze maze, Player player) {
		for (int check = 0; check < 32; check++) {
			if (Math.abs(player.player_x - (hsletter_x(check, maze) + hs_x_offset)) < touching_distance
					&& Math.abs(player.player_y - (hsletter_y(check, maze) + hs_y_offset)) < touching_distance)
				return (hs_chars[check]);
		}
		return (null);
	}

	public int touching_distance = maze_zoom / 3;

	public int hsletter_x(int i, Maze maze) {
		return (maze.maze_x + hs_chars_xy[i][0] * maze_zoom);
	}

	public int hsletter_y(int i, Maze maze) {
		return (maze.maze_y + hs_chars_xy[i][1] * maze_zoom);
	}

	public String enter_highscores(Graphics background_graphics, BufferStrategy buffer, Player player, List rusherList) {
		int highscore_enter_delay = 3;
		String initials = null;
		Graphics highscore_graphics = null;
		Maze hsmaze = new Maze();
		try {
			URL url = new URL("file:resources/highscore3.png");
			hsmaze.maze_img = ImageIO.read(url);
		} catch (IOException e) {
			System.out.println(e);
		}
		hsmaze.maze_pixel_width = hsmaze.maze_img.getWidth();
		hsmaze.maze_pixel_height = hsmaze.maze_img.getHeight();
		hsmaze.maze_x = 0;
		hsmaze.maze_y = 0;
		player.player_x = 100;
		player.player_y = 100;
		Font hsfont = new Font("SansSerif", Font.BOLD, 30);
		int hs_x_offset = 8;
		int hs_y_offset = maze_zoom - 8;
		int box_width = maze_zoom * 3;
		int box_height = maze_zoom + maze_zoom / 2;
		int hsbox_x = (int) (hsmaze.maze_pixel_width / 2 * maze_zoom - box_width / 2 + maze_zoom / 2);
		int hsbox_y = (int) (hsmaze.maze_pixel_height / 2 * maze_zoom - box_height / 2);
		Color fillcolor = Color.black;
		String priorletter = null;
		String hsbuf = null;
		Font timeFont = new Font("SansSerif", Font.BOLD, 15);
		String backspace_char = "\u2408";// changed from back arrow to bs
		String enter_char = "\u21B5";
		Color hsfontcolor = Color.white;
		Random rnd = new Random();
		rnd.setSeed(333);
		long colorchangedelay = 0;
		long colorchangerate = 50;

		// KERNA)L
		// HIGHSCORE KERNAL
		// KERNAL
		boolean highscore_entered = false;
		long highscore_delay = System.currentTimeMillis() + 60 * 1000;
		while (!(highscore_entered == true && System.currentTimeMillis() > highscore_delay)
				&& !(keyboard.keyDown(KeyEvent.VK_ESCAPE))) { // Highscore
			// kernal HS
			// KERNAL
			try {
				highscore_graphics = buffer.getDrawGraphics();
				highscore_graphics.setColor(fillcolor);
				highscore_graphics.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
				highscore_graphics.setFont(hsfont);
				highscore_graphics.setColor(Color.white);
				highscore_graphics.drawImage(hsmaze.maze_img, hsmaze.maze_x, hsmaze.maze_y,
						hsmaze.maze_pixel_width * maze_zoom, hsmaze.maze_pixel_height * maze_zoom, null);
				// draw high score letters on top of maze
				for (int i = 0; i < 34; i++) {

					highscore_graphics.drawString(hs_chars[i], hs_x_offset + hsletter_x(i, hsmaze),
							hs_y_offset + hsletter_y(i, hsmaze));

				}

				// drawing highscore box
				highscore_graphics.setColor(Color.black);
				highscore_graphics.fillRect(hsmaze.maze_x + hsbox_x, hsmaze.maze_y + hsbox_y, box_width, box_height);
				highscore_graphics.setColor(hsfontcolor);
				if (hsbuf != null)
					highscore_graphics.drawString(hsbuf, hsmaze.maze_x + hsbox_x + hs_x_offset,
							hsmaze.maze_y + hsbox_y + hs_y_offset);

				// draw time

				highscore_graphics.setFont(timeFont);
				highscore_graphics.setColor(Color.white);
				highscore_graphics.drawString(
						String.format("Time: %d.%02d", player.completedtime / 1000, player.completedtime % 1000),
						hsmaze.maze_x + hsbox_x + hs_x_offset + 8,
						hsmaze.maze_y + hsbox_y + hs_y_offset + maze_zoom / 2);

				player = update_player(true, hsmaze, player);
				// public void draw_player (Player player, Graphics backbuffer,
				// BufferedImage maze_img, int maze_pixel_width, int
				// maze_pixel_height) {

				draw_player(highscore_graphics, hsmaze, player, rusherList);
				if (!buffer.contentsLost())

					buffer.show();

				String letter = player_touching_letter(hs_x_offset, hs_y_offset, hsmaze, player);
				if (letter != null && !highscore_entered && letter != priorletter) {
					if (letter == backspace_char) {
						int hssize = hsbuf.length();
						if (hssize > 0) {
							hsbuf = hsbuf.substring(0, hssize - 1);
						}
					} else {
						if (hsbuf == null)
							hsbuf = letter;
						else if (hsbuf.length() < 3 && letter != enter_char)
							hsbuf += letter;
					}
					if (letter == enter_char) {
						highscore_entered = true;
						highscore_delay = System.currentTimeMillis() + highscore_enter_delay * 1000;
					}
				}
				if (highscore_entered && System.currentTimeMillis() > colorchangedelay) {
					colorchangedelay = System.currentTimeMillis() + colorchangerate;
					hsfontcolor = new Color(rnd.nextInt(0xff), rnd.nextInt(0xff), rnd.nextInt(0xff));
				}
				priorletter = letter;

				// Poll the keyboard
				keyboard.poll();
				try {

					Thread.sleep(KernalSleepTime);

				} catch (InterruptedException e) {
					System.out.println(e);

				}
			}

			finally {
				// Release resources

				if (highscore_graphics != null)

					highscore_graphics.dispose();
			}
		}
		if (!(keyboard.keyDown(KeyEvent.VK_ESCAPE))) {
			initials = hsbuf;
			return (initials);
		} else
			return (null);
	}

	public boolean maze_fits_on_screen(int dx, int dy, Maze maze) {
		if ((maze.maze_x + dx) > 0)
			return (false);
		if ((maze.maze_x + dx + maze.maze_pixel_width * maze_zoom) < FRAME_WIDTH)
			return (false);
		if ((maze.maze_y + dy) > 0)
			return (false);
		if ((maze.maze_y + dy + maze.maze_pixel_height * maze_zoom) < FRAME_HEIGHT)
			return (false);
		// if ((maze_y - FRAME_HEIGHT+dy) < maze_pixel_height*maze_zoom) return
		// (false);
		return (true);
	}

	public boolean player_on_screen(int dx, int dy, Player player) {
		if ((player.player_x + dx) < 0)
			return (false);
		if ((player.player_x + dx) > FRAME_WIDTH)
			return (false);
		if ((player.player_y + dy) < 0)
			return (false);
		if ((player.player_y + dy) > FRAME_HEIGHT)
			return (false);
		return (true);
	}

	public void draw_player(Graphics backbuffer, Maze maze, Player player, List rusherList) {
		Rusher rusher = (Rusher) rusherList.get(player.rushian);
		BufferedImage player_img = rusher.spritesheet.getSubimage(player.animationFrame / AnimationSpeed * 16,
				player.player_facing_direction * 16, player.player_width / 2, player.player_height / 2);
		backbuffer.drawImage(player_img, player.player_x - player.player_center_w,
				player.player_y - player.player_center_h, player.player_width, player.player_height, null);
		// java2s.com/Tutorial/Java/0261__2D-Graphics/
	}

	public void draw_coins(Graphics backbuffer, List coins, Maze maze) {
		Coin coin = (Coin) coins.get(0);
		BufferedImage coin_img = coin.spritesheet.getSubimage(coin.animationFrame / AnimationSpeed * 16, 0,
				coin.width / 2, coin.height / 2);
		coin.animationFrame++;
		if (coin.animationFrame >= MaxanimationFrames)
			coin.animationFrame = 0;
		coins.set(0, coin);

		for (int thiscoin = 0; thiscoin < coins.size(); thiscoin++) {
			coin = (Coin) coins.get(thiscoin);
			int coinx = maze.maze_x + coin.x * maze_zoom - coin.width / 2 + maze_zoom / 2;
			int coiny = maze.maze_y + coin.y * maze_zoom - coin.height / 2 + maze_zoom / 2;
			if ((coinx < (FRAME_WIDTH + coin.width) && coinx > -coin.width)
					&& (coiny < (FRAME_HEIGHT + coin.height) && coiny > -coin.height))

				backbuffer.drawImage(coin_img, coinx, coiny, coin.width, coin.height, null);
		}
	}

	public int coinCollision(Player player, List coins, Maze maze) {
		Coin coin;
		int collected = 0;
		int dx = 0, dy = 0;
		int pxright = (player.player_x + dx + player.player_center_w);
		int pybottom = (player.player_y + dy + player.player_center_h);
		int pxleft = (player.player_x + dx - player.player_center_w);
		int pytop = (player.player_y + dy - player.player_center_h);

		for (Iterator<Coin> iter = coins.listIterator(); iter.hasNext();) {
			coin = (Coin) iter.next();
			int coinx = maze.maze_x + coin.x * maze_zoom - coin.width / 2 + maze_zoom / 2;
			int coiny = maze.maze_y + coin.y * maze_zoom - coin.height / 2 + maze_zoom / 2;
			if ((coinx < (FRAME_WIDTH + coin.width) && coinx > -coin.width)
					&& (coiny < (FRAME_HEIGHT + coin.height) && coiny > -coin.height)) {
				int cl = coinx;
				int cr = coinx + coin.width;
				int ct = coiny;
				int cb = coiny + coin.height;

				if ((cl >= pxleft && cl <= pxright && ct >= pytop && ct <= pybottom)
						|| (cr >= pxleft && cr <= pxright && ct >= pytop && ct <= pybottom)
						|| (cl >= pxleft && cl <= pxright && cb >= pytop && cb <= pybottom)
						|| (cr >= pxleft && cr <= pxright && cb >= pytop && cb <= pybottom)) {
					collected++;
					iter.remove();


				}

			}
		}
		return (collected);
	}

	public boolean player_on_color(int pixelcolor, int dx, int dy, Maze maze, Player player) {

		int pxright = (player.player_x + dx - maze.maze_x + player.player_center_w) / maze_zoom;
		int pybottom = (player.player_y + dy - maze.maze_y + player.player_center_h) / maze_zoom;
		int pxleft = (player.player_x + dx - maze.maze_x - player.player_center_w) / maze_zoom;
		int pytop = (player.player_y + dy - maze.maze_y - player.player_center_h) / maze_zoom;

		if (pxright >= maze.maze_pixel_width || pybottom >= maze.maze_pixel_height) // only
			// bottom
			// and
			// right
			// cause
			// outofbounds
			// exception
			// so
			// we
			// just
			// check
			// those
			return (false);

		if (maze.maze_img.getRGB(pxleft, pytop) == pixelcolor)
			return (true);

		if (maze.maze_img.getRGB(pxright, pytop) == pixelcolor)
			return (true);

		if (maze.maze_img.getRGB(pxleft, pybottom) == pixelcolor)
			return (true);

		if (maze.maze_img.getRGB(pxright, pybottom) == pixelcolor)
			return (true);

		return (false);
	}

	public boolean player_on_path(int dx, int dy, Maze maze, Player player) {

		int pxright = (player.player_x + dx - maze.maze_x + player.player_center_w) / maze_zoom;
		int pybottom = (player.player_y + dy - maze.maze_y + player.player_center_h) / maze_zoom;
		int pxleft = (player.player_x + dx - maze.maze_x - player.player_center_w) / maze_zoom;
		int pytop = (player.player_y + dy - maze.maze_y - player.player_center_h) / maze_zoom;

		if (pxright >= maze.maze_pixel_width || pybottom >= maze.maze_pixel_height) // only
			// bottom
			// and
			// right
			// cause
			// outofbounds
			// exception
			// so
			// we
			// just
			// check
			// those
			return (false);

		if (maze.maze_img.getRGB(pxleft, pytop) != mazepathcolor
				&& maze.maze_img.getRGB(pxleft, pytop) != mazeorigincolor
				&& maze.maze_img.getRGB(pxleft, pytop) != mazegoalcolor)
			return (false);

		if (maze.maze_img.getRGB(pxright, pytop) != mazepathcolor
				&& maze.maze_img.getRGB(pxright, pytop) != mazeorigincolor
				&& maze.maze_img.getRGB(pxright, pytop) != mazegoalcolor)
			return (false);

		if (maze.maze_img.getRGB(pxleft, pybottom) != mazepathcolor
				&& maze.maze_img.getRGB(pxleft, pybottom) != mazeorigincolor
				&& maze.maze_img.getRGB(pxleft, pybottom) != mazegoalcolor)
			return (false);

		if (maze.maze_img.getRGB(pxright, pybottom) != mazepathcolor
				&& maze.maze_img.getRGB(pxright, pybottom) != mazeorigincolor
				&& maze.maze_img.getRGB(pxright, pybottom) != mazegoalcolor)
			return (false);

		return (true);
	}


	/*
	 * rle = "R30D78L12"
	 * 
	 * parsepos = 0 powtick = 84 cmove = 0 movecounter = 0
	 * 
	 * while(cmove < powtick and parsepos < len(rle)): while(movecounter == 0):
	 * cchar = rle[parsepos] if(cchar.isalpha()): cdir = cchar parsepos+=1 cnum
	 * = 0 elif(cchar.isdigit()): while(rle[parsepos].isdigit() and parsepos <
	 * len(rle)): cnum = cnum * 10 cnum += int(rle[parsepos]) parsepos+= 1 if
	 * parsepos >= len(rle): break movecounter = cnum else: cdir = "x" break
	 * cmove+=1 movecounter -= 1 if powtick != cmove: cdir = "x" print cdir
	 */
	/*
	 * class Pow{ String rle = new String ("&"); int cmove = 0; int powtick; int
	 * powcount = 0;
	 * 
	 * }
	 */
	public int powplayback(Player player) {
		player.cmove = 0;
		if (!player.powplayback_enabled)
			return pstill;
		int parsepos = 0;
		int movecounter = -1;
		int cdir = 0, cnum = 0;

		while (player.cmove < player.powtick && parsepos < player.rle.length()) {
			while (movecounter < 0 && parsepos < player.rle.length()) {
				int cchar = (int) player.rle.charAt(parsepos);
				if (cchar >= 65 & cchar <= 90) { /* isAlpha? */
					cdir = cchar - 65;

					parsepos++;
				} else if (cchar >= 48 & cchar <= 57) { /* isDigit? */
					cnum = 0;

					while (isDigit(Character.toString(player.rle.charAt(parsepos))) && parsepos < player.rle.length()) {
						cnum *= 10;
						cnum += (int) (player.rle.charAt(parsepos)) - 48;
						parsepos++;
						if (parsepos >= player.rle.length())
							break;
					}
					movecounter = cnum;
				} else {
					cdir = pstill;
					break;
				}
			}
			player.cmove++;
			movecounter--;
		}
		if (player.powtick != player.cmove)
			cdir = pstill;
		return cdir;

	}

	/*
	 * int powdir = pstill, lastdir = pstill; int parsepos = 0;
	 * if(maze.powenabled) { char parsechar = maze.pow.charAt(parsepos);
	 * if(Character.isLetter(parsechar)){ powdir = parsechar; lastdir = powdir;
	 * } else if (Character.isDigit(parsechar)) return(powdir); } else {
	 * return(powdir); } return(powdir); }
	 */
	public boolean isAlpha(String name) {
		return name.matches("[a-zA-Z]+");
	}

	public boolean isDigit(String name) {
		return name.matches("[0-9]+");
	}

	public Player update_player(boolean scrollonly, Maze maze, Player player) {
		player.moving = false;
		// Check pow
		int powdir = powplayback(player);
		if (player.powplayback_enabled) {
			player.powtick++;
		}
		// Check keyboard
		player.player_moving_direction = 0; /*
		 * clear before setting bits for
		 * directions
		 */
		if (keyboard.keyDown(KeyEvent.VK_W) || keyboard.keyDown(KeyEvent.VK_UP) || ((powdir & pup) > 0)) {
			player.moving = true;
			player.player_moving_direction |= pup;
			if (player_on_path(0, -player.player_dy, maze, player))
				if ((maze_fits_on_screen(0, player.player_dy, maze) || scrollonly)
						&& player.player_y <= FRAME_HEIGHT / 2)
					maze.maze_y += player.player_dy;
				else if (player_on_screen(0, -player.player_dy, player))
					player.player_y -= player.player_dy;
		}
		if (keyboard.keyDown(KeyEvent.VK_A) || keyboard.keyDown(KeyEvent.VK_LEFT) || ((powdir & pleft) > 0)) {
			player.moving = true;
			player.player_moving_direction |= pleft;
			if (player_on_path(-player.player_dx, 0, maze, player))
				if ((maze_fits_on_screen(player.player_dx, 0, maze) || scrollonly)
						&& player.player_x <= FRAME_WIDTH / 2)
					maze.maze_x += player.player_dx;
				else if (player_on_screen(-player.player_dx, 0, player))
					player.player_x -= player.player_dx;
		}
		if (keyboard.keyDown(KeyEvent.VK_S) || keyboard.keyDown(KeyEvent.VK_DOWN) || ((powdir & pdown) > 0)) {
			player.moving = true;
			player.player_moving_direction |= pdown;
			if (player_on_path(0, player.player_dy, maze, player))
				if ((maze_fits_on_screen(0, -player.player_dy, maze) || scrollonly)
						&& player.player_y >= FRAME_HEIGHT / 2)
					maze.maze_y -= player.player_dy;
				else if (player_on_screen(0, player.player_dy, player))
					player.player_y += player.player_dy;
		}
		if (keyboard.keyDown(KeyEvent.VK_D) || keyboard.keyDown(KeyEvent.VK_RIGHT) || ((powdir & pright) > 0)) {
			player.moving = true;
			player.player_moving_direction |= pright;
			if (player_on_path(player.player_dx, 0, maze, player))
				if ((maze_fits_on_screen(-player.player_dx, 0, maze) || scrollonly)
						&& player.player_x >= FRAME_WIDTH / 2)
					maze.maze_x -= player.player_dx;
				else if (player_on_screen(player.player_dx, 0, player))
					player.player_x += player.player_dx;
		}
		if (player.moving) {
			if ((player.player_moving_direction & pup) > 0)
				player.player_facing_direction = 1;
			if ((player.player_moving_direction & pdown) > 0)
				player.player_facing_direction = 3;
			if ((player.player_moving_direction & pleft) > 0)
				player.player_facing_direction = 2;
			if ((player.player_moving_direction & pright) > 0)
				player.player_facing_direction = 0;

			player.animationFrame++;
			if (player.animationFrame >= MaxanimationFrames)
				player.animationFrame = 0;

		} else {
			player.player_moving_direction = pstill;
		}
		return (player);
	}

	public static void savehighscores(String maze, ScoreArrays scoreArrays) {
		JSONArray times = new JSONArray();
		JSONArray initials = new JSONArray();
		JSONArray pows = new JSONArray();
		JSONArray coins = new JSONArray();

		// JSONObject mazeshigh = new JSONObject();
		for (int i = 0; i < 10; i++) {
			times.add(scoreArrays.times[i]);
			initials.add(scoreArrays.initials[i]);
			pows.add(scoreArrays.pows[i]);
			coins.add(scoreArrays.coins[i]);
		}

		JSONObject mazehigh = new JSONObject();

		mazehigh.put("mazename", maze);
		mazehigh.put("times", times);
		mazehigh.put("initials", initials);
		mazehigh.put("pows", pows);
		mazehigh.put("coins", coins);

		// attempt to write new highscore JSONObject to file highscores.json
		try {
			FileWriter file = new FileWriter("mazes/" + maze + ".highscore");
			file.write(mazehigh.toJSONString());
			file.flush();
			file.close();

		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}

	public static JSONObject gethighscoretable(String maze) {
		JSONParser parser = new JSONParser();
		try {

			Object obj = parser.parse(new FileReader("mazes/" + maze + ".highscore"));
			JSONObject mazehigh = (JSONObject) obj;
			return (mazehigh);

		} catch (IOException | ParseException e) {
			Random rnd = new Random();
		//	rnd.setSeed(555);
			// if highscore table doesn't exist, create one.

			JSONArray times = new JSONArray();
			JSONArray initials = new JSONArray();
			JSONArray pows = new JSONArray();
			JSONArray coins = new JSONArray();

			JSONObject mazeshigh = new JSONObject();
			for (int i = 0; i < 10; i++) {
				times.add(new Long((rnd.nextLong() & 0xffff) + 0x8000));
				coins.add(new Long(0L));
				initials.add("bot");
				pows.add(new String(""));

			}

			JSONObject mazehigh = new JSONObject();

			mazehigh.put("mazename", maze);
			mazehigh.put("times", times);
			mazehigh.put("initials", initials);
			mazehigh.put("pows", pows);
			mazehigh.put("coins", coins);

			// attempt to write new highscore JSONObject to file highscores.json
			try {
				FileWriter file = new FileWriter("mazes/" + maze + ".highscore");
				file.write(mazehigh.toJSONString());
				file.flush();
				file.close();

			} catch (IOException e2) {
				e2.printStackTrace();
			}
			return (mazehigh);

		}
	}

	public static boolean checkIfHighscore(long score, ScoreArrays scoreArrays) {
		if (score < getScore(scoreArrays, 9))
			return (true);
		return (false);
	}

	public static ScoreArrays converthighscores(JSONObject mazescores) {
		ScoreArrays scoreArrays = new ScoreArrays();
		JSONArray times = (JSONArray) mazescores.get("times");
		JSONArray initials = (JSONArray) mazescores.get("initials");
		JSONArray pows = (JSONArray) mazescores.get("pows");
		JSONArray coins = (JSONArray) mazescores.get("coins");

		for (int index = 0; index < 10; index++) {
			scoreArrays.times[index] = (Long) times.get(index); // (Long) type
			// conversion
			// between
			// JSON and
			// java
			// objects
			scoreArrays.initials[index] = (String) initials.get(index);
			scoreArrays.pows[index] = (String) pows.get(index);
			
			Long coinL = (Long) coins.get(index);
			scoreArrays.coins[index] = coinL.intValue();

		}
		return scoreArrays;
	}

	public static JSONArray getpow(JSONObject mazescores) {
		System.out.println(mazescores);
		JSONArray pow = (JSONArray) mazescores.get("pows");
		return (pow);

	}

	public static int findhighscore(ScoreArrays scoreArrays, Long score, String initials) {
		for (int index = 0; index < 10; index++) {
			if (getScore(scoreArrays, index) == score && scoreArrays.initials[index] == initials) {
				return (index);
			}
		}
		return (-1);
	}

	public static void sorthighscores(ScoreArrays sortedScoreArrays) {
		int left = 0;
		int right = 9;
		scoreQuickSort(sortedScoreArrays, left, right);
	}
    public static void displayhighscores(ScoreArrays scoreArrays, Graphics graphics, int hsx, int hsy,
			int lasthighscoreidx) {
    	final int xpos_rank = 10,
    		    xpos_initials = xpos_rank + 44,
    		    xpos_coins = xpos_initials + 56,
    		    xpos_time = xpos_coins + 44,
    		    xpos_score = xpos_time + 80,
    		    xpos_width = xpos_score + 80;
    			
		// print lasths
		sorthighscores(scoreArrays);
		int left = 0;
		int right = 9;
		scoreQuickSort(scoreArrays, left, right);
		final int ystep = 22;
		final int starty = hsy + ystep*2;
		graphics.setColor(Color.black);
		graphics.fillRect(hsx , hsy , hsx + xpos_width, ystep * 12);
		graphics.setColor(Color.yellow);
		graphics.drawString(String.format("HIGHSCORES"), hsx, hsy + ystep);
		int totalCoins = 0;
		int printy = 0;
		for (int index = 0; index < 10; index++) {
			printy = starty + ystep * index;
			// String temp =(String)scores.get(index);
			// long time = Long.parseLong(temp);
			graphics.setColor(Color.blue);
			graphics.drawString(String.format("%d", index + 1), hsx + xpos_rank, printy);
			long time = scoreArrays.times[index];
//			long time = getScore(scoreArrays, index);
			if (index == lasthighscoreidx) // TODO does not work, need to fix
				graphics.setColor(Color.red);
			else
				graphics.setColor(Color.white);
			int coins = scoreArrays.coins[index];
			totalCoins += coins;
			graphics.drawString(String.format("%s", scoreArrays.coins[index]), hsx + xpos_coins, printy);
			graphics.drawString(String.format("%s", scoreArrays.initials[index]), hsx + xpos_initials, printy);
			graphics.drawString(String.format("%d.%02d", time / 1000, time % 1000 / 10), hsx + xpos_time, printy);
			long score = getScore(scoreArrays, index);
			graphics.setColor(Color.PINK);
			graphics.drawString(String.format("%d.%02d", score / 1000, score % 1000 / 10), hsx + xpos_score, printy);
		}
		printy += ystep;
		graphics.drawString(String.format("%s", totalCoins), hsx, printy);
	}

    public static long getScore(ScoreArrays scoreArrays, int index){
 	   return(scoreArrays.times[index] - scoreArrays.coins[index]*coinreward*1000);
     }
    public static long getScore(long time, int coins){
 	   return(time - coins*coinreward*1000);
     }
 	
	static int partition(ScoreArrays scoreArrays, int left, int right)

	{

		int i = left, j = right;

		Long tmpl;
		int tmpi;
		String stmp;

		Long pivot = getScore(scoreArrays, (left + right) / 2);

		while (i <= j) {

			while (getScore(scoreArrays, i) < pivot)

				i++;

			while (getScore(scoreArrays, j) > pivot)

				j--;

			if (i <= j) {

				tmpl = scoreArrays.times[i];

				scoreArrays.times[i] = scoreArrays.times[j];

				scoreArrays.times[j] = tmpl;
				
				tmpi = scoreArrays.coins[i];

				scoreArrays.coins[i] = scoreArrays.coins[j];

				scoreArrays.coins[j] = tmpi;

				stmp = scoreArrays.initials[i];

				scoreArrays.initials[i] = scoreArrays.initials[j];

				scoreArrays.initials[j] = stmp;

				stmp = scoreArrays.pows[i];

				scoreArrays.pows[i] = scoreArrays.pows[j];

				scoreArrays.pows[j] = stmp;

				i++;

				j--;

			}

		}
		;

		return i;

	}

	static void scoreQuickSort(ScoreArrays scoreArrays, int left, int right) {
		int index = partition(scoreArrays, left, right); // TODO check sorting
		if (left < index - 1)
			scoreQuickSort(scoreArrays, left, index - 1);
		if (index < right)
			scoreQuickSort(scoreArrays, index, right);
	}

	public static JSONArray findmazefiles() {

		JSONArray mazelist = new JSONArray();
		mazelist.add("dummy");

		Path dir = Paths.get("");
		dir = dir.resolve("mazes");

		// System.out.format("%s%n",dir.toAbsolutePath());
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
			for (Path entry : stream) {
				String fname = new String(entry.getFileName().toString());
				mazelist.add(fname);
				/*
				 * if(fname.contains("maze")) { mazelist.add(fname); }
				 */
			}
		} catch (IOException x) {
			System.err.println(x);
		}
		System.out.println(mazelist);
		return (mazelist);
	}

	//was:public void readPNGchunk(final String[] args) throws IOException {
	public static String readPNGchunk(File fileIn, String keyword) throws IOException {
		try (ImageInputStream input = ImageIO.createImageInputStream(fileIn)) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			ImageReader reader = readers.next(); // TODO: Validate that there are readers

			reader.setInput(input);
			String value = getTextEntry(reader.getImageMetadata(0), keyword);

			return(value);
		}
		//return(null);
	}

	private String createOutputName(final File file) {
		String name = file.getName();
		int dotIndex = name.lastIndexOf('.');

		String baseName = name.substring(0, dotIndex);
		String extension = name.substring(dotIndex);

		return baseName + "_copy" + extension;
	}

	public void addTextEntry(final IIOMetadata metadata, final String key, final String value) throws IIOInvalidTreeException {
		IIOMetadataNode textEntry = new IIOMetadataNode("TextEntry");
		textEntry.setAttribute("keyword", key);
		textEntry.setAttribute("value", value);

		IIOMetadataNode text = new IIOMetadataNode("Text");
		text.appendChild(textEntry);

		IIOMetadataNode root = new IIOMetadataNode(IIOMetadataFormatImpl.standardMetadataFormatName);
		root.appendChild(text);

		metadata.mergeTree(IIOMetadataFormatImpl.standardMetadataFormatName, root);
	}

	private static String getTextEntry(final IIOMetadata metadata, final String key) {
		IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
		//       NodeList entries = root;
		
		NodeList entries = root.getElementsByTagName("TextEntry");

		for (int i = 0; i < entries.getLength(); i++) {
			IIOMetadataNode node = (IIOMetadataNode) entries.item(i);
			//System.out.println("PNGvalue="+node.getAttribute("value"));
			if (node.getAttribute("keyword").equals(key)) {
				return node.getAttribute("value");
			}
		}

		return null;
	}
	public static JSONArray findRushers() {

		JSONArray mazelist = new JSONArray();
		mazelist.add("dummy");

		Path dir = Paths.get("");
		dir = dir.resolve("mazes");

		// System.out.format("%s%n",dir.toAbsolutePath());
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
			for (Path entry : stream) {
				String fname = new String(entry.getFileName().toString());
				mazelist.add(fname);
				/*
				 * if(fname.contains("maze")) { mazelist.add(fname); }
				 */
			}
		} catch (IOException x) {
			System.err.println(x);
		}
		System.out.println(mazelist);
		return (mazelist);
	}
	public static List getRushers(String rushersDir){
		List<Rusher> rushers = new ArrayList<Rusher>();
		File rusherFile = null;
		Path dir = Paths.get("");
		dir = dir.resolve(rushersDir);
		// System.out.format("%s%n",dir.toAbsolutePath());
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
			for (Path entry : stream) {
			    String fname = new String(entry.getFileName().toString());
				rusherFile = new File(rushersDir+fname);
				Rusher tempRusher = new Rusher();
				
				tempRusher.spritesheet = ImageIO.read(rusherFile);
				tempRusher.titleTag = readPNGchunk(rusherFile, "Title");
				tempRusher.name = fname;
				System.out.println(tempRusher.name + "  Title=" + tempRusher.titleTag);
				
				rushers.add(tempRusher);
			}
		} catch (IOException x) {
			System.out.println("can't read sprite file");
			System.err.println(x);
		}


		return(rushers);
	}
	//title=zombie.png,coins=215,music=zombie.mod,acceleration=8,bounce=0,deceleration=8,maxspeed=8
	public static void mazeSelectSpriteSelector(JSONArray mazelist, Graphics graphics, int mazecount,
			Player player, int thumbnailzoom, int thumbnailheight, int thumbnailwidth, AnimatedSprite bouncylock, AnimatedSprite arrow, List rusherList) {
		player.player_moving_direction = pleft;
		player.player_x = thumbnailwidth * thumbnailzoom;
		player.spritesheet_player_width = player.spritesheet.getWidth(null) / spritesheeth;
		player.spritesheet_player_height = player.spritesheet.getHeight(null) / spritesheetv;
	
		BufferedImage player_img = null;
		int y = 325;
		int x = 400;
		for (int rushian_number = 0; rushian_number < rusherList.size(); rushian_number++) {
			Rusher rusher = (Rusher) rusherList.get(rushian_number);
			player_img = rusher.spritesheet.getSubimage(player.animationFrame / AnimationSpeed * 16,
					player.player_facing_direction * 16, player.spritesheet_player_width,
					player.spritesheet_player_height);
			graphics.drawImage(player_img, x, y, player.player_width, player.player_height, null);
			if (rusher.titleTag != null) {
				String title = rusher.titleTag.toLowerCase();
				int coinsIndex = rusher.titleTag.toLowerCase().indexOf("coins=");
				if (coinsIndex<0) System.out.println("No coins found for " + rusher.name);
				else {
					coinsIndex += 6;
					int commaIndex = title.indexOf(",", coinsIndex);
					if (commaIndex > 0) {
						String coinsRequired = title.substring(coinsIndex, commaIndex);
						graphics.drawString(coinsRequired, x, y+64);
						//System.out.println("coins for rusher = " + coinsRequired);
					}
				}
			}
			bouncylock.x = x;
			bouncylock.y = y;
			if(getTotalCoins(mazelist, player) <= getRushianPrice(rushian_number, rusherList))
				drawAnimatedSprite(bouncylock, graphics);
			x += 40;
			
			arrow.x = 400 + player.rushian * 40;
			arrow.y = y + 16;
			drawAnimatedSprite(arrow, graphics);
		}
		bouncylock.animationFrame ++;
		arrow.animationFrame ++;
		
	}
	public static int getRushianPrice(int rushian, List rusherList){
		Rusher rusher = (Rusher) rusherList.get(rushian);
		int coinsRequired = -1;
		if (rusher.titleTag != null) {
			String title = rusher.titleTag.toLowerCase();
			int coinsIndex = rusher.titleTag.toLowerCase().indexOf("coins=");
			if (coinsIndex<0) System.out.println("No coins found for " + rusher.name);
			else {
				coinsIndex += 6;
				int commaIndex = title.indexOf(",", coinsIndex);
				if (commaIndex > 0) {
				//	Long coinsRequiredL = (Long) (title.substring(coinsIndex, commaIndex));
					coinsRequired = Integer.parseInt(title.substring(coinsIndex, commaIndex));
					//System.out.println("coins for rusher = " + coinsRequired);
				}
			}
		}
		return coinsRequired;
	}
	public static int getTotalCoins(JSONArray mazelist, Player player){
		int currentRushian = player.rushian;
		ScoreArrays scoreArrays = converthighscores(gethighscoretable(mazelist.get(player.currentMaze).toString()));
		int totalCoins = 0;
		for (int i=0;i<10;i++) {
			totalCoins += scoreArrays.coins[i];
		}
		return totalCoins;
	}
	public static void drawBouncyLock(int x, int y, Graphics graphics){
		BufferedImage lockImg = null;
		try{
			URL lockURL = new URL("file:resources/BouncyLock.png");
			lockImg = ImageIO.read(lockURL);
		}
		catch (IOException e){
			System.out.println(e);
		}
		graphics.drawImage(lockImg.getSubimage(1 / AnimationSpeed * 16,
				0, 16,
				16), x, y, 32, 32, null);

	}
	public static void drawAnimatedSprite(AnimatedSprite sprite, Graphics graphics){
		int frame = (sprite.animationFrame / AnimationSpeed) % sprite.numFrames;
		int subx = 16*(frame % 4);
		int suby = 16*(frame / 4);
		graphics.drawImage(sprite.spritesheet.getSubimage(subx, suby, 16, 16), sprite.x, sprite.y, 32, 32, null);

	}
	public static void mazeSelectPlayersprite(int topmaze, JSONArray mazelist, Graphics graphics, int mazecount,
			Player player, int thumbnailzoom, int thumbnailheight, int thumbnailwidth, List rusherList) {
		player.player_moving_direction = pleft;
		player.player_x = thumbnailwidth * thumbnailzoom;
		player.spritesheet_player_width = player.spritesheet.getWidth(null) / spritesheeth;
		player.spritesheet_player_height = player.spritesheet.getHeight(null) / spritesheetv;
		BufferedImage player_img = null;
		
		Rusher rusher = (Rusher) rusherList.get(player.rushian);
		player_img = rusher.spritesheet.getSubimage(player.animationFrame / AnimationSpeed * 16,
				player.player_facing_direction * 16, player.spritesheet_player_width, player.spritesheet_player_height);
		
		
//		player_img = player.spritesheet.getSubimage(player.animationFrame / AnimationSpeed * 16,
	//			player.player_facing_direction * 16, player.spritesheet_player_width, player.spritesheet_player_height);
		player.animationFrame++;
		if (player.animationFrame >= MaxanimationFrames)
			player.animationFrame = 0;
		graphics.drawImage(player_img, player.player_x, player.player_y, player.player_width, player.player_height,
				null);
	}

	public static void displayMazeThumbs(int topmaze, JSONArray mazelist, Graphics graphics, int mazecount,
			Player player, int thumbnailzoom, int thumbnailheight, int thumbnailwidth) {
		//Font MazeSelectFont = new Font(Font.MONOSPACED, Font.PLAIN, 20);
		Font MazeSelectFont =  gameFont.deriveFont(16f);

		graphics.setFont(MazeSelectFont);
		graphics.setColor(Color.white);
		BufferedImage mazeimage = null;
		int y = 0;
		while (y < FRAME_HEIGHT && topmaze < mazecount) {
			try {
				// System.out.println(mazelist.get(topmaze).toString());
				mazeimage = ImageIO.read(new File("mazes/" + mazelist.get(topmaze).toString()));
			} catch (IOException e) {
				System.out.println(e);
			}
			String mazename = mazelist.get(topmaze).toString();
			mazename = mazename.replaceAll(".png", "");
			graphics.drawString(mazename, thumbnailwidth * thumbnailzoom + player.player_width,
					y + mazeimage.getHeight() / 2);
			graphics.drawImage(mazeimage.getSubimage(0, 0, thumbnailwidth, thumbnailheight), 0, y,
					thumbnailwidth * thumbnailzoom, thumbnailheight * thumbnailzoom, null);
			y += thumbnailheight * thumbnailzoom;
			topmaze++;
		}
	}

	public static int mazeSelect(JSONArray mazelist, BufferStrategy buffer, KeyboardInput keyboard, int mazecount,
			Player player, int lasthighscoreidx, ScoreArrays scoreArrays, 
			AnimatedSprite bouncylock, AnimatedSprite arrow, List rusherList) {
		int lastmaze = player.currentMaze; // storing last maze so we can use for
		// highscore highlight
		int thumbnailheight = FRAME_HEIGHT / maze_zoom;
		// int thumbnailheight = 16;
		int thumbnailwidth = FRAME_WIDTH / maze_zoom;
		int thumbnailzoom = 8;
		int cursory = 0;
		keyboard.poll();
		player.animationFrame = 0;
		player.player_moving_direction = 0;
		player.player_x = 0;
		player.player_y = 0;
		/*
		 * TimerTask objectupdatetimer = new TimerTask() { public void run() {
		 * objectupdatetick ++;
		 * 
		 * } }; Timer obt = new Timer();
		 * obt.scheduleAtFixedRate(objectupdatetimer, mazeselect_bandwidth,
		 * mazeselect_bandwidth);
		 */
		scoreArrays = converthighscores(
				gethighscoretable(mazelist.get(player.currentMaze + cursory).toString()));
		Graphics graphics = buffer.getDrawGraphics();
		objectupdatetick = 0;
		boolean mazeSelectPending = true;
		int returnValue = 0;
		Saying saying = null;
		while (mazeSelectPending){
		
			if (objectupdatetick > 0) {
				objectupdatetick --;

				graphics.setColor(Color.BLACK);
				graphics.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
				player.player_y = cursory * thumbnailzoom * thumbnailheight;
				displayMazeThumbs(player.currentMaze, mazelist, graphics, mazecount, player, thumbnailzoom, thumbnailheight,
						thumbnailwidth);

				mazeSelectPlayersprite(player.currentMaze, mazelist, graphics, mazecount, player, thumbnailzoom,
						thumbnailheight, thumbnailwidth, rusherList);
				mazeSelectSpriteSelector(mazelist, graphics, mazecount, player, thumbnailzoom,
						thumbnailheight, thumbnailwidth, bouncylock, arrow, rusherList);
				
				if ((player.currentMaze + cursory) == lastmaze)
					displayhighscores(scoreArrays, graphics, FRAME_WIDTH / 2, 0, lasthighscoreidx);
				else
					displayhighscores(scoreArrays, graphics, FRAME_WIDTH / 2, 0, -1);
				
				processSaying(saying,graphics);
				buffer.show();

			}
            keyboard.poll();
			if (keyboard.keyDownOnce(KeyEvent.VK_W) || keyboard.keyDownOnce(KeyEvent.VK_UP)) {
				cursory -= 1;
				scoreArrays = converthighscores(gethighscoretable(mazelist.get(player.currentMaze + cursory).toString()));
			}
			if ((keyboard.keyDownOnce(KeyEvent.VK_S) || keyboard.keyDownOnce(KeyEvent.VK_DOWN))
					&& (player.currentMaze + cursory) < (mazecount - 1)) {
				cursory += 1;
				scoreArrays = converthighscores(gethighscoretable(mazelist.get(player.currentMaze + cursory).toString()));
			}
			if (cursory >= (FRAME_HEIGHT / (thumbnailheight * thumbnailzoom))) {
				cursory -= 1;
				if (player.currentMaze < mazecount - 1)
					player.currentMaze++;
			}
			if (cursory < 0) {
				cursory += 1;
				if (player.currentMaze > 1)
					player.currentMaze--;
			}
			if ((keyboard.keyDownOnce(KeyEvent.VK_LEFT) || keyboard.keyDownOnce(KeyEvent.VK_A)) && player.rushian > 0)
				player.rushian --;
			if ((keyboard.keyDownOnce(KeyEvent.VK_RIGHT) || keyboard.keyDownOnce(KeyEvent.VK_D)) && player.rushian <= (rusherList.size() - 2))
				player.rushian ++; //TODO rusherlist size is a liar
			if (keyboard.keyDownOnce(KeyEvent.VK_ENTER))
				if (getTotalCoins(mazelist, player) >= getRushianPrice(player.rushian, rusherList)){
					selectTransition3D(buffer, player.currentMaze, cursory, thumbnailzoom, thumbnailheight, thumbnailwidth, mazelist);
					returnValue = (player.currentMaze + cursory);
					mazeSelectPending = false;
				} else {
				 saying = new Saying(2000,"Not enough coins!",340,300,Color.red,Color.black,250);
				}
			if (keyboard.keyDownOnce(KeyEvent.VK_ESCAPE)) {
				returnValue = 0;
				mazeSelectPending = false;
			}
			if (keyboard.keyDownOnce(KeyEvent.VK_R)) {
				returnValue = (player.currentMaze + cursory) * -1;
				mazeSelectPending = false;
				
			}
		}
		return (returnValue);
	}

	public static void selectTransition3D(BufferStrategy buffer, int currentMaze, int cursory, int thumbnailzoom,
			int thumbnailheight, int thumbnailwidth, JSONArray mazelist) {
		float taccel = (float) 1.07;
		float x1start = 0;
		float y1start = cursory * thumbnailheight * thumbnailzoom;
		float x2start = thumbnailwidth * thumbnailzoom;
		float y2start = y1start + thumbnailheight * thumbnailzoom;
		float x1end = 0;
		float y1end = 0;
		float x2end = FRAME_WIDTH;
		float y2end = FRAME_HEIGHT;
		float x1delta = (x1end - x1start);
		float y1delta = (y1end - y1start);
		float x2delta = (x2end - x2start);
		float y2delta = (y2end - y2start);
		float x1 = x1start;
		float y1 = y1start;
		float x2 = x2start;
		float y2 = y2start;
		BufferedImage mazeimage = null;
		try {
			mazeimage = ImageIO.read(new File("mazes/" + mazelist.get(currentMaze + cursory).toString()));
		} catch (IOException e) {
		}
		float dist = (float) 0.01;
		while (dist < 1) {
			Graphics graphics = buffer.getDrawGraphics();
			x1 = x1start + x1delta * dist;
			y1 = y1start + y1delta * dist;
			x2 = x2start + x2delta * dist;
			y2 = y2start + y2delta * dist;
			graphics.drawImage(mazeimage.getSubimage(0, 0, thumbnailwidth, thumbnailheight), (int) x1, (int) y1,
					(int) (x2 - x1), (int) (y2 - y1), null);

			buffer.show();
			dist *= taccel;
			try {

				Thread.sleep(KernalSleepTime);

			} catch (InterruptedException e) {

			}
		}
	}
	public static BufferedImage enter_maze(Player player, JSONArray mazelist, ScoreArrays scoreArrays) {
		BufferedImage mazeimage = null;
		try {
			mazeimage = ImageIO.read(new File("mazes/" + mazelist.get(player.currentMaze).toString()));
		} catch (IOException e) {
		}

		scoreArrays = converthighscores(gethighscoretable(mazelist.get(player.currentMaze).toString()));
		return (mazeimage);
	}
	public static class Sound {

	    
	    private static AudioClip clip;
        /*
	    private Sound(String filename) throws MalformedURLException {
	    	clip = Applet.newAudioClip(new URL("file:"+filename));
	        //WAS: clip = Applet.newAudioClip(getClass().getClassLoader().getResource(name));
	    }*/

	    public void play() {
	        new Thread() {
	            public void run() {
	                clip.play();
	            }
	        }.start();
	    }

		public static AudioClip  sounds(File fileName) throws MalformedURLException {
			clip = Applet.newAudioClip(new URL("file:"+fileName));
			return clip;
		}

	}
	public static void sampleplayback_test(final File fileName)
			throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException 
	{
		Sound.sounds(fileName).play();
		
	}
	public static void sampleplayback(final File fileName)
			throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
		class AudioListener implements LineListener {
			private boolean done = false;

			@Override
			public synchronized void update(LineEvent event) {
				javax.sound.sampled.LineEvent.Type eventType = event.getType();
				if (eventType == javax.sound.sampled.LineEvent.Type.STOP  ||
						eventType
						==
						javax.sound.sampled.LineEvent.Type.CLOSE)
				{
					done = true;
					notifyAll();
				}
			}

			public synchronized void waitUntilDone() throws InterruptedException {
				while (!done) {
					wait();
				}
			}
		}

		new Thread(new Runnable() {

			public void run() {
				AudioListener listener = new AudioListener();

				try {
					AudioInputStream ais = AudioSystem.getAudioInputStream(fileName);
					//AudioFormat format = ais.getFormat();
					//https://stackoverflow.com/questions/18942424/error-playing-audio-file-from-java-via-pulseaudio-on-ubuntu
					//DataLine.Info info = new DataLine.Info(Clip.class, format);
					//wasClip clip = (Clip)AudioSystem.getLine(info);

					Clip clip = AudioSystem.getClip(null);
					clip.open(ais);
					clip.addLineListener(listener);
					clip.start();
					while(clip.isRunning())
					{
						Thread.yield();
					}
					listener.waitUntilDone();
					clip.removeLineListener(listener);
					clip.close();
					ais.close();
				} catch (Exception e) {

					System.out.println("public static void sampleplayback(final File fileName)");
					System.out.println(e);
				}
			}
		}).start();
	}
	final static int splashTextWaveHeight = 15, goalsplashx = 0, goalsplashy = 5, creditssplashx = 0,
			creditssplashy = 9, anykeysplashx = 5, anykeysplashy = 9, keyssplashx = 0, keyssplashy = 1;

	public static void splashtext(Graphics splashgraphics, double yoffset) {
		Font splashFont = new Font("SansSerif", Font.BOLD, 20);
		 splashFont =  gameFont.deriveFont(20f);

		
		splashgraphics.setFont(splashFont);
		splashgraphics.setColor(Color.white);
		splashgraphics.drawString(goalsplash, tenthx(goalsplashx), (int) (tenthy(goalsplashy) + yoffset));
		multilinedrawstring(keyssplash, tenthx(keyssplashx), (int) (tenthy(keyssplashy) + yoffset), splashgraphics, 20);
		splashgraphics.drawString(creditssplash, tenthx(creditssplashx), (int) (tenthy(creditssplashy) + yoffset));
		splashgraphics.drawString(anykeysplash, tenthx(anykeysplashx), (int) (tenthy(anykeysplashy) + yoffset));
	}

	public static int tenthx(int x) {
		return (x * FRAME_WIDTH / 10);
	}

	public static int tenthy(int y) {
		return (y * FRAME_HEIGHT / 10);
	}

	public static void multilinedrawstring(String rawstring, int x, int y, Graphics screen, int fontheight) {
		String[] splitstring = rawstring.split("\n");
		for (int i = 0; i < splitstring.length; i++) {
			screen.drawString(splitstring[i], x, y);
			y += fontheight;
		}
	}

	public static void splashScreen(BufferStrategy frontBuffer) {
		double yoffset = 0;
		double waveangle = 0;
		double wavespeed = 0.05;
		BufferedImage splashimage = null;
		BufferedImage titleimage = null;
		try {
			splashimage = ImageIO.read(new File("resources/splash2.png"));
		} catch (IOException e) {
			System.out.println(e);
		}
		try {
			titleimage = ImageIO.read(new File("resources/gametitle.png"));
		} catch (IOException e) {
			System.out.println(e);
		}
		BufferedImage splashtext_buffer = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g2d = splashtext_buffer.getGraphics();
		splashtext(g2d, 0);


		// BufferStrategy backBuffer = canvas.getBufferStrategy();
		// Graphics backGraphics = backBuffer.getDrawGraphics();
		BufferedImage backBuffer = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics backGraphics = backBuffer.createGraphics();
		backGraphics.drawImage(splashimage, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, null);
		backGraphics.drawImage(titleimage, FRAME_WIDTH / 2 - titleimage.getWidth() / 2, FRAME_HEIGHT / 4, null);

		Graphics frontGraphics = null;
		Mixer mixer = getmixer("resources/sommargalaxen_short.mod");
		playsong(mixer);
		boolean framevalid = false;
		objectupdatetick = 0;
		Saying saying = new Saying(5L,"Welcome Rushian!",10,10,Color.blue,Color.black,15);
		// splashKernel
		while (!keyboard.poll()) {
			// _update
			if (objectupdatetick > 0) {
				//System.out.println("update start");
				
				objectupdatetick--;
				yoffset = Math.sin(waveangle) * splashTextWaveHeight;
				waveangle += wavespeed;
	//			if (objectupdatetick == 0)
					framevalid = false;
			//	if (objectupdatetick>0)  System.out.println("ticks =" + objectupdatetick);
				//System.out.println("update end");
				
			}
			// _draw
			if (!framevalid) {
				try {
					//System.out.println("draw start");
					frontGraphics = frontBuffer.getDrawGraphics();
					
					frontGraphics.drawImage(backBuffer, 0, 0, null);
					frontGraphics.drawImage(splashtext_buffer, 0, (int) yoffset, null);
					processSaying(saying,frontGraphics);
					
									if (!frontBuffer.contentsLost())
										frontBuffer.show();
					
					framevalid = true;
					//System.out.println("draw end");
				}finally {
					//System.out.println("release start");
					// Release resources
					if (frontGraphics != null)
						frontGraphics.dispose();
					if (g2d != null)
						g2d.dispose();
					//System.out.println("release end");
				}
			}
			//if (objectupdatetick>0)  System.out.println("ticks =" + objectupdatetick);

		}
		frontGraphics.dispose();
		while (keyboard.poll()) {} // wait for key to be released
		mixer.stopPlayback();
	}
	public static void processSaying(Saying saying, Graphics graphics){
		if (saying == null)
			return;
		
		if(System.currentTimeMillis() < saying.expires) {
		
			graphics.setColor(saying.fgcol);
			if (saying.startTime == 0)
				graphics.drawString(saying.text, saying.x, saying.y);
			else {
				Long flashchunk = ((saying.startTime - System.currentTimeMillis() ) / saying.flash);
				int flashstat = flashchunk.intValue() % 2;
				if (flashstat == 0)
					graphics.drawString(saying.text, saying.x, saying.y);
			}
		}
	}
	
	public static Mixer getmixer(String fname){
		Mixer mixer = null;
		try {
			Helpers.registerAllClasses();
			File music = new File(fname);
			Properties props = new Properties();
			props.setProperty(ModContainer.PROPERTY_PLAYER_ISP, "3");
			props.setProperty(ModContainer.PROPERTY_PLAYER_STEREO, "2");
			//	        props.setProperty(ModContainer.PROPERTY_PLAYER_WIDESTEREOMIX, "FALSE");
			//        props.setProperty(ModContainer.PROPERTY_PLAYER_NOISEREDUCTION, "FALSE");
			//      props.setProperty(ModContainer.PROPERTY_PLAYER_NOLOOPS, "FALSE");
			props.setProperty(ModContainer.PROPERTY_PLAYER_MEGABASS, "TRUE");
			props.setProperty(ModContainer.PROPERTY_PLAYER_BITSPERSAMPLE, "16");
			props.setProperty(ModContainer.PROPERTY_PLAYER_FREQUENCY, "48000");
			MultimediaContainerManager.configureContainer(props);
			URL modUrl = music.toURI().toURL();
			MultimediaContainer multimediaContainer = MultimediaContainerManager.getMultimediaContainer(modUrl);
			mixer = multimediaContainer.createNewMixer();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(3);
		} catch (Exception e) {

			System.out.println("public static void sampleplayback(final File fileName)");
			System.out.println(e);
		}
		return(mixer);
	}
	public static void playsong(final Mixer mixer){
		new Thread(new Runnable() {

			public void run() {

				try {
					mixer.startPlayback();
				} catch (Exception e) {

					System.out.println("public static void sampleplayback(final File fileName)");
					System.out.println(e);
				}
			}
		}).start();
	}

	public static void main(String[] args) {
		System.setProperty("sun.java2d.opengl", "true");
		Mazerush app = new Mazerush();
		app.setTitle("Maze Rush!");
		app.setVisible(true);
		app.run();
		System.exit(0);
	}

}