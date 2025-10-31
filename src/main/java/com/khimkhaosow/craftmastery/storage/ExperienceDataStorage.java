package com.khimkhaosow.craftmastery.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.JsonObject;
import com.khimkhaosow.craftmastery.experience.ExperienceType;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.experience.PointsType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Хранилище данных опыта игроков
 */
public class ExperienceDataStorage extends WorldSavedData {

    private static final String DATA_NAME = "craftmastery_experience";
    private final Map<UUID, PlayerExperienceData> playerDataMap;

    public ExperienceDataStorage() {
        super(DATA_NAME);
        this.playerDataMap = new HashMap<>();
    }

    public ExperienceDataStorage(String name) {
        super(name);
        this.playerDataMap = new HashMap<>();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        playerDataMap.clear();

        // Читаем данные всех игроков
        for (String key : nbt.getKeySet()) {
            try {
                UUID playerUUID = UUID.fromString(key);
                NBTTagCompound playerNbt = nbt.getCompoundTag(key);
                
                PlayerExperienceData data = loadPlayerData(playerUUID, playerNbt);
                playerDataMap.put(playerUUID, data);
            } catch (IllegalArgumentException e) {
                // Пропускаем неверные UUID
                continue;
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        // Сохраняем данные каждого игрока
        for (Map.Entry<UUID, PlayerExperienceData> entry : playerDataMap.entrySet()) {
            NBTTagCompound playerNbt = new NBTTagCompound();
            savePlayerData(entry.getValue(), playerNbt);
            nbt.setTag(entry.getKey().toString(), playerNbt);
        }

        return nbt;
    }

    private PlayerExperienceData loadPlayerData(UUID playerUUID, NBTTagCompound nbt) {
        PlayerExperienceData data = new PlayerExperienceData(playerUUID);

        // Загружаем базовые данные
        data.setLevel(nbt.getInteger("level"));
        data.setCurrentLevelExperience(nbt.getFloat("currentLevelExperience"));
        data.setTotalExperience(nbt.getFloat("totalExperience"));

        // Загружаем опыт по типам
        NBTTagCompound expByType = nbt.getCompoundTag("experienceByType");
        for (ExperienceType type : ExperienceType.values()) {
            if (expByType.hasKey(type.name())) {
                data.setExperience(type, expByType.getFloat(type.name()));
            }
        }

        // Загружаем множители опыта
        NBTTagCompound expMultipliers = nbt.getCompoundTag("experienceMultipliers");
        for (ExperienceType type : ExperienceType.values()) {
            if (expMultipliers.hasKey(type.name())) {
                data.setExperienceMultiplier(type, expMultipliers.getFloat(type.name()));
            }
        }

        // Загружаем очки
        NBTTagCompound points = nbt.getCompoundTag("points");
        for (PointsType type : PointsType.values()) {
            if (points.hasKey(type.name())) {
                data.setPoints(type, points.getInteger(type.name()));
            }
        }

        return data;
    }

    private void savePlayerData(PlayerExperienceData data, NBTTagCompound nbt) {
        // Сохраняем базовые данные
        nbt.setInteger("level", data.getLevel());
        nbt.setFloat("currentLevelExperience", data.getCurrentLevelExperience());
        nbt.setFloat("totalExperience", data.getTotalExperience());

        // Сохраняем опыт по типам
        NBTTagCompound expByType = new NBTTagCompound();
        for (ExperienceType type : ExperienceType.values()) {
            expByType.setFloat(type.name(), data.getExperience(type));
        }
        nbt.setTag("experienceByType", expByType);

        // Сохраняем множители опыта
        NBTTagCompound expMultipliers = new NBTTagCompound();
        for (ExperienceType type : ExperienceType.values()) {
            expMultipliers.setFloat(type.name(), data.getExperienceMultiplier(type));
        }
        nbt.setTag("experienceMultipliers", expMultipliers);

        // Сохраняем очки
        NBTTagCompound points = new NBTTagCompound();
        for (PointsType type : PointsType.values()) {
            points.setInteger(type.name(), data.getPoints(type));
        }
        nbt.setTag("points", points);
    }

    public void savePlayer(PlayerExperienceData data) {
        playerDataMap.put(data.getPlayerUUID(), data);
        markDirty();
    }

    public PlayerExperienceData getPlayerData(UUID playerUUID) {
        return playerDataMap.computeIfAbsent(playerUUID, PlayerExperienceData::new);
    }

    public void resetPlayerData(UUID playerUUID) {
        if (playerDataMap.containsKey(playerUUID)) {
            playerDataMap.get(playerUUID).reset();
            markDirty();
        }
    }

    public JsonObject serializeToJson(UUID playerUUID) {
        PlayerExperienceData data = getPlayerData(playerUUID);
        JsonObject json = new JsonObject();

        json.addProperty("level", data.getLevel());
        json.addProperty("currentLevelExperience", data.getCurrentLevelExperience());
        json.addProperty("totalExperience", data.getTotalExperience());

        // Сериализуем опыт по типам
        JsonObject expByType = new JsonObject();
        for (ExperienceType type : ExperienceType.values()) {
            expByType.addProperty(type.name(), data.getExperience(type));
        }
        json.add("experienceByType", expByType);

        // Сериализуем множители опыта
        JsonObject expMultipliers = new JsonObject();
        for (ExperienceType type : ExperienceType.values()) {
            expMultipliers.addProperty(type.name(), data.getExperienceMultiplier(type));
        }
        json.add("experienceMultipliers", expMultipliers);

        // Сериализуем очки
        JsonObject points = new JsonObject();
        for (PointsType type : PointsType.values()) {
            points.addProperty(type.name(), data.getPoints(type));
        }
        json.add("points", points);

        return json;
    }
}