package com.khimkhaosow.craftmastery.experience;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Управляет визуальными эффектами получения опыта
 * Singleton-класс, используется только на клиентской стороне.
 */
@SideOnly(Side.CLIENT) // Указывает, что этот класс используется только на клиенте
public class ExperienceEffectManager {

    // Единственный экземпляр класса (Singleton)
    private static ExperienceEffectManager instance;

    // Список активных эффектов
    private final List<ExperienceEffect> activeEffects;

    // Приватный конструктор для обеспечения singleton
    private ExperienceEffectManager() {
        this.activeEffects = new ArrayList<>();
    }

    /**
     * Получает единственный экземпляр менеджера эффектов.
     * @return Экземпляр ExperienceEffectManager
     */
    public static ExperienceEffectManager getInstance() {
        if (instance == null) {
            instance = new ExperienceEffectManager();
        }
        return instance;
    }

    /**
     * Создает новый эффект получения опыта и добавляет его в список активных.
     * @param player Игрок, для которого создается эффект
     * @param position Позиция эффекта
     * @param amount Количество опыта
     * @param source Источник опыта
     */
    public void createEffect(EntityPlayer player, Vec3d position, int amount, ExperienceEffect.ExperienceSource source) {
        activeEffects.add(new ExperienceEffect(player, position, amount, source));
    }

    /**
     * Создает эффект в позиции игрока (с небольшим смещением по высоте).
     * @param player Игрок, для которого создается эффект
     * @param amount Количество опыта
     * @param source Источник опыта
     */
    public void createEffectAtPlayer(EntityPlayer player, int amount, ExperienceEffect.ExperienceSource source) {
        // Используем add() вместо addVector() для суммирования векторов
        Vec3d playerPos = player.getPositionVector();
        Vec3d offset = new Vec3d(0, player.height + 0.5, 0);
        Vec3d effectPos = playerPos.add(offset);
        createEffect(player, effectPos, amount, source);
    }

    /**
     * Обработчик тика клиента. Обновляет и удаляет устаревшие эффекты.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return; // Обрабатываем только в конце тика

        Iterator<ExperienceEffect> iterator = activeEffects.iterator();
        while (iterator.hasNext()) {
            ExperienceEffect effect = iterator.next();
            effect.update(); // Обновляем состояние эффекта
            if (!effect.isAlive()) { // Проверяем, жив ли эффект
                iterator.remove(); // Удаляем устаревший эффект
            }
        }
    }

    /**
     * Обработчик рендеринга мира. Отрисовывает все активные эффекты.
     */
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player; // Получаем игрока
        if (player == null) return; // Если игрока нет (например, в меню), выходим

        for (ExperienceEffect effect : activeEffects) { // Проходим по всем активным эффектам
            effect.render(event.getPartialTicks()); // Отрисовываем эффект
        }
    }

    /**
     * Очищает все активные эффекты.
     */
    public void clearEffects() {
        activeEffects.clear();
    }

    /**
     * Получает количество активных эффектов.
     * @return Количество активных эффектов
     */
    public int getActiveEffectsCount() {
        return activeEffects.size();
    }

    // --- Методы, добавленные для удовлетворения требований ExperienceManager.java ---
    // Эти методы были вызваны в ExperienceManager.java, но не реализованы ранее.

    /**
     * Проверяет, есть ли активные эффекты.
     * @return true, если есть активные эффекты, иначе false
     */
    public boolean hasActiveEffects() {
        return !activeEffects.isEmpty();
    }

    /**
     * Получает список активных эффектов.
     * @return Список активных эффектов
     */
    public List<ExperienceEffect> getActiveEffects() {
        // Возвращаем копию списка, чтобы избежать проблем с модификацией извне
        return new ArrayList<>(activeEffects);
    }

    /**
     * Синхронизирует эффекты с сервера (заглушка).
     * В реальном проекте здесь будет логика обновления эффектов на клиенте
     * на основе данных, полученных от сервера.
     * @param effects Список эффектов для синхронизации
     */
    public void syncEffects(List<ExperienceEffect> effects) {
        // В простейшем случае, просто заменяем текущие эффекты на полученные.
        // В более сложном случае, нужно сопоставить и обновить/добавить/удалить эффекты.
        // Для заглушки просто очистим и добавим новые.
        clearEffects();
        activeEffects.addAll(effects);
    }

    /**
     * Обновляет состояние эффектов (вызывается на клиенте).
     * В текущей реализации обновление происходит в onClientTick.
     * Этот метод может быть использован для других целей, например, для синхронизации с сервером.
     */
    public void update() {
        // Логика обновления, если требуется отдельно от тика.
        // В текущей реализации, обновление происходит в onClientTick.
        // Оставим пустым или добавим логику, если нужно.
        // Для простоты, просто вызовем обновление для каждого эффекта.
        // (Хотя это уже делается в onClientTick)
        // activeEffects.forEach(ExperienceEffect::update);
    }

    /**
     * Создает эффект получения опыта (альтернативный метод с Vec3d).
     * @param pos Позиция эффекта
     * @param amount Количество опыта
     * @param levelUp Является ли это повышением уровня
     */
    public void createExperienceEffect(Vec3d pos, float amount, boolean levelUp) {
        // Предположим, что EntityPlayer передается в этот метод или получается из контекста.
        // Для простоты, используем текущего игрока.
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null) {
            // Определяем источник на основе параметра levelUp или другого контекста.
            ExperienceEffect.ExperienceSource source = levelUp ? ExperienceEffect.ExperienceSource.LEVEL_UP : ExperienceEffect.ExperienceSource.CRAFTING;
            // Преобразуем float amount в int (или используйте double/float в ExperienceEffect, если нужно точность)
            createEffect(player, pos, (int) amount, source);
        }
    }
}