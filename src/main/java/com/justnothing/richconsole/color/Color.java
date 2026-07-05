package com.justnothing.richconsole.color;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.justnothing.richconsole.terminal.TerminalTheme;

/**
 * Terminal color definition.
 * Ported from rich/color.py.
 */
public final class Color {

    private final String name;
    private final ColorType type;
    private final Integer number;
    private final ColorTriplet triplet;

    // ANSI color name -> color number mapping (from Python source)
    private static final Map<String, Integer> ANSI_COLOR_NAMES;
    static {
        Map<String, Integer> m = new HashMap<>();
        m.put("black", 0);
        m.put("red", 1);
        m.put("green", 2);
        m.put("yellow", 3);
        m.put("blue", 4);
        m.put("magenta", 5);
        m.put("cyan", 6);
        m.put("white", 7);
        m.put("bright_black", 8);
        m.put("bright_red", 9);
        m.put("bright_green", 10);
        m.put("bright_yellow", 11);
        m.put("bright_blue", 12);
        m.put("bright_magenta", 13);
        m.put("bright_cyan", 14);
        m.put("bright_white", 15);
        m.put("grey0", 16);
        m.put("gray0", 16);
        m.put("navy_blue", 17);
        m.put("dark_blue", 18);
        m.put("blue3", 20);
        m.put("blue1", 21);
        m.put("dark_green", 22);
        m.put("deep_sky_blue4", 25);
        m.put("dodger_blue3", 26);
        m.put("dodger_blue2", 27);
        m.put("green4", 28);
        m.put("spring_green4", 29);
        m.put("turquoise4", 30);
        m.put("deep_sky_blue3", 32);
        m.put("dodger_blue1", 33);
        m.put("green3", 40);
        m.put("spring_green3", 41);
        m.put("dark_cyan", 36);
        m.put("light_sea_green", 37);
        m.put("deep_sky_blue2", 38);
        m.put("deep_sky_blue1", 39);
        m.put("spring_green2", 47);
        m.put("cyan3", 43);
        m.put("dark_turquoise", 44);
        m.put("turquoise2", 45);
        m.put("green1", 46);
        m.put("spring_green1", 48);
        m.put("medium_spring_green", 49);
        m.put("cyan2", 50);
        m.put("cyan1", 51);
        m.put("dark_red", 88);
        m.put("deep_pink4", 125);
        m.put("purple4", 55);
        m.put("purple3", 56);
        m.put("blue_violet", 57);
        m.put("orange4", 94);
        m.put("grey37", 59);
        m.put("gray37", 59);
        m.put("medium_purple4", 60);
        m.put("slate_blue3", 62);
        m.put("royal_blue1", 63);
        m.put("chartreuse4", 64);
        m.put("dark_sea_green4", 71);
        m.put("pale_turquoise4", 66);
        m.put("steel_blue", 67);
        m.put("steel_blue3", 68);
        m.put("cornflower_blue", 69);
        m.put("chartreuse3", 76);
        m.put("cadet_blue", 73);
        m.put("sky_blue3", 74);
        m.put("steel_blue1", 81);
        m.put("pale_green3", 114);
        m.put("sea_green3", 78);
        m.put("aquamarine3", 79);
        m.put("medium_turquoise", 80);
        m.put("chartreuse2", 112);
        m.put("sea_green2", 83);
        m.put("sea_green1", 85);
        m.put("aquamarine1", 122);
        m.put("dark_slate_gray2", 87);
        m.put("dark_magenta", 91);
        m.put("dark_violet", 128);
        m.put("purple", 129);
        m.put("light_pink4", 95);
        m.put("plum4", 96);
        m.put("medium_purple3", 98);
        m.put("slate_blue1", 99);
        m.put("yellow4", 106);
        m.put("wheat4", 101);
        m.put("grey53", 102);
        m.put("gray53", 102);
        m.put("light_slate_grey", 103);
        m.put("light_slate_gray", 103);
        m.put("medium_purple", 104);
        m.put("light_slate_blue", 105);
        m.put("dark_olive_green3", 149);
        m.put("dark_sea_green", 108);
        m.put("light_sky_blue3", 110);
        m.put("sky_blue2", 111);
        m.put("dark_sea_green3", 150);
        m.put("dark_slate_gray3", 116);
        m.put("sky_blue1", 117);
        m.put("chartreuse1", 118);
        m.put("light_green", 120);
        m.put("pale_green1", 156);
        m.put("dark_slate_gray1", 123);
        m.put("red3", 160);
        m.put("medium_violet_red", 126);
        m.put("magenta3", 164);
        m.put("dark_orange3", 166);
        m.put("indian_red", 167);
        m.put("hot_pink3", 168);
        m.put("medium_orchid3", 133);
        m.put("medium_orchid", 134);
        m.put("medium_purple2", 140);
        m.put("dark_goldenrod", 136);
        m.put("light_salmon3", 173);
        m.put("rosy_brown", 138);
        m.put("grey63", 139);
        m.put("gray63", 139);
        m.put("medium_purple1", 141);
        m.put("gold3", 178);
        m.put("dark_khaki", 143);
        m.put("navajo_white3", 144);
        m.put("grey69", 145);
        m.put("gray69", 145);
        m.put("light_steel_blue3", 146);
        m.put("light_steel_blue", 147);
        m.put("yellow3", 184);
        m.put("dark_sea_green2", 157);
        m.put("light_cyan3", 152);
        m.put("light_sky_blue1", 153);
        m.put("green_yellow", 154);
        m.put("dark_olive_green2", 155);
        m.put("dark_sea_green1", 193);
        m.put("pale_turquoise1", 159);
        m.put("deep_pink3", 162);
        m.put("magenta2", 200);
        m.put("hot_pink2", 169);
        m.put("orchid", 170);
        m.put("medium_orchid1", 207);
        m.put("orange3", 172);
        m.put("light_pink3", 174);
        m.put("pink3", 175);
        m.put("plum3", 176);
        m.put("violet", 177);
        m.put("light_goldenrod3", 179);
        m.put("tan", 180);
        m.put("misty_rose3", 181);
        m.put("thistle3", 182);
        m.put("plum2", 183);
        m.put("khaki3", 185);
        m.put("light_goldenrod2", 222);
        m.put("light_yellow3", 187);
        m.put("grey84", 188);
        m.put("gray84", 188);
        m.put("light_steel_blue1", 189);
        m.put("yellow2", 190);
        m.put("dark_olive_green1", 192);
        m.put("honeydew2", 194);
        m.put("light_cyan1", 195);
        m.put("red1", 196);
        m.put("deep_pink2", 197);
        m.put("deep_pink1", 199);
        m.put("magenta1", 201);
        m.put("orange_red1", 202);
        m.put("indian_red1", 204);
        m.put("hot_pink", 206);
        m.put("dark_orange", 208);
        m.put("salmon1", 209);
        m.put("light_coral", 210);
        m.put("pale_violet_red1", 211);
        m.put("orchid2", 212);
        m.put("orchid1", 213);
        m.put("orange1", 214);
        m.put("sandy_brown", 215);
        m.put("light_salmon1", 216);
        m.put("light_pink1", 217);
        m.put("pink1", 218);
        m.put("plum1", 219);
        m.put("gold1", 220);
        m.put("navajo_white1", 223);
        m.put("misty_rose1", 224);
        m.put("thistle1", 225);
        m.put("yellow1", 226);
        m.put("light_goldenrod1", 227);
        m.put("khaki1", 228);
        m.put("wheat1", 229);
        m.put("cornsilk1", 230);
        m.put("grey100", 231);
        m.put("gray100", 231);
        m.put("grey3", 232);
        m.put("gray3", 232);
        m.put("grey7", 233);
        m.put("gray7", 233);
        m.put("grey11", 234);
        m.put("gray11", 234);
        m.put("grey15", 235);
        m.put("gray15", 235);
        m.put("grey19", 236);
        m.put("gray19", 236);
        m.put("grey23", 237);
        m.put("gray23", 237);
        m.put("grey27", 238);
        m.put("gray27", 238);
        m.put("grey30", 239);
        m.put("gray30", 239);
        m.put("grey35", 240);
        m.put("gray35", 240);
        m.put("grey39", 241);
        m.put("gray39", 241);
        m.put("grey42", 242);
        m.put("gray42", 242);
        m.put("grey46", 243);
        m.put("gray46", 243);
        m.put("grey50", 244);
        m.put("gray50", 244);
        m.put("grey54", 245);
        m.put("gray54", 245);
        m.put("grey58", 246);
        m.put("gray58", 246);
        m.put("grey62", 247);
        m.put("gray62", 247);
        m.put("grey66", 248);
        m.put("gray66", 248);
        m.put("grey70", 249);
        m.put("gray70", 249);
        m.put("grey74", 250);
        m.put("gray74", 250);
        m.put("grey78", 251);
        m.put("gray78", 251);
        m.put("grey82", 252);
        m.put("gray82", 252);
        m.put("grey85", 253);
        m.put("gray85", 253);
        m.put("grey89", 254);
        m.put("gray89", 254);
        m.put("grey93", 255);
        m.put("gray93", 255);
        ANSI_COLOR_NAMES = Collections.unmodifiableMap(m);
    }

    /**
     * Regex pattern for parsing color strings: #rrggbb, color(N), rgb(r,g,b)
     */
    static final Pattern RE_COLOR = Pattern.compile(
        "^#([0-9a-f]{6})$|color\\(([0-9]{1,3})\\)$|rgb\\(([\\d\\s,]+)\\)$"
    );

    // Caches (equivalent to Python's lru_cache)
    private static final ConcurrentHashMap<String, Color> PARSE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String[]> ANSI_CODES_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Color> DOWNGRADE_CACHE = new ConcurrentHashMap<>();

    public Color(String name, ColorType type) {
        this(name, type, null, null);
    }

    public Color(String name, ColorType type, Integer number) {
        this(name, type, number, null);
    }

    public Color(String name, ColorType type, Integer number, ColorTriplet triplet) {
        this.name = name;
        this.type = type;
        this.number = number;
        this.triplet = triplet;
    }

    public String getName() {
        return name;
    }

    public ColorType getType() {
        return type;
    }

    public Integer getNumber() {
        return number;
    }

    public ColorTriplet getTriplet() {
        return triplet;
    }

    /**
     * Get the native color system for this color.
     */
    public ColorSystem getSystem() {
        if (type == ColorType.DEFAULT) {
            return ColorSystem.STANDARD;
        }
        return switch (type) {
            case STANDARD -> ColorSystem.STANDARD;
            case EIGHT_BIT -> ColorSystem.EIGHT_BIT;
            case TRUECOLOR -> ColorSystem.TRUECOLOR;
            case WINDOWS -> ColorSystem.WINDOWS;
            default -> ColorSystem.STANDARD;
        };
    }

    /**
     * Check if the color is ultimately defined by the system.
     */
    public boolean isSystemDefined() {
        ColorSystem sys = getSystem();
        return sys != ColorSystem.EIGHT_BIT && sys != ColorSystem.TRUECOLOR;
    }

    /**
     * Check if the color is a default color.
     */
    public boolean isDefault() {
        return type == ColorType.DEFAULT;
    }

    /**
     * Get an equivalent color triplet for this color.
     */
    public ColorTriplet getTruecolor(TerminalTheme theme, boolean foreground) {
        if (theme == null) {
            theme = TerminalTheme.DEFAULT;
        }
        if (type == ColorType.TRUECOLOR) {
            return triplet;
        } else if (type == ColorType.EIGHT_BIT) {
            return Palettes.EIGHT_BIT_PALETTE.get(number);
        } else if (type == ColorType.STANDARD) {
            return theme.getAnsiColor(number);
        } else if (type == ColorType.WINDOWS) {
            return Palettes.WINDOWS_PALETTE.get(number);
        } else {
            // DEFAULT
            return foreground ? theme.getForegroundColor() : theme.getBackgroundColor();
        }
    }

    /**
     * Create a Color from its 8-bit ANSI number.
     */
    public static Color fromAnsi(int number) {
        return new Color(
            "color(" + number + ")",
            number < 16 ? ColorType.STANDARD : ColorType.EIGHT_BIT,
            number
        );
    }

    /**
     * Create a truecolor RGB color from a triplet of values.
     */
    public static Color fromTriplet(ColorTriplet triplet) {
        return new Color(triplet.hex(), ColorType.TRUECOLOR, null, triplet);
    }

    /**
     * Create a truecolor from three color components in the range 0-255.
     */
    public static Color fromRgb(int red, int green, int blue) {
        return fromTriplet(new ColorTriplet(red, green, blue));
    }

    /**
     * Get a Color instance representing the default color.
     */
    public static Color defaultColor() {
        return new Color("default", ColorType.DEFAULT);
    }

    /**
     * Parse a color definition string.
     * Supports: "default", named ANSI colors, "#rrggbb", "color(N)", "rgb(r,g,b)"
     */
    public static Color parse(String color) {
        Color cached = PARSE_CACHE.get(color);
        if (cached != null) {
            return cached;
        }

        String normalized = color.toLowerCase().trim();

        Color result;
        if (normalized.equals("default")) {
            result = new Color(normalized, ColorType.DEFAULT);
        } else {
            Integer colorNumber = ANSI_COLOR_NAMES.get(normalized);
            if (colorNumber != null) {
                result = new Color(
                    normalized,
                    colorNumber < 16 ? ColorType.STANDARD : ColorType.EIGHT_BIT,
                    colorNumber
                );
            } else {
                Matcher matcher = RE_COLOR.matcher(normalized);
                if (!matcher.matches()) {
                    throw new ColorParseError("'" + color + "' is not a valid color");
                }

                String color24 = matcher.group(1);  // #rrggbb
                String color8 = matcher.group(2);    // color(N)
                String colorRgb = matcher.group(3);  // rgb(r,g,b)

                if (color24 != null) {
                    ColorTriplet t = new ColorTriplet(
                        Integer.parseInt(color24.substring(0, 2), 16),
                        Integer.parseInt(color24.substring(2, 4), 16),
                        Integer.parseInt(color24.substring(4, 6), 16)
                    );
                    result = new Color(normalized, ColorType.TRUECOLOR, null, t);
                } else if (color8 != null) {
                    int number = Integer.parseInt(color8);
                    if (number > 255) {
                        throw new ColorParseError("color number must be <= 255 in '" + normalized + "'");
                    }
                    result = new Color(
                        normalized,
                        number < 16 ? ColorType.STANDARD : ColorType.EIGHT_BIT,
                        number
                    );
                } else {
                    // rgb(r,g,b)
                    String[] components = colorRgb.split(",");
                    if (components.length != 3) {
                        throw new ColorParseError("expected three components in '" + color + "'");
                    }
                    int r = Integer.parseInt(components[0].trim());
                    int g = Integer.parseInt(components[1].trim());
                    int b = Integer.parseInt(components[2].trim());
                    if (r > 255 || g > 255 || b > 255) {
                        throw new ColorParseError("color components must be <= 255 in '" + color + "'");
                    }
                    ColorTriplet t = new ColorTriplet(r, g, b);
                    result = new Color(normalized, ColorType.TRUECOLOR, null, t);
                }
            }
        }

        // Simple cache eviction: if cache is too large, clear it
        if (PARSE_CACHE.size() > 1024) {
            PARSE_CACHE.clear();
        }
        PARSE_CACHE.put(color, result);
        return result;
    }

    /**
     * Get the ANSI escape codes for this color.
     */
    public String[] getAnsiCodes(boolean foreground) {
        String cacheKey = name + ":" + foreground;
        String[] cached = ANSI_CODES_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String[] codes;
        if (type == ColorType.DEFAULT) {
            codes = new String[]{foreground ? "39" : "49"};
        } else if (type == ColorType.WINDOWS || type == ColorType.STANDARD) {
            int num = number;
            int fore = num < 8 ? 30 : 82;
            int back = num < 8 ? 40 : 92;
            codes = new String[]{String.valueOf(foreground ? fore + num : back + num)};
        } else if (type == ColorType.EIGHT_BIT) {
            codes = new String[]{foreground ? "38" : "48", "5", String.valueOf(number)};
        } else {
            // TRUECOLOR
            codes = new String[]{
                foreground ? "38" : "48",
                "2",
                String.valueOf(triplet.red()),
                String.valueOf(triplet.green()),
                String.valueOf(triplet.blue())
            };
        }

        if (ANSI_CODES_CACHE.size() > 1024) {
            ANSI_CODES_CACHE.clear();
        }
        ANSI_CODES_CACHE.put(cacheKey, codes);
        return codes;
    }

    /**
     * Downgrade a color system to a system with fewer colors.
     */
    public Color downgrade(ColorSystem system) {
        String cacheKey = name + ":" + type + ":" + system;
        Color cached = DOWNGRADE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Color result;
        if (type == ColorType.DEFAULT || type.getValue() == system.getValue()) {
            result = this;
        } else if (system == ColorSystem.EIGHT_BIT && getSystem() == ColorSystem.TRUECOLOR) {
            result = downgradeTruecolorToEightBit();
        } else if (system == ColorSystem.STANDARD) {
            result = downgradeToStandard();
        } else if (system == ColorSystem.WINDOWS) {
            result = downgradeToWindows();
        } else {
            result = this;
        }

        if (DOWNGRADE_CACHE.size() > 1024) {
            DOWNGRADE_CACHE.clear();
        }
        DOWNGRADE_CACHE.put(cacheKey, result);
        return result;
    }

    private Color downgradeTruecolorToEightBit() {
        float[] norm = triplet.normalized();
        float r = norm[0], g = norm[1], b = norm[2];

        // Convert to HLS to check saturation
        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float l = (max + min) / 2f;
        float s;
        if (max == min) {
            s = 0f;
        } else {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
        }

        // If saturation is under 15% assume it is grayscale
        if (s < 0.15f) {
            int gray = Math.round(l * 25f);
            int colorNumber;
            if (gray == 0) {
                colorNumber = 16;
            } else if (gray == 25) {
                colorNumber = 231;
            } else {
                colorNumber = 231 + gray;
            }
            return new Color(name, ColorType.EIGHT_BIT, colorNumber);
        }

        int red = triplet.red(), green = triplet.green(), blue = triplet.blue();
        float sixRed = red < 95 ? red / 95f : 1f + (red - 95) / 40f;
        float sixGreen = green < 95 ? green / 95f : 1f + (green - 95) / 40f;
        float sixBlue = blue < 95 ? blue / 95f : 1f + (blue - 95) / 40f;

        int colorNumber = 16 + 36 * Math.round(sixRed) + 6 * Math.round(sixGreen) + Math.round(sixBlue);
        return new Color(name, ColorType.EIGHT_BIT, colorNumber);
    }

    private Color downgradeToStandard() {
        ColorTriplet t;
        if (getSystem() == ColorSystem.TRUECOLOR) {
            t = triplet;
        } else {
            t = Palettes.EIGHT_BIT_PALETTE.get(number);
        }
        int colorNumber = Palettes.STANDARD_PALETTE.match(t);
        return new Color(name, ColorType.STANDARD, colorNumber);
    }

    private Color downgradeToWindows() {
        ColorTriplet t;
        if (getSystem() == ColorSystem.TRUECOLOR) {
            t = triplet;
        } else {
            if (number < 16) {
                return new Color(name, ColorType.WINDOWS, number);
            }
            t = Palettes.EIGHT_BIT_PALETTE.get(number);
        }
        int colorNumber = Palettes.WINDOWS_PALETTE.match(t);
        return new Color(name, ColorType.WINDOWS, colorNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Color other)) return false;
        return name.equals(other.name)
            && type == other.type
            && (Objects.equals(number, other.number))
            && (Objects.equals(triplet, other.triplet));
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (triplet != null ? triplet.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Color(");
        sb.append(name).append(", ").append(type);
        if (number != null) sb.append(", number=").append(number);
        if (triplet != null) sb.append(", triplet=").append(triplet);
        sb.append(")");
        return sb.toString();
    }

    /**
     * Get the ANSI_COLOR_NAMES map.
     */
    public static Map<String, Integer> getAnsiColorNames() {
        return ANSI_COLOR_NAMES;
    }
}
