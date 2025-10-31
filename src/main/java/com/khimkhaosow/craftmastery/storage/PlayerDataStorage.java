package com.khimkhaosow.craftmastery.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData; // Добавлен импорт PlayerExperienceData
import com.khimkhaosow.craftmastery.util.Reference;
import net.minecraft.entity.player.EntityPlayer; // Добавлен импорт EntityPlayer
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.nbt.NBTTagCompound;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataStorage extends WorldSavedData {
    public static final String DATA_NAME = Reference.MOD_ID + "_PlayerData";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Map<UUID, PlayerExperienceData> playerData; // Теперь PlayerExperienceData должен быть найден

    public PlayerDataStorage() {
        super(DATA_NAME);
        this.playerData = new HashMap<>();
    }

    public PlayerDataStorage(String name) {
        super(name);
        this.playerData = new HashMap<>();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        String json = nbt.getString("PlayerData");
        if (!json.isEmpty()) {
            Type type = new TypeToken<Map<UUID, PlayerExperienceData>>(){}.getType(); // PlayerExperienceData используется здесь
            this.playerData = GSON.fromJson(json, type);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        String json = GSON.toJson(this.playerData);
        nbt.setString("PlayerData", json);
        return nbt;
    }

    public void savePlayerData(UUID playerUUID, PlayerExperienceData data) { // PlayerExperienceData используется здесь
        this.playerData.put(playerUUID, data);
        this.markDirty();
    }

    public PlayerExperienceData getPlayerData(UUID playerUUID) { // PlayerExperienceData используется здесь
        return this.playerData.getOrDefault(playerUUID, new PlayerExperienceData(playerUUID));
    }

    public static PlayerDataStorage get(EntityPlayer player) { // EntityPlayer используется здесь
        PlayerDataStorage storage = (PlayerDataStorage) player.world.getMapStorage().getOrLoadData(PlayerDataStorage.class, DATA_NAME);
        if (storage == null) {
            storage = new PlayerDataStorage();
            player.world.getMapStorage().setData(DATA_NAME, storage);
        }
        return storage;
    }
}