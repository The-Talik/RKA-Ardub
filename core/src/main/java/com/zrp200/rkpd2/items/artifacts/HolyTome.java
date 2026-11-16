/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zrp200.rkpd2.items.artifacts;

import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.buffs.MagicImmune;
import com.zrp200.rkpd2.actors.buffs.Regeneration;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.HeroClass;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.actors.hero.spells.ClericSpell;
import com.zrp200.rkpd2.actors.hero.spells.SpellEmpower;
import com.zrp200.rkpd2.items.Item;
import com.zrp200.rkpd2.items.bags.Bag;
import com.zrp200.rkpd2.items.rings.RingOfEnergy;
import com.zrp200.rkpd2.journal.Catalog;
import com.zrp200.rkpd2.mechanics.Ballistica;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.sprites.ItemSprite;
import com.zrp200.rkpd2.sprites.ItemSpriteSheet;
import com.zrp200.rkpd2.ui.ActionIndicator;
import com.zrp200.rkpd2.ui.HeroIcon;
import com.zrp200.rkpd2.ui.QuickSlotButton;
import com.zrp200.rkpd2.utils.GLog;
import com.zrp200.rkpd2.utils.SafeCast;
import com.zrp200.rkpd2.windows.WndClericSpells;
import com.watabou.utils.Bundle;
import com.watabou.noosa.Visual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HolyTome extends Artifact {

	{
		image = ItemSpriteSheet.ARTIFACT_TOME;

		exp = 0;
		levelCap = 10;

		charge = Math.min(level()+3, 10);
		partialCharge = 0;
		chargeCap = Math.min(level()+3, 10);

		{
			Hero hero = Dungeon.hero;
			if (hero == null) hero = SafeCast.cast(Char.restoring, Hero.class);
			if (hero != null && hero.heroClass.isExact(HeroClass.CLERIC, hero)) {
				charge *= 2;
				chargeCap *= 2;
				partialCharge *= 2;
			}
		}

		defaultAction = AC_CAST;

		unique = true;
		bones = false;
	}

	public boolean isDepleted() {
		return !SpellEmpower.isActive() && charge < 0;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return SpellEmpower.isActive() ?
				new ItemSprite.Glowing(0xFFFF00)
				: isDepleted() ? new ItemSprite.Glowing(0)
				: super.glowing();
	}

	@Override
	public String desc() {
		String desc = super.desc();
		String state = SpellEmpower.isActive() ? "empowered"
				: isDepleted() ? "depleted" :
				null;
		if (state != null) desc += "\n\n" + Messages.get(this, state);
		return desc;
	}

	public float getCharges() {
		return charge + partialCharge - getMinimalCharge();
	}

	public static final String AC_CAST = "CAST";

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if ((isEquipped( hero ) || hero.canHaveTalent(Talent.LIGHT_READING))
				&& !cursed
				&& hero.buff(MagicImmune.class) == null) {
			actions.add(AC_CAST);
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute(hero, action);

		if (hero.buff(MagicImmune.class) != null) return;

		if (action.equals(AC_CAST)) {

			if (!isEquipped(hero) && !hero.canHaveTalent(Talent.LIGHT_READING)) GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			else if (cursed)       GLog.i( Messages.get(this, "cursed") );
			else {

				GameScene.show(new WndClericSpells(this, hero, false));

			}

		}
	}

	//used to ensure tome has variable targeting logic for whatever spell is being case
	public ClericSpell targetingSpell = null;

	@Override
	public int targetingPos(Hero user, int dst) {
		if (targetingSpell == null || !targetingSpell.usesTargeting()) {
			return super.targetingPos(user, dst);
		} else {
			return new Ballistica( user.pos, dst, targetingSpell.targetingFlags() ).collisionPos;
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)){
			if (collect && hero.canHaveTalent(Talent.LIGHT_READING)){
				activate(hero);
			}

			return true;
		} else
			return false;
	}

	@Override
	public boolean collect( Bag container ) {
		if (super.collect(container)){
			if (container.owner instanceof Hero
					&& passiveBuff == null
					&& ((Hero) container.owner).canHaveTalent(Talent.LIGHT_READING)){
				activate((Hero) container.owner);
			}
			return true;
		} else{
			return false;
		}
	}

	@Override
	protected void onDetach() {
		if (passiveBuff != null){
			passiveBuff.detach();
			passiveBuff = null;
		}
	}

	public static Talent[] allSpellTalents = {
		Talent.HOLY_INTUITION, Talent.SHIELD_OF_LIGHT, Talent.METAEXPRESSION,
		Talent.RECALL_INSCRIPTION, Talent.SUNRAY, Talent.DIVINE_SENSE, Talent.BLESS, Talent.RADIANT_GRAPPLER,
		Talent.CLEANSE, Talent.HOLY_LANCE, Talent.HALLOWED_GROUND, Talent.MNEMONIC_PRAYER, Talent.DIVINE_ADVENT,
			Talent.LAY_ON_HANDS, Talent.AURA_OF_PROTECTION, Talent.WALL_OF_LIGHT, Talent.LIMIT_BREAK,
        Talent.ARS_RETRACTANDI, Talent.MESSIS_POTENTIAE, Talent.PETITIO_MAGICA,
		Talent.DIVINE_INTERVENTION, Talent.JUDGEMENT, Talent.FLASH,
			Talent.BODY_FORM, Talent.SPIRIT_FORM, Talent.MIND_FORM, Talent.META_FORM,
		Talent.BEAMING_RAY, Talent.LIFE_LINK, Talent.STASIS, Talent.ENRAGE
	};

	public boolean canCast( Hero hero, ClericSpell spell ){
		return spell != null && (isEquipped(hero) || (hero.canHaveTalent(Talent.LIGHT_READING) && hero.belongings.contains(this)))
				&& hero.buff(MagicImmune.class) == null
				&& (spell.ignoreChargeUse(this) || charge - getMinimalCharge() >= spell.chargeUse(hero))
				&& spell.canCast(hero);
	}

	public void spendCharge( float chargesSpent ){
		partialCharge -= chargesSpent;
		while (partialCharge < 0){
			charge--;
			partialCharge++;
		}

		//target hero level is 1 + 2*tome level
		int lvlDiffFromTarget = Dungeon.hero.lvl - (1+level()*2);
		//plus an extra one for each level after 6
		if (level() >= 7){
			lvlDiffFromTarget -= level()-6;
		}

		if (lvlDiffFromTarget >= 0){
			exp += Math.round(chargesSpent * 10f * Math.pow(1.1f, lvlDiffFromTarget));
		} else {
			exp += Math.round(chargesSpent * 10f * Math.pow(0.75f, -lvlDiffFromTarget));
		}

		if (exp >= (level() + 1) * 50 && level() < levelCap) {
			upgrade();
			Catalog.countUse(HolyTome.class);
			exp -= level() * 50;
			GLog.p(Messages.get(this, "levelup"));

		}

		updateQuickslot();
	}

	public void directCharge(float amount){
		if (charge < chargeCap) {
			partialCharge += amount;
			while (partialCharge >= 1f) {
				charge++;
				partialCharge--;
			}
			if (charge >= chargeCap){
				partialCharge = 0;
				charge = chargeCap;
			}
			updateQuickslot();
		}
		updateQuickslot();
	}

	@Override
	public Item upgrade() {
		super.upgrade();
		chargeCap = Math.min(level() + 3, 10);
		Hero hero = Dungeon.hero;
		if (hero == null) hero = SafeCast.cast(Char.restoring, Hero.class);
		if (hero != null && hero.heroClass.isExact(HeroClass.CLERIC, hero)) chargeCap *= 2;
		return this;
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new TomeRecharge();
	}

    public int getMinimalCharge(){
        if (Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.RECARGA_ADDITA)){
            final int[] bonusCharges = {0};
            ArrayList<ClericSpell> currentSpells = new ArrayList<>();
            for (int i = 1; i <= 3; i++)
                currentSpells.addAll(ClericSpell.getSpellList(Dungeon.hero, i));
            ArrayList<ClericSpell> allSpells = ClericSpell.getCenobiteSpells();
            currentSpells.forEach(spell -> {
                if (!allSpells.contains(spell))
                    bonusCharges[0] -= 1;
            });
            return bonusCharges[0];
        } else {
            return 0;
        }
    }

	@Override
	public void charge(Hero target, float amount) {
		if (cursed || target.buff(MagicImmune.class) != null) return;

		if (charge < chargeCap) {
			if (!isEquipped(target)) amount *= 0.75f*target.shiftedPoints(Talent.LIGHT_READING)/3f;
			partialCharge += 0.25f*amount;
			while (partialCharge >= 1f) {
				charge++;
				partialCharge--;
			}
			if (charge >= chargeCap){
				partialCharge = 0;
				charge = chargeCap;
			}
			updateQuickslot();
		}
	}

    public boolean isFullyCharged(){
        return charge == chargeCap;
    }

	public static final int MAX_QUICK_SPELLS = 5;
	// Publish an immutable snapshot: the UI and recharge actor read this concurrently.
	private volatile List<QuickSpellAction> quickSpells = Collections.emptyList();

	public ArrayList<ClericSpell> getQuickSpells() {
		ArrayList<ClericSpell> spells = new ArrayList<>();
		for (QuickSpellAction action : quickSpells) spells.add(action.spell);
		return spells;
	}

	public void setQuickSpell(ClericSpell spell){
		ArrayList<QuickSpellAction> updated = new ArrayList<>(quickSpells);
		if (spell == null) {
			updated.clear();
		} else if (!updated.removeIf(action -> action.spell.getClass() == spell.getClass())) {
			updated.add(0, new QuickSpellAction(spell));
			if (updated.size() > MAX_QUICK_SPELLS) updated.remove(updated.size()-1);
		}
		quickSpells = Collections.unmodifiableList(updated);
		if (passiveBuff != null) {
			if (quickSpells.isEmpty()) ActionIndicator.clearAction((ActionIndicator.Action) passiveBuff);
			else ActionIndicator.setAction((ActionIndicator.Action) passiveBuff);
		}
		ActionIndicator.refresh();
	}

	private static final String QUICK_CLS = "quick_cls";
	private static final String QUICK_CLASSES = "quick_classes";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		List<QuickSpellAction> snapshot = quickSpells;
		Class[] classes = new Class[snapshot.size()];
		for (int i = 0; i < classes.length; i++) classes[i] = snapshot.get(i).spell.getClass();
		bundle.put(QUICK_CLASSES, classes);
		if (classes.length > 0) bundle.put(QUICK_CLS, classes[0]);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		Class[] classes = bundle.contains(QUICK_CLASSES) ? bundle.getClassArray(QUICK_CLASSES)
				: bundle.contains(QUICK_CLS) ? new Class[]{bundle.getClass(QUICK_CLS)} : new Class[0];
		ArrayList<QuickSpellAction> restored = new ArrayList<>();
		if (classes != null) for (Class quickCls : classes) {
			if (restored.size() == MAX_QUICK_SPELLS) break;
			if (restored.stream().anyMatch(action -> action.spell.getClass() == quickCls)) continue;
			for (ClericSpell spell : ClericSpell.getAllSpells()){
				if (spell.getClass() == quickCls){
					restored.add(new QuickSpellAction(spell));
					break;
				}
			}
		}
		quickSpells = Collections.unmodifiableList(restored);
	}

	public class TomeRecharge extends ArtifactBuff implements ActionIndicator.Action {

		@Override
		public boolean attachTo(Char target) {
			if (super.attachTo(target)) {
				if (!quickSpells.isEmpty()) ActionIndicator.setAction(this);
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void detach() {
			super.detach();
			ActionIndicator.clearAction(this);
		}

		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null) {
				if (Regeneration.regenOn()) {
					float missing = (chargeCap - charge);
					if (level() > 7) missing += 5*(level() - 7)/3f;
                    int baseRecharge = 45;
                    if (((Hero)target).pointsInTalent(Talent.RECARGA_ADDITA) > 1)
                        baseRecharge = 35;
                    float turnsToCharge = Math.max(0, baseRecharge - missing);
					turnsToCharge /= RingOfEnergy.artifactChargeMultiplier(target);
					float chargeToGain = (1f / turnsToCharge);
					if (!isEquipped(Dungeon.hero)){
						chargeToGain *= 0.75f*Dungeon.hero.shiftedPoints(Talent.LIGHT_READING)/3f;
					}
					partialCharge += chargeToGain;
				}

				while (partialCharge >= 1) {
					charge++;
					partialCharge -= 1;
					if (charge == chargeCap){
						partialCharge = 0;
					}

				}
			} else {
				partialCharge = 0;
			}

			updateQuickslot();

			spend( TICK );
			if (!quickSpells.isEmpty() && ActionIndicator.action != this) {
				ActionIndicator.setAction(this);
			}

			return true;
		}

		@Override
		public String actionName() {
			return quickSpells.isEmpty() ? Messages.get(HolyTome.this, "ac_cast") : quickSpells.get(0).actionName();
		}

		@Override
		public int actionIcon() {
			return quickSpells.isEmpty() ? HeroIcon.NONE : quickSpells.get(0).actionIcon();
		}

		@Override
		public int indicatorColor() {
			return 0x002157;
		}

		@Override
		public boolean usable() {
			Hero hero = Dungeon.hero;
			// Keep assigned spells visible while out of charge, on cooldown, or silenced.
			return !quickSpells.isEmpty() && (hero == null || isEquipped(hero)
					|| (hero.canHaveTalent(Talent.LIGHT_READING) && hero.belongings.contains(HolyTome.this)));
		}

		@Override
		public void doAction() {
			if (!quickSpells.isEmpty()) quickSpells.get(0).doAction();
		}

		@Override
		public List<? extends ActionIndicator.Action> indicatorActions() {
			return quickSpells;
		}
	}

	private class QuickSpellAction implements ActionIndicator.Action {
		private final ClericSpell spell;

		QuickSpellAction(ClericSpell spell) { this.spell = spell; }

		@Override public String actionName() { return spell.name(); }
		@Override public int actionIcon() { return spell.icon() + HeroIcon.SPELL_ACTION_OFFSET; }
		@Override public int indicatorColor() { return 0x002157; }
		@Override public Visual primaryVisual() {
			HeroIcon icon = new HeroIcon(this);
			spell.tintIcon(icon);
			return icon;
		}
		@Override public boolean usable() {
			Hero hero = Dungeon.hero;
			return hero != null && !cursed && hero.buff(MagicImmune.class) == null && canCast(hero, spell);
		}

		@Override public void doAction() {
			if (cursed){
				GLog.w(Messages.get(HolyTome.this, "cursed"));
				return;
			}

			if (!usable()){
				GLog.w(Messages.get(HolyTome.this, "no_spell"));
				return;
			}

			if (targetingSpell == spell && spell.usesTargeting() && QuickSlotButton.lastTarget != null
					&& QuickSlotButton.targetingSlot != -1 &&
					Dungeon.quickslot.getItem(QuickSlotButton.targetingSlot) == HolyTome.this) {
				targetingSpell = spell;
				int cell = QuickSlotButton.autoAim(QuickSlotButton.lastTarget, HolyTome.this);

				if (cell != -1){
					GameScene.handleCell(cell);
				} else {
					//couldn't auto-aim, just target the position and hope for the best.
					GameScene.handleCell( QuickSlotButton.lastTarget.pos );
				}
			} else {
				// A different pinned spell must not complete the previous spell's target selector.
				GameScene.cancelCellSelector();
				QuickSlotButton.cancel();
				targetingSpell = spell;
				spell.onCast(HolyTome.this, Dungeon.hero);

				if (spell.usesTargeting() && Dungeon.quickslot.contains(HolyTome.this)){
					targetingSpell = spell;
					QuickSlotButton.useTargeting(Dungeon.quickslot.getSlot(HolyTome.this));
				}
			}
		}
	}

}
