package net.xuwu.betterae2.client;

import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.xuwu.betterae2.compat.CuriosTerminalCompat;

/** Client-side fast check so the sidebar follows inventory and Curios changes immediately. */
public final class ClientTerminalState
{
    private static final double MINIMUM_VISIBLE_POWER = 0.1D;

    private ClientTerminalState()
    {
    }

    public static boolean hasUsableTerminal(Player player)
    {
        if (player == null || player.isRemoved())
        {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++)
        {
            if (hasPower(player, inventory.getItem(index)))
            {
                return true;
            }
        }

        if (ModList.get().isLoaded("curios"))
        {
            for (ItemStack curiosStack : CuriosTerminalCompat.findCurios(player,
                    stack -> stack != null && !stack.isEmpty()
                            && stack.getItem() instanceof WirelessTerminalItem))
            {
                if (hasPower(player, curiosStack))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasPower(Player player, ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof WirelessTerminalItem terminal
                && terminal.hasPower(player, MINIMUM_VISIBLE_POWER, stack);
    }
}
