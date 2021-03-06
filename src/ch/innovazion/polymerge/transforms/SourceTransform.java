/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2019 Innovazion
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package ch.innovazion.polymerge.transforms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.innovazion.polymerge.utils.LineStream;

public abstract class SourceTransform {
	
	private final Path root;
	
	public SourceTransform(Path root) {
		this.root = root;
	}
	
	protected Path resolveIdentifier(String identifier) throws IOException {
		Path resolved = root.resolve(identifier);
		Path parent = resolved.getParent();
		
		if(parent != null) {
			Files.createDirectories(parent);
		}
		
		if(!Files.exists(resolved)) {
			Files.createFile(resolved);
		}
		
		return resolved;
	}
	
	public abstract void apply(String identifier, LineStream stream) throws IOException;
}
