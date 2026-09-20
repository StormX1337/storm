package xyz.stormclient.target;

/** What a combat module is allowed to look at. */
public final class TargetFilter {

    public boolean players   = true;
    public boolean mobs      = false;
    public boolean animals   = false;
    public boolean invisible = false;
    public boolean teammates = false;
    public boolean friends   = false;
    public boolean dead      = false;
    public boolean npcs      = false;
    public double  range     = 3.0;
    public double  wallRange = 3.0;
    public boolean throughWalls = true;

    public TargetFilter range(double range) { this.range = range; this.wallRange = range; return this; }
    public TargetFilter players(boolean v)  { this.players = v; return this; }
    public TargetFilter mobs(boolean v)     { this.mobs = v; return this; }
    public TargetFilter animals(boolean v)  { this.animals = v; return this; }
    public TargetFilter invisible(boolean v){ this.invisible = v; return this; }
    public TargetFilter teammates(boolean v){ this.teammates = v; return this; }
    public TargetFilter friends(boolean v)  { this.friends = v; return this; }
    public TargetFilter npcs(boolean v)     { this.npcs = v; return this; }
}
