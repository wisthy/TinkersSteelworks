package tsteelworks.items;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumArmorMaterial;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import tsteelworks.lib.Repo;
import tsteelworks.lib.TSteelworksRegistry;

public class TSArmorBasic extends ItemArmor
{
    public String textureName;

    public TSArmorBasic(int par1, EnumArmorMaterial par2EnumArmorMaterial, int par3, String textureName)
    {
        super(par1, par2EnumArmorMaterial, 0, par3);
        setCreativeTab(TSteelworksRegistry.SteelworksCreativeTab);
        this.textureName = textureName;
    }

    @Override
    public String getArmorTexture (ItemStack stack, Entity entity, int slot, int layer)
    {
        return Repo.textureDir + "textures/armor/" + textureName + "_" + layer + ".png";
    }

    @Override
    public void registerIcons (IconRegister par1IconRegister)
    {
        itemIcon = par1IconRegister.registerIcon(Repo.textureDir + "armor/" + textureName + "_"
                + (armorType == 0 ? "helmet" : armorType == 1 ? "chestplate" : armorType == 2 ? "leggings" : armorType == 3 ? "boots" : "helmet"));
    }
}
