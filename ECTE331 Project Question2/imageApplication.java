package image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;
import javax.imageio.ImageIO;

public class imageApplication {

	// --- Configuration ---
	public static final int L = 255; // The highest intensity value for an 8-bit image
	public static final int NUM_LOOPS = 3; // Number of times to run each test for a stable average

	/**
	 * Main entry point for the application. Orchestrates the reading of an image,
	 * running single-threaded and various multi-threaded histogram equalization algorithms,
	 * and printing their performance.
	 * @param args Command line arguments (not used).
	 */
	public static void main(String[] args) {
		String inputImagePath = "Rain_Tree.jpg"; // Assumes image is in project root
		String outputBasePath = "./"; // Save output in the project root

		System.out.println("Reading image: " + inputImagePath);
		colourImage inputImg = imageReadWrite.readJpgImage(inputImagePath);
		if (inputImg == null) return; // Exit if image read failed

		// --- Run Single-Threaded Benchmark ---
		System.out.println("\n--- Starting Single-Threaded Benchmark ---");
		long totalSingleThreadTime = 0;
		for (int i = 0; i < NUM_LOOPS; i++) {
			colourImage outputImg = new colourImage(inputImg.width, inputImg.height);
			long startTime = System.nanoTime();
			SingleHistogramEqualization(inputImg, outputImg);
			totalSingleThreadTime += (System.nanoTime() - startTime);
		}
		long avgSingleThreadTime = (totalSingleThreadTime / NUM_LOOPS) / 1_000_000;
		System.out.printf("Single-Threaded Execution Time (avg over %d runs): %d ms\n", NUM_LOOPS, avgSingleThreadTime);
		
		// Save one final result for verification
		colourImage finalOutputSingle = new colourImage(inputImg.width, inputImg.height);
		SingleHistogramEqualization(inputImg, finalOutputSingle);
		imageReadWrite.writeJpgImage(finalOutputSingle, outputBasePath + "Rain_Tree_Output_Single.jpg");

		// CHANGE: The array is now named 'numOfThreads' as per the requirement.
		int[] numOfThreads = {1, 2, 4, 8};

		// --- Run Multi-Threaded Benchmarks ---
		for (int num : numOfThreads) {
			// Design i: Shared Atomic Histogram (Contiguous)
			runAndMeasure("Shared Atomic (Contiguous)", inputImg, num, (in, out) ->
					multiThreadedHistogramEqualization_SharedAtomic(in, out, num, 'a'));
			
			// Design i: Shared Atomic Histogram (Interleaved)
			runAndMeasure("Shared Atomic (Interleaved)", inputImg, num, (in, out) ->
					multiThreadedHistogramEqualization_SharedAtomic(in, out, num, 'b'));

			// Design ii: Sub-Histograms
			runAndMeasure("Sub-Histograms", inputImg, num, (in, out) ->
					multiThreadedHistogramEqualization_SubHistograms(in, out, num));
		}
	}

	/**
	 * CHANGE: Helper method to run and measure the execution time of a given algorithm.
	 * This refactors the main method to be cleaner and fixes the averaging bug.
	 */
	private static void runAndMeasure(String testName, colourImage input, int numThreads, RunnableAlgorithm algorithm) {
		System.out.printf("\n--- Starting: %s with %d thread(s) ---\n", testName, numThreads);
		long totalTime = 0;
		for (int i = 0; i < NUM_LOOPS; i++) {
			colourImage output = new colourImage(input.width, input.height);
			long startTime = System.nanoTime();
			algorithm.run(input, output);
			totalTime += (System.nanoTime() - startTime);
		}
		long avgTime = (totalTime / NUM_LOOPS) / 1_000_000;
		System.out.printf("%s with %d thread(s) - Execution Time (avg over %d runs): %d ms\n", testName, numThreads, NUM_LOOPS, avgTime);
	}

	// Functional interface for passing different algorithms to the test runner
	@FunctionalInterface
	interface RunnableAlgorithm {
		void run(colourImage input, colourImage output);
	}

	/**
	 * Implements histogram equalization using a single thread. Follows the optimized algorithm.
	 */
	public static void SingleHistogramEqualization(colourImage input, colourImage output) {
		int size = input.height * input.width;

		for (int colorChannel = 0; colorChannel < 3; colorChannel++) {
			int[] histogram = new int[L + 1];
			for (int i = 0; i < input.height; i++) {
				for (int j = 0; j < input.width; j++) {
					histogram[input.pixels[i][j][colorChannel]]++;
				}
			}

			long[] cumulativeHist = new long[L + 1];
			cumulativeHist[0] = histogram[0];
			for (int i = 1; i <= L; i++) {
				cumulativeHist[i] = cumulativeHist[i - 1] + histogram[i];
			}
			
			// This is the optimized approach: create a lookup table (LUT)
			int[] lookupTable = new int[L + 1];
			for (int i = 0; i <= L; i++) {
				lookupTable[i] = (int) Math.round((double) (cumulativeHist[i] * L) / size);
			}

			for (int i = 0; i < input.height; i++) {
				for (int j = 0; j < input.width; j++) {
					output.pixels[i][j][colorChannel] = (short) lookupTable[input.pixels[i][j][colorChannel]];
				}
			}
		}
	}

	/**
	 * Implements histogram equalization using multiple threads and a shared AtomicIntegerArray.
	 */
	public static void multiThreadedHistogramEqualization_SharedAtomic(colourImage input, colourImage output, int numOfThreads, char variant) {
		int totalPixels = input.width * input.height;

		for (int colorChannel = 0; colorChannel < 3; colorChannel++) {
			short[] channelPixels1D = flattenChannel(input, colorChannel);
			AtomicIntegerArray atomicHistogram = new AtomicIntegerArray(L + 1);
			
			List<Thread> threads = new ArrayList<>();
			int pixelsPerThread = totalPixels / numOfThreads;
			for (int t = 0; t < numOfThreads; t++) {
				int start = t * pixelsPerThread;
				int end = (t == numOfThreads - 1) ? totalPixels : start + pixelsPerThread;
				Thread thread = new Thread(new AtomicHistogramWorker(channelPixels1D, start, end, atomicHistogram, variant, t, numOfThreads));
				threads.add(thread);
				thread.start();
			}

			for (Thread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
			
			// Remainder of algorithm is sequential and uses the final histogram
			processAndMap(input, output, colorChannel, atomicHistogram, totalPixels);
		}
	}

	/**
	 * Implements histogram equalization using multiple threads, each with its own sub-histogram.
	 */
	public static void multiThreadedHistogramEqualization_SubHistograms(colourImage input, colourImage output, int numOfThreads) {
		int totalPixels = input.width * input.height;
		ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);

		for (int colorChannel = 0; colorChannel < 3; colorChannel++) {
			short[] channelPixels1D = flattenChannel(input, colorChannel);
			
			List<Future<int[]>> futureResults = new ArrayList<>();
			int pixelsPerThread = totalPixels / numOfThreads;
			for (int t = 0; t < numOfThreads; t++) {
				int start = t * pixelsPerThread;
				int end = (t == numOfThreads - 1) ? totalPixels : start + pixelsPerThread;
				futureResults.add(executor.submit(new SubHistogramWorker(channelPixels1D, start, end)));
			}

			int[] finalHistogram = new int[L + 1];
			for (Future<int[]> future : futureResults) {
				try {
					int[] localHist = future.get();
					for (int i = 0; i <= L; i++) {
						finalHistogram[i] += localHist[i];
					}
				} catch (Exception e) { e.printStackTrace(); }
			}
			
			processAndMap(input, output, colorChannel, finalHistogram, totalPixels);
		}
		executor.shutdown();
	}
	
	// --- Helper Methods and Classes ---

	private static short[] flattenChannel(colourImage input, int channel) {
		short[] flattened = new short[input.width * input.height];
		int k = 0;
		for (int i = 0; i < input.height; i++) {
			for (int j = 0; j < input.width; j++) {
				flattened[k++] = input.pixels[i][j][channel];
			}
		}
		return flattened;
	}
	
	private static void processAndMap(colourImage input, colourImage output, int channel, AtomicIntegerArray histogram, int totalPixels) {
		int[] regularHistogram = new int[L + 1];
		for (int i = 0; i <= L; i++) regularHistogram[i] = histogram.get(i);
		processAndMap(input, output, channel, regularHistogram, totalPixels);
	}

	private static void processAndMap(colourImage input, colourImage output, int channel, int[] histogram, int totalPixels) {
		long[] cumulativeHist = new long[L + 1];
		cumulativeHist[0] = histogram[0];
		for (int i = 1; i <= L; i++) {
			cumulativeHist[i] = cumulativeHist[i - 1] + histogram[i];
		}

		int[] lookupTable = new int[L + 1];
		for (int i = 0; i <= L; i++) {
			lookupTable[i] = (int) Math.round((double) (cumulativeHist[i] * L) / totalPixels);
		}

		for (int i = 0; i < input.height; i++) {
			for (int j = 0; j < input.width; j++) {
				output.pixels[i][j][channel] = (short) lookupTable[input.pixels[i][j][channel]];
			}
		}
	}
	
	static class AtomicHistogramWorker implements Runnable {
		private final short[] pixels;
		private final int start, end, threadId, numThreads;
		private final AtomicIntegerArray sharedHistogram;
		private final char variant;

		AtomicHistogramWorker(short[] p, int s, int e, AtomicIntegerArray hist, char v, int tId, int nT) {
			pixels = p; start = s; end = e; sharedHistogram = hist; variant = v; threadId = tId; numThreads = nT;
		}

		@Override
		public void run() {
			if (variant == 'a') {
				for (int i = start; i < end; i++) sharedHistogram.incrementAndGet(pixels[i] & 0xFFFF);
			} else { // variant 'b'
				for (int i = threadId; i < pixels.length; i += numThreads) sharedHistogram.incrementAndGet(pixels[i] & 0xFFFF);
			}
		}
	}

	static class SubHistogramWorker implements Callable<int[]> {
		private final short[] pixels;
		private final int start, end;

		SubHistogramWorker(short[] p, int s, int e) { pixels = p; start = s; end = e; }

		@Override
		public int[] call() {
			int[] localHistogram = new int[L + 1];
			for (int i = start; i < end; i++) localHistogram[pixels[i] & 0xFFFF]++;
			return localHistogram;
		}
	}

	static class imageReadWrite {
		public static colourImage readJpgImage(String fileName) {
			try {
				BufferedImage image = ImageIO.read(new File(fileName));
				if (image == null) {
					System.err.println("Error: Could not read image file. It might be missing or corrupted.");
					return null;
				}
				colourImage img = new colourImage(image.getWidth(), image.getHeight());
				for (int y = 0; y < img.height; y++) {
					for (int x = 0; x < img.width; x++) {
						Color color = new Color(image.getRGB(x, y));
						img.pixels[y][x][0] = (short) color.getRed();
						img.pixels[y][x][1] = (short) color.getGreen();
						img.pixels[y][x][2] = (short) color.getBlue();
					}
				}
				return img;
			} catch (IOException e) {
				System.err.println("Error reading image file: " + e.getMessage());
				return null;
			}
		}

		public static void writeJpgImage(colourImage img, String fileName) {
			try {
				BufferedImage image = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB);
				for (int y = 0; y < img.height; y++) {
					for (int x = 0; x < img.width; x++) {
						int r = img.pixels[y][x][0];
						int g = img.pixels[y][x][1];
						int b = img.pixels[y][x][2];
						image.setRGB(x, y, new Color(r, g, b).getRGB());
					}
				}
				ImageIO.write(image, "jpg", new File(fileName));
				System.out.println("Successfully saved image to: " + fileName);
			} catch (IOException e) {
				System.err.println("Error writing image file: " + e.getMessage());
			}
		}
	}

	static class colourImage {
		public int width, height;
		public short[][][] pixels;

		public colourImage(int w, int h) {
			width = w;
			height = h;
			pixels = new short[h][w][3];
		}
	}
}
