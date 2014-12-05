package tsteelworks.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import tconstruct.library.util.CoordTuple;
import tconstruct.library.util.IFacingLogic;
import tconstruct.library.util.IMasterLogic;
import tsteelworks.TSteelworks;
import tsteelworks.blocks.logic.TurbineLogic;
import tsteelworks.client.block.MachineRender;
import tsteelworks.lib.Repo;
import tsteelworks.lib.TSteelworksRegistry;
import tsteelworks.lib.blocks.TSInventoryBlock;

//TODO: Keep steam on block removal
public class MachineBlock extends TSInventoryBlock
{
    static ArrayList<CoordTuple> directions = new ArrayList<CoordTuple>(6);

    public MachineBlock(int id) {
            super(id, Material.iron);
            setHardness(3F);
            setResistance(20F);
            this.setCreativeTab(TSteelworksRegistry.SteelworksCreativeTab);
            setUnlocalizedName("tsteelworks.Machine");
    }
    
    @Override
    public TileEntity createNewTileEntity(World world) {
            return null; 
    }
    
    @Override
    public TileEntity createTileEntity (World world, int metadata)
    {
        return new TurbineLogic();
    }
   
    @Override
    public boolean onBlockActivated (World world, int x, int y, int z, EntityPlayer player, int side, float clickX, float clickY, float clickZ)
    {
        ItemStack heldItem = player.inventory.getCurrentItem();
        if (heldItem != null)
        {
            FluidStack liquid = FluidContainerRegistry.getFluidForFilledItem(player.getCurrentEquippedItem());
            if (liquid == null) return false;
            if (!liquid.getFluid().equals(FluidRegistry.getFluid("steam"))) return false;
            TurbineLogic logic = (TurbineLogic) world.getBlockTileEntity(x, y, z);
            if (liquid != null)
            {
                int amount = logic.fill(ForgeDirection.UNKNOWN, liquid, false);
                if (amount == liquid.amount)
                {
                    logic.fill(ForgeDirection.UNKNOWN, liquid, true);
                    if (!player.capabilities.isCreativeMode)
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, consumeItem(heldItem));
                    return true;
                }
                else
                    return true;
            }
        }

        return false;
    }
    
    public static ItemStack consumeItem (ItemStack stack)
    {
        if (stack.stackSize == 1)
        {
            if (stack.getItem().hasContainerItem())
                return stack.getItem().getContainerItemStack(stack);
            else
                return null;
        }
        else
        {
            stack.splitStack(1);
            return stack;
        }
    }
    
    @Override
    public Integer getGui (World world, int x, int y, int z, EntityPlayer entityplayer)
    {
        return null;
    }
    
    @Override
    public String[] getTextureNames ()
    {
        final String[] textureNames = { "turbine_front", "turbine_side", "turbine_back"};
        return textureNames;
    }
        
    @Override
    public void registerIcons (IconRegister iconRegister)
    {
        final String[] textureNames = getTextureNames();
        icons = new Icon[textureNames.length];

        for (int i = 0; i < icons.length; ++i)
            icons[i] = iconRegister.registerIcon(Repo.textureDir + textureNames[i]);
    }
    
    @Override
    public Icon getIcon (int side, int meta)
    {
        if (meta == 0)
        {
            return icons[getTextureIndex(side)];
        }
        return icons[0];
    }
    
    @Override
    public Icon getBlockTexture (IBlockAccess world, int x, int y, int z, int side)
    {
        final TileEntity logic = world.getBlockTileEntity(x, y, z);
        final short direction = (logic instanceof IFacingLogic) ? ((IFacingLogic) logic).getRenderDirection() : 0;
        final int meta = world.getBlockMetadata(x, y, z);
        if (meta == 0) {
            if (side == direction)
            {
                return icons[0];
            }
            else if (side / 2 == direction / 2)
            {
                return icons[2];
            }
            return icons[1];
        }
        return icons[0];
    }
    
    public int getTextureIndex (int side)
    {
        return getTextureIndex(side, false);
    }

    public int getTextureIndex (int side, boolean alt)
    {
        if (side == 0)
            return 2;
        if (side == 1)
            return 0;

        return alt ? 9 : 1;
    }

    public int getRenderType ()
    {
        return MachineRender.model;
    }
    
    @Override
    public void randomDisplayTick (World world, int x, int y, int z, Random random)
    {
        if (isActive(world, x, y, z))
        {
            final TileEntity logic = world.getBlockTileEntity(x, y, z);
            byte face = 0;
            if (logic instanceof IFacingLogic)
                face = ((IFacingLogic) logic).getRenderDirection();
            final float f = x + 0.5F;
            final float f1 = y + 0.5F + ((random.nextFloat() * 6F) / 16F);
            final float f2 = z + 0.5F;
            final float f3 = 0.35F;
            final float f4 = (random.nextFloat() * 0.6F) - 0.3F;
            switch (face)
            {
            case 4:
                world.spawnParticle("explode", f - f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                break;
            case 5:
                world.spawnParticle("explode", f + f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                break;
            case 2:
                world.spawnParticle("explode", f + f4, f1, f2 - f3, 0.0D, 0.0D, 0.0D);
                break;
            case 3:
                world.spawnParticle("explode", f + f4, f1, f2 + f3, 0.0D, 0.0D, 0.0D);
                break;
            }
        }
    }
    
    @Override
    public boolean canConnectRedstone (IBlockAccess world, int x, int y, int z, int side)
    {
        final TileEntity logic = world.getBlockTileEntity(x, y, z);
        return (logic instanceof IMasterLogic);
    }
    
    boolean activeRedstone (World world, int x, int y, int z)
    {
        final Block wire = Block.blocksList[world.getBlockId(x, y, z)];
        if ((wire != null) && (wire.blockID == Block.redstoneWire.blockID))
            return world.getBlockMetadata(x, y, z) > 0;
        return false;
    }
    
    @Override
    public void getSubBlocks (int id, CreativeTabs tab, List list)
    {
//        for (int iter = 0; iter < 0; iter++)
            list.add(new ItemStack(id, 1, 0));
    }
    
    @Override
    public void onNeighborBlockChange (World world, int x, int y, int z, int nBlockID)
    {
        final TileEntity logic = world.getBlockTileEntity(x, y, z);
        if (logic instanceof TurbineLogic)
            ((TurbineLogic) logic).setActive(world.isBlockIndirectlyGettingPowered(x, y, z));
    }
    
    @Override
    public int damageDropped (int meta)
    {
        return meta;
    }
    
    @Override
    public int quantityDropped (final Random random)
    {
        return 1;
    }
    
    @Override
    public Object getModInstance ()
    {
        return TSteelworks.instance;
    }
 
    static
    {
        directions.add(new CoordTuple(0, -1, 0));
        directions.add(new CoordTuple(0, 1, 0));
        directions.add(new CoordTuple(0, 0, -1));
        directions.add(new CoordTuple(0, 0, 1));
        directions.add(new CoordTuple(-1, 0, 0));
        directions.add(new CoordTuple(1, 0, 0));
    }
}
