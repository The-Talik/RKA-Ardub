package com.zrp200.rkpd2.items.artifacts;

import com.zrp200.rkpd2.actors.buffs.MagicImmune;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.mobs.npcs.Shopkeeper;
import com.zrp200.rkpd2.items.Item;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.windows.WndBag;
import com.zrp200.rkpd2.windows.WndTradeItem;

/** The armband's portable selling ability, also inherited by the Soul of Yendor. */
public final class ArtifactSales {
    public static final String AC_SELL = "SELL";

    private ArtifactSales() {}

    public static int percentage(Artifact artifact) {
        return Math.max(0, Math.min(10, artifact.level())) * 10;
    }

    public static boolean available(Artifact artifact, Hero hero) {
        return artifact.isEquipped(hero) && !artifact.cursed && hero.buff(MagicImmune.class) == null;
    }

    public static String description(Artifact artifact) {
        return Messages.get(ArtifactSales.class, "desc", percentage(artifact));
    }

    public static void open(Artifact artifact, Hero hero) {
        if (!available(artifact, hero)) return;
        WndBag.ItemSelector selector = new WndBag.ItemSelector() {
            @Override public String textPrompt() {
                return Messages.get(ArtifactSales.class, "prompt", percentage(artifact));
            }

            @Override public boolean itemSelectable(Item item) {
                return Shopkeeper.canSell(item);
            }

            @Override public void onSelect(Item item) {
                if (item == null || !available(artifact, hero)) return;
                WndBag owner = GameScene.selectItem(this);
                GameScene.show(new WndTradeItem(item, owner, percentage(artifact),
                        () -> ArtifactSales.open(artifact, hero)));
            }
        };
        GameScene.selectItem(selector);
    }
}
