package tsteelworks.blocks.logic;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.tileentity.Hopper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import tconstruct.library.util.IFacingLogic;
import tsteelworks.TSteelworks;
import tsteelworks.inventory.HighOvenDuctContainer;
import tsteelworks.lib.ConfigCore;

// TODO: Lots
public class HighOvenDuctLogic extends TSMultiServantLogic implements IInventory, IFacingLogic, Hopper
{
    byte direction = 0;
    int mode = 0;
    private ItemStack[] inventory = new ItemStack[9];
    private int transferCooldown = -1;
    
    public int getMode ()
    {
        return mode;
    }
    
    public void setMode (int newMode)
    {
        this.mode = (newMode < 6) ? newMode : 5; 
    }
    
    /* TSServantLogic */
    
    @Override
    public boolean canUpdate ()
    {
        return true;//hasValidMaster();
    }
    
    /* Duct Logic */
    
    public boolean updateDuct()
    {
        if (this.worldObj != null && !this.worldObj.isRemote)
        {
            if (!this.isCoolingDown())
            {
                boolean flag = this.insertItemToInventory();
                flag = suckItemsIntoDuct(this) || flag;

                if (flag)
                {
                    this.setTransferCooldown(8);
                    this.onInventoryChanged();
                    return true;
                }
            }

            return false;
        }
        else
        {
            return false;
        }
    }
    
    private boolean insertItemToInventory()
    {
        if (!hasValidMaster() || mode == 5) return false;
        IInventory masterInventory = this.getOutputInventory();

        if (masterInventory == null)
            return false;
        else
        {   
            for (int slot = 0; slot < this.getSizeInventory(); slot += 1)
            {
                if (this.getStackInSlot(slot) != null)
                {
                    ItemStack copyStack = this.getStackInSlot(slot).copy();
                    ItemStack outputStack = insertStack(masterInventory, this.decrStackSize(slot, 1), this.getRenderDirection(), mode);
                    if (outputStack == null || outputStack.stackSize == 0)
                    {
                        masterInventory.onInventoryChanged();
                        return true;
                    }
                    this.setInventorySlotContents(slot, copyStack);
                }
            }
            return false;
        }
    }
    
    public boolean suckItemsIntoDuct(Hopper localInventory)
    {
        if (mode == 5) return false;
        IInventory outsideInventory = getExternalInventory(localInventory, direction);

        if (outsideInventory != null)
        {
            byte side = 0;

            if (outsideInventory instanceof ISidedInventory && side > -1)
            {
                ISidedInventory isidedinventory = (ISidedInventory)outsideInventory;
                int[] slots = isidedinventory.getAccessibleSlotsFromSide(side);

                for (int i = 0; i < slots.length; ++i)
                {
                    if (insertStackFromInventory(localInventory, outsideInventory, slots[i], side, mode))
                        return true;
                }
            }
            else
            {
                int j = outsideInventory.getSizeInventory();

                for (int k = 0; k < j; ++k)
                {
                    if (insertStackFromInventory(localInventory, outsideInventory, k, side, mode))
                        return true;
                }
            }
        }
        else
        {
            if (ConfigCore.enableDuctVacuum)
            {
                EntityItem entityitem = getExternalItemEntity(localInventory.getWorldObj(), localInventory.getXPos(), localInventory.getYPos(), localInventory.getZPos(), direction);
    
                if (entityitem != null)
                    return insertStackFromEntity(localInventory, entityitem, mode);
            }
        }

        return false;
    }
    
    private static boolean insertStackFromInventory(Hopper localInventory, IInventory outsideInventory, int slot, int side, int transferMode)
    {
        ItemStack itemstack = outsideInventory.getStackInSlot(slot);

        if (itemstack != null && canExtractItemFromInventory(outsideInventory, itemstack, slot, side))
        {
            ItemStack itemstack1 = itemstack.copy();
            ItemStack outputStack = insertStack(localInventory, outsideInventory.decrStackSize(slot, 1), -1, transferMode);

            if (outputStack == null || outputStack.stackSize == 0)
            {
                outsideInventory.onInventoryChanged();
                return true;
            }

            outsideInventory.setInventorySlotContents(slot, itemstack1);
        }

        return false;
    }
    
    public static boolean insertStackFromEntity(IInventory localInventory, EntityItem item, int transferMode)
    {
        boolean flag = false;

        if (item == null)
            return false;
        else
        {
            ItemStack itemstack = item.getEntityItem().copy();
            ItemStack itemstack1 = insertStack(localInventory, itemstack, -1, transferMode);

            if (itemstack1 != null && itemstack1.stackSize != 0)
                item.setEntityItemStack(itemstack1);
            else
            {
                flag = true;
                item.setDead();
            }
            return flag;
        }
    }

    public static ItemStack insertStack(IInventory iiventory, ItemStack stack, int side, int transferMode)
    {
        if (iiventory instanceof ISidedInventory && side > -1)
        {
            ISidedInventory isidedinventory = (ISidedInventory)iiventory;
            int[] slot = isidedinventory.getAccessibleSlotsFromSide(side);

            for (int i = 0; i < slot.length && stack != null && stack.stackSize > 0; ++i)
                stack = sendItemsToLocation(iiventory, stack, slot[i], side);
        }
        else
        {
            if (transferMode == 4)
            {
                for (int slot = 4; slot < iiventory.getSizeInventory() && stack != null && stack.stackSize > 0; slot += 1)
                {
                    stack = sendItemsToLocation(iiventory, stack, slot, side);
                }
            }
            else
            {
                stack = sendItemsToLocation(iiventory, stack, transferMode, side);
            }
        }

        if (stack != null && stack.stackSize == 0)
            stack = null;

        return stack;
    }
    
    private static boolean canInsertItemToInventory(IInventory iiventory, ItemStack stack, int slot, int side)
    {
        return !iiventory.isItemValidForSlot(slot, stack) ? false : !(iiventory instanceof ISidedInventory) || ((ISidedInventory)iiventory).canInsertItem(slot, stack, side);
    }
    
    private static boolean canExtractItemFromInventory(IInventory iiventory, ItemStack stack, int slot, int side)
    {
        return !(iiventory instanceof ISidedInventory) || ((ISidedInventory)iiventory).canExtractItem(slot, stack, side);
    }
    
    private static ItemStack sendItemsToLocation(IInventory iinventory, ItemStack stack, int slot, int side)
    {
        ItemStack masterStack = iinventory.getStackInSlot(slot);

        if (canInsertItemToInventory(iinventory, stack, slot, side))
        {
            boolean flag = false;

            if (masterStack == null)
            {
                int max = Math.min(stack.getMaxStackSize(), iinventory.getInventoryStackLimit());
                if (max >= stack.stackSize)
                {
                    iinventory.setInventorySlotContents(slot, stack);
                    stack = null;
                }
                else
                {
                    iinventory.setInventorySlotContents(slot, stack.splitStack(max));
                }
                flag = true;
            }
            else if (areItemStacksEqualItem(masterStack, stack))
            {
                int max = Math.min(stack.getMaxStackSize(), iinventory.getInventoryStackLimit());
                if (max > masterStack.stackSize)
                {
                    int l = Math.min(stack.stackSize, max - masterStack.stackSize);
                    stack.stackSize -= l;
                    masterStack.stackSize += l;
                    flag = l > 0;
                }
            }
            if (flag)
                iinventory.onInventoryChanged();
        }
        return stack;
    }
    
    private IInventory getOutputInventory()
    {
        final int mx = getMasterPosition().x;
        final int my = getMasterPosition().y;
        final int mz = getMasterPosition().z;
        HighOvenLogic logic = (HighOvenLogic) worldObj.getBlockTileEntity(mx, my, mz);
        return logic;
    }
    
    public static IInventory getExternalInventory(Hopper localInventory, byte facing)
    {
        double checkXPos = localInventory.getXPos();
        double checkYPos = localInventory.getYPos();
        double checkZPos = localInventory.getZPos();
        
        switch (facing)
        {
            case 0: // Down
                checkYPos = localInventory.getYPos() - 1.0D;
                break;
            case 1: // Up
                checkYPos = localInventory.getYPos() + 1.0D;
                break;
            case 2: // North
                checkZPos = localInventory.getZPos() - 1.0D;
                break;
            case 3: // South
                checkZPos = localInventory.getZPos() + 1.0D;
                break;
            case 4: // West
                checkXPos = localInventory.getXPos() - 1.0D;
                break;
            case 5: // East
                checkXPos = localInventory.getXPos() + 1.0D;
                break;
            default:
                break;
        }
        
        return getInventoryAtLocation(localInventory.getWorldObj(), checkXPos, checkYPos, checkZPos);
    }
    
    public static EntityItem getExternalItemEntity(World world, double minX, double minY, double minZ, byte facing)
    {
        double x = minX;
        double maxX = minX;
        double y = minY;
        double maxY = minY;
        double z = minZ;
        double maxZ = minZ;
        switch (facing)
        {
            case 0: // Down
                y = minY - 1.0D;
                maxY = minY - 1.0D;
                break;
            case 1: // Up
                maxY = minY + 1.0D;
                break;
            case 2: // North
                z = minZ - 1.0D;
                maxZ = minZ - 1.0D;
                break;
            case 3: // South
                maxZ = minZ + 1.0D;
                break;
            case 4: // West
                x = minX - 1.0D;
                maxX = minX - 1.0D;
                break;
            case 5: // East
                maxX = minX + 1.0D;
                break;
            default:
                break;
        }
        List list = world.selectEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getAABBPool().getAABB(x, y, z, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D), IEntitySelector.selectAnything);
        return list.size() > 0 ? (EntityItem)list.get(0) : null;
    }
    
    public static IInventory getInventoryAtLocation(World world, double minX, double minY, double maxX)
    {
        IInventory iinventory = null;
        int i = MathHelper.floor_double(minX);
        int j = MathHelper.floor_double(minY);
        int k = MathHelper.floor_double(maxX);
        TileEntity tileentity = world.getBlockTileEntity(i, j, k);

        if (tileentity != null && tileentity instanceof IInventory)
        {
            iinventory = (IInventory)tileentity;
            if (iinventory instanceof TileEntityChest)
            {
                int l = world.getBlockId(i, j, k);
                Block block = Block.blocksList[l];

                if (block instanceof BlockChest)
                    iinventory = ((BlockChest)block).getInventory(world, i, j, k);
            }
        }
        if (iinventory == null)
        {
            List list = world.getEntitiesWithinAABBExcludingEntity((Entity)null, AxisAlignedBB.getAABBPool().getAABB(minX, minY, maxX, minX + 1.0D, minY + 1.0D, maxX + 1.0D), IEntitySelector.selectInventories);
            if (list != null && list.size() > 0)
                iinventory = (IInventory)list.get(world.rand.nextInt(list.size()));
        }

        return iinventory;
    }
    
    private static boolean areItemStacksEqualItem(ItemStack stack1, ItemStack stack2)
    {
        return stack1.itemID != stack2.itemID ? false : (stack1.getItemDamage() != stack2.getItemDamage() ? false : (stack1.stackSize > stack1.getMaxStackSize() ? false : ItemStack.areItemStackTagsEqual(stack1, stack2)));
    }
    
    /**
     * Gets the world X position for this hopper entity.
     */
    public double getXPos()
    {
        return (double)this.xCoord;
    }

    /**
     * Gets the world Y position for this hopper entity.
     */
    public double getYPos()
    {
        return (double)this.yCoord;
    }

    /**
     * Gets the world Z position for this hopper entity.
     */
    public double getZPos()
    {
        return (double)this.zCoord;
    }

    public void setTransferCooldown(int par1)
    {
        this.transferCooldown = par1;
    }

    public boolean isCoolingDown()
    {
        return this.transferCooldown > 0;
    }
    
    /* TileEntity */
    
    @Override
    public void updateEntity ()
    {
        if (this.worldObj != null && !this.worldObj.isRemote)
        {
            --this.transferCooldown;

            if (!this.isCoolingDown())
            {
                this.setTransferCooldown(0);
                this.updateDuct();
            }
        }
    }
    
    /* Container */
    
    public Container getGuiContainer (InventoryPlayer inventoryplayer, World world, int x, int y, int z)
    {
        return new HighOvenDuctContainer(inventoryplayer, this);
    }
    
    /* IInventory */
    
    @Override
    public void onInventoryChanged ()
    {
        //updateEntity();
        super.onInventoryChanged();
    }
    
    @Override
    public int getSizeInventory ()
    {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot (int slot)
    {
        return inventory[slot];
    }

    @Override
    public ItemStack decrStackSize (int slot, int quantity)
    {
        if (inventory[slot] != null)
        {
            if (inventory[slot].stackSize <= quantity)
            {
                final ItemStack stack = inventory[slot];
                inventory[slot] = null;
                return stack;
            }
            final ItemStack split = inventory[slot].splitStack(quantity);
            if (inventory[slot].stackSize == 0)
            {
                inventory[slot] = null;
            }
            return split;
        }
        else
            return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing (int slot)
    {
        return null;
    }

    @Override
    public void setInventorySlotContents (int slot, ItemStack itemstack)
    {
        inventory[slot] = itemstack;
        if ((itemstack != null) && (itemstack.stackSize > getInventoryStackLimit()))
        {
            itemstack.stackSize = getInventoryStackLimit();
        }
    }
    
    @Override
    public boolean isUseableByPlayer (EntityPlayer entityplayer)
    {
        if (worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) != this)
            return false;
        else
            return entityplayer.getDistance(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64D;
    }

    @Override
    public String getInvName ()
    {
        return "container.HighOvenDuct";
    }
    
    @Override
    public boolean isInvNameLocalized ()
    {
        return (getInvName() != null) && (getInvName().length() > 0);
    }

    @Override
    public int getInventoryStackLimit ()
    {
        return 64;
    }

    @Override
    public void openChest ()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void closeChest ()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isItemValidForSlot (int slot, ItemStack itemstack)
    {
        if (slot < getSizeInventory())
            if ((inventory[slot] == null) || ((itemstack.stackSize + inventory[slot].stackSize) <= getInventoryStackLimit())) 
                return true;
        return false;
    }

    /* IFacingLogic */
    
    @Override
    public byte getRenderDirection ()
    {
        return direction;
    }

    @Override
    public ForgeDirection getForgeDirection ()
    {
        return ForgeDirection.VALID_DIRECTIONS[direction];
    }

    @Override
    public void setDirection (int side)
    {}

    @Override
    public void setDirection (float yaw, float pitch, EntityLivingBase player)
    {
        if (pitch > 45)
        {
            direction = 1;
        }
        else
            if (pitch < -45)
            {
                direction = 0;
            }
            else
            {
                final int facing = MathHelper.floor_double((yaw / 360) + 0.5D) & 3;
                switch (facing)
                {
                    case 0:
                        direction = 2;
                        break;
                    case 1:
                        direction = 5;
                        break;
                    case 2:
                        direction = 3;
                        break;
                    case 3:
                        direction = 4;
                        break;
                }
            }
    }
    
    /* NBT */
    
    @Override
    public void readFromNBT (NBTTagCompound tags)
    {
        this.mode = tags.getInteger("Mode");
        super.readFromNBT(tags);
        NBTTagList itemList = tags.getTagList("Items");
        this.inventory = new ItemStack[getSizeInventory()];
        this.transferCooldown = tags.getInteger("TransferCooldown");
        this.direction = tags.getByte("Direction");
        
        for (int iter = 0; iter < itemList.tagCount(); iter++)
        {
            NBTTagCompound tagList = (NBTTagCompound) itemList.tagAt(iter);
            byte slotID = tagList.getByte("Slot");
            if (slotID >= 0 && slotID < inventory.length)
            {
                inventory[slotID] = ItemStack.loadItemStackFromNBT(tagList);
            }
        }
        
    }

    @Override
    public void writeToNBT (NBTTagCompound tags)
    {
        
        super.writeToNBT(tags);
        NBTTagList nbttaglist = new NBTTagList();
        for (int iter = 0; iter < this.inventory.length; iter++)
        {
            if (this.inventory[iter] != null)
            {
                NBTTagCompound tagList = new NBTTagCompound();
                tagList.setByte("Slot", (byte) iter);
                this.inventory[iter].writeToNBT(tagList);
                nbttaglist.appendTag(tagList);
            }
        }
        tags.setTag("Items", nbttaglist);
        tags.setByte("Direction", this.direction);
        tags.setInteger("Mode", this.mode);
    }
    
    @Override
    public Packet getDescriptionPacket ()
    {
        final NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new Packet132TileEntityData(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket (INetworkManager net, Packet132TileEntityData packet)
    {
        readFromNBT(packet.data);
        worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord);
    }
}
