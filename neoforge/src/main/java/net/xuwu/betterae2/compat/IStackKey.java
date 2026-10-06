package net.xuwu.betterae2.compat;

import net.minecraft.world.item.ItemStack;

/** Minimal typed storage-key contract used by the synchronisation layer. */
public interface IStackKey<T extends IStackKey<T>>
{
    ItemStack copyStack();

    boolean isEmpty();
}
