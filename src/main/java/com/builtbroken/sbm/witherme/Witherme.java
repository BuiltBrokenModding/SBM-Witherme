package com.builtbroken.sbm.witherme;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = Witherme.DOMAIN, acceptableRemoteVersions = "*")
@Mod.EventBusSubscriber()
public class Witherme
{
    public static final String DOMAIN = "sbmdualwither";
    public static final String PREFIX = DOMAIN + ":";

    @SubscribeEvent
    public static void onEntitySpawned(LivingDeathEvent event)
    {
        final Entity entity = event.getEntity();
        if (event.getEntity() instanceof EntityWither)
        {
            final World world = entity.getEntityWorld();
            if (!world.isRemote)
            {
                for (int i = 0; i < 3; i++)
                {
                    final EntityWither wither = new EntityWither(world);
                    wither.setPosition(entity.posX, entity.posY, entity.posZ);
                    world.spawnEntity(wither);
                }
            }
        }
    }
}
