package net.arcanamod.world;

import net.arcanamod.ArcanaConfig;
import net.arcanamod.blocks.DelegatingBlock;
import net.arcanamod.blocks.Taint;
import net.arcanamod.blocks.TaintedBlock;
import net.arcanamod.capabilities.AuraChunk;
import net.arcanamod.network.Connection;
import net.arcanamod.network.PkSyncChunkAura;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.dispenser.IPosition;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Streams.stream;

/**
 * A view of the nodes in the world for a particular tick.
 */
public class ServerAuraView implements AuraView{
	
	ServerWorld world;
	
	public ServerAuraView(ServerWorld world){
		this.world = world;
	}
	
	public Collection<Node> getAllNodes(){
		Collection<Node> allNodes = new ArrayList<>();
		for(ChunkHolder holder : world.getChunkProvider().chunkManager.getLoadedChunksIterable()){
			Chunk chunk = holder.getChunkIfComplete();
			if(chunk != null){
				AuraChunk nc = AuraChunk.getFrom(chunk);
				if(nc != null)
					allNodes.addAll(nc.getNodes());
			}
		}
		return allNodes;
	}
	
	public void sendChunkToClients(IPosition pos){
		sendChunkToClients(new BlockPos(pos));
	}
	
	public void sendChunkToClients(BlockPos pos){
		sendChunkToClients(new ChunkPos(pos));
	}
	
	public void sendChunkToClients(ChunkPos pos){
		Connection.INSTANCE.send(PacketDistributor.ALL.noArg(), new PkSyncChunkAura(pos, getNodesWithinChunk(pos), getTaintWithinChunk(pos)));
	}
	
	public void sendAllChunksToClients(Collection<? extends IPosition> pos){
		// don't send sync packets for the same chunk
		pos.stream()
				.map(BlockPos::new)
				.map(ChunkPos::new)
				.distinct()
				.forEach(this::sendChunkToClients);
	}
	
	@SuppressWarnings({"ConstantConditions", "UnstableApiUsage"})
	public void tickTaintLevel(){
		Map<ChunkPos, AuraChunk> loaded = stream(world.getChunkProvider().chunkManager.getLoadedChunksIterable())
				.map(ChunkHolder::getChunkIfComplete)
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(
						Chunk::getPos,
						AuraChunk::getFrom
				));
		// go through all loaded chunks
		loaded.forEach((pos, chunk) -> {
			// if they have more than 60 flux, place a tainted block and consumer 40
			if(chunk.getTaintLevel() >= ArcanaConfig.TAINT_SPAWN_THRESHOLD.get()){
				// pick a completely random block
				BlockPos blockPos = pos.asBlockPos().up(world.rand.nextInt(256)).north(world.rand.nextInt(16)).east(world.rand.nextInt(16));
				BlockState state = world.getBlockState(blockPos);
				Block block = Taint.getTaintedOfBlock(state.getBlock());
				if(block != null){
					world.setBlockState(blockPos, DelegatingBlock.switchBlock(state, block).with(TaintedBlock.UNTAINTED, false));
					chunk.addTaint(-ArcanaConfig.TAINT_SPAWN_COST.get());
				}
			}
			// and for each of their loaded neighbors,
			for(ChunkPos neighbor : neighbors(pos))
				if(loaded.containsKey(neighbor)){
					// if they have more than 30 more flux
					AuraChunk chunk1 = loaded.get(neighbor);
					if(chunk1.getTaintLevel() - chunk.getTaintLevel() > 30){
						// take a third of the difference
						int diff = (chunk1.getTaintLevel() - chunk.getTaintLevel()) / 8;
						chunk.addTaint(diff);
						chunk1.addTaint(-diff);
					}
				}
		});
		// send an update packet
	}
	
	private List<ChunkPos> neighbors(ChunkPos pos){
		return Arrays.asList(
				new ChunkPos(pos.x - 1, pos.z),
				new ChunkPos(pos.x + 1, pos.z),
				new ChunkPos(pos.x, pos.z - 1),
				new ChunkPos(pos.x, pos.z + 1),
				new ChunkPos(pos.x - 1, pos.z - 1),
				new ChunkPos(pos.x + 1, pos.z - 1),
				new ChunkPos(pos.x - 1, pos.z + 1),
				new ChunkPos(pos.x + 1, pos.z + 1)
		);
	}
	
	public World getWorld(){
		return world;
	}
}