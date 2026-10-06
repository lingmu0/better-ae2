package net.xuwu.betterae2.client;

import net.xuwu.betterae2.compat.ButtonState;
import net.xuwu.betterae2.compat.CommonConfigRuntime;
import net.xuwu.betterae2.compat.ItemStackKey;
import net.xuwu.betterae2.common.StorageEntry;
import net.xuwu.betterae2.common.StorageSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Client-side filtering and sorting for the AE2 sidebar snapshot. */
public final class ClientStorageView
{
    private StorageSnapshot loadedSnapshot;
    private String loadedSearch = "";
    private ButtonState loadedPrimarySort;
    private ButtonState loadedSecondarySort;
    private ButtonState loadedReverse;
    private List<Entry> orderedEntries = List.of();

    public List<Entry> entries(StorageSnapshot snapshot, String searchText)
    {
        String normalizedSearch = searchText == null ? "" : searchText.toLowerCase(Locale.ENGLISH).trim();
        boolean snapshotChanged = snapshot != loadedSnapshot;
        boolean searchChanged = !Objects.equals(normalizedSearch, loadedSearch);
        boolean sortChanged = loadedPrimarySort != CommonConfigRuntime.uiSortButton
                || loadedSecondarySort != CommonConfigRuntime.uiSecondSortButton
                || loadedReverse != CommonConfigRuntime.uiReverseButton;

        if (snapshotChanged || searchChanged || sortChanged)
        {
            loadedSnapshot = snapshot;
            loadedSearch = normalizedSearch;
            loadedPrimarySort = CommonConfigRuntime.uiSortButton;
            loadedSecondarySort = CommonConfigRuntime.uiSecondSortButton;
            loadedReverse = CommonConfigRuntime.uiReverseButton;

            if (snapshot == null || !snapshot.available())
            {
                orderedEntries = List.of();
                return orderedEntries;
            }

            ArrayList<Entry> result = new ArrayList<>();
            for (StorageEntry entry : snapshot.entries())
            {
                if (entry == null || entry.stack().isEmpty() || entry.amount() <= 0L)
                {
                    continue;
                }
                ItemStackKey key = new ItemStackKey(entry.stack());
                String name = entry.stack().getHoverName().getString().toLowerCase(Locale.ENGLISH);
                String id = entry.stack().getItem().toString().toLowerCase(Locale.ENGLISH);
                if (normalizedSearch.isEmpty() || name.contains(normalizedSearch) || id.contains(normalizedSearch))
                {
                    result.add(new Entry(key, entry.amount()));
                }
            }

            Comparator<Entry> comparator = Comparator
                    .comparing((Entry entry) -> entry.key().copyStack().getHoverName().getString(),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparingLong(Entry::amount);
            if (loadedReverse == ButtonState.ENABLED)
            {
                comparator = comparator.reversed();
            }
            result.sort(comparator);
            orderedEntries = List.copyOf(result);
        }

        return orderedEntries;
    }

    public record Entry(ItemStackKey key, long amount)
    {
    }
}
