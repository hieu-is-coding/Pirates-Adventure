package gamestates;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.*;
import entities.*;
import levels.LevelManager;
import main.Game;
import ui.*;
import utilz.LoadSave;
import static utilz.Constants.Cloud.*;

public class Playing extends State {
	private Player player;
	private LevelManager LManager;
	private EnemyManager EManger;
	private PauseOverlay PauseO; //pause gui
	private GameOverOverlay GameO;
	private LevelCompletedOverlay LevelO;
	private boolean paused = false;
	private int xLvlOffset;
	private int maxLvlOffsetX;
	private BufferedImage backgroundImg, CLOUD;
	private boolean gameOver;
	private boolean lvlCompleted;
	private boolean playerDying;
	
	private static long timeCheck=-1, lastCheck, timeOutF=-1, timeOutL, totalTimeOut;

	public Playing(Game game) {
		super(game);
		LManager = new LevelManager(game);
		EManger = new EnemyManager(this);
		player = new Player(200, 200, (int) (64 * Game.SCALE), (int) (40 * Game.SCALE), this);
		player.loadLvlData(LManager.getCurrentLevel().getLevelData()); //loadLvl
		player.setSpawn(LManager.getCurrentLevel().getPlayerSpawn()); //spawn
		PauseO = new PauseOverlay(this);
		GameO = new GameOverOverlay(this);
		LevelO = new LevelCompletedOverlay(this);
		loadImg();
		maxLvlOffsetX = LManager.getCurrentLevel().getLvlOffset();
		EManger.loadEnemies(LManager.getCurrentLevel()); //load enemy
	}

	public void loadNextLevel() {
		resetAll();
		LManager.NextLevel();
		player.setSpawn(LManager.getCurrentLevel().getPlayerSpawn());
	}
	
	private void loadImg() {
		backgroundImg = LoadSave.GetImg("playing.png");
		CLOUD = LoadSave.GetImg("clouds.png");
	}

	public void update() {
		//get the time starting level;
		if(timeCheck==-1) timeCheck = System.currentTimeMillis();
		if (paused) {
			if(timeOutF==-1) timeOutF =  System.currentTimeMillis();
			PauseO.update();
			timeOutL = System.currentTimeMillis();
		} else if (lvlCompleted) {
			if(timeOutF==-1) timeOutF =  System.currentTimeMillis();
			LevelO.update();
			timeOutL = System.currentTimeMillis();
		} else if (gameOver) {
			if(timeOutF==-1) timeOutF =  System.currentTimeMillis();
			GameO.update();
			timeOutL = System.currentTimeMillis();
		} else if (playerDying) {
			if(timeOutF==-1) timeOutF =  System.currentTimeMillis();
			player.update();
			timeOutL = System.currentTimeMillis();
		} else {
			player.update();
			EManger.update(LManager.getCurrentLevel().getLevelData(), player);
			checkCloseBorder();
			//get last time playing and set time out
			if(timeOutF!=-1) {
				totalTimeOut += timeOutL - timeOutF;
				timeOutF = -1;
			}
			lastCheck = System.currentTimeMillis();
		}
	}
	
	public void checkCloseBorder() {
		int leftSide = (int) (0.2 * Game.GWIDTH);
		int rightSide = (int) (0.8 * Game.GWIDTH);
		int playerX = (int)player.getHitbox().x;
		
		if(playerX - xLvlOffset > rightSide) {
			xLvlOffset = playerX - rightSide; 
		}
		else if(playerX - xLvlOffset < leftSide) {
			xLvlOffset = playerX - leftSide; 
		}
		if(xLvlOffset > maxLvlOffsetX) {
			xLvlOffset = maxLvlOffsetX;
		}
		else if(xLvlOffset < 0) {
			xLvlOffset = 0;
		}
	}


	public void render(Graphics g) {
		g.drawImage(backgroundImg, 0, 0, Game.GWIDTH, Game.GHEIGHT, null);
		
		// cloud render
		for (int i = 0; i < 3; i++)
			g.drawImage(CLOUD, i * CLOUD_WID - (int) (xLvlOffset * 0.3), (int) (204 * Game.SCALE), CLOUD_WID, CLOUD_HEI, null);

		LManager.render(g, xLvlOffset);
		player.render(g, xLvlOffset);
		EManger.render(g, xLvlOffset);

		if (paused) {
			g.setColor(new Color(0, 0, 0, 150));
			g.fillRect(0, 0, Game.GWIDTH, Game.GHEIGHT);
			PauseO.render(g);
		} else if (gameOver)
			GameO.render(g);
		else if (lvlCompleted)
			LevelO.render(g);
	}

	public void resetAll() {
		gameOver = false;
		paused = false;
		lvlCompleted = false;
		playerDying = false;
		player.resetAll();
		EManger.resetAll();
	}

	public void setGameOver() {
		this.gameOver = true;
	}


	public void checkEnemyHit(Rectangle2D.Float attackBox) {
		EManger.checkEnemyHit(attackBox);
	}

	public void mouseClicked(MouseEvent e) {
		if (!gameOver)
			if (e.getButton() == MouseEvent.BUTTON1)
				player.setAttacking(true);
	}

	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_A:
			player.setLeft(true);
			break;
		case KeyEvent.VK_D:
			player.setRight(true);
			break;
		case KeyEvent.VK_W:
			player.setJump(true);
//			if(player.isInAir() && player.canJumpAgain()) {
//				player.setDoubleJump(true);
//				player.setJumpAgain();
//			}
			break;
		case KeyEvent.VK_ESCAPE:
			paused = !paused;
			break;
		}
	}


	public void keyReleased(KeyEvent e) {
		if (!gameOver)
			switch (e.getKeyCode()) {
			case KeyEvent.VK_A:
				player.setLeft(false);
				break;
			case KeyEvent.VK_D:
				player.setRight(false);
				break;
			case KeyEvent.VK_W:
				player.setJump(false);
				if(!player.canJumpAgain()) player.setDoubleJump(false);
				break;
			}

	}


	public void mousePressed(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				PauseO.mousePressed(e);
			else if (lvlCompleted)
				LevelO.mousePressed(e);
		} else
			GameO.mousePressed(e);

	}

	public void mouseReleased(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				PauseO.mouseReleased(e);
			else if (lvlCompleted)
				LevelO.mouseReleased(e);
		} else
			GameO.mouseReleased(e);
	}

	public void mouseMoved(MouseEvent e) {
		if (!gameOver) {
			if (paused)
				PauseO.mouseMoved(e);
			else if (lvlCompleted)
				LevelO.mouseMoved(e);
		} else
			GameO.mouseMoved(e);
	}

	public void setLevelCompleted() {
		this.lvlCompleted = true;
		game.getAudioPlayer().lvlCompleted();
	}

	public void setMaxLvlOffset(int lvlOffset) {
		this.maxLvlOffsetX = lvlOffset;
	}

	public void backInG() {
		paused = false;
	}

	public void windowFocusLost() {
		player.resetDirBooleans();
	}

	public Player getPlayer() {
		return player;
	}

	public EnemyManager getEnemyManager() {
		return EManger;
	}

	public LevelManager getLevelManager() {
		return LManager;
	}

	public void setPlayerDead(boolean playerDying) {
		this.playerDying = playerDying;

	}
	
	public boolean getLvlCompleted() {
		return lvlCompleted;
	}
	
	public static long getTime() {
		return lastCheck - timeCheck - totalTimeOut;
	}
}