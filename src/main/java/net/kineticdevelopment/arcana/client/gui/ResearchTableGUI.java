package net.kineticdevelopment.arcana.client.gui;

import net.kineticdevelopment.arcana.common.containers.AspectSlot;
import net.kineticdevelopment.arcana.common.containers.ResearchTableContainer;
import net.kineticdevelopment.arcana.common.network.Connection;
import net.kineticdevelopment.arcana.common.network.inventory.PktRequestAspectSync;
import net.kineticdevelopment.arcana.common.objects.tile.ResearchTableTileEntity;
import net.kineticdevelopment.arcana.core.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class ResearchTableGUI extends GuiAspectContainer{
	
	public static final int WIDTH = 376;
	public static final int HEIGHT = 280;
	
	private static final ResourceLocation bg = new ResourceLocation(Main.MODID, "textures/gui/container/gui_researchbook.png");
	
	ResearchTableTileEntity te;
	int page = 0;
	
	GuiButton leftArrow, rightArrow;
	
	public ResearchTableGUI(ResearchTableTileEntity te, ResearchTableContainer container){
		super(container);
		this.te = te;
		xSize = WIDTH;
		ySize = HEIGHT;
	}
	
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY){
		mc.getTextureManager().bindTexture(bg);
		drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, WIDTH, HEIGHT, 378, 378);
	}
	
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY){
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
	}
	
	public void drawScreen(int mouseX, int mouseY, float partialTicks){
		drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		renderHoveredToolTip(mouseX, mouseY);
	}
	
	public void initGui(){
		super.initGui();
		
		leftArrow = addButton(new ChangeAspectPageButton(0, 11, 183, false));
		rightArrow = addButton(new ChangeAspectPageButton(0, 112, 183, true));
		
		Connection.network.sendToServer(new PktRequestAspectSync());
	}
	
	protected void actionPerformed(@Nonnull GuiButton button) throws IOException{
		super.actionPerformed(button);
		if(button == leftArrow && page > 0)
			page--;
		ResearchTableContainer container = (ResearchTableContainer)aspectContainer;
		if(button == rightArrow && container.scrollableSlots.size() > 30 * (page + 1))
			page++;
		
		refreshSlotVisibility();
	}
	
	protected void refreshSlotVisibility(){
		ResearchTableContainer container = (ResearchTableContainer)aspectContainer;
		List<AspectSlot> slots = container.scrollableSlots;
		for(int i = 0; i < slots.size(); i++){
			AspectSlot slot = slots.get(i);
			slot.visible = i >= 30 * page && i < 30 * (page + 1);
		}
	}
	
	class ChangeAspectPageButton extends GuiButton{
		
		boolean right;
		
		public ChangeAspectPageButton(int buttonId, int x, int y, boolean right){
			super(buttonId, x, y, 15, 11, "");
			this.right = right;
		}
		
		public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float partialTicks){
			if(visible){
				hovered = mouseX >= guiLeft + x && mouseY >= guiTop + y && mouseX < guiLeft + x + width && mouseY < guiTop + y + height;
				int teX = right ? 120 : 135;
				int teY = 307;
				ResearchTableContainer container = (ResearchTableContainer)aspectContainer;
				// first check if there are multiple pages
				if(container.scrollableSlots.size() > 30)
					if(right){
						// if I am not on the last page
						if(container.scrollableSlots.size() > 30 * (page + 1)){
							teY -= 11;
							if(hovered)
								teY -= 11;
						}
					}else{
						// if I am not on the first page
						if(page > 0){
							teY -= 11;
							if(hovered)
								teY -= 11;
						}
					}
				// then just draw
				mc.getTextureManager().bindTexture(bg);
				GlStateManager.disableLighting();
				GlStateManager.color(1f, 1f, 1f);
				drawModalRectWithCustomSizedTexture(guiLeft + x, guiTop + y, teX, teY, width, height, 378, 378);
			}
		}
		
		public boolean mousePressed(@Nonnull Minecraft mc, int mouseX, int mouseY){
			return mouseX >= guiLeft + x && mouseY >= guiTop + y && mouseX < guiLeft + x + width && mouseY < guiTop + y + height;
		}
	}
}