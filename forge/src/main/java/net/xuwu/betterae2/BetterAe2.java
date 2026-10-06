package net.xuwu.betterae2;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

/**
 * Better AE2 entry point.
 *
 * <p>The actual storage access is deliberately kept in the server-side
 * {@code common} package so every action is checked against the current
 * AE2 network.</p>
 */
@Mod(BetterAe2.MODID)
public final class BetterAe2
{
    public static final String MODID = "better_ae2";

    public BetterAe2()
    {
        NetworkHandler.register();
    }

    @SuppressWarnings("removal")
    public static ResourceLocation id(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}
