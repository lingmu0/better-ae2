package net.xuwu.betterae2.common;

import net.xuwu.betterae2.compat.DimensionsNet;
import net.xuwu.betterae2.compat.UnifiedStorage;
import net.xuwu.betterae2.compat.KeyAmount;
import net.xuwu.betterae2.compat.ItemStackKey;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Read-only adapter from the AE2 API to this add-on's packets. */
public final class NetworkStorage
{
    private NetworkStorage()
    {
    }

    public static StorageSnapshot snapshot(Player player)
    {
        boolean shiftPlayer = StorageActions.isShiftPlayerInventoryEnabled(player);
        boolean shiftContainer = StorageActions.isShiftContainerEnabled(player);
        boolean sidebarHidden = StorageActions.isSidebarHidden(player);
        DimensionsNet network = DimensionsNet.getNetFromPlayer(player);
        if (network == null)
        {
            return StorageSnapshot.unavailable(shiftPlayer, shiftContainer, sidebarHidden);
        }

        return new StorageSnapshot(
                true,
                network.getNetworkName().getString(),
                shiftPlayer,
                shiftContainer,
                sidebarHidden,
                entries(network.getUnifiedStorage())
        );
    }

    /** Returns only the small metadata portion used by incremental sync packets. */
    public static StorageSnapshot metadata(Player player)
    {
        boolean shiftPlayer = StorageActions.isShiftPlayerInventoryEnabled(player);
        boolean shiftContainer = StorageActions.isShiftContainerEnabled(player);
        boolean sidebarHidden = StorageActions.isSidebarHidden(player);
        DimensionsNet network = DimensionsNet.getNetFromPlayer(player);
        if (network == null)
        {
            return StorageSnapshot.unavailable(shiftPlayer, shiftContainer, sidebarHidden);
        }

        return new StorageSnapshot(
                true,
                network.getNetworkName().getString(),
                shiftPlayer,
                shiftContainer,
                sidebarHidden,
                List.of()
        );
    }

    public static List<StorageEntry> entries(UnifiedStorage storage)
    {
        List<StorageEntry> entries = new ArrayList<>();
        if (storage == null)
        {
            return entries;
        }

        for (KeyAmount stored : storage.getStorage())
        {
            if (stored.amount() <= 0L || !(stored.key() instanceof ItemStackKey itemKey) || itemKey.isEmpty())
            {
                continue;
            }
            long insertedTime = storage.getCreationTimeMap().getOrDefault(stored.key(), 0L);
            long modifiedTime = storage.getLastModifiedTimeMap().getOrDefault(stored.key(), 0L);
            entries.add(new StorageEntry(itemKey.copyStack(), stored.amount(), insertedTime, modifiedTime));
        }
        return entries;
    }
}
