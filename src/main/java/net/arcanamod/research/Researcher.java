package net.arcanamod.research;

import net.arcanamod.event.ResearchEvent;
import net.arcanamod.research.impls.ResearcherCapability;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface Researcher{
	
	/**
	 * Returns the last index of entry section unlocked for that research.
	 * Returns 0 for entries that have not been unlocked yet, or have no progress.
	 *
	 * @param entry
	 * 		The research entry to check the status of.
	 * @return The last index of entry section unlocked, or 0 if it hasn't been unlocked or progressed.
	 */
	int entryStage(ResearchEntry entry);
	
	boolean isPuzzleCompleted(Puzzle puzzle);
	
	/**
	 * Increments the stage for an entry.
	 *
	 * <p>If the new section has no requirements, this continues to increment the stage
	 * until it reaches either a section with requirements, or the end of the entry.
	 *
	 * <p>Fires {@link ResearchEvent} if the page is not already complete.
	 *
	 * <p>Has no effect if the page is already complete.
	 *
	 * <p>TODO: addenda.
	 *
	 * @param entry
	 * 		The research page to advance.
	 */
	void advanceEntry(ResearchEntry entry);
	
	void completePuzzle(Puzzle puzzle);
	
	void resetPuzzle(Puzzle puzzle);
	
	/**
	 * Sets this researchers progress for an entry to its maximum progress
	 *
	 * <p>Fires {@link ResearchEvent} if the page is not already complete.
	 *
	 * <p>Has no effect if the page is already complete.
	 *
	 * @param entry
	 * 		The research entry to complete.
	 */
	void completeEntry(ResearchEntry entry);
	
	/**
	 * Removes all progress on the given entry.
	 *
	 * <p>Fires {@link ResearchEvent} if the page is not already incomplete.
	 *
	 * @param entry
	 * 		The research entry to reset.
	 */
	void resetEntry(ResearchEntry entry);
	
	void setPlayer(PlayerEntity player);
	
	PlayerEntity getPlayer();
	
	/**
	 * Returns a map containing this researcher's data, where the keys are the keys of all sections
	 * that have a stage greater than 0, and the values are the current stage of that entry. Entries
	 * with 0 progress may be included in this map.
	 *
	 * @return A Map containing the research entry data of this researcher.
	 */
	Map<ResourceLocation, Integer> getEntryData();
	
	void setEntryData(Map<ResourceLocation, Integer> data);
	
	Set<ResourceLocation> getPuzzleData();
	
	void setPuzzleData(Set<ResourceLocation> data);
	
	default CompoundNBT serializeNBT(){
		CompoundNBT compound = new CompoundNBT();
		
		CompoundNBT entries = new CompoundNBT();
		getEntryData().forEach((key, value) -> entries.putInt(key.toString(), value));
		compound.put("entries", entries);
		
		ListNBT puzzles = new ListNBT();
		getPuzzleData().forEach(puzzle -> puzzles.add(StringNBT.valueOf(puzzle.toString())));
		compound.put("puzzles", puzzles);
		return compound;
	}
	
	default void deserializeNBT(@Nonnull CompoundNBT data){
		Map<ResourceLocation, Integer> entryDat = new HashMap<>();
		CompoundNBT entries = data.getCompound("entries");
		for(String s : entries.keySet())
			entryDat.put(new ResourceLocation(s), entries.getInt(s));
		setEntryData(entryDat);
		
		Set<ResourceLocation> puzzleDat = new HashSet<>();
		ListNBT puzzles = data.getList("puzzles", Constants.NBT.TAG_STRING);
		for(INBT key : puzzles)
			puzzleDat.add(new ResourceLocation(key.getString()));
		
		setPuzzleData(puzzleDat);
	}
	
	static boolean canAdvanceEntry(Researcher r, ResearchEntry entry){
		if(isEntryVisible(entry, r))
			if(entry.sections().size() > r.entryStage(entry))
				return entry.sections().get(r.entryStage(entry)).getRequirements().stream().allMatch(x -> x.satisfied(r.getPlayer()));
		// at maximum
		return false;
	}
	
	static void takeRequirementsAndAdvanceEntry(Researcher r, ResearchEntry entry){
		if(canAdvanceEntry(r, entry)){
			entry.sections().get(r.entryStage(entry)).getRequirements().forEach(requirement -> requirement.take(r.getPlayer()));
			r.advanceEntry(entry);
		}
	}
	
	/**
	 * Returns a player's researcher capability, or null if there is no attached researcher capability.
	 *
	 * @param p
	 * 		The player to get a capability from.
	 * @return The player's researcher capability.
	 */
	static Researcher getFrom(@Nonnull PlayerEntity p){
		return p.getCapability(ResearcherCapability.RESEARCHER_CAPABILITY, null).orElse(null);
	}
	
	static boolean isEntryVisible(ResearchEntry entry, @Nonnull Researcher r){
		// abridged version of ResearchBookGUI#style
		
		if(r.entryStage(entry) >= entry.sections().size())
			return true;
		if(r.entryStage(entry) > 0)
			return true;
		if(entry.meta().contains("root") && entry.parents().size() == 0)
			return true;
		if(!entry.meta().contains("hidden"))
			return entry.parents().stream().allMatch(x -> isEntryVisible(ResearchBooks.getEntry(x), r));
		return false;
	}
}