package com.builtbroken.sbm.witherme.entity;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class EntityMiniWither extends EntityMob implements IRangedAttackMob
{
    private static final DataParameter<Integer> FIRST_HEAD_TARGET = EntityDataManager.<Integer>createKey(EntityWither.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> SECOND_HEAD_TARGET = EntityDataManager.<Integer>createKey(EntityWither.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> THIRD_HEAD_TARGET = EntityDataManager.<Integer>createKey(EntityWither.class, DataSerializers.VARINT);
    private static final DataParameter<Integer>[] HEAD_TARGETS = new DataParameter[]{FIRST_HEAD_TARGET, SECOND_HEAD_TARGET, THIRD_HEAD_TARGET};
    private final float[] xRotationHeads = new float[2];
    private final float[] yRotationHeads = new float[2];
    private final float[] xRotOHeads = new float[2];
    private final float[] yRotOHeads = new float[2];
    private final int[] nextHeadUpdate = new int[2];
    private final int[] idleHeadUpdates = new int[2];
    /** Time before the Wither tries to break blocks */
    private int blockBreakCounter;

    /** Selector used to determine the entities a wither boss should attack. */
    private static final Predicate<Entity> NOT_UNDEAD = new Predicate<Entity>()
    {
        public boolean apply(@Nullable Entity p_apply_1_)
        {
            return p_apply_1_ instanceof EntityLivingBase && ((EntityLivingBase) p_apply_1_).getCreatureAttribute() != EnumCreatureAttribute.UNDEAD && ((EntityLivingBase) p_apply_1_).attackable();
        }
    };

    public EntityMiniWither(World worldIn)
    {
        super(worldIn);
        this.setHealth(this.getMaxHealth());
        this.setSize(0.9F, 3.5F);
        this.isImmuneToFire = true;
        ((PathNavigateGround) this.getNavigator()).setCanSwim(true);
        this.experienceValue = 30;
    }

    @Override
    protected void initEntityAI()
    {
        this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIAttackRanged(this, 1.0D, 40, 20.0F));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 1.0D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, new Class[0]));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityLiving.class, 0, false, false, NOT_UNDEAD));
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(FIRST_HEAD_TARGET, Integer.valueOf(0));
        this.dataManager.register(SECOND_HEAD_TARGET, Integer.valueOf(0));
        this.dataManager.register(THIRD_HEAD_TARGET, Integer.valueOf(0));
    }

    @Override
    public void setCustomNameTag(String name)
    {
        super.setCustomNameTag(name);
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return SoundEvents.ENTITY_WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }

    @Override
    public void onLivingUpdate()
    {
        this.motionY *= 0.6000000238418579D;

        if (!this.world.isRemote && this.getWatchedTargetId(0) > 0)
        {
            Entity entity = this.world.getEntityByID(this.getWatchedTargetId(0));

            if (entity != null)
            {
                if (this.posY < entity.posY || !this.isArmored() && this.posY < entity.posY + 5.0D)
                {
                    if (this.motionY < 0.0D)
                    {
                        this.motionY = 0.0D;
                    }

                    this.motionY += (0.5D - this.motionY) * 0.6000000238418579D;
                }

                double d0 = entity.posX - this.posX;
                double d1 = entity.posZ - this.posZ;
                double d3 = d0 * d0 + d1 * d1;

                if (d3 > 9.0D)
                {
                    double d5 = (double) MathHelper.sqrt(d3);
                    this.motionX += (d0 / d5 * 0.5D - this.motionX) * 0.6000000238418579D;
                    this.motionZ += (d1 / d5 * 0.5D - this.motionZ) * 0.6000000238418579D;
                }
            }
        }

        if (this.motionX * this.motionX + this.motionZ * this.motionZ > 0.05000000074505806D)
        {
            this.rotationYaw = (float) MathHelper.atan2(this.motionZ, this.motionX) * (180F / (float) Math.PI) - 90.0F;
        }

        super.onLivingUpdate();

        for (int i = 0; i < 2; ++i)
        {
            this.yRotOHeads[i] = this.yRotationHeads[i];
            this.xRotOHeads[i] = this.xRotationHeads[i];
        }

        for (int j = 0; j < 2; ++j)
        {
            int k = this.getWatchedTargetId(j + 1);
            Entity entity1 = null;

            if (k > 0)
            {
                entity1 = this.world.getEntityByID(k);
            }

            if (entity1 != null)
            {
                double d11 = this.getHeadX(j + 1);
                double d12 = this.getHeadY(j + 1);
                double d13 = this.getHeadZ(j + 1);
                double d6 = entity1.posX - d11;
                double d7 = entity1.posY + (double) entity1.getEyeHeight() - d12;
                double d8 = entity1.posZ - d13;
                double d9 = (double) MathHelper.sqrt(d6 * d6 + d8 * d8);
                float f = (float) (MathHelper.atan2(d8, d6) * (180D / Math.PI)) - 90.0F;
                float f1 = (float) (-(MathHelper.atan2(d7, d9) * (180D / Math.PI)));
                this.xRotationHeads[j] = this.rotlerp(this.xRotationHeads[j], f1, 40.0F);
                this.yRotationHeads[j] = this.rotlerp(this.yRotationHeads[j], f, 10.0F);
            }
            else
            {
                this.yRotationHeads[j] = this.rotlerp(this.yRotationHeads[j], this.renderYawOffset, 10.0F);
            }
        }

        boolean flag = this.isArmored();

        for (int l = 0; l < 3; ++l)
        {
            double d10 = this.getHeadX(l);
            double d2 = this.getHeadY(l);
            double d4 = this.getHeadZ(l);
            this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d10 + this.rand.nextGaussian() * 0.30000001192092896D, d2 + this.rand.nextGaussian() * 0.30000001192092896D, d4 + this.rand.nextGaussian() * 0.30000001192092896D, 0.0D, 0.0D, 0.0D);

            if (flag && this.world.rand.nextInt(4) == 0)
            {
                this.world.spawnParticle(EnumParticleTypes.SPELL_MOB, d10 + this.rand.nextGaussian() * 0.30000001192092896D, d2 + this.rand.nextGaussian() * 0.30000001192092896D, d4 + this.rand.nextGaussian() * 0.30000001192092896D, 0.699999988079071D, 0.699999988079071D, 0.5D);
            }
        }
    }

    @Override
    protected void updateAITasks()
    {
        super.updateAITasks();

        for (int i = 1; i < 3; ++i)
        {
            if (this.ticksExisted >= this.nextHeadUpdate[i - 1])
            {
                this.nextHeadUpdate[i - 1] = this.ticksExisted + 10 + this.rand.nextInt(10);

                if (this.world.getDifficulty() == EnumDifficulty.NORMAL || this.world.getDifficulty() == EnumDifficulty.HARD)
                {
                    int j3 = i - 1;
                    int k3 = this.idleHeadUpdates[i - 1];
                    this.idleHeadUpdates[j3] = this.idleHeadUpdates[i - 1] + 1;

                    if (k3 > 15)
                    {
                        float f = 10.0F;
                        float f1 = 5.0F;
                        double d0 = MathHelper.nextDouble(this.rand, this.posX - 10.0D, this.posX + 10.0D);
                        double d1 = MathHelper.nextDouble(this.rand, this.posY - 5.0D, this.posY + 5.0D);
                        double d2 = MathHelper.nextDouble(this.rand, this.posZ - 10.0D, this.posZ + 10.0D);
                        this.launchWitherSkullToCoords(i + 1, d0, d1, d2, true);
                        this.idleHeadUpdates[i - 1] = 0;
                    }
                }

                int k1 = this.getWatchedTargetId(i);

                if (k1 > 0)
                {
                    Entity entity = this.world.getEntityByID(k1);

                    if (entity != null && entity.isEntityAlive() && this.getDistanceSq(entity) <= 900.0D && this.canEntityBeSeen(entity))
                    {
                        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage)
                        {
                            this.updateWatchedTargetId(i, 0);
                        }
                        else
                        {
                            this.launchWitherSkullToEntity(i + 1, (EntityLivingBase) entity);
                            this.nextHeadUpdate[i - 1] = this.ticksExisted + 40 + this.rand.nextInt(20);
                            this.idleHeadUpdates[i - 1] = 0;
                        }
                    }
                    else
                    {
                        this.updateWatchedTargetId(i, 0);
                    }
                }
                else
                {
                    List<EntityLivingBase> list = this.world.<EntityLivingBase>getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(20.0D, 8.0D, 20.0D), Predicates.and(NOT_UNDEAD, EntitySelectors.NOT_SPECTATING));

                    for (int j2 = 0; j2 < 10 && !list.isEmpty(); ++j2)
                    {
                        EntityLivingBase entitylivingbase = list.get(this.rand.nextInt(list.size()));

                        if (entitylivingbase != this && entitylivingbase.isEntityAlive() && this.canEntityBeSeen(entitylivingbase))
                        {
                            if (entitylivingbase instanceof EntityPlayer)
                            {
                                if (!((EntityPlayer) entitylivingbase).capabilities.disableDamage)
                                {
                                    this.updateWatchedTargetId(i, entitylivingbase.getEntityId());
                                }
                            }
                            else
                            {
                                this.updateWatchedTargetId(i, entitylivingbase.getEntityId());
                            }

                            break;
                        }

                        list.remove(entitylivingbase);
                    }
                }
            }
        }

        if (this.getAttackTarget() != null)
        {
            this.updateWatchedTargetId(0, this.getAttackTarget().getEntityId());
        }
        else
        {
            this.updateWatchedTargetId(0, 0);
        }

        if (this.blockBreakCounter > 0)
        {
            --this.blockBreakCounter;

            if (this.blockBreakCounter == 0 && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.world, this))
            {
                int i1 = MathHelper.floor(this.posY);
                int l1 = MathHelper.floor(this.posX);
                int i2 = MathHelper.floor(this.posZ);
                boolean flag = false;

                for (int k2 = -1; k2 <= 1; ++k2)
                {
                    for (int l2 = -1; l2 <= 1; ++l2)
                    {
                        for (int j = 0; j <= 3; ++j)
                        {
                            int i3 = l1 + k2;
                            int k = i1 + j;
                            int l = i2 + l2;
                            BlockPos blockpos = new BlockPos(i3, k, l);
                            IBlockState iblockstate = this.world.getBlockState(blockpos);
                            Block block = iblockstate.getBlock();

                            if (!block.isAir(iblockstate, this.world, blockpos) && block.canEntityDestroy(iblockstate, world, blockpos, this) && net.minecraftforge.event.ForgeEventFactory.onEntityDestroyBlock(this, blockpos, iblockstate))
                            {
                                flag = this.world.destroyBlock(blockpos, true) || flag;
                            }
                        }
                    }
                }

                if (flag)
                {
                    this.world.playEvent((EntityPlayer) null, 1022, new BlockPos(this), 0);
                }
            }
        }

        if (this.ticksExisted % 20 == 0)
        {
            this.heal(1.0F);
        }
    }

    @Override
    public void setInWeb()
    {
    }

    private double getHeadX(int p_82214_1_)
    {
        if (p_82214_1_ <= 0)
        {
            return this.posX;
        }
        else
        {
            float f = (this.renderYawOffset + (float) (180 * (p_82214_1_ - 1))) * 0.017453292F;
            float f1 = MathHelper.cos(f);
            return this.posX + (double) f1 * 1.3D;
        }
    }

    private double getHeadY(int p_82208_1_)
    {
        return p_82208_1_ <= 0 ? this.posY + 3.0D : this.posY + 2.2D;
    }

    private double getHeadZ(int p_82213_1_)
    {
        if (p_82213_1_ <= 0)
        {
            return this.posZ;
        }
        else
        {
            float f = (this.renderYawOffset + (float) (180 * (p_82213_1_ - 1))) * 0.017453292F;
            float f1 = MathHelper.sin(f);
            return this.posZ + (double) f1 * 1.3D;
        }
    }

    private float rotlerp(float p_82204_1_, float p_82204_2_, float p_82204_3_)
    {
        float f = MathHelper.wrapDegrees(p_82204_2_ - p_82204_1_);

        if (f > p_82204_3_)
        {
            f = p_82204_3_;
        }

        if (f < -p_82204_3_)
        {
            f = -p_82204_3_;
        }

        return p_82204_1_ + f;
    }

    private void launchWitherSkullToEntity(int p_82216_1_, EntityLivingBase p_82216_2_)
    {
        this.launchWitherSkullToCoords(p_82216_1_, p_82216_2_.posX, p_82216_2_.posY + (double) p_82216_2_.getEyeHeight() * 0.5D, p_82216_2_.posZ, p_82216_1_ == 0 && this.rand.nextFloat() < 0.001F);
    }

    /**
     * Launches a Wither skull toward (par2, par4, par6)
     */
    private void launchWitherSkullToCoords(int p_82209_1_, double x, double y, double z, boolean invulnerable)
    {
        this.world.playEvent((EntityPlayer) null, 1024, new BlockPos(this), 0);
        double d0 = this.getHeadX(p_82209_1_);
        double d1 = this.getHeadY(p_82209_1_);
        double d2 = this.getHeadZ(p_82209_1_);
        double d3 = x - d0;
        double d4 = y - d1;
        double d5 = z - d2;
        EntityWitherSkull entitywitherskull = new EntityWitherSkull(this.world, this, d3, d4, d5);

        if (invulnerable)
        {
            entitywitherskull.setInvulnerable(true);
        }

        entitywitherskull.posY = d1;
        entitywitherskull.posX = d0;
        entitywitherskull.posZ = d2;
        this.world.spawnEntity(entitywitherskull);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor)
    {
        this.launchWitherSkullToEntity(0, target);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        if (this.isEntityInvulnerable(source))
        {
            return false;
        }
        else if (source != DamageSource.DROWN && !(source.getTrueSource() instanceof EntityWither))
        {
            if (this.isArmored())
            {
                Entity entity = source.getImmediateSource();

                if (entity instanceof EntityArrow)
                {
                    return false;
                }
            }

            Entity entity1 = source.getTrueSource();

            if (entity1 != null && !(entity1 instanceof EntityPlayer) && entity1 instanceof EntityLivingBase && ((EntityLivingBase) entity1).getCreatureAttribute() == this.getCreatureAttribute())
            {
                return false;
            }
            else
            {
                if (this.blockBreakCounter <= 0)
                {
                    this.blockBreakCounter = 20;
                }

                for (int i = 0; i < this.idleHeadUpdates.length; ++i)
                {
                    this.idleHeadUpdates[i] += 3;
                }

                return super.attackEntityFrom(source, amount);
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier)
    {
        EntityItem entityitem = this.dropItem(Items.NETHER_STAR, 1);

        if (entityitem != null)
        {
            entityitem.setNoDespawn();
        }
    }

    @Override
    protected void despawnEntity()
    {
        this.idleTime = 0;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender()
    {
        return 15728880;
    }

    @Override
    public void fall(float distance, float damageMultiplier)
    {
    }

    @Override
    public void addPotionEffect(PotionEffect potioneffectIn)
    {
    }

    @Override
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.7D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(3.0D);
    }

    @SideOnly(Side.CLIENT)
    public float getHeadYRotation(int p_82207_1_)
    {
        return this.yRotationHeads[p_82207_1_];
    }

    @SideOnly(Side.CLIENT)
    public float getHeadXRotation(int p_82210_1_)
    {
        return this.xRotationHeads[p_82210_1_];
    }

    /**
     * Returns the target entity ID if present, or -1 if not @param par1 The target offset, should be from 0-2
     */
    public int getWatchedTargetId(int head)
    {
        return ((Integer) this.dataManager.get(HEAD_TARGETS[head])).intValue();
    }

    /**
     * Updates the target entity ID
     */
    public void updateWatchedTargetId(int targetOffset, int newId)
    {
        this.dataManager.set(HEAD_TARGETS[targetOffset], Integer.valueOf(newId));
    }

    /**
     * Returns whether the wither is armored with its boss armor or not by checking whether its health is below half of
     * its maximum.
     */
    public boolean isArmored()
    {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute()
    {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected boolean canBeRidden(Entity entityIn)
    {
        return false;
    }

    @Override
    public void setSwingingArms(boolean swingingArms)
    {
    }
}