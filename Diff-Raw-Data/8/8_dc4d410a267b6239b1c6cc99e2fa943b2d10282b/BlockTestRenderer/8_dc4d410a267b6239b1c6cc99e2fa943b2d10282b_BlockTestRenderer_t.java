 /* Copyright (c) 2012-2013 Jesper Öqvist <jesper@llbit.se>
  *
  * This file is part of Chunky.
  *
  * Chunky is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Chunky is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * You should have received a copy of the GNU General Public License
  * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
  */
 package se.llbit.chunky.main;
 
 import java.io.File;
 import java.io.PrintStream;
 
 import se.llbit.chunky.renderer.test.TestRenderer;
 import se.llbit.chunky.resources.TexturePackLoader;
 import se.llbit.util.ProgramProperties;
 
 /**
  * Test renderer application
  * @author Jesper Öqvist <jesper@llbit.se>
  */
 public class BlockTestRenderer {
 
 	/**
 	 * Entry point
 	 * @param args
 	 */
 	public static void main(String[] args) {
		String lastTexturePack = ProgramProperties.getProperty("lastTexturePack");
		if (lastTexturePack != null)
			TexturePackLoader.loadTexturePack(new File(lastTexturePack), false);
		else
 			TexturePackLoader.loadTexturePack(Chunky.getMinecraftJar(), false);
 		
 		String block = "";
 		for (int i = 0; i < args.length; ++i) {
 			String arg = args[i];
 			if (arg.equals("-help") || arg.equals("-h")) {
 				printHelp(System.out);
 				return;
 			} else {
 				if (block.isEmpty()) {
 					block = arg;
 				} else {
 					System.err.println("Too many arguments!");
 					System.exit(1);
 				}
 			}
 		}
 		
 		TestRenderer renderer;
 		
 		if (!block.isEmpty()) {
 			int sep = block.indexOf(':');
 			String blockPart;
 			if (sep == -1) {
 				blockPart = block;
 			} else {
 				blockPart = block.substring(0, sep);
 			}
 			int blockId = Integer.parseInt(blockPart);
 			renderer = new TestRenderer(null, blockId);
 		} else {
 			renderer = new TestRenderer(null);
 		}
 		
 		renderer.start();
 	}
 
 	private static void printHelp(PrintStream out) {
 		out.println("Usage: BlockTestRenderer [ID[:METADATA]]");
 		out.println("    ID         is the id of the block to render");
 		out.println("    METADATA   specifies the metadata (TBD)");
 	}
 }
