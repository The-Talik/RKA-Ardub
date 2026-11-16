package com.zrp200.rkpd2.actors.buffs;

import com.zrp200.rkpd2.messages.Messages;
import com.watabou.utils.GameMath;

import static com.zrp200.rkpd2.Dungeon.hero;

public abstract class Cooldown extends FlavourBuff {
    public static <T extends Cooldown> T affectHero(Class<T> cls) {
        T buff = Buff.affect(hero, cls);
        buff.postpone(Math.max(0, buff.duration()));
        return buff;
    }

    public abstract float duration();

    @Override
    public void spend(float time) {
        super.spend(time);
        // An instant-cast offset must not schedule an expired cooldown in the past.
        if (cooldown() < 0) timeToNow();
    }

    public float iconFadePercent() {
        return duration() <= 0 ? 0 : GameMath.gate(0, visualcooldown() / duration(), 1);
    }

    public String toString() {
        return Messages.get(this, "name");
    }

    public String desc() {
        return Messages.get(this, "desc", dispTurns(visualcooldown()));
    }
}
