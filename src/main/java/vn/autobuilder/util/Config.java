package vn.autobuilder.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Cau hinh luu tai config/autobuilder.properties (dinh dang properties de
 * khong can thu vien JSON nao het).
 */
public final class Config {

    private static final Config INSTANCE = new Config();

    public static Config get() { return INSTANCE; }

    /* --- Thong so engine --- */
    public int placeDelay = 2;            // tick nghi giua 2 lan dat block
    public double reach = 4.5D;           // tam dat block
    public float lookSpeed = 14.0F;       // so do camera quay duoc moi tick (nho = muot)
    public boolean autoSprint = true;
    public boolean autoJump = true;
    public boolean sneakPlace = true;     // snea khi dat de khong mo ngan ruong / dong cua
    public int chestRestockDelay = 8;

    /* --- Thong so luu lai de lan sau khong phai nhap lai --- */
    public int lastX = 0, lastY = 64, lastZ = 0;
    public int chestX = 0, chestY = 64, chestZ = 0;
    public boolean hasChest = false;
    public int rotation = 0;
    public String lastFile = "";

    private Config() {
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("autobuilder.properties");
    }

    public static Path schematicsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("schematics");
    }

    public void load() {
        Properties p = new Properties();
        Path file = configPath();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (Exception ignored) {
            }
        }
        placeDelay = parseInt(p.getProperty("placeDelay"), 2);
        reach = parseDouble(p.getProperty("reach"), 4.5D);
        lookSpeed = (float) parseDouble(p.getProperty("lookSpeed"), 14.0D);
        autoSprint = parseBool(p.getProperty("autoSprint"), true);
        autoJump = parseBool(p.getProperty("autoJump"), true);
        sneakPlace = parseBool(p.getProperty("sneakPlace"), true);
        chestRestockDelay = parseInt(p.getProperty("chestRestockDelay"), 8);
        lastX = parseInt(p.getProperty("lastX"), 0);
        lastY = parseInt(p.getProperty("lastY"), 64);
        lastZ = parseInt(p.getProperty("lastZ"), 0);
        chestX = parseInt(p.getProperty("chestX"), 0);
        chestY = parseInt(p.getProperty("chestY"), 64);
        chestZ = parseInt(p.getProperty("chestZ"), 0);
        hasChest = parseBool(p.getProperty("hasChest"), false);
        rotation = parseInt(p.getProperty("rotation"), 0);
        lastFile = p.getProperty("lastFile", "");
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("placeDelay", String.valueOf(placeDelay));
        p.setProperty("reach", String.valueOf(reach));
        p.setProperty("lookSpeed", String.valueOf(lookSpeed));
        p.setProperty("autoSprint", String.valueOf(autoSprint));
        p.setProperty("autoJump", String.valueOf(autoJump));
        p.setProperty("sneakPlace", String.valueOf(sneakPlace));
        p.setProperty("chestRestockDelay", String.valueOf(chestRestockDelay));
        p.setProperty("lastX", String.valueOf(lastX));
        p.setProperty("lastY", String.valueOf(lastY));
        p.setProperty("lastZ", String.valueOf(lastZ));
        p.setProperty("chestX", String.valueOf(chestX));
        p.setProperty("chestY", String.valueOf(chestY));
        p.setProperty("chestZ", String.valueOf(chestZ));
        p.setProperty("hasChest", String.valueOf(hasChest));
        p.setProperty("rotation", String.valueOf(rotation));
        p.setProperty("lastFile", lastFile);
        try (OutputStream out = Files.newOutputStream(configPath())) {
            p.store(out, "AutoBuilder config");
        } catch (Exception ignored) {
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    private static boolean parseBool(String s, boolean def) {
        if (s == null) return def;
        return s.trim().equalsIgnoreCase("true");
    }
}
