package net.xuwu.betterae2.mixin;

import mezz.jei.common.transfer.TransferOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Reads transfer operations from JEI's packet variants without hard-linking a version-specific class. */
@Pseudo
@Mixin(targets = {
        "mezz.jei.common.network.packets.PacketRecipeTransfer",
        "mezz.jei.common.network.packets.legacy.PacketRecipeTransfer",
        "mezz.jei.common.network.packets.legacy.PacketRecipeTransferCounted",
        "mezz.jei.common.network.packets.PacketRecipeTransferWithResult",
        "mezz.jei.common.network.packets.PacketRecipeTransferCountedWithResult"
}, remap = false)
public interface JeiRecipeTransferPacketAccessor
{
    @Accessor(value = "transferOperations", remap = false)
    List<TransferOperation> bbd$getTransferOperations();
}
