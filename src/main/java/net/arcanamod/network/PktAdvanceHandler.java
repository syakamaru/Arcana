package net.arcanamod.network;

import net.arcanamod.Arcana;
import net.arcanamod.research.ResearchBooks;
import net.arcanamod.research.ResearchEntry;
import net.arcanamod.research.Researcher;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktAdvanceHandler implements IMessageHandler<PktAdvanceHandler.PktAdvanceResearch, IMessage>{
	
	public IMessage onMessage(PktAdvanceResearch message, MessageContext ctx){
		ResearchEntry entry = ResearchBooks.getEntry(message.getKey());
		if(entry != null)
			Researcher.getFrom(Arcana.proxy.getPlayerOnClient()).advanceEntry(entry);
		// else print error
		return null;
	}
	
	public static class PktAdvanceResearch extends StringPacket{
		
		public PktAdvanceResearch(){
		}
		
		public PktAdvanceResearch(ResourceLocation entryKey){
			this.entryKey = entryKey.toString();
		}
		
		public ResourceLocation getKey(){
			return new ResourceLocation(entryKey);
		}
	}
}