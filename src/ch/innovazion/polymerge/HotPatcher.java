package ch.innovazion.polymerge;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HotPatcher extends Patcher {
	
	private final Map<Path, String> locationCache = new HashMap<>();
	private final Map<String, List<Path>> reverseLocationCache = new HashMap<>();
	
	public HotPatcher(String target, Path core, Path patches, Path output) {
		super(target, core, patches, output);
	}
	
	public void patch() throws IOException {
		super.patch();
		
		System.out.println("Starting watch service...");
		
		WatchService coreService = FileSystems.getDefault().newWatchService();
		WatchService patchesService = FileSystems.getDefault().newWatchService();

		registerRecursiveWatchService(getCore(), coreService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
		registerRecursiveWatchService(getPatches(), patchesService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);

		while(true) {
			try {
				WatchKey coreKey = coreService.poll();
				WatchKey patchesKey = patchesService.poll();

				if(coreKey != null) {
					coreKey.pollEvents().stream().map(this::cast).forEach(this::handleCoreChange);
					coreKey.reset();
				}
				
				if(patchesKey != null) {
					patchesKey.pollEvents().stream().map(this::cast).forEach(this::handlePatchesChange);
					patchesKey.reset();
				}
								
				Thread.sleep(500);
			} catch (InterruptedException e) {
				;
			}
		}
	}
	
	private void registerRecursiveWatchService(Path root, WatchService service, Kind<?>... events) throws IOException {
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
	        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
	            dir.register(service, events);
	            return FileVisitResult.CONTINUE;
	        }
	    });
	}
	
	private void handleCoreChange(WatchEvent<Path> event) {
		Path modified = event.context();
		Kind<Path> kind = event.kind();
		
		try {
			System.out.println("Watchservice (Core) [" + kind + "]: " + modified);
			
			Path relative = getCore().relativize(modified);
			Path target = getOutput().resolve(relative);
			
			if(kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
				Files.copy(modified, target);
				patchFromCache(relative.toString());
			} else if(kind == ENTRY_DELETE) {
				Files.deleteIfExists(target);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void patchFromCache(String location) throws IOException {
		List<Path> patches = reverseLocationCache.get(location);
		
		if(patches != null) {
			for(Path path : patches) {
				patch(path);
			}
		}
	}
	
	private void handlePatchesChange(WatchEvent<Path> event) {
		Path modified = event.context();
		Kind<Path> kind = event.kind();
		
		try {
			System.out.println("Watchservice (Patches) [" + kind + "]: " + modified);
			
			if(kind == ENTRY_DELETE || kind == ENTRY_MODIFY) {
				String location = locationCache.remove(modified);
				
				if(location != null) {
					Files.copy(getCore().resolve(location), getOutput().resolve(location), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			
			if(kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
				patch(modified);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Configuration patch(Path path) throws IOException {
		Configuration config = super.patch(path);
		
		locationCache.put(path, config.getLocation());
		reverseLocationCache.computeIfAbsent(config.getLocation(), e -> new ArrayList<>()).add(path);
		
		return config;
	}
	
	
	@SuppressWarnings("unchecked")
	private WatchEvent<Path> cast(WatchEvent<?> event) {
		return (WatchEvent<Path>) event;
	}
}