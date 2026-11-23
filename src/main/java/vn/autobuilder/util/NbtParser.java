package vn.autobuilder.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Trinh doc NBT (Named Binary Tag) thuần Java.
 *
 * Viết riêng để KHÔNG phụ thuộc tên class NBT của Minecraft (NbtCompound, NbtList,
 * NbtIo, NbtSizeTracker...). Nhờ vậy file này không bao giờ bị lỗi compile dù
 * Mojang/Yarn đổi tên API trong các bản update mới.
 */
public final class NbtParser {

    public static final int TAG_END = 0;
    public static final int TAG_BYTE = 1;
    public static final int TAG_SHORT = 2;
    public static final int TAG_INT = 3;
    public static final int TAG_LONG = 4;
    public static final int TAG_FLOAT = 5;
    public static final int TAG_DOUBLE = 6;
    public static final int TAG_BYTE_ARRAY = 7;
    public static final int TAG_STRING = 8;
    public static final int TAG_LIST = 9;
    public static final int TAG_COMPOUND = 10;
    public static final int TAG_INT_ARRAY = 11;
    public static final int TAG_LONG_ARRAY = 12;

    /** Giới hạn an toàn cho điện thoại cấu hình thấp (200 MB). */
    private static final int MAX_BYTES = 200 * 1024 * 1024;

    public static final Tag EMPTY = new Tag(TAG_END, null);

    private NbtParser() {
    }

    /* ---------------- Tag (node dữ liệu) ---------------- */

    public static final class Tag {
        public final int type;
        public final Object value;

        Tag(int type, Object value) {
            this.type = type;
            this.value = value;
        }

        public boolean isCompound() { return type == TAG_COMPOUND; }
        public boolean isList() { return type == TAG_LIST; }
        public boolean isNumber() { return type >= TAG_BYTE && type <= TAG_DOUBLE; }
        public boolean isMissing() { return this == EMPTY; }

        @SuppressWarnings("unchecked")
        public Map<String, Tag> compound() {
            if (type == TAG_COMPOUND && value instanceof Map) return (Map<String, Tag>) value;
            return Collections.emptyMap();
        }

        @SuppressWarnings("unchecked")
        public List<Tag> list() {
            if (type == TAG_LIST && value instanceof List) return (List<Tag>) value;
            return Collections.emptyList();
        }

        public Set<String> keys() { return compound().keySet(); }
        public int size() {
            if (isCompound()) return compound().size();
            if (isList()) return list().size();
            return 0;
        }

        public Tag get(String key) {
            Tag t = compound().get(key);
            return t == null ? EMPTY : t;
        }

        public List<Tag> getList(String key) { return get(key).list(); }

        public String getString(String key, String def) {
            Tag t = get(key);
            if (t.type == TAG_STRING && t.value instanceof String) return (String) t.value;
            if (t.isNumber()) return String.valueOf(t.value);
            return def;
        }

        public String asString() {
            if (type == TAG_STRING && value instanceof String) return (String) value;
            return "";
        }

        public long asLong() {
            if (value instanceof Number) return ((Number) value).longValue();
            if (value instanceof String) {
                try { return Long.parseLong((String) value); } catch (Exception ignored) { }
            }
            return 0L;
        }

        public int asInt() { return (int) asLong(); }

        public double asDouble() {
            if (value instanceof Number) return ((Number) value).doubleValue();
            return 0.0D;
        }

        public int getInt(String key, int def) {
            Tag t = get(key);
            return t.isNumber() ? t.asInt() : def;
        }

        public long getLong(String key, long def) {
            Tag t = get(key);
            return t.isNumber() ? t.asLong() : def;
        }

        public double getDouble(String key, double def) {
            Tag t = get(key);
            return t.isNumber() ? t.asDouble() : def;
        }

        public byte[] getByteArray(String key) {
            Tag t = get(key);
            if (t.type == TAG_BYTE_ARRAY && t.value instanceof byte[]) return (byte[]) t.value;
            return new byte[0];
        }

        public int[] getIntArray(String key) {
            Tag t = get(key);
            if (t.type == TAG_INT_ARRAY && t.value instanceof int[]) return (int[]) t.value;
            return new int[0];
        }

        public long[] getLongArray(String key) {
            Tag t = get(key);
            if (t.type == TAG_LONG_ARRAY && t.value instanceof long[]) return (long[]) t.value;
            return new long[0];
        }
    }

    /* ---------------- Đọc file ---------------- */

    /** Đọc 1 file NBT, tự nhận biết gzip (mọi file .litematic / .schem đều nén gzip). */
    public static Tag readFile(Path file) throws IOException {
        byte[] all = Files.readAllBytes(file);
        if (all.length > MAX_BYTES) throw new IOException("File qua lon (" + all.length + " byte)");
        byte[] data = all;

        if (all.length > 2 && (all[0] & 0xFF) == 0x1F && (all[1] & 0xFF) == 0x8B) {
            try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(all), 65536)) {
                data = gz.readAllBytes();
            } catch (IOException io) {
                throw new IOException("File bi hong hoac khong phai NBT: " + io.getMessage());
            }
        }
        return readRoot(data);
    }

    /** Đọc từ mảng byte đã giải nén. */
    public static Tag readRoot(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        int type = in.readUnsignedByte();
        if (type == TAG_END) throw new IOException("NBT rong");
        readName(in);
        return readPayload(in, type);
    }

    /* ---------------- Bộ giải NBT ---------------- */

    private static String readName(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        if (len <= 0) return "";
        byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private static Tag readPayload(DataInputStream in, int type) throws IOException {
        switch (type) {
            case TAG_BYTE:
                return new Tag(type, (byte) in.readByte());
            case TAG_SHORT:
                return new Tag(type, in.readShort());
            case TAG_INT:
                return new Tag(type, in.readInt());
            case TAG_LONG:
                return new Tag(type, in.readLong());
            case TAG_FLOAT:
                return new Tag(type, in.readFloat());
            case TAG_DOUBLE:
                return new Tag(type, in.readDouble());
            case TAG_BYTE_ARRAY: {
                int n = in.readInt();
                if (n < 0 || n > MAX_BYTES) throw new EOFException("ByteArray qua lon");
                byte[] b = new byte[n];
                in.readFully(b);
                return new Tag(type, b);
            }
            case TAG_STRING: {
                int n = in.readUnsignedShort();
                byte[] b = new byte[n];
                in.readFully(b);
                return new Tag(type, new String(b, StandardCharsets.UTF_8));
            }
            case TAG_LIST: {
                int elType = in.readUnsignedByte();
                int n = in.readInt();
                if (n < 0) n = 0;
                if (n > 8_000_000) throw new EOFException("List qua lon");
                if (elType == TAG_END || n == 0) return new Tag(TAG_LIST, Collections.emptyList());
                List<Tag> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(readPayload(in, elType));
                return new Tag(TAG_LIST, list);
            }
            case TAG_COMPOUND: {
                Map<String, Tag> map = new HashMap<>();
                while (true) {
                    int t = in.readUnsignedByte();
                    if (t == TAG_END) break;
                    String name = readName(in);
                    map.put(name, readPayload(in, t));
                }
                return new Tag(TAG_COMPOUND, map);
            }
            case TAG_INT_ARRAY: {
                int n = in.readInt();
                if (n < 0) n = 0;
                int[] a = new int[n];
                for (int i = 0; i < n; i++) a[i] = in.readInt();
                return new Tag(type, a);
            }
            case TAG_LONG_ARRAY: {
                int n = in.readInt();
                if (n < 0) n = 0;
                long[] a = new long[n];
                for (int i = 0; i < n; i++) a[i] = in.readLong();
                return new Tag(type, a);
            }
            default:
                throw new EOFException("Tag NBT khong ho tro: " + type);
        }
    }
}
