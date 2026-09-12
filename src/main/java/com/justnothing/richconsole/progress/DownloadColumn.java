package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.text.Text;

/**
 * Renders file size downloaded and total, e.g. '0.5/2.3 GB'.
 * Ported from rich/progress.py DownloadColumn.
 */
public class DownloadColumn extends ProgressColumn {

    private static final String[] DECIMAL_SUFFIXES =
            {"bytes", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    private static final String[] BINARY_SUFFIXES =
            {"bytes", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"};

    private final boolean binaryUnits;

    public DownloadColumn() {
        this(false);
    }

    /**
     * @param binaryUnits use binary units (KiB, MiB, ...) instead of decimal ones
     */
    public DownloadColumn(boolean binaryUnits) {
        this.binaryUnits = binaryUnits;
    }

    @Override
    public Object render(Progress.Task task) {
        long completed = (long) task.getCompleted();
        Double total = task.getTotal();
        long base = total != null ? (long) (double) total : completed;

        ProgressFormats.Unit unit = ProgressFormats.pickUnitAndSuffix(
                base, binaryUnits ? BINARY_SUFFIXES : DECIMAL_SUFFIXES,
                binaryUnits ? 1024 : 1000);
        int precision = unit.unit() == 1 ? 0 : 1;

        String completedStr = String.format("%,." + precision + "f", (double) completed / unit.unit());
        String totalStr;
        if (total != null) {
            totalStr = String.format("%,." + precision + "f", (double) (long) (double) total / unit.unit());
        } else {
            totalStr = "?";
        }

        String status = completedStr + "/" + totalStr + " " + unit.suffix();
        return new Text(status, "progress.download");
    }
}
