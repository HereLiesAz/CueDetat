# 35. The Parable of the Hybrid Eye

In the beginning, the Eye was singular. It was the generic object detector from the tribe of ML Kit. It was good at seeing "things" but knew not what a "ball" was. It saw squares where there were circles and was easily swayed by the false prophets of light and shadow.

This was not The Way.

A new doctrine was formed: The Hybrid Eye. It is a faith of two stages, known as the Scout and the Sniper.

### The Scout: ML Kit
The first stage remains the ML Kit Object Detector. Its purpose is not to find the *truth*, but to find the *area where the truth might lie*. It scans the entire frame and returns coarse bounding boxes around potential candidates. It is fast, but imprecise. It is the **Scout**.

### The Sniper: OpenCV
For each bounding box the Scout provides, the **Sniper** is deployed. The Sniper is a set of focused OpenCV functions that operate *only* within that small region of interest. This conserves the sacred resource of computation.

The Sniper has two rites of refinement:

1.  **The Rite of Hough:** This uses `Imgproc.HoughCircles`. It is effective on well-defined circles but can be misled by the profane geometry of ellipses caused by perspective.
2.  **The Rite of Contour:** This is the default and holier rite. It finds all contours in the region, fits a minimum enclosing circle to them, and then judges them based on their **circularity**. This rite is far more tolerant of perspective distortion.

To aid the Sniper, its aim is guided by a **Dynamic Rangefinder**. The system calculates the expected on-screen pixel radius of a ball at the Y-coordinate of the Scout's bounding box. This provides the Sniper with a very tight `minRadius` and `maxRadius`, drastically reducing false positives.

The color mask used by these rites is now derived from a **Statistical Sacrament**. Instead of a single pixel, a 5x5 patch of the felt is sampled. The system calculates the **mean and standard deviation** of the HSV values, creating a mask that is adaptive and resilient to the chaos of real-world lighting. This is The Way.