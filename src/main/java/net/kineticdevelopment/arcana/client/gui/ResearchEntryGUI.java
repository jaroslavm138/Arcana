package net.kineticdevelopment.arcana.client.gui;

import net.kineticdevelopment.arcana.client.research.EntrySectionRenderer;
import net.kineticdevelopment.arcana.client.research.RequirementRenderer;
import net.kineticdevelopment.arcana.core.research.EntrySection;
import net.kineticdevelopment.arcana.core.research.Requirement;
import net.kineticdevelopment.arcana.core.research.ResearchEntry;
import net.kineticdevelopment.arcana.core.research.Researcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class ResearchEntryGUI extends GuiScreen{
	
	ResourceLocation bg;
	ResearchEntry entry;
	int index;
	
	GuiButton left, right;
	
	// there is: golem, crucible, crafting, infusion circle, arcane crafting, structure, wand(, arrow), crafting result
	public static final String OVERLAY_SUFFIX = "_gui_overlay.png";
	public static final String SUFFIX = "_gui.png";
	
	// 256 x 181 @ 0,0
	public static final int PAGE_X = 17;
	public static final int PAGE_Y = 10;
	public static final int PAGE_WIDTH = 105;
	public static final int PAGE_HEIGHT = 155;
	public static final int RIGHT_X_OFFSET = 119;
	
	public ResearchEntryGUI(ResearchEntry entry){
		this.entry = entry;
		bg = new ResourceLocation(entry.key().getResourceDomain(), "textures/gui/research/" + entry.category().getBook().getPrefix() + SUFFIX);
	}
	
	public void drawScreen(int mouseX, int mouseY, float partialTicks){
		drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		mc.getTextureManager().bindTexture(bg);
		drawTexturedModalRect((width - 256) / 2, (height - 181) / 2, 0, 0, 256, 181);
		
		// Main rendering
		if(totalLength() > index){
			EntrySection section = getSectionAtIndex(index);
			if(section != null)
				EntrySectionRenderer.get(section).render(section, sectionIndex(index), width, height, mouseX, mouseY, false, mc.player);
		}
		if(totalLength() > index + 1){
			EntrySection section = getSectionAtIndex(index + 1);
			if(section != null)
				EntrySectionRenderer.get(section).render(section, sectionIndex(index + 1), width, height, mouseX, mouseY, true, mc.player);
		}
		
		// Requirements
		Researcher r = Researcher.getFrom(mc.player);
		if(r.stage(entry) < entry.sections().size() && entry.sections().get(r.stage(entry)).getRequirements().size() > 0){
			List<Requirement> requirements = entry.sections().get(r.stage(entry)).getRequirements();
			int y = (height - 181) / 2 + 190;
			if(requirements.size() > 10)
				y += 10;
			final int baseX = (width / 2) - (10 * requirements.size());
			for(int i = 0, size = requirements.size(); i < size; i++){
				Requirement requirement = requirements.get(i);
				renderer(requirement).render(baseX + i * 20, y, requirement, mc.player.ticksExisted, partialTicks, mc.player);
				renderAmount(baseX + i * 20, y, requirement.getAmount(), requirement.satisfied(mc.player));
			}
			// Show tooltips
			for(int i = 0, size = requirements.size(); i < size; i++)
				if(mouseX >= 20 * i + baseX && mouseX <= 20 * i + baseX + 16 && mouseY >= y && mouseY <= y + 16){
					GuiUtils.drawHoveringText(renderer(requirements.get(i)).tooltip(requirements.get(i), mc.player), mouseX, mouseY, width, height, -1, mc.fontRenderer);
					break;
				}
		}
		
		// After-renders (such as tooltips)
		if(totalLength() > index){
			EntrySection section = getSectionAtIndex(index);
			if(section != null)
				EntrySectionRenderer.get(section).renderAfter(section, sectionIndex(index), width, height, mouseX, mouseY, false, mc.player);
		}
		if(totalLength() > index + 1){
			EntrySection section = getSectionAtIndex(index + 1);
			if(section != null)
				EntrySectionRenderer.get(section).renderAfter(section, sectionIndex(index + 1), width, height, mouseX, mouseY, true, mc.player);
		}
	}
	
	public void initGui(){
		final int y = (height - 181) / 2 + 190;
		final int x = width / 2 - 6;
		final int dist = 127;
		left = addButton(new ChangePageButton(0, x - dist, y, false));
		right = addButton(new ChangePageButton(1, x + dist, y, true));
		
		updateButtonVisibility();
	}
	
	protected void actionPerformed(GuiButton button){
		if(button == left && canTurnLeft())
			index -= 2;
		else if(button == right && canTurnRight())
			index += 2;
		updateButtonVisibility();
	}
	
	protected void updateButtonVisibility(){
		left.visible = canTurnLeft();
		right.visible = canTurnRight();
	}
	
	private boolean canTurnRight(){
		return index < totalLength() - 2;
	}
	
	private boolean canTurnLeft(){
		return index > 0;
	}
	
	private int totalLength(){
		return entry.sections().stream().mapToInt(this::span).sum();
	}
	
	private EntrySection getSectionAtIndex(int index){
		if(index == 0)
			return entry.sections().get(0);
		int cur = 0;
		for(EntrySection section : entry.sections()){
			if(cur <= index && cur + span(section) > index)
				return section;
			cur += span(section);
		}
		return null;
	}
	
	private int sectionIndex(int index){
		int cur = 0;
		for(EntrySection section : entry.sections()){
			if(cur <= index && cur + span(section) > index)
				return index - cur;
			cur += span(section);
		}
		return 0; // throw/show an error
	}
	
	private <T extends Requirement> RequirementRenderer<T> renderer(T requirement){
		return RequirementRenderer.get(requirement);
	}
	
	private void renderAmount(int x, int y, int amount, boolean complete){
		if(amount == 1){
			//display tick or cross
		}else{
			String s = String.valueOf(amount);
			GlStateManager.disableLighting();
			GlStateManager.disableDepth();
			GlStateManager.disableBlend();
			mc.fontRenderer.drawStringWithShadow(s, (float)(x + 19 - 2 - mc.fontRenderer.getStringWidth(s)), (float)(y + 6 + 3), complete ? 0xaaffaa : 0xffaaaa);
			GlStateManager.enableBlend();
			GlStateManager.enableLighting();
			GlStateManager.enableDepth();
		}
	}
	
	private int span(EntrySection section){
		return EntrySectionRenderer.get(section).span(section, mc.player);
	}
	
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	class ChangePageButton extends GuiButton{
		
		boolean right;
		
		public ChangePageButton(int buttonId, int x, int y, boolean right){
			super(buttonId, x, y, 12, 6, "");
			this.right = right;
		}
		
		@ParametersAreNonnullByDefault
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks){
			if(visible){
				hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
				float mult = hovered ? 1f : 0.5f;
				GlStateManager.color(mult, mult, mult, 1f);
				int texX = right ? 12 : 0;
				int texY = 185;
				mc.getTextureManager().bindTexture(bg);
				drawTexturedModalRect(x, y, texX, texY, width, height);
				GlStateManager.color(1f, 1f, 1f, 1f);
			}
		}
	}
}