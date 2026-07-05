package com.justnothing.richconsole.style;

import java.util.Map;

import com.justnothing.richconsole.color.Color;

/**
 * Builder for constructing Style instances.
 * Ported from rich/style.py.
 */
public class StyleBuilder {

    private Color color;
    private Color bgcolor;
    private Boolean bold;
    private Boolean dim;
    private Boolean italic;
    private Boolean underline;
    private Boolean blink;
    private Boolean blink2;
    private Boolean reverse;
    private Boolean conceal;
    private Boolean strike;
    private Boolean underline2;
    private Boolean frame;
    private Boolean encircle;
    private Boolean overline;
    private String link;
    private Map<String, Object> meta;

    StyleBuilder() {
    }

    public StyleBuilder color(Color color) {
        this.color = color;
        return this;
    }

    public StyleBuilder color(String color) {
        this.color = color != null ? Color.parse(color) : null;
        return this;
    }

    public StyleBuilder bgcolor(Color bgcolor) {
        this.bgcolor = bgcolor;
        return this;
    }

    public StyleBuilder bgcolor(String bgcolor) {
        this.bgcolor = bgcolor != null ? Color.parse(bgcolor) : null;
        return this;
    }

    public StyleBuilder bold(Boolean bold) {
        this.bold = bold;
        return this;
    }

    public StyleBuilder dim(Boolean dim) {
        this.dim = dim;
        return this;
    }

    public StyleBuilder italic(Boolean italic) {
        this.italic = italic;
        return this;
    }

    public StyleBuilder underline(Boolean underline) {
        this.underline = underline;
        return this;
    }

    public StyleBuilder blink(Boolean blink) {
        this.blink = blink;
        return this;
    }

    public StyleBuilder blink2(Boolean blink2) {
        this.blink2 = blink2;
        return this;
    }

    public StyleBuilder reverse(Boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public StyleBuilder conceal(Boolean conceal) {
        this.conceal = conceal;
        return this;
    }

    public StyleBuilder strike(Boolean strike) {
        this.strike = strike;
        return this;
    }

    public StyleBuilder underline2(Boolean underline2) {
        this.underline2 = underline2;
        return this;
    }

    public StyleBuilder frame(Boolean frame) {
        this.frame = frame;
        return this;
    }

    public StyleBuilder encircle(Boolean encircle) {
        this.encircle = encircle;
        return this;
    }

    public StyleBuilder overline(Boolean overline) {
        this.overline = overline;
        return this;
    }

    public StyleBuilder link(String link) {
        this.link = link;
        return this;
    }

    public StyleBuilder meta(Map<String, Object> meta) {
        this.meta = meta;
        return this;
    }

    public Style build() {
        return new Style(this);
    }

    // Package-private getters for Style constructor
    Color getColor() {
        return color;
    }

    Color getBgcolor() {
        return bgcolor;
    }

    Boolean getBold() {
        return bold;
    }

    Boolean getDim() {
        return dim;
    }

    Boolean getItalic() {
        return italic;
    }

    Boolean getUnderline() {
        return underline;
    }

    Boolean getBlink() {
        return blink;
    }

    Boolean getBlink2() {
        return blink2;
    }

    Boolean getReverse() {
        return reverse;
    }

    Boolean getConceal() {
        return conceal;
    }

    Boolean getStrike() {
        return strike;
    }

    Boolean getUnderline2() {
        return underline2;
    }

    Boolean getFrame() {
        return frame;
    }

    Boolean getEncircle() {
        return encircle;
    }

    Boolean getOverline() {
        return overline;
    }

    String getLink() {
        return link;
    }

    Map<String, Object> getMeta() {
        return meta;
    }
}
