package net.xuwu.betterae2.compat;

import appeng.api.stacks.AEItemKey;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** AE2-backed item key used by the copied sidebar code. */
public final class ItemStackKey implements IStackKey<ItemStackKey>
{
    public static final ItemStackKey EMPTY = new ItemStackKey((AEItemKey) null);

    private final AEItemKey key;

    public ItemStackKey(ItemStack stack)
    {
        this(AEItemKey.of(stack));
    }

    private ItemStackKey(AEItemKey key)
    {
        this.key = key;
    }

    public static ItemStackKey fromAEKey(AEItemKey key)
    {
        return key == null ? EMPTY : new ItemStackKey(key);
    }

    public AEItemKey aeKey()
    {
        return key;
    }

    public ItemStack copyStack()
    {
        return key == null ? ItemStack.EMPTY : key.toStack(1);
    }

    public ItemStack getRenderStack()
    {
        return copyStack();
    }

    public ItemStack copyStackWithCount(long count)
    {
        if (key == null || count <= 0L)
        {
            return ItemStack.EMPTY;
        }
        return key.toStack((int) Math.min(Integer.MAX_VALUE, count));
    }

    public int getVanillaMaxStackSize()
    {
        return key == null ? 0 : key.getMaxStackSize();
    }

    @Override
    public boolean isEmpty()
    {
        return key == null;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof ItemStackKey itemKey && Objects.equals(key, itemKey.key);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(key);
    }

    @Override
    public String toString()
    {
        return key == null ? "empty" : key.toString();
    }
}
