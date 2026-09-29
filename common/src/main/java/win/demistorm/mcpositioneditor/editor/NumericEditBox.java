package win.demistorm.mcpositioneditor.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

// 26.1 dropped EditBox#setFilter
public class NumericEditBox extends EditBox {

    public NumericEditBox(Font font, int x, int y, int width, int height, Component title) {
        super(font, x, y, width, height, title);
    }

    @Override
    public void insertText(String text) {
        String before = getValue();
        super.insertText(text);
        if (!valid(getValue())) {
            setValue(before);
        }
    }

    private static boolean valid(String s) {
        return s.isEmpty() || s.equals("-") || s.matches("-?\\d*(\\.\\d*)?");
    }
}
