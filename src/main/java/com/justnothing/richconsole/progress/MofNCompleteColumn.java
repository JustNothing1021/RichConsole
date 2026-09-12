package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.text.Text;

/**
 * Renders the completed count / total, e.g. '  10/1000'.
 * Ported from rich/progress.py MofNCompleteColumn.
 */
public class MofNCompleteColumn extends ProgressColumn {

    private final String separator;

    public MofNCompleteColumn() {
        this("/");
    }

    public MofNCompleteColumn(String separator) {
        this.separator = separator;
    }

    @Override
    public Object render(Progress.Task task) {
        long completed = (long) task.getCompleted();
        Double total = task.getTotal();
        String totalStr = total != null ? String.valueOf((long) (double) total) : "?";
        String formatted = String.format("%" + totalStr.length() + "d%s%s", completed, separator, totalStr);
        return new Text(formatted, "progress.download");
    }
}
