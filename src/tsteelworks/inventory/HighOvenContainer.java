package tsteelworks.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import tsteelworks.blocks.logic.HighOvenLogic;

public class HighOvenContainer extends TSActiveContainer
{
    public HighOvenLogic logic;
    public InventoryPlayer playerInv;
    public int fuel = 0;

    public HighOvenContainer(InventoryPlayer inventoryplayer, HighOvenLogic highoven)
    {
        logic = highoven;
        playerInv = inventoryplayer;

        /* HighOven Misc inventory */
        addSlotToContainer(new Slot(highoven, logic.SLOT_OXIDIZER, 55, 16)); // oxidizer
        addSlotToContainer(new Slot(highoven, logic.SLOT_REDUCER, 55, 34)); // reducer
        addSlotToContainer(new Slot(highoven, logic.SLOT_PURIFIER, 55, 52)); // purifier
        addSlotToContainer(new Slot(highoven, logic.SLOT_FUEL, 126, 52)); // fuel        
        /* HighOven Ore inventory */
        for (int y = 0; y < highoven.layers; y++)
            addDualSlotToContainer(new TSActiveSlot(highoven, logic.SLOT_FIRST_MELTABLE + y, 28, 7 + (y * 18), y < 6));

        /* Player inventory */
        for (int column = 0; column < 3; column++)
            for (int row = 0; row < 9; row++)
                addSlotToContainer(new Slot(inventoryplayer, row + (column * 9) + 9, 54 + (row * 18), 84 + (column * 18)));
        for (int column = 0; column < 9; column++)
            addSlotToContainer(new Slot(inventoryplayer, column, 54 + (column * 18), 142));
    }

    @Override
    public boolean canInteractWith (EntityPlayer entityplayer)
    {
        return logic.isUseableByPlayer(entityplayer);
    }

    @Override
    public void detectAndSendChanges ()
    {
        super.detectAndSendChanges();
    }

    @Override
    public ItemStack transferStackInSlot (EntityPlayer player, int slotID)
    {
        ItemStack stack = null;
        final Slot slot = (Slot) inventorySlots.get(slotID);
        if ((slot != null) && slot.getHasStack())
        {
            final ItemStack slotStack = slot.getStack();
            stack = slotStack.copy();

            if (slotID < logic.getSizeInventory())
            {
                if (!mergeItemStack(slotStack, logic.getSizeInventory(), inventorySlots.size(), true))
                    return null;
            }
            else if (!mergeItemStack(slotStack, 0, logic.getSizeInventory(), false))
                return null;
            if (slotStack.stackSize == 0)
                slot.putStack((ItemStack) null);
            else
                slot.onSlotChanged();
        }
        return stack;
    }

    @Override
    public void updateProgressBar (int id, int value)
    {
        if (id == 0)
            logic.fuelBurnTime = value / 12;
    }

    @Override
    protected boolean mergeItemStack (ItemStack inputStack, int startSlot, int endSlot, boolean flag)
    {
        boolean merged = false;
        int slotPos = startSlot;
        if (flag)
            slotPos = endSlot - 1;
        Slot slot;
        ItemStack slotStack;
        if (inputStack.isStackable())
            while ((inputStack.stackSize > 0) && ((!flag && (slotPos < endSlot)) || (flag && (slotPos >= startSlot))))
            {
                slot = (Slot) inventorySlots.get(slotPos);
                slotStack = slot.getStack();
                if ((slotStack != null) && (slotStack.itemID == inputStack.itemID) && (!inputStack.getHasSubtypes() || (inputStack.getItemDamage() == slotStack.getItemDamage()))
                        && ItemStack.areItemStackTagsEqual(inputStack, slotStack))
                {
                    final int totalSize = slotStack.stackSize + inputStack.stackSize;
                    if (totalSize <= inputStack.getMaxStackSize())
                    {
                        inputStack.stackSize = 0;
                        slotStack.stackSize = totalSize;
                        slot.onSlotChanged();
                        merged = true;
                    }
                    else if (slotStack.stackSize < inputStack.getMaxStackSize())
                    {
                        inputStack.stackSize -= inputStack.getMaxStackSize() - slotStack.stackSize;
                        slotStack.stackSize = inputStack.getMaxStackSize();
                        slot.onSlotChanged();
                        merged = true;
                    }
                }
                if (flag)
                    --slotPos;
                else
                    ++slotPos;
            }
        if (inputStack.stackSize > 0)
        {
            if (flag)
                slotPos = endSlot - 1;
            else
                slotPos = startSlot;
            while ((!flag && (slotPos < endSlot)) || (flag && (slotPos >= startSlot)))
            {
                slot = (Slot) inventorySlots.get(slotPos);
                slotStack = slot.getStack();
                if (slotStack == null)
                {
                    slot.putStack(inputStack.copy());
                    slot.onSlotChanged();
                    inputStack.stackSize = 0;
                    merged = true;
                    break;
                }
                if (flag)
                    --slotPos;
                else
                    ++slotPos;
            }
        }
        return merged;
    }
}
