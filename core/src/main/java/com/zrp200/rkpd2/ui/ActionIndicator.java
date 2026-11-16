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

package com.zrp200.rkpd2.ui;

import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.SPDAction;
import com.zrp200.rkpd2.actors.buffs.Berserk;
import com.zrp200.rkpd2.actors.buffs.BrawlerBuff;
import com.zrp200.rkpd2.actors.buffs.Buff;
import com.zrp200.rkpd2.actors.buffs.Combo;
import com.zrp200.rkpd2.actors.buffs.HighnessBuff;
import com.zrp200.rkpd2.actors.buffs.Momentum;
import com.zrp200.rkpd2.actors.buffs.MonkEnergy;
import com.zrp200.rkpd2.actors.buffs.Preparation;
import com.zrp200.rkpd2.actors.buffs.RKChampionBuff;
import com.zrp200.rkpd2.actors.buffs.RobotBuff;
import com.zrp200.rkpd2.actors.buffs.SnipersMark;
import com.zrp200.rkpd2.actors.buffs.WarriorParry;
import com.zrp200.rkpd2.items.artifacts.CloakOfShadows;
import com.zrp200.rkpd2.items.artifacts.HolyTome;
import com.zrp200.rkpd2.items.artifacts.KromerCloak;
import com.zrp200.rkpd2.items.weapon.melee.MeleeWeapon;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.scenes.PixelScene;
import com.zrp200.rkpd2.windows.WndActionList;
import com.watabou.input.GameAction;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;
import com.zrp200.rkpd2.actors.hero.spells.SpellEmpower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionIndicator extends Component {

	public static Action action;
	public static ActionIndicator instance;
	private final ArrayList<ActionTag> tags = new ArrayList<>();
	private boolean needsRefresh = true;
	private boolean flipped;
	private int rowsPerColumn = 1;
	private float edgeWidth = Tag.SIZE;

	public ActionIndicator() {
		instance = this;
		setSize(Tag.SIZE, Tag.SIZE);
		visible = false;
	}

	@Override
	public void destroy() {
		if (instance == this) instance = null;
		longClickListener.destroy();
		super.destroy();
	}

	// Most screens have room for a vertical stack. Wrap inward on short screens.
	public void layoutStack(float edgeX, float bottom, float tagWidth, boolean onLeft, float availableHeight) {
		flipped = onLeft;
		edgeWidth = tagWidth;
		rowsPerColumn = Math.min(Math.max(1, tags.size()), Math.max(1, (int)(availableHeight / Tag.SIZE)));
		int columns = (Math.max(1, tags.size()) + rowsPerColumn - 1) / rowsPerColumn;
		float stackWidth = tagWidth + (columns - 1) * Tag.SIZE;
		setRect(onLeft ? edgeX : edgeX - (columns - 1) * Tag.SIZE,
				bottom - rowsPerColumn * Tag.SIZE, stackWidth, rowsPerColumn * Tag.SIZE);
	}

	@Override
	protected synchronized void layout() {
		super.layout();
		if (tags == null) return;
		for (int i = 0; i < tags.size(); i++) {
			int column = i / rowsPerColumn;
			float tagWidth = column == 0 ? edgeWidth : Tag.SIZE;
			float tagX = flipped ? x + (column == 0 ? 0 : edgeWidth + (column-1) * Tag.SIZE)
					: right() - edgeWidth - column * Tag.SIZE;
			ActionTag tag = tags.get(i);
			tag.setRect(tagX, bottom() - (i % rowsPerColumn + 1) * Tag.SIZE, tagWidth, Tag.SIZE);
			tag.flip(flipped);
		}
	}

	@Override
	public void update() {
		synchronized (ActionIndicator.class) {
			if (action != null && !action.usable()) clearAction(action);
			List<? extends Action> actions = action == null ? Collections.emptyList() : action.indicatorActions();
			boolean changed = tags.size() != actions.size();
			if (!changed) for (int i = 0; i < tags.size(); i++) {
				if (tags.get(i).rowAction != actions.get(i)) { changed = true; break; }
			}
			if (changed) {
				for (ActionTag tag : tags) { remove(tag); tag.destroy(); }
				tags.clear();
				for (int i = 0; i < actions.size(); i++) {
					ActionTag tag = new ActionTag(actions.get(i), i == 0);
					tags.add(tag);
					add(tag);
					tag.flash();
				}
				GameScene.updateTags = true;
				layout();
			} else if (needsRefresh) {
				for (ActionTag tag : tags) tag.refreshVisuals();
			}
			visible = !tags.isEmpty();
			needsRefresh = false;
		}
		super.update();
	}

	private class ActionTag extends Tag {
		private final Action rowAction;
		private final boolean primary;
		private Visual primaryVis;
		private Visual secondVis;
		private int lastIcon;
		private boolean lastEmpowered;

		ActionTag(Action action, boolean primary) {
			super(action.indicatorColor());
			this.rowAction = action;
			this.primary = primary;
			setColor(action.indicatorColor());
			refreshVisuals();
		}

		@Override public GameAction keyAction() { return primary ? SPDAction.TAG_ACTION : null; }

		void refreshVisuals() {
			if (primaryVis != null) { primaryVis.destroy(); primaryVis.killAndErase(); }
			if (secondVis != null) { secondVis.destroy(); secondVis.killAndErase(); }
			primaryVis = rowAction.primaryVisual();
			add(primaryVis);
			secondVis = rowAction.secondaryVisual();
			if (secondVis != null) add(secondVis);
			lastIcon = rowAction.actionIcon();
			lastEmpowered = SpellEmpower.isActive();
			setColor(rowAction.indicatorColor());
			layout();
		}

		@Override protected void layout() {
			super.layout();
			if (primaryVis == null) return;
			primaryVis.x = !flipped ? x + (SIZE - primaryVis.width()) / 2f + 1
					: x + width - (SIZE + primaryVis.width()) / 2f - 1;
			primaryVis.y = y + (height - primaryVis.height()) / 2f;
			PixelScene.align(primaryVis);
			if (secondVis != null) {
				secondVis.x = secondVis.width() > 16 ? primaryVis.center().x - secondVis.width()/2f
						: primaryVis.center().x + 8 - secondVis.width();
				secondVis.y = primaryVis.center().y + 8 - (secondVis instanceof BitmapText
						? ((BitmapText)secondVis).baseLine() : secondVis.height());
				PixelScene.align(secondVis);
			}
		}

		@Override public void update() {
			super.update();
			if (lastIcon != rowAction.actionIcon() || lastEmpowered != SpellEmpower.isActive()) refreshVisuals();
			float alpha = Dungeon.hero != null && Dungeon.hero.ready ? (rowAction.usable() ? 1f : 0.3f) : 0.5f;
			if (primaryVis != null) primaryVis.alpha(alpha);
			if (secondVis != null) secondVis.alpha(alpha);
		}

		@Override protected void onClick() {
			super.onClick();
			if (Dungeon.hero != null && Dungeon.hero.ready) rowAction.doAction();
		}
		@Override protected String hoverText() { return Messages.titleCase(rowAction.actionName()); }
		@Override protected boolean onLongClick() {
			GameScene.show(new WndActionList());
			return true;
		}
	}

	private final Button longClickListener = new Button() {
		@Override public GameAction keyAction() { return SPDAction.TAG_CYCLE; }
		@Override protected void onClick() { GameScene.show(new WndActionList()); }
	};

	public static boolean setAction(Action action){
		if(action == null || !action.usable()) return false;
		synchronized (ActionIndicator.class) {
			ActionIndicator.action = action;
			refresh();
		}
		return true;
	}

	// list of action buffs that we should replace it with.
	public static final Class<?extends Buff>[] actionBuffClasses = new Class[]{
			Preparation.class,
			SnipersMark.class,
			Combo.class,
			Momentum.class,
			MonkEnergy.class,
			Berserk.class,
			MeleeWeapon.Charger.class,
            HolyTome.TomeRecharge.class,
			RobotBuff.class,
			HighnessBuff.class,
			BrawlerBuff.class,
			RKChampionBuff.class,
			WarriorParry.class,
			CloakOfShadows.cloakRecharge.class,
			KromerCloak.cloakRecharge.class
	};
	private static boolean findAction(boolean cycle) {
		if(Dungeon.hero == null) return false;
		if(action == null) cycle = false;
		int start = -1;
		if(cycle) while(++start < actionBuffClasses.length && !actionBuffClasses[start].isInstance(action));

		for(int i = (start+1)%actionBuffClasses.length; i != start && i < actionBuffClasses.length; i++) {
			for(Buff b : Dungeon.hero.buffs(actionBuffClasses[i])) {
				if( b != action && setAction( (Action) b ) ) return true;
			}
			if(cycle && i+1 == actionBuffClasses.length) i = -1;
		}
		return false;
	}

	public static void clearAction(){
		clearAction(null);
	}

	public static void clearAction(Action action){
		synchronized (ActionIndicator.class) {
			if (action == null || ActionIndicator.action == action) {
				ActionIndicator.action = null;
				findAction(false);
			}
		}
	}

	public static void refresh(){
		synchronized (ActionIndicator.class) {
			if (instance != null) {
				instance.needsRefresh = true;
			}
		}
	}

	public interface Action {

		default List<? extends Action> indicatorActions() { return Collections.singletonList(this); }

		default String actionName() { return Messages.get(this, "action_name"); };

		default int actionIcon(){
			return HeroIcon.NONE;
		}

		//usually just a static icon, unless overridden
		default Visual primaryVisual(){
			return new HeroIcon(this);
		}

		//a smaller visual on the bottom-right, usually a tiny icon or bitmap text
		default Visual secondaryVisual(){
			return null; //no second visual by default
		}

		int indicatorColor();

		void doAction();

		default boolean usable() { return true; }
		default boolean isSelectable() { return action != this && usable(); }
	}

}
