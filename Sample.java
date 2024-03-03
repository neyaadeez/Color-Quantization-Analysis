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
		// for(int i=0; i<histogram[0].length; i++){
		// 	System.out.println(i+": "+histogram[0][i]);
		// }
        // for(int i=0; i<histogram[1].length; i++){
		// 	System.out.println(i+": "+histogram[1][i]);
		// }
        // for(int i=0; i<histogram[2].length; i++){
		// 	System.out.println(i+": "+histogram[2][i]);
		// }

        // Determine bucket sizes and representative colors for each channel
        int[][] bucketSizesRepR = BucketSizesRep(histogram, 0, bucketSize);
        int[][] bucketSizesRepG = BucketSizesRep(histogram, 1, bucketSize);
        int[][] bucketSizesRepB = BucketSizesRep(histogram, 2, bucketSize);

        // Quantize each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Find the bucket index and representative color for each channel
                int bucketIndexR = findBucketIndex(r, bucketSizesRepR);
                int bucketIndexG = findBucketIndex(g, bucketSizesRepG);
                int bucketIndexB = findBucketIndex(b, bucketSizesRepB);
                

                // Combine channels
                int quantizedRGB = (r == 0 ? 0: (bucketSizesRepR[bucketIndexR][1] << 16)) |
                                   (g == 0 ? 0: (bucketSizesRepG[bucketIndexG][1] << 8)) |
                                   (b == 0 ? 0: (bucketSizesRepB[bucketIndexB][1]));
                

                quantizedImage.setRGB(x, y, quantizedRGB);
            }
        }

        return quantizedImage;
    }

	public static int[][] BucketSizesRep(int[][] histogram, int channel, int totalBuckets) {
        int[][] bucketSizesRep = new int[totalBuckets][2];
    
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
                bucketSizesRep[currentBucket][0] = i - (overshoot ? 1 : 0);
                bucketSizesRep[currentBucket][1] = representativeColor;
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
                bucketSizesRep[currentBucket][0] = 255 - (overshoot ? 1 : 0);
                bucketSizesRep[currentBucket][1] = currentSize == 0 ? 255 : currentBucketSum / currentSize;
                currentBucket++;
            }
        }
    
        return bucketSizesRep;
    }
    
    

	public static int findBucketIndex(int value, int[][] bucketSizesRep) {
		int index = 0;
		// Find the bucket index where the value falls
		while (index < bucketSizesRep.length && value > bucketSizesRep[index][0]) {
			index++;
		}
		// Adjust index to ensure it's within bounds
		return Math.min(index, bucketSizesRep.length - 1);
	}
	

    public static BufferedImage UniformQuantizeImage(BufferedImage originalImage, int totalBuckets) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage quantizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Calculate the number of buckets per channel
        int n = (int) Math.round(Math.pow(totalBuckets, 1.0 / 3.0));
        // System.out.println(totalBuckets+" : "+n);
        // int bucketSize = 256 / n;

        double[][] bucketSizesRep = calculateBucketSizesAndRepresentatives0(n);

        // Quantize each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int bucketIndexR = findBucketIndex0(r, bucketSizesRep);
                int bucketIndexG = findBucketIndex0(g, bucketSizesRep);
                int bucketIndexB = findBucketIndex0(b, bucketSizesRep);
    
                // Retrieve representative values from the bucketSizesRep array
                int quantizedR = (int)bucketSizesRep[bucketIndexR][1];
                int quantizedG = (int)bucketSizesRep[bucketIndexG][1];
                int quantizedB = (int)bucketSizesRep[bucketIndexB][1];
    
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
    
    public static int findBucketIndex0(int value, double[][] bucketSizesRep) {
        int index = 0;
        // Find the bucket index where the value falls
        while (index < bucketSizesRep.length && value > bucketSizesRep[index][0]) {
            index++;
        }
        // Adjust index to ensure it's within bounds
        return Math.min(index, bucketSizesRep.length - 1);
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

        System.out.println("errorR: "+errrorR);
        System.out.println("errorG: "+errrorG);
        System.out.println("errorB: "+errrorB);

        return totalError;
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

        BufferedImage quantizedImg;


		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;

        switch (quantizationMode) {
            case 1:
                quantizedImg = UniformQuantizeImage(imgOne, numberOfBuckets);
                lbIm1 = new JLabel(new ImageIcon(quantizedImg));
                frame.getContentPane().add(createHeaderPanel("Uniform Quantization", lbIm1), c);
                System.out.println("Total Error: "+calculateAbsoluteError(imgOne, quantizedImg));
                break;
            case 2:
                quantizedImg = nonUniformQuantizeImage(imgOne, numberOfBuckets);
                lbIm1 = new JLabel(new ImageIcon(quantizedImg));
                frame.getContentPane().add(createHeaderPanel("Non - Uniform Quantization", lbIm1), c);
                System.out.println("Total Error: "+calculateAbsoluteError(imgOne, quantizedImg));
                break;
    
            default:
                System.out.println("wrong mode selection");
                break;
        }
        
		frame.pack();
		frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
    private static JPanel createHeaderPanel(String headerText, JLabel contentLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel(headerText);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(contentLabel, BorderLayout.CENTER);
        return panel;
    }

	public static void main(String[] args) {
		Sample ren = new Sample();
		ren.showIms(args);
	}

}