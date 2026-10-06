package net.xuwu.betterae2.compat;

import appeng.api.storage.StorageHelper;
import appeng.api.stacks.AEItemKey;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Thin AE2 MEStorage adapter used by the server-authoritative sidebar actions. */
public final class UnifiedStorage
{
    private final DimensionsNet network;
    private final Map<IStackKey<?>, Long> creationTimes = new HashMap<>();
    private final Map<IStackKey<?>, Long> modifiedTimes = new HashMap<>();

    UnifiedStorage(DimensionsNet network)
    {
        this.network = network;
    }

    public List<KeyAmount> getStorage()
    {
        java.util.ArrayList<KeyAmount> result = new java.util.ArrayList<>();
        for (var entry : network.storage().getAvailableStacks())
        {
            if (entry.getKey() instanceof AEItemKey itemKey && entry.getLongValue() > 0L)
            {
                ItemStackKey key = ItemStackKey.fromAEKey(itemKey);
                result.add(new KeyAmount(key, entry.getLongValue()));
                creationTimes.putIfAbsent(key, 0L);
                modifiedTimes.putIfAbsent(key, 0L);
            }
        }
        return result;
    }

    public KeyAmount getStackByKey(ItemStackKey key)
    {
        if (key == null || key.isEmpty())
        {
            return new KeyAmount(key, 0L);
        }
        long amount = network.storage().getAvailableStacks().get(key.aeKey());
        if (amount > 0L)
        {
            creationTimes.putIfAbsent(key, 0L);
            modifiedTimes.putIfAbsent(key, 0L);
        }
        return new KeyAmount(key, amount);
    }

    /** Inserts and returns the leftover amount, matching the original action code. */
    public KeyAmount insert(ItemStackKey key, long amount, boolean ignored)
    {
        if (key == null || key.isEmpty() || amount <= 0L)
        {
            return new KeyAmount(key, Math.max(0L, amount));
        }
        long inserted = StorageHelper.poweredInsert(network.energySource(), network.storage(),
                key.aeKey(), amount, network.actionSource());
        if (inserted > 0L)
        {
            modifiedTimes.put(key, 0L);
            creationTimes.putIfAbsent(key, 0L);
        }
        return new KeyAmount(key, Math.max(0L, amount - inserted));
    }

    /** Extracts and returns the amount actually removed. */
    public KeyAmount extract(ItemStackKey key, long amount, boolean ignored, boolean ignoredSecond)
    {
        if (key == null || key.isEmpty() || amount <= 0L)
        {
            return new KeyAmount(key, 0L);
        }
        long extracted = StorageHelper.poweredExtraction(network.energySource(), network.storage(),
                key.aeKey(), amount, network.actionSource());
        return new KeyAmount(key, Math.max(0L, extracted));
    }

    public Map<IStackKey<?>, Long> getCreationTimeMap()
    {
        return creationTimes;
    }

    public Map<IStackKey<?>, Long> getLastModifiedTimeMap()
    {
        return modifiedTimes;
    }

    public AutoCloseable subscribeAny(Object owner, Runnable listener)
    {
        return () -> { };
    }

    public AutoCloseable subscribeDelta(Object owner, DeltaListener listener)
    {
        return () -> { };
    }

    @FunctionalInterface
    public interface DeltaListener
    {
        void onDelta(IStackKey<?> key, long amount, boolean removed);
    }
}
