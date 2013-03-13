package micdoodle8.mods.galacticraft.core.items;

import java.util.ArrayList;
import java.util.List;

import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.client.ClientProxyCore;
import micdoodle8.mods.galacticraft.core.client.GCCorePlayerBaseClient;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GCCoreItemParachute extends Item
{
	protected List<Icon> icons = new ArrayList<Icon>();
	
	public static final String[] names = {
		"plain", // 0
		"black", // 1
		"blue", // 2
		"lime", // 3
		"brown", // 4
		"darkblue", // 5
		"darkgray", // 6
		"darkgreen", // 7
		"gray", // 8
		"magenta", // 9
		"orange", // 10
		"pink", // 11
		"purple", // 12
		"red", // 13
		"teal", // 14
		"yellow"}; // 15

	public GCCoreItemParachute(int par1)
	{
		super(par1);
		this.setMaxDamage(0);
		this.setHasSubtypes(true);
		this.setMaxStackSize(1);
		this.setCreativeTab(GalacticraftCore.galacticraftTab);
	}

    @Override
    public void getSubItems(int par1, CreativeTabs par2CreativeTabs, List par3List)
    {
    	for (int i = 0; i < GCCoreItemParachute.names.length; i++)
    	{
            par3List.add(new ItemStack(par1, 1, i));
    	}
    }

    @Override
    public int getMetadata(int par1)
    {
        return par1;
    }

	@Override
	@SideOnly(Side.CLIENT)
	public void func_94581_a(IconRegister iconRegister)
	{
		List<ItemStack> list = new ArrayList<ItemStack>();
		this.getSubItems(this.itemID, this.getCreativeTab(), list);

		if (list.size() > 0)
		{
			for (ItemStack itemStack : list)
			{
				this.icons.add(iconRegister.func_94245_a(this.getUnlocalizedName(itemStack).replace("item.", "galacticraftcore:")));
			}
		}
		else
		{
			this.iconIndex = iconRegister.func_94245_a(this.getUnlocalizedName().replace("item.", "galacticraftcore:"));
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack itemStack)
	{
		return "item.parachute_" + names[itemStack.getItemDamage()];
	}

	@Override
	public Icon getIconFromDamage(int damage)
	{
		if (this.icons.size() > damage)
		{
			return this.icons.get(damage);
		}

		return super.getIconFromDamage(damage);
	}

//    @Override
//	@SideOnly(Side.CLIENT)
//    public int getIconFromDamage(int par1)
//    {
//    	switch (par1)
//    	{
//    	case 0: // plain
//    		return 49;
//    	case 1: // black
//    		return 34;
//    	case 2: // blue
//    		return 46;
//    	case 3: // green
//    		return 44;
//    	case 4: // brown
//    		return 37;
//    	case 5: // dark blue
//    		return 38;
//    	case 6: // dark gray
//    		return 42;
//    	case 7: // dark green
//    		return 36;
//    	case 8: // gray
//    		return 41;
//    	case 9: // magenta
//    		return 47;
//    	case 10: // orange
//    		return 48;
//    	case 11: // pink
//    		return 43;
//    	case 12: // purple
//    		return 39;
//    	case 13: // red
//    		return 35;
//    	case 14: // teal
//    		return 40;
//    	case 15: // yellow
//    		return 45;
//    	}
//
//    	return 0;
//    }

    public static int getParachuteDamageValueFromDye(int meta)
    {
    	switch (meta)
    	{
    	case 0:
    		return 1;
    	case 1:
    		return 13;
    	case 2:
    		return 7;
    	case 3:
    		return 4;
    	case 4:
    		return 5;
    	case 5:
    		return 12;
    	case 6:
    		return 14;
    	case 7:
    		return 8;
    	case 8:
    		return 6;
    	case 9:
    		return 11;
    	case 10:
    		return 3;
    	case 11:
    		return 15;
    	case 12:
    		return 2;
    	case 13:
    		return 9;
    	case 14:
    		return 10;
    	case 15:
    		return 0;
    	}

    	return -1;
    }

    @Override
    public EnumRarity getRarity(ItemStack par1ItemStack)
    {
		return EnumRarity.uncommon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4)
    {
    	if (par2EntityPlayer.worldObj.isRemote)
    	{
    		final GCCorePlayerBaseClient playerBaseCl = PlayerUtil.getPlayerBaseClientFromPlayer(par2EntityPlayer);

    		if (playerBaseCl != null && playerBaseCl.getUseTutorialText())
    		{
            	par3List.add("Press " + Keyboard.getKeyName(ClientProxyCore.GCKeyHandler.tankRefill.keyCode) + " to access");
            	par3List.add("     Galacticraft Inventory");
    		}
    	}
    }
}