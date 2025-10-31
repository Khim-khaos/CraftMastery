package com.khimkhaosow.craftmastery.network.messages;

import java.util.ArrayList;
import java.util.List;

import com.khimkhaosow.craftmastery.CraftMastery;
import com.khimkhaosow.craftmastery.experience.ExperienceEffect;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageExperienceEffectSync implements IMessage, IMessageHandler<MessageExperienceEffectSync, IMessage> {
    
    private List<ExperienceEffect> effects;
    
    public MessageExperienceEffectSync() {
        this.effects = new ArrayList<>();
    }
    
    public MessageExperienceEffectSync(List<ExperienceEffect> effects) {
        this.effects = effects;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        effects = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float amount = buf.readFloat();
            boolean levelUp = buf.readBoolean();
            long creationTime = buf.readLong();
            
            ExperienceEffect effect = new ExperienceEffect(
                net.minecraft.client.Minecraft.getMinecraft().player,
                new Vec3d(x, y, z),
                amount,
                levelUp ? com.khimkhaosow.craftmastery.experience.ExperienceEffect.ExperienceSource.LEVEL_UP
                        : com.khimkhaosow.craftmastery.experience.ExperienceEffect.ExperienceSource.CRAFTING
            );
            effect.setCreationTime(creationTime);
            effects.add(effect);
        }
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(effects.size());
        
        for (ExperienceEffect effect : effects) {
            Vec3d pos = effect.getPosition();
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
            buf.writeFloat(effect.getAmount());
            buf.writeBoolean(effect.isLevelUp());
            buf.writeLong(effect.getCreationTime());
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageExperienceEffectSync message, MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ExperienceManager.getInstance().handleEffectSync(message.effects);
            });
        }
        return null;
    }
}