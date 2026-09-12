package com.justnothing.richconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.box.Box;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.console.Group;
import com.justnothing.richconsole.markdown.Markdown;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.pretty.Pretty;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.syntax.Syntax;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.text.Text;

public class TestCardDemoTest {

    public static void main(String[] args) {
        Console console = Console.of(cfg -> cfg
                .withWidth(80)
                .withForceTerminal(true)
                .withColorSystem("auto"));

        Table card = Table.grid(1, true);
        card.setTitle("RichConsole features");
        card.addColumn("Feature", "bold red", "center").setNoWrap(true);
        card.addColumn("Demonstration");

        // Colors
        Table colorTable = Table.of(cfg -> cfg.box(null).expand(false).showHeader(false).showEdge(false).padEdge(false));
        colorTable.addColumn("colors", null, null);
        colorTable.addColumn("box", null, null);
        colorTable.addRow(
                "[bold green]✓ 4-bit color[/]\n[bold blue]✓ 8-bit color[/]\n[bold magenta]✓ Truecolor (16.7 million)[/]\n[bold yellow]✓ Dumb terminals[/]\n[bold cyan]✓ Automatic color conversion",
                new ColorBox());
        card.addRow("Colors", new Group(Arrays.asList(colorTable, new Text("\n"))));

        card.addRow("Styles\n",
                "All ANSI styles: [bold]bold[/], [dim]dim[/], [italic]italic[/], [underline]underline[/], [strike]strikethrough[/], [reverse]reverse[/], and even [blink]blink[/].");

        String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque in metus sed sapien ultricies pretium a at justo. Maecenas luctus velit et auctor maximus.";
        Table loremGrid = Table.grid(1);
        Text leftText = Text.of(lorem, cfg -> cfg.style("green").justify("left").end(""));
        Text centerText = Text.of(lorem, cfg -> cfg.style("yellow").justify("center").end(""));
        Text rightText = Text.of(lorem, cfg -> cfg.style("blue").justify("right").end(""));
        Text fullText = Text.of(lorem, cfg -> cfg.style("red").justify("full").end(""));
        loremGrid.addRow(leftText, centerText, rightText, fullText);
        card.addRow("Text\n", new Group(Arrays.asList(
                Text.fromMarkup("Word wrap text. Justify [green]left[/], [yellow]center[/], [blue]right[/] or [red]full[/].\n"),
                loremGrid,
                new Text("\n"))));

        console.print(card);
    }

    private static class ColorBox implements RichRenderable {
        @Override
        public Iterable<?> richConsole(Console console, ConsoleOptions options) {
            List<Segment> segments = new ArrayList<>();
            int width = options.getMaxWidth();
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < width; x++) {
                    double h = (double) x / width;
                    double l = 0.1 + ((double) y / 5) * 0.7;
                    double l2 = l + 0.7 / 10;
                    int[] rgb1 = hslToRgb(h, l, 1.0);
                    int[] rgb2 = hslToRgb(h, l2, 1.0);
                    String bgcolor = String.format("#%02x%02x%02x", rgb1[0], rgb1[1], rgb1[2]);
                    String color = String.format("#%02x%02x%02x", rgb2[0], rgb2[1], rgb2[2]);
                    segments.add(new Segment("\u2584", Style.parse(color + " on " + bgcolor)));
                }
                segments.add(Segment.line());
            }
            return segments;
        }

        @Override
        public Measurement richMeasure(Console console, ConsoleOptions options) {
            return new Measurement(8, options.getMaxWidth());
        }
    }

    private static int[] hslToRgb(double h, double l, double s) {
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs((h * 6) % 2 - 1));
        double m = l - c / 2;
        double r = 0, g = 0, b = 0;
        int i = (int) (h * 6);
        switch (i % 6) {
            case 0:
                r = c;
                g = x;
                b = 0;
                break;
            case 1:
                r = x;
                g = c;
                b = 0;
                break;
            case 2:
                r = 0;
                g = c;
                b = x;
                break;
            case 3:
                r = 0;
                g = x;
                b = c;
                break;
            case 4:
                r = x;
                g = 0;
                b = c;
                break;
            case 5:
                r = c;
                g = 0;
                b = x;
                break;
        }
        return new int[] {
                (int) Math.round((r + m) * 255),
                (int) Math.round((g + m) * 255),
                (int) Math.round((b + m) * 255)
        };
    }
}