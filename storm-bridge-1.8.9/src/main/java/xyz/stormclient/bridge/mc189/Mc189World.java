package xyz.stormclient.bridge.mc189;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import xyz.stormclient.bridge.IEntity;
import xyz.stormclient.bridge.IWorld;

public final class Mc189World implements IWorld {

    private final WorldClient world;

    public Mc189World(WorldClient world) { this.world = world; }

    public WorldClient handle() { return world; }

    @Override public List<IEntity> entities() {
        List<IEntity> out = new ArrayList<IEntity>();
        for (Entity entity : world.loadedEntityList) out.add(Mc189Entities.wrap(entity));
        return out;
    }

    @Override public List<IEntity> players() {
        List<IEntity> out = new ArrayList<IEntity>();
        for (Entity entity : world.playerEntities) out.add(Mc189Entities.wrap(entity));
        return out;
    }

    @Override public IEntity entityById(int id) {
        Entity entity = world.getEntityByID(id);
        return entity == null ? null : Mc189Entities.wrap(entity);
    }

    private IBlockState state(int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z));
    }

    @Override public String blockName(int x, int y, int z) {
        Block block = state(x, y, z).getBlock();
        return String.valueOf(Block.blockRegistry.getNameForObject(block));
    }

    @Override public int blockId(int x, int y, int z) {
        return Block.getIdFromBlock(state(x, y, z).getBlock());
    }

    @Override public boolean isAir(int x, int y, int z) {
        return state(x, y, z).getBlock().getMaterial() == Material.air;
    }

    @Override public boolean isLiquid(int x, int y, int z) {
        return state(x, y, z).getBlock().getMaterial().isLiquid();
    }

    @Override public boolean isSolid(int x, int y, int z) {
        return state(x, y, z).getBlock().getMaterial().isSolid();
    }

    @Override public boolean isReplaceable(int x, int y, int z) {
        Material material = state(x, y, z).getBlock().getMaterial();
        return material == Material.air || material.isReplaceable();
    }

    @Override public boolean isContainer(int x, int y, int z) {
        Object tile = world.getTileEntity(new BlockPos(x, y, z));
        return tile instanceof TileEntityChest
                || tile instanceof TileEntityEnderChest
                || tile instanceof TileEntityFurnace;
    }

    @Override public float slipperiness(int x, int y, int z) {
        return state(x, y, z).getBlock().slipperiness;
    }

    @Override public long worldTime() { return world.getWorldTime(); }
    @Override public int dimension()  { return world.provider.getDimensionId(); }
    @Override public int minY()       { return 0; }
    @Override public int maxY()       { return 256; }

    @Override public int[] raytraceBlock(double reach) {
        MovingObjectPosition hit = Minecraft.getMinecraft().thePlayer.rayTrace(reach, 1.0F);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        BlockPos pos = hit.getBlockPos();
        return new int[] { pos.getX(), pos.getY(), pos.getZ(), hit.sideHit.getIndex() };
    }

    @Override public boolean collides(double minX, double minY, double minZ,
                                      double maxX, double maxY, double maxZ) {
        AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
        return !world.getCollidingBoundingBoxes(Minecraft.getMinecraft().thePlayer, box).isEmpty();
    }

    /** Straight line check between two points, used by the ESP wall test. */
    public boolean canSee(double x1, double y1, double z1, double x2, double y2, double z2) {
        return world.rayTraceBlocks(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2)) == null;
    }
}
