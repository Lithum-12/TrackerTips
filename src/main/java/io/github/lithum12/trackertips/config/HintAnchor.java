package io.github.lithum12.trackertips.config;

/**
 * Where the hint popup stack is anchored on screen. Feature: previously hints were always
 * bottom-left only; this lets {@code TTClientConfig#ANCHOR} (and the in-game config GUI) place
 * them at any of the eight common HUD corners/edges instead.
 *
 * <p>{@link #horizontal()} and {@link #vertical()} describe which screen edge(s) the anchor point
 * sits on; {@link #growsDown()} describes which direction newly-stacked hints extend in, chosen
 * so the stack always grows away from whichever screen edge it's anchored to (bottom-anchored
 * stacks grow upward, top- and middle-anchored stacks grow downward) rather than off the edge.
 *
 * <p>{@code TTClientConfig#OFFSET_X}/{@code OFFSET_Y} are interpreted relative to whichever edge(s)
 * the chosen anchor touches; they have no effect on whichever axis is centered (e.g. offset_x is
 * ignored for {@link #TOP}/{@link #BOTTOM}, whose horizontal position is always centered).
 */
public enum HintAnchor {
    BOTTOM_LEFT(Horizontal.LEFT, Vertical.BOTTOM),
    LEFT(Horizontal.LEFT, Vertical.MIDDLE),
    TOP_LEFT(Horizontal.LEFT, Vertical.TOP),
    TOP(Horizontal.CENTER, Vertical.TOP),
    TOP_RIGHT(Horizontal.RIGHT, Vertical.TOP),
    RIGHT(Horizontal.RIGHT, Vertical.MIDDLE),
    BOTTOM_RIGHT(Horizontal.RIGHT, Vertical.BOTTOM),
    /** Horizontally centered, anchored to the bottom of the screen - i.e. directly above the hotbar. */
    BOTTOM(Horizontal.CENTER, Vertical.BOTTOM);

    public enum Horizontal { LEFT, CENTER, RIGHT }
    public enum Vertical { TOP, MIDDLE, BOTTOM }

    private final Horizontal horizontal;
    private final Vertical vertical;

    HintAnchor(Horizontal horizontal, Vertical vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public Horizontal horizontal() { return horizontal; }
    public Vertical vertical() { return vertical; }

    /** Whether newly-stacked hints should extend downward (true) or upward (false) from the anchor point. */
    public boolean growsDown() { return vertical != Vertical.BOTTOM; }

    /** Whether this anchor touches the screen's left edge - the only side vanilla chat can overlap. */
    public boolean touchesLeftEdge() { return horizontal == Horizontal.LEFT; }
}
