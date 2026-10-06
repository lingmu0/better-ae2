package net.xuwu.betterae2.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.xuwu.betterae2.BetterAe2;
import net.xuwu.betterae2.client.ClientStorageState;
import net.xuwu.betterae2.client.SidebarRenderer;
import net.xuwu.betterae2.client.SidebarScreenAccess;

import java.util.List;

/** Keeps JEI's overlay and ingredient lookup aware of the real sidebar Slot area. */
@JeiPlugin
public final class BetterAe2JeiPlugin implements IModPlugin
{
    @Override
    public ResourceLocation getPluginUid()
    {
        return BetterAe2.id("jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration)
    {
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class,
                new IGuiContainerHandler<AbstractContainerScreen<?>>()
                {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> screen)
                    {
                        if (!(screen instanceof SidebarScreenAccess access)
                                || access.bbd$getSearchBox() == null
                                || access.bbd$isSidebarHidden()
                                || !ClientStorageState.available())
                        {
                            return List.of();
                        }

                        return List.of(new Rect2i(
                                access.bbd$getSidebarX(),
                                access.bbd$getSidebarY(),
                                SidebarRenderer.WIDTH,
                                SidebarRenderer.getPanelHeight()
                        ));
                    }
        });
    }

}
