import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class Sample {

	JFrame frame, frame2;
	JLabel lbIm1, lbIm2;
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
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage quantizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Iterate over each pixel and calculate the histogram for each channel
        int[][][] histogram = new int[256][256][256]; // Histogram for each channel

        // Calculate histogram
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
				histogram[r][0][0]++;
				histogram[0][g][0]++;
				histogram[0][0][b]++;
            }
        }
		// for(int i=0; i<256; i++){
		// 	System.out.println(i+": "+histogram[i][0][0]);
		// }

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
                int quantizedRGB = (bucketSizesAndRepresentativesR[bucketIndexR][1] << 16) |
                                   (bucketSizesAndRepresentativesG[bucketIndexG][1] << 8) |
                                   bucketSizesAndRepresentativesB[bucketIndexB][1];

                quantizedImage.setRGB(x, y, quantizedRGB);
            }
        }

        return quantizedImage;
    }

	public static int[][] calculateBucketSizesAndRepresentatives(int[][][] histogram, int channel, int totalBuckets) {
        int[][] bucketSizesAndRepresentatives = new int[totalBuckets][2];

        // Calculate total pixel count for the given channel
        int totalPixels = 0;
        // for (int r = 0; r < 256; r++) {
        //     for (int g = 0; g < 256; g++) {
        //         for (int b = 0; b < 256; b++) {
        //             totalPixels += histogram[r][g][b];
        //         }
        //     }
        // }
		for(int i=0; i<256; i++){
			totalPixels += histogram[channel == 0 ? i : 0][channel == 1 ? i : 0][channel == 2 ? i : 0];
		}
		System.out.println("totalPix: "+ totalPixels);
		System.out.println("totalBuckets: "+ totalBuckets);

        // Calculate bucket sizes and representative colors based on pixel distribution in the given channel
        int pixelsInBucket = 0;
        int bucketIndex = 0;
        int bucketSum = 0;
        for (int i = 0; i < 256; i++) {
            pixelsInBucket += histogram[channel == 0 ? i : 0][channel == 1 ? i : 0][channel == 2 ? i : 0];
            if ((pixelsInBucket > totalPixels / totalBuckets)) {
				System.out.println("pixbuck: "+pixelsInBucket);
                bucketSizesAndRepresentatives[bucketIndex][0] = i; // Bucket size
                bucketSizesAndRepresentatives[bucketIndex][1] = bucketSum / pixelsInBucket; // Representative color
                bucketIndex++;
                pixelsInBucket = 0;
                bucketSum = 0;
            }
            bucketSum += (histogram[channel == 0 ? i : 0][channel == 1 ? i : 0][channel == 2 ? i : 0] * i);
        }

        return bucketSizesAndRepresentatives;
    }

	public static int findBucketIndex(int value, int[][] bucketSizesAndRepresentatives) {
		int index = 0;
		// Find the bucket index where the value falls
		while (index < bucketSizesAndRepresentatives.length && value >= bucketSizesAndRepresentatives[index][0]) {
			index++;
		}
		// Adjust index to ensure it's within bounds
		return Math.min(index, bucketSizesAndRepresentatives.length - 1);
	}
	

    public static BufferedImage quantizeImage(int quantizationMode, BufferedImage originalImage, int totalBuckets) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage quantizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Calculate the number of buckets per channel
        int n = (int) Math.round(Math.pow(totalBuckets, 1.0 / 3.0));
        int bucketSize = 256 / n;

        // Quantize each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Quantize each channel
                int quantizedR = (r / bucketSize) * bucketSize + bucketSize / 2;
                int quantizedG = (g / bucketSize) * bucketSize + bucketSize / 2;
                int quantizedB = (b / bucketSize) * bucketSize + bucketSize / 2;

                // Combine channels
                int quantizedRGB = (quantizedR << 16) | (quantizedG << 8) | quantizedB;

                quantizedImage.setRGB(x, y, quantizedRGB);
            }
        }

        return quantizedImage;
    }

    public static double calculateAbsoluteError(BufferedImage originalImage, BufferedImage quantizedImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        double totalError = 0;

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
                double errorR = Math.abs(originalR - quantizedR);
                double errorG = Math.abs(originalG - quantizedG);
                double errorB = Math.abs(originalB - quantizedB);

                // Calculate total absolute error
                totalError += errorR + errorG + errorB;
            }
        }

        // Normalize absolute error by the number of pixels and channels
        return totalError / (width * height * 3);
    }

	public void showIms(String[] args){

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

		// Use label to display the image
		frame = new JFrame();
		frame2 = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		frame2.getContentPane().setLayout(gLayout);

        int quantizationMode = Integer.parseInt(args[1]);
        int numberOfBuckets = Integer.parseInt(args[2]);
        BufferedImage quantizedImg = quantizeImage(quantizationMode, imgOne, numberOfBuckets);

		BufferedImage nonUni = nonUniformQuantizeImage(imgOne, numberOfBuckets);
		System.out.println("Uniform: "+calculateAbsoluteError(imgOne, quantizedImg));
        System.out.println("non-Uniform: "+calculateAbsoluteError(imgOne, nonUni));

		lbIm1 = new JLabel(new ImageIcon(quantizedImg));
		lbIm2 = new JLabel(new ImageIcon(nonUni));


		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame2.getContentPane().add(lbIm2, c);

		frame2.pack();
		frame2.setVisible(true);
        frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		Sample ren = new Sample();
		ren.showIms(args);
	}

}