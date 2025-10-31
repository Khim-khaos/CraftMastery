package com.khimkhaosow.craftmastery.experience;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List; // Добавлен

// Импорты для логирования
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Импорты для других компонентов мода
import com.khimkhaosow.craftmastery.CraftMastery; // Уже был
import com.khimkhaosow.craftmastery.config.ModConfig; // Added
import com.khimkhaosow.craftmastery.network.NetworkHandler; // Уже был
import com.khimkhaosow.craftmastery.network.messages.MessageExperienceSync; // Уже был
import com.khimkhaosow.craftmastery.network.messages.MessageExperienceEffectSync; // Added
import com.khimkhaosow.craftmastery.permissions.PermissionManager; // Уже был
import com.khimkhaosow.craftmastery.permissions.PermissionType; // Уже был
import com.khimkhaosow.craftmastery.storage.PlayerDataStorage; // Уже был
import com.khimkhaosow.craftmastery.util.Reference; // Уже был
import com.khimkhaosow.craftmastery.gui.widgets.ExperienceBarWidget; // Added import

// Импорты для Minecraft/Forge
import net.minecraft.client.Minecraft; // Добавлен
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d; // Уже был
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Менеджер опыта игроков с поддержкой эффектов и кривой опыта
 */
public class ExperienceManager {

    private static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    private static ExperienceManager instance;

    // Конфигурационные множители опыта
    private final Map<ExperienceType, Float> globalMultipliers;

    // Конфигурация получения очков из опыта
    private final Map<ExperienceType, Float> pointsConversionRates;

    // Хранилище данных опыта
    private PlayerDataStorage storage;

    // Системы визуальных эффектов и расчета опыта
    private final ExperienceCurve experienceCurve;
    private final ExperienceEffectManager effectManager;
    
    @SideOnly(Side.CLIENT)
    private ExperienceBarWidget experienceBarWidget; // Предполагается, что этот класс существует

    public ExperienceManager() {
        this.globalMultipliers = new HashMap<>();
        this.pointsConversionRates = new HashMap<>();
        this.experienceCurve = new ExperienceCurve();
        // --- ИСПРАВЛЕНО: используем singleton для ExperienceEffectManager ---
        this.effectManager = ExperienceEffectManager.getInstance();

        setupDefaultConfiguration();
    }

    /**
     * Инициализирует хранилище данных для мира
     */
    public void initStorage(World world) {
        if (world == null || world.isRemote) {
            return;
        }

        MapStorage mapStorage = world.getMapStorage();
        if (mapStorage == null) {
            CraftMastery.logger.error("Unable to access world storage for experience data");
            return;
        }

        storage = (PlayerDataStorage) mapStorage.getOrLoadData(PlayerDataStorage.class, PlayerDataStorage.DATA_NAME);

        if (storage == null) {
            storage = new PlayerDataStorage();
            mapStorage.setData(PlayerDataStorage.DATA_NAME, storage);
            mapStorage.saveAllData();
        }
    }

    public static ExperienceManager getInstance() {
        if (instance == null) {
            instance = new ExperienceManager();
        }
        return instance;
    }

    private void setupDefaultConfiguration() {
        // Множители опыта по умолчанию
        globalMultipliers.put(ExperienceType.BLOCK_MINING, 1.0f);
        globalMultipliers.put(ExperienceType.CRAFTING, 1.0f);
        globalMultipliers.put(ExperienceType.MOB_KILL, 1.0f);
        globalMultipliers.put(ExperienceType.PLAYER_KILL, 1.0f);

        // Конвертация опыта в очки изучения
        pointsConversionRates.put(ExperienceType.BLOCK_MINING, 0.1f);
        pointsConversionRates.put(ExperienceType.CRAFTING, 0.5f);
        pointsConversionRates.put(ExperienceType.MOB_KILL, 0.2f);
        pointsConversionRates.put(ExperienceType.PLAYER_KILL, 1.0f);
    }

    /**
     * Получает данные опыта игрока
     */
    public PlayerExperienceData getPlayerData(UUID playerUUID) {
        if (storage == null) {
            CraftMastery.logger.warn("Experience storage not initialized, creating temporary data for {}", playerUUID);
            return new PlayerExperienceData(playerUUID);
        }
        PlayerExperienceData data = storage.getPlayerData(playerUUID);
        storage.markDirty();
        return data;
    }

    /**
     * Получает данные опыта игрока
     */
    public PlayerExperienceData getPlayerData(EntityPlayer player) {
        if (player == null) return null;
        return getPlayerData(player.getUniqueID());
    }

    /**
     * Сохраняет данные игрока
     */
    private void savePlayerData(PlayerExperienceData data) {
        if (storage != null) {
            storage.savePlayerData(data.getPlayerUUID(), data);
        } else {
            CraftMastery.logger.error("Experience storage not initialized!" );
        }
    }

    /**
     * Добавляет опыт игроку
     */
    public void addExperience(EntityPlayer player, ExperienceType type, float baseAmount) {
        if (player == null || baseAmount <= 0) return;

        PlayerExperienceData data = getPlayerData(player);
        float globalMultiplier = globalMultipliers.getOrDefault(type, 1.0f);
        float actualAmount = baseAmount * globalMultiplier;

        // Добавляем опыт
        int oldLevel = data.getLevel();
        data.addExperience(type, actualAmount);

        // Проверяем повышение уровня
        boolean leveledUp = data.getLevel() > oldLevel;
        if (leveledUp) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                net.minecraft.util.text.TextFormatting.GREEN + "Поздравляем! Вы достигли уровня " + data.getLevel() + "!"));
        }

        // Создаем визуальный эффект получения опыта
        createExperienceEffect(player, actualAmount, leveledUp);

        // Отправляем уведомление об опыте
        player.sendMessage(new net.minecraft.util.text.TextComponentString(
            net.minecraft.util.text.TextFormatting.YELLOW + "Получено " + String.format("%.1f", actualAmount) + " опыта (" + type.getDisplayName() + ")"));

        // Конвертируем часть опыта в очки изучения
        float pointsToAdd = actualAmount * pointsConversionRates.getOrDefault(type, 0.0f);
        if (pointsToAdd > 0) {
            data.addPoints(PointsType.LEARNING, (int) pointsToAdd);
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                net.minecraft.util.text.TextFormatting.AQUA + "Получено " + (int) pointsToAdd + " очков изучения!"));
        }

        // Сохраняем изменения
        savePlayerData(data);

        // Синхронизация с клиентом если это серверный игрок
        if (player instanceof EntityPlayerMP) {
            syncToClient((EntityPlayerMP) player, data);
        }
    }

    /**
     * Добавляет очки игроку
     */
    public void addPoints(EntityPlayer player, PointsType type, int amount) {
        if (player == null || amount == 0) return;

        PlayerExperienceData data = getPlayerData(player);
        data.addPoints(type, amount);

        // Сохраняем изменения
        savePlayerData(data);

        // Синхронизация с клиентом если это серверный игрок
        if (player instanceof EntityPlayerMP) {
            syncToClient((EntityPlayerMP) player, data);
        }

        // Отправляем уведомление
        player.sendMessage(new net.minecraft.util.text.TextComponentString(
            net.minecraft.util.text.TextFormatting.AQUA + "Получено " + amount + " " + type.getDisplayName() + "!"));
    }

    /**
     * Снимает очки с игрока
     */
    public boolean spendPoints(EntityPlayer player, PointsType type, int amount) {
        if (player == null || amount <= 0) return false;

        PlayerExperienceData data = getPlayerData(player);
        boolean success = data.spendPoints(type, amount);

        if (success) {
            savePlayerData(data);
            if (player instanceof EntityPlayerMP) {
                syncToClient((EntityPlayerMP) player, data);
            }
        }

        return success;
    }

    /**
     * Событие: игрок сломал блок
     */
    @SubscribeEvent // Аннотация должна быть найдена
    public void onBlockBreak(BreakEvent event) {
        EntityPlayer player = event.getPlayer(); // EntityPlayer должен быть найден
        if (player == null || player.world.isRemote) return;

        // Проверяем права доступа
        if (!PermissionManager.getInstance().hasPermission(player, PermissionType.LEARN_RECIPES)) { // Предполагаю, что тут LEARN_RECIPES, а не OPEN_INTERFACE
            return;
        }

        // Добавляем опыт за добычу блока
        float experience = calculateBlockExperience(event);
        if (experience > 0) {
            addExperience(player, ExperienceType.BLOCK_MINING, experience);
        }
    }

    /**
     * Событие: игрок зашел на сервер
     */
    @SubscribeEvent // Аннотация должна быть найдена
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        initStorage(player.world);
        PlayerExperienceData data = getPlayerData(player);
        CraftMastery.logger.info("Loaded experience data for player: {}", player.getName());
        if (player instanceof EntityPlayerMP) {
            syncToClient((EntityPlayerMP) player, data);
        }
    }

    /**
     * Событие: игрок вышел с сервера
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        if (storage != null) {
            PlayerExperienceData data = getPlayerData(player);
            savePlayerData(data);
            CraftMastery.logger.info("Saved experience data for player: {}", player.getName());
        }

        // Очищаем эффекты при выходе игрока
        clearEffects();
    }

    /**
     * Обработка изменения размера экрана
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onResolutionChanged(net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof net.minecraft.client.gui.GuiIngameMenu || 
            event.getGui() instanceof net.minecraft.client.gui.inventory.GuiInventory) {
            Minecraft mc = Minecraft.getMinecraft();
            updateWidgetScale(mc, mc.displayWidth, mc.displayHeight);
        }
    }

    /**
     * Обработка рендера игрового интерфейса
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderGameOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
        if (event.getType() == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.EXPERIENCE) {
            Minecraft mc = Minecraft.getMinecraft();
            renderWidget(mc);
        }
    }

    /**
     * Событие: смерть существа
     */
    @SubscribeEvent // Аннотация должна быть найдена
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            if (player == null || player.world.isRemote) return;

            // Проверяем права доступа
            if (!PermissionManager.getInstance().hasPermission(player, PermissionType.OPEN_INTERFACE)) {
                return;
            }

            ExperienceType experienceType;
            float experience;

            if (event.getEntity() instanceof EntityPlayer) {
                // Убийство игрока
                experienceType = ExperienceType.PLAYER_KILL;
                experience = 50.0f; // Базовый опыт за убийство игрока
            } else {
                // Убийство моба
                experienceType = ExperienceType.MOB_KILL;
                experience = calculateMobExperience(event.getEntity());
            }

            addExperience(player, experienceType, experience);
        }
    }

    /**
     * Вычисляет опыт за сломанный блок
     */
    private float calculateBlockExperience(BreakEvent event) {
        // Опыт зависит от твердости блока и инструмента
        float hardness = event.getState().getBlock().getBlockHardness(event.getState(), event.getWorld(), event.getPos());

        // Минимальный опыт 1.0, максимальный 10.0
        return Math.max(1.0f, Math.min(10.0f, hardness * 2.0f));
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        initStorage(world);
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (storage != null) {
            storage.markDirty();
        }
    }

    public void syncWithConfig() {
        globalMultipliers.put(ExperienceType.BLOCK_MINING,
                ModConfig.globalExperienceMultiplier * ModConfig.blockMiningMultiplier);
        globalMultipliers.put(ExperienceType.CRAFTING,
                ModConfig.globalExperienceMultiplier * ModConfig.craftingMultiplier);
        globalMultipliers.put(ExperienceType.MOB_KILL,
                ModConfig.globalExperienceMultiplier * ModConfig.mobKillMultiplier);
        globalMultipliers.put(ExperienceType.PLAYER_KILL,
                ModConfig.globalExperienceMultiplier * ModConfig.playerKillMultiplier);

        pointsConversionRates.put(ExperienceType.BLOCK_MINING, ModConfig.blockMiningToLearningRatio);
        pointsConversionRates.put(ExperienceType.CRAFTING, ModConfig.craftingToLearningRatio);
        pointsConversionRates.put(ExperienceType.MOB_KILL, ModConfig.mobKillToLearningRatio);
        pointsConversionRates.put(ExperienceType.PLAYER_KILL, ModConfig.playerKillToLearningRatio);
    }

    /**
     * Вычисляет опыт за убийство моба
     */
    private float calculateMobExperience(net.minecraft.entity.Entity entity) {
        // Базовый опыт в зависимости от типа моба
        if (entity instanceof net.minecraft.entity.boss.EntityDragon) {
            return 1000.0f; // Дракон
        } else if (entity instanceof net.minecraft.entity.boss.EntityWither) {
            return 500.0f; // Визер
        } else if (entity instanceof net.minecraft.entity.monster.EntityMob) {
            return 10.0f; // Обычный моб
        } else if (entity instanceof net.minecraft.entity.passive.EntityAnimal) {
            return 2.0f; // Животное
        }

        return 5.0f; // По умолчанию
    }

    /**
     * Синхронизирует данные с клиентом
     */
    private void syncToClient(EntityPlayerMP player, PlayerExperienceData data) {
        // Отправляем основные данные опыта
        // --- ПРЕДПОЛАГАЕТСЯ, ЧТО MessageExperienceSync ИМЕЕТ КОНСТРУКТОР (PlayerExperienceData, float) ---
        NetworkHandler.INSTANCE.sendTo(new MessageExperienceSync(data, getLevelProgress(data)), player);
        
        // Отправляем информацию об эффектах всем игрокам в радиусе видимости
        // --- ПРЕДПОЛАГАЕТСЯ, ЧТО effectManager ИМЕЕТ МЕТОДЫ hasActiveEffects() и getActiveEffects() ---
        // --- ПРЕДПОЛАГАЕТСЯ, ЧТО MessageExperienceEffectSync ИМЕЕТ КОНСТРУКТОР (List<ExperienceEffect>) ---
        if (effectManager.hasActiveEffects()) {
            NetworkHandler.INSTANCE.sendToAllTracking(
                new MessageExperienceEffectSync(effectManager.getActiveEffects()),
                player
            );
        }
    }

    /**
     * Сбрасывает данные игрока
     */
    public void resetPlayerData(EntityPlayer player) {
        if (player == null) return;

        PlayerExperienceData data = getPlayerData(player);
        data.reset();
        savePlayerData(data);

        // CraftMastery.logger должен быть найден
        CraftMastery.logger.info("Reset experience data for player: {}", player.getName());
    }

    /**
     * Устанавливает глобальный множитель опыта
     */
    public void setGlobalMultiplier(ExperienceType type, float multiplier) {
        globalMultipliers.put(type, Math.max(0.0f, multiplier));
    }

    /**
     * Получает глобальный множитель опыта
     */
    public float getGlobalMultiplier(ExperienceType type) {
        return globalMultipliers.getOrDefault(type, 1.0f);
    }

    /**
     * Создает эффект получения опыта
     */
    public void createExperienceEffect(EntityPlayer player, float amount, boolean levelUp) {
        if (player.world.isRemote) {
            Vec3d pos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
            // --- ПРЕДПОЛАГАЕТСЯ, ЧТО effectManager ИМЕЕТ МЕТОД createExperienceEffect(Vec3d, float, boolean) ---
            effectManager.createExperienceEffect(pos, amount, levelUp);
        }
    }

    /**
     * Получает менеджер эффектов (только для клиента)
     */
    @SideOnly(Side.CLIENT)
    public ExperienceEffectManager getEffectManager() {
        return effectManager;
    }

    /**
     * Получает прогресс уровня игрока (0.0 - 1.0)
     */
    public float getLevelProgress(PlayerExperienceData data) {
        // --- ИСПРАВЛЕНО: явное преобразование для избежания потери точности ---
        long currentXp = (long) data.getTotalExperience(); // Используем long для промежуточного хранения
        int currentLevel = data.getLevel();
        long xpForNextLevel = experienceCurve.getExperienceForLevel(currentLevel + 1);
        
        if (xpForNextLevel == 0) return 0.0f; // Избегаем деления на 0
        return (float) (currentXp % xpForNextLevel) / xpForNextLevel;
    }
    
    /**
     * Обновляет визуальные эффекты
     */
    @SideOnly(Side.CLIENT)
    public void updateEffects() {
        if (effectManager != null) {
            // ПРЕДПОЛАГАЕТСЯ, ЧТО effectManager ИМЕЕТ МЕТОД update()
            effectManager.update();
        }
    }
    
    /**
     * Обрабатывает синхронизацию эффектов от сервера
     */
    @SideOnly(Side.CLIENT)
    public void handleEffectSync(List<ExperienceEffect> effects) { // Требует import java.util.List;
        if (effectManager != null) {
            // ПРЕДПОЛАГАЕТСЯ, ЧТО effectManager ИМЕЕТ МЕТОД syncEffects(List<ExperienceEffect>)
            effectManager.syncEffects(effects);
        }
    }
    
    /**
     * Очищает все эффекты
     */
    public void clearEffects() {
        if (effectManager != null) {
            effectManager.clearEffects(); // ПРЕДПОЛАГАЕТСЯ, ЧТО МЕТОД СУЩЕСТВУЕТ
        }
    }
    
    /**
     * Получает кривую опыта
     */
    public ExperienceCurve getExperienceCurve() {
        return experienceCurve;
    }

    /**
     * Обрабатывает обновления синхронизации с сервера
     * @param data Данные опыта игрока
     * @param levelProgress Прогресс текущего уровня (0.0 - 1.0)
     */
    @SideOnly(Side.CLIENT)
    public void handleSyncUpdate(PlayerExperienceData data, float levelProgress) {
        // Обновляем данные в хранилище
        if (storage != null) {
            storage.savePlayerData(data.getPlayerUUID(), data);
        }

        // Обновляем интерфейс, если он открыт
        // ПРЕДПОЛАГАЕТСЯ, ЧТО GuiExperience СУЩЕСТВУЕТ
        if (Minecraft.getMinecraft().currentScreen instanceof com.khimkhaosow.craftmastery.gui.GuiExperience) {
            ((com.khimkhaosow.craftmastery.gui.GuiExperience)Minecraft.getMinecraft().currentScreen).updateData(data, levelProgress);
        }

        // Обновляем виджет опыта
        if (experienceBarWidget != null) {
            // ПРЕДПОЛАГАЕТСЯ, ЧТО У ExperienceBarWidget ЕСТЬ МЕТОДЫ setProgress, setLevel, updateDisplay
            experienceBarWidget.setProgress(levelProgress);
            experienceBarWidget.setLevel(data.getLevel());
            experienceBarWidget.updateDisplay();
        }
    }

    /**
     * Инициализирует виджет опыта на клиентской стороне
     */
    @SideOnly(Side.CLIENT)
    public void initializeWidget(Minecraft mc) { // Требует import net.minecraft.client.Minecraft;
        if (experienceBarWidget == null) {
            // ПРЕДПОЛАГАЕТСЯ, ЧТО У ExperienceBarWidget ЕСТЬ КОНСТРУКТОР (Minecraft)
            experienceBarWidget = new ExperienceBarWidget(mc, mc.player);
        }
    }

    /**
     * Отрисовывает виджет опыта
     */
    @SideOnly(Side.CLIENT)
    public void renderWidget(Minecraft mc) { // Требует import net.minecraft.client.Minecraft;
        if (experienceBarWidget != null && mc.player != null && !mc.gameSettings.hideGUI) {
            PlayerExperienceData data = getPlayerData(mc.player);
            experienceBarWidget.setLevel(data.getLevel());
            experienceBarWidget.setProgress(getLevelProgress(data));
            experienceBarWidget.render(); // ПРЕДПОЛАГАЕТСЯ, ЧТО МЕТОД СУЩЕСТВУЕТ
        }
    }

    /**
     * Обновляет размер виджета при изменении размера экрана
     */
    @SideOnly(Side.CLIENT)
    public void updateWidgetScale(Minecraft mc, int width, int height) { // Требует import net.minecraft.client.Minecraft;
        if (experienceBarWidget != null) {
            // ПРЕДПОЛАГАЕТСЯ, ЧТО У ExperienceBarWidget ЕСТЬ МЕТОД updateScale(int, int)
            experienceBarWidget.updateScale(width, height);
        }
    }
}