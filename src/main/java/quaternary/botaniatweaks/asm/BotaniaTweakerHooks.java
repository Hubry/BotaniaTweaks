package quaternary.botaniatweaks.asm;

import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fluids.capability.IFluidHandler;
import quaternary.botaniatweaks.config.BotaniaTweaksConfig;
import quaternary.botaniatweaks.etc.CatchallFlowerComponent;
import vazkii.botania.api.recipe.IFlowerComponent;
import vazkii.botania.common.Botania;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unused") //Everything here called through ASM
public class BotaniaTweakerHooks {
	
	/// decay tweak
	
	public static int getPassiveDecayTime() {
		return BotaniaTweaksConfig.PASSIVE_DECAY_TIMER;
	}
	
	public static boolean shouldFlowerDecay(String name) {
		return BotaniaTweaksConfig.SHOULD_ALSO_BE_PASSIVE_MAP.getOrDefault(name, false);
	}
	
	/// manastorm tweak
	
	public static int getManastormBurstMana() {
		return MathHelper.floor(120 * BotaniaTweaksConfig.MANASTORM_SCALE_FACTOR);
	}
	
	public static int getManastormBurstStartingMana() {
		return MathHelper.floor(340 * BotaniaTweaksConfig.MANASTORM_SCALE_FACTOR);
	}
	
	//Is this loss
	public static float getManastormBurstLossjpgPerTick() {
		return BotaniaTweaksConfig.MANASTORM_SCALE_FACTOR;
	}
	
	/// entro tweak
	
	public static int getEntropinnyumMaxMana() {
		//6500 is the default (check subtileentropinnyum)
		return 6500 * (BotaniaTweaksConfig.SUPER_ENTROPINNYUM ? 8 : 1);
	}
	
	/// spectro tweak
	
	public static int getSpectrolusManaPerWool() {
		//300 is the default (check subtilespectrolus)
		return 300 * (BotaniaTweaksConfig.SUPER_SPECTROLUS ? 10 : 1);
	}
	
	/// apothecary tweak
	
	//Copy from TileAltar
	private static final Pattern SEED_PATTERN = Pattern.compile("(?:(?:(?:[A-Z-_.:]|^)seed)|(?:(?:[a-z-_.:]|^)Seed))(?:[sA-Z-_.:]|$)");
	
	@CapabilityInject(IFluidHandler.class)
	public static final Capability<IFluidHandler> FLUID_CAP = null;
	
	public static IFlowerComponent getFlowerComponent(IFlowerComponent comp, ItemStack stack) {
		//If the tweak is disabled, or if Botania has already chosen a good flower component, just don't change anything
		if(!BotaniaTweaksConfig.EVERYTHING_APOTHECARY || comp != null) return comp;
		
		//If it's a seed, don't allow it in, since yknow it has to complete the craft
		if(SEED_PATTERN.matcher(stack.getUnlocalizedName()).find()) return null;
		
		//Don't allow buckets in since it's annoying when the empty bucket goes in
		if(stack.getItem() instanceof ItemBucket) return null;
		if(stack.hasCapability(FLUID_CAP, null)) return null;
		
		//K cool
		return new CatchallFlowerComponent();
	}
	
	/// orechid tweak
	
	public static boolean orechidGog = Botania.gardenOfGlassLoaded;
	
	public static void onConfigChanged() { //not actually called through asm
		switch(BotaniaTweaksConfig.ORECHID_MODE) {
			case DEFAULT:
				orechidGog = Botania.gardenOfGlassLoaded;
				break;
			case FORCE_GOG:
				orechidGog = true;
				break;
			case FORCE_NO_GOG:
				orechidGog = false;
				break;
		}
	}
	
	/// entro tnt duplication tweak
	
	public static List<EntityTNTPrimed> processTNTList(List<EntityTNTPrimed> inList) {
		Iterator<EntityTNTPrimed> it = inList.iterator();
		while(it.hasNext()) {
			EntityTNTPrimed tnt = it.next();
			if(!BotaniaTweaksConfig.ALLOW_DUPLICATED_TNT && tnt.getTags().contains("CheatyDupe")) {
				if(tnt.getFuse() == 1) doTNTSilliness(tnt);
				
				it.remove();
				continue;
			}
			
			if(BotaniaTweaksConfig.FORCE_VANILLA_TNT && !tnt.getClass().equals(EntityTNTPrimed.class)) {
				it.remove();
			}
		}
		return inList;
	}
	
	static void doTNTSilliness(EntityTNTPrimed tnt) {
		try {
			NBTTagCompound fireworkNBT = JsonToNBT.getTagFromJson("{Fireworks:{Flight:1b,Explosions:[{Type:2b,Trail:1b,Colors:[I;15790320]},{Type:1b,Colors:[I;11743532],Flicker:1b}]}}");
			
			ItemStack fireworkItem = new ItemStack(Items.FIREWORKS, 1, 0);
			fireworkItem.setTagCompound(fireworkNBT);
			EntityFireworkRocket firework = new EntityFireworkRocket(tnt.world, tnt.posX, tnt.posY, tnt.posZ, fireworkItem);
			firework.lifetime = 1; //Kaboom!
			tnt.world.spawnEntity(firework);
			
			List<EntityPlayer> nearbyPlayers = tnt.world.playerEntities.stream().filter(player -> player.getDistanceSq(tnt) < 25 * 25).collect(Collectors.toList());
			
			for(EntityPlayer p : nearbyPlayers) {
				TextComponentTranslation flowerName = new TextComponentTranslation("tile.botania:flower.entropinnyum.name");
				TextComponentTranslation niceTry = new TextComponentTranslation("botania_tweaks.entrodupe.nicetry");
				TextComponentTranslation chatString = new TextComponentTranslation("chat.type.text", flowerName, niceTry);
				p.sendMessage(chatString);
			}
			
			tnt.setDead(); //No EXPLOSION!!
		} catch(NBTException bleh) {
			// :D
		}
	}
}
