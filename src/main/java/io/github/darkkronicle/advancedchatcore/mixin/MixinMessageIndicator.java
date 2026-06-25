package io.github.darkkronicle.advancedchatcore.mixin;

import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 26.2: Yarn MessageIndicator is now the record net.minecraft.client.multiplayer.chat.GuiMessageTag.
@Mixin(GuiMessageTag.class)
public class MixinMessageIndicator {

    // 26.2: indicatorColor() is now the record accessor (returns the stored int). Overriding it at
    // HEAD with setReturnValue still works. Yarn loggedName() is now logTag().
    @Inject(method = "indicatorColor", at = @At("HEAD"), cancellable = true)
    private void getColor(CallbackInfoReturnable<Integer> ci) {
        GuiMessageTag indicator = ((GuiMessageTag) (Object) this);
        String name = indicator.logTag();
        ci.setReturnValue(switch (name) {
            case "Modified" -> ConfigStorage.ChatScreen.MODIFIED.config.getColor().intValue;
            case "Filtered" -> ConfigStorage.ChatScreen.FILTERED.config.getColor().intValue;
            case "Not Secure" -> ConfigStorage.ChatScreen.NOT_SECURE.config.getColor().intValue;
            default -> // And "System"
                    ConfigStorage.ChatScreen.SYSTEM.config.getColor().intValue;
        });

    }

    // 26.2: icon() is now the record accessor; return type MessageIndicator.Icon -> GuiMessageTag.Icon.
    @Inject(method = "icon", at = @At("HEAD"), cancellable = true)
    private void getIcon(CallbackInfoReturnable<GuiMessageTag.Icon> ci) {
        if (!ConfigStorage.ChatScreen.SHOW_CHAT_ICONS.config.getBooleanValue()) {
            ci.setReturnValue(null);
        }
    }

}
