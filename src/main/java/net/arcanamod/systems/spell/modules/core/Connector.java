package net.arcanamod.systems.spell.modules.core;

import net.arcanamod.systems.spell.modules.SpellModule;
import net.minecraft.nbt.CompoundNBT;

public class Connector extends SpellModule {
	@Override
	public String getName() {
		return "connector";
	}

	@Override
	public int getInputAmount() {
		return 1;
	}

	@Override
	public boolean canConnect(SpellModule connectingModule) {
		return true;
	}

	@Override
	public int getOutputAmount() {
		return 1;
	}

	@Override
	public CompoundNBT toNBT() {
		return new CompoundNBT();
	}
}
