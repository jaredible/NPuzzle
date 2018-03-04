package game;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Main extends Canvas implements MouseListener, MouseMotionListener, KeyListener {
	private static final long serialVersionUID = 1L;

	public static boolean DEV;
	public static int N = 4;
	public static final int SCALE = 100;
	public static final String TITLE = "NPuzzle";

	private final int[] tiles = new int[N * N];

	private int sx, sy = 0;
	private int ex, ey = 0;

	private final Font font = new Font("monospace", Font.BOLD, SCALE / 2);
	private final Color color1 = new Color(120, 0, 0);
	private final Color color2 = new Color(210, 0, 0);
	private final Color fontColor = new Color(255, 255, 255);
	private final Color backgroundColor = new Color(0, 0, 0);

	private int globalRenderOffset;

	private long startTime;
	private double currentTime;
	private boolean started = false;

	private File fastestTimeFile = new File("fastestTime.txt");
	private double fastestTime = Double.MAX_VALUE;

	public Main() {
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}

	private void init() {
		globalRenderOffset = (getWidth() - N * SCALE) / 2;

		reset();

		loadLeaderboard();

		requestFocus();
	}

	private void exit() {
		System.exit(0);
	}

	private void reset() {
		setSolved();
		scramble();

		repaint();
	}

	private void setSolved() {
		int l = tiles.length;
		for (int i = 1; i < l; i++)
			tiles[i - 1] = i;
		tiles[l - 1] = 0; // sets empty space at bottom right
		ex = ey = N - 1; // sets empty coords at bottom right
	}

	private void scramble() {
		Random r = new Random();

		int i = 0;
		int n = (int) Math.pow(N, N + 1);
		while (i < n) {
			sx = r.nextInt(N);
			sy = r.nextInt(N);
			if (!onEmpty(sx, sy)) {
				shift(sx, sy, ex, ey);
				i++;
			}
		}
	}

	private boolean shift(int sx, int sy, int ex, int ey) {
		boolean isDirty = false;
		if (!onEmpty(sx, sy)) {
			int i = 0;
			if (sx == ex) {
				if (sy < ey) {
					i = ey - 1;
					while (i >= sy) {
						if (canSwap(sx, i)) isDirty = swap(sx, i);
						i--;
					}
				} else if (sy > ey) {
					i = ey + 1;
					while (i <= sy) {
						if (canSwap(sx, i)) isDirty = swap(sx, i);
						i++;
					}
				}
			}
			if (sy == ey) {
				if (sx < ex) {
					i = ex - 1;
					while (i >= sx) {
						if (canSwap(i, sy)) isDirty = swap(i, sy);
						i--;
					}
				} else if (sx > ex) {
					i = ex + 1;
					while (i <= sx) {
						if (canSwap(i, sy)) isDirty = swap(i, sy);
						i++;
					}
				}
			}
		}
		return isDirty;
	}

	private boolean swap(int x, int y) {
		int tile = tiles[x + y * N];
		tiles[x + y * N] = 0;
		tiles[ex + ey * N] = tile;
		ex = x;
		ey = y;
		return true;
	}

	private boolean canSwap(int x, int y) {
		if (x + (y - 1) * N == ex + ey * N) return true;
		if (x + (y + 1) * N == ex + ey * N) return true;
		if ((x - 1) + y * N == ex + ey * N) return true;
		if ((x + 1) + y * N == ex + ey * N) return true;
		return false;
	}

	private boolean onEmpty(int x, int y) {
		return x + y * N == ex + ey * N;
	}

	private boolean checkGameBounds(int x, int y) {
		return x >= 0 && y >= 0 && x < N && y < N;
	}

	private boolean hasWon() {
		int l = tiles.length;
		for (int i = 1; i < l; i++)
			if (tiles[i - 1] != i) return false;

		started = false;
		currentTime = Math.round((System.nanoTime() - startTime) / 1e9 * 100.0) / 100.0;
		return true;
	}

	private void update() {
		if (checkGameBounds(sx, sy)) {
			if (shift(sx, sy, ex, ey)) {
				repaint();
				if (!started) startTime = System.nanoTime();
				started = true;
			}
			if (hasWon()) {
				updateFastestScore();
				if (JOptionPane.showConfirmDialog(this, "You took " + getTimeString() + " seconds! \nWould you like to restart?", "SOLVED!", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
					reset();
				else exit();
			}
		}
	}

	private String getTimeString() {
		return "" + currentTime;
	}

	public void paint(Graphics g) {
		g.setColor(backgroundColor);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setFont(font);

		int x;
		int i;
		int offset = 10;
		for (int y = 0; y < N; y++)
			for (x = 0; x < N; x++) {
				i = x + y * N;
				if (tiles[i] != 0) {
					g.setColor((tiles[i] + 1) % 2 == 0 ? color1 : color2);
					g.fillRect(globalRenderOffset + x * SCALE + offset / 2, globalRenderOffset + y * SCALE + offset / 2, SCALE - offset, SCALE - offset);
					g.setColor(fontColor);
					g.drawString("" + tiles[i], globalRenderOffset + (x * SCALE + SCALE / N), globalRenderOffset + (y * SCALE + SCALE - SCALE / 3));
				}
			}
	}

	private void loadLeaderboard() {
		try {
			if (!fastestTimeFile.exists()) return;

			BufferedReader br = new BufferedReader(new FileReader(fastestTimeFile));
			String line = "";

			while ((line = br.readLine()) != null) {
				line = line.trim();
				fastestTime = Integer.parseInt(line) / 100.0;
			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void saveLeaderboard() {
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(fastestTimeFile));
			pw.println((int) (fastestTime * 100));
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateFastestScore() {
		if (currentTime < fastestTime) fastestTime = currentTime;
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			DEV = args[0].equals("DEV");
			if (args.length == 2 && args[1] != null) N = Integer.valueOf(args[1]);
		}

		Main game = new Main();
		Dimension dim = new Dimension(N * SCALE, N * SCALE);
		game.setMinimumSize(dim);
		game.setMaximumSize(dim);
		game.setPreferredSize(dim);

		JFrame frame = new JFrame(TITLE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(game, BorderLayout.CENTER);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		frame.setAlwaysOnTop(DEV);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				game.saveLeaderboard();
				System.out.println("fastestTime file updated");
			}
		});

		game.init();
	}

	public void mouseDragged(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
		sx = (globalRenderOffset + e.getX()) / SCALE;
		sy = (globalRenderOffset + e.getY()) / SCALE;
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		update();
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) reset();
		else update();
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}
}