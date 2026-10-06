package net.xuwu.betterae2.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xuwu.betterae2.BetterAe2;
import org.lwjgl.glfw.GLFW;

/** Client key mappings for the two one-click deposit actions. */
@Mod.EventBusSubscriber(
        modid = BetterAe2.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class SidebarKeyMappings
{
    public static final KeyMapping DEPOSIT_CONTAINER = new KeyMapping(
            "key.better_ae2.deposit_container",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.better_ae2"
    );
    public static final KeyMapping DEPOSIT_PLAYER = new KeyMapping(
            "key.better_ae2.deposit_player",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.better_ae2"
    );

    private SidebarKeyMappings()
    {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event)
    {
        event.register(DEPOSIT_CONTAINER);
        event.register(DEPOSIT_PLAYER);
    }
}
