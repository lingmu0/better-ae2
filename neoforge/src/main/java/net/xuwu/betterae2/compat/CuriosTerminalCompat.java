package net.xuwu.betterae2.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Curios API access, isolated so callers can avoid loading this class when Curios is absent. */
public final class CuriosTerminalCompat
{
    private CuriosTerminalCompat()
    {
    }

    public static List<ItemStack> findCurios(Player player, Predicate<ItemStack> predicate)
    {
        List<ItemStack> stacks = new ArrayList<>();
        for (SlotResult result : CuriosApi.getCuriosHelper().findCurios(player, predicate))
        {
            stacks.add(result.stack());
        }
        return stacks;
    }
}
