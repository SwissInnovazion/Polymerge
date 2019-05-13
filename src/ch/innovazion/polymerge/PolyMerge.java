package ch.innovazion.polymerge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PolyMerge {
	
	private final Path sources;
	private final Path patched;
	private final Path core;
	
	private final String target;
	
	public PolyMerge(Path sources, Path patched, String target) {
		this.sources = sources;
		this.patched = patched;
		this.core = sources.resolve("core");
		
		this.target = target;
	}
	
	public void start() {
		try {
			Files.createDirectories(sources);
			Files.createDirectories(patched);
			Files.createDirectory(core);
		} catch (IOException e) {
			System.err.println("Failed to create root directories.");
			return;
		}
			
		System.out.println("Starting HotPatcher for target " + target + "...");
		
		Path patches = sources.resolve(target.split(".")[0]);
		Path output = patched.resolve(target);
		
		if(Files.exists(patches) && Files.isDirectory(patches)) {
			Patcher patcher = new HotPatcher(target, core, patches, output);
			
			try {
				patcher.patch();
				System.out.println("Patcher terminated normally.");
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Patcher terminated with fatal failure.");
			}
			
		} else {
			System.err.println("Nothing to be done.");
		}
	}
}
