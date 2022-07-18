package com.joehxtees;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import javax.imageio.ImageIO;

public class ImageProcessor {

	public static BufferedImage readFromFile(final String filename) throws IOException {
		return ImageIO.read(new File(filename));
	}

	public static BufferedImage readFromUrl(final String src) throws MalformedURLException, IOException {
		return ImageIO.read(new URL(src));
	}

	public static void write(final String path, final RenderedImage image) throws IOException {
		write(path, "image.png", image);
	}

	public static void write(final String path, final String filename, final RenderedImage image) throws IOException {
		ImageIO.write(image, "png", new File(path + File.separatorChar + filename));
	}

	public static BufferedImage resizeImage(final Image image) {
		final Image resized = image.getScaledInstance(700, -1, Image.SCALE_SMOOTH);

		final BufferedImage resizedBuffer = new BufferedImage(resized.getWidth(null), resized.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    final Graphics2D g2d = resizedBuffer.createGraphics();
	    g2d.drawImage(resized, 0, 0, null);
	    g2d.dispose();

	    return resizedBuffer;
	}

	public static BufferedImage eraseBackground(final Image image) throws IOException {
		final BufferedImage newImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    final Graphics g = newImage.getGraphics();
	    g.drawImage(image, 0, 0, null);
	    g.dispose();

	    final BufferedImage template = readFromFile("template.png");

	    for (int x = 0; x < newImage.getWidth(); x++) {
	    	for (int y = 0; y < newImage.getHeight(); y++) {

	    		final Color oldColor = new Color(newImage.getRGB(x, y));
	    		final Color templateColor = new Color(template.getRGB(x, y), true);
	    		final Color newColor = new Color(oldColor.getRed(), oldColor.getGreen(), oldColor.getBlue(), templateColor.getAlpha());

    			newImage.setRGB(x, y, newColor.getRGB());

	    	}
	    }

	    return newImage;

	}

	private static boolean isWhite(final int rgb) {
		return Color.WHITE.equals(new Color(rgb));
	}

	public static BufferedImage eraseBackground2(final Image image) {
		final BufferedImage newImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    final Graphics g = newImage.getGraphics();
	    g.drawImage(image, 0, 0, null);
	    g.dispose();

		final Stack<Point> stack = new Stack<>();
		stack.push(new Point(0,0));

		while (!stack.isEmpty()) {
			final Point point = stack.pop();

			if (point.x >= 0 && point.x < newImage.getWidth()
					&& point.y >= 0 && point.y < newImage.getHeight()) {

				final Color color = new Color(newImage.getRGB(point.x, point.y), true);

				final float brightness = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2];

				if (brightness > 0.75 && color.getAlpha() == 255) {
					final int alpha = (int) (256.0 * brightness);
					final Color newColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 256 - alpha);

					newImage.setRGB(point.x, point.y, newColor.getRGB());

					stack.push(new Point(point.x + 1, point.y    ));
					stack.push(new Point(point.x    , point.y + 1));
					stack.push(new Point(point.x - 1, point.y    ));
					stack.push(new Point(point.x    , point.y - 1));
				}
			}
		}

		return newImage;
	}
}
