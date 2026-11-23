package vn.autobuilder.builder;

import net.minecraft.block.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bản vẽ đã được đọc xong: danh sách block + kích thước khối bao.
 */
public final class Blueprint {

    public final String fileName;
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;
    /** Toạ độ local 0..size-1, đã chuẩn hoá về góc (0,0,0). */
    public final List<Placement> placements;
    public final int unknownBlocks;

    public Blueprint(String fileName, int sizeX, int sizeY, int sizeZ,
                     List<Placement> placements, int unknownBlocks) {
        this.fileName = fileName;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.placements = Collections.unmodifiableList(new ArrayList<>(placements));
        this.unknownBlocks = unknownBlocks;
    }

    /** Kích thước sau khi xoay (chỉ đổi chỗ X <-> Z với 90°/270°). */
    public int rotatedSizeX(int rotation) {
        return (rotation & 1) == 1 ? sizeZ : sizeX;
    }

    public int rotatedSizeZ(int rotation) {
        return (rotation & 1) == 1 ? sizeX : sizeZ;
    }

    public int size() {
        return placements.size();
    }

    public String describe() {
        return fileName + " - " + sizeX + "x" + sizeY + "x" + sizeZ + " - " + placements.size() + " block";
    }

    public List<String> summary() {
        List<String> lines = new ArrayList<>();
        lines.add("File: " + fileName);
        lines.add("Kich thuoc: " + sizeX + " x " + sizeY + " x " + sizeZ);
        lines.add("So block can dat: " + placements.size());
        if (unknownBlocks > 0) lines.add("Block khong nhan dien duoc: " + unknownBlocks);
        return lines;
    }

    /** Đếm nhanh số block theo loại (dùng cho báo cáo thiếu đồ). */
    public int count(BlockState state) {
        int n = 0;
        for (Placement p : placements) {
            if (p.state.getBlock() == state.getBlock()) n++;
        }
        return n;
    }
}
