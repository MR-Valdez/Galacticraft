package micdoodle8.mods.galacticraft.mars.entities;

import icbm.api.IMissile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import micdoodle8.mods.galacticraft.api.entity.IDockable;
import micdoodle8.mods.galacticraft.api.entity.IRocketType;
import micdoodle8.mods.galacticraft.api.entity.IWorldTransferCallback;
import micdoodle8.mods.galacticraft.api.tile.IFuelDock;
import micdoodle8.mods.galacticraft.api.tile.ILandingPadAttachable;
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider;
import micdoodle8.mods.galacticraft.api.world.IOrbitDimension;
import micdoodle8.mods.galacticraft.core.ASMHelper.RuntimeInterface;
import micdoodle8.mods.galacticraft.core.GCCoreConfigManager;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.blocks.GCCoreBlockLandingPadFull;
import micdoodle8.mods.galacticraft.core.entities.EntitySpaceshipBase;
import micdoodle8.mods.galacticraft.core.entities.GCCorePlayerMP;
import micdoodle8.mods.galacticraft.core.event.GCCoreLandingPadRemovalEvent;
import micdoodle8.mods.galacticraft.core.items.GCCoreItems;
import micdoodle8.mods.galacticraft.core.tile.GCCoreTileEntityCargoPad;
import micdoodle8.mods.galacticraft.core.tile.GCCoreTileEntityFuelLoader;
import micdoodle8.mods.galacticraft.core.tile.GCCoreTileEntityLandingPad;
import micdoodle8.mods.galacticraft.core.util.PacketUtil;
import micdoodle8.mods.galacticraft.core.util.WorldUtil;
import micdoodle8.mods.galacticraft.mars.tile.GCMarsTileEntityLaunchController;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import universalelectricity.core.vector.Vector3;
import com.google.common.io.ByteArrayDataInput;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;

public class GCMarsEntityCargoRocket extends EntitySpaceshipBase implements IRocketType, IDockable, IInventory, IWorldTransferCallback
{
    public FluidTank spaceshipFuelTank = new FluidTank(this.getFuelTankCapacity());
    public EnumRocketType rocketType;
    public float rumble;
    public int destinationFrequency;
    public TileEntity targetTile;
    protected ItemStack[] cargoItems;
    public IUpdatePlayerListBox rocketSoundUpdater;
    private IFuelDock landingPad;
    public boolean landing;

    public GCMarsEntityCargoRocket(World par1World)
    {
        super(par1World);
    }

    public GCMarsEntityCargoRocket(World par1World, double par2, double par4, double par6, EnumRocketType rocketType)
    {
        super(par1World);
        this.setPosition(par2, par4 + this.yOffset, par6);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = par2;
        this.prevPosY = par4;
        this.prevPosZ = par6;
        this.rocketType = rocketType;
        this.cargoItems = new ItemStack[this.getSizeInventory()];
    }

    public int getFuelTankCapacity()
    {
        return 2000;
    }

    @Override
    public int getScaledFuelLevel(int scale)
    {
        final double fuelLevel = this.spaceshipFuelTank.getFluid() == null ? 0 : this.spaceshipFuelTank.getFluid().amount;

        return (int) (fuelLevel * scale / this.getFuelTankCapacity());
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        
        if (Loader.isModLoaded("ICBM|Explosion"))
        {
            try
            {
                Class.forName("icbm.api.RadarRegistry").getMethod("register", Entity.class).invoke(null, this);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setDead()
    {
        super.setDead();
        
        if (Loader.isModLoaded("ICBM|Explosion"))
        {
            try
            {
                Class.forName("icbm.api.RadarRegistry").getMethod("unregister", Entity.class).invoke(null, this);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        if (this.rocketSoundUpdater != null)
        {
            this.rocketSoundUpdater.update();
        }
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (!this.worldObj.isRemote && this.getLandingPad() != null && this.getLandingPad().getConnectedTiles() != null)
        {
            for (ILandingPadAttachable tile : this.getLandingPad().getConnectedTiles())
            {
                if (this.worldObj.getBlockTileEntity(((TileEntity) tile).xCoord, ((TileEntity) tile).yCoord, ((TileEntity) tile).zCoord) != null && this.worldObj.getBlockTileEntity(((TileEntity) tile).xCoord, ((TileEntity) tile).yCoord, ((TileEntity) tile).zCoord) instanceof GCCoreTileEntityFuelLoader)
                {
                    if (tile instanceof GCCoreTileEntityFuelLoader && ((GCCoreTileEntityFuelLoader) tile).getEnergyStored() > 0)
                    {
                        if (this.launchPhase == EnumLaunchPhase.LAUNCHED.getPhase())
                        {
                            this.setPad(null);
                        }
                    }
                }
            }
        }

        if (this.rumble > 0)
        {
            this.rumble--;
        }

        if (this.rumble < 0)
        {
            this.rumble++;
        }

        if (this.launchPhase == EnumLaunchPhase.IGNITED.getPhase() || this.launchPhase == EnumLaunchPhase.LAUNCHED.getPhase())
        {
            this.performHurtAnimation();

            this.rumble = (float) this.rand.nextInt(3) - 3;
        }

        int i;

        if (this.timeUntilLaunch >= 100)
        {
            i = Math.abs(this.timeUntilLaunch / 100);
        }
        else
        {
            i = 1;
        }

        if ((this.getLaunched() || this.launchPhase == EnumLaunchPhase.IGNITED.getPhase() && this.rand.nextInt(i) == 0) && !GCCoreConfigManager.disableSpaceshipParticles && this.hasValidFuel())
        {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
            {
                this.spawnParticles(this.getLaunched());
            }
        }

        if (this.rocketSoundUpdater != null && (this.launchPhase == EnumLaunchPhase.IGNITED.getPhase() || this.getLaunched()))
        {
            this.rocketSoundUpdater.update();
        }

        if (this.launchPhase == EnumLaunchPhase.LAUNCHED.getPhase() && this.hasValidFuel())
        {
            double d = this.timeSinceLaunch / 250;

            d = Math.min(d, 1);
            
            d *= 5;

            if (!this.landing)
            {
                if (d != 0.0)
                {
                    this.motionY = -d * Math.cos((this.rotationPitch - 180) * Math.PI / 180.0D);
                }
            }
            else
            {
                this.motionY = (this.posY - this.targetTile.yCoord) / -100.0D;
                
                if (this.targetTile != null && this.worldObj.isRemote)
                {
                    landLoop:
                        if (this.posY - this.targetTile.yCoord < 5)
                        {
                            for (int x = MathHelper.floor_double(this.posX) - 1; x <= MathHelper.floor_double(this.posX) + 1; x++)
                            {
                                for (int y = MathHelper.floor_double(this.posY - 3.5D); y <= MathHelper.floor_double(this.posY) + 1; y++)
                                {
                                    for (int z = MathHelper.floor_double(this.posZ) - 1; z <= MathHelper.floor_double(this.posZ) + 1; z++)
                                    {
                                        TileEntity tile = this.worldObj.getBlockTileEntity(x, y, z);
    
                                        if (tile instanceof IFuelDock)
                                        {
                                            this.landRocket(x, y, z);
                                            break landLoop;
                                        }
                                    }
                                }
                            }
                        }
                }
            }

            double multiplier = 1.0D;

            if (this.worldObj.provider instanceof IGalacticraftWorldProvider)
            {
                multiplier = ((IGalacticraftWorldProvider) this.worldObj.provider).getFuelUsageMultiplier();

                if (multiplier <= 0)
                {
                    multiplier = 1;
                }
            }

            if (this.timeSinceLaunch % MathHelper.floor_double(3 * (1 / multiplier)) == 0)
            {
                this.removeFuel(1);
            }
        }
        else if (!this.hasValidFuel() && this.getLaunched() && !this.worldObj.isRemote)
        {
            if (Math.abs(Math.sin(this.timeSinceLaunch / 1000)) / 10 != 0.0)
            {
                this.motionY -= Math.abs(Math.sin(this.timeSinceLaunch / 1000)) / 20;
            }
        }
    }

    protected void spawnParticles(boolean launched)
    {
        final double x1 = 2 * Math.cos(this.rotationYaw * Math.PI / 180.0D) * Math.sin(this.rotationPitch * Math.PI / 180.0D);
        final double z1 = 2 * Math.sin(this.rotationYaw * Math.PI / 180.0D) * Math.sin(this.rotationPitch * Math.PI / 180.0D);
        double y1 = 2 * Math.cos((this.rotationPitch - 180) * Math.PI / 180.0D);

        final double y = this.prevPosY + (this.posY - this.prevPosY);

        if (!this.isDead)
        {
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX + 0.4 - this.rand.nextDouble() / 10 + x1, y - 0.0D + y1, this.posZ + 0.4 - this.rand.nextDouble() / 10 + z1, x1, y1, z1, this.getLaunched());
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX - 0.4 + this.rand.nextDouble() / 10 + x1, y - 0.0D + y1, this.posZ + 0.4 - this.rand.nextDouble() / 10 + z1, x1, y1, z1, this.getLaunched());
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX - 0.4 + this.rand.nextDouble() / 10 + x1, y - 0.0D + y1, this.posZ - 0.4 + this.rand.nextDouble() / 10 + z1, x1, y1, z1, this.getLaunched());
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX + 0.4 - this.rand.nextDouble() / 10 + x1, y - 0.0D + y1, this.posZ - 0.4 + this.rand.nextDouble() / 10 + z1, x1, y1, z1, this.getLaunched());
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX + x1, y - 0.0D + y1, this.posZ + z1, x1, y1, z1, this.getLaunched());
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX + 0.4 + x1, y - 0.0D + y1, this.posZ + z1, x1, y1, z1, this.getLaunched());
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX - 0.4 + x1, y - 0.0D + y1, this.posZ + z1, x1, y1, z1, this.getLaunched());
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX + x1, y - 0.0D + y1, this.posZ + 0.4D + z1, x1, y1, z1, this.getLaunched());
            GalacticraftCore.proxy.spawnParticle("launchflame", this.posX + x1, y - 0.0D + y1, this.posZ - 0.4D + z1, x1, y1, z1, this.getLaunched());
        }
    }

    @Override
    public void readNetworkedData(ByteArrayDataInput dataStream)
    {
        super.readNetworkedData(dataStream);
        this.spaceshipFuelTank.setFluid(new FluidStack(GalacticraftCore.FUEL, dataStream.readInt()));
        this.rocketType = EnumRocketType.values()[dataStream.readInt()];
        this.landing = dataStream.readBoolean();
        this.destinationFrequency = dataStream.readInt();
    }

    @Override
    public ArrayList<Object> getNetworkedData(ArrayList<Object> list)
    {
        super.getNetworkedData(list);
        list.add(this.spaceshipFuelTank.getFluid() == null ? 0 : this.spaceshipFuelTank.getFluid().amount);
        list.add(this.rocketType != null ? this.rocketType.getIndex() : 0);
        list.add(this.landing);
        list.add(this.destinationFrequency);
        return list;
    }

    public boolean hasValidFuel()
    {
        return !(this.spaceshipFuelTank.getFluid() == null || this.spaceshipFuelTank.getFluid().amount == 0);
    }

    @Override
    public void onReachAtmoshpere()
    {
        this.teleport();
    }

    public void teleport()
    {
        worldLoop:
            for (int i = 0; i < FMLCommonHandler.instance().getMinecraftServerInstance().worldServers.length; i++)
            {
                WorldServer world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServers[i];
                
                for (int j = 0; j < world.loadedTileEntityList.size(); j++)
                {
                    TileEntity tile = (TileEntity) world.loadedTileEntityList.get(j);
                    
                    if (tile instanceof GCMarsTileEntityLaunchController)
                    {
                        GCMarsTileEntityLaunchController launchController = (GCMarsTileEntityLaunchController) tile;
                        
                        if (launchController.frequency == this.destinationFrequency && launchController.attachedPad != null)
                        {
                            this.targetTile = launchController.attachedPad;
                            break worldLoop;
                        }
                    }
                }
            }
    
        if (this.targetTile != null)
        {
            if (this.targetTile.worldObj.provider.dimensionId != this.worldObj.provider.dimensionId)
            {
                if (!this.targetTile.worldObj.isRemote && this.targetTile.worldObj instanceof WorldServer)
                {
                    WorldUtil.transferEntityToDimension(this, this.targetTile.worldObj.provider.dimensionId, (WorldServer) this.targetTile.worldObj, false);
                }
            }
            else
            {
                this.setPosition(this.targetTile.xCoord + 0.5F, this.targetTile.yCoord + 200, this.targetTile.zCoord + 0.5F);
                this.landing = true;
            }
        }
        else
        {
            this.setDead();
        }
        
        if (this.riddenByEntity != null)
        {
            if (this.riddenByEntity instanceof GCCorePlayerMP)
            {
                GCCorePlayerMP player = (GCCorePlayerMP) this.riddenByEntity;

                HashMap<String, Integer> map = WorldUtil.getArrayOfPossibleDimensions(WorldUtil.getPossibleDimensionsForSpaceshipTier(this.getRocketTier()), player);

                String temp = "";
                int count = 0;

                for (Entry<String, Integer> entry : map.entrySet())
                {
                    temp = temp.concat(entry.getKey() + (count < map.entrySet().size() - 1 ? "." : ""));
                    count++;
                }

                player.playerNetServerHandler.sendPacketToPlayer(PacketUtil.createPacket(GalacticraftCore.CHANNEL, 2, new Object[] { player.username, temp }));
                player.setSpaceshipTier(this.getRocketTier());
                player.setUsingPlanetGui();

                this.onTeleport(player);
                player.mountEntity(this);

                if (!this.isDead)
                {
                    this.setDead();
                }
            }
        }
    }

    @Override
    protected void failRocket()
    {
        if (this.landing && this.targetTile != null && this.worldObj.getBlockTileEntity(this.targetTile.xCoord, this.targetTile.yCoord, this.targetTile.zCoord) == this.targetTile && this.posY - this.targetTile.yCoord < 5)
        {
            for (int x = MathHelper.floor_double(this.posX) - 1; x <= MathHelper.floor_double(this.posX) + 1; x++)
            {
                for (int y = MathHelper.floor_double(this.posY - 2.5D); y <= MathHelper.floor_double(this.posY) + 1; y++)
                {
                    for (int z = MathHelper.floor_double(this.posZ) - 1; z <= MathHelper.floor_double(this.posZ) + 1; z++)
                    {
                        TileEntity tile = this.worldObj.getBlockTileEntity(x, y, z);
                        
                        if (tile instanceof IFuelDock)
                        {
                            this.landRocket(x, y, z);
                        }
                    }
                }
            }
        }
        else
        {
            super.failRocket();
        }
    }
    
    private void landRocket(int x, int y, int z)
    {
        TileEntity tile = this.worldObj.getBlockTileEntity(x, y, z);
        
        if (tile instanceof IFuelDock)
        {
            IFuelDock dock = (IFuelDock) tile;
            
            if (this.isDockValid(dock))
            {
                if (!this.worldObj.isRemote)
                {
                    this.launchPhase = EnumLaunchPhase.UNIGNITED.getPhase();
                    this.landing = false;
                }
                
                this.setPosition(this.posX, y + 0.2 + this.yOffset, this.posZ);
                return;
            }
        }
    }

    public void onTeleport(EntityPlayerMP player)
    {
        ;
    }
    
    @Override
    public void ignite()
    {
        boolean frequencySet = false;
        
        blockLoop:
            for (int x = MathHelper.floor_double(this.posX) - 1; x <= MathHelper.floor_double(this.posX) + 1; x++)
            {
                for (int y = MathHelper.floor_double(this.posY) - 3; y <= MathHelper.floor_double(this.posY) + 1; y++)
                {
                    for (int z = MathHelper.floor_double(this.posZ) - 1; z <= MathHelper.floor_double(this.posZ) + 1; z++)
                    {
                        TileEntity tile = this.worldObj.getBlockTileEntity(x, y, z);
                        
                        if (tile instanceof IFuelDock)
                        {
                            IFuelDock dock = (IFuelDock) tile;
                            
                            GCMarsTileEntityLaunchController launchController = null;
                            
                            for (ILandingPadAttachable connectedTile : dock.getConnectedTiles())
                            {
                                if (connectedTile instanceof GCMarsTileEntityLaunchController)
                                {
                                    launchController = (GCMarsTileEntityLaunchController) connectedTile;
                                    break;
                                }
                            }
                            
                            if (launchController != null)
                            {
                                if (!launchController.getDisabled(0) && launchController.getEnergyStored() > 0.0F)
                                {
                                    if (launchController.frequencyValid && launchController.destFrequencyValid)
                                    {
                                        this.destinationFrequency = launchController.destFrequency;
                                        frequencySet = true;
                                        break blockLoop;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        
        
        if (frequencySet)
        {
            super.ignite();
        }
    }

    @Override
    public void onLaunch()
    {
        super.onLaunch();

        if (!this.worldObj.isRemote)
        {
            if (!(this.worldObj.provider instanceof IOrbitDimension) && this.riddenByEntity != null && this.riddenByEntity instanceof GCCorePlayerMP)
            {
                ((GCCorePlayerMP) this.riddenByEntity).setCoordsTeleportedFromX(this.riddenByEntity.posX);
                ((GCCorePlayerMP) this.riddenByEntity).setCoordsTeleportedFromZ(this.riddenByEntity.posZ);
            }
            
            int amountRemoved = 0;

            for (int x = MathHelper.floor_double(this.posX) - 1; x <= MathHelper.floor_double(this.posX) + 1; x++)
            {
                for (int y = MathHelper.floor_double(this.posY) - 3; y <= MathHelper.floor_double(this.posY) + 1; y++)
                {
                    for (int z = MathHelper.floor_double(this.posZ) - 1; z <= MathHelper.floor_double(this.posZ) + 1; z++)
                    {
                        final int id = this.worldObj.getBlockId(x, y, z);
                        final Block block = Block.blocksList[id];

                        if (block != null && block instanceof GCCoreBlockLandingPadFull)
                        {
                            if (amountRemoved < 9)
                            {
                                GCCoreLandingPadRemovalEvent event = new GCCoreLandingPadRemovalEvent(this.worldObj, x, y, z);
                                MinecraftForge.EVENT_BUS.post(event);

                                if (event.allow)
                                {
                                    this.worldObj.setBlockToAir(x, y, z);
                                    amountRemoved = 9;
                                }
                            }
                        }
                    }
                }
            }

            this.playSound("random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }
    }

    @Override
    public boolean interactFirst(EntityPlayer par1EntityPlayer)
    {
        if (!this.worldObj.isRemote)
        this.ignite();
//        if (this.launchPhase == EnumLaunchPhase.LAUNCHED.getPhase())
//        {
//            return false;
//        }
//
//        if (this.riddenByEntity != null && this.riddenByEntity instanceof GCCorePlayerMP)
//        {
//            if (!this.worldObj.isRemote)
//            {
//                final Object[] toSend = { ((EntityPlayerMP) this.riddenByEntity).username };
//                ((EntityPlayerMP) this.riddenByEntity).playerNetServerHandler.sendPacketToPlayer(PacketUtil.createPacket(GalacticraftCore.CHANNEL, 13, toSend));
//                final Object[] toSend2 = { 0 };
//                ((EntityPlayerMP) par1EntityPlayer).playerNetServerHandler.sendPacketToPlayer(PacketUtil.createPacket(GalacticraftCore.CHANNEL, 22, toSend2));
//                ((GCCorePlayerMP) par1EntityPlayer).setChatCooldown(0);
//                par1EntityPlayer.mountEntity(null);
//            }
//
//            return true;
//        }
//        else if (par1EntityPlayer instanceof GCCorePlayerMP)
//        {
//            if (!this.worldObj.isRemote)
//            {
//                final Object[] toSend = { par1EntityPlayer.username };
//                ((EntityPlayerMP) par1EntityPlayer).playerNetServerHandler.sendPacketToPlayer(PacketUtil.createPacket(GalacticraftCore.CHANNEL, 8, toSend));
//                final Object[] toSend2 = { 1 };
//                ((EntityPlayerMP) par1EntityPlayer).playerNetServerHandler.sendPacketToPlayer(PacketUtil.createPacket(GalacticraftCore.CHANNEL, 22, toSend2));
//                ((GCCorePlayerMP) par1EntityPlayer).setChatCooldown(0);
//                par1EntityPlayer.mountEntity(this);
//            }
//
//            return true;
//        }

        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt)
    {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("Type", this.rocketType.getIndex());

        if (this.spaceshipFuelTank.getFluid() != null)
        {
            nbt.setTag("fuelTank", this.spaceshipFuelTank.writeToNBT(new NBTTagCompound()));
        }

        if (this.getSizeInventory() > 0)
        {
            final NBTTagList var2 = new NBTTagList();

            for (int var3 = 0; var3 < this.cargoItems.length; ++var3)
            {
                if (this.cargoItems[var3] != null)
                {
                    final NBTTagCompound var4 = new NBTTagCompound();
                    var4.setByte("Slot", (byte) var3);
                    this.cargoItems[var3].writeToNBT(var4);
                    var2.appendTag(var4);
                }
            }

            nbt.setTag("Items", var2);
        }
        
        nbt.setBoolean("TargetValid", this.targetTile != null);
        
        if (this.targetTile != null)
        {
            nbt.setInteger("targetTileX", this.targetTile.xCoord);
            nbt.setInteger("targetTileY", this.targetTile.yCoord);
            nbt.setInteger("targetTileZ", this.targetTile.zCoord);
        }
        
        nbt.setBoolean("Landing", this.landing);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt)
    {
        super.readEntityFromNBT(nbt);
        this.rocketType = EnumRocketType.values()[nbt.getInteger("Type")];

        if (nbt.hasKey("fuelTank"))
        {
            this.spaceshipFuelTank.readFromNBT(nbt.getCompoundTag("fuelTank"));
        }

        if (this.getSizeInventory() > 0)
        {
            final NBTTagList var2 = nbt.getTagList("Items");
            this.cargoItems = new ItemStack[this.getSizeInventory()];

            for (int var3 = 0; var3 < var2.tagCount(); ++var3)
            {
                final NBTTagCompound var4 = (NBTTagCompound) var2.tagAt(var3);
                final int var5 = var4.getByte("Slot") & 255;

                if (var5 >= 0 && var5 < this.cargoItems.length)
                {
                    this.cargoItems[var5] = ItemStack.loadItemStackFromNBT(var4);
                }
            }
        }
        
        if (nbt.getBoolean("TargetValid") && nbt.hasKey("targetTileX"))
        {
            this.targetTile = this.worldObj.getBlockTileEntity(nbt.getInteger("targetTileX"), nbt.getInteger("targetTileY"), nbt.getInteger("targetTileZ"));
        }
        
        this.landing = nbt.getBoolean("Landing");
    }

    @Override
    public EnumRocketType getType()
    {
        return this.rocketType;
    }

    @Override
    public int getSizeInventory()
    {
        return this.rocketType.getInventorySpace();
    }

    @Override
    public int addFuel(FluidStack liquid, boolean doFill)
    {
        final FluidStack liquidInTank = this.spaceshipFuelTank.getFluid();

        if (liquid != null && FluidRegistry.getFluidName(liquid).equalsIgnoreCase("Fuel"))
        {
            if (liquidInTank == null || liquidInTank.amount + liquid.amount <= this.spaceshipFuelTank.getCapacity())
            {
                return this.spaceshipFuelTank.fill(liquid, doFill);
            }
        }

        return 0;
    }

    @Override
    public FluidStack removeFuel(int amount)
    {
        return this.spaceshipFuelTank.drain(amount, true);
    }

    @Override
    public EnumCargoLoadingState addCargo(ItemStack stack, boolean doAdd)
    {
        if (this.rocketType.getInventorySpace() <= 3)
        {
            return EnumCargoLoadingState.NOINVENTORY;
        }

        int count = 0;

        for (count = 0; count < this.cargoItems.length - 2; count++)
        {
            ItemStack stackAt = this.cargoItems[count];

            if (stackAt != null && stackAt.itemID == stack.itemID && stackAt.getItemDamage() == stack.getItemDamage() && stackAt.stackSize < stackAt.getMaxStackSize())
            {
                if (doAdd)
                {
                    this.cargoItems[count].stackSize += stack.stackSize;
                }

                return EnumCargoLoadingState.SUCCESS;
            }
        }

        for (count = 0; count < this.cargoItems.length - 3; count++)
        {
            ItemStack stackAt = this.cargoItems[count];

            if (stackAt == null)
            {
                if (doAdd)
                {
                    this.cargoItems[count] = stack;
                }

                return EnumCargoLoadingState.SUCCESS;
            }
        }

        return EnumCargoLoadingState.FULL;
    }

    @Override
    public RemovalResult removeCargo(boolean doRemove)
    {
        for (int i = 0; i < this.cargoItems.length - 2; i++)
        {
            ItemStack stackAt = this.cargoItems[i];

            if (stackAt != null)
            {
                if (doRemove && --this.cargoItems[i].stackSize <= 0)
                {
                    this.cargoItems[i] = null;
                }

                return new RemovalResult(EnumCargoLoadingState.SUCCESS, new ItemStack(stackAt.itemID, 1, stackAt.getItemDamage()));
            }
        }

        return new RemovalResult(EnumCargoLoadingState.EMPTY, null);
    }

    @Override
    public void onWorldTransferred(World world)
    {
        if (this.targetTile != null)
        {
            this.setPosition(this.targetTile.xCoord + 0.5F, this.targetTile.yCoord + 200, this.targetTile.zCoord + 0.5F);
            this.landing = true;
        }
        else
        {
            this.setDead();
        }
    }

    @Override
    public ItemStack getStackInSlot(int par1)
    {
        return this.cargoItems[par1];
    }

    @Override
    public ItemStack decrStackSize(int par1, int par2)
    {
        if (this.cargoItems[par1] != null)
        {
            ItemStack var3;

            if (this.cargoItems[par1].stackSize <= par2)
            {
                var3 = this.cargoItems[par1];
                this.cargoItems[par1] = null;
                return var3;
            }
            else
            {
                var3 = this.cargoItems[par1].splitStack(par2);

                if (this.cargoItems[par1].stackSize == 0)
                {
                    this.cargoItems[par1] = null;
                }

                return var3;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int par1)
    {
        if (this.cargoItems[par1] != null)
        {
            final ItemStack var2 = this.cargoItems[par1];
            this.cargoItems[par1] = null;
            return var2;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
    {
        this.cargoItems[par1] = par2ItemStack;

        if (par2ItemStack != null && par2ItemStack.stackSize > this.getInventoryStackLimit())
        {
            par2ItemStack.stackSize = this.getInventoryStackLimit();
        }
    }

    @Override
    public String getInvName()
    {
        return LanguageRegistry.instance().getStringLocalization("container.spaceship.name");
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public void onInventoryChanged()
    {
    }

    @Override
    public void openChest()
    {
    }

    @Override
    public void closeChest()
    {
    }

    @Override
    public boolean isInvNameLocalized()
    {
        return true;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer)
    {
        return this.isDead ? false : entityplayer.getDistanceSqToEntity(this) <= 64.0D;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack)
    {
        return false;
    }

    @Override
    public void setPad(IFuelDock pad)
    {
        this.landingPad = pad;
    }

    @Override
    public IFuelDock getLandingPad()
    {
        return this.landingPad;
    }

    @Override
    public void onPadDestroyed()
    {
        if (!this.isDead && this.launchPhase != EnumLaunchPhase.LAUNCHED.getPhase())
        {
            this.dropShipAsItem();
            this.setDead();
        }
    }

    @Override
    public boolean isDockValid(IFuelDock dock)
    {
        return dock instanceof GCCoreTileEntityLandingPad || dock instanceof GCCoreTileEntityCargoPad;
    }

    @Override
    public int getRocketTier()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxFuel()
    {
        return this.spaceshipFuelTank.getCapacity();
    }

    @Override
    public int getPreLaunchWait()
    {
        return 20;
    }
    
    @Override
    public List<ItemStack> getItemsDropped()
    {
        final List<ItemStack> items = new ArrayList<ItemStack>();
        items.add(new ItemStack(GCCoreItems.rocketTier1, 1, this.rocketType.getIndex()));

        if (this.cargoItems != null)
        {
            for (final ItemStack item : this.cargoItems)
            {
                if (item != null)
                {
                    items.add(item);
                }
            }
        }

        return items;
    }
    
    @RuntimeInterface(clazz = "icbm.api.IMissileLockable", modID = "ICBM|Explosion")
    public boolean canLock(IMissile missile)
    {
        return true;
    }

    @RuntimeInterface(clazz = "icbm.api.IMissileLockable", modID = "ICBM|Explosion")
    public Vector3 getPredictedPosition(int ticks)
    {
        return new Vector3(this);
    }

    @RuntimeInterface(clazz = "icbm.api.sentry.IAATarget", modID = "ICBM|Explosion")
    public void destroyCraft()
    {
        this.setDead();
    }

    @RuntimeInterface(clazz = "icbm.api.sentry.IAATarget", modID = "ICBM|Explosion")
    public int doDamage(int damage)
    {
        return (int) (this.shipDamage += damage);
    }

    @RuntimeInterface(clazz = "icbm.api.sentry.IAATarget", modID = "ICBM|Explosion")
    public boolean canBeTargeted(Object entity)
    {
        return this.launchPhase == EnumLaunchPhase.LAUNCHED.getPhase() && this.timeSinceLaunch > 50;
    }
}