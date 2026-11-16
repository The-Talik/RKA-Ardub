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

import com.zrp200.rkpd2.Badges;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.hero.HeroSubClass;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.actors.hero.abilities.Ratmogrify;
import com.zrp200.rkpd2.effects.Speck;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.PixelScene;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TalentsPane extends ScrollPane {

	ArrayList<TalentTierPane> panes = new ArrayList<>();
	ArrayList<ColorBlock> separators = new ArrayList<>();

	ColorBlock sep;
	ColorBlock blocker;
	RenderedTextBlock blockText;

	public TalentsPane( TalentButton.Mode mode ) {
		this( mode, Dungeon.hero.talents );
	}

	public TalentsPane( TalentButton.Mode mode, ArrayList<LinkedHashMap<Talent, Integer>> talents ) {
		super(new Component());

		Ratmogrify.useRatroicEnergy = Dungeon.hero != null && Dungeon.hero.armorAbility instanceof Ratmogrify;

		int tiersAvailable = 1;

		if (mode == TalentButton.Mode.INFO){
			if (!Badges.isUnlocked(Badges.Badge.LEVEL_REACHED_1)){
				tiersAvailable = 1;
			} else if (!Badges.isUnlocked(Badges.Badge.LEVEL_REACHED_2) || !Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_2)){
				tiersAvailable = 2;
			} else if (!Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_4)){
				tiersAvailable = 3;
			} else {
				tiersAvailable = Talent.MAX_TALENT_TIERS;
			}
		} else {
			while (tiersAvailable < Talent.MAX_TALENT_TIERS
					&& Dungeon.hero.lvl+1 >= Talent.tierLevelThresholds[tiersAvailable+1]){
				tiersAvailable++;
			}
			if (tiersAvailable > 2 && Dungeon.hero.subClass == HeroSubClass.NONE && Dungeon.hero.heroClass2 == null){
				tiersAvailable = 2;
			} else if (tiersAvailable > 3 && (Dungeon.hero.subClass2 == HeroSubClass.NONE && Dungeon.hero.armorAbility == null)){
				tiersAvailable = 3;
			}
			if (tiersAvailable == 3 && Dungeon.hero.heroClass2 != null && Dungeon.hero.subClass != HeroSubClass.NONE){
				tiersAvailable = 4;
			}
		}

		tiersAvailable = Math.min(tiersAvailable, talents.size());

		for (int i = 0; i < Math.min(tiersAvailable, talents.size()); i++){
			if (talents.get(i).isEmpty()) continue;

			TalentTierPane pane = new TalentTierPane(talents.get(i), i+1, mode);
			panes.add(pane);
			content.add(pane);

			ColorBlock sep = new ColorBlock(0, 1, 0xFF000000);
			separators.add(sep);
			content.add(sep);
		}

		sep = new ColorBlock(0, 1, 0xFF000000);
		content.add(sep);

		blocker = new ColorBlock(0, 0, 0xFF222222);
		content.add(blocker);

		if (tiersAvailable == 1) {
			blockText = PixelScene.renderTextBlock(Messages.get(this, "unlock_tier2"), 6);
			content.add(blockText);
		} else if (tiersAvailable == 2) {
			blockText = PixelScene.renderTextBlock(Messages.get(this, "unlock_tier3"), 6);
			content.add(blockText);
		} else if (tiersAvailable == 3) {
			blockText = PixelScene.renderTextBlock(Messages.get(this, "unlock_tier4"), 6);
			content.add(blockText);
		} else {
			blockText = null;
		}

		for (int i = panes.size()-1; i >= 0; i--){
			content.bringToFront(panes.get(i));
		}
	}

	@Override
	protected void layout() {
		super.layout();

		float top = 0;
		for (int i = 0; i < panes.size(); i++){
			top += 2;
			panes.get(i).setRect(x, top, width, 0);
			top = panes.get(i).bottom();

			separators.get(i).x = 0;
			separators.get(i).y = top + 2;
			separators.get(i).size(width, 1);

			top += 3;

		}

		float bottom;
		if (blockText != null) {
			bottom = Math.max(height, top + 20);

			blocker.x = 0;
			blocker.y = top;
			blocker.size(width, bottom - top);

			blockText.maxWidth((int) width);
			blockText.align(RenderedTextBlock.CENTER_ALIGN);
			blockText.setPos((width - blockText.width()) / 2f, blocker.y + (bottom - blocker.y - blockText.height()) / 2);
		} else {
			bottom = Math.max(height, top);

			blocker.visible = false;
		}

		content.setSize(width, bottom);
	}

	public static class TalentTierPane extends Component {

		private int tier;

		public RenderedTextBlock title;
		ArrayList<TalentButton> buttons;

		ArrayList<Image> stars = new ArrayList<>();

		public TalentTierPane(LinkedHashMap<Talent, Integer> talents, int tier, TalentButton.Mode mode){
			super();

			this.tier = tier;

			title = PixelScene.renderTextBlock(Messages.titleCase(Messages.get(TalentsPane.class, "tier", tier)), 9);
			title.hardlight(Window.TITLE_COLOR);
			add(title);

			if (mode == TalentButton.Mode.UPGRADE) setupStars();

			buttons = new ArrayList<>();
			for (Talent talent : talents.keySet()){
				TalentButton btn = new TalentButton(tier, talent, talents.get(talent), mode){
					@Override
					public void upgradeTalent() {
						super.upgradeTalent();
						if (parent != null) {
							setupStars();
							TalentTierPane.this.layout();
						}
					}
				};
				buttons.add(btn);
				add(btn);
			}

		}

		private void setupStars(){
			if (!stars.isEmpty()){
				for (Image im : stars){
					im.killAndErase();
				}
				stars.clear();
			}

			int totStars = Talent.getMaxPoints(tier) + Dungeon.hero.bonusTalentPoints(tier);
			int openStars = Dungeon.hero.talentPointsAvailable(tier);
			int usedStars = Dungeon.hero.talentPointsSpent(tier);
			for (int i = 0; i < totStars; i++){
				Image im = new Speck().image(Speck.STAR);
				stars.add(im);
				add(im);
				if (i >= openStars && i < (openStars + usedStars)){
					im.tint(0.75f, 0.75f, 0.75f, 0.9f);
				} else if (i >= (openStars + usedStars)){
					im.tint(0f, 0f, 0f, 0.9f);
				}
			}
		}

		@Override
		protected void layout() {
			super.layout();

			int regStars = Talent.getMaxPoints(tier);
			int starsPerRow = Math.max(1, (int)((width - title.width() - 2) / 6));

			float titleWidth = title.width();
			titleWidth += 2 + Math.min(Math.min(stars.size(), regStars), starsPerRow)*6;
			title.setPos(x + (width - titleWidth)/2f, y);

			float left = title.right() + 2;

			float starTop = title.top();
			if (regStars < stars.size()) starTop -= 2;

			float headerBottom = title.bottom();
			int starsPlaced = 0;
			for (int i = 0; i < stars.size(); i++){
				if (i > 0 && (starsPlaced == starsPerRow || i == regStars)) {
					starTop += 6;
					left = title.right() + 2;
					starsPlaced = 0;
				}
				Image star = stars.get(i);
				star.x = left;
				star.y = starTop;
				PixelScene.align(star);
				headerBottom = Math.max(headerBottom, star.y + star.height);
				left += 6;
				starsPlaced++;
			}
			// Keep icons at their normal size and fit as many columns as the viewport allows.
			int maxTalentsPerRow = Math.max(1, (int)(width / TalentButton.WIDTH));
			int rows = 1+(buttons.size()-1)/maxTalentsPerRow;
			int buttonsPerRow = buttons.size() / rows;
			int extra = buttons.size() % rows;  // amount of rows that need an extra button to fit properly
			if (extra > 0) buttonsPerRow++;
			float gap = (width - buttonsPerRow*TalentButton.WIDTH)/(buttonsPerRow+1);
			float bottom = headerBottom;
			int placed = 0;
			left = x + gap;
			for (TalentButton btn : buttons){
				btn.setPos(left, bottom + 4);
				PixelScene.align(btn);
				left += btn.width() + gap;
				if(++placed == buttonsPerRow && --rows >= 0) {
					if (--extra == 0) {
						buttonsPerRow--;
						// copy-pasted from above
						gap = (width - buttonsPerRow*TalentButton.WIDTH)/(buttonsPerRow+1);
					}
					left = x + gap;
					bottom = btn.bottom();
					placed = 0;
				}
			}

			height = bottom - y;

		}

	}
}
