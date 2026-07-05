package com.justnothing.richconsole.demo;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.box.Box;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.console.Group;
import com.justnothing.richconsole.markdown.Markdown;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.pretty.Pretty;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.syntax.Syntax;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test Card — comprehensive feature showcase, similar to Python rich's {@code python -m rich} output.
 */
public class TestCardDemo {

    // =========================================================================
    // ColorBox — HSL color gradient renderer
    // =========================================================================

    private static class ColorBox implements RichRenderable {
        @Override
        public Iterable<?> richConsole(Console console, ConsoleOptions options) {
            List<Segment> segments = new ArrayList<>();
            int maxWidth = options.getMaxWidth();

            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < maxWidth; x++) {
                    double h = (double) x / maxWidth;
                    double l = 0.1 + ((double) y / 5) * 0.7;
                    double l2 = l + 0.7 / 10;

                    int[] rgb1 = hslToRgb(h, l, 1.0);
                    int[] rgb2 = hslToRgb(h, l2, 1.0);

                    String bgcolor = String.format("#%02x%02x%02x", rgb1[0], rgb1[1], rgb1[2]);
                    String color = String.format("#%02x%02x%02x", rgb2[0], rgb2[1], rgb2[2]);

                    segments.add(new Segment("\u2584",
                            Style.parse(color + " on " + bgcolor)));
                }
                segments.add(Segment.line());
            }
            return segments;
        }
    }

    /** Convert HSL to RGB. h in [0,1], l in [0,1], s in [0,1]. Returns [r,g,b] in 0-255. */
    private static int[] hslToRgb(double h, double l, double s) {
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs((h * 6) % 2 - 1));
        double m = l - c / 2;
        double r, g, b;
        int sector = (int) (h * 6) % 6;
        switch (sector) {
            case 0:  r = c; g = x; b = 0; break;
            case 1:  r = x; g = c; b = 0; break;
            case 2:  r = 0; g = c; b = x; break;
            case 3:  r = 0; g = x; b = c; break;
            case 4:  r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return new int[]{
                (int) Math.round((r + m) * 255),
                (int) Math.round((g + m) * 255),
                (int) Math.round((b + m) * 255)
        };
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Table comparison(RichRenderable r1, RichRenderable r2) {
        Table table = Table.of(cfg -> cfg.showHeader(false).padEdge(false).box(null).expand(true));
        table.addColumn("1", null, null).setRatio(1.0);
        table.addColumn("2", null, null).setRatio(1.0);
        table.addRow(r1, r2);
        return table;
    }

    // =========================================================================
    // Main
    // =========================================================================

    public static void main(String[] args) {
        Console console = Console.of(cfg -> {});

        Table card = Table.grid(1, true);
        card.setTitle("RichConsole features");
        card.addColumn("Feature", "bold red", "center").setNoWrap(true);
        card.addColumn("Demonstration");

        // ── Colors ──
        Table colorTable = Table.of(cfg -> cfg.box(null).expand(false).showHeader(false).showEdge(false).padEdge(false));
        colorTable.addColumn("colors", null, null);
        colorTable.addColumn("box", null, null);
        colorTable.addRow(
                "[bold green]\u2713 4-bit color[/]\n[bold blue]\u2713 8-bit color[/]\n[bold magenta]\u2713 Truecolor (16.7 million)[/]\n[bold yellow]\u2713 Dumb terminals[/]\n[bold cyan]\u2713 Automatic color conversion",
                new ColorBox());
        card.addRow("Colors", colorTable);

        // ── Styles ──
        card.addRow("Styles",
                "All ANSI styles: [bold]bold[/], [dim]dim[/], [italic]italic[/], [underline]underline[/], [strike]strikethrough[/], [reverse]reverse[/], and even [blink]blink[/].");

        // ── Text ──
        String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque in metus sed sapien ultricies pretium a at justo. Maecenas luctus velit et auctor maximus.";
        Table loremGrid = Table.grid(1);
        loremGrid.setPadEdge(false);
        Text leftText = Text.of(lorem, cfg -> cfg.style("green").justify("left").end(""));
        Text centerText = Text.of(lorem, cfg -> cfg.style("yellow").justify("center").end(""));
        Text rightText = Text.of(lorem, cfg -> cfg.style("blue").justify("right").end(""));
        Text fullText = Text.of(lorem, cfg -> cfg.style("red").justify("full").end(""));
        loremGrid.addRow(leftText, centerText, rightText, fullText);
        card.addRow("Text", new Group(java.util.Arrays.asList(
                Text.fromMarkup("Word wrap text. Justify [green]left[/], [yellow]center[/], [blue]right[/] or [red]full[/].\n"),
                loremGrid)));

        // ── CJK ──
        card.addRow("Asian\nlanguage\nsupport",
                "\uD83C\uDDE8\uD83C\uDDF3  该库支持中文，日文和韩文文本！\n\uD83C\uDDEF\uD83C\uDDF5  ライブラリは中国語、日本語、韓国語のテキストをサポートしています\n\uD83C\uDDF0\uD83C\uDDF7  이 라이브러리는 중국어, 일본어 및 한국어 텍스트를 지원합니다");

        // ── Markup ──
        card.addRow("Markup",
                "[bold magenta]RichConsole[/] supports a simple [i]bbcode[/i]-like [b]markup[/b] for [yellow]color[/], [underline]style[/], and emoji!");

        // ── Tables ──
        Table movieTable = Table.of(cfg -> cfg
                .box(Box.SIMPLE).showHeader(true).expand(false).showEdge(false));
        movieTable.addColumn("[green]Date[/]", "green", null).setNoWrap(true);
        movieTable.addColumn("[blue]Title[/]", "blue", null);
        movieTable.addColumn("[cyan]Production Budget[/]", "cyan", "right").setNoWrap(true);
        movieTable.addColumn("[magenta]Box Office[/]", "magenta", "right").setNoWrap(true);
        movieTable.addRow("Dec 20, 2019", "Star Wars: The Rise of Skywalker", "$275,000,000", "$375,126,118");
        movieTable.addRow("May 25, 2018", "[b]Solo[/]: A Star Wars Story", "$275,000,000", "$393,151,347");
        movieTable.addRow("Dec 15, 2017", "Star Wars Ep. VIII: The Last Jedi", "$262,000,000", "[bold]$1,332,539,889[/bold]");
        movieTable.addRow("May 19, 1999", "Star Wars Ep. [b]I[/b]: [i]The phantom Menace", "$115,000,000", "$1,027,044,677");
        card.addRow("Tables", movieTable);

        // ── Syntax & Pretty ──
        String code = """
                def iter_last(values: Iterable[T]) -> Iterable[Tuple[bool, T]]:
                    \"""Iterate and generate a tuple with a flag for last value.\"""
                    iter_values = iter(values)
                    try:
                        previous_value = next(iter_values)
                    except StopIteration:
                        return
                    for value in iter_values:
                        yield False, previous_value
                        previous_value = value
                    yield True, previous_value""";
        Map<String, Object> prettyData = new LinkedHashMap<>();
        prettyData.put("foo", java.util.Arrays.asList(3.1427, java.util.Arrays.asList("Paul Atreides", "Vladimir Harkonnen", "Thufir Hawat")));
        prettyData.put("atomic", Arrays.asList(false, true, null));

        card.addRow("Syntax\nhighlighting\n&\npretty\nprinting",
                comparison(
                        Syntax.of(code, cfg -> cfg.lexerName("python").lineNumbers(true).startLine(1)),
                        new Pretty(prettyData, true, true)));

        // ── Markdown ──
        String mdExample = """
                # Markdown

                Supports much of the *markdown* __syntax__!

                - Headers
                - Basic formatting: **bold**, *italic*, `code`
                - Block quotes
                - Lists, and more...
                """;
        card.addRow("Markdown", comparison(
                Text.fromMarkup("[cyan]" + mdExample),
                new Markdown(mdExample)));

        // ── more ──
        card.addRow("+more!",
                "Progress bars, columns, styled logging handler, tracebacks, etc...");

        console.println(card);
        console.println();
        console.println(Panel.of(
                """
                [b magenta]Hope you enjoy using RichConsole![/]
                
                This is a Java port of Python's Rich library.
                
                [cyan]https://github.com/JustNothing1021/RichConsole[/cyan]
                """,
                cfg -> cfg.borderStyle("green").title("RichConsole").padding(1, 2)));
    }
}
