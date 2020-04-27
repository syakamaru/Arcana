package net.arcanamod.blocks.saplings;

import net.arcanamod.worldgen.trees.DairGenerator;
import net.arcanamod.blocks.bases.SaplingBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

/**
 * @author Mozaran
 * <p>
 * Used to grow all variations of the dair tree
 */
public class DairSapling extends SaplingBase{
	boolean tainted;
	boolean untainted;
	
	public DairSapling(String name, boolean tainted, boolean untainted){
		super(name);
		this.tainted = tainted;
		this.untainted = untainted;
	}
	
	@Override
	public void generateTree(World worldIn, BlockPos pos, IBlockState state, Random rand){
		if(!TerrainGen.saplingGrowTree(worldIn, rand, pos))
			return;
		
		WorldGenerator worldgenerator;
		if(tainted){
			worldgenerator = untainted ? new DairGenerator(true, true, true) : new DairGenerator(true, true, false);
		}else{
			worldgenerator = new DairGenerator(true, false);
		}
		
		worldIn.setBlockState(pos, Blocks.AIR.getDefaultState(), 4);
		
		if(!worldgenerator.generate(worldIn, rand, pos)){
			worldIn.setBlockState(pos, state, 4);
		}
	}
}