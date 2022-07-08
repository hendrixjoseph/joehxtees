package com.joehxtees;

import java.io.IOException;

public class Main {

	public static void main(final String[] args) throws IOException {

		TeeProcessor.delete();

		final TeeProcessor processor = new TeeProcessor();

		processor.createDetailPages();
		processor.createIndex();
		processor.createSiteMap();
		processor.copyStaticFiles();

		System.out.println("Done!");
	}
}
