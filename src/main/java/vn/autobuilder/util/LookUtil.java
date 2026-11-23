package vn.autobuilder.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Xoay camera MOT NUOC moi tick (gioi han so do) nen canh luon muot,
 * khong bi giat nhu khi setYaw truc tiep.
 */
public final class LookUtil {

    private LookUtil() {
    }

    public static void lookSmoothAt(ClientPlayerEntity player, Vec3d target, float maxStepDegrees) {
        float[] a = BlockStateUtil.anglesTo(player.getEyePos(), target);
        float step = Math.max(2.0F, maxStepDegrees);

        float dy = MathHelper.wrapDegrees(a[0] - player.getYaw());
        float dp = MathHelper.wrapDegrees(a[1] - player.getPitch());

        player.setYaw(player.getYaw() + MathHelper.clamp(dy, -step, step));
        player.setPitch(player.getPitch() + MathHelper.clamp(dp, -step, step));
    }

    public static void snapLook(ClientPlayerEntity player, Vec3d target) {
        float[] a = BlockStateUtil.anglesTo(player.getEyePos(), target);
        player.setYaw(a[0]);
        player.setPitch(a[1]);
    }

    /** Tha tat ca phim di chuyen (khong duoc de phim bam khi mo menu). */
    public static void stopMovement(GameOptions options) {
        options.forwardKey.setPressed(false);
        options.backKey.setPressed(false);
        options.leftKey.setPressed(false);
        options.rightKey.setPressed(false);
        options.jumpKey.setPressed(false);
        options.sneakKey.setPressed(false);
        options.sprintKey.setPressed(false);
    }
}
