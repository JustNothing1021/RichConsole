package com.justnothing.richconsole.color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A palette of available colors.
 * Ported from rich/palette.py.
 */
public class Palette {

    private final List<int[]> colors;
    private final Map<Integer, Integer> matchCache = new HashMap<>();

    public Palette(List<int[]> colors) {
        this.colors = colors;
    }

    /**
     * Get a color triplet by index.
     */
    public ColorTriplet get(int index) {
        int[] rgb = colors.get(index);
        return new ColorTriplet(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Get the number of colors in this palette.
     */
    public int size() {
        return colors.size();
    }

    /**
     * Find a color from a palette that most closely matches a given color,
     * using weighted Euclidean distance.
     *
     * @param color the RGB triplet to match
     * @return index of closest matching color
     */
    public int match(ColorTriplet color) {
        return match(color.red(), color.green(), color.blue());
    }

    /**
     * Find a color from a palette that most closely matches a given color,
     * using weighted Euclidean distance (same algorithm as Python version).
     *
     * @param red   red component (0-255)
     * @param green green component (0-255)
     * @param blue  blue component (0-255)
     * @return index of closest matching color
     */
    public int match(int red, int green, int blue) {
        // Cache key: pack RGB into a single int
        int cacheKey = (red << 16) | (green << 8) | blue;
        Integer cached = matchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        int minIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < colors.size(); i++) {
            int[] entry = colors.get(i);
            int red2 = entry[0];
            int green2 = entry[1];
            int blue2 = entry[2];

            int redMean = (red + red2) >> 1;
            int dr = red - red2;
            int dg = green - green2;
            int db = blue - blue2;

            double distance = Math.sqrt(
                (((512 + redMean) * (long) dr * dr) >> 8)
                    + 4L * dg * dg
                    + (((767 - redMean) * (long) db * db) >> 8)
            );

            if (distance < minDistance) {
                minDistance = distance;
                minIndex = i;
            }
        }

        matchCache.put(cacheKey, minIndex);
        return minIndex;
    }
}
