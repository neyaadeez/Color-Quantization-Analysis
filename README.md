# Color-Quantization-Analysis

This repository contains a program to perform color quantization on RGB images. The program supports two quantization modes: uniform quantization (mode 1) and non-uniform quantization (mode 2).

## How to Use

To use the program, follow these steps:

1. Compile the program.
2. Run the compiled program on the command line with the following parameters:

```
YourProgram.exe Image.rgb Q B
```

- `Image.rgb`: Path to the input image file in 8 bits per channel RGB format (24 bits per pixel).
- `Q`: Quantization mode. Possible values are 1, 2, or 3.
- `B`: Total number of buckets, a cubic value in the range [2^3, 255^3].

## Example Usage

1. For uniform quantization with 8 colors:
```
YourProgram.exe Image.rgb 1 8
```

2. For non-uniform quantization with 27 colors:
```
YourProgram.exe Image.rgb 2 27
```

## Implementation Details

- **Mode 1 (Uniform Quantization):** Divides each channel into equally spaced square root of B buckets, where B is the total number of buckets provided. The mid-point of each range is taken as the representative color for all pixels in that bucket.

- **Mode 2 (Non-Uniform Quantization):** Divides each channel into square root of B buckets, where bucket spacing is dynamically assigned based on the distribution of pixel values in each channel. Smaller ranged buckets are assigned to values that occur more often to reduce overall error. The representative color for each pixel is set as the average value of each color in the bucket it belongs to.

## Note
- The provided image is assumed to be of size 512x512.
- All parameters are expected to have reasonable values within specified ranges.

## Credits

This program is developed as part of an assignment for [Professor Parag Havaldar's](https://viterbi.usc.edu/directory/faculty/Havaldar/Parag) course. The assignment specifications and guidelines were provided by Professor Parag Havaldar.
