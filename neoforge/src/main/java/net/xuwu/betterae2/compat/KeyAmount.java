package net.xuwu.betterae2.compat;

/** Key/count pair matching the storage code's original shape. */
public record KeyAmount(IStackKey<?> key, long amount)
{
    public boolean isEmpty()
    {
        return key == null || key.isEmpty() || amount <= 0L;
    }
}
