package net.arcanamod.systems.spell;

import net.arcanamod.aspects.Aspect;
import net.arcanamod.aspects.Aspects;
import net.arcanamod.client.gui.UiUtil;
import net.arcanamod.items.WandItem;
import net.arcanamod.items.attachment.Focus;
import net.arcanamod.systems.spell.casts.Casts;
import net.arcanamod.systems.spell.casts.ICast;
import net.arcanamod.systems.spell.modules.SpellModule;
import net.arcanamod.systems.spell.modules.circle.DoubleModifierCircle;
import net.arcanamod.systems.spell.modules.core.*;
import net.arcanamod.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static net.arcanamod.util.Pair.of;

/**
 * Spell is made of SpellModules that are bound together.
 */
public class Spell implements ISpell {
	public SpellModule mainModule;
	public UUID spellUUID;

	public Spell(SpellModule beginModule){
		mainModule = beginModule;
		spellUUID = UUID.randomUUID();
	}

	public SpellModule getBeginModule() {
		return mainModule;
	}

	private static Logger logger = LogManager.getLogger();

	/**
	 * Create new Spell serializer that can be used to deserialize or serialize spell.
	 * @return Spell serializer.
	 */
	public static Serializer getSerializer() {
		return new Spell.Serializer();
	}

	/**
	 * Run Spell.
	 * Goes trough all spell modules and executes {@link ICast}.
	 * @param spell Spell to run.
	 * @param caster Player that uses the Spell.
	 * @param sender {@link net.minecraft.item.ItemStack} that {@link net.minecraft.item.Item} extends {@link WandItem}
	 * @param action Spell use Action.
	 */
	public static void runSpell(Spell spell, PlayerEntity caster, Object sender, ICast.Action action){
		for (SpellModule module : spell.mainModule.getBoundModules()) {
			Logic.runSpellModule(spell, module, caster, sender, action, new ArrayList<>(),new ArrayList<>());
		}
	}

	public static void updateSpellStatusBar(PlayerEntity player){
		if (player.getHeldItem(Hand.MAIN_HAND).getItem() instanceof WandItem) {
			if (WandItem.getFocus(player.getHeldItem(Hand.MAIN_HAND)) != Focus.NO_FOCUS) {
				if (WandItem.getFocus(player.getHeldItem(Hand.MAIN_HAND)).getSpell(player.getHeldItem(Hand.MAIN_HAND))!=null) {
					for (SpellModule module : WandItem.getFocus(player.getHeldItem(Hand.MAIN_HAND)).getSpell(player.getHeldItem(Hand.MAIN_HAND)).mainModule.getBoundModules()) {
						Logic.updateSpellStatusBarRecursive(module, player, new ArrayList<>());
					}
				}
			}
		}
	}

	/**
	 * Cost of spell in AspectStacks.
	 *
	 * @return returns cost of spell.
	 */
	@Override
	public SpellCosts getSpellCosts() {
		return Logic.getSpellCost(mainModule,new SpellCosts(0,0,0,0,0,0,0));
	}

	public Optional<ITextComponent> getName(CompoundNBT nbt){
		// TODO: add tooltip
		return Optional.of(new StringTextComponent("//FIXME: NAME NOT IMPLEMENTED!!!")/*new TranslationTextComponent("spell." + getId().getNamespace() + "." + getId().getPath())*/);
	}

	public int getSpellColor() {
		return Logic.blendAndGetColor(mainModule, 0x000000);
	}

	public static class Serializer{
		/**
		 * Spell to Spell NBT
		 * @param spell Spell to NBT serialize
		 * @param compound Existing CompoundNBT or new.
		 * @return Serialized Spell
		 */
		public CompoundNBT serializeNBT(Spell spell, CompoundNBT compound){
			compound.put("spell",serialize(spell.mainModule, new CompoundNBT(), new ListNBT(), 0));
			return compound;
		}

		/**
		 * Spell NBT to Spell Object
		 * @param compound Spell NBT
		 * @return Deserialized Spell
		 */
		public Spell deserializeNBT(CompoundNBT compound){
			Spell spell = new Spell(new StartCircle());
			if (compound.get("spell") != null)
				spell.mainModule.bindModule(deserialize(spell.mainModule, (CompoundNBT) compound.get("spell"), 0));
			return spell;
		}

		private CompoundNBT serialize(SpellModule toSerialize, CompoundNBT prevModule, ListNBT prevBound, int deepness) {
			ListNBT boundList = new ListNBT();
			CompoundNBT moduleNBT = new CompoundNBT();
			for (SpellModule module : toSerialize.getBoundModules()) {
				if (module!=null) {
					moduleNBT = new CompoundNBT();
					boundList = new ListNBT();
					moduleNBT.putString("name", module.getName());
					moduleNBT.put("data", module.toNBT());
					prevBound.add(moduleNBT);
					serialize(module, moduleNBT, boundList, ++deepness);
				}
			}
			prevModule.put("bound",prevBound);
			return moduleNBT;
		}

		private SpellModule deserialize(SpellModule toDeserialize, CompoundNBT spellNBT, int deepness) {
			SpellModule createdModule = null;
			if (!spellNBT.getString("name").equals(""))
				createdModule = SpellModule.fromNBT(spellNBT);

			if (spellNBT.get("bound") != null && createdModule != null) {
				for (INBT inbt : ((ListNBT) Objects.requireNonNull(spellNBT.get("bound")))) {
					if (inbt instanceof CompoundNBT) {
						CompoundNBT compound = ((CompoundNBT) inbt);
						createdModule.bindModule(deserialize(toDeserialize, compound, ++deepness));
					}
				}
			}
			return createdModule;
		}
	}

	private static class Logic {
		private static SpellModule updateSpellStatusBarRecursive(SpellModule toUnbound, PlayerEntity player,
																 List<Pair<Aspect,Aspect>> castMethodsAspects) {
/*			if (toUnbound.getBoundModules().size() > 0){
				for (SpellModule module : toUnbound.getBoundModules()) {
					if (module instanceof CastMethod)
						castMethodsAspects.add(of(((CastMethod) module).aspect, Aspects.EMPTY));
					else if (module instanceof CastMethodSin) {
						castMethodsAspects.get(castMethodsAspects.size() - 1).setSecond(((CastMethodSin) module).aspect);
						return updateSpellStatusBarRecursive(module, player, castMethodsAspects);
					}
				}
			}else{
				for (Pair<Aspect,Aspect> castMethodsAspect : castMethodsAspects)
					if (castMethodsAspect.getFirst() == Aspects.EARTH && castMethodsAspect.getSecond() == Aspects.LUST)
						if (player.isCrouching()) {
							player.sendStatusMessage(new TranslationTextComponent("status.arcana.selection_mode"), true);
						} else {
							player.sendStatusMessage(new TranslationTextComponent("status.arcana.break_mode"), true);
						}
			}*/
			return null;
		}

		/**
		 * Run spell Recursion.
		 */
		private static SpellModule runSpellModule(Spell spell, SpellModule toUnbound, PlayerEntity caster, Object sender, ICast.Action action,
												  List<Pair<Aspect,Aspect>> castMethodsAspects, List<ICast> casts) {
			SpellModule mod = null;
			if (toUnbound.getBoundModules().size() > 0){
				for (SpellModule module : toUnbound.getBoundModules()) {
					if (module instanceof CastMethod)
						castMethodsAspects.add(of(((CastMethod) module).aspect,Aspects.EMPTY));
					else if (module instanceof CastMethodSin) {
						if (castMethodsAspects.size() > 0)
							castMethodsAspects.get(castMethodsAspects.size()-1).setSecond(((CastMethodSin) module).aspect);
					} else if (module instanceof CastCircle)
						casts.add(((CastCircle) module).cast);
					mod = runSpellModule(spell, module, caster, sender, action, castMethodsAspects, casts);
				}
			}else{
				for (ICast cast : casts){
					for (Pair<Aspect,Aspect> castMethodsAspect : castMethodsAspects)
						cast.use(spell.spellUUID, caster, sender, castMethodsAspect, action);
				}
			}
			return mod;
		}

		private static int blendAndGetColor(SpellModule toUnbound, int color){
			if (toUnbound.getBoundModules().size() > 0){
				for (SpellModule module : toUnbound.getBoundModules()) {
					if (module instanceof CastCircle)
						if (color != 0x000000)
							color = UiUtil.blend(((CastCircle)module).cast.getSpellAspect().getColorRange().get(3),color,0.5f);
						else color = ((CastCircle)module).cast.getSpellAspect().getColorRange().get(3);
					return blendAndGetColor(module, color);
				}
			}
			return color;
		}

		public static SpellCosts getSpellCost(SpellModule toUnbound, SpellCosts cost) {
			if (toUnbound.getBoundModules().size() > 0){
				for (SpellModule module : toUnbound.getBoundModules()) {
					if (module instanceof CastMethod) {
						Aspect aspect = ((CastMethod)module).aspect;
						if (aspect==Aspects.EARTH)
							cost.earth.setAmount(cost.earth.getAmount()+1);
						if (aspect==Aspects.AIR)
							cost.air.setAmount(cost.air.getAmount()+1);
						if (aspect==Aspects.WATER)
							cost.water.setAmount(cost.water.getAmount()+1);
						if (aspect==Aspects.FIRE)
							cost.fire.setAmount(cost.fire.getAmount()+1);
						if (aspect==Aspects.ORDER)
							cost.order.setAmount(cost.order.getAmount()+1);
						if (aspect==Aspects.CHAOS)
							cost.chaos.setAmount(cost.chaos.getAmount()+1);
					}
					return getSpellCost(module, cost);
				}
			}
			return cost;
		}
	}

	/**
	 * Example spells.
	 */
	public static class Samples{
		/**
		 * Create example basic Spell that is used for testing.
		 * @return a basic Spell
		 */
		public static Spell createBasicSpell(){
			Connector startToCastMethod_connector = new Connector();
			Connector castMethodToCastCircle_connector = new Connector();
			DoubleModifierCircle doubleModifierCircle = new DoubleModifierCircle();
			CastCircle castCircle = new CastCircle();
			doubleModifierCircle.firstAspect = Aspects.AIR;
			doubleModifierCircle.secondAspect = Aspects.FIRE;
			castCircle.cast = Casts.ICE_CAST;
			castCircle.bindModule(doubleModifierCircle);
			castMethodToCastCircle_connector.bindModule(castCircle);

			CastMethodSin sinMethod = new CastMethodSin();
			sinMethod.aspect = Aspects.GREED;
			sinMethod.bindModule(castMethodToCastCircle_connector);

			CastMethod castMethod = new CastMethod();
			castMethod.aspect = Aspects.FIRE;
			castMethod.bindModule(sinMethod);
			startToCastMethod_connector.bindModule(castMethod);
			Spell spell = new Spell(new StartCircle());
			spell.mainModule.bindModule(startToCastMethod_connector);
			return spell;
		}

		/**
		 * Create example advanced Spell that is used for testing.
		 * @return a basic Spell
		 */
		public static Spell createAdvancedSpell(){
			// Create connectors
			Connector startToCastMethod_connector = new Connector();
			Connector castMethodToCastCircle0_connector = new Connector();
			Connector castMethodToCastCircle1_connector = new Connector();

			// Create Double Modifier Circle
			DoubleModifierCircle doubleModifierCircle = new DoubleModifierCircle();

			// Create Cast Circles
			CastCircle castCircle0 = new CastCircle();
			CastCircle castCircle1 = new CastCircle();

			// Bind "castMethodToCastCircle1_connector" to "castCircle1" connector
			castMethodToCastCircle1_connector.bindModule(castCircle1);

			// Add modifiers to doubleModifierCircle
			doubleModifierCircle.firstAspect = Aspects.AIR;
			doubleModifierCircle.secondAspect = Aspects.FIRE;

			// Set MINING_CAST to castCircle0
			castCircle0.cast = Casts.EXCHANGE_CAST;

			// Bind "castCircle0" to "doubleModifierCircle" connector
			castCircle0.bindModule(doubleModifierCircle);

			// Set FABRIC_CAST to castCircle0
			castCircle1.cast = Casts.MINING_CAST;

			// Bind "castMethodToCastCircle0_connector" to "castCircle0" connector
			castMethodToCastCircle0_connector.bindModule(castCircle0);

			// Create Cast Method
			CastMethod castMethod = new CastMethod();

			// Set aspect to cast method
			castMethod.aspect = Aspects.EARTH;

			// Bind "castMethod" to connectors
			castMethod.bindModule(castMethodToCastCircle0_connector);
			castMethod.bindModule(castMethodToCastCircle1_connector);

			// Bind "startToCastMethod_connector" to "castMethod"
			startToCastMethod_connector.bindModule(castMethod);

			// Create spell
			Spell spell = new Spell(new StartCircle());

			// Create mainModule and bind modules to mainModule
			spell.mainModule.bindModule(startToCastMethod_connector);

			// Return spell
			return spell;
		}

		/**
		 * Create example debug Spell that is used for testing.
		 * @return a basic Spell
		 */
		public static Spell createDebugSpell(){
			CastCircle castCircle0 = new CastCircle();
			CastCircle castCircle1 = new CastCircle();

			castCircle0.cast = Casts.MINING_CAST;
			castCircle1.cast = Casts.FABRIC_CAST;
			CastMethod castMethod = new CastMethod();
			castMethod.aspect = Aspects.EARTH;
			castMethod.bindModule(castCircle0);
			castMethod.bindModule(castCircle1);

			Spell spell = new Spell(new StartCircle());

			spell.mainModule.bindModule(castMethod);
			return spell;
		}
	}
}