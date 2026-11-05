/*
 * Порт Fokuso (https://github.com/QazCetelic/Fokuso), © QazCetelic, LGPL-3.0.
 * Адаптация для Minecraft 1.21.1 - zvezdochkin.
 */
package ru.zvezdochkin.filters.mixin;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zvezdochkin.filters.client.ChatFilterSystem;

/**
 * Перехватывает все сообщения, попадающие в чат.
 *
 * <p>Инжект стоит в 3-аргументном {@code addMessage}: 1-аргументный сам
 * делегирует в него, а подписанный чат игроков зовёт его напрямую. Так мы
 * ловим и системные сообщения модов, и обычный чат.</p>
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zvezdochkinFilters$filterChat(Component message, MessageSignature signature,
                                               GuiMessageTag tag, CallbackInfo ci) {
        if (ChatFilterSystem.filter(message)) {
            ci.cancel();
        }
    }
}
