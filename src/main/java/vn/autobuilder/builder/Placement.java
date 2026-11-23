package vn.autobuilder.builder;

import net.minecraft.block.BlockState;

/**
 * 1 block trong bản vẽ, toạ độ LOCAL (bắt đầu từ 0,0,0) của bản vẽ.
 */
public final class Placement {
    public final int x;
    public final int y;
    public final int z;
    public final BlockState state;

    public Placement(int x, int y, int z, BlockState state) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.state = state;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }
}
