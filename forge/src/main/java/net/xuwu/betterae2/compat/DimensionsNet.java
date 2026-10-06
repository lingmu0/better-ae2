package net.xuwu.betterae2.compat;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.storage.MEStorage;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.core.AEConfig;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-side AE2 access selected from a linked wireless terminal.
 *
 * <p>The name is retained only as a small compatibility seam for the sidebar logic. The adapter
 * talks directly to AE2's public storage API.</p>
 */
public final class DimensionsNet
{
    private static final Map<Player, DimensionsNet> CACHE = new WeakHashMap<>();

    private final ServerPlayer player;
    private final ItemStack terminalStack;
    private final WirelessTerminalItem terminal;
    private final IGrid grid;
    private final MEStorage storage;
    private final IActionSource actionSource;
    private final IEnergySource energySource;
    private final UnifiedStorage unifiedStorage;

    private DimensionsNet(ServerPlayer player, ItemStack terminalStack,
                          WirelessTerminalItem terminal, IGrid grid)
    {
        this.player = player;
        this.terminalStack = terminalStack;
        this.terminal = terminal;
        this.grid = grid;
        this.storage = grid.getStorageService().getInventory();
        this.actionSource = IActionSource.ofPlayer(player);
        this.energySource = this::extractPower;
        this.unifiedStorage = new UnifiedStorage(this);
    }

    public static DimensionsNet getNetFromPlayer(Player player)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return null;
        }

        TerminalReference reference = findTerminal(serverPlayer);
        if (reference == null)
        {
            CACHE.remove(serverPlayer);
            return null;
        }

        IGrid grid = reference.grid();
        if (grid == null || grid.getStorageService() == null
                || grid.getStorageService().getInventory() == null)
        {
            CACHE.remove(serverPlayer);
            return null;
        }

        DimensionsNet existing = CACHE.get(serverPlayer);
        if (existing != null && existing.terminalStack == reference.stack() && existing.grid == grid)
        {
            return existing;
        }

        DimensionsNet created = new DimensionsNet(serverPlayer, reference.stack(), reference.terminal(), grid);
        CACHE.put(serverPlayer, created);
        return created;
    }

    public static boolean isPortableTerminal(ItemStack stack)
    {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof WirelessTerminalItem;
    }

    public UnifiedStorage getUnifiedStorage()
    {
        return unifiedStorage;
    }

    public Component getNetworkName()
    {
        return Component.literal("AE2");
    }

    MEStorage storage()
    {
        return storage;
    }

    IActionSource actionSource()
    {
        return actionSource;
    }

    IEnergySource energySource()
    {
        return energySource;
    }

    private double extractPower(double amount, Actionable mode, PowerMultiplier ignoredMultiplier)
    {
        if (amount <= 0.0D)
        {
            return 0.0D;
        }

        double available = Math.min(amount, terminal.getAECurrentPower(terminalStack));
        if (mode == Actionable.SIMULATE)
        {
            return available;
        }
        return available > 0.0D && terminal.usePower(player, available, terminalStack) ? available : 0.0D;
    }

    private static TerminalReference findTerminal(ServerPlayer player)
    {
        Inventory inventory = player.getInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++)
        {
            ItemStack stack = inventory.getItem(index);
            if (isPortableTerminal(stack))
            {
                TerminalReference reference = linkedReference(player, stack);
                if (reference != null)
                {
                    return reference;
                }
            }
        }

        if (ModList.get().isLoaded("curios"))
        {
            for (ItemStack curiosStack : CuriosTerminalCompat.findCurios(player,
                    DimensionsNet::isPortableTerminal))
            {
                TerminalReference reference = linkedReference(player, curiosStack);
                if (reference != null)
                {
                    return reference;
                }
            }
        }
        return null;
    }

    private static TerminalReference linkedReference(ServerPlayer player, ItemStack stack)
    {
        WirelessTerminalItem terminal = (WirelessTerminalItem) stack.getItem();
        PortableTerminalHost host = new PortableTerminalHost(player, stack);
        if (!host.rangeCheck())
        {
            return null;
        }

        IGrid grid = terminal.getLinkedGrid(stack, player.level(), null);
        if (grid == null || !terminal.hasPower(player, host.requiredPower(grid), stack))
        {
            return null;
        }
        return new TerminalReference(stack, terminal, grid);
    }

    private static final class PortableTerminalHost extends WirelessTerminalMenuHost
    {
        private PortableTerminalHost(ServerPlayer player, ItemStack stack)
        {
            super(player, null, stack, (ignoredPlayer, ignoredMenu) -> { });
        }

        private double requiredPower(IGrid grid)
        {
            double closestDistanceSquared = Double.MAX_VALUE;
            for (IWirelessAccessPoint accessPoint : grid.getMachines(WirelessAccessPointBlockEntity.class))
            {
                closestDistanceSquared = Math.min(closestDistanceSquared, getWapSqDistance(accessPoint));
            }
            if (closestDistanceSquared == Double.MAX_VALUE)
            {
                return Double.POSITIVE_INFINITY;
            }
            return AEConfig.instance().wireless_getDrainRate(Math.sqrt(closestDistanceSquared));
        }
    }

    private record TerminalReference(ItemStack stack, WirelessTerminalItem terminal, IGrid grid)
    {
    }
}
