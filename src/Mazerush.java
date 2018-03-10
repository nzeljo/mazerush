
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

public class Mazerush extends JFrame {
	static final long serialVersionUID = 1L;
	static final int FRAME_WIDTH = 640, 
			FRAME_HEIGHT = 480, 
			maze_zoom = 40, 
			player_speed = 8, // actual movement = mazezoom / player_speed

			objectupdate_bandwidth = 14, // Was 14, //time in milliseconds
			// between object updates
			mazeselect_bandwidth = 200, coinprobability = 100, // was 10
			maze_subimage_width = FRAME_WIDTH / maze_zoom, maze_subimage_height = FRAME_HEIGHT / maze_zoom,
			KernalSleepTime = 10, pup = 0b0001, pdown = 0b0010, pleft = 0b0100, pright = 0b1000, pstill = 0,
			AnimationSpeed = 10, // was 10,Higher number = slower
			MaxanimationFrames = 4 * AnimationSpeed;
	static String goalsplash = "Object: finish the maze as fast as possible",
			keyssplash = "WASD or arrow keys to move\nEnter to skip forward\nBackspace to skip back",
			creditssplash = "Credits", anykeysplash = "Press any key to continue";
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
		
	}
	static class Rusher {
		BufferedImage spritesheet = null;
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

	class ScoreArrays {
		Long[] times = new Long[10];
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
		
		int current_maze = 1;
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

		// GAME KERNAL
		// GAME KERNAL
		// GAME KERNAL
		while (current_maze > 0) {
			current_maze = mazeSelect(mazelist, buffer, keyboard, mazecount, player, current_maze, lasthighscoreidx,
					scoreArrays, bouncylock, arrow, rusherList);
			if (current_maze > 0) {
				Mixer mixer = getmixer("resources/Waiting for loaders.mod");
				playsong(mixer);
				doMazeRun(current_maze, player, mazelist, maze, scoreArrays, backbuffer, buffer, g2d, coins, rusherList);
				mixer.stopPlayback();
			} else if (current_maze < 0) {
				// ------------------------------------POW TEST CODE
				// ----------------
				current_maze *= -1;
				String maze_name = mazelist.get(current_maze).toString();
				JSONObject hstable = (JSONObject) gethighscoretable(maze_name);

				JSONArray maze_pow = getpow(hstable);

				if (maze_pow != null) {
					if (maze_pow.get(0) != null) {
						String mazepowstring = (String) maze_pow.get(0);
						// getmazeruntime(mazepowstring, current_maze, player,
						// mazelist, maze, scoreArrays);
						System.out.println(
								getmazeruntime(mazepowstring, current_maze, player, mazelist, maze, scoreArrays));
						player.maze_completed = false;
					}
				}
			}
		}
	}

	public void initmazerun(int current_maze, Player player, JSONArray mazelist, Maze maze, ScoreArrays scoreArrays) {
		player.player_x = maze_zoom;
		player.player_y = maze_zoom;
		player.player_moving_direction = 0;
		player.player_dx = player.player_dy = maze_zoom / player_speed;
		player.animationFrame = 0;
		player.maze_completed = false;
		maze.maze_x = 0; // all mazes start at 0,0
		maze.maze_y = 0; // could change so that we look for the mazeorigincolor
		maze.maze_img = enter_maze(current_maze, mazelist, scoreArrays);
		maze.maze_pixel_width = maze.maze_img.getWidth();
		maze.maze_pixel_height = maze.maze_img.getHeight();
		player.clockstarted = false;
	}

	public long getmazeruntime(String maze_pow, int current_maze, Player player, JSONArray mazelist, Maze maze,
			ScoreArrays scoreArrays) {

		if (maze_pow.length() > 0) {
			player.powplayback_enabled = true;
			player.powtick = 0;
			player.rle = maze_pow;
		} else
			return (-1);
		initmazerun(current_maze, player, mazelist, maze, scoreArrays);
		while (!player.maze_completed) { // TODO find a condition
			player = update_objects(maze, player);
		}
		return (player.completedtime);
	}

	public void doMazeRun(int current_maze, Player player, JSONArray mazelist, Maze maze, ScoreArrays scoreArrays,
			Graphics backbuffer, BufferStrategy buffer, Graphics2D g2d, List coins, List rusherList) {

		boolean fanfareplaying = false;
		long completed_delay = 0;
		File fanfare = new File("resources/fanfare1.wav");
		int lasthighscoreidx = -1;

		if (current_maze != 0) {
			player.powplayback_enabled = false; // default
			if (current_maze < 0) {
				current_maze *= -1;
			}
			initmazerun(current_maze, player, mazelist, maze, scoreArrays);
			coins.clear();
			placeCoins(coins, maze);
			objectupdatetick = 0;
		}
		Font timeFont = new Font("SansSerif", Font.BOLD, 20);
		int maze_overscan_x = 0;
		int maze_overscan_y = 0;
		boolean FrameValid = false;
		File footsteps = new File("resources/footstep3.wav");
		File coin_collected_sound = new File("resources/135936__bradwesson__collectcoin.wav");
		player.coinsCollected = 0;
		player.coinCollisions = 0;

		while (current_maze > 0) {
			// inner kernel loop starts here

			while (objectupdatetick > 0) {
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
				if (check_if_highscore(player.completedtime, scoreArrays)) {
					String initials = enter_highscores(backbuffer, buffer, player, rusherList);
					if (initials != null) {
						scoreArrays.times[9] = player.completedtime;
						scoreArrays.initials[9] = initials;
						scoreArrays.pows[9] = player.rle;
						sorthighscores(scoreArrays);
						savehighscores(mazelist.get(current_maze).toString(), scoreArrays);
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
				current_maze = 0;
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
					backbuffer.setColor(Color.white);
					backbuffer.fillRect(5, 20, 185, 25);
					backbuffer.setColor(Color.black);
					backbuffer.drawString(
							String.format("Time: %d.%02d", player.completedtime / 1000, player.completedtime % 1000),
							10, 40); // TODO move to other side of screen if
					// player is on top of it
					backbuffer.drawString(String.format("FPS: %d", currentFPS), 10, 80);
					backbuffer.setColor(Color.red);
					backbuffer.drawString(String.format("Coins: %d", player.coinsCollected), 10, 120);
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
		Random rnd = new Random();
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
		JSONArray scores = new JSONArray();
		JSONArray initials = new JSONArray();
		JSONArray pows = new JSONArray();

		// JSONObject mazeshigh = new JSONObject();
		for (int i = 0; i < 10; i++) {
			scores.add(scoreArrays.times[i]);
			initials.add(scoreArrays.initials[i]);
			pows.add(scoreArrays.pows[i]);
		}

		JSONObject mazehigh = new JSONObject();

		mazehigh.put("mazename", maze);
		mazehigh.put("highscores", scores);
		mazehigh.put("initials", initials);
		mazehigh.put("pows", pows);

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
			rnd.setSeed(555);
			// if highscore table doesn't exist, create one.

			JSONArray scores = new JSONArray();
			JSONArray initials = new JSONArray();
			JSONArray pows = new JSONArray();

			JSONObject mazeshigh = new JSONObject();
			for (int i = 0; i < 10; i++) {
				scores.add(new Long((rnd.nextLong() & 0xffff) + 0x8000));
				int ch = rnd.nextInt(26) + 65;
				char initchar = new Character((char) ch);
				initials.add(Character.toString(initchar));
				pows.add(new String(""));

			}

			JSONObject mazehigh = new JSONObject();

			mazehigh.put("mazename", maze);
			mazehigh.put("highscores", scores);
			mazehigh.put("initials", initials);
			mazehigh.put("pows", pows);

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

	public static boolean check_if_highscore(long completedtime, ScoreArrays scoreArrays) {
		if (completedtime < scoreArrays.times[9])
			return (true);
		return (false);
	}

	public static void converthighscores(JSONObject mazescores, ScoreArrays scoreArrays) {
		JSONArray scores = (JSONArray) mazescores.get("highscores");
		JSONArray initials = (JSONArray) mazescores.get("initials");
		JSONArray pows = (JSONArray) mazescores.get("pows");

		for (int index = 0; index < 10; index++) {
			scoreArrays.times[index] = (Long) scores.get(index); // (Long) type
			// conversion
			// between
			// JSON and
			// java
			// objects
			scoreArrays.initials[index] = (String) initials.get(index);
			scoreArrays.pows[index] = (String) pows.get(index);

		}
	}

	public static JSONArray getpow(JSONObject mazescores) {
		System.out.println(mazescores);
		JSONArray pow = (JSONArray) mazescores.get("pows");
		return (pow);

	}

	public static int findhighscore(ScoreArrays scoreArrays, Long score, String initials) {
		for (int index = 0; index < 10; index++) {
			if (scoreArrays.times[index] == score && scoreArrays.initials[index] == initials) {
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
		// print lasths
		sorthighscores(scoreArrays);
		int left = 0;
		int right = 9;
		scoreQuickSort(scoreArrays, left, right);
		final int ystep = 25;
		final int starty = hsy + ystep;
		graphics.setColor(Color.white);
		graphics.fillRect(hsx - 5, hsy - 20, 206, ystep * 11);
		graphics.setColor(Color.black);
		graphics.drawString(String.format("HIGHSCORES"), hsx, hsy);
		for (int index = 0; index < 10; index++) {
			int printy = starty + ystep * index;
			// String temp =(String)scores.get(index);
			// long time = Long.parseLong(temp);
			long time = scoreArrays.times[index];
			if (index == lasthighscoreidx) // TODO does not work, need to fix
				graphics.setColor(Color.blue);
			else
				graphics.setColor(Color.black);
			// graphics.drawString(String.format("%d. %s (%d.%02d)", index + 1,
			// sortedinitials[index], time / 1000, time % 1000 / 10), 430,
			// printy);
			graphics.drawString(String.format("%d. %s", index + 1, scoreArrays.initials[index]), hsx, printy);
			graphics.drawString(String.format("(%d.%02d)", time / 1000, time % 1000 / 10), hsx + 86, printy);
			// graphics.drawString(String.format("Time: %04d.%02d",
			// completedtime / 1000, completedtime % 1000), 10, 40);
		}
	}

	static int partition(ScoreArrays scoreArrays, int left, int right)

	{

		int i = left, j = right;

		Long tmp;
		String stmp;

		Long pivot = scoreArrays.times[(left + right) / 2];

		while (i <= j) {

			while (scoreArrays.times[i] < pivot)

				i++;

			while (scoreArrays.times[j] > pivot)

				j--;

			if (i <= j) {

				tmp = scoreArrays.times[i];

				scoreArrays.times[i] = scoreArrays.times[j];

				scoreArrays.times[j] = tmp;

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
			System.out.println("PNGvalue="+node.getAttribute("value"));
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
				tempRusher.name = fname;
				System.out.println(tempRusher);
				
				rushers.add(tempRusher);
			}
		} catch (IOException x) {
			System.out.println("can't read sprite file");
			System.err.println(x);
		}


		return(rushers);
	}
	//title=zombie.png,coins=215,music=zombie.mod,acceleration=8,bounce=0,deceleration=8,maxspeed=8
	public static void mazeSelectSpriteSelector(int topmaze, JSONArray mazelist, Graphics graphics, int mazecount,
			Player player, int thumbnailzoom, int thumbnailheight, int thumbnailwidth, AnimatedSprite bouncylock, AnimatedSprite arrow, List rusherList) {
		player.player_moving_direction = pleft;
		player.player_x = thumbnailwidth * thumbnailzoom;
		player.spritesheet_player_width = player.spritesheet.getWidth(null) / spritesheeth;
		player.spritesheet_player_height = player.spritesheet.getHeight(null) / spritesheetv;
	
		BufferedImage player_img = null;
		int y = 325;
		int x = 400;
		for (int i = 0; i < rusherList.size(); i++) {
			Rusher rusher = (Rusher) rusherList.get(i);
			player_img = rusher.spritesheet.getSubimage(player.animationFrame / AnimationSpeed * 16,
					player.player_facing_direction * 16, player.spritesheet_player_width,
					player.spritesheet_player_height);
			graphics.drawImage(player_img, x, y, player.player_width, player.player_height, null);
			bouncylock.x = x;
			bouncylock.y = y;
			drawAnimatedSprite(bouncylock, graphics);
			bouncylock.animationFrame ++;
			x += 40;
			
			arrow.x = 400 + player.rushian * 40;
			arrow.y = y + 16;
			drawAnimatedSprite(arrow, graphics);
			arrow.animationFrame ++;
		}
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
		Font MazeSelectFont = new Font(Font.MONOSPACED, Font.PLAIN, 20);
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
			Player player, int listlocation, int lasthighscoreidx, ScoreArrays scoreArrays, AnimatedSprite bouncylock, AnimatedSprite arrow, List rusherList) {

		//testcode        public void readPNGchunk(File fileIn, String keyword) throws IOException {
		try {
			File file = new File("rushians/zombie.png");

			System.out.println("zombie.png Title = " + readPNGchunk(file, "Title"));

		}
		catch (IOException e) {
			System.out.println(e);
		}
		int lastmaze = listlocation; // storing last maze so we can use for
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
		converthighscores(gethighscoretable(mazelist.get(listlocation + cursory).toString()), scoreArrays);
		Graphics graphics = buffer.getDrawGraphics();

		while (!(keyboard.keyDown(KeyEvent.VK_ENTER) || keyboard.keyDown(KeyEvent.VK_ESCAPE)
				|| keyboard.keyDown(KeyEvent.VK_R))) {
			while ((objectupdatetick >> 2) > 0) {
				objectupdatetick = 0;

				graphics.setColor(Color.BLACK);
				graphics.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
				player.player_y = cursory * thumbnailzoom * thumbnailheight;
				displayMazeThumbs(listlocation, mazelist, graphics, mazecount, player, thumbnailzoom, thumbnailheight,
						thumbnailwidth);

				mazeSelectPlayersprite(listlocation, mazelist, graphics, mazecount, player, thumbnailzoom,
						thumbnailheight, thumbnailwidth, rusherList);
				mazeSelectSpriteSelector(listlocation, mazelist, graphics, mazecount, player, thumbnailzoom,
						thumbnailheight, thumbnailwidth, bouncylock, arrow, rusherList);
				if ((listlocation + cursory) == lastmaze)
					displayhighscores(scoreArrays, graphics, FRAME_WIDTH * 3 / 5, 0, lasthighscoreidx);
				else
					displayhighscores(scoreArrays, graphics, FRAME_WIDTH * 3 / 5, 0, -1);
				buffer.show();

			}

			keyboard.poll();
			if (keyboard.keyDownOnce(KeyEvent.VK_W) || keyboard.keyDownOnce(KeyEvent.VK_UP)) {
				cursory -= 1;
				converthighscores(gethighscoretable(mazelist.get(listlocation + cursory).toString()), scoreArrays);
			}
			if ((keyboard.keyDownOnce(KeyEvent.VK_S) || keyboard.keyDownOnce(KeyEvent.VK_DOWN))
					&& (listlocation + cursory) < (mazecount - 1)) {
				cursory += 1;
				converthighscores(gethighscoretable(mazelist.get(listlocation + cursory).toString()), scoreArrays);
			}
			if (cursory >= (FRAME_HEIGHT / (thumbnailheight * thumbnailzoom))) {
				cursory -= 1;
				if (listlocation < mazecount - 1)
					listlocation++;
			}
			if (cursory < 0) {
				cursory += 1;
				if (listlocation > 1)
					listlocation--;
			}
			if ((keyboard.keyDownOnce(KeyEvent.VK_LEFT) || keyboard.keyDownOnce(KeyEvent.VK_A)) && player.rushian > 0)
				player.rushian --;
			if ((keyboard.keyDownOnce(KeyEvent.VK_RIGHT) || keyboard.keyDownOnce(KeyEvent.VK_D)) && player.rushian <= (rusherList.size() - 2))
				player.rushian ++; //TODO rusherlist size is a liar
		}
		if (keyboard.keyDownOnce(KeyEvent.VK_ENTER)) {
			selectTransition3D(buffer, listlocation, cursory, thumbnailzoom, thumbnailheight, thumbnailwidth, mazelist);
			return (listlocation + cursory);
		} else if (keyboard.keyDownOnce(KeyEvent.VK_R)) {
			return ((listlocation + cursory) * -1);
		} else
			return (0); // if escape pressed
	}

	public static void selectTransition(BufferStrategy buffer, int listlocation, int cursory, int thumbnailzoom,
			int thumbnailheight, int thumbnailwidth, JSONArray mazelist) {
		float tframes = 32;
		float x1start = 0;
		float y1start = cursory * thumbnailheight * thumbnailzoom;
		float x2start = thumbnailwidth * thumbnailzoom;
		float y2start = y1start + thumbnailheight * thumbnailzoom;
		float x1end = 0;
		float y1end = 0;
		float x2end = FRAME_WIDTH;
		float y2end = FRAME_HEIGHT;
		float x1delta = (x1end - x1start) / tframes;
		float y1delta = (y1end - y1start) / tframes;
		float x2delta = (x2end - x2start) / tframes;
		float y2delta = (y2end - y2start) / tframes;
		float x1 = x1start;
		float y1 = y1start;
		float x2 = x2start;
		float y2 = y2start;
		BufferedImage mazeimage = null;
		try {
			mazeimage = ImageIO.read(new File("mazes/" + mazelist.get(listlocation + cursory).toString()));
		} catch (IOException e) {
			System.out.println(e);
		}
		for (int i = 1; i < tframes; i++) {
			Graphics graphics = buffer.getDrawGraphics();
			graphics.drawImage(mazeimage.getSubimage(0, 0, thumbnailwidth, thumbnailheight), (int) x1, (int) y1,
					(int) (x2 - x1), (int) (y2 - y1), null);
			x1 += x1delta;
			y1 += y1delta;
			x2 += x2delta;
			y2 += y2delta;
			buffer.show();
			try {

				Thread.sleep(KernalSleepTime);

			} catch (InterruptedException e) {
				System.out.println(e);

			}
		}
	}

	public static void selectTransition3D(BufferStrategy buffer, int listlocation, int cursory, int thumbnailzoom,
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
			mazeimage = ImageIO.read(new File("mazes/" + mazelist.get(listlocation + cursory).toString()));
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

	public static void showmazes(int listlocation, BufferedImage[] mazeimages, Graphics screen) {
		int gap = FRAME_WIDTH / 7;
		int y = FRAME_HEIGHT / 2;
		screen.drawImage(mazeimages[listlocation], gap, y, 64, 64, null);
		screen.drawImage(mazeimages[listlocation + 1], gap * 3, y, 64, 64, null);
		screen.drawImage(mazeimages[listlocation + 2], gap * 5, y, 64, 64, null);
	}

	public static BufferedImage enter_maze(int current_maze, JSONArray mazelist, ScoreArrays scoreArrays) {
		BufferedImage mazeimage = null;
		try {
			mazeimage = ImageIO.read(new File("mazes/" + mazelist.get(current_maze).toString()));
		} catch (IOException e) {
		}

		converthighscores(gethighscoretable(mazelist.get(current_maze).toString()), scoreArrays);
		return (mazeimage);
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
					AudioFormat format = ais.getFormat();
					//https://stackoverflow.com/questions/18942424/error-playing-audio-file-from-java-via-pulseaudio-on-ubuntu
					DataLine.Info info = new DataLine.Info(Clip.class, format);
					Clip clip = (Clip)AudioSystem.getLine(info);

					//was Clip clip = AudioSystem.getClip();
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
		while (!keyboard.poll()) {
			// _update
			while (objectupdatetick > 0) {
				//System.out.println("update start");
				
				objectupdatetick--;
				yoffset = Math.sin(waveangle) * splashTextWaveHeight;
				waveangle += wavespeed;
				if (objectupdatetick == 0)
					framevalid = false;
				if (objectupdatetick>0)  System.out.println("ticks =" + objectupdatetick);
				//System.out.println("update end");
				
			}
			// _draw
			if (!framevalid) {
				try {
					//System.out.println("draw start");
					frontGraphics = frontBuffer.getDrawGraphics();
					
					frontGraphics.drawImage(backBuffer, 0, 0, null);
					frontGraphics.drawImage(splashtext_buffer, 0, (int) yoffset, null);

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