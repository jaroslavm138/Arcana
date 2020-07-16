package net.arcanamod.event;

import net.arcanamod.aspects.AspectUtils;
import net.arcanamod.aspects.Aspects;
import net.arcanamod.aspects.IAspectHandler;
import net.arcanamod.blocks.tiles.AspectBookshelfTileEntity;
import net.arcanamod.blocks.tiles.JarTileEntity;
import net.arcanamod.client.render.ArcanaParticles;
import net.arcanamod.client.render.AspectParticleData;
import net.arcanamod.client.render.NumberParticleData;
import net.arcanamod.items.ArcanaItems;
import net.arcanamod.util.GogglePriority;
import net.arcanamod.util.RayTraceUtils;
import net.arcanamod.world.AuraView;
import net.arcanamod.world.Node;
import net.arcanamod.world.ServerAuraView;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;

@Mod.EventBusSubscriber
public class PlayerTickHandler{

	@SubscribeEvent
	public static void tick(TickEvent.PlayerTickEvent event){
		PlayerEntity player = event.player;
		
		if(player instanceof ServerPlayerEntity && event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END){
			ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)player;
			// If the player is near a node,
			AuraView view = new ServerAuraView(serverPlayerEntity.getServerWorld());
			Collection<Node> ranged = new ArrayList<>(view.getNodesWithinAABB(player.getBoundingBox().grow(2)));
			
			// and is holding the scribbled notes item,
			if(!ranged.isEmpty() && player.inventory.hasItemStack(new ItemStack(ArcanaItems.SCRIBBLED_NOTES.get()))){
				// it switches it for a complete version,
				player.inventory.setInventorySlotContents(player.inventory.getSlotFor(new ItemStack(ArcanaItems.SCRIBBLED_NOTES.get())), new ItemStack(ArcanaItems.SCRIBBLED_NOTES_COMPLETE.get()));
				// and gives them a status message.
				ITextComponent status = new TranslationTextComponent("status.get_complete_note").applyTextStyles(TextFormatting.ITALIC, TextFormatting.LIGHT_PURPLE);
				serverPlayerEntity.sendStatusMessage(status, false);
			}
		}
		
		if(player instanceof ClientPlayerEntity && event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.END){
			ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)player;
			World world = clientPlayerEntity.world;
			BlockPos pos = RayTraceUtils.getTargetBlockPos(clientPlayerEntity, world, 6);
			TileEntity te = world.getTileEntity(pos);
			if(te instanceof JarTileEntity){
				if(world.isRemote()){
					GogglePriority priority = GogglePriority.getClientGogglePriority();
					JarTileEntity jte = (JarTileEntity)te;
					if(priority == GogglePriority.SHOW_NODE || priority == GogglePriority.SHOW_ASPECTS)
						if(jte.vis.getHolder(0).getContainedAspect() != Aspects.EMPTY){
							double srx = (-Math.sin(Math.toRadians(clientPlayerEntity.rotationYaw)));
							double crx = (Math.cos(Math.toRadians(clientPlayerEntity.rotationYaw)));
							world.addParticle(new AspectParticleData(AspectUtils.getAspectTextureLocation(jte.vis.getHolder(0).getContainedAspect()), ArcanaParticles.ASPECT_PARTICLE.get()),
									pos.getX() + 0.5D + ((-srx) / 2), pos.getY() + 0.8D, pos.getZ() + 0.5D + ((-crx) / 2), 0, 0, 0);
						}
				}
			}
			if(te instanceof AspectBookshelfTileEntity){
				if(world.isRemote()){
					GogglePriority priority = GogglePriority.getClientGogglePriority();
					AspectBookshelfTileEntity abte = (AspectBookshelfTileEntity)te;
					if(priority == GogglePriority.SHOW_NODE || priority == GogglePriority.SHOW_ASPECTS){
						IAspectHandler vis = IAspectHandler.getFrom(abte);
						for(int i = 0; i < vis.getHoldersAmount(); i++){
							if(vis.getHolder(i).getContainedAspect() != Aspects.EMPTY){
								double srx = (-Math.sin(Math.toRadians(clientPlayerEntity.rotationYaw)));
								double crx = (Math.cos(Math.toRadians(clientPlayerEntity.rotationYaw)));
								world.addParticle(new AspectParticleData(AspectUtils.getAspectTextureLocation(vis.getHolder(i).getContainedAspect()), ArcanaParticles.ASPECT_PARTICLE.get()),
										pos.getX() + 0.5D + ((-srx) / 2), pos.getY() + 0.8D, pos.getZ() + 0.5D + ((-crx) / 2), 0, 0, 0);
								int currVis = vis.getHolder(i).getCurrentVis();
								int currVis_4th = Integer.parseInt((""+currVis).substring((""+currVis).length()-1));
								world.addParticle(new NumberParticleData(currVis_4th, ArcanaParticles.NUMBER_PARTICLE.get()),
										pos.getX() + 0.5D + ((-srx) / 2) + 0.08f, pos.getY() + 0.71D, pos.getZ() + 0.5D + ((-crx) / 2)+ 0.05f, 0, 0, 0);
								// TODO: Needs more tweak.
							}
						}
					}
				}
			}
			/*if(te instanceof CrucibleTileEntity){
				if(world.isRemote()){
					GogglePriority priority = GogglePriority.getGogglePriority();
					CrucibleTileEntity crucible = (CrucibleTileEntity)te;
					if(priority == GogglePriority.SHOW_NODE || priority == GogglePriority.SHOW_ASPECTS){
						List<AspectStack> stacks = crucible.getAspectStacks();
						float rotation = player.rotationYaw;
						double rotOffsetX = -Math.cos(Math.toRadians(rotation));
						double rotOffsetZ = -Math.sin(Math.toRadians(rotation));
						double aspectSize = .4;
						double x = pos.getX() + .5 - rotOffsetX * (stacks.size() / 2f) * aspectSize / 2, y = pos.getY() + 1.2, z = pos.getZ() + .5 - rotOffsetZ * stacks.size() * aspectSize / 2;
						for(int i = 0, size = stacks.size(); i < size; i++){
							AspectStack stack = stacks.get(i);
							renderNumberParticles(x + rotOffsetX * i * aspectSize, y, z + rotOffsetZ * i * aspectSize, rotation, stack.getAmount(), world);
							world.addParticle(new AspectParticleData(AspectUtils.getAspectTextureLocation(stack.getAspect()), ArcanaParticles.ASPECT_PARTICLE.get()),
									x + rotOffsetX * i * aspectSize, y + .2, z + rotOffsetZ * i * aspectSize, 0, 0, 0);
						}
					}
				}
			}*/
		}
	}
	
	private static void renderNumberParticles(double baseX, double baseY, double baseZ, float rotation, int number, World world){
		String numberStr = String.valueOf(number);
		char[] array = numberStr.toCharArray();
		double rotOffsetX = -Math.cos(Math.toRadians(rotation));
		double rotOffsetZ = -Math.sin(Math.toRadians(rotation));
		double size = .1;
		double x = baseX - array.length * rotOffsetX * size;
		double z = baseZ - array.length * rotOffsetZ * size;
		for(int i = 0, length = array.length; i < length; i++){
			char c = array[i];
			world.addParticle(new NumberParticleData(Integer.parseInt(String.valueOf(c)), ArcanaParticles.NUMBER_PARTICLE.get()), false, x + rotOffsetX * i * size, baseY, z + rotOffsetZ * i * size, 0, 0, 0);
		}
	}
}