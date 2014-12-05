package tsteelworks.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import tsteelworks.blocks.logic.DeepTankLogic;

public class DeepTankContainer extends TSActiveContainer
{
    public DeepTankLogic logic;
    public InventoryPlayer playerInv;

    public DeepTankContainer(InventoryPlayer inventoryplayer, DeepTankLogic tank)
    {
        logic = tank;
        playerInv = inventoryplayer;
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
        return null;
    }

    @Override
    protected boolean mergeItemStack (ItemStack inputStack, int startSlot, int endSlot, boolean flag)
    {
        return false;
    }
}
