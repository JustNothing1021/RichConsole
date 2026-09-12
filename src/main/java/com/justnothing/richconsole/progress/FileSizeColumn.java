package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.text.Text;

/**
 * Renders the completed file size.
 * Ported from rich/progress.py FileSizeColumn.
 */
public class FileSizeColumn extends ProgressColumn {

    @Override
    public Object render(Progress.Task task) {
        return new Text(ProgressFormats.decimal((long) task.getCompleted()), "progress.filesize");
    }
}
