package net.kineticdevelopment.arcana.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.kineticdevelopment.arcana.client.research.ClientBooks;
import net.kineticdevelopment.arcana.core.research.ResearchBook;
import net.kineticdevelopment.arcana.core.research.ServerBooks;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PktSyncBooksHandler implements IMessageHandler<PktSyncBooksHandler.PktSyncBooks, IMessage>{
	
	public IMessage onMessage(PktSyncBooks message, MessageContext ctx){
		// from server -> client
		// TODO: this should probably be made into something somewhat better
		ClientBooks.books = message.data;
		return null;
	}
	
	public static class PktSyncBooks implements IMessage{
		
		Map<ResourceLocation, ResearchBook> data = new LinkedHashMap<>();
		
		public PktSyncBooks(){}
		
		public PktSyncBooks(Map<ResourceLocation, ResearchBook> data){
			this.data = data;
		}
		
		public void fromBytes(ByteBuf buf){
			// NBTBase # read (DataInput)
			// used with ByteBufInputStream
			ByteBufInputStream stream = new ByteBufInputStream(buf);
			NBTTagCompound nbt = null;
			try{
				nbt = CompressedStreamTools.read(stream, NBTSizeTracker.INFINITE);
			}catch(IOException e){
				e.printStackTrace();
			}
			
			if(nbt != null){
				//
				NBTTagList books = nbt.getTagList("books", 10);
				for(NBTBase bookElement : books){
					NBTTagCompound book = (NBTTagCompound)bookElement;
					// deserialize book
					ResearchBook book1 = ResearchBook.deserialize(book);
					data.put(book1.getKey(), book1);
				}
			}
		}
		
		public void toBytes(ByteBuf buf){
			// NBTBase # write (DataOutput)
			// used with ByteBufOutputStream
			ByteBufOutputStream stream = new ByteBufOutputStream(buf);
			NBTTagCompound nbt = new NBTTagCompound();
			NBTTagList books = new NBTTagList();
			// add each book
			data.forEach((location, book) -> books.appendTag(book.serialize(location)));
			nbt.setTag("books", books);
			try{
				CompressedStreamTools.write(nbt, stream);
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
}