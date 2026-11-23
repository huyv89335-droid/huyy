package vn.autobuilder.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Chuyển đổi giữa "Name" + "Properties" trong palette schematic  <->  BlockState của game.
 */
public final class BlockStateUtil {

    private BlockStateUtil() {
    }

    /** Ví dụ: parse("minecraft:oak_stairs", Properties{facing:"east", half:"top"}) */
    public static BlockState parse(String fullName, NbtParser.Tag properties) {
        String id = fullName == null ? "" : fullName.trim();
        if (id.isEmpty()) return null;
        int colon = id.indexOf(':');
        String namespace = colon > 0 ? id.substring(0, colon) : "minecraft";
        String path = colon > 0 ? id.substring(colon + 1) : id;

        Identifier identifier;
        try {
            identifier = Identifier.of(namespace, path);
        } catch (Exception e) {
            return null;
        }

        Block block = Registries.BLOCK.get(identifier);
        if (block == null) return null;

        BlockState state = block.getDefaultState();

        if (properties != null && properties.isCompound()) {
            for (String key : properties.keys()) {
                String value = properties.getString(key, "");
                if (value.isEmpty()) continue;
                Property<?> prop = findProperty(state, key);
                if (prop != null) state = withValue(state, prop, value);
            }
        }
        return state;
    }

    private static Property<?> findProperty(BlockState state, String name) {
        for (Property<?> p : state.getProperties()) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

    /** Dùng raw type để tránh rắc rối generic của Property<T>. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState withValue(BlockState state, Property prop, String value) {
        try {
            Object parsed = prop.parse(value).orElse(null);
            if (parsed != null) return state.with(prop, (Comparable) parsed);
        } catch (Exception ignored) {
        }
        return state;
    }

    /** Xoay BlockState theo số bước 90° (dùng chung với xoay toạ độ). */
    public static BlockState rotate(BlockState state, int steps) {
        if (state == null) return null;
        switch (steps & 3) {
            case 1: return state.rotate(Rotation.CLOCKWISE_90);
            case 2: return state.rotate(Rotation.CLOCKWISE_180);
            case 3: return state.rotate(Rotation.COUNTERCLOCKWISE_90);
            default: return state;
        }
    }

    /** Item cần cầm trên tay để đặt block này. */
    public static Item itemOf(BlockState state) {
        if (state == null) return Items.AIR;
        Item item = state.getBlock().asItem();
        return item == null ? Items.AIR : item;
    }

    public static boolean isAirLike(BlockState state) {
        return state == null || state.isAir();
    }

    public static Vec3d center(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /**
     * Góc nhìn (yaw, pitch) cần có để nhìn từ điểm A sang điểm B.
     * yaw/pitch theo đúng quy ước của Minecraft.
     */
    public static float[] anglesTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(horizontal, 1.0E-4D))));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0F, 90.0F)};
    }

    /** Direction gần nhất giữa 2 điểm (chỉ xét phương ngang). */
    public static Direction horizontalTowards(double dx, double dz) {
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
