package com.joehxtees;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;

public class Main {

	private static final String TITLE = "{{ title }}";
	private static final String BULLETS = "{{ bullets }}";
	private static final String IMAGE = "{{ image }}";
	private static final String SRC = "{{ src }}";
	private static final String CONTENT = "{{ content }}";
	private static final String PATH = "{{ path }}";

	private static final String SITE_DIR = "_site";
	private static final String IMAGE_FILENAME = "image.jpg";

	public static void oldmain(final String[] args) throws IOException {
		final String string = Files.readString(Path.of("shirt.template.html"));
		final String newString = string.replaceAll("##unless-list.+", "");
		System.out.println(string);
		System.out.println(newString);
	}

	public static void main(final String[] args) throws IOException {
//		try(InputStream in = new URL("https://m.media-amazon.com/images/I/A13usaonutL._CLa%7C2140%2C2000%7C81vVq7CoDRL.png%7C0%2C0%2C2140%2C2000%2B0.0%2C0.0%2C2140.0%2C2000.0_AC_UL1500_.png").openStream()){
//		    Files.copy(in, Paths.get("image.jpg"));
//		}

		final Gson gson = new Gson();

		final Reader reader = new FileReader("tees.json");

		final List<List<?>> objects = gson.fromJson(reader, List.class);

		final List<Tee> tees = objects.stream().map(o -> {
			final String title = o.get(0).toString();
			final List<String> bullets = (List<String>) o.get(1);
			final String imageSrc = o.get(2).toString();
			final String src = o.get(3).toString();

			return new Tee(title, imageSrc, src, bullets);
		  })
		  .collect(Collectors.toList());

		final String listTemplate = Files.readString(Path.of("list.template.html"));
		final String teeTemplate = Files.readString(Path.of("shirt.template.html"));
		final String detailTemplate = Files.readString(Path.of("detail.template.html"));

		final Map<Tee, String> teeHtmls = tees.stream().collect(
			Collectors.toMap(
				tee -> tee,
				tee -> templatize(teeTemplate, tee)
			)
		);

		Files.createDirectories(Paths.get(SITE_DIR));

		final Writer index = new FileWriter(SITE_DIR + "/index.html");

		index.write(
		  listTemplate.replace(CONTENT,
			teeHtmls.entrySet().stream().map(entry -> {
				final Tee tee = entry.getKey();
				final String teeHtml = entry.getValue();

				return processTee(tee, teeHtml, detailTemplate);
			}).collect(Collectors.joining())
		  )
	    );

		reader.close();
		index.close();

		Files.list(Path.of(""))
			.filter(path -> path.toString().endsWith("css"))
			.forEach(cssFile -> {
				try {
					Files.copy(cssFile, Path.of(SITE_DIR, cssFile.toString()));
				} catch (final IOException e) {
					e.printStackTrace();
				}
			});

		System.out.println("Done!");
	}

	private static String processTee(final Tee tee, final String teeHtml, final String detailTemplate) {
		final String detailHtml = templatize(detailTemplate, tee)
				.replace(CONTENT, teeHtml.replaceAll("##unless-list.+", "").replace(IMAGE, IMAGE_FILENAME));

		Path dir = Paths.get(SITE_DIR, tee.slug);

		while(Files.exists(dir)) {
			tee.slugId++;
			dir = Paths.get(SITE_DIR, tee.slug, Integer.toString(tee.slugId));
		}

		try {
			Files.createDirectories(dir);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		try (Writer detail = new FileWriter(dir.toString() + "/index.html")) {
			detail.write(detailHtml);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final String slugId = tee.slugId == 0 ? "" : tee.slugId + "/";
		final String path = tee.slug + "/" + slugId;

		return teeHtml
				.replace("##unless-list", "")
				.replace(PATH, path)
				.replace(IMAGE, path + IMAGE_FILENAME);
	}


	private static String templatize(final String template, final Tee tee) {
		return template
				.replace(TITLE, tee.title)
				//.replace(IMAGE, tee.slug + ".jpg")
				.replace(SRC, tee.src)
				.replace(BULLETS, tee.bullets.stream().collect(Collectors.joining("</li><li>", "<li>", "</li>")));
	}

	public static class Tee {
		private int slugId = 0;

		private final String title;
		private final String slug;
		private final String imageSrc;
		private final String src;
		private final List<String> bullets = new ArrayList<>();

		public Tee(final String title, final String imageSrc, final String src, final List<String> bullets) {
			this.title = title;
			this.slug = title.toLowerCase().replace(' ' , '-').replaceAll("&.+?;|\\?","").replaceAll("-+", "-");
			this.imageSrc = imageSrc;
			this.src = src;
			this.bullets.addAll(bullets);
		}

		@Override
		public String toString() {
			return new Gson().toJson(this);
		}
	}
}
