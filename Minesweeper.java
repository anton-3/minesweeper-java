import acm.program.*;
import acm.util.RandomGenerator;
import acm.graphics.*;

import java.awt.Color;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Minesweeper extends GraphicsProgram {
	private int rowCount;
	private int columnCount;
	private GRect[][] board;
	private boolean[][] mines;
	private boolean[][] uncoveredSquares;
	private ArrayList<GLabel> mineLabels = new ArrayList<GLabel>();
	private ArrayList<GImage> flags = new ArrayList<GImage>();
	private GLabel flagLabel;
	private GLabel timeLabel;
	private boolean activateTimer = false;
	private boolean minesCreated = false;
	private boolean gameOver = false;
	private boolean debugMode = false;
	private boolean onTitleScreen;
	private ArrayList<GLabel> difficultyLabels = new ArrayList<GLabel>();
	private static final Color UNCOVERED_COLOR = Color.WHITE;
	private static final Color COVERED_COLOR = new Color(215, 215, 215);
	private int numMines;
	private int squareSize;
	private static final RandomGenerator RGEN = RandomGenerator.getInstance();
	private static final int TOP_BAR_HEIGHT = 50;
	private static final int LENGTH = 576; // length of a side of the application
	public static final int APPLICATION_WIDTH = LENGTH;
	public static final int APPLICATION_HEIGHT = LENGTH + TOP_BAR_HEIGHT + 22;
	public static final int APPLICATION_Y = 0;

	// colors for the mine labels by number
	private static final Color[] NUMBER_COLORS = new Color[] { Color.BLUE, new Color(0, 128, 0), // greenish
			Color.RED, new Color(0, 0, 128), // bluish purple
			new Color(128, 0, 0), // brownish
			Color.BLACK, Color.GRAY };

	public void run() {
		addMouseListeners();
		while (true) {
			playGame();
			pause(500);
			waitForClick();
			resetGame();
		}
	}

	private void playGame() {
		createTitleScreen();
		runTimer();
	}

	private void resetGame() {
		minesCreated = false;
		gameOver = false;
		mineLabels.clear();
		flags.clear();
		difficultyLabels.clear();
		removeAll();
	}

	private void createTitleScreen() {
		onTitleScreen = true;
		GLabel easy = new GLabel("EASY");
		GLabel normal = new GLabel("NORMAL");
		GLabel hard = new GLabel("HARD");
		difficultyLabels.add(easy);
		difficultyLabels.add(normal);
		difficultyLabels.add(hard);
		easy.setColor(Color.GREEN);
		normal.setColor(Color.ORANGE);
		hard.setColor(Color.RED);
		for (int i = 0; i < difficultyLabels.size(); i++) {
			GLabel label = difficultyLabels.get(i);
			label.setFont("serif-60");
			double x = getWidth() / 2 - label.getWidth() / 2;
			double y = getHeight() / 2 + i * (getHeight() / 6);
			label.setLocation(x, y);
			add(label);
		}
		GLabel title = new GLabel("MINESWEEPER");
		title.setFont("serif-60");
		title.setLocation(getWidth() / 2 - title.getWidth() / 2, getHeight() / 4);
		add(title);
	}

	private void setDifficulty(int difficulty) {
		// 0 is easy, 1 is normal, 2 is hard
		// rowCount * squareSize and columnCount * squareSize has to == 576
		switch (difficulty) {
		case 0:
			rowCount = columnCount = 8;
			squareSize = 72;
			numMines = 10;
			break;
		case 1:
			rowCount = columnCount = 16;
			squareSize = 36;
			numMines = 40;
			break;
		case 2:
			rowCount = columnCount = 24;
			squareSize = 24;
			numMines = 90;
			break;
		}
	}

	private void addInterface() {
		board = new GRect[rowCount][columnCount];
		mines = new boolean[rowCount][columnCount];
		uncoveredSquares = new boolean[rowCount][columnCount];
		drawTopBar();
		for (int i = 0; i < columnCount; i++) {
			for (int j = 0; j < rowCount; j++) {
				GRect square = new GRect(i * squareSize, j * squareSize + TOP_BAR_HEIGHT, squareSize, squareSize);
				square.setFilled(true);
				square.setFillColor(COVERED_COLOR);
				board[j][i] = square;
				add(square);
			}
		}
	}

	private void drawTopBar() {
		GImage flag = new GImage("flag_icon.png", LENGTH / 4, TOP_BAR_HEIGHT / 8);
		flag.setSize(TOP_BAR_HEIGHT * 3 / 4, TOP_BAR_HEIGHT * 3 / 4);
		add(flag);
		flagLabel = new GLabel("" + numMines, LENGTH / 4 + 50, TOP_BAR_HEIGHT * 3 / 4);
		flagLabel.setFont("serif-35");
		add(flagLabel);
		timeLabel = new GLabel("000", LENGTH * 3 / 5, TOP_BAR_HEIGHT * 3 / 4);
		timeLabel.setFont("serif-35");
		add(timeLabel);
	}

	private void runTimer() {
		int time = 0;
		while (!activateTimer) {
			// wait until timer is activated (when player clicks for the first time)
			pause(100);
		}
		while (activateTimer) {
			time += 1;
			if (time >= 1000)
				return;
			String startStr = "";
			if (time < 10) {
				startStr = "00";
			} else if (time < 100) {
				startStr = "0";
			}
			timeLabel.setLabel(startStr + time);
			pause(1000);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {

		GObject element = getElementAt(e.getX(), e.getY());
		// if there isn't a square where the click was
		// or the game is over
		if (element == null || gameOver)
			return;

		/// handle setting the difficulty on the title screen
		if (onTitleScreen) {
			if (difficultyLabels.contains(element)) {
				removeAll();
				onTitleScreen = false;
				setDifficulty(difficultyLabels.indexOf(element));
				addInterface();
			}
			return;
		}

		if (e.getButton() == 1 && !e.isAltDown()) {
			// if left click
			handleLeftClick(element);
		} else if (e.getButton() == 3 || (e.getButton() == 1 && e.isAltDown())) {
			// else if right click
			handleRightClick(element);
		}
	}

	private void handleLeftClick(GObject element) {
		// if the element clicked was a flag
		if (flags.contains(element) || mineLabels.contains(element))
			return;
		int[] coords = getSquareCoords((GRect) element);
		// if this is the first click
		// makes sure the first clicked square isn't a mine
		if (!minesCreated) {
			createMineLocations(coords);
			activateTimer = true;
			if (debugMode)
				println("first click coords: " + coords[0] + ", " + coords[1]);
		}

		handleSquareClick(coords[0], coords[1]);
	}

	private void handleRightClick(GObject element) {
		if (flags.contains(element)) {
			// if the square has a flag on it already then remove it
			remove(element);
			flags.remove(element);
		} else if (mineLabels.contains(element)) {
			// handle clicking on mine labels
			element.sendBackward();
			handleRightClick(getElementAt(element.getX(), element.getY()));
			element.sendForward();
			return; // already did updateFlagLabel
		} else {
			// else add a flag
			GImage flag = new GImage("flag_icon.png", element.getX(), element.getY());
			flag.setSize(squareSize, squareSize);
			add(flag);
			flags.add(flag);
		}
		updateFlagLabel();
	}

	private void updateFlagLabel() {
		int flagsLeft = numMines - flags.size();
		// add a space if flagsLeft is one digit and positive
		String str = flagsLeft < 10 && flagsLeft >= 0 ? " " : "";
		flagLabel.setLabel(str + flagsLeft);
	}

	private void handleSquareClick(int row, int column) {
		if (mines[row][column]) {
			endGame("loss");
			return;
		}
		uncover(row, column);
		removeMineLabels();
		handleSquares(getNeighborCoords(row, column));
		addMineLabels();
		if (hasWon())
			endGame("win");
	}

	private int[] getSquareCoords(GRect square) {
		int[] coords = new int[2];
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < columnCount; j++) {
				if (board[i][j] == square) {
					coords[0] = i;
					coords[1] = j;
				}
			}
		}
		return coords;
	}

	private ArrayList<int[]> getNeighborCoords(int row, int col) {
		// get coordinates for all 8 neighbors to a square
		int[][] coords = new int[8][2];
		for (int i = 0; i < 3; i++) {
			coords[i][0] = row - 1;
			coords[i + 5][0] = row + 1;
		}
		coords[3][0] = coords[4][0] = row;
		coords[0][1] = coords[3][1] = coords[5][1] = col - 1;
		coords[1][1] = coords[6][1] = col;
		coords[2][1] = coords[4][1] = coords[7][1] = col + 1;
		return filterInvalidCoords(coords);
	}

	private ArrayList<int[]> filterInvalidCoords(int[][] oldCoords) {
		boolean[] valids = new boolean[8];
		Arrays.fill(valids, true);
		int rowAboveIdx = oldCoords[0][0];
		int rowBelowIdx = oldCoords[5][0];
		int colLeftIdx = oldCoords[0][1];
		int colRightIdx = oldCoords[2][1];
		// declare invalid any coords outside the bounds of the board
		if (rowAboveIdx < 0)
			valids[0] = valids[1] = valids[2] = false;
		if (rowBelowIdx >= rowCount)
			valids[5] = valids[6] = valids[7] = false;
		if (colLeftIdx < 0)
			valids[0] = valids[3] = valids[5] = false;
		if (colRightIdx >= columnCount)
			valids[2] = valids[4] = valids[7] = false;
		// declare invalid any coords that have already been uncovered
		for (int i = 0; i < 8; i++) {
			if (!valids[i])
				continue;
			int row = oldCoords[i][0];
			int column = oldCoords[i][1];
			if (uncoveredSquares[row][column])
				valids[i] = false;
		}

		ArrayList<int[]> coords = new ArrayList<int[]>();
		for (int i = 0; i < valids.length; i++) {
			if (valids[i])
				coords.add(oldCoords[i]);
		}
		return coords;
	}

	private void handleSquares(ArrayList<int[]> neighbors) {
		int numMines = getNumMines(neighbors);
		if (numMines > 0) {
			return;
		}
		for (int i = 0; i < neighbors.size(); i++) {
			int row = neighbors.get(i)[0];
			int column = neighbors.get(i)[1];
			uncover(row, column);
			handleSquares(getNeighborCoords(row, column));
		}
	}

	private void removeMineLabels() {
		while (mineLabels.size() > 0) {
			remove(mineLabels.get(0));
			mineLabels.remove(0);
		}
	}

	private void addMineLabels() {
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < columnCount; j++) {
				// if the square hasn't been uncovered, don't label it
				if (!uncoveredSquares[i][j])
					continue;
				int numMines = getNumMines(getNeighborCoords(i, j));
				if (numMines > 0) {
					addMineLabel(board[i][j], numMines);
				}
			}
		}
	}

	private void addMineLabel(GRect square, int num) {
		GLabel label = new GLabel(String.valueOf(num));
		label.setFont("monospace-" + squareSize * 3 / 4);
		double x = square.getX() + square.getWidth() / 2 - label.getWidth() / 2;
		double y = square.getY() + square.getHeight() * 3 / 4;
		label.setLocation(x, y);
		label.setColor(NUMBER_COLORS[num - 1]);
		add(label);
		mineLabels.add(label);
	}

	private void createMineLocations(int[] firstClickCoords) {
		mines = new boolean[rowCount][columnCount];
		int totalMines = 0;
		while (totalMines < numMines) {
			int row = RGEN.nextInt(0, rowCount - 1);
			int col = RGEN.nextInt(0, columnCount - 1);
			// if randomly chosen square doesn't already have a mine in it
			// and wasn't the first square clicked
			if (!mines[row][col] && firstClickCoords[0] != row && firstClickCoords[1] != col) {
				mines[row][col] = true;
				totalMines++;
			}
		}
		minesCreated = true;
		printMines(); // print mine locations to console for debugging
	}

	private int getNumMines(ArrayList<int[]> squares) {
		int numMines = 0;
		for (int i = 0; i < squares.size(); i++) {
			int row = squares.get(i)[0];
			int column = squares.get(i)[1];
			if (mines[row][column])
				numMines++;
		}
		return numMines;
	}

	private void uncover(int row, int column) {
		board[row][column].setFillColor(UNCOVERED_COLOR);
		uncoveredSquares[row][column] = true;
	}

	private boolean hasWon() {
		int uncoveredSquareCount = 0;
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < columnCount; j++) {
				if (!uncoveredSquares[i][j])
					uncoveredSquareCount++;
			}
		}
		return uncoveredSquareCount <= numMines;
	}

	private void endGame(String result) {
		gameOver = true;
		activateTimer = false;
		switch (result) {
		case ("win"):
			endGameMsg("You Win");
			break;
		case ("loss"):
			revealMines();
			endGameMsg("You Lose");
			break;
		}
	}

	private void endGameMsg(String msg) {
		GLabel winLabel = new GLabel(msg);
		winLabel.setFont("serif-50");
		winLabel.setLocation(getWidth() / 2 - winLabel.getWidth() / 2, getHeight() / 4);
		GRect rect = new GRect(winLabel.getX() - 15, winLabel.getY() - winLabel.getHeight(), winLabel.getWidth() + 30,
				winLabel.getHeight() + 30);
		rect.setFilled(true);
		rect.setFillColor(Color.WHITE);
		add(rect);
		add(winLabel);
	}

	private void revealMines() {
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < columnCount; j++) {
				if (mines[i][j]) {
					GRect square = board[i][j];
					square.setFillColor(Color.WHITE);
					square.sendToFront();
					GImage mine = new GImage("mine.png", square.getX(), square.getY());
					mine.setSize(squareSize, squareSize);
					add(mine);
				}
			}
		}
	}

	private void printMines() {
		if (!debugMode)
			return;
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < columnCount; j++) {
				print(mines[j][i] ? "X " : "_ ");
			}
			println();
		}
	}
}
