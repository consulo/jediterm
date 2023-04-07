/**
 * @author VISTALL
 * @since 07/04/2023
 */
module jediterm {
    requires static consulo.annotation;
    requires org.slf4j;
    requires java.desktop;

    exports com.jediterm.terminal.debug;
    exports com.jediterm.terminal.emulator.mouse;
    exports com.jediterm.terminal.emulator.charset;
    exports com.jediterm.terminal.model;
    exports com.jediterm.terminal.ui;
    exports com.jediterm.terminal.ui.settings;
    exports com.jediterm.terminal.util;
}