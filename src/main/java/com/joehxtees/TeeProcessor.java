package com.joehxtees;

import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;

public class TeeProcessor {

	private static enum Tag {
		title, bullets, image, src, content, path, base_path, head, body, similar_tees;

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

		final String indexHtml = this.createPage(this.listTemplate.head, body, "List of T-Shirts", "");

		final Writer index = new FileWriter(SITE_DIR + "/index.html");

		index.write(minify(indexHtml));

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
		Files.list(Path.of("static"))
			.forEach(file -> {
				try {
					Files.copy(file, Path.of(SITE_DIR, file.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			});
	}

	private String minify(final String string) {
		return Arrays.stream(string.split("\n"))
					 .filter(s -> !s.isBlank())
					 .map(String::trim)
					 .collect(Collectors.joining());
	}

	private void createDetailPage(final Entry<Tee, String> entry) throws IOException {
		createDetailPage(entry.getKey(), entry.getValue());
	}

	private void createDetailPage(final Tee tee,  final String teeHtml) throws IOException {
		final String head = templatize(this.detailTemplate.head, tee);
		final String body = this.detailTemplate.getBodyWithContent(teeHtml
													.replaceAll("##unless-list.+", "")
													.replace(Tag.image.toString(), IMAGE_FILENAME));

		final String similarTeesHtml =
				"<h3>Similar Shirts</h3><div class='similar-shirts'>"
				+ tee.getSimilarTees(2).stream()
				.map(t -> ("<div><a href='/"
						+ t.getPath()
						+ "'><h4>"
						+ t.getTitle()
						+ "</h4><img src='/"
						+ t.getPath()
						+ "image.png' width='700' height='711' /></a></div>"))
				.collect(Collectors.joining())
				+ "</div>";

		final String detailHtml = createPage(head,
				body.replace(Tag.similar_tees.toString(), similarTeesHtml),
				tee.getTitle(),
				tee.getPath());

		final Path dir = createTeeDirectory(tee);

		final Writer detail = new FileWriter(dir.toString() + "/index.html");

		detail.write(minify(detailHtml));

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
				.replace(Tag.base_path.toString(), BASE_PATH) // TODO: Remove this line
				.replace(Tag.path.toString(), tee.getPath())
				.replace(Tag.src.toString(), tee.getSrcWithSlug())
				.replace(Tag.bullets.toString(), tee.getBullets().stream().collect(Collectors.joining("</li><li>", "<li>", "</li>")));

	}

	private String createPage(final String head, final String body, final String title, final String path) {
		return this.mainTemplate.replace(Tag.title.toString(), title)
								.replace(Tag.path.toString(), path)
								.replace(Tag.head.toString(), head)
								.replace(Tag.body.toString(), body);
	}

	private void downloadImage(final Tee tee) throws MalformedURLException, IOException {
		final Path dir = createTeeDirectory(tee);

		final BufferedImage image = ImageProcessor.readFromUrl(tee.getImageSrc());

		final BufferedImage newImage = ImageProcessor.eraseBackground(image);
		final BufferedImage resizedImage = ImageProcessor.resizeImage(newImage);

		ImageProcessor.write(dir.toString(),  resizedImage);

		System.out.println(dir.toString() + " written.");
	}
}
