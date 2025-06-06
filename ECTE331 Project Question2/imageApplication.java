package image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList; // Added for managing threads
import java.util.List; // Added for managing threads
import java.util.concurrent.atomic.AtomicIntegerArray; // Added for shared atomic histogram
import java.util.concurrent.Callable; // Added for sub-histogram worker
import java.util.concurrent.ExecutorService; // Added for managing thread pool
import java.util.concurrent.Executors; // Added for managing thread pool
import java.util.concurrent.Future; // Added for getting results from Callable
import javax.imageio.ImageIO;

 
public class imageApplication{

	public static int level = 255;
	public static int numloops = 5;

   
	public static void main(String[] args) {
		String inputImagePath = "C:\\Users\\shahd\\Downloads\\ECTE331 Project Question2\\";
		String outputBasePath = "C:\\Users\\shahd\\Downloads\\ECTE331 Project Question2\\";

		String fileName1 = inputImagePath + "Rain_Tree.jpg";
		// String fileName2="c:/image/Rain_Tree.jpg"; // This variable was unused for specific outputs

		// int[] numThread = {1,2,4,10}; // Will be used later for multithreading
		
		colourImage inputImg = new colourImage();
        // read the image filename1 and store its dimension and pixel values in ImgStruct
		imageReadWrite.readJpgImage(fileName1, inputImg);

		if (inputImg.width == 0 || inputImg.height == 0) {
			System.err.println("Failed to read input image or image is empty. Aborting processing.");
			return; // Exit main or handle error appropriately
		}

		// Create an output image structure
		colourImage outputImg = new colourImage();
		outputImg.width = inputImg.width;
		outputImg.height = inputImg.height;
		outputImg.pixels = new short[inputImg.height][inputImg.width][3];

		System.out.println("Starting Single-Threaded Histogram Equalization...");
		long startTimeSingle = System.currentTimeMillis();
		for(int i = 0; i < numloops; i++) {
			SingleHistogramEqualization(inputImg, outputImg); // Use a distinct output for verification if needed
		}
		long endTimeSingle = System.currentTimeMillis();
		long durationSingle = (endTimeSingle - startTimeSingle) / numloops;
		System.out.println("Single-Threaded Execution Time (avg over " + numloops + " runs): " + durationSingle + " ms");

		// Save single-threaded result
		String singleThreadedOutputFile = outputBasePath + "Rain_Tree_Output_Single.jpg";
		imageReadWrite.writeJpgImage(outputImg, singleThreadedOutputFile);
		System.out.println("Processed image (single-threaded) saved to: " + singleThreadedOutputFile);

		// --- Multi-Threaded Execution (Design i - Shared Atomic Histogram - Variant a) ---
		int[] numThreadsOptions = {1, 2, 4, 8}; // Example thread counts to test
		colourImage outputImgMultiAtomic = new colourImage(); // Separate output for this version
		outputImgMultiAtomic.width = inputImg.width;
		outputImgMultiAtomic.height = inputImg.height;
		outputImgMultiAtomic.pixels = new short[inputImg.height][inputImg.width][3];

		for (int numThreads : numThreadsOptions) {
			System.out.println("\nStarting Multi-Threaded (Shared Atomic Histogram, Variant a) with " + numThreads + " threads...");
			long startTimeMultiAtomicA = System.currentTimeMillis();
			for (int i = 0; i < numloops; i++) {
				multiThreadedHistogramEqualization_SharedAtomic(inputImg, outputImgMultiAtomic, numThreads, 'a');
			}
			long endTimeMultiAtomicA = System.currentTimeMillis();
			long durationMultiAtomicA = (endTimeMultiAtomicA - startTimeMultiAtomicA) / numloops;
			System.out.println("Multi-Threaded (Shared Atomic, Variant a) with " + numThreads + " threads - Execution Time (avg over " + numloops + " runs): " + durationMultiAtomicA + " ms");
			
			// Save multi-threaded result (optional, could be for the last numThreads option or specific one)
			if (numThreads == numThreadsOptions[numThreadsOptions.length-1]) { // Save last result for variant 'a'
				String multiAtomicAOutputFile = outputBasePath + "Rain_Tree_Output_Multi_Atomic_A_" + numThreads + ".jpg";
				imageReadWrite.writeJpgImage(outputImgMultiAtomic, multiAtomicAOutputFile);
				System.out.println("Processed image (multi-threaded, atomic, variant a, " + numThreads + " threads) saved to: " + multiAtomicAOutputFile);
			}

			// Variant b for Shared Atomic Histogram
			System.out.println("\nStarting Multi-Threaded (Shared Atomic Histogram, Variant b) with " + numThreads + " threads...");
			colourImage outputImgMultiAtomicB = new colourImage(); // Separate output for this version if needed for verification
			outputImgMultiAtomicB.width = inputImg.width;
			outputImgMultiAtomicB.height = inputImg.height;
			outputImgMultiAtomicB.pixels = new short[inputImg.height][inputImg.width][3];
			long startTimeMultiAtomicB = System.currentTimeMillis();
			for (int i = 0; i < numloops; i++) {
				multiThreadedHistogramEqualization_SharedAtomic(inputImg, outputImgMultiAtomicB, numThreads, 'b');
			}
			long endTimeMultiAtomicB = System.currentTimeMillis();
			long durationMultiAtomicB = (endTimeMultiAtomicB - startTimeMultiAtomicB) / numloops;
			System.out.println("Multi-Threaded (Shared Atomic, Variant b) with " + numThreads + " threads - Execution Time (avg over " + numloops + " runs): " + durationMultiAtomicB + " ms");

			if (numThreads == numThreadsOptions[numThreadsOptions.length-1]) { // Save last result for variant 'b'
				String multiAtomicBOutputFile = outputBasePath + "Rain_Tree_Output_Multi_Atomic_B_" + numThreads + ".jpg";
				imageReadWrite.writeJpgImage(outputImgMultiAtomicB, multiAtomicBOutputFile);
				System.out.println("Processed image (multi-threaded, atomic, variant b, " + numThreads + " threads) saved to: " + multiAtomicBOutputFile);
			}
		}

		// --- Multi-Threaded Execution (Design ii - Multiple Sub-Histograms) ---
		colourImage outputImgMultiSub = new colourImage(); // Separate output for this version
		outputImgMultiSub.width = inputImg.width;
		outputImgMultiSub.height = inputImg.height;
		outputImgMultiSub.pixels = new short[inputImg.height][inputImg.width][3];

		for (int numThreads : numThreadsOptions) { // Reuse numThreadsOptions
			System.out.println("\nStarting Multi-Threaded (Multiple Sub-Histograms) with " + numThreads + " threads...");
			long startTimeMultiSub = System.currentTimeMillis();
			for (int i = 0; i < numloops; i++) {
				multiThreadedHistogramEqualization_SubHistograms(inputImg, outputImgMultiSub, numThreads);
			}
			long endTimeMultiSub = System.currentTimeMillis();
			long durationMultiSub = (endTimeMultiSub - startTimeMultiSub) / numloops;
			System.out.println("Multi-Threaded (Sub-Histograms) with " + numThreads + " threads - Execution Time (avg over " + numloops + " runs): " + durationMultiSub + " ms");

			// Save multi-threaded result (optional)
			if (numThreads == numThreadsOptions[numThreadsOptions.length-1]) { // Save last result
				String multiSubHistOutputFile = outputBasePath + "Rain_Tree_Output_Multi_SubHist_" + numThreads + ".jpg";
				imageReadWrite.writeJpgImage(outputImgMultiSub, multiSubHistOutputFile);
				System.out.println("Processed image (multi-threaded, sub-histograms, " + numThreads + " threads) saved to: " + multiSubHistOutputFile);
			}
		}
	} // main		

	public static void SingleHistogramEqualization(colourImage input, colourImage output) {
		int width = input.width;
		int height = input.height;
		int size = height * width;

		for(int colorChannel = 0; colorChannel < 3; colorChannel++) {
			// 1. Histogram Computation
			int[] histogram = new int[level + 1]; // Size L+1 (e.g., 256 for 0-255)
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					histogram[input.pixels[i][j][colorChannel]]++;
				}
			}

			// 2. Cumulative Histogram Transformation
			long[] cumulativeHistogram = new long[level + 1];
			cumulativeHistogram[0] = histogram[0];
			for(int i = 1; i <= level; i++) {
				cumulativeHistogram[i] = cumulativeHistogram[i-1] + histogram[i];
			}

			// Transform cumulative histogram: CH[i] = round((CH[i] * L) / size)
			int[] transformedCumulativeHistogram = new int[level + 1];
			for(int i = 0; i <= level; i++) {
				transformedCumulativeHistogram[i] = (int) Math.round((double)(cumulativeHistogram[i] * level) / size);
			}

			// 3. Image Mapping
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					short originalPixelValue = input.pixels[i][j][colorChannel];
					output.pixels[i][j][colorChannel] = (short) transformedCumulativeHistogram[originalPixelValue];
				}
			}
		}
	} // SingleHistogramEqualization 

	// Helper class for multi-threaded histogram building (Shared Atomic Histogram)
	static class AtomicHistogramWorker implements Runnable {
		private final short[] channelPixels1D;
		private final int startIdx;
		private final int endIdx;
		private final AtomicIntegerArray sharedHistogram;
		private final char variant; // 'a' for contiguous, 'b' for interleaved
		private final int numThreads; // Needed for interleaved variant
		private final int threadId;   // Needed for interleaved variant


		public AtomicHistogramWorker(short[] channelPixels1D, int startIdx, int endIdx, AtomicIntegerArray sharedHistogram, char variant, int threadId, int numThreads) {
			this.channelPixels1D = channelPixels1D;
			this.startIdx = startIdx;
			this.endIdx = endIdx;
			this.sharedHistogram = sharedHistogram;
			this.variant = variant;
			this.threadId = threadId;
			this.numThreads = numThreads;
		}

		@Override
		public void run() {
			if (variant == 'a') { // Variant (a) - Contiguous Blocks
				for (int i = startIdx; i < endIdx; i++) {
					sharedHistogram.incrementAndGet(channelPixels1D[i] & 0xFFFF); // Use mask for unsigned short
				}
			} else if (variant == 'b') { // Variant (b) - Interleaved Pixels
				for (int i = threadId; i < channelPixels1D.length; i += numThreads) {
                    // Ensure the pixel index is within the worker's conceptual overall segment,
                    // though actual access is interleaved across the whole array.
                    // This check might be redundant if startIdx/endIdx are not used for 'b'
                    // or if partitions for 'b' are defined differently.
                    // For now, assuming each thread processes its interleaved part of the *entire* channel.
					sharedHistogram.incrementAndGet(channelPixels1D[i] & 0xFFFF);
				}
			}
		}
	}

	public static void multiThreadedHistogramEqualization_SharedAtomic(colourImage input, colourImage output, int numOfThreads, char variant) {
		int width = input.width;
		int height = input.height;
		int totalPixelsPerChannel = width * height;

		List<Thread> threads = new ArrayList<>();

		for (int colorChannel = 0; colorChannel < 3; colorChannel++) {
			// 1. Flatten pixel data for the current channel to 1D array
			short[] channelPixels1D = new short[totalPixelsPerChannel];
			int k = 0;
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					channelPixels1D[k++] = input.pixels[i][j][colorChannel];
				}
			}

			// 2. Initialize shared AtomicIntegerArray histogram
			AtomicIntegerArray atomicHistogram = new AtomicIntegerArray(level + 1);

			// 3. Create and start threads for histogram building
			threads.clear();
			int pixelsPerThread = totalPixelsPerChannel / numOfThreads;
			int remainderPixels = totalPixelsPerChannel % numOfThreads;

			for (int t = 0; t < numOfThreads; t++) {
				int startIdx = 0; // Only used for variant 'a'
				int endIdx = 0;   // Only used for variant 'a'

				if (variant == 'a') {
					startIdx = t * pixelsPerThread + Math.min(t, remainderPixels);
					endIdx = startIdx + pixelsPerThread + (t < remainderPixels ? 1 : 0);
				}
                // For variant 'b', startIdx/endIdx for the worker are conceptual for its assigned portion,
                // but the loop iterates based on threadId and numThreads across the whole array.
                // The AtomicHistogramWorker constructor takes startIdx and endIdx, but they are not strictly
                // used by variant 'b' in its loop logic, which is fine.
				AtomicHistogramWorker worker = new AtomicHistogramWorker(channelPixels1D, startIdx, endIdx, atomicHistogram, variant, t, numOfThreads);
				Thread thread = new Thread(worker);
				threads.add(thread);
				thread.start();
			}

			// 4. Wait for all threads to complete
			for (Thread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					System.err.println("Thread interrupted: " + e.getMessage());
					Thread.currentThread().interrupt(); // Restore interrupted status
				}
			}

			// 5. Convert AtomicIntegerArray to int[] histogram
			int[] finalHistogram = new int[level + 1];
			for (int i = 0; i <= level; i++) {
				finalHistogram[i] = atomicHistogram.get(i);
			}

			// 6. Cumulative Histogram Transformation (Main thread)
			long[] cumulativeHistogram = new long[level + 1];
			cumulativeHistogram[0] = finalHistogram[0];
			for (int i = 1; i <= level; i++) {
				cumulativeHistogram[i] = cumulativeHistogram[i-1] + finalHistogram[i];
			}

			int[] transformedCumulativeHistogram = new int[level + 1];
			for (int i = 0; i <= level; i++) {
				transformedCumulativeHistogram[i] = (int) Math.round((double)(cumulativeHistogram[i] * level) / totalPixelsPerChannel);
			}

			// 7. Image Mapping (Main thread)
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					short originalPixelValue = input.pixels[i][j][colorChannel];
					output.pixels[i][j][colorChannel] = (short) transformedCumulativeHistogram[originalPixelValue & 0xFFFF];
				}
			}
		} // end of colorChannel loop
	} // multiThreadedHistogramEqualization_SharedAtomic

	// Helper class for multi-threaded histogram building (Multiple Sub-Histograms)
	static class SubHistogramWorker implements Callable<int[]> {
		private final short[] channelPixels1D;
		private final int startIdx;
		private final int endIdx;
		private final int histogramLevels;

		public SubHistogramWorker(short[] channelPixels1D, int startIdx, int endIdx, int histogramLevels) {
			this.channelPixels1D = channelPixels1D;
			this.startIdx = startIdx;
			this.endIdx = endIdx;
			this.histogramLevels = histogramLevels;
		}

		@Override
		public int[] call() throws Exception {
			int[] localHistogram = new int[histogramLevels + 1];
			for (int i = startIdx; i < endIdx; i++) {
				localHistogram[channelPixels1D[i] & 0xFFFF]++; // Use mask for unsigned short
			}
			return localHistogram;
		}
	}

	public static void multiThreadedHistogramEqualization_SubHistograms(colourImage input, colourImage output, int numOfThreads) {
		int width = input.width;
		int height = input.height;
		int totalPixelsPerChannel = width * height;

		ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);

		for (int colorChannel = 0; colorChannel < 3; colorChannel++) {
			// 1. Flatten pixel data for the current channel to 1D array
			short[] channelPixels1D = new short[totalPixelsPerChannel];
			int k = 0;
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					channelPixels1D[k++] = input.pixels[i][j][colorChannel];
				}
			}

			// 2. Create tasks for local histogram building
			List<Future<int[]>> futureLocalHistograms = new ArrayList<>();
			int pixelsPerThread = totalPixelsPerChannel / numOfThreads;
			int remainderPixels = totalPixelsPerChannel % numOfThreads;

			for (int t = 0; t < numOfThreads; t++) {
				int startIdx = t * pixelsPerThread + Math.min(t, remainderPixels);
				int endIdx = startIdx + pixelsPerThread + (t < remainderPixels ? 1 : 0);
				SubHistogramWorker worker = new SubHistogramWorker(channelPixels1D, startIdx, endIdx, level);
				futureLocalHistograms.add(executor.submit(worker));
			}

			// 3. Collect and merge local histograms
			int[] finalHistogram = new int[level + 1];
			for (Future<int[]> future : futureLocalHistograms) {
				try {
					int[] localHist = future.get();
					for (int i = 0; i <= level; i++) {
						finalHistogram[i] += localHist[i];
					}
				} catch (Exception e) {
					System.err.println("Error collecting sub-histogram: " + e.getMessage());
					Thread.currentThread().interrupt();
					// Handle error appropriately, maybe by aborting or using partial data if robust
					return; // Exit if a thread failed critically
				}
			}

			// 4. Cumulative Histogram Transformation (Main thread)
			long[] cumulativeHistogram = new long[level + 1];
			cumulativeHistogram[0] = finalHistogram[0];
			for (int i = 1; i <= level; i++) {
				cumulativeHistogram[i] = cumulativeHistogram[i-1] + finalHistogram[i];
			}

			int[] transformedCumulativeHistogram = new int[level + 1];
			for (int i = 0; i <= level; i++) {
				if (totalPixelsPerChannel > 0) { // Avoid division by zero for empty images
 				    transformedCumulativeHistogram[i] = (int) Math.round((double)(cumulativeHistogram[i] * level) / totalPixelsPerChannel);
				} else {
					transformedCumulativeHistogram[i] = 0;
				}
			}

			// 5. Image Mapping (Main thread)
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					short originalPixelValue = input.pixels[i][j][colorChannel];
					output.pixels[i][j][colorChannel] = (short) transformedCumulativeHistogram[originalPixelValue & 0xFFFF];
				}
			}
		} // end of colorChannel loop

		executor.shutdown(); // Important to shut down the executor service
	} // multiThreadedHistogramEqualization_SubHistograms
  
    	
/**
 * 
 * A class with 2 Utility methods to read the pixels and dimension of an image, and write the image data into a jpeg file
 *
 */
static class imageReadWrite{

	public static void readJpgImage(String fileName, colourImage ImgStruct) {
		 try {
	            // Read the image file
	            File file = new File(fileName);
	            BufferedImage image = ImageIO.read(file);
	            
	            System.out.println("file: "+file.getCanonicalPath());
	            
	            // Check if the image is in sRGB color space
	            if (!image.getColorModel().getColorSpace().isCS_sRGB()) {
	                System.out.println("Image is not in sRGB color space");
	                return;
	            }
	            
	            // Get the width and height of the image
	            int width = image.getWidth();
	            int height = image.getHeight();
	            ImgStruct.width=width;
	            ImgStruct.height=height;
	            ImgStruct.pixels=new short[height][width][3];

	           // Loop over each pixel of the image and store its RGB color components in the array
	            for (int y = 0; y < height; y++) {
	                for (int x = 0; x < width; x++) {
	                    // Get the color of the current pixel
	                    int pixel = image.getRGB(x, y);
	                    Color color = new Color(pixel, true);

	                    // Store the red, green, and blue color components of the pixel in the array
	                    ImgStruct.pixels[y][x][0] = (short) color.getRed();
	                    ImgStruct.pixels[y][x][1] = (short) color.getGreen();
	                    ImgStruct.pixels[y][x][2] = (short) color.getBlue();
	                }
	            }            
	                       

	        } catch (IOException e) {
	            System.out.println("Error reading image file: " + e.getMessage());
	        }  	
	}

	public static void writeJpgImage(colourImage ImgStruct, String fileName) {
		 try {
	    	 int width = ImgStruct.width;
	         int height = ImgStruct.height;
	         BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

	         // Set the RGB color values of the BufferedImage using the pixel array
	         for (int y = 0; y < height; y++) {
	             for (int x = 0; x < width; x++) {
	                 int rgb = new Color(ImgStruct.pixels[y][x][0], ImgStruct.pixels[y][x][1], ImgStruct.pixels[y][x][2]).getRGB();
	                 image.setRGB(x, y, rgb);
	             }
	         }

	         // Write the BufferedImage to a JPEG file
	         File outputFile = new File(fileName);
	         ImageIO.write(image, "jpg", outputFile);

	     } catch (IOException e) {
	         System.out.println("Error writing image file: " + e.getMessage());
	     }       
	
       }//

}

static class matManipulation{
	/**
	 * reshape a matrix to a 1-D vector
	 */
	public static void mat2Vect (short [][] mat, int width, int height, short[] vect) {
		for(int i=0;i<height; i++)
			for (int j=0; j<width; j++)
				vect[j+i*width]=mat[i][j];
	}
	
}


static class colourImage {
	/**
	 * A datastructure to store a colour image
	 */
	public int width;
	public int height;
	public short pixels[][][];
}

} // This is the potentially missing closing brace for imageApplication class
