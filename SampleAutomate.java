import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.text.DecimalFormat;


public class SampleAutomate {

	BufferedImage imgOne;

	// Modify the height and width values here to read and display an image with
  	// different dimensions. 
	int width = 512;
	int height = 512;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					// byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
            raf.close();
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}


	public static BufferedImage nonUniformQuantizeImage(BufferedImage originalImage, int totalBuckets) {
		int n = (int) Math.round(Math.pow(totalBuckets, 1.0 / 3.0));
		int bucketSize = n;
        // System.out.println("bukctSize: "+bucketSize);
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage quantizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Iterate over each pixel and calculate the histogram for each channel
        int[][] histogram = new int[3][256]; // Histogram for each channel

        // Calculate histogram
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
				histogram[0][r]++;
				histogram[1][g]++;
				histogram[2][b]++;
            }
        }
		// for(int i=0; i<256; i++){
		// 	System.out.println(histogram[0][i]);
		// }
        // System.out.println("--------------------------endofR--------");
        // for(int i=0; i<histogram[1].length; i++){
		// 	System.out.println(histogram[1][i]);
		// }
        // System.out.println("---------------------endofG----------");
        // for(int i=0; i<histogram[2].length; i++){
		// 	System.out.println(histogram[2][i]);
		// }
        // System.out.println("-------------------endofB--------");

        // Determine bucket sizes and representative colors for each channel
        int[][] bucketSizesAndRepresentativesR = calculateBucketSizesAndRepresentatives(histogram, 0, bucketSize);
        int[][] bucketSizesAndRepresentativesG = calculateBucketSizesAndRepresentatives(histogram, 1, bucketSize);
        int[][] bucketSizesAndRepresentativesB = calculateBucketSizesAndRepresentatives(histogram, 2, bucketSize);

        // Quantize each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Find the bucket index and representative color for each channel
                int bucketIndexR = findBucketIndex(r, bucketSizesAndRepresentativesR);
                int bucketIndexG = findBucketIndex(g, bucketSizesAndRepresentativesG);
                int bucketIndexB = findBucketIndex(b, bucketSizesAndRepresentativesB);
                

                // Combine channels
                int quantizedRGB = (r == 0 ? 0: (bucketSizesAndRepresentativesR[bucketIndexR][1] << 16)) |
                                   (g == 0 ? 0: (bucketSizesAndRepresentativesG[bucketIndexG][1] << 8)) |
                                   (b == 0 ? 0: (bucketSizesAndRepresentativesB[bucketIndexB][1]));
                

                quantizedImage.setRGB(x, y, quantizedRGB);
            }
        }

        return quantizedImage;
    }

	public static int[][] calculateBucketSizesAndRepresentatives(int[][] histogram, int channel, int totalBuckets) {
        int[][] bucketSizesAndRepresentatives = new int[totalBuckets][2];
    
        // Calculate total pixel count and initialize variables
        int totalPixels = 0;
        for (int i = 0; i < 256; i++) {
            totalPixels += histogram[channel][i];
        }
        int targetSize = totalPixels / totalBuckets;
        boolean overshoot = true;
        int currentBucket = 0;
        int currentSize = 0;
        int currentBucketSum = 0;
    
        for (int i = 0; i < 256; i++) {
            int count = histogram[channel][i];
    
            // Check if adding current value would exceed target
            if (currentSize + count > targetSize && currentBucket < totalBuckets) {
                // Calculate representative color (handle empty bucket case)
                int representativeColor = currentSize == 0 ? i : currentBucketSum / currentSize;
                bucketSizesAndRepresentatives[currentBucket][0] = i - (overshoot ? 1 : 0);
                bucketSizesAndRepresentatives[currentBucket][1] = representativeColor;
                currentBucket++;
                currentSize = count;
                currentBucketSum = count * i;
                overshoot = !overshoot;
            } else {
                // Update current size and sum if below target
                currentSize += count;
                currentBucketSum += count * i;
            }
    
            // Fill the last bucket even if it doesn't reach target size
            if (i == 255 && currentBucket < totalBuckets - 1) {
                bucketSizesAndRepresentatives[currentBucket][0] = 255 - (overshoot ? 1 : 0);
                bucketSizesAndRepresentatives[currentBucket][1] = currentSize == 0 ? 255 : currentBucketSum / currentSize;
                currentBucket++;
            }
        }
    
        return bucketSizesAndRepresentatives;
    }
    
    

	public static int findBucketIndex(int value, int[][] bucketSizesAndRepresentatives) {
		int index = 0;
		// Find the bucket index where the value falls
		while (index < bucketSizesAndRepresentatives.length && value > bucketSizesAndRepresentatives[index][0]) {
			index++;
		}
		// Adjust index to ensure it's within bounds
		return Math.min(index, bucketSizesAndRepresentatives.length - 1);
	}
	

    public static BufferedImage quantizeImage(BufferedImage originalImage, int totalBuckets) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage quantizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Calculate the number of buckets per channel
        int n = (int) Math.round(Math.pow(totalBuckets, 1.0 / 3.0));
        // System.out.println(totalBuckets+" : "+n);
        // int bucketSize = 256 / n;

        double[][] bucketSizesAndRepresentatives = calculateBucketSizesAndRepresentatives0(n);

        // Quantize each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Quantize each channel
                // int quantizedR = (r / bucketSize) * bucketSize + bucketSize / 2;
                // int quantizedG = (g / bucketSize) * bucketSize + bucketSize / 2;
                // int quantizedB = (b / bucketSize) * bucketSize + bucketSize / 2;
                // int bucketIndexR = findBucketIndex0(r, bucketSizesAndRepresentatives);
                // int bucketIndexG = findBucketIndex0(g, bucketSizesAndRepresentatives);
                // int bucketIndexB = findBucketIndex0(b, bucketSizesAndRepresentatives);



                // // Combine channels
                // // int quantizedRGB = (quantizedR << 16) | (quantizedG << 8) | quantizedB;

                // int quantizedRGB = (r == 0 ? 0: (bucketSizesAndRepresentatives[bucketIndexR][1] << 16)) |
                //                    (g == 0 ? 0: (bucketSizesAndRepresentatives[bucketIndexG][1] << 8)) |
                //                    (b == 0 ? 0: (bucketSizesAndRepresentatives[bucketIndexB][1]));
                

                int bucketIndexR = findBucketIndex0(r, bucketSizesAndRepresentatives);
                int bucketIndexG = findBucketIndex0(g, bucketSizesAndRepresentatives);
                int bucketIndexB = findBucketIndex0(b, bucketSizesAndRepresentatives);
    
                // Retrieve representative values from the bucketSizesAndRepresentatives array
                int quantizedR = (int)bucketSizesAndRepresentatives[bucketIndexR][1];
                int quantizedG = (int)bucketSizesAndRepresentatives[bucketIndexG][1];
                int quantizedB = (int)bucketSizesAndRepresentatives[bucketIndexB][1];
    
                // Combine channels
                int quantizedRGB = (quantizedR << 16) | (quantizedG << 8) | quantizedB;
    

                quantizedImage.setRGB(x, y, quantizedRGB);
            }
        }

        return quantizedImage;
    }

    public static double[][] calculateBucketSizesAndRepresentatives0(int totalBuckets) {
        double[][] colorAndRepresentatives = new double[totalBuckets+1][2];
    
        for (int i = 0; i < totalBuckets; i++) {
            // Calculate the boundary values for the bucket
            double startValue = i * (256.0 / totalBuckets);
            double endValue = (i + 1) * (256.0 / totalBuckets) - 1; // Adjusted end value
    
            // Round the values to the nearest integer
            int roundedStartValue = (int) Math.round(startValue);
            int roundedEndValue = (int) Math.round(endValue);
            double representativeColor = (roundedStartValue + roundedEndValue) / 2.0;
    
            // Assign the boundary values and representative color
            colorAndRepresentatives[i][0] = roundedEndValue; // Bucket size (boundary value)
            colorAndRepresentatives[i][1] = representativeColor; // Representative color
    
            // Break if the end value exceeds 255
            // if (roundedEndValue >= 256) {
            //     break;
            // }
    
            // Print for debugging
            // System.out.println(i + "-Bucket size: " + colorAndRepresentatives[i][0]);
            // System.out.println("Representative: " + colorAndRepresentatives[i][1]);
        }
    
        return colorAndRepresentatives;
    }    

    // public static double[][] calculateBucketSizesAndRepresentatives0(int totalBuckets) {
    //     double[][] colorAndRepresentatives = new double[totalBuckets][2];
    //     //double bucketSize = Math.round(256.0f / totalBuckets);
    //     for (int i = 0; i < totalBuckets; i++) {
    //             // Calculate the boundary values for the bucket
    //             double startValue = i * (256/totalBuckets);
    //             double endValue = startValue + (256/totalBuckets); // Subtract 1 to avoid overlapping with the next bucket
        
    //             // Assign the boundary values and representative color
    //             colorAndRepresentatives[i][0] = endValue; // Bucket size (boundary value)
    //             colorAndRepresentatives[i][1] = (startValue + endValue) / 2; // Representative color


    //         // colorAndRepresentatives[i][0] = bucketSize; // Bucket size
    //         // colorAndRepresentatives[i][1] = bucketSize * (i + 1) - bucketSize / 2; // Representative
    //         if((256/totalBuckets) * (i) - (256/totalBuckets) /2  >= 256){
    //             break;
    //         }
    //         System.out.println(i+"-Bucket size: " + colorAndRepresentatives[i][0]);
    //         System.out.println("Representative: " + colorAndRepresentatives[i][1]);
    //     }
    //     return colorAndRepresentatives;
    // }
    
    public static int findBucketIndex0(int value, double[][] bucketSizesAndRepresentatives) {
        int index = 0;
        // Find the bucket index where the value falls
        while (index < bucketSizesAndRepresentatives.length && value > bucketSizesAndRepresentatives[index][0]) {
            index++;
        }
        // Adjust index to ensure it's within bounds
        return Math.min(index, bucketSizesAndRepresentatives.length - 1);
    }
    
    
    


    public static int calculateAbsoluteError(BufferedImage originalImage, BufferedImage quantizedImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int totalError = 0;
        int errrorR = 0;
        int errrorG = 0;
        int errrorB = 0;

        // Calculate absolute error for each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalRGB = originalImage.getRGB(x, y);
                int quantizedRGB = quantizedImage.getRGB(x, y);

                // Extract original and quantized RGB values
                int originalR = (originalRGB >> 16) & 0xFF;
                int originalG = (originalRGB >> 8) & 0xFF;
                int originalB = originalRGB & 0xFF;

                int quantizedR = (quantizedRGB >> 16) & 0xFF;
                int quantizedG = (quantizedRGB >> 8) & 0xFF;
                int quantizedB = quantizedRGB & 0xFF;

                // Calculate absolute error for each channel
                int errorR = Math.abs(originalR - quantizedR);
                int errorG = Math.abs(originalG - quantizedG);
                int errorB = Math.abs(originalB - quantizedB);

                // Calculate total absolute error
                totalError += errorR + errorG + errorB;
                errrorR += errorR;
                errrorG += errorG;
                errrorB += errorB;
            }
        }

        // System.out.println("errorR: "+errrorR);
        // System.out.println("errorG: "+errrorG);
        // System.out.println("errorB: "+errrorB);

        // Normalize absolute error by the number of pixels and channels
        return totalError;
    }

	public void showIms(String[] args){

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

		// Use label to display the image

        int quantizationMode = Integer.parseInt(args[1]);
        int numberOfBuckets = Integer.parseInt(args[2]);
        BufferedImage quantizedImg;

        switch (quantizationMode) {
            case 1:
                quantizedImg = quantizeImage(imgOne, numberOfBuckets);
                System.out.println(calculateAbsoluteError(imgOne, quantizedImg));
                break;
            case 2:
                quantizedImg = nonUniformQuantizeImage(imgOne, numberOfBuckets);
                System.out.println(calculateAbsoluteError(imgOne, quantizedImg));
                break;
        
            default:
                System.out.println("wrong mode selection");
                break;
        }


		// BufferedImage nonUni = nonUniformQuantizeImage(imgOne, numberOfBuckets);
        // DecimalFormat numberFormat = new DecimalFormat("#.00");

        // for(int i = 0; i<256; i++){
        //     int colors = (int)Math.pow(i+1, 3);
        //     BufferedImage m1 = quantizeImage(quantizationMode, imgOne, colors);
        //     // BufferedImage m2 = nonUniformQuantizeImage(imgOne, colors);
        //     System.out.println(calculateAbsoluteError(imgOne, m1));
        //     // System.out.println(colors+": non-uniform -> "+numberFormat.format(calculateAbsoluteError(imgOne, m2)));
        // }
        // System.out.println(calculateAbsoluteError(imgOne, nonUni));

    }

	public static void main(String[] args) {
		SampleAutomate ren = new SampleAutomate();
		ren.showIms(args);
	}

}