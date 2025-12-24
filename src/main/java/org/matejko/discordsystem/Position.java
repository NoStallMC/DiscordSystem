package main.java.org.matejko.discordsystem;

public class Position {
    public static final Position ORIGIN = new Position(0, 0, 0);
    private final double x;
    private final double y;
    private final double z;
    
    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public static Position fromLocation(org.bukkit.Location location) {
        return new Position(
            location.getX(),
            location.getY(),
            location.getZ()
        );
    }
    @Override
    public String toString() {
        return String.format("(%.1f, %.1f, %.1f)", x, y, z);
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
}
