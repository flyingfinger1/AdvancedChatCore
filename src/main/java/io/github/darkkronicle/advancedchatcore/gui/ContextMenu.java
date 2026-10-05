package io.github.darkkronicle.advancedchatcore.gui;

import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.TextUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;

public class ContextMenu extends WidgetBase {

    private final LinkedHashMap<Component, ContextConsumer> options;
    private Component hoveredEntry = null;

    @Getter
    private final int contextX;

    @Getter
    private final int contextY;

    @Getter
    @Setter
    private Runnable close;

    @Setter
    @Getter
    private Color background;

    @Setter
    @Getter
    private Color hover;

    public ContextMenu(int x, int y, LinkedHashMap<Component, ContextConsumer> options, Runnable close) {
        this(x, y, options, close, new Color(0, 0, 0, 200), new Color(255, 255, 255, 100));
    }

    public ContextMenu(int x, int y, LinkedHashMap<Component, ContextConsumer> options, Runnable close, Color background, Color hover) {
        super(x, y, 10, 10);
        this.contextX = x;
        this.contextY = y;
        this.options = options;
        updateDimensions();
        this.close = close;
        this.background = background;
        this.hover = hover;
    }

    public void updateDimensions() {
        setWidth(TextUtil.getMaxLengthString(options.keySet().stream().map(Component::getString).toList()) + 4);
        setHeight(options.size() * (fontHeight + 2));
        int windowWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int windowHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (x + width > windowWidth) {
            x = windowWidth - width;
        }
        if (y + height > windowHeight) {
            y = windowHeight - height;
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        boolean success = super.onMouseClicked(mouseButtonEvent, doubleClick);
        if (success) {
            return true;
        }
        // Didn't click on this
        close.run();
        return false;
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        // 26.3: MouseButtonEvent.button() is in the InputConstants.MOUSE_BUTTON_* space (LEFT == 1),
        // not GLFW's (LEFT == 0). Comparing against 0 here silently dropped every context-menu entry
        // click, so the menu options did nothing.
        if (mouseButtonEvent.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (hoveredEntry == null) {
            return false;
        }
        options.get(hoveredEntry).takeAction(contextX, contextY);
        close.run();
        return true;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
        RenderUtils.drawRect(ctx, x, y, width, height, background.color());
        int rX = x + 2;
        int rY = y + 2;
        hoveredEntry = null;
        for (Component option : options.keySet()) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= rY - 2 && mouseY < rY + fontHeight + 1) {
                hoveredEntry = option;
                RenderUtils.drawRect(ctx, rX - 2, rY - 2, width, fontHeight + 2, hover.color());
            }
            drawStringWithShadow(ctx, rX, rY, -1, option.getString());
            rY += fontHeight + 2;
        }
    }

    public interface ContextConsumer  {
        void takeAction(int x, int y);
    }
}
