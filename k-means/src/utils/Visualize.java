package utils;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import clustering.ICPoint;
import clustering.IPoint;

public class Visualize extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2028147485776361687L;
	private Container c;
	private Dimension dim = new Dimension(800, 800);
	private PointsPanel panel;

	public Visualize() {
		this.c = this.getContentPane();
		this.setSize(this.dim);
		this.setPreferredSize(this.dim);
		this.setVisible(true);
		this.setName("Visualization");

		this.setBackground(Color.WHITE);
		this.setForeground(Color.BLACK);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public void drawCPoints(int maxXY, List<ICPoint> cPoints) {
		this.c.removeAll();
		this.panel = this.panel == null ? new PointsPanel(maxXY) : this.panel;
		panel.setCPoints(cPoints);
		this.c.add(panel);
		this.pack();
		this.repaint();
	}

	public void drawPoints(int maxXY, List<IPoint> points) {
		this.c.removeAll();
		this.panel = this.panel == null ? new PointsPanel(maxXY) : this.panel;
		panel.setPoints(points);
		this.c.add(panel);
		this.pack();
		this.repaint();
	}

	private static class PointsPanel extends JPanel {
		private static final long serialVersionUID = -731110979827788751L;
		private List<ICPoint> cPoints;
		private List<IPoint> points;
		private final int X_SIZE = 700;
		private final int X_OFFSET = 5;
		private final int Y_SIZE = 700;
		private final int Y_OFFSET = 5;
		private int maxXY;
		private HashMap<IPoint, Color> colors = new HashMap<IPoint, Color>();
		private int mode = 0;
		private final int CPOINT = 1, POINT = 2;

		public PointsPanel(int maxXY) {
			this.maxXY = maxXY;
		}

		public void setCPoints(List<ICPoint> cPoints) {
			this.cPoints = cPoints;
			this.mode = CPOINT;
		}

		public void setPoints(List<IPoint> points) {
			this.points = points;
			this.mode = POINT;
		}

		private void drawCPoints(Graphics2D g2) {
			for (ICPoint p : this.cPoints) {
				drawPoint(g2, p);
				drawCentroid(g2, p.getCentroid());
			}
		}

		private void drawPoints(Graphics2D g2) {
			for (IPoint p : this.points) {
				drawPoint(g2, p);
			}
		}

		private void drawCentroid(Graphics2D g2, IPoint c) {
			if (c == null)
				return;
			int x;
			int y;
			x = this.getX(c.get(0));
			y = this.getY(c.get(1));
			this.setAssignedColor(c, g2);
			g2.fillOval(x, y, 15, 15);
		}

		private void drawPoint(Graphics2D g2, IPoint p) {
			int x;
			int y;
			x = this.getX(p.get(0));
			y = this.getY(p.get(1));
			this.setAssignedColor(p, g2);
			g2.fillOval(x, y, 5, 5);
		}

		private void drawPoint(Graphics2D g2, ICPoint p) {
			int x;
			int y;
			x = this.getX(p.get(0));
			y = this.getY(p.get(1));
			this.setAssignedColor(p.getCentroid(), g2);
			g2.fillOval(x, y, 5, 5);
		}

		public void paint(Graphics g) {
			this.removeAll();
			Graphics2D g2 = (Graphics2D) g;
			this.drawAxis(g2);

			switch (this.mode) {
			case CPOINT:
				this.drawCPoints(g2);
				break;
			case POINT:
				this.drawPoints(g2);
				break;
			}
		}

		private void drawAxis(Graphics2D g2) {
			g2.setColor(Color.BLACK);
			g2.drawLine(0 + X_OFFSET, 0 + Y_OFFSET, X_SIZE + X_OFFSET,
					0 + Y_OFFSET);
			g2.drawLine(0 + X_OFFSET, 0 + Y_OFFSET, 0 + X_OFFSET, Y_SIZE
					+ Y_OFFSET);
		}

		private int getX(double x) {
			return (int) ((x / maxXY) * X_SIZE + X_OFFSET);
		}

		private int getY(double y) {
			return (int) ((y / maxXY) * Y_SIZE + Y_OFFSET);
		}
		
		private void setAssignedColor(IPoint centroid, Graphics2D g2) {
			if (centroid == null)
				return;

			Color c = colors.get(centroid);
			if (c == null) {
				Random r = new Random();
				c = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
				colors.put(centroid, c);
			}

			g2.setColor(c);
		}

	}
}
