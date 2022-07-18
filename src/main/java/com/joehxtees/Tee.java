package com.joehxtees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.Gson;

public class Tee {
	private static final Map<String, Integer> SAME_TEE_COUNT = new HashMap<>();
	private static final Map<String, List<Tee>> SIMILAR_TEE_MAP = new HashMap<>();

	private final int slugId;

	private final String title;
	private final String slug;
	private final String imageSrc;
	private final String src;
	private final List<String> bullets = new ArrayList<>();

	public Tee(final String title, final String imageSrc, final String src, final List<String> bullets) {
		this.title = title;
		this.slug = title.toLowerCase().replace(' ' , '-').replaceAll("&.+?;|[^A-Za-z0-9-]","").replaceAll("-+", "-");
		this.imageSrc = imageSrc;
		this.src = src;
		this.bullets.addAll(bullets);

		this.slugId = SAME_TEE_COUNT.compute(this.slug, (k,v) -> v == null ? 0 : v + 1);

		Arrays.stream(this.slug.split("-"))
			  .filter(s -> !List.of("the").contains(s))
			  .forEach(s -> SIMILAR_TEE_MAP.computeIfAbsent(s, k -> new ArrayList<>()).add(this));
	}

	public String getTitle() {
		return this.title;
	}

	public String getImageSrc() {
		return this.imageSrc;
	}

	public String getSrc() {
		return this.src;
	}

	public String getSrcWithSlug() {
		final int position = this.src.indexOf("/dp");
		return this.src.substring(0, position) + "/" + this.slug + this.src.substring(position);
	}

	public List<String> getBullets() {
		return this.bullets;
	}

	public String getPath() {
		final String slugId = this.slugId == 0 ? "" : this.slugId + "/";
		return this.slug + "/" + slugId;
	}

	public List<Tee> getSimilarTees(final long maxsize) {
		return Arrays.stream(this.slug.split("-"))
			  .map(s -> SIMILAR_TEE_MAP.get(s))
			  .filter(Objects::nonNull)
			  .flatMap(List::stream)
			  .distinct()
			  .filter(tee -> !tee.equals(this))
			  .limit(maxsize)
			  .collect(Collectors.toList());

	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
