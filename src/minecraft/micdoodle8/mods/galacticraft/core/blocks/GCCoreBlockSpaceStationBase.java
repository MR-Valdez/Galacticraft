package micdoodle8.mods.galacticraft.core.blocks;

import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.tile.GCCoreTileEntitySpaceStationBase;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GCCoreBlockSpaceStationBase extends BlockContainer
{
	private Icon[] spaceStationIcons;
	
	public GCCoreBlockSpaceStationBase(int par1) 
	{
		super(par1, Material.rock);
	}
	
    public float getBlockHardness(World par1World, int par2, int par3, int par4)
    {
        return -1.0F;
    }

    @Override
	@SideOnly(Side.CLIENT)
    public void func_94332_a(IconRegister par1IconRegister)
    {
    	spaceStationIcons = new Icon[2];
    	spaceStationIcons[0] = par1IconRegister.func_94245_a("galacticraftcore:space_station_top");
    	spaceStationIcons[1] = par1IconRegister.func_94245_a("galacticraftcore:space_station_side");
    }

    @SideOnly(Side.CLIENT)
    public Icon getBlockTexture(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
    	switch (par5)
    	{
    	case 1:
    		return this.spaceStationIcons[0];
    	default:
    		return this.spaceStationIcons[1];
    	}
    }

	@Override
	public TileEntity createNewTileEntity(World world) 
	{
		return new GCCoreTileEntitySpaceStationBase();
	}
}