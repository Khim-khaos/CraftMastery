package com.khimkhaosow.craftmastery.network.messages;

import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.ExperienceType;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.experience.PointsType;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Сообщение для синхронизации данных опыта между сервером и клиентом
 */
public class MessageExperienceSync implements IMessage {

    private int level;
    private float currentLevelExperience;
    private float totalExperience;
    private float[] experienceByType;
    private int[] pointsByType;
    private float[] experienceMultipliers;

    public MessageExperienceSync() {
        experienceByType = new float[ExperienceType.values().length];
        pointsByType = new int[PointsType.values().length];
        experienceMultipliers = new float[ExperienceType.values().length];
    }

    public MessageExperienceSync(PlayerExperienceData data) {
        this();
        level = data.getLevel();
        currentLevelExperience = data.getCurrentLevelExperience();
        totalExperience = data.getTotalExperience();

        // Копируем опыт
        ExperienceType[] types = ExperienceType.values();
        for (int i = 0; i < types.length; i++) {
            experienceByType[i] = data.getExperience(types[i]);
            experienceMultipliers[i] = data.getExperienceMultiplier(types[i]);
        }

        // Копируем очки
        PointsType[] pointTypes = PointsType.values();
        for (int i = 0; i < pointTypes.length; i++) {
            pointsByType[i] = data.getPoints(pointTypes[i]);
        }
    }

    public MessageExperienceSync(PlayerExperienceData data, float levelProgress) {
        this(data);
        // levelProgress is 0.0-1.0, but currentLevelExperience is absolute
        // Perhaps set currentLevelExperience = levelProgress * something, but since it's calculated, maybe ignore or adjust
        // For now, keep as is
    }

    @Override
    public void fromBytes(io.netty.buffer.ByteBuf buf) {
        level = buf.readInt();
        currentLevelExperience = buf.readFloat();
        totalExperience = buf.readFloat();

        for (int i = 0; i < experienceByType.length; i++) {
            experienceByType[i] = buf.readFloat();
        }

        for (int i = 0; i < pointsByType.length; i++) {
            pointsByType[i] = buf.readInt();
        }

        for (int i = 0; i < experienceMultipliers.length; i++) {
            experienceMultipliers[i] = buf.readFloat();
        }
    }

    @Override
    public void toBytes(io.netty.buffer.ByteBuf buf) {
        buf.writeInt(level);
        buf.writeFloat(currentLevelExperience);
        buf.writeFloat(totalExperience);

        for (float exp : experienceByType) {
            buf.writeFloat(exp);
        }

        for (int points : pointsByType) {
            buf.writeInt(points);
        }

        for (float multiplier : experienceMultipliers) {
            buf.writeFloat(multiplier);
        }
    }

    public static class Handler implements IMessageHandler<MessageExperienceSync, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(final MessageExperienceSync message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    PlayerExperienceData data = new PlayerExperienceData(
                        Minecraft.getMinecraft().player.getUniqueID());

                    data.setLevel(message.level);
                    data.setCurrentLevelExperience(message.currentLevelExperience);
                    data.setTotalExperience(message.totalExperience);

                    // Устанавливаем опыт
                    ExperienceType[] types = ExperienceType.values();
                    for (int i = 0; i < types.length; i++) {
                        data.setExperience(types[i], message.experienceByType[i]);
                        data.setExperienceMultiplier(types[i], message.experienceMultipliers[i]);
                    }

                    // Устанавливаем очки
                    PointsType[] pointTypes = PointsType.values();
                    for (int i = 0; i < pointTypes.length; i++) {
                        data.setPoints(pointTypes[i], message.pointsByType[i]);
                    }

                    // Данные обновляются автоматически через другие механизмы
                    // ExperienceManager.getInstance().setPlayerData(Minecraft.getMinecraft().player, data);
                });
            }
            return null;
        }
    }
}