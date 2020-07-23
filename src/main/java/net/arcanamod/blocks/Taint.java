package net.arcanamod.blocks;

import com.google.common.collect.Lists;
import net.arcanamod.Arcana;
import net.arcanamod.aspects.VisShareable;
import net.arcanamod.blocks.tainted.TaintedFallingBlock;
import net.arcanamod.blocks.tainted.TaintedPlantBlock;
import net.arcanamod.blocks.tiles.JarTileEntity;
import net.arcanamod.entities.tainted.*;
import net.arcanamod.fluids.ArcanaFluids;
import net.arcanamod.world.ServerAuraView;
import net.minecraft.block.*;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.IShearable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static net.arcanamod.blocks.DelegatingBlock.switchBlock;

public class Taint{
	
	public static final BooleanProperty UNTAINTED = BooleanProperty.create("untainted"); // false by default
	private static final Map<Block, Block> taintMap = new HashMap<>();
	private static final Map<Block, Block> deadMap = new HashMap<>();

	public static void init(){
		addDeadUnstableBlock(
				Blocks.OAK_LEAVES,Blocks.BIRCH_LEAVES,Blocks.SPRUCE_LEAVES,Blocks.JUNGLE_LEAVES,Blocks.DARK_OAK_LEAVES,Blocks.ACACIA_LEAVES,
				ArcanaBlocks.DAIR_LEAVES.get(),ArcanaBlocks.EUCALYPTUS_LEAVES.get(),ArcanaBlocks.GREATWOOD_LEAVES.get(),ArcanaBlocks.HAWTHORN_LEAVES.get(),ArcanaBlocks.SILVERWOOD_LEAVES.get()
		);
	}
	
	@SuppressWarnings("deprecation")
	public static Block taintedOf(Block parent, Block... blocks){
		Block tainted;
		if (parent instanceof FallingBlock)
			tainted = new TaintedFallingBlock(parent);
		else if (parent instanceof IPlantable || parent instanceof IShearable || parent instanceof IGrowable)
			tainted = new TaintedPlantBlock(parent);
		else
			tainted = new TaintedBlock(parent);

		//Add children to TaintMapping, NOT parent (see TaintedBlock)!
		for (Block block : blocks) {
			Taint.addTaintMapping(block, tainted);
		}

		return tainted;
	}

	public static void addDeadUnstableBlock(Block... blocks) {
		deadMap.putAll(Lists.newArrayList(blocks).stream().collect(Collectors.toMap(block -> block,block -> Blocks.AIR)));
	}

	@SuppressWarnings("deprecation")
	public static Block deadOf(Block parent, Block... blocks){
		Block dead;
		if (parent instanceof IPlantable || parent instanceof IShearable || parent instanceof IGrowable)
			dead = new DeadPlantBlock(parent);
		else
			dead = new DeadBlock(parent);

		//Add children to DeadMapping, NOT parent (see DeadBlock)!
		for (Block block : blocks) {
			Taint.addDeadMapping(block, dead);
		}

		return dead;
	}
	
	public static Block getPureOfBlock(Block block){
		return taintMap.entrySet().stream()
				.filter(entry -> entry.getValue() == block)
				.map(Map.Entry::getKey)
				.findAny().orElse(null);
	}

	public static Block getLivingOfBlock(Block block){
		return deadMap.entrySet().stream()
				.filter(entry -> entry.getValue() == block)
				.map(Map.Entry::getKey)
				.findAny().orElse(null);
	}

	public static Block getDeadOfBlock(Block block){
		return deadMap.getOrDefault(block,block);
	}
	
	public static Block getTaintedOfBlock(Block block){
		return taintMap.get(block);
	}

	public static void addTaintMapping(Block original, Block tainted){
		taintMap.put(original, tainted);
	}

	public static void addDeadMapping(Block original, Block dead){
		deadMap.put(original, dead);
	}
	
	public static void tickTaintedBlock(BlockState state, ServerWorld world, BlockPos pos, Random random){
		if(state.getBlock() == ArcanaFluids.TAINT_FLUID_BLOCK.get() || !state.get(UNTAINTED)){
			// and if flux level is greater than 5,
			ServerAuraView auraView = new ServerAuraView(world);
			int at = auraView.getTaintAt(pos);
			if(at > 5){
				// pick a block within a 4x6x4 area
				// If this block is air, stop. If this block doesn't have a tainted form, re-roll.
				// Do this up to 5 times.
				Block tainted = null;
				BlockPos taintingPos = pos;
				int iter = 0;
				while(tainted == null && iter < 5){
					taintingPos = pos.north(random.nextInt(9) - 4).west(random.nextInt(9) - 4).up(random.nextInt(13) - 6);
					tainted = world.getBlockState(taintingPos).getBlock();
					if(tainted.isAir(world.getBlockState(taintingPos), world, taintingPos)){
						tainted = null;
						break;
					}
					tainted = Taint.getTaintedOfBlock(tainted);
					iter++;
				}
				// Replace it with its tainted form if found.
				if(tainted != null){
					BlockState taintedState = switchBlock(world.getBlockState(taintingPos), tainted).with(UNTAINTED, false);
					world.setBlockState(taintingPos, taintedState);
					// Reduce flux level
					auraView.addTaintAt(pos, -1);
					// Schedule a tick
					world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), taintTickWait(at));
				}
			}
		}
	}
	
	public static int taintTickWait(int taintLevel){
		// more taint level -> less tick wait
		int base = (int)((1d / taintLevel) * 200);
		return base > 0 ? base : 1;
	}

	public static void tickTaintInContainer(Object sender) {
		if (sender instanceof JarTileEntity){
			JarTileEntity jar = (JarTileEntity)sender;
			if (!((VisShareable)jar).isSecure()) {
				if (jar.getWorld().rand.nextInt(20) == 2)
					jar.vis.drain(0, 1, false);
				if (jar.getWorld().isRemote) return;
				ServerAuraView auraView = new ServerAuraView((ServerWorld) jar.getWorld());
				if (jar.getWorld().rand.nextInt(20) == 2)
					auraView.addTaintAt(jar.getPos(), 1);
			}
		}
	}
	
	public static boolean isAreaInTaintBiome(BlockPos pos, World world){
		// check if they're in a taint biome
		// 7x13x7 cube, centred on the entity
		// at least 20 tainted blocks
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int counter = 0;
		for(int x = -3; x < 7; x++)
			for(int y = -6; y < 13; y++)
				for(int z = -3; z < 7; z++){
					mutable.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
					if(isTainted(world.getBlockState(mutable).getBlock()))
						counter++;
					if(counter >= 20)
						return true;
				}
		return false;
	}
	
	@SuppressWarnings({"rawtypes"})
	private static final Map<EntityType, EntityType> entityTaintMap = new HashMap<>();

	@SuppressWarnings({"rawtypes"})
	public static EntityType taintedEntityOf(EntityType entity){
		if(entity.getRegistryName() == null)
			return null;
		EntityType tainted;
		if(entity == EntityType.BAT)
			tainted = EntityType.Builder.create(TaintedBatEntity::new, EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		else if(entity == EntityType.EVOKER || entity == EntityType.ILLUSIONER || entity == EntityType.VINDICATOR || entity == EntityType.PILLAGER)
			tainted = EntityType.Builder.create(TaintedIllagerEntity::new, EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		else if(entity == EntityType.CREEPER)
			tainted = EntityType.Builder.create(TaintedCreeperEntity::new, EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		else if(entity == EntityType.SQUID)
			tainted = EntityType.Builder.create(TaintedSquidEntity::new, EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		else if(entity == EntityType.GHAST)
			tainted = EntityType.Builder.create(TaintedGhastEntity::new, EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		else if(entity == EntityType.CAVE_SPIDER)
			tainted = EntityType.Builder.create(TaintedCaveSpiderEntity::new, EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		else if(entity == EntityType.SKELETON)
			tainted = EntityType.Builder.create(TaintedSkeletonEntity::new, EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		else if (entity == EntityType.SLIME)
			tainted = EntityType.Builder.create(TaintedSlimeEntity::new, EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		else
			tainted = EntityType.Builder.<TaintedEntity>create((p_create_1_, p_create_2_) -> new TaintedEntity(p_create_1_, p_create_2_, entity), EntityClassification.MONSTER)
					.size(entity.getSize().width, entity.getSize().height).build(new ResourceLocation(Arcana.MODID, "tainted_" + entity.getRegistryName().getPath()).toString());
		entityTaintMap.put(entity, tainted);
		return tainted;
	}

	@SuppressWarnings({"rawtypes"})
	public static EntityType getTaintedOfEntity(EntityType entity) {
		return entityTaintMap.get(entity);
	}

	public static boolean isTainted(EntityType<?> entity) {
		return entityTaintMap.containsValue(entity);
	}
	
	public static boolean isTainted(Block block) {
		return taintMap.containsValue(block);
	}

	@SuppressWarnings("rawtypes")
	public static Collection<EntityType> getTaintedEntities(){
		return entityTaintMap.values();
	}
}