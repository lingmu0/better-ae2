package net.xuwu.betterae2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.xuwu.betterae2.compat.ItemStackKey;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.common.network.IConnectionToServer;
import mezz.jei.common.network.packets.PlayToServerPacket;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.xuwu.betterae2.NetworkHandler;
import net.xuwu.betterae2.client.ClientStorageState;
import net.xuwu.betterae2.common.NetworkStorageMenuAccess;
import net.xuwu.betterae2.common.NetworkStorageSlot;
import net.xuwu.betterae2.common.RecipeFill;
import net.xuwu.betterae2.common.StorageEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Adds every network entry to standard JEI recipe detection and transfer. */
@Pseudo
@Mixin(targets = "mezz.jei.library.transfer.BasicRecipeTransferHandler", remap = false)
public abstract class JeiBasicRecipeTransferHandlerMixin
{
    @Shadow
    @Final
    private IRecipeTransferInfo transferInfo;

    @Unique
    private final List<NetworkStorageSlot> bbd$temporaryNetworkSlots = new ArrayList<>();

    @Unique
    private AbstractContainerMenu bbd$temporaryNetworkMenu;

    @Unique
    private boolean bbd$maxTransfer;

    @Unique
    private boolean bbd$requireCompleteSets;

    @Unique
    private Object bbd$recipeTransferContext;

    @Inject(method = "transferRecipe", at = @At("HEAD"), remap = false, require = 0)
    private void bbd$addPageIndependentSources(AbstractContainerMenu menu,
                                                 Object recipe,
                                                 IRecipeSlotsView recipeSlots,
                                                 Player player,
                                                 boolean maxTransfer,
                                                 boolean doTransfer,
                                                 CallbackInfoReturnable<IRecipeTransferError> callbackInfo)
    {
        bbd$recipeTransferContext = null;
        bbd$preparePageIndependentSources(menu, recipe, maxTransfer);
    }

    @Inject(
            method = "transferRecipe(Lmezz/jei/api/recipe/transfer/IRecipeTransferContext;Z)"
                    + "Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void bbd$addContextPageIndependentSources(@Coerce Object transferContext,
                                                       boolean doTransfer,
                                                       CallbackInfoReturnable<IRecipeTransferError> callbackInfo)
    {
        bbd$recipeTransferContext = transferContext;
        Object rawMenu = bbd$invokeTransferContext(transferContext, "getContainer");
        Object recipe = bbd$invokeTransferContext(transferContext, "getRecipe");
        Object maxTransfer = bbd$invokeTransferContext(transferContext, "isMaxTransfer");
        if (rawMenu instanceof AbstractContainerMenu menu)
        {
            bbd$preparePageIndependentSources(menu, recipe,
                    maxTransfer instanceof Boolean value && value);
        }
        else
        {
            bbd$removeTemporaryNetworkSlots();
        }
    }

    @Unique
    private void bbd$preparePageIndependentSources(AbstractContainerMenu menu, Object recipe,
                                                    boolean maxTransfer)
    {
        bbd$maxTransfer = maxTransfer;
        bbd$requireCompleteSets = transferInfo.requireCompleteSets(menu, recipe);
        bbd$removeTemporaryNetworkSlots();
        if (!ClientStorageState.available() || ClientStorageState.isSidebarHidden()
                || !(menu instanceof NetworkStorageMenuAccess access))
        {
            return;
        }

        bbd$temporaryNetworkMenu = menu;
        Set<ItemStackKey> representedKeys = new HashSet<>();
        for (NetworkStorageSlot slot : access.bbd$getNetworkSlots())
        {
            if (slot.isActive() && slot.hasItem() && slot.getKey() != null)
            {
                representedKeys.add(slot.getKey());
            }
        }

        List<StorageEntry> entries = ClientStorageState.entries();
        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++)
        {
            StorageEntry entry = entries.get(entryIndex);
            if (entry == null || entry.amount() <= 0L || entry.stack().isEmpty())
            {
                continue;
            }

            ItemStackKey key = new ItemStackKey(entry.stack());
            if (!representedKeys.add(key))
            {
                continue;
            }

            NetworkStorageSlot slot = new NetworkStorageSlot(menu, entryIndex, 0, 0);
            slot.update(entryIndex, key, entry.amount(), true);
            access.bbd$addNetworkSlot(slot);
            bbd$temporaryNetworkSlots.add(slot);
        }
    }

    @Inject(method = "transferRecipe", at = @At("RETURN"), remap = false, require = 0)
    private void bbd$removePageIndependentSources(CallbackInfoReturnable<IRecipeTransferError> callbackInfo)
    {
        bbd$removeTemporaryNetworkSlots();
        bbd$recipeTransferContext = null;
    }

    @Redirect(
            method = {"transferRecipe", "transferRecipeInternal"},
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/api/recipe/transfer/IRecipeTransferInfo;"
                            + "getInventorySlots(Lnet/minecraft/world/inventory/AbstractContainerMenu;"
                            + "Ljava/lang/Object;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Slot> bbd$includeNetworkIngredientSlots(IRecipeTransferInfo transferInfo,
                                                          AbstractContainerMenu menu, Object recipe)
    {
        List<Slot> result = new ArrayList<>(transferInfo.getInventorySlots(menu, recipe));
        if (!ClientStorageState.available() || ClientStorageState.isSidebarHidden()
                || !(menu instanceof NetworkStorageMenuAccess access))
        {
            return result;
        }

        for (NetworkStorageSlot slot : access.bbd$getNetworkSlots())
        {
            if (slot.isActive() && slot.hasItem() && !result.contains(slot))
            {
                result.add(slot);
            }
        }

        // These hidden-page slots are in menu.slots for this call, so JEI accepts their indexes.
        // They are still added explicitly because transfer-info implementations normally return
        // only ordinary inventory slots.
        for (NetworkStorageSlot slot : bbd$temporaryNetworkSlots)
        {
            if (slot.isActive() && slot.hasItem() && !result.contains(slot))
            {
                result.add(slot);
            }
        }
        return result;
    }

    @WrapOperation(
            method = {"transferRecipe", "transferRecipeInternal", "sendTransferWithResult"},
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/common/network/IConnectionToServer;"
                            + "sendPacketToServer(Lmezz/jei/common/network/packets/PlayToServerPacket;)V",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void bbd$sendPageIndependentTransfer(IConnectionToServer connection,
                                                  PlayToServerPacket packet,
                                                  Operation<Void> original)
    {
        if (packet instanceof JeiRecipeTransferPacketAccessor transfer)
        {
            List<TransferOperation> operations = transfer.bbd$getTransferOperations();
            List<RecipeFill> fills = new ArrayList<>(operations.size());
            boolean valid = true;

            for (TransferOperation operation : operations)
            {
                int sourceId = operation.inventorySlotId();
                StorageEntry virtualEntry = bbd$getVirtualSourceEntry(sourceId);
                ItemStack source = virtualEntry == null
                        ? bbd$getClientSourceStack(sourceId)
                        : virtualEntry.stack().copy();

                if (source.isEmpty())
                {
                    valid = false;
                    break;
                }

                source.setCount(1);
                fills.add(new RecipeFill(operation.craftingSlotId(), source, 1));
            }

            // PacketRecipeTransfer serializes the whole inventory-slot list, including every
            // temporary client-only slot. Use our server-authoritative fill packet whenever such
            // slots exist, even if this particular recipe selected only a visible-page source.
            if (!bbd$temporaryNetworkSlots.isEmpty() && valid && !fills.isEmpty())
            {
                NetworkHandler.fillRecipe(fills, bbd$maxTransfer, bbd$requireCompleteSets);
                bbd$completeRecipeTransferContext();
                return;
            }
        }

        original.call(connection, packet);
    }

    @Unique
    private StorageEntry bbd$getVirtualSourceEntry(int slotId)
    {
        for (NetworkStorageSlot slot : bbd$temporaryNetworkSlots)
        {
            if (slot.index != slotId)
            {
                continue;
            }

            int entryIndex = slot.getVisualIndex();
            List<StorageEntry> entries = ClientStorageState.entries();
            return entryIndex >= 0 && entryIndex < entries.size() ? entries.get(entryIndex) : null;
        }
        return null;
    }

    @Unique
    private static ItemStack bbd$getClientSourceStack(int slotId)
    {
        if (slotId < 0)
        {
            return ItemStack.EMPTY;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.containerMenu == null
                || slotId >= minecraft.player.containerMenu.slots.size())
        {
            return ItemStack.EMPTY;
        }

        Slot slot = minecraft.player.containerMenu.slots.get(slotId);
        return slot == null || !slot.hasItem() ? ItemStack.EMPTY : slot.getItem().copy();
    }

    @Unique
    private void bbd$removeTemporaryNetworkSlots()
    {
        if (bbd$temporaryNetworkMenu == null)
        {
            return;
        }

        if (bbd$temporaryNetworkMenu instanceof NetworkStorageMenuAccess access)
        {
            for (NetworkStorageSlot slot : bbd$temporaryNetworkSlots)
            {
                access.bbd$removeNetworkSlot(slot);
            }
        }
        bbd$temporaryNetworkSlots.clear();
        bbd$temporaryNetworkMenu = null;
    }

    @Unique
    private static Object bbd$invokeTransferContext(Object context, String methodName)
    {
        if (context == null)
        {
            return null;
        }
        try
        {
            ClassLoader classLoader = context.getClass().getClassLoader();
            Class<?> contextType = Class.forName(
                    "mezz.jei.api.recipe.transfer.IRecipeTransferContext", false, classLoader);
            return contextType.getMethod(methodName).invoke(context);
        }
        catch (ReflectiveOperationException | LinkageError exception)
        {
            return null;
        }
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void bbd$completeRecipeTransferContext()
    {
        Object context = bbd$recipeTransferContext;
        if (context == null)
        {
            return;
        }
        try
        {
            ClassLoader classLoader = context.getClass().getClassLoader();
            Class<?> contextType = Class.forName(
                    "mezz.jei.api.recipe.transfer.IRecipeTransferContext", false, classLoader);
            Class<?> resultType = Class.forName(
                    "mezz.jei.api.recipe.transfer.RecipeTransferResult", false, classLoader);
            Object success = Enum.valueOf((Class<? extends Enum>) resultType, "SUCCESS");
            contextType.getMethod("completeRecipeTransfer", resultType).invoke(context, success);
        }
        catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ignored)
        {
            // This callback is optional; the JEI packet path still handles versions without this API.
        }
    }
}
