package xyz.stormclient.util;

import java.util.List;
import java.util.Random;

public final class RandomUtil {

    private static final Random RANDOM = new Random();

    private RandomUtil() { }

    public static double range(double min, double max) {
        return min >= max ? min : min + RANDOM.nextDouble() * (max - min);
    }

    public static int range(int min, int max) {
        return min >= max ? min : min + RANDOM.nextInt(max - min + 1);
    }

    public static float gaussian(float mean, float deviation) {
        return (float) (mean + RANDOM.nextGaussian() * deviation);
    }

    public static boolean chance(double percent) { return RANDOM.nextDouble() * 100.0 < percent; }

    public static <T> T pick(List<T> list) {
        return list.isEmpty() ? null : list.get(RANDOM.nextInt(list.size()));
    }

    public static Random random() { return RANDOM; }
}
