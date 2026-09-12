package com.justnothing.richconsole.console;

/**
 * Export format templates for HTML/SVG output.
 * Ported from rich/_export_format.py.
 */
public final class ExportFormat {

    private ExportFormat() {
    }

    /**
     * HTML template. Placeholders in order: %1$s stylesheet, %2$s foreground,
     * %3$s background, %4$s code.
     */
    public static final String CONSOLE_HTML_FORMAT = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
            %1$s
            body {
                color: %2$s;
                background-color: %3$s;
            }
            </style>
            </head>
            <body>
                <pre style="font-family:Menlo,'DejaVu Sans Mono',consolas,'Courier New',monospace"><code style="font-family:inherit">%4$s</code></pre>
            </body>
            </html>
            """;

    /**
     * SVG template. Placeholders in order (as passed by Console.exportSvg()):
     * %1$s unique_id, %2$s char_width, %3$s char_height, %4$s line_height,
     * %5$s terminal_width, %6$s terminal_height, %7$s width, %8$s height,
     * %9$s terminal_x, %10$s terminal_y, %11$s styles, %12$s chrome,
     * %13$s backgrounds, %14$s matrix, %15$s lines.
     */
    public static final String CONSOLE_SVG_FORMAT = """
            <svg class="rich-terminal" viewBox="0 0 %7$s %8$s" xmlns="http://www.w3.org/2000/svg">
                <!-- Generated with RichConsole -->
                <style>

                @font-face {
                    font-family: "Fira Code";
                    src: local("FiraCode-Regular"),
                            url("https://cdnjs.cloudflare.com/ajax/libs/firacode/6.2.0/woff2/FiraCode-Regular.woff2") format("woff2"),
                            url("https://cdnjs.cloudflare.com/ajax/libs/firacode/6.2.0/woff/FiraCode-Regular.woff") format("woff");
                    font-style: normal;
                    font-weight: 400;
                }
                @font-face {
                    font-family: "Fira Code";
                    src: local("FiraCode-Bold"),
                            url("https://cdnjs.cloudflare.com/ajax/libs/firacode/6.2.0/woff2/FiraCode-Bold.woff2") format("woff2"),
                            url("https://cdnjs.cloudflare.com/ajax/libs/firacode/6.2.0/woff/FiraCode-Bold.woff") format("woff");
                    font-style: bold;
                    font-weight: 700;
                }

                .%1$s-matrix {
                    font-family: Fira Code, monospace;
                    font-size: %3$spx;
                    line-height: %4$spx;
                    font-variant-east-asian: full-width;
                }

                .%1$s-title {
                    font-size: 18px;
                    font-weight: bold;
                    font-family: arial;
                }

                %11$s
                </style>

                <defs>
                <clipPath id="%1$s-clip-terminal">
                  <rect x="0" y="0" width="%5$s" height="%6$s" />
                </clipPath>
                %15$s
                </defs>

                %12$s
                <g transform="translate(%9$s, %10$s)" clip-path="url(#%1$s-clip-terminal)">
                %13$s
                <g class="%1$s-matrix">
                %14$s
                </g>
                </g>
            </svg>
            """;

    /**
     * HTML-escape text, matching Python's {@code html.escape(text, quote=True)}.
     */
    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
