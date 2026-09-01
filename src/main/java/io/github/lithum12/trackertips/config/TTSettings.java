package io.github.lithum12.trackertips.config;

public class TTSettings {

    public boolean enable = true;
    public int checkInterval = 20;
    public int maxActiveHints = 3;
    public int defaultDuration = 240;
    public boolean debug = false;

    /** When true, players may use "/tt" as a shorthand for "/trackertips". Off by default. */
    public boolean shortcutCommand = false;
}