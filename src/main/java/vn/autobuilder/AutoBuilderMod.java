package vn.autobuilder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.autobuilder.builder.BuilderEngine;
import vn.autobuilder.util.Config;

/**
 * Diem vao chinh (chay ca client + server).
 * Phan logic xay block chi chay o client nen o day chi khoi tao config.
 */
public class AutoBuilderMod implements ModInitializer {

    public static final String MOD_ID = "autobuilder";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoBuilder");

    @Override
    public void onInitialize() {
        Config.get().load();
        BuilderEngine.get();
        LOGGER.info("[AutoBuilder] Khoi tao xong. Thu muc ban ve: " + Config.schematicsDir());
    }
}
