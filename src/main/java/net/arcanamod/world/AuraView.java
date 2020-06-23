package net.arcanamod.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.DistExecutor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface AuraView{
	
	Function<World, AuraView> SIDED_FACTORY = DistExecutor.runForDist(() -> () -> (world) -> world instanceof ClientWorld ? new ClientAuraView((ClientWorld)world) : null, () -> () -> (world) -> world instanceof ServerWorld ? new ServerAuraView((ServerWorld)world) : null);
	
	Collection<Node> getAllNodes();
	
	World getWorld();
	
	default AuraChunk getAuraChunk(ChunkPos pos){
		IChunk chunk = getWorld().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
		if(chunk instanceof Chunk)
			return AuraChunk.getFrom((Chunk)chunk);
		return null;
	}
	
	default AuraChunk getAuraChunk(BlockPos pos){
		return getAuraChunk(new ChunkPos(pos));
	}
	
	default Collection<Node> getNodesWithinChunk(ChunkPos pos){
		AuraChunk nc = getAuraChunk(pos);
		return nc != null ? nc.getNodes() : Collections.emptyList();
	}
	
	default int getTaintWithinChunk(ChunkPos pos){
		AuraChunk nc = getAuraChunk(pos);
		return nc != null ? nc.getTaintLevel() : -1;
	}
	
	default int getTaintAt(BlockPos pos){
		return getTaintWithinChunk(new ChunkPos(pos));
	}
	
	/**
	 * Adds taint to a particular chunk. Returns the previous taint level, or -1 if the chunk isn't loaded.
	 *
	 * @param pos
	 * 		The chunk to add taint to.
	 * @return The previous taint level.
	 */
	default int addTaintToChunk(ChunkPos pos, int amount){
		AuraChunk nc = getAuraChunk(pos);
		if(nc != null){
			int level = nc.getTaintLevel();
			nc.addTaint(amount);
			return level;
		}else{
			return -1;
		}
	}
	
	default int addTaintAt(BlockPos pos, int amount){
		return addTaintToChunk(new ChunkPos(pos), amount);
	}
	
	/**
	 * Sets the taint level of a particular chunk. Returns the previous taint level, or -1 if the chunk isn't loaded.
	 *
	 * @param pos
	 * 		The chunk to set the taint of.
	 * @return The previous taint level.
	 */
	default int setTaintOfChunk(ChunkPos pos, int amount){
		AuraChunk nc = getAuraChunk(pos);
		if(nc != null){
			int level = nc.getTaintLevel();
			nc.setTaint(amount);
			return level;
		}else{
			return -1;
		}
	}
	
	default int setTaintAt(BlockPos pos, int amount){
		return setTaintOfChunk(new ChunkPos(pos), amount);
	}
	
	default Collection<Node> getNodesWithinAABB(AxisAlignedBB bounds){
		// get all related chunks
		// that is, all chunks between minX and maxX, minZ and maxZ
		ChunkPos min = new ChunkPos(new BlockPos(bounds.minX, 0, bounds.minZ));
		ChunkPos max = new ChunkPos(new BlockPos(bounds.maxX, 0, bounds.maxZ));
		List<ChunkPos> relevant = new ArrayList<>();
		if(!min.equals(max))
			for(int xx = min.x; xx <= max.x; xx++)
				for(int zz = min.z; zz <= max.z; zz++)
					relevant.add(new ChunkPos(xx, zz));
		else
			relevant.add(min);
		//then getNodesWithinAABB for each
		List<Node> list = new ArrayList<>();
		for(ChunkPos pos : relevant)
			if(getAuraChunk(pos) != null)
				list.addAll(getAuraChunk(pos).getNodesWithinAABB(bounds));
		return list;
	}
	
	default Collection<Node> getNodesOfType(NodeType type){
		return getAllNodes().stream()
				.filter(node -> node.type() == type)
				.collect(Collectors.toList());
	}
	
	default Collection<Node> getNodesOfTypeWithinAABB(AxisAlignedBB bounds, NodeType type){
		// get all related chunks
		// that is, all chunks between minX and maxX, minZ and maxZ
		ChunkPos min = new ChunkPos(new BlockPos(bounds.minX, 0, bounds.minZ));
		ChunkPos max = new ChunkPos(new BlockPos(bounds.maxX, 0, bounds.maxZ));
		List<ChunkPos> relevant = new ArrayList<>();
		if(!min.equals(max))
			for(int xx = min.x; xx <= max.x; xx++)
				for(int zz = min.z; zz <= max.z; zz++)
					relevant.add(new ChunkPos(xx, zz));
		else
			relevant.add(min);
		//then getNodesWithinAABB foreach
		return relevant.stream()
				.map(this::getAuraChunk)
				.map(chunk -> chunk.getNodesWithinAABB(bounds))
				.flatMap(Collection::stream)
				.filter(node -> node.type() == type)
				.collect(Collectors.toList());
	}
	
	default Collection<Node> getNodesExcluding(Node excluded){
		return getAllNodes().stream()
				.filter(node -> node != excluded)
				.collect(Collectors.toList());
	}
	
	default Collection<Node> getNodesOfTypeExcluding(NodeType type, Node excluded){
		return getAllNodes().stream()
				.filter(node -> node.type() == type)
				.filter(node -> node != excluded)
				.collect(Collectors.toList());
	}
	
	default Collection<Node> getNodesWithinAABBExcluding(AxisAlignedBB bounds, Node excluded){
		// get all related chunks
		// that is, all chunks between minX and maxX, minZ and maxZ
		ChunkPos min = new ChunkPos(new BlockPos(bounds.minX, 0, bounds.minZ));
		ChunkPos max = new ChunkPos(new BlockPos(bounds.maxX, 0, bounds.maxZ));
		List<ChunkPos> relevant = new ArrayList<>();
		if(!min.equals(max))
			for(int xx = min.x; xx <= max.x; xx++)
				for(int zz = min.z; zz <= max.z; zz++)
					relevant.add(new ChunkPos(xx, zz));
		else
			relevant.add(min);
		//then getNodesWithinAABB foreach
		return relevant.stream()
				.map(this::getAuraChunk)
				.map(chunk -> chunk.getNodesWithinAABB(bounds))
				.flatMap(Collection::stream)
				.filter(node -> node != excluded)
				.collect(Collectors.toList());
	}
	
	default Collection<Node> getNodesOfTypeWithinAABBExcluding(AxisAlignedBB bounds, NodeType type, Node excluded){
		// get all related chunks
		// that is, all chunks between minX and maxX, minZ and maxZ
		ChunkPos min = new ChunkPos(new BlockPos(bounds.minX, 0, bounds.minZ));
		ChunkPos max = new ChunkPos(new BlockPos(bounds.maxX, 0, bounds.maxZ));
		List<ChunkPos> relevant = new ArrayList<>();
		if(!min.equals(max))
			for(int xx = min.x; xx <= max.x; xx++)
				for(int zz = min.z; zz <= max.z; zz++)
					relevant.add(new ChunkPos(xx, zz));
		else
			relevant.add(min);
		//then getNodesWithinAABB foreach
		return relevant.stream()
				.map(this::getAuraChunk)
				.map(chunk -> chunk.getNodesWithinAABB(bounds))
				.flatMap(Collection::stream)
				.filter(node -> node.type() == type)
				.filter(node -> node != excluded)
				.collect(Collectors.toList());
	}
	
	default boolean addNode(Node node){
		// get the relevant chunk
		AuraChunk nc = getAuraChunk(new ChunkPos(new BlockPos(node)));
		if(nc != null){
			nc.addNode(node);
			return true;
		}
		return false;
	}
	
	default boolean removeNode(Node node){
		// get the relevant chunk
		AuraChunk nc = getAuraChunk(new ChunkPos(new BlockPos(node)));
		if(nc != null && nc.getNodes().contains(node)){
			nc.removeNode(node);
			return true;
		}
		return false;
	}
}