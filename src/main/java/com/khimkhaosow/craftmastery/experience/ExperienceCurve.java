package com.khimkhaosow.craftmastery.experience;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.khimkhaosow.craftmastery.CraftMastery;

// --- Добавленные импорты ---
import com.khimkhaosow.craftmastery.experience.ExperienceType; // Для использования в других методах (если потребуется)
import com.khimkhaosow.craftmastery.experience.PointsType;     // Для использования в других методах (если потребуется)

/**
 * Класс для управления кривой опыта и расчета уровней
 */
public class ExperienceCurve {
    private static final int MAX_LEVEL = 100;
    private final long[] experienceForLevel;
    private final int[] pointsPerLevel;
    private final float[] multiplierPerLevel;

    public ExperienceCurve() {
        this.experienceForLevel = new long[MAX_LEVEL + 1];
        this.pointsPerLevel = new int[MAX_LEVEL + 1];
        this.multiplierPerLevel = new float[MAX_LEVEL + 1];

        initializeDefaultCurve();
    }

    /**
     * Инициализирует стандартную кривую опыта
     */
    private void initializeDefaultCurve() {
        // Базовый опыт для первого уровня
        experienceForLevel[0] = 0;
        experienceForLevel[1] = 100;

        // Очки за уровень
        pointsPerLevel[0] = 0;
        pointsPerLevel[1] = 1;

        // Множители опыта
        multiplierPerLevel[0] = 1.0f;
        multiplierPerLevel[1] = 1.0f;

        // Генерируем значения для остальных уровней
        for (int level = 2; level <= MAX_LEVEL; level++) {
            // Формула: каждый следующий уровень требует на 15% больше опыта
            experienceForLevel[level] = Math.round(experienceForLevel[level - 1] * 1.15);

            // Очки: каждые 5 уровней дают +1 очко
            pointsPerLevel[level] = 1 + (level / 5);

            // Множитель: небольшое увеличение каждые 10 уровней
            multiplierPerLevel[level] = 1.0f + (level / 10) * 0.1f;
        }
    }

    /**
     * Загружает кривую опыта из JSON конфигурации
     */
    public void loadFromJson(JsonObject json) {
        try {
            if (json.has("experienceCurve")) {
                JsonArray expArray = json.getAsJsonArray("experienceCurve");
                for (int i = 0; i < expArray.size() && i <= MAX_LEVEL; i++) {
                    experienceForLevel[i] = expArray.get(i).getAsLong();
                }
            }

            if (json.has("pointsPerLevel")) {
                JsonArray pointsArray = json.getAsJsonArray("pointsPerLevel");
                for (int i = 0; i < pointsArray.size() && i <= MAX_LEVEL; i++) {
                    pointsPerLevel[i] = pointsArray.get(i).getAsInt();
                }
            }

            if (json.has("multipliers")) {
                JsonArray multArray = json.getAsJsonArray("multipliers");
                for (int i = 0; i < multArray.size() && i <= MAX_LEVEL; i++) {
                    multiplierPerLevel[i] = multArray.get(i).getAsFloat();
                }
            }
        } catch (Exception e) {
            CraftMastery.logger.error("Error loading experience curve from JSON", e);
            initializeDefaultCurve(); // Восстанавливаем стандартную кривую в случае ошибки
        }
    }

    /**
     * Сохраняет кривую опыта в JSON
     */
    public JsonObject saveToJson() {
        JsonObject json = new JsonObject();
        JsonArray expArray = new JsonArray();
        JsonArray pointsArray = new JsonArray();
        JsonArray multArray = new JsonArray();

        for (int i = 0; i <= MAX_LEVEL; i++) {
            expArray.add(experienceForLevel[i]);
            pointsArray.add(pointsPerLevel[i]);
            multArray.add(multiplierPerLevel[i]);
        }

        json.add("experienceCurve", expArray);
        json.add("pointsPerLevel", pointsArray);
        json.add("multipliers", multArray);

        return json;
    }

    /**
     * Получает необходимый опыт для достижения уровня
     */
    public long getExperienceForLevel(int level) {
        if (level < 0) return 0;
        if (level > MAX_LEVEL) level = MAX_LEVEL; // Ограничиваем максимальным уровнем
        return experienceForLevel[level];
    }

    /**
     * Вычисляет уровень по опыту
     */
    public int calculateLevel(long experience) {
        for (int level = MAX_LEVEL; level >= 0; level--) {
            if (experience >= experienceForLevel[level]) {
                return level;
            }
        }
        return 0; // Если опыт меньше, чем нужно для 1-го уровня
    }

    /**
     * Вычисляет прогресс до следующего уровня (0.0 - 1.0)
     */
    public float calculateProgress(long experience) {
        int currentLevel = calculateLevel(experience);
        if (currentLevel >= MAX_LEVEL) return 1.0f; // Максимальный уровень - 100% прогресс

        long currentLevelExp = experienceForLevel[currentLevel];
        long nextLevelExp = experienceForLevel[currentLevel + 1];
        long expInLevel = experience - currentLevelExp;
        long expNeeded = nextLevelExp - currentLevelExp;

        return (float) expInLevel / expNeeded;
    }

    /**
     * Проверяет, является ли уровень максимальным
     */
    public boolean isMaxLevel(int level) {
        return level >= MAX_LEVEL;
    }

    /**
     * Получает максимальный уровень
     */
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    /**
     * Получает количество очков за уровень
     */
    public int getPointsForLevel(int level) {
        if (level < 0) return 0;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        return pointsPerLevel[level];
    }

    /**
     * Получает множитель опыта для уровня
     */
    public float getMultiplierForLevel(int level) {
        if (level < 0) return 1.0f;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        return multiplierPerLevel[level];
    }

    /**
     * Устанавливает количество очков за уровень (для конфигурации)
     */
    public void setPointsForLevel(int level, int points) {
        if (level >= 0 && level <= MAX_LEVEL) {
            pointsPerLevel[level] = Math.max(0, points); // Не меньше 0
        }
    }

    /**
     * Устанавливает множитель опыта для уровня (для конфигурации)
     */
    public void setMultiplierForLevel(int level, float multiplier) {
        if (level >= 0 && level <= MAX_LEVEL) {
            multiplierPerLevel[level] = Math.max(0.1f, multiplier); // Не меньше 0.1
        }
    }
}
