package net.arcanamod.world;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.arcanamod.aspects.Aspect;
import net.minecraft.dispenser.IPosition;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.util.Objects;
import java.util.UUID;

// implements position for BlockPos constructor convenience
public class Node implements IPosition{
	
	Reference2IntMap<Aspect> aspects;
	NodeType type;
	double x, y, z;
	UUID nodeUniqueId = MathHelper.getRandomUUID();
	
	public Node(Reference2IntMap<Aspect> aspects, NodeType type, double x, double y, double z){
		this.aspects = aspects;
		this.type = type;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Node(Reference2IntMap<Aspect> aspects, NodeType type, double x, double y, double z, UUID nodeUniqueId){
		this.aspects = aspects;
		this.type = type;
		this.x = x;
		this.y = y;
		this.z = z;
		this.nodeUniqueId = nodeUniqueId;
	}
	
	// might as well pick the fast version
	public Reference2IntMap<Aspect> aspects(){
		return aspects;
	}
	
	public NodeType type(){
		return type;
	}
	
	public CompoundNBT serializeNBT(){
		CompoundNBT nbt = new CompoundNBT();
		nbt.putString("type", NodeType.TYPES.inverse().get(type()).toString());
		CompoundNBT aspectsNBT = new CompoundNBT();
		aspects().forEach((aspect, integer) -> aspectsNBT.putInt(aspect.name(), integer));
		nbt.put("aspects", aspectsNBT);
		nbt.putDouble("x", getX());
		nbt.putDouble("y", getY());
		nbt.putDouble("z", getZ());
		nbt.putUniqueId("nodeUniqueId", nodeUniqueId);
		return nbt;
	}
	
	public static Node fromNBT(CompoundNBT nbt){
		Reference2IntMap<Aspect> aspects = new Reference2IntOpenHashMap<>();
		CompoundNBT aspectsNBT = nbt.getCompound("aspects");
		for(String s : aspectsNBT.keySet())
			aspects.put(Aspect.valueOf(s), aspectsNBT.getInt(s));
		NodeType type = NodeType.TYPES.get(new ResourceLocation(nbt.getString("type")));
		double x = nbt.getDouble("x"), y = nbt.getDouble("y"), z = nbt.getDouble("z");
		return nbt.hasUniqueId("nodeUniqueId") ? new Node(aspects, type, x, y, z, nbt.getUniqueId("nodeUniqueId")) : new Node(aspects, type, x, y, z);
	}
	
	public double getX(){
		return x;
	}
	
	public double getY(){
		return y;
	}
	
	public double getZ(){
		return z;
	}
	
	public void setType(NodeType type){
		this.type = type;
	}
	
	public void setX(double x){
		this.x = x;
	}
	
	public void setY(double y){
		this.y = y;
	}
	
	public void setZ(double z){
		this.z = z;
	}
	
	public UUID nodeUniqueId(){
		return nodeUniqueId;
	}
	
	public String toString(){
		return "Node{" +
				"aspects=" + aspects +
				", type=" + type +
				", x=" + x +
				", y=" + y +
				", z=" + z +
				", nodeUniqueId=" + nodeUniqueId +
				'}';
	}
}