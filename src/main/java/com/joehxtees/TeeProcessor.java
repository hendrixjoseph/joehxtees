package com.joehxtees;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

public class TeeProcessor {
	private static final String TITLE = "{{ title }}";
	private static final String BULLETS = "{{ bullets }}";
	private static final String IMAGE = "{{ image }}";
	private static final String SRC = "{{ src }}";
	private static final String CONTENT = "{{ content }}";
	private static final String PATH = "{{ path }}";
	private static final String BASE_PATH_TAG = "{{ base path }}";

	private static final String SITE_DIR = "_site";
	private static final String IMAGE_FILENAME = "image.jpg";
	//private static final String BASE_PATH = "https://www.joehxtees.com/";
	private static final String BASE_PATH = "https://www.joehxblog.com/joehxtees/";
	//private static final String BASE_PATH = "file:///C:/Users/hendr/git/joehxtees/_site/";

	private final List<Tee> tees;
	private final Map<Tee, String> teeHtmls;

	private final String listTemplate;
	private final String teeTemplate;
	private final String detailTemplate;

	@FunctionalInterface
	private static interface ExConsumer<T, E extends IOException> {
	    void accept(T t) throws IOException;
	}

	private static <T, E extends IOException> Function<T, IOException> throwMapper(final ExConsumer<T, E> c) {
	    return t -> {
	        try {
	            c.accept(t);
	            return null;
	        }
	        catch (final IOException e) {
	            return e;
	        }
	    };
	}

	public static void delete() throws IOException {
		if (Files.exists(Path.of(SITE_DIR))) {
			throwFirstIOException(
				Files.walk(Path.of(SITE_DIR))
					.sorted(Comparator.reverseOrder())
					.map(throwMapper(Files::delete))
			);
		}
	}

	private static void throwFirstIOException(final Stream<IOException> stream) throws IOException {
		final Optional<IOException> e =
			stream
				.filter(Objects::nonNull)
				.findFirst();

		if(!e.isEmpty()) {
			throw e.get();
		}
	}

	public TeeProcessor() throws IOException {
		this.listTemplate = Files.readString(Path.of("list.template.html"));
		this.teeTemplate = Files.readString(Path.of("shirt.template.html"));
		this.detailTemplate = Files.readString(Path.of("detail.template.html"));

		Files.createDirectories(Paths.get(SITE_DIR));

		final Gson gson = new Gson();

		final Reader reader = new FileReader("tees.json");

		final List<List<?>> objects = gson.fromJson(reader, List.class);

		this.tees = objects.stream().map(o -> {
			final String title = o.get(0).toString();
			final List<String> bullets = (List<String>) o.get(1);
			final String imageSrc = o.get(2).toString();
			final String src = o.get(3).toString();

			return new Tee(title, imageSrc, src, bullets);
		  })
		  .collect(Collectors.toList());

		this.teeHtmls = this.tees.stream().collect(
			Collectors.toMap(
				tee -> tee,
				tee -> templatize(this.teeTemplate, tee)
			)
		);

		reader.close();
	}

	public void downloadImages() throws IOException {
		throwFirstIOException(
			this.tees.parallelStream()
				.map(throwMapper(this::downloadImage))
		);
	}

	public void createDetailPages() throws IOException {
		throwFirstIOException(
			this.teeHtmls.entrySet()
				.stream()
				.map(throwMapper(this::createDetailPage))
		);
	}

	public void createIndex() throws IOException {
		final Writer index = new FileWriter(SITE_DIR + "/index.html");

		index.write(
		  this.listTemplate.replace(CONTENT,
			this.teeHtmls.entrySet().stream().map(entry -> {
				final Tee tee = entry.getKey();
				final String teeHtml = entry.getValue();

				 return teeHtml
					 .replace("##unless-list", "")
					 .replace(PATH, tee.getPath())
					 .replace(IMAGE, tee.getPath() + IMAGE_FILENAME);
			}).collect(Collectors.joining())
		  )
	    );

		index.close();
	}

	public void createSiteMap() throws IOException {
		final Writer sitemap = new FileWriter(SITE_DIR + "/sitemap.xml");

		sitemap.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sitemap.write("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

		final String lastModTag = "<lastmod>"
				+ LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
				+ "</lastmod>";

		sitemap.write(
			this.tees.stream()
				.map(Tee::getPath)
				.map(path -> BASE_PATH + path)
				.map(url -> "<loc>" + url + "</loc>")
				.map(locTag -> locTag + lastModTag)
				.map(tags -> "<url>" + tags + "</url>")
				.collect(Collectors.joining())
		);

		sitemap.write("</urlset>");

		sitemap.close();
	}

	public void copyStaticFiles() throws IOException {
		Files.list(Path.of(""))
			.filter(path -> path.toString().endsWith("css") || path.toString().endsWith("js"))
			.forEach(file -> {
				try {
					Files.copy(file, Path.of(SITE_DIR, file.toString()), StandardCopyOption.REPLACE_EXISTING);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			});
	}

	private void createDetailPage(final Entry<Tee, String> entry) throws IOException {
		createDetailPage(entry.getKey(), entry.getValue());
	}

	private void createDetailPage(final Tee tee,  final String teeHtml) throws IOException {
		final String detailHtml = templatize(this.detailTemplate, tee)
				.replace(CONTENT, teeHtml.replaceAll("##unless-list.+", "")
				.replace(IMAGE, IMAGE_FILENAME));

		final Path dir = createTeeDirectory(tee);

		final Writer detail = new FileWriter(dir.toString() + "/index.html");

		detail.write(detailHtml);

		detail.close();
	}

	private Path createTeeDirectory(final Tee tee) throws IOException {
		final Path dir = Paths.get(SITE_DIR, tee.getPath());

		Files.createDirectories(dir);

		return dir;
	}

	private String templatize(final String template, final Tee tee) {
		return template
				.replace(TITLE, tee.getTitle())
				.replace(BASE_PATH_TAG, BASE_PATH)
				.replace(SRC, tee.getSrcWithSlug())
				.replace(BULLETS, tee.getBullets().stream().collect(Collectors.joining("</li><li>", "<li>", "</li>")));
	}

	private void downloadImage(final Tee tee) throws MalformedURLException, IOException {
		final Path dir = createTeeDirectory(tee);

		final BufferedImage image = ImageIO.read(new URL(tee.getImageSrc()));

		final BufferedImage newImage = eraseBackground(image);
		final BufferedImage resizedImage = resizeImage(newImage);

		ImageIO.write(resizedImage, "png", new File(dir.toString() + "/image.png"));

		System.out.println(dir.toString() + " written.");
	}

	private BufferedImage resizeImage(final BufferedImage image) {
		final Image resized = image.getScaledInstance(700, -1, Image.SCALE_SMOOTH);

		final BufferedImage resizedBuffer = new BufferedImage(resized.getWidth(null), resized.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    final Graphics2D g2d = resizedBuffer.createGraphics();
	    g2d.drawImage(resized, 0, 0, null);
	    g2d.dispose();

	    return resizedBuffer;
	}

	private BufferedImage eraseBackground(final BufferedImage image) {
		final BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

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
