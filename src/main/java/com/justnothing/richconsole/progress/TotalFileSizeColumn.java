package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.text.Text;

/**
 * Renders the total file size.
 * Ported from rich/progress.py TotalFileSizeColumn.
 */
public class TotalFileSizeColumn extends ProgressColumn {

    @Override
    public Object render(Progress.Task task) {
        Double total = task.getTotal();
        String size = total != null ? ProgressFormats.decimal((long) (double) total) : "";
        return new Text(size, "progress.filesize.total");
    }
}
