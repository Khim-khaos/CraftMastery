package com.khimkhaosow.craftmastery.experience;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 1. Импорты для других компонентов мода
import com.khimkhaosow.craftmastery.CraftMastery; // Уже был
import com.khimkhaosow.craftmastery.experience.ExperienceType; // Добавлен импорт
import com.khimkhaosow.craftmastery.experience.PointsType;     // Добавлен импорт

/**
 * Данные опыта и очков игрока
 */
public class PlayerExperienceData {

    private final UUID playerUUID;

    // Текущий уровень игрока
    private int level = 1;

    // Текущий опыт в рамках уровня
    private float currentLevelExperience = 0.0f;

    // Общий накопленный опыт
    private float totalExperience = 0.0f;

    // Очки игрока
    private final Map<PointsType, Integer> points; // PointsType должен быть найден

    // Опыт по источникам
    private final Map<ExperienceType, Float> experienceByType; // ExperienceType должен быть найден

    // Настройки множителей опыта (из конфигурации)
    private final Map<ExperienceType, Float> experienceMultipliers; // ExperienceType должен быть найден

    public PlayerExperienceData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.points = new HashMap<>();
        this.experienceByType = new HashMap<>();
        this.experienceMultipliers = new HashMap<>();

        // Инициализация всех типов очков
        for (PointsType type : PointsType.values()) { // PointsType должен быть найден
            points.put(type, 0);
        }

        // Инициализация опыта по типам
        for (ExperienceType type : ExperienceType.values()) { // ExperienceType должен быть найден
            experienceByType.put(type, 0.0f);
            experienceMultipliers.put(type, 1.0f); // По умолчанию множитель 1.0
        }
    }

    /**
     * Добавляет опыт игроку
     */
    public void addExperience(ExperienceType type, float amount) { // ExperienceType должен быть найден
        if (amount <= 0) return;

        float actualAmount = amount * experienceMultipliers.get(type); // ExperienceType должен быть найден
        experienceByType.put(type, experienceByType.get(type) + actualAmount); // ExperienceType должен быть найден
        currentLevelExperience += actualAmount;
        totalExperience += actualAmount;

        // Проверяем, не пора ли повысить уровень
        checkLevelUp();

        // CraftMastery.logger должен быть найден (предполагается, что это статическое поле в классе CraftMastery)
        CraftMastery.logger.debug("Player {} gained {} experience from {}, total: {}",
            playerUUID, actualAmount, type.name(), totalExperience);
    }

    /**
     * Проверяет и выполняет повышение уровня
     */
    private void checkLevelUp() {
        float requiredExperience = getExperienceForNextLevel();
        while (currentLevelExperience >= requiredExperience) {
            levelUp();
        }
    }

    /**
     * Повышает уровень игрока
     */
    private void levelUp() {
        currentLevelExperience -= getExperienceForNextLevel();
        level++;

        // CraftMastery.logger должен быть найден (предполагается, что это статическое поле в классе CraftMastery)
        CraftMastery.logger.info("Player {} leveled up to level {}", playerUUID, level);
    }

    /**
     * Получает необходимый опыт для следующего уровня
     */
    public float getExperienceForNextLevel() {
        // Формула: базовый_опыт * (уровень ^ 1.5)
        return 100.0f * (float) Math.pow(level, 1.5);
    }

    /**
     * Получает прогресс до следующего уровня в процентах
     */
    public float getLevelProgress() {
        float required = getExperienceForNextLevel();
        return required > 0 ? (currentLevelExperience / required) * 100.0f : 100.0f;
    }

    /**
     * Добавляет очки игроку
     */
    public void addPoints(PointsType type, int amount) { // PointsType должен быть найден
        if (amount == 0) return;

        int currentPoints = points.getOrDefault(type, 0); // PointsType должен быть найден
        points.put(type, currentPoints + amount); // PointsType должен быть найден

        // CraftMastery.logger должен быть найден (предполагается, что это статическое поле в классе CraftMastery)
        CraftMastery.logger.debug("Player {} gained {} {}, total: {}",
            playerUUID, amount, type.getDisplayName(), points.get(type)); // PointsType должен быть найден
    }

    /**
     * Тратит очки игрока
     */
    public boolean spendPoints(PointsType type, int amount) { // PointsType должен быть найден
        int currentPoints = points.getOrDefault(type, 0); // PointsType должен быть найден
        if (currentPoints < amount) {
            return false;
        }

        points.put(type, currentPoints - amount); // PointsType должен быть найден
        // CraftMastery.logger должен быть найден (предполагается, что это статическое поле в классе CraftMastery)
        CraftMastery.logger.debug("Player {} spent {} {}, remaining: {}",
            playerUUID, amount, type.getDisplayName(), points.get(type)); // PointsType должен быть найден
        return true;
    }

    /**
     * Получает количество очков определенного типа
     */
    public int getPoints(PointsType type) { // PointsType должен быть найден
        return points.getOrDefault(type, 0); // PointsType должен быть найден
    }

    /**
     * Получает опыт определенного типа
     */
    public float getExperience(ExperienceType type) { // ExperienceType должен быть найден
        return experienceByType.getOrDefault(type, 0.0f); // ExperienceType должен быть найден
    }

    /**
     * Устанавливает множитель опыта для типа
     */
    public void setExperienceMultiplier(ExperienceType type, float multiplier) { // ExperienceType должен быть найден
        experienceMultipliers.put(type, Math.max(0.0f, multiplier)); // ExperienceType должен быть найден
    }

    /**
     * Получает множитель опыта для типа
     */
    public float getExperienceMultiplier(ExperienceType type) { // ExperienceType должен быть найден
        return experienceMultipliers.getOrDefault(type, 1.0f); // ExperienceType должен быть найден
    }

    /**
     * Устанавливает опыт определенного типа
     */
    public void setExperience(ExperienceType type, float experience) { // ExperienceType должен быть найден
        experienceByType.put(type, Math.max(0.0f, experience)); // ExperienceType должен быть найден
    }

    /**
     * Устанавливает количество очков определенного типа
     */
    public void setPoints(PointsType type, int amount) { // PointsType должен быть найден
        points.put(type, Math.max(0, amount)); // PointsType должен быть найден
    }

    /**
     * Устанавливает текущий уровень
     */
    public void setLevel(int newLevel) {
        this.level = Math.max(1, newLevel);
    }

    /**
     * Устанавливает текущий опыт уровня
     */
    public void setCurrentLevelExperience(float experience) {
        this.currentLevelExperience = Math.max(0.0f, experience);
    }

    /**
     * Устанавливает общий опыт
     */
    public void setTotalExperience(float experience) {
        this.totalExperience = Math.max(0.0f, experience);
    }

    // Геттеры
    public int getLevel() { return level; }
    public float getCurrentLevelExperience() { return currentLevelExperience; }
    public float getTotalExperience() { return totalExperience; }
    public UUID getPlayerUUID() { return playerUUID; }

    /**
     * Сбрасывает все данные игрока
     */
    public void reset() {
        level = 1;
        currentLevelExperience = 0.0f;
        totalExperience = 0.0f;

        for (PointsType type : PointsType.values()) { // PointsType должен быть найден
            points.put(type, 0); // PointsType должен быть найден
        }

        for (ExperienceType type : ExperienceType.values()) { // ExperienceType должен быть найден
            experienceByType.put(type, 0.0f); // ExperienceType должен быть найден
        }
    }
}