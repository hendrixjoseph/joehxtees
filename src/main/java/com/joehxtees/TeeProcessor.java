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

	private static enum Tag {
		title, bullets, image, src, content, path, base_path, head, body;

		@Override
		public String toString() {
			return "{{ " + this.name().replace('_', ' ') + " }}";
		}
	}

	private static final String SITE_DIR = "_site";
	private static final String IMAGE_FILENAME = "image.png";
	private static final String BASE_PATH = "https://www.joehxtees.com/";
	//private static final String BASE_PATH = "https://www.joehxblog.com/joehxtees/";
	//private static final String BASE_PATH = "file:///C:/Users/hendr/git/joehxtees/_site/";

	private final List<Tee> tees;
	private final Map<Tee, String> teeHtmls;

	private final Template listTemplate;
	private final Template detailTemplate;

	private final String mainTemplate;
	private final String teeTemplate;

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
					.filter(path -> !path.toString().contains(".git"))
					.filter(path -> !path.equals(Path.of(SITE_DIR)))
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

	private static class Template {
		private final String head;
		private final String body;

		public static Template fromFiles(final String name) throws IOException {
			return new Template(Files.readString(Path.of(name + ".head.template.html")),
					            Files.readString(Path.of(name + ".body.template.html")));
		}

		public Template(final String head, final String body) {
			this.head = head;
			this.body = body;
		}

		public String getBodyWithContent(final String content) {
			return this.body.replace(Tag.content.toString(), content);
		}
	}

	public TeeProcessor() throws IOException {
		this.listTemplate = Template.fromFiles("list");
		this.detailTemplate = Template.fromFiles("detail");

		this.mainTemplate = Files.readString(Path.of("main.template.html"));
		this.teeTemplate = Files.readString(Path.of("shirt.template.html"));

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


		final String content = this.teeHtmls.entrySet().stream().map(entry -> {
			final Tee tee = entry.getKey();
			final String teeHtml = entry.getValue();

			 return teeHtml
				 .replace("##unless-list", "")
				 .replace(Tag.path.toString(), tee.getPath())
				 .replace(Tag.image.toString(), tee.getPath() + IMAGE_FILENAME);
		}).collect(Collectors.joining());

		final String body = this.listTemplate.getBodyWithContent(content);

		final String indexHtml = this.createPage(this.listTemplate.head, body, "List of T-Shirts");

		final Writer index = new FileWriter(SITE_DIR + "/index.html");

		index.write(indexHtml);

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
		final List<String> filesToCopy = List.of("CNAME","robots.txt");

		Files.list(Path.of(""))
			.filter(path -> path.toString().endsWith("css")
						 || path.toString().endsWith("js")
						 || filesToCopy.contains(path.toString()))
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
		final String head = templatize(this.detailTemplate.head, tee);
		final String body = this.detailTemplate.getBodyWithContent(teeHtml
													.replaceAll("##unless-list.+", "")
													.replace(Tag.image.toString(), IMAGE_FILENAME));

		final String detailHtml = createPage(head, body, tee.getTitle());

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
				.replace(Tag.title.toString(), tee.getTitle())
				.replace(Tag.base_path.toString(), BASE_PATH)
				.replace(Tag.src.toString(), tee.getSrcWithSlug())
				.replace(Tag.bullets.toString(), tee.getBullets().stream().collect(Collectors.joining("</li><li>", "<li>", "</li>")));

	}

	private String createPage(final String head, final String body, final String title) {
		return this.mainTemplate.replace(Tag.title.toString(), title)
								.replace(Tag.head.toString(), head)
								.replace(Tag.body.toString(), body);
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
