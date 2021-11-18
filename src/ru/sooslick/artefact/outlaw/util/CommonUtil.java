package ru.sooslick.artefact.outlaw.util;

import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * Utility class with common methods like formatting or random
 */
public class CommonUtil {
    public static final Random RANDOM = new Random();

    private CommonUtil() {}

    /**
     * Returns one random element from collection
     * @param set collection
     * @return random element of collection
     */
    public static <E> E getRandomOf(Collection<E> set) {
        if (set.size() <= 0)
            return null;
        return set.stream().skip(RANDOM.nextInt(set.size())).findFirst().orElse(null);
    }

    public static ChatColor getRandomColor() {
        return ChatColor.getByChar(Integer.toHexString(RANDOM.nextInt(16)));
    }
}
