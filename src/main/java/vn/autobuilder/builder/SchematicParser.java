package vn.autobuilder.builder;

import net.minecraft.block.BlockState;
import vn.autobuilder.util.BlockStateUtil;
import vn.autobuilder.util.NbtParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Doc 2 dinh dang ban ve:
 *   1) .litematic  - Litematica (Region + BlockStatePalette + BlockStates long[])
 *   2) .schem      - Sponge Schematic v2 / v3 (WorldEdit) - co Palette + BlockData varint
 *
 * Ket qua: Blueprint chua toa do local 0..size-1 + BlockState.
 */
public final class SchematicParser {

    private SchematicParser() {
    }

    public static Blueprint load(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        String lower = fileName.toLowerCase(Locale.ROOT);
        NbtParser.Tag root = NbtParser.readFile(file);

        if (lower.endsWith(".litematic")) {
            Blueprint bp = parseLitematic(root, fileName);
            if (bp != null) return bp;
            return finishOrThrow(fileName, parseSponge(root, fileName));
        }
        Blueprint sp = parseSponge(root, fileName);
        if (sp != null) return sp;
        return finishOrThrow(fileName, parseLitematic(root, fileName));
    }

    private static Blueprint finishOrThrow(String fileName, Blueprint bp) throws IOException {
        if (bp == null) {
            throw new IOException("Khong doc duoc block nao. Chi ho tro .litematic (Litematica) va .schem (WorldEdit/Sponge).");
        }
        return bp;
    }

    /* =====================================================================
     *  1) LITEMATIC
     * ===================================================================== */

    private static Blueprint parseLitematic(NbtParser.Tag root, String fileName) {
        try {
            NbtParser.Tag regions = root.get("Regions");
            if (!regions.isCompound()) return null;

            List<Placement> raw = new ArrayList<>();
            int unknown = 0;

            for (String regionName : regions.keys()) {
                NbtParser.Tag region = regions.get(regionName);
                if (!region.isCompound()) continue;

                NbtParser.Tag sizeTag = region.get("Size");
                int sx = Math.abs(sizeTag.getInt("x", 0));
                int sy = Math.abs(sizeTag.getInt("y", 0));
                int sz = Math.abs(sizeTag.getInt("z", 0));
                if (sx <= 0 || sy <= 0 || sz <= 0) continue;

                NbtParser.Tag posTag = region.get("Position");
                int px = posTag.getInt("x", 0);
                int py = posTag.getInt("y", 0);
                int pz = posTag.getInt("z", 0);

                // Palette
                List<BlockState> states = new ArrayList<>();
                for (NbtParser.Tag entry : region.getList("BlockStatePalette")) {
                    states.add(BlockStateUtil.parse(entry.getString("Name", ""), entry.get("Properties")));
                }
                if (states.isEmpty()) continue;

                long[] data = region.getLongArray("BlockStates");
                if (data.length == 0) continue;

                int total = sx * sy * sz;
                int bits = Math.max(2, ceilLog2(states.size()));
                LiteBitArray array = new LiteBitArray(data, bits, total);

                int index = 0;
                for (int y = 0; y < sy; y++) {
                    for (int z = 0; z < sz; z++) {
                        for (int x = 0; x < sx; x++) {
                            int id = array.getAt(index++);
                            BlockState st = (id >= 0 && id < states.size()) ? states.get(id) : null;
                            if (st == null) { unknown++; continue; }
                            if (st.isAir()) continue;
                            raw.add(new Placement(px + x, py + y, pz + z, st));
                        }
                    }
                }
            }

            if (raw.isEmpty()) return null;
            return normalize(fileName, raw, unknown);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Cach dong goi bit cua Litematica: cac entry dac lien tuc nhau, co the
     * vuoct qua ranh gioi cua 1 long (khac voi cach dong goi cua Minecraft).
     */
    private static final class LiteBitArray {
        private final long[] data;
        private final int bitsPerEntry;
        private final long mask;
        private final int capacity;

        LiteBitArray(long[] data, int bitsPerEntry, int capacity) {
            this.data = data;
            this.bitsPerEntry = Math.max(1, bitsPerEntry);
            this.mask = this.bitsPerEntry >= 64 ? -1L : ((1L << this.bitsPerEntry) - 1L);
            this.capacity = capacity;
        }

        int getAt(int index) {
            if (index < 0 || index >= capacity) return 0;
            long startOffset = (long) index * (long) bitsPerEntry;
            int startLong = (int) (startOffset >> 6);
            int endLong = (int) ((((long) index + 1L) * (long) bitsPerEntry - 1L) >> 6);
            int startBit = (int) (startOffset & 0x3FL);
            if (startLong < 0 || startLong >= data.length) return 0;
            if (startLong == endLong || endLong >= data.length) {
                return (int) ((data[startLong] >>> startBit) & mask);
            }
            int endOffset = 64 - startBit;
            return (int) ((data[startLong] >>> startBit | data[endLong] << endOffset) & mask);
        }
    }

    private static int ceilLog2(int value) {
        int bits = 1;
        int size = 2;
        while (size < value) {
            size <<= 1;
            bits++;
        }
        return bits;
    }

    /* =====================================================================
     *  2) SPONGE SCHEMATIC (.schem)
     * ===================================================================== */

    private static Blueprint parseSponge(NbtParser.Tag root, String fileName) {
        try {
            if (!root.isCompound()) return null;

            int version = root.getInt("Version", 0);
            NbtParser.Tag paletteTag = root.get("Palette");
            NbtParser.Tag dataTag = root.get("BlockData");
            if (version <= 0 || !paletteTag.isCompound() || dataTag.type != NbtParser.TAG_BYTE_ARRAY) return null;

            int w = Math.abs((int) root.get("Width").asLong());
            int h = Math.abs((int) root.get("Height").asLong());
            int l = Math.abs((int) root.get("Length").asLong());
            if (w <= 0 || h <= 0 || l <= 0) return null;
            if ((long) w * h * l > 64_000_000L) return null;

            int[] offset = root.getIntArray("Offset");
            int ox = offset.length >= 3 ? offset[0] : 0;
            int oy = offset.length >= 3 ? offset[1] : 0;
            int oz = offset.length >= 3 ? offset[2] : 0;

            Map<Integer, BlockState> states = new HashMap<>();
            int unknown = 0;
            for (String key : paletteTag.keys()) {
                NbtParser.Tag entry = paletteTag.get(key);
                int id = entry.isCompound() ? entry.getInt("Id", -1) : entry.asInt();
                if (id < 0) { unknown++; continue; }
                String name = entry.isCompound() && !entry.getString("Name", "").isEmpty()
                        ? entry.getString("Name", "") : key;
                NbtParser.Tag props = entry.isCompound() ? entry.get("Properties") : null;
                BlockState st = BlockStateUtil.parse(name, props);
                if (st == null) unknown++;
                states.put(id, st);
            }
            if (states.isEmpty()) return null;

            byte[] data = dataTag.value instanceof byte[] ? (byte[]) dataTag.value : new byte[0];
            VarReader reader = new VarReader(data);
            List<Placement> raw = new ArrayList<>();

            for (int y = 0; y < h; y++) {
                for (int z = 0; z < l; z++) {
                    for (int x = 0; x < w; x++) {
                        int id = reader.read();
                        BlockState st = states.get(id);
                        if (st == null) { unknown++; continue; }
                        if (st.isAir()) continue;
                        raw.add(new Placement(ox + x, oy + y, oz + z, st));
                    }
                }
            }

            if (raw.isEmpty()) return null;
            return normalize(fileName, raw, unknown);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Bo doc so nguyen bien doi do dai (varint) cua Sponge. */
    private static final class VarReader {
        private final byte[] data;
        private int pos = 0;

        VarReader(byte[] data) { this.data = data; }

        int read() {
            int value = 0;
            int shift = 0;
            while (pos < data.length) {
                byte b = data[pos++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
                if (shift > 28) break;
            }
            return value;
        }
    }

    /* =====================================================================
     *  Chung: dua toa do ve 0..size-1
     * ===================================================================== */

    private static Blueprint normalize(String fileName, List<Placement> raw, int unknown) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Placement p : raw) {
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.z < minZ) minZ = p.z;
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
            if (p.z > maxZ) maxZ = p.z;
        }

        List<Placement> list = new ArrayList<>(raw.size());
        for (Placement p : raw) {
            list.add(new Placement(p.x - minX, p.y - minY, p.z - minZ, p.state));
        }

        return new Blueprint(fileName,
                maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1, list, unknown);
    }
}
