package win.demistorm.mcpositioneditor;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MCPositionEditor {
    public static final String MOD_ID = "mcpositioneditor";
    public static final Logger log = LoggerFactory.getLogger(MOD_ID);

    public static final boolean debugMode = false;

    static {
        Configurator.setLevel(MOD_ID, debugMode ? Level.DEBUG : Level.INFO);
    }

    public static void initialize() {
        log.info("Easy Item Positions loaded!");
    }
}
