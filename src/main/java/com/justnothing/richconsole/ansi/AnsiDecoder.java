package com.justnothing.richconsole.ansi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.justnothing.richconsole.color.Color;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.text.Text;

/**
 * Translate ANSI codes into styled Text.
 * Ported from rich/ansi.py AnsiDecoder.
 */
// TODO: integrate into Console.renderStr() for ANSI string handling (Python: Text.from_ansi() via AnsiDecoder)
public class AnsiDecoder {

    /**
     * Regex for tokenizing ANSI escape sequences.
     */
    private static final Pattern RE_ANSI = Pattern.compile(
        """
                \\x1b[0-?]|
                \\x1b](.*?)\\x1b\\\\|
                \\x1b([(@-Z\\\\-_]|\\[[0-?]*[ -/]*[@-~])
                """
    );

    /**
     * Map from SGR code to style definition string.
     */
    private static final Map<Integer, String> SGR_STYLE_MAP;

    static {
        Map<Integer, String> m = new HashMap<>();
        m.put(1, "bold");
        m.put(2, "dim");
        m.put(3, "italic");
        m.put(4, "underline");
        m.put(5, "blink");
        m.put(6, "blink2");
        m.put(7, "reverse");
        m.put(8, "conceal");
        m.put(9, "strike");
        m.put(21, "underline2");
        m.put(22, "not dim not bold");
        m.put(23, "not italic");
        m.put(24, "not underline");
        m.put(25, "not blink");
        m.put(26, "not blink2");
        m.put(27, "not reverse");
        m.put(28, "not conceal");
        m.put(29, "not strike");
        m.put(30, "color(0)");
        m.put(31, "color(1)");
        m.put(32, "color(2)");
        m.put(33, "color(3)");
        m.put(34, "color(4)");
        m.put(35, "color(5)");
        m.put(36, "color(6)");
        m.put(37, "color(7)");
        m.put(39, "default");
        m.put(40, "on color(0)");
        m.put(41, "on color(1)");
        m.put(42, "on color(2)");
        m.put(43, "on color(3)");
        m.put(44, "on color(4)");
        m.put(45, "on color(5)");
        m.put(46, "on color(6)");
        m.put(47, "on color(7)");
        m.put(49, "on default");
        m.put(51, "frame");
        m.put(52, "encircle");
        m.put(53, "overline");
        m.put(54, "not frame not encircle");
        m.put(55, "not overline");
        m.put(90, "color(8)");
        m.put(91, "color(9)");
        m.put(92, "color(10)");
        m.put(93, "color(11)");
        m.put(94, "color(12)");
        m.put(95, "color(13)");
        m.put(96, "color(14)");
        m.put(97, "color(15)");
        m.put(100, "on color(8)");
        m.put(101, "on color(9)");
        m.put(102, "on color(10)");
        m.put(103, "on color(11)");
        m.put(104, "on color(12)");
        m.put(105, "on color(13)");
        m.put(106, "on color(14)");
        m.put(107, "on color(15)");
        SGR_STYLE_MAP = Collections.unmodifiableMap(m);
    }

    private Style currentStyle;

    public AnsiDecoder() {
        this.currentStyle = Style.nullStyle();
    }

    /**
     * Decode ANSI codes in terminal text, splitting by lines.
     *
     * @param terminalText text containing ANSI codes
     * @return a list of Text instances, one per line
     */
    public List<Text> decode(String terminalText) {
        List<Text> result = new ArrayList<>();
        for (String line : terminalText.split("\\r?\\n")) {
            result.add(decodeLine(line));
        }
        return result;
    }

    /**
     * Decode a single line containing ANSI codes.
     *
     * @param line a line of terminal output
     * @return a Text instance marked up according to ANSI codes
     */
    public Text decodeLine(String line) {
        Text text = new Text();
        // Handle carriage return: take the part after the last \r
        int lastCr = line.lastIndexOf('\r');
        if (lastCr >= 0) {
            line = line.substring(lastCr + 1);
        }

        for (AnsiToken token : ansiTokenize(line)) {
            if (token.plain != null && !token.plain.isEmpty()) {
                Style appendStyle = currentStyle.isNull() ? null : currentStyle;
                text.append(token.plain, appendStyle);
            } else if (token.osc != null) {
                if (token.osc.startsWith("8;")) {
                    String rest = token.osc.substring(2);
                    int semicolonIdx = rest.indexOf(';');
                    if (semicolonIdx >= 0) {
                        String link = rest.substring(semicolonIdx + 1);
                        currentStyle = currentStyle.updateLink(link.isEmpty() ? null : link);
                    }
                }
            } else if (token.sgr != null) {
                // Parse SGR codes
                List<Integer> codes = new ArrayList<>();
                for (String part : token.sgr.split(";")) {
                    if (part.isEmpty() || !isDigits(part)) {
                        codes.add(0);
                    } else {
                        codes.add(Math.min(255, Integer.parseInt(part)));
                    }
                }

                Iterator<Integer> iterCodes = codes.iterator();
                while (iterCodes.hasNext()) {
                    int code = iterCodes.next();
                    if (code == 0) {
                        // Reset
                        currentStyle = Style.nullStyle();
                    } else if (SGR_STYLE_MAP.containsKey(code)) {
                        // Known SGR style
                        currentStyle = currentStyle.add(Style.parse(SGR_STYLE_MAP.get(code)));
                    } else if (code == 38) {
                        // Foreground
                        if (iterCodes.hasNext()) {
                            int colorType = iterCodes.next();
                            if (colorType == 5 && iterCodes.hasNext()) {
                                int colorNum = iterCodes.next();
                                currentStyle = currentStyle.add(
                                    Style.fromColor(Color.fromAnsi(colorNum), null));
                            } else if (colorType == 2) {
                                try {
                                    int r = iterCodes.next();
                                    int g = iterCodes.next();
                                    int b = iterCodes.next();
                                    currentStyle = currentStyle.add(
                                        Style.fromColor(Color.fromRgb(r, g, b), null));
                                } catch (Exception ignored) {
                                    // Not enough parameters, ignore
                                }
                            }
                        }
                    } else if (code == 48) {
                        // Background
                        if (iterCodes.hasNext()) {
                            int colorType = iterCodes.next();
                            if (colorType == 5 && iterCodes.hasNext()) {
                                int colorNum = iterCodes.next();
                                currentStyle = currentStyle.add(
                                    Style.fromColor(null, Color.fromAnsi(colorNum)));
                            } else if (colorType == 2) {
                                try {
                                    int r = iterCodes.next();
                                    int g = iterCodes.next();
                                    int b = iterCodes.next();
                                    currentStyle = currentStyle.add(
                                        Style.fromColor(null, Color.fromRgb(r, g, b)));
                                } catch (Exception ignored) {
                                    // Not enough parameters, ignore
                                }
                            }
                        }
                    }
                    // Unknown codes are silently ignored
                }
            }
        }

        return text;
    }

    /**
     * Tokenize a string into plain text and ANSI escape sequences.
     */
    private static List<AnsiToken> ansiTokenize(String ansiText) {
        List<AnsiToken> tokens = new ArrayList<>();
        Matcher matcher = RE_ANSI.matcher(ansiText);
        int position = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            String osc = matcher.group(1);
            String sgr = matcher.group(2);

            // Plain text before this match
            if (start > position) {
                tokens.add(new AnsiToken(ansiText.substring(position, start), null, null));
            }

            if (sgr != null) {
                if ("(".equals(sgr)) {
                    // Skip character set designation
                    position = end + 1;
                    continue;
                }
                if (sgr.endsWith("m")) {
                    // SGR sequence: strip the leading '[' and trailing 'm'
                    tokens.add(new AnsiToken("", sgr.substring(1, sgr.length() - 1), osc));
                } else {
                    tokens.add(new AnsiToken("", sgr, osc));
                }
            } else {
                tokens.add(new AnsiToken("", null, osc));
            }

            position = end;
        }

        // Remaining plain text
        if (position < ansiText.length()) {
            tokens.add(new AnsiToken(ansiText.substring(position), null, null));
        }

        return tokens;
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return !s.isEmpty();
    }

    /**
     * Internal token for ANSI tokenization.
     */
    private static class AnsiToken {
        final String plain;
        final String sgr;
        final String osc;

        AnsiToken(String plain, String sgr, String osc) {
            this.plain = plain;
            this.sgr = sgr;
            this.osc = osc;
        }
    }
}
