package xyz.stormclient.bridge;

import java.util.List;

public interface IWorld {

    List<IEntity> entities();
    List<IEntity> players();
    IEntity entityById(int id);

    String blockName(int x, int y, int z);
    int    blockId(int x, int y, int z);

    boolean isAir(int x, int y, int z);
    boolean isLiquid(int x, int y, int z);
    boolean isSolid(int x, int y, int z);
    boolean isReplaceable(int x, int y, int z);
    /** Chest, ender chest, shulker, furnace ... anything with a container GUI. */
    boolean isContainer(int x, int y, int z);

    /** Block friction, 0.6 for most blocks, 0.98 for ice. */
    float slipperiness(int x, int y, int z);

    long worldTime();
    int  dimension();
    int  minY();
    int  maxY();

    /** Raytrace from the player eyes, returns {x,y,z,face} or null. */
    int[] raytraceBlock(double reach);

    /** True when the given AABB collides with the world. */
    boolean collides(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
