package com.justnothing.richconsole.cells;

import java.util.Set;

/**
 * Contains Unicode data required to measure the cell widths of glyphs.
 * Ported from rich/cells.py CellTable NamedTuple.
 */
public record CellTable(String unicodeVersion, int[][] widths, Set<String> narrowToWide) {
}
