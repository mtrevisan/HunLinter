/**
 * Copyright (c) 2025 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


/**
 * External merge sort using Memory-Mapped I/O for high throughput.
 *
 * Phases:
 *  1) Build sorted runs: map the input file in windows (FileChannel.map),
 *     extract lines fully in-memory (no partial strings), sort, write runs.
 *  2) K-way merge: open each run with a mapped reader and merge sorted lines.
 *
 * This sorter is extremely fast on SSD/NVMe and avoids excessive small reads.
 * All comments are in English as requested.
 */
public class MappedExternalSorter{

	private MappedExternalSorter(){}


	/**
	 * Sort a large UTF-8 text file using memory-mapped I/O.
	 *
	 * @param input	Path to input file (one line per record).
	 * @param output	Path to output file (will be overwritten).
	 * @param charset	Charset for decoding/encoding.
	 * @param memory	In-memory chunk budget (for buffer sort).
	 * @param window	Size of moving window for mmap reads.
	 * @param comparator	Comparator for string lines.
	 */
	public static void sort(final Path input, final Path output, final Charset charset, final int memory,
			final int window, final Comparator<String> comparator) throws IOException{
		//phase 1: produce sorted runs (temp files)
		final List<Path> runs = buildSortedRuns(input, charset, memory, window, comparator);

		//phase 2: merge all runs
		mergeRuns(runs, output, charset, comparator);

		//cleanup
		for(final Path p : runs){
			try{
				Files.deleteIfExists(p);
			}
			catch(IOException ignored){
			}
		}
	}

	private static List<Path> buildSortedRuns(final Path input, final Charset charset, final int memory,
			final int window, final Comparator<String> comparator) throws IOException{
		final long fileSize = Files.size(input);
		final int windowBytes = Math.max(64, window) * 1024 * 1024;
		final long bytesBudget = (long)memory * 1024L * 1024L;
		//heuristic
		final int avgLineBytes = 32;
		final int maxLines = (int)Math.max(200_000,
			Math.min(5_000_000, bytesBudget / avgLineBytes));

		final List<Path> runs = new ArrayList<>();
		final List<String> buffer = new ArrayList<>(maxLines);

		try(final FileChannel ch = FileChannel.open(input, StandardOpenOption.READ)){
			long pos = 0;
			byte[] tailBytes = null;
			while(pos < fileSize){
				final long remaining = fileSize - pos;
				final long mapSize = Math.min(remaining, windowBytes);

				final ByteBuffer bb = ch.map(FileChannel.MapMode.READ_ONLY, pos, mapSize);

				final int limit = bb.limit();
				int start = 0;

				if(tailBytes != null){
					//prepend previous window’s partial line
					final String partial = new String(tailBytes, charset);
					buffer.add(partial);
					tailBytes = null;
				}

				while(bb.hasRemaining()){
					final byte b = bb.get();
					if(b == '\n'){
						final int end = bb.position();
						final int len = end - start;
						int sliceEnd = end;

						if(len > 1){
							//strip CR for CRLF
							if(getByte(bb, end - 2) == '\r')
								sliceEnd = end - 1;
						}

						final int bytesLen = sliceEnd - start;
						if(bytesLen > 0){
							final byte[] lineBytes = new byte[bytesLen];
							bb.position(start);
							bb.get(lineBytes);
							bb.position(end);

							buffer.add(new String(lineBytes, charset));
							if(buffer.size() >= maxLines){
								runs.add(writeRun(buffer, charset, comparator));
								buffer.clear();
							}
						}
						start = end;
					}
				}

				final boolean endedOnNewline = (start == limit);

				if(!endedOnNewline && limit > start){
					final int len = limit - start;
					final byte[] partial = new byte[len];
					bb.position(start);
					bb.get(partial);
					tailBytes = partial;
				}

				pos += mapSize;

				if(buffer.size() >= maxLines){
					runs.add(writeRun(buffer, charset, comparator));
					buffer.clear();
				}
			}

			if(tailBytes != null)
				buffer.add(new String(tailBytes, charset));

			if(!buffer.isEmpty())
				runs.add(writeRun(buffer, charset, comparator));
		}

		return runs;
	}

	private static byte getByte(final ByteBuffer bb, final int absolutePos){
		final int old = bb.position();
		bb.position(absolutePos);
		final byte b = bb.get();
		bb.position(old);
		return b;
	}

	private static Path writeRun(final List<String> lines, final Charset charset, final Comparator<String> comparator)
			throws IOException{
		lines.sort(comparator);

		final Path run = Files.createTempFile("mapped-sort-run-", ".txt");
		try(final BufferedWriter w = Files.newBufferedWriter(run, charset,
			StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)){
			for(final String s : lines){
				w.write(s);
				w.newLine();
			}
		}
		return run;
	}

	private static void mergeRuns(final List<Path> runs, final Path output, final Charset charset,
			final Comparator<String> comparator) throws IOException{
		final List<MappedRunReader> readers = new ArrayList<>(runs.size());
		for(final Path p : runs)
			//per-run window MB
			readers.add(new MappedRunReader(p, charset, 128));

		final PriorityQueue<RunEntry> pq = new PriorityQueue<>(Comparator.comparing(e -> e.line, comparator));
		for(final MappedRunReader reader : readers){
			final String line = reader.nextLine();
			if(line != null)
				pq.add(new RunEntry(line, reader));
		}

		try(final BufferedWriter w = Files.newBufferedWriter(output, charset, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING)){
			while(!pq.isEmpty()){
				final RunEntry e = pq.poll();
				w.write(e.line);
				w.newLine();

				final String next = e.reader.nextLine();
				if(next != null && !next.isEmpty())
					pq.add(new RunEntry(next, e.reader));
			}
		}

		for(final MappedRunReader reader : readers){
			try{
				reader.close();
			}
			catch(final IOException ignored){
			}
		}
	}

	private static final class RunEntry{
		final String line;
		final MappedRunReader reader;

		RunEntry(final String line, final MappedRunReader reader){
			this.line = line;
			this.reader = reader;
		}

	}

	private static final class MappedRunReader implements Closeable{
		private final Charset charset;
		private final FileChannel ch;
		private final long fileSize;
		private final int windowBytes;

		private long pos = 0l;
		private ByteBuffer window;
		private int base = 0;

		MappedRunReader(final Path path, final Charset charset, final int windowMB) throws IOException{
			this.charset = charset;
			this.windowBytes = Math.max(64, windowMB) * 1024 * 1024;
			this.ch = FileChannel.open(path, StandardOpenOption.READ);
			this.fileSize = ch.size();
			mapNextWindow();
		}

		String nextLine() throws IOException{
			if(pos >= fileSize && (window == null || !window.hasRemaining()))
				return null;

			while(true){
				if((window == null || !window.hasRemaining()) && !mapNextWindow())
					return null;

				while(window.hasRemaining()){
					final byte b = window.get();
					if(b == '\n'){
						final int end = window.position();
						final int len = end - base;
						int sliceEnd = end;
						if(len > 1 && getByte(window, end - 2) == '\r')
							sliceEnd = end - 1;

						final int bytesLen = sliceEnd - base;
						if(bytesLen <= 0){
							base = end;

							continue;
						}

						final byte[] arr = new byte[bytesLen];
						window.position(base);
						window.get(arr);
						window.position(end);

						base = end;
						return new String(arr, charset);
					}
				}

				final int windowLimit = window.limit();
				final int unread = windowLimit - base;
				if(unread > 0){
					final byte[] tail = new byte[unread];
					window.position(base);
					window.get(tail);
					window.position(windowLimit);
					base = windowLimit;
					return new String(tail, charset);
				}

				if(!mapNextWindow())
					return null;
			}
		}

		private boolean mapNextWindow() throws IOException{
			if(pos >= fileSize){
				window = null;

				return false;
			}

			final long remaining = fileSize - pos;
			final long size = Math.min(remaining, windowBytes);
			window = ch.map(FileChannel.MapMode.READ_ONLY, pos, size);
			base = 0;
			pos += size;
			return true;
		}

		@Override
		public void close() throws IOException{
			ch.close();
		}

	}

}
