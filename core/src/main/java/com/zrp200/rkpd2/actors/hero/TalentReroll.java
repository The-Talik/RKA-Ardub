package com.zrp200.rkpd2.actors.hero;

import java.util.ArrayList;
import java.util.Collection;
import com.watabou.utils.Random;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Slot-based changes shared by Ars and Metamorphosis. Tiers here are zero-based. */
public final class TalentReroll {
    private TalentReroll() {}

    public static boolean replace(Hero hero, int tierIndex, Talent oldTalent, Talent newTalent) {
        if (tierIndex < 0 || tierIndex >= hero.talents.size()) return false;
        LinkedHashMap<Talent, Integer> tier = hero.talents.get(tierIndex);
        if (!tier.containsKey(oldTalent) || ownedTalents(hero).contains(newTalent)) return false;

        LinkedHashMap<Talent, Integer> replacement = new LinkedHashMap<>();
        for (Map.Entry<Talent, Integer> slot : tier.entrySet()) {
            replacement.put(slot.getKey() == oldTalent ? newTalent : slot.getKey(),
                    slot.getKey() == oldTalent ? Math.min(slot.getValue(), newTalent.maxPoints()) : slot.getValue());
        }
        hero.talents.set(tierIndex, replacement);
        record(hero, oldTalent, newTalent);
        return true;
    }

    /** Slots count as owned even when no points have been invested. */
    public static Set<Talent> ownedTalents(Hero hero) {
        Set<Talent> result = EnumSet.noneOf(Talent.class);
        for (Map<Talent, Integer> tier : hero.talents) result.addAll(tier.keySet());
        return result;
    }

    public static boolean add(Hero hero, int tierIndex, Talent talent) {
        if (talent == null || tierIndex < 0 || tierIndex >= hero.talents.size()
                || ownedTalents(hero).contains(talent)) return false;
        hero.talents.get(tierIndex).put(talent, 0);
        return true;
    }

    /** Preserve the other-class/talent draw weights, but finish if every talent is owned. */
    public static Talent rollAddedTalent(Hero hero, int tierIndex) {
        if (tierIndex < 0 || tierIndex > 2) return null;
        Set<Talent> owned = ownedTalents(hero);
        LinkedHashMap<Talent, Float> candidates = new LinkedHashMap<>();
        for (HeroClass cls : HeroClass.values()) {
            if (cls == hero.heroClass || (tierIndex == 2 && cls == HeroClass.RAT_KING)) continue;
            Talent[] pool = Talent.talentList(cls, tierIndex + 1);
            for (Talent talent : pool) {
                if (!owned.contains(talent)) {
                    candidates.put(talent, candidates.getOrDefault(talent, 0f) + 1f / pool.length);
                }
            }
        }
        return candidates.isEmpty() ? null : Random.chances(candidates);
    }

    /** Mutare/Metamorphosis offers one unused talent per eligible class and one bonus. */
    public static LinkedHashMap<Talent, Integer> metamorphOptions(Hero hero, Talent replacing, int tierIndex) {
        LinkedHashMap<Talent, Integer> options = new LinkedHashMap<>();
        if (tierIndex < 0 || tierIndex >= hero.talents.size()
                || !hero.talents.get(tierIndex).containsKey(replacing)) return options;
        int points = hero.talents.get(tierIndex).get(replacing);
        Set<Talent> owned = ownedTalents(hero);
        for (HeroClass cls : HeroClass.values()) {
            ArrayList<LinkedHashMap<Talent, Integer>> clsTalents = new ArrayList<>();
            Talent.initClassTalents(cls, clsTalents);
            Set<Talent> candidates = clsTalents.get(tierIndex).keySet();
            if (candidates.contains(replacing)) continue;
            candidates.removeAll(owned);
            if (hero.subClass == HeroSubClass.MONK) candidates.remove(Talent.STRONGMAN);
            if (!candidates.isEmpty()) options.put(Random.element(candidates), points);
        }
        ArrayList<Talent> bonuses = new ArrayList<>();
        Talent[] pool = Talent.bonusTalents.get(tierIndex + 1);
        if (pool != null) for (Talent talent : pool) if (!owned.contains(talent)) bonuses.add(talent);
        if (!bonuses.isEmpty()) options.put(Random.element(bonuses), points);
        return options;
    }

    public static void record(Hero hero, Talent oldTalent, Talent newTalent) {
        // Keep original -> current, rather than accumulating intermediate replacements.
        ArrayList<Talent> origins = new ArrayList<>();
        for (Map.Entry<Talent, Talent> entry : hero.metamorphedTalents.entrySet()) {
            if (entry.getValue() == oldTalent) origins.add(entry.getKey());
        }
        if (origins.isEmpty()) origins.add(oldTalent);
        for (Talent origin : origins) {
            if (origin == newTalent) hero.metamorphedTalents.remove(origin);
            else hero.metamorphedTalents.put(origin, newTalent);
        }
    }

    public static Talent roll(Talent.Aspect aspect, Collection<Talent> excluded) {
        LinkedHashMap<Talent, Float> candidates = new LinkedHashMap<>();
        for (Talent talent : Talent.values()) {
            if (talent.aspect == aspect && !excluded.contains(talent)) {
                candidates.put(talent, talent.maxPoints() == 4 ? 1f / 5
                        : talent.maxPoints() == 3 ? 1f / 3 : 1f);
            }
        }
        // Same rank weights as rejection sampling, without an infinite loop if the pool is empty.
        return candidates.isEmpty() ? null : Random.chances(candidates);
    }

    public static Talent roll(Hero hero, Talent.Aspect aspect) {
        return roll(aspect, ownedTalents(hero));
    }

    public static ArrayList<LinkedHashMap<Talent, Integer>> snapshot(Hero hero) {
        ArrayList<LinkedHashMap<Talent, Integer>> result = new ArrayList<>();
        for (LinkedHashMap<Talent, Integer> tier : hero.talents) result.add(new LinkedHashMap<>(tier));
        return result;
    }

    public static Talent.Aspect mostCommonAspect(Iterable<Talent> talents) {
        EnumMap<Talent.Aspect, Integer> counts = new EnumMap<>(Talent.Aspect.class);
        for (Talent talent : talents) counts.put(talent.aspect, counts.getOrDefault(talent.aspect, 0) + 1);
        Talent.Aspect best = Talent.Aspect.NEUTRAL;
        int max = 0;
        for (Map.Entry<Talent.Aspect, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > max) {
                best = entry.getKey();
                max = entry.getValue();
            }
        }
        return best;
    }
}
