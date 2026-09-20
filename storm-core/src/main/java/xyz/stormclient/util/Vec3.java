package xyz.stormclient.util;

/** Minimal immutable 3D vector, keeps the core free of Minecraft's own math classes. */
public final class Vec3 {

    public final double x, y, z;

    public Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    public Vec3 add(double dx, double dy, double dz) { return new Vec3(x + dx, y + dy, z + dz); }
    public Vec3 add(Vec3 o)      { return new Vec3(x + o.x, y + o.y, z + o.z); }
    public Vec3 sub(Vec3 o)      { return new Vec3(x - o.x, y - o.y, z - o.z); }
    public Vec3 scale(double f)  { return new Vec3(x * f, y * f, z * f); }

    public double length() { return Math.sqrt(x * x + y * y + z * z); }

    public double distanceTo(Vec3 o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public Vec3 normalize() {
        double len = length();
        return len == 0 ? new Vec3(0, 0, 0) : new Vec3(x / len, y / len, z / len);
    }

    @Override public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}
