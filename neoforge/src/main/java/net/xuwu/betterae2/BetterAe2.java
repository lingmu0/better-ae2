package net.xuwu.betterae2;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** NeoForge 1.21.1 entry point. */
@Mod(BetterAe2.MODID)
public final class BetterAe2
{
    public static final String MODID = "better_ae2";

    public BetterAe2(IEventBus modEventBus, ModContainer ignored)
    {
        modEventBus.addListener(NetworkHandler::registerPayloads);
    }

    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
