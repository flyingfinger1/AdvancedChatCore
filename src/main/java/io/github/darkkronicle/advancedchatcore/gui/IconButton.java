package io.github.darkkronicle.advancedchatcore.gui;

import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.Colors;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public class IconButton extends CleanButton {

    @Setter
    @Getter
    private int padding;

    @Setter
    @Getter
    private Identifier icon;

    @Setter
    @Getter
    private int iconWidth;

    @Setter
    @Getter
    private int iconHeight;

    @Setter
    @Getter
    private Consumer<IconButton> onClick;

    @Getter
    @Setter
    private String onHover;

    public IconButton(int x, int y, int sideLength, int iconLength, Identifier icon, Consumer<IconButton> mouseClick) {
        this(x, y, sideLength, sideLength, iconLength, iconLength, icon, mouseClick);
    }

    public IconButton(int x, int y, int width, int height, int iconWidth, int iconHeight, Identifier icon, Consumer<IconButton> mouseClick) {
        this(x, y, width, height, 2, iconWidth, iconHeight, icon, mouseClick, null);
    }

    public IconButton(int x, int y, int width, int height, int padding, int iconWidth, int iconHeight, Identifier icon, Consumer<IconButton> mouseClick, String onHover) {
        super(x, y, width, height, null, null);
        this.padding = padding;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        this.icon = icon;
        this.onClick = mouseClick;
        this.onHover = onHover;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean unused) {
        int relMX = mouseX - x;
        int relMY = mouseY - y;
        hovered = relMX >= 0 && relMX <= width && relMY >= 0 && relMY <= height;

        Color plusBack = Colors.getInstance().getColorOrWhite("background").withAlpha(100);
        if (hovered) {
            plusBack = Colors.getInstance().getColorOrWhite("hover").withAlpha(plusBack.alpha());
        }

        RenderUtils.drawRect(ctx, x, y, width, height, plusBack.color());

        // Draw the whole icon texture scaled into the padded button area. This blit overload is
        // blit(id, x1, y1, x2, y2, minU, maxU, minV, maxV) with corner coords and normalized
        // (0..1) UVs, which is the 26.x equivalent of the old DrawableHelper.drawTexture that
        // scaled an explicitly-sized texture. MaLiLib's drawTexturedRect samples 1:1, so cannot scale.
        ctx.blit(icon, x + padding, y + padding, x + width - padding, y + height - padding,
                0.0f, 1.0f, 0.0f, 1.0f);

        if (hovered && onHover != null) {
            RenderUtils.drawCenteredString(ctx, mouseX + 4, mouseY - 16,
                    Colors.getInstance().getColorOrWhite("white").color(), onHover);
        }
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        this.mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        onClick.accept(this);
        return true;
    }

}
