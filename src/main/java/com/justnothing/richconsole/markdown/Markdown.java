package com.justnothing.richconsole.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.box.Box;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.containers.Renderables;
import com.justnothing.richconsole.rule.Rule;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.style.StyleStack;
import com.justnothing.richconsole.syntax.Syntax;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.text.Text;

/**
 * A Markdown renderable.
 * Ported from rich/markdown.py Markdown class.
 *
 * <p>Parses Markdown text using commonmark-java and renders it
 * with Rich styles, matching the Python rich library's output.</p>
 *
 * <p>Note: Since both {@code org.commonmark.node.Text} and
 * {@code text.com.justnothing.richconsole.Text} share the same simple name,
 * the commonmark Text node is referenced by its fully qualified name
 * to avoid the conflict.</p>
 */
public class Markdown implements RichRenderable {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final String[] LEVEL_ALIGN = {
            null, "center", "left", "left", "left", "left", "left"
    };

    private static final String DEFAULT_CODE_THEME = "monokai";
    private static final String DEFAULT_STYLE = "none";

    // =========================================================================
    // Instance fields
    // =========================================================================

    private final String markup;
    private final String codeTheme;
    private final String justify;
    private final Object style;
    private final boolean hyperlinks;
    private final Node parsed;

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Fluent configuration object for Markdown construction.
     * Usage: {@code Markdown.of("# Hello", cfg -> cfg.codeTheme("github-dark").hyperlinks(false))}
     */
    public static class Config {
        public String codeTheme = DEFAULT_CODE_THEME;
        public String justify;
        public Object style = DEFAULT_STYLE;
        public boolean hyperlinks = true;

        public Config codeTheme(String codeTheme) { this.codeTheme = codeTheme; return this; }
        public Config justify(String justify) { this.justify = justify; return this; }
        public Config style(Object style) { this.style = style; return this; }
        public Config hyperlinks(boolean hyperlinks) { this.hyperlinks = hyperlinks; return this; }
    }

    // =========================================================================
    // Factory method
    // =========================================================================

    /**
     * Create a Markdown with fluent configuration.
     * <pre>{@code
     * Markdown.of("# Hello", cfg -> cfg.codeTheme("github-dark").hyperlinks(false))
     * }</pre>
     *
     * @param markup      the Markdown text (required)
     * @param configurer  a consumer that configures the Markdown options
     * @return a new Markdown instance
     */
    public static Markdown of(String markup, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new Markdown(markup, cfg);
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    private Markdown(String markup, Config cfg) {
        this(markup, cfg.codeTheme, cfg.justify, cfg.style, cfg.hyperlinks);
    }

    public Markdown(String markup, String codeTheme, String justify,
                    Object style, boolean hyperlinks) {
        this.markup = markup;
        this.codeTheme = codeTheme != null ? codeTheme : DEFAULT_CODE_THEME;
        this.justify = justify;
        this.style = style;
        this.hyperlinks = hyperlinks;

        List<Extension> extensions = new ArrayList<>();
        extensions.add(TablesExtension.create());
        extensions.add(StrikethroughExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        this.parsed = parser.parse(markup != null ? markup : "");
    }

    public Markdown(String markup) {
        this(markup, DEFAULT_CODE_THEME, null, DEFAULT_STYLE, true);
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    public Iterable<Segment> richConsole(Console console, ConsoleOptions options) {
        Style baseStyle = console.getStyle(this.style != null ? this.style : DEFAULT_STYLE);
        MarkdownContext context = new MarkdownContext(console, options, baseStyle);

        List<Segment> result = new ArrayList<>();
        boolean newLine = false;

        Node child = parsed.getFirstChild();
        while (child != null) {
            List<Segment> rendered = renderBlockNode(child, context);
            if (rendered != null && !rendered.isEmpty()) {
                if (newLine) {
                    result.add(Segment.line());
                }
                result.addAll(rendered);
                newLine = true;
            }
            child = child.getNext();
        }

        return result;
    }

    // =========================================================================
    // Block-level rendering
    // =========================================================================

    private List<Segment> renderBlockNode(Node node, MarkdownContext context) {
        if (node instanceof Heading) {
            return renderHeading((Heading) node, context);
        } else if (node instanceof Paragraph) {
            return renderParagraph((Paragraph) node, context);
        } else if (node instanceof FencedCodeBlock) {
            return renderCodeBlock((FencedCodeBlock) node, context);
        } else if (node instanceof IndentedCodeBlock) {
            return renderIndentedCodeBlock((IndentedCodeBlock) node, context);
        } else if (node instanceof BlockQuote) {
            return renderBlockQuote((BlockQuote) node, context);
        } else if (node instanceof ThematicBreak) {
            return renderHorizontalRule(context);
        } else if (node instanceof BulletList) {
            return renderBulletList((BulletList) node, context);
        } else if (node instanceof OrderedList) {
            return renderOrderedList((OrderedList) node, context);
        } else if (node instanceof TableBlock) {
            return renderTable((TableBlock) node, context);
        }
        return renderUnknownBlock(node, context);
    }

    // ---- Heading ----

    private List<Segment> renderHeading(Heading heading, MarkdownContext context) {
        int level = heading.getLevel();
        String tag = "h" + level;
        String styleName = "markdown." + tag;
        Style headingStyle = context.console.getStyle(styleName);
        String headingJustify = level >= 1 && level <= 6 ? LEVEL_ALIGN[level] : "left";

        Text text = collectInlineText(heading, context);
        text.setJustify(headingJustify);
        text.stylize(headingStyle);
        text.setEnd(""); // Block spacing is handled by richConsole()

        return toSegmentList(text.richConsole(context.console, context.options));
    }

    // ---- Paragraph ----

    private List<Segment> renderParagraph(Paragraph para, MarkdownContext context) {
        String paraJustify = this.justify != null ? this.justify : "left";
        Style paraStyle = context.console.getStyle("markdown.paragraph");

        Text text = collectInlineText(para, context);
        text.setJustify(paraJustify);
        text.setEnd(""); // Block spacing is handled by richConsole()
        if (paraStyle != null) {
            text.stylize(paraStyle);
        }

        return toSegmentList(text.richConsole(context.console, context.options));
    }

    // ---- Code Block ----

    private List<Segment> renderCodeBlock(FencedCodeBlock codeBlock, MarkdownContext context) {
        String code = codeBlock.getLiteral();
        String info = codeBlock.getInfo();
        String lexerName = (info != null && !info.isEmpty()) ? info.split(" ", 2)[0] : "text";

        Syntax syntax = new Syntax(
                code != null ? trimTrailing(code) : "",
                lexerName, codeTheme, true, 1, null, null, 4, true, null, 1);
        return toSegmentList(syntax.richConsole(context.console, context.options));
    }

    private List<Segment> renderIndentedCodeBlock(IndentedCodeBlock codeBlock, MarkdownContext context) {
        String code = codeBlock.getLiteral();

        Syntax syntax = new Syntax(
                code != null ? trimTrailing(code) : "",
                "text", codeTheme, true, 1, null, null, 4, true, null, 1);
        return toSegmentList(syntax.richConsole(context.console, context.options));
    }

    // ---- Block Quote ----

    private List<Segment> renderBlockQuote(BlockQuote blockQuote, MarkdownContext context) {
        Style quoteStyle = context.console.getStyle("markdown.block_quote");
        ConsoleOptions innerOptions = context.options.updateWidth(
                context.options.getMaxWidth() - 4);

        Renderables elements = new Renderables();
        Node child = blockQuote.getFirstChild();
        while (child != null) {
            List<Segment> rendered = renderBlockNode(child, context.withOptions(innerOptions));
            if (rendered != null) {
                elements.add(new SegmentList(rendered));
            }
            child = child.getNext();
        }

        List<List<Segment>> lines = context.console.renderLines(
                elements, innerOptions, quoteStyle, true, false);
        Segment padding = new Segment("| ", quoteStyle);

        List<Segment> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            result.add(padding);
            result.addAll(lines.get(i));
            if (i < lines.size() - 1) {
                result.add(Segment.line());
            }
        }
        return result;
    }

    // ---- Horizontal Rule ----

    private List<Segment> renderHorizontalRule(MarkdownContext context) {
        Style hrStyle = context.console.getStyle("markdown.hr");
        Rule rule = new Rule(null, "-", hrStyle, "center");
        List<Segment> result = new ArrayList<>();
        for (Object seg : rule.richConsole(context.console, context.options)) {
            if (seg instanceof Segment s) {
                result.add(s);
            }
        }
        return result;
    }

    // ---- Bullet List ----

    private List<Segment> renderBulletList(BulletList list, MarkdownContext context) {
        Style bulletStyle = context.console.getStyle("markdown.item.bullet");

        List<Segment> result = new ArrayList<>();
        Node child = list.getFirstChild();
        boolean first = true;
        while (child != null) {
            if (child instanceof ListItem) {
                if (!first) {
                    result.add(Segment.line());
                }
                result.addAll(renderListItem(
                        (ListItem) child, context, bulletStyle, null, null));
                first = false;
            }
            child = child.getNext();
        }
        return result;
    }

    // ---- Ordered List ----

    private List<Segment> renderOrderedList(OrderedList list, MarkdownContext context) {
        Style numberStyle = context.console.getStyle("markdown.item.number");
        int startNumber = list.getStartNumber();

        int itemCount = 0;
        Node child = list.getFirstChild();
        while (child != null) {
            if (child instanceof ListItem) itemCount++;
            child = child.getNext();
        }
        int lastNumber = startNumber + itemCount - 1;
        int numberWidth = String.valueOf(lastNumber).length() + 2;

        List<Segment> result = new ArrayList<>();
        child = list.getFirstChild();
        int number = startNumber;
        boolean first = true;
        while (child != null) {
            if (child instanceof ListItem) {
                if (!first) {
                    result.add(Segment.line());
                }
                result.addAll(renderListItem(
                        (ListItem) child, context, numberStyle, number, numberWidth));
                number++;
                first = false;
            }
            child = child.getNext();
        }
        return result;
    }

    // ---- List Item ----

    private List<Segment> renderListItem(ListItem item, MarkdownContext context,
                                          Style prefixStyle, Integer number, Integer numberWidth) {
        boolean isBullet = (number == null);
        int indentWidth = isBullet ? 3 : numberWidth;
        ConsoleOptions innerOptions = context.options.updateWidth(
                context.options.getMaxWidth() - indentWidth);

        Renderables elements = new Renderables();
        Node child = item.getFirstChild();
        while (child != null) {
            List<Segment> rendered = renderBlockNode(child, context.withOptions(innerOptions));
            if (rendered != null) {
                elements.add(new SegmentList(rendered));
            }
            child = child.getNext();
        }

        List<List<Segment>> lines = context.console.renderLines(
                elements, innerOptions, null, true, false);
        Segment newLineSeg = Segment.line();

        Segment bullet = isBullet
                ? new Segment(" - ", prefixStyle)
                : new Segment(String.format("%" + (numberWidth - 1) + "d ", number), prefixStyle);
        Segment indent = new Segment(" ".repeat(indentWidth), prefixStyle);

        List<Segment> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            result.add(i == 0 ? bullet : indent);
            result.addAll(lines.get(i));
            if (i < lines.size() - 1) {
                result.add(newLineSeg);
            }
        }
        return result;
    }

    // ---- Table (GFM) ----

    private List<Segment> renderTable(TableBlock tableBlock, MarkdownContext context) {
        // Walk the AST manually: TableBlock → TableHead/TableBody → TableRow → TableCell
        List<Text> headerCells = new ArrayList<>();
        List<List<Text>> bodyRows = new ArrayList<>();

        Node tableChild = tableBlock.getFirstChild();
        while (tableChild != null) {
            if (tableChild instanceof TableHead) {
                extractTableRows(tableChild, headerCells, null);
            } else if (tableChild instanceof TableBody) {
                extractTableRows(tableChild, null, bodyRows);
            }
            tableChild = tableChild.getNext();
        }

        Table table = new Table();
        table.setBox(Box.SIMPLE);
        table.setPadEdge(false);
        table.setStyle("markdown.table.border");
        table.setShowEdge(true);
        table.setCollapsePadding(true);

        for (Text cellText : headerCells) {
            cellText.stylize("markdown.table.header");
            table.addColumn(cellText);
        }

        for (List<Text> row : bodyRows) {
            table.addRow(row.toArray());
        }

        return toSegmentList(table.richConsole(context.console, context.options));
    }

    private void extractTableRows(Node section, List<Text> headerCells,
                                   List<List<Text>> bodyRows) {
        Node row = section.getFirstChild();
        while (row != null) {
            if (row instanceof TableRow) {
                List<Text> cells = new ArrayList<>();
                Node cell = row.getFirstChild();
                while (cell != null) {
                    if (cell instanceof TableCell) {
                        TableCell tc = (TableCell) cell;
                        String alignment = tc.getAlignment() != null
                                ? tc.getAlignment().name().toLowerCase() : "left";
                        Text cellText = new Text();
                        cellText.setJustify(alignment);
                        collectCellText(cellText, tc);
                        cells.add(cellText);
                    }
                    cell = cell.getNext();
                }
                if (headerCells != null && headerCells.isEmpty()) {
                    headerCells.addAll(cells);
                } else if (bodyRows != null) {
                    bodyRows.add(cells);
                }
            }
            row = row.getNext();
        }
    }

    private void collectCellText(Text text, Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof org.commonmark.node.Text cmText) {
                text.append(cmText.getLiteral());
            } else if (child instanceof Code) {
                text.append(((Code) child).getLiteral());
            } else {
                collectCellText(text, child);
            }
            child = child.getNext();
        }
    }

    // ---- Unknown block ----

    private List<Segment> renderUnknownBlock(Node node, MarkdownContext context) {
        Text text = collectInlineText(node, context);
        if (text.length() == 0) {
            return java.util.Collections.emptyList();
        }
        text.setEnd(""); // Block spacing is handled by richConsole()
        return toSegmentList(text.richConsole(context.console, context.options));
    }

    // =========================================================================
    // Inline content collection
    // =========================================================================

    private Text collectInlineText(Node node, MarkdownContext context) {
        Text text = new Text();
        text.setJustify("left");
        collectInlineInto(text, node, context);
        return text;
    }

    private void collectInlineInto(Text text, Node node, MarkdownContext context) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof org.commonmark.node.Text cmText) {
                text.append(cmText.getLiteral(), context.currentStyle());
            } else if (child instanceof SoftLineBreak) {
                text.append(" ", context.currentStyle());
            } else if (child instanceof HardLineBreak) {
                text.append("\n", context.currentStyle());
            } else if (child instanceof Emphasis) {
                context.enterStyle("markdown.em");
                collectInlineInto(text, child, context);
                context.leaveStyle();
            } else if (child instanceof StrongEmphasis) {
                context.enterStyle("markdown.strong");
                collectInlineInto(text, child, context);
                context.leaveStyle();
            } else if (child instanceof org.commonmark.ext.gfm.strikethrough.Strikethrough) {
                context.enterStyle("markdown.s");
                collectInlineInto(text, child, context);
                context.leaveStyle();
            } else if (child instanceof Code) {
                context.enterStyle("markdown.code");
                text.append(((Code) child).getLiteral(), context.currentStyle());
                context.leaveStyle();
            } else if (child instanceof Link) {
                renderInlineLink(text, (Link) child, context);
            } else if (child instanceof Image) {
                renderInlineImage(text, (Image) child, context);
            } else {
                collectInlineInto(text, child, context);
            }
            child = child.getNext();
        }
    }

    private void renderInlineLink(Text text, Link link, MarkdownContext context) {
        if (hyperlinks) {
            Style linkStyle = context.console.getStyle("markdown.link_url");
            if (linkStyle != null) {
                String href = link.getDestination();
                Style hrefStyle = linkStyle.add(Style.nullStyle().updateLink(href));
                context.pushStyle(hrefStyle);
                collectInlineInto(text, link, context);
                context.popStyle();
            } else {
                context.enterStyle("markdown.link");
                collectInlineInto(text, link, context);
                context.leaveStyle();
            }
        } else {
            context.enterStyle("markdown.link");
            collectInlineInto(text, link, context);
            context.leaveStyle();
            text.append(" (", null);
            context.enterStyle("markdown.link_url");
            text.append(link.getDestination(), context.currentStyle());
            context.leaveStyle();
            text.append(")", null);
        }
    }

    private void renderInlineImage(Text text, Image img, MarkdownContext context) {
        context.enterStyle("markdown.link");
        text.append("\uD83C\uDF07 ", context.currentStyle());
        if (img.getTitle() != null && !img.getTitle().isEmpty()) {
            text.append(img.getTitle(), context.currentStyle());
        } else {
            String src = img.getDestination();
            String name = src != null
                    ? src.substring(Math.max(0, src.lastIndexOf('/') + 1)) : "image";
            text.append(name, context.currentStyle());
        }
        text.append(" ", null);
        context.leaveStyle();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static List<Segment> toSegmentList(Iterable<?> iterable) {
        List<Segment> list = new ArrayList<>();
        if (iterable != null) {
            for (Object item : iterable) {
                if (item instanceof Segment s) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    /** Java 8 compatible stripTrailing — removes trailing whitespace. */
    private static String trimTrailing(String s) {
        if (s == null || s.isEmpty()) return s;
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) <= ' ') {
            end--;
        }
        return s.substring(0, end);
    }

    // =========================================================================
    // MarkdownContext
    // =========================================================================

    private static class MarkdownContext {
        final Console console;
        ConsoleOptions options;
        final StyleStack styleStack;

        MarkdownContext(Console console, ConsoleOptions options, Style baseStyle) {
            this.console = console;
            this.options = options;
            this.styleStack = new StyleStack(baseStyle != null ? baseStyle : Style.nullStyle());
        }

        Style currentStyle() {
            return styleStack.current();
        }

        void enterStyle(String styleName) {
            Style s = console.getStyle(styleName);
            styleStack.push(s);
        }

        void pushStyle(Style s) {
            styleStack.push(s);
        }

        void leaveStyle() {
            if (styleStack.size() > 1) {
                styleStack.pop();
            }
        }

        void popStyle() {
            if (styleStack.size() > 1) {
                styleStack.pop();
            }
        }

        MarkdownContext withOptions(ConsoleOptions newOptions) {
            return new MarkdownContext(console, newOptions, styleStack.current());
        }
    }

    // =========================================================================
    // SegmentList
    // =========================================================================

    private static class SegmentList implements RichRenderable {
        private final List<Segment> segments;

        SegmentList(List<Segment> segments) {
            this.segments = segments;
        }

        @Override
        public Iterable<Segment> richConsole(Console console, ConsoleOptions options) {
            return segments;
        }
    }
}
