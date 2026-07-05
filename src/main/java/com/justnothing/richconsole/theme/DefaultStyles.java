package com.justnothing.richconsole.theme;

import java.util.LinkedHashMap;
import java.util.Map;

import com.justnothing.richconsole.style.Style;

/**
 * Default style definitions for the Rich console.
 * Ported from rich/default_styles.py.
 */
public final class DefaultStyles {

    private static final Map<String, Style> STYLES = new LinkedHashMap<>();

    static {
        STYLES.put("none", Style.nullStyle());
        STYLES.put("reset", Style.parse("default on default not dim not bold not italic not underline not blink not blink2 not reverse not conceal not strike"));
        STYLES.put("dim", Style.parse("dim"));
        STYLES.put("bright", Style.parse("not dim"));
        STYLES.put("bold", Style.parse("bold"));
        STYLES.put("strong", Style.parse("bold"));
        STYLES.put("code", Style.parse("reverse bold"));
        STYLES.put("italic", Style.parse("italic"));
        STYLES.put("emphasize", Style.parse("italic"));
        STYLES.put("underline", Style.parse("underline"));
        STYLES.put("blink", Style.parse("blink"));
        STYLES.put("blink2", Style.parse("blink2"));
        STYLES.put("reverse", Style.parse("reverse"));
        STYLES.put("strike", Style.parse("strike"));
        STYLES.put("black", Style.parse("black"));
        STYLES.put("red", Style.parse("red"));
        STYLES.put("green", Style.parse("green"));
        STYLES.put("yellow", Style.parse("yellow"));
        STYLES.put("magenta", Style.parse("magenta"));
        STYLES.put("cyan", Style.parse("cyan"));
        STYLES.put("white", Style.parse("white"));
        STYLES.put("inspect.attr", Style.parse("yellow italic"));
        STYLES.put("inspect.attr.dunder", Style.parse("yellow italic dim"));
        STYLES.put("inspect.callable", Style.parse("bold red"));
        STYLES.put("inspect.async_def", Style.parse("italic bright_cyan"));
        STYLES.put("inspect.def", Style.parse("italic bright_cyan"));
        STYLES.put("inspect.class", Style.parse("italic bright_cyan"));
        STYLES.put("inspect.error", Style.parse("bold red"));
        STYLES.put("inspect.equals", Style.parse(""));
        STYLES.put("inspect.help", Style.parse("cyan"));
        STYLES.put("inspect.doc", Style.parse("dim"));
        STYLES.put("inspect.value.border", Style.parse("green"));
        STYLES.put("live.ellipsis", Style.parse("bold red"));
        STYLES.put("layout.tree.row", Style.parse("not dim red"));
        STYLES.put("layout.tree.column", Style.parse("not dim blue"));
        STYLES.put("logging.keyword", Style.parse("bold yellow"));
        STYLES.put("logging.level.notset", Style.parse("dim"));
        STYLES.put("logging.level.debug", Style.parse("green"));
        STYLES.put("logging.level.info", Style.parse("blue"));
        STYLES.put("logging.level.warning", Style.parse("yellow"));
        STYLES.put("logging.level.error", Style.parse("red bold"));
        STYLES.put("logging.level.critical", Style.parse("red bold reverse"));
        STYLES.put("log.level", Style.nullStyle());
        STYLES.put("log.time", Style.parse("cyan dim"));
        STYLES.put("log.message", Style.nullStyle());
        STYLES.put("log.path", Style.parse("dim"));
        STYLES.put("repr.ellipsis", Style.parse("yellow"));
        STYLES.put("repr.indent", Style.parse("green dim"));
        STYLES.put("repr.error", Style.parse("red bold"));
        STYLES.put("repr.str", Style.parse("green not italic not bold"));
        STYLES.put("repr.brace", Style.parse("bold"));
        STYLES.put("repr.comma", Style.parse("bold"));
        STYLES.put("repr.ipv4", Style.parse("bold bright_green"));
        STYLES.put("repr.ipv6", Style.parse("bold bright_green"));
        STYLES.put("repr.eui48", Style.parse("bold bright_green"));
        STYLES.put("repr.eui64", Style.parse("bold bright_green"));
        STYLES.put("repr.tagStart", Style.parse("bold"));
        STYLES.put("repr.tagName", Style.parse("bright_magenta bold"));
        STYLES.put("repr.tagContents", Style.parse("default"));
        STYLES.put("repr.tagEnd", Style.parse("bold"));
        STYLES.put("repr.attribName", Style.parse("yellow not italic"));
        STYLES.put("repr.attribEqual", Style.parse("bold"));
        STYLES.put("repr.attribValue", Style.parse("magenta not italic"));
        STYLES.put("repr.number", Style.parse("cyan bold not italic"));
        STYLES.put("repr.numberComplex", Style.parse("cyan bold not italic"));
        STYLES.put("repr.boolTrue", Style.parse("bright_green italic"));
        STYLES.put("repr.boolFalse", Style.parse("bright_red italic"));
        STYLES.put("repr.none", Style.parse("magenta italic"));
        STYLES.put("repr.url", Style.parse("underline bright_blue not italic not bold"));
        STYLES.put("repr.uuid", Style.parse("bright_yellow not bold"));
        STYLES.put("repr.call", Style.parse("magenta bold"));
        STYLES.put("repr.path", Style.parse("magenta"));
        STYLES.put("repr.filename", Style.parse("bright_magenta"));
        STYLES.put("rule.line", Style.parse("bright_green"));
        STYLES.put("rule.text", Style.nullStyle());
        STYLES.put("json.brace", Style.parse("bold"));
        STYLES.put("json.boolTrue", Style.parse("bright_green italic"));
        STYLES.put("json.boolFalse", Style.parse("bright_red italic"));
        STYLES.put("json.null", Style.parse("magenta italic"));
        STYLES.put("json.number", Style.parse("cyan bold not italic"));
        STYLES.put("json.str", Style.parse("green not italic not bold"));
        STYLES.put("json.key", Style.parse("blue bold"));
        STYLES.put("prompt", Style.nullStyle());
        STYLES.put("prompt.choices", Style.parse("magenta bold"));
        STYLES.put("prompt.default", Style.parse("cyan bold"));
        STYLES.put("prompt.invalid", Style.parse("red"));
        STYLES.put("prompt.invalid.choice", Style.parse("red"));
        STYLES.put("pretty", Style.nullStyle());
        STYLES.put("scope.border", Style.parse("blue"));
        STYLES.put("scope.key", Style.parse("yellow italic"));
        STYLES.put("scope.key.special", Style.parse("yellow italic dim"));
        STYLES.put("scope.equals", Style.parse("red"));
        STYLES.put("table.header", Style.parse("bold"));
        STYLES.put("table.footer", Style.parse("bold"));
        STYLES.put("table.cell", Style.nullStyle());
        STYLES.put("table.title", Style.parse("italic"));
        STYLES.put("table.caption", Style.parse("italic dim"));
        STYLES.put("traceback.error", Style.parse("red italic"));
        STYLES.put("traceback.border.syntax_error", Style.parse("bright_red"));
        STYLES.put("traceback.border", Style.parse("red"));
        STYLES.put("traceback.text", Style.nullStyle());
        STYLES.put("traceback.title", Style.parse("red bold"));
        STYLES.put("traceback.exc_type", Style.parse("bright_red bold"));
        STYLES.put("traceback.exc_value", Style.nullStyle());
        STYLES.put("traceback.offset", Style.parse("bright_red bold"));
        STYLES.put("traceback.error_range", Style.parse("underline bold"));
        STYLES.put("traceback.note", Style.parse("green bold"));
        STYLES.put("traceback.group.border", Style.parse("magenta"));
        STYLES.put("bar.back", Style.parse("grey23"));
        STYLES.put("bar.complete", Style.parse("rgb(249,38,114)"));
        STYLES.put("bar.finished", Style.parse("rgb(114,156,31)"));
        STYLES.put("bar.pulse", Style.parse("rgb(249,38,114)"));
        STYLES.put("progress.description", Style.nullStyle());
        STYLES.put("progress.filesize", Style.parse("green"));
        STYLES.put("progress.filesize.total", Style.parse("green"));
        STYLES.put("progress.download", Style.parse("green"));
        STYLES.put("progress.elapsed", Style.parse("yellow"));
        STYLES.put("progress.percentage", Style.parse("magenta"));
        STYLES.put("progress.remaining", Style.parse("cyan"));
        STYLES.put("progress.data.speed", Style.parse("red"));
        STYLES.put("progress.spinner", Style.parse("green"));
        STYLES.put("status.spinner", Style.parse("green"));
        STYLES.put("tree", Style.parse(""));
        STYLES.put("tree.line", Style.parse(""));
        STYLES.put("markdown.paragraph", Style.parse(""));
        STYLES.put("markdown.text", Style.parse(""));
        STYLES.put("markdown.em", Style.parse("italic"));
        STYLES.put("markdown.emph", Style.parse("italic"));
        STYLES.put("markdown.strong", Style.parse("bold"));
        STYLES.put("markdown.code", Style.parse("bold cyan on black"));
        STYLES.put("markdown.code_block", Style.parse("cyan on black"));
        STYLES.put("markdown.block_quote", Style.parse("magenta"));
        STYLES.put("markdown.list", Style.parse("cyan"));
        STYLES.put("markdown.item", Style.parse(""));
        STYLES.put("markdown.item.bullet", Style.parse("bold"));
        STYLES.put("markdown.item.number", Style.parse("cyan"));
        STYLES.put("markdown.hr", Style.parse("dim"));
        STYLES.put("markdown.h1.border", Style.parse(""));
        STYLES.put("markdown.h1", Style.parse("bold underline"));
        STYLES.put("markdown.h2", Style.parse("magenta underline"));
        STYLES.put("markdown.h3", Style.parse("magenta bold"));
        STYLES.put("markdown.h4", Style.parse("magenta italic"));
        STYLES.put("markdown.h5", Style.parse("italic"));
        STYLES.put("markdown.h6", Style.parse("dim"));
        STYLES.put("markdown.h7", Style.parse("italic dim"));
        STYLES.put("markdown.link", Style.parse("bright_blue"));
        STYLES.put("markdown.link_url", Style.parse("blue underline"));
        STYLES.put("markdown.s", Style.parse("strike"));
        STYLES.put("markdown.table.border", Style.parse("cyan"));
        STYLES.put("markdown.table.header", Style.parse("cyan not bold"));
        STYLES.put("iso8601.date", Style.parse("blue"));
        STYLES.put("iso8601.time", Style.parse("magenta"));
        STYLES.put("iso8601.timezone", Style.parse("yellow"));
        STYLES.put("syntax.line_number", Style.parse("dim"));
        STYLES.put("syntax.highlight_line", Style.parse("bold"));
        STYLES.put("syntax.line_pointer", Style.parse("red"));
        STYLES.put("syntax.separator", Style.parse("dim"));
    }

    private DefaultStyles() {
    }

    /**
     * Get a copy of the default styles map.
     */
    public static Map<String, Style> getStyles() {
        return new LinkedHashMap<>(STYLES);
    }

    /**
     * Get a specific default style by name.
     */
    public static Style getStyle(String name) {
        return STYLES.get(name);
    }
}
