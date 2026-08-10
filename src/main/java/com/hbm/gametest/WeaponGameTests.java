package com.hbm.gametest;

import com.hbm.HbmNuclearTech;
import com.hbm.weapon.ballistics.BallisticProjectileEntity;
import com.hbm.weapon.ballistics.BallisticsService;
import com.hbm.weapon.data.AmmoDefinition;
import com.hbm.weapon.data.GunDefinition;
import com.hbm.weapon.data.GunDefinitionRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(HbmNuclearTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WeaponGameTests {
    private static final String PLATFORM = "weapon_platform";

    @GameTest(template = PLATFORM, timeoutTicks = 20)
    public static void conventionalRoundDamagesLivingTarget(GameTestHelper helper) {
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0D, 2.0D, 8.0D));
        Vec3 origin = shooter.getEyePosition();
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.0D, 2.0D, 8.0D));
        float healthBefore = target.getHealth();

        GunDefinition gun = gun("gun_star_f");
        AmmoDefinition ammo = ammo("p22_fmj");
        Vec3 direction = target.getBoundingBox().getCenter().subtract(origin).normalize();
        BallisticsService.fire(shooter, gun, ammo, origin, direction, 0.0D);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(target.getHealth() < healthBefore,
                    "A conventional server trajectory did not damage its target");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void slugReplacesBuckshotPellets(GameTestHelper helper) {
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0D, 2.0D, 8.0D));
        GunDefinition gun = gun("gun_spas12");
        AmmoDefinition buckshot = ammo("g12_buckshot");
        AmmoDefinition slug = ammo("g12_slug");
        Vec3 origin = helper.absoluteVec(new Vec3(3.0D, 3.0D, 8.0D));

        BallisticsService.fire(shooter, gun, buckshot, origin, new Vec3(1.0D, 0.0D, 0.0D), 0.0D);
        helper.assertValueEqual(BallisticsService.activeRoundCount(helper.getLevel(), shooter.getUUID()),
                buckshot.getPelletCount(),
                "Buckshot did not create its data-defined pellet count");

        BallisticsService.fire(shooter, gun, slug, origin, new Vec3(1.0D, 0.0D, 0.0D), 0.0D);
        helper.assertValueEqual(BallisticsService.activeRoundCount(helper.getLevel(), shooter.getUUID()),
                buckshot.getPelletCount() + 1,
                "A shotgun slug must add exactly one trajectory");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 20)
    public static void congoLakeUsesVisibleGravityProjectile(GameTestHelper helper) {
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0D, 2.0D, 8.0D));
        GunDefinition gun = gun("gun_congolake");
        AmmoDefinition ammo = ammo("g40_he");
        Vec3 origin = helper.absoluteVec(new Vec3(3.0D, 5.0D, 8.0D));
        BallisticProjectileEntity projectile = BallisticProjectileEntity.launch(
                shooter, gun, ammo, origin, new Vec3(1.0D, 0.0D, 0.0D));
        double startingY = projectile.getY();

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(projectile.isAlive(), "The 40mm projectile disappeared before its arc was observable");
            helper.assertTrue(projectile.getY() < startingY,
                    "The 40mm projectile did not follow its data-defined gravity arc");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 20)
    public static void heAndHeatGrenadesDetonateOnEntityImpact(GameTestHelper helper) {
        Zombie heShooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(2.0D, 2.0D, 4.0D));
        Zombie heTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(6.0D, 2.0D, 4.0D));
        Zombie heatShooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(2.0D, 2.0D, 12.0D));
        Zombie heatTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(6.0D, 2.0D, 12.0D));
        float heHealth = heTarget.getHealth();
        float heatHealth = heatTarget.getHealth();
        GunDefinition gun = gun("gun_congolake");

        BallisticProjectileEntity he = launchAt(heShooter, heTarget, gun, ammo("g40_he"));
        BallisticProjectileEntity heat = launchAt(heatShooter, heatTarget, gun, ammo("g40_heat"));

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(he.isRemoved(), "The 40mm HE projectile did not detonate on entity impact");
            helper.assertTrue(heat.isRemoved(), "The 40mm HEAT projectile did not detonate on entity impact");
            helper.assertTrue(!heTarget.isAlive() || heTarget.getHealth() < heHealth,
                    "The 40mm HE impact did not damage its target");
            helper.assertTrue(!heatTarget.isAlive() || heatTarget.getHealth() < heatHealth,
                    "The 40mm HEAT impact did not damage its target");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 20)
    public static void upperBodyHitAppliesHeadshotMultiplier(GameTestHelper helper) {
        Zombie bodyShooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0D, 2.0D, 4.0D));
        Zombie bodyTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.0D, 2.0D, 4.0D));
        Zombie headShooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0D, 2.0D, 12.0D));
        Zombie headTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.0D, 2.0D, 12.0D));
        float initialHealth = bodyTarget.getHealth();
        GunDefinition gun = gun("gun_star_f");
        AmmoDefinition ammo = ammo("p22_fmj");

        fireAtHeight(bodyShooter, bodyTarget, gun, ammo, 0.45D);
        fireAtHeight(headShooter, headTarget, gun, ammo, 0.86D);

        helper.runAfterDelay(2, () -> {
            float bodyDamage = initialHealth - bodyTarget.getHealth();
            float headDamage = initialHealth - headTarget.getHealth();
            helper.assertTrue(bodyDamage > 0.0F, "The body-shot control did not connect");
            helper.assertTrue(headDamage > bodyDamage,
                    "An upper-body hit did not apply the data-defined headshot multiplier");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 20)
    public static void armorPiercingRoundReducesLiveArmorMitigation(GameTestHelper helper) {
        Zombie fmjShooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0D, 2.0D, 4.0D));
        Zombie fmjTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.0D, 2.0D, 4.0D));
        Zombie apShooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.0D, 2.0D, 12.0D));
        Zombie apTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.0D, 2.0D, 12.0D));
        equipDiamondArmor(fmjTarget);
        equipDiamondArmor(apTarget);
        float initialHealth = fmjTarget.getHealth();
        GunDefinition gun = gun("gun_stg77");

        fireAtHeight(fmjShooter, fmjTarget, gun, ammo("r556_fmj"), 0.45D);
        fireAtHeight(apShooter, apTarget, gun, ammo("r556_ap"), 0.45D);

        helper.runAfterDelay(2, () -> {
            float fmjDamage = initialHealth - fmjTarget.getHealth();
            float apDamage = initialHealth - apTarget.getHealth();
            helper.assertTrue(fmjDamage > 0.0F, "The armored FMJ control did not connect");
            helper.assertTrue(apDamage > fmjDamage,
                    "The AP profile did not reduce live armor mitigation despite its lower raw damage");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 240)
    public static void simultaneousRifleAndShotgunLoadStaysWithinTickBudget(GameTestHelper helper) {
        List<Zombie> rifleShooters = spawnLoadShooters(helper, 16);
        List<Zombie> shotgunShooters = spawnLoadShooters(helper, 8);
        GunDefinition rifle = gun("gun_stg77");
        AmmoDefinition rifleAmmo = ammo("r556_fmj");
        GunDefinition shotgun = gun("gun_spas12");
        AmmoDefinition buckshot = ammo("g12_buckshot");
        Vec3 direction = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 origin = helper.absoluteVec(new Vec3(8.0D, 5.0D, 8.0D));

        helper.runAtTickTime(20, () -> BallisticsService.resetMetrics(helper.getLevel()));
        helper.onEachTick(() -> {
            long loadTick = helper.getTick() - 20L;
            if (loadTick < 0L || loadTick >= 200L) {
                return;
            }
            if (crossedShotBoundary(loadTick, rifle.getRoundsPerMinute())) {
                rifleShooters.forEach(shooter ->
                        BallisticsService.fire(shooter, rifle, rifleAmmo, origin, direction, 0.0D));
            }
            if (crossedShotBoundary(loadTick, shotgun.getRoundsPerMinute())) {
                shotgunShooters.forEach(shooter ->
                        BallisticsService.fire(shooter, shotgun, buckshot, origin, direction, 0.0D));
            }
        });

        helper.runAtTickTime(225, () -> {
            double averageMillis = BallisticsService.averageTickMillis(helper.getLevel());
            long measuredTicks = BallisticsService.measuredTickCount(helper.getLevel());
            HbmNuclearTech.LOGGER.info(
                    "Weapon load profile: 16 StG 77 plus 8 SPAS-12 users averaged {} ms across {} ballistics ticks.",
                    averageMillis, measuredTicks);
            helper.assertTrue(measuredTicks >= 200L,
                    "The ballistics profiler did not capture the full simultaneous-user window");
            helper.assertTrue(averageMillis <= 5.0D,
                    "The simultaneous 16-rifle/8-shotgun ballistics average exceeded 5 ms: "
                            + averageMillis + " ms");
            helper.succeed();
        });
    }

    private static void fireAtHeight(Zombie shooter, Zombie target, GunDefinition gun,
                                     AmmoDefinition ammo, double heightFraction) {
        Vec3 origin = shooter.getEyePosition();
        Vec3 targetPoint = new Vec3(
                target.getX(),
                target.getBoundingBox().minY + target.getBbHeight() * heightFraction,
                target.getZ());
        BallisticsService.fire(shooter, gun, ammo, origin, targetPoint.subtract(origin).normalize(), 0.0D);
    }

    private static BallisticProjectileEntity launchAt(Zombie shooter, Zombie target, GunDefinition gun,
                                                       AmmoDefinition ammo) {
        Vec3 origin = shooter.getEyePosition();
        Vec3 direction = target.getBoundingBox().getCenter().subtract(origin).normalize();
        return BallisticProjectileEntity.launch(shooter, gun, ammo, origin, direction);
    }

    private static List<Zombie> spawnLoadShooters(GameTestHelper helper, int count) {
        List<Zombie> shooters = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double x = 2.0D + index % 6 * 2.0D;
            double z = 2.0D + index / 6 * 2.0D;
            shooters.add(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(x, 2.0D, z)));
        }
        return shooters;
    }

    private static boolean crossedShotBoundary(long tick, int rpm) {
        long previous = tick <= 0L ? 0L : (tick - 1L) * rpm / 1200L;
        long current = tick * rpm / 1200L;
        return current > previous;
    }

    private static void equipDiamondArmor(Zombie target) {
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        target.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        target.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
    }

    private static GunDefinition gun(String path) {
        return GunDefinitionRegistry.requireGun(id(path));
    }

    private static AmmoDefinition ammo(String path) {
        AmmoDefinition definition = GunDefinitionRegistry.ammo(id(path));
        if (definition == null) {
            throw new IllegalStateException("Missing built-in test ammunition hbm:" + path);
        }
        return definition;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, path);
    }

    private WeaponGameTests() {
    }
}
