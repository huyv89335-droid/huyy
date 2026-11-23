package vn.autobuilder.builder;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import vn.autobuilder.util.BlockStateUtil;
import vn.autobuilder.util.ChatUtil;
import vn.autobuilder.util.Config;
import vn.autobuilder.util.InventoryUtil;
import vn.autobuilder.util.LookUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * May xay block: chay 1 lan moi tick (ClientTickEvents.END_CLIENT_TICK).
 *
 * Thu tu lam viec moi tick:
 *   1. Bo qua nhung block da co trong the gioi.
 *   2. Chon block GAN NHAT con thieu (uu tien tang cao truoc de khong bi chen).
 *   3. Neu thieu do: tim trong tui -> hoac di lay tu ruong.
 *   4. Xoay camera muot ve block, di toi gan (tu nhay khi vuong).
 *   5. Chon mat do hop le, dat block, nghi theo placeDelay.
 */
public final class BuilderEngine {

    private static BuilderEngine instance;

    public static BuilderEngine get() {
        if (instance == null) instance = new BuilderEngine();
        return instance;
    }

    /* ---------------- Trang thai ---------------- */
    private Blueprint blueprint = null;
    private String fileName = "";
    private BlockPos origin = BlockPos.ORIGIN;
    private BlockPos chestPos = null;
    private int rotation = 0;
    private boolean enabled = false;

    private final List<Task> tasks = new ArrayList<>();
    private int total = 0;
    private int done = 0;
    private int skipped = 0;

    private int cooldown = 0;
    private int warnCooldown = 0;
    private int hudCooldown = 0;
    private int chestStage = 0;        // 0 = ranh, 1 = dang di toi ruong, 2 = dang lay do
    private int chestCooldown = 0;
    private Item needItem = null;

    private final Config config = Config.get();

    private BuilderEngine() {
        rotation = config.rotation & 3;
        if (config.hasChest) {
            chestPos = new BlockPos(config.chestX, config.chestY, config.chestZ);
        }
        origin = new BlockPos(config.lastX, config.lastY, config.lastZ);
    }

    private static final class Task {
        final BlockPos pos;
        final BlockState state;
        int attempts = 0;

        Task(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }

    /* ================= API cho GUI ================= */

    public boolean hasBlueprint() { return blueprint != null; }

    public Blueprint getBlueprint() { return blueprint; }

    public String getFileName() { return fileName; }

    public BlockPos getOrigin() { return origin; }

    public BlockPos getChestPos() { return chestPos; }

    public int getRotation() { return rotation; }

    public int getPlaceDelay() { return config.placeDelay; }

    public boolean isEnabled() { return enabled; }

    public void setBlueprint(Blueprint bp, String name) {
        this.blueprint = bp;
        this.fileName = name == null ? "" : name;
        Config.get().lastFile = this.fileName;
        rebuildQueue();
    }

    public void setOrigin(BlockPos pos) {
        this.origin = pos;
        config.lastX = pos.getX();
        config.lastY = pos.getY();
        config.lastZ = pos.getZ();
        rebuildQueue();
    }

    public void setChestPos(BlockPos pos) {
        if (pos == null) {
            chestPos = null;
            config.hasChest = false;
        } else {
            chestPos = pos;
            config.hasChest = true;
            config.chestX = pos.getX();
            config.chestY = pos.getY();
            config.chestZ = pos.getZ();
        }
    }

    public void cycleRotation() {
        rotation = (rotation + 1) & 3;
        config.rotation = rotation;
        rebuildQueue();
    }

    public void cycleSpeed() {
        int[] options = {1, 2, 3, 5, 8, 12};
        int idx = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == config.placeDelay) { idx = i; break; }
        }
        config.placeDelay = options[(idx + 1) % options.length];
    }

    public boolean start() {
        if (blueprint == null || blueprint.placements.isEmpty()) {
            ChatUtil.error("Chua chon ban ve (.litematic / .schem)!");
            return false;
        }
        rebuildQueue();
        done = 0;
        skipped = 0;
        cooldown = 0;
        chestStage = 0;
        needItem = null;
        enabled = true;
        ChatUtil.say("BAT DAU xay: " + blueprint.describe()
                + " | goc " + origin.toShortString()
                + " | xoay " + (rotation * 90) + " do");
        config.save();
        return true;
    }

    public void stop(String reason) {
        if (!enabled && reason == null) return;
        enabled = false;
        chestStage = 0;
        needItem = null;
        config.save();
        if (reason != null) {
            ChatUtil.say(reason + " (dat duoc " + done + "/" + total + ")");
        }
    }

    public List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        lines.add(fileName.isEmpty() ? "(chua chon ban ve)" : fileName);
        if (blueprint != null) {
            lines.add("Kich thuoc: " + blueprint.sizeX + "x" + blueprint.sizeY + "x" + blueprint.sizeZ
                    + "  |  Block: " + blueprint.size());
        }
        lines.add("Goc: " + origin.toShortString() + "  |  Xoay: " + (rotation * 90) + " do"
                + "  |  toc do: " + config.placeDelay + "t");
        lines.add("Ruong: " + (chestPos != null ? chestPos.toShortString() : "khong dung"));
        if (enabled) {
            lines.add("Trang thai: BAT  |  Xong: " + done + "/" + total + "  (bo qua " + skipped + ")");
        } else {
            lines.add("Trang thai: TAT");
        }
        return lines;
    }

    /* ================= Toa do + xoay ================= */

    private void rebuildQueue() {
        tasks.clear();
        if (blueprint == null) return;
        double cx = blueprint.sizeX * 0.5D;
        double cz = blueprint.sizeZ * 0.5D;
        for (Placement p : blueprint.placements) {
            BlockState st = BlockStateUtil.rotate(p.state, rotation);
            if (st == null) continue;
            tasks.add(new Task(worldPos(p, cx, cz), st));
        }
        total = tasks.size();
        done = 0;
        skipped = 0;
    }

    /** Xoay toa do local quanh tam ban ve, roi cong voi goc da nhap. */
    private BlockPos worldPos(Placement p, double cx, double cz) {
        double lx = p.x - cx;
        double lz = p.z - cz;
        double rx;
        double rz;
        switch (rotation & 3) {
            case 1: rx = -lz; rz = lx; break;
            case 2: rx = -lx; rz = -lz; break;
            case 3: rx = lz; rz = -lx; break;
            default: rx = lx; rz = lz;
        }
        int wx = (int) Math.round(rx + cx);
        int wz = (int) Math.round(rz + cz);
        return new BlockPos(origin.getX() + wx, origin.getY() + p.y, origin.getZ() + wz);
    }

    /* ================= VONG LAP CHINH ================= */

    public void tick(MinecraftClient client) {
        if (client == null) return;
        GameOptions options = client.options;
        if (options != null && !enabled) return;

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;

        if (!enabled) return;
        if (player == null || world == null) { stop("Dung - khong o trong the gioi"); return; }
        if (client.currentScreen != null) { LookUtil.stopMovement(options); return; }

        if (cooldown > 0) cooldown--;
        if (warnCooldown > 0) warnCooldown--;
        if (chestCooldown > 0) chestCooldown--;
        if (hudCooldown > 0) hudCooldown--;

        /* 1) Bo qua cac block da xong (check toi da 128 block/tick de nhe) */
        int guard = 0;
        while (!tasks.isEmpty() && guard++ < 128) {
            Task head = tasks.get(0);
            BlockState now = world.getBlockState(head.pos);
            if (now.getBlock() == head.state.getBlock()) { tasks.remove(0); done++; continue; }
            if (!now.isAir() && !now.isLiquid()) { tasks.remove(0); done++; continue; }
            break;
        }

        if (tasks.isEmpty()) { stop("HOAN THANH ban ve!"); return; }

        /* 2) Dang di lay do o ruong */
        if (chestStage != 0) { handleChest(client, player, world, options); return; }

        /* 3) Chon block gan nhat con thieu */
        Vec3d eye = player.getEyePos();
        Task best = null;
        double bestCost = Double.MAX_VALUE;
        int scan = Math.min(tasks.size(), 400);
        int playerY = player.getBlockPos().getY();

        for (int i = 0; i < scan; i++) {
            Task t = tasks.get(i);
            if (t.attempts > 12) continue;
            Vec3d center = Vec3d.ofCenter(t.pos);
            double cost = center.distanceTo(eye);
            if (t.pos.getY() < playerY - 1) cost += 25.0D;
            if (cost < bestCost) { bestCost = cost; best = t; }
        }
        if (best == null) { stop("Khong dat duoc block nao nua (bi chan?)"); return; }

        /* 4) Chuan bi block can cam */
        BlockState want = best.state;
        Item item = BlockStateUtil.itemOf(want);
        if (item == Items.AIR) {
            best.attempts = 99;
            skipped++;
            cooldown = 2;
            return;
        }

        int slot = InventoryUtil.findInHotbar(player, item);
        if (slot < 0) InventoryUtil.moveToHotbar(player, item, InventoryUtil.currentSlot(player));
        slot = InventoryUtil.findInHotbar(player, item);

        if (slot < 0) {
            if (player.isCreative()) {
                InventoryUtil.giveCreative(player, item);
                slot = InventoryUtil.findInHotbar(player, item);
            } else if (chestPos != null) {
                needItem = item;
                chestStage = 1;
                LookUtil.stopMovement(options);
                return;
            } else {
                if (warnCooldown <= 0) {
                    ChatUtil.error("Thieu block: " + item.getName().getString()
                            + " (hoac khai bao toa do ruong trong menu)");
                    warnCooldown = 120;
                }
                LookUtil.stopMovement(options);
                cooldown = 20;
                return;
            }
        }
        InventoryUtil.setHotbarSlot(player, slot);

        /* 5) Xoay camera + di chuyen */
        Vec3d target = Vec3d.ofCenter(best.pos);
        double dist = target.distanceTo(eye);

        if (dist > config.reach - 0.3D) {
            walkTowards(player, world, options, best.pos);
            hud("AutoBuilder " + percent() + "%  -  di chuyen toi " + item.getName().getString());
            return;
        }

        options.forwardKey.setPressed(false);
        options.sprintKey.setPressed(false);
        if (config.sneakPlace) options.sneakKey.setPressed(true);
        lookAtBestPoint(player, world, best);
        hud("AutoBuilder " + percent() + "%  -  dat: " + item.getName().getString());

        if (cooldown > 0) return;

        /* 6) Dat block */
        Direction face = pickSupportFace(player, world, best.pos);
        if (face == null) {
            best.attempts++;
            if (best.attempts > 12) { skipped++; tasks.remove(best); }
            return;
        }

        BlockPos support = best.pos.offset(face);
        Direction side = face.getOpposite();
        Vec3d normal = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        Vec3d hitVec = Vec3d.ofCenter(support).add(normal.multiply(0.5D));

        BlockHitResult hit = new BlockHitResult(hitVec, side, support, false);
        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        player.swingHand(Hand.MAIN_HAND);

        best.attempts++;
        cooldown = Math.max(1, config.placeDelay);
    }

    private String percent() {
        if (total <= 0) return "0";
        int pct = (int) Math.round(done * 100.0D / total);
        return String.valueOf(Math.min(100, Math.max(0, pct)));
    }

    private void hud(String text) {
        if (hudCooldown <= 0) {
            ChatUtil.hud(text);
            hudCooldown = 10;
        }
    }

    /* ================= Di chuyen ================= */

    private void walkTowards(ClientPlayerEntity player, ClientWorld world, GameOptions options, BlockPos target) {
        Vec3d feet = player.getPos();
        double dx = (target.getX() + 0.5D) - feet.x;
        double dz = (target.getZ() + 0.5D) - feet.z;
        double dy = target.getY() - feet.y;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        LookUtil.lookSmoothAt(player, Vec3d.ofCenter(target), config.lookSpeed);

        if (horizontal > 1.2D) {
            options.forwardKey.setPressed(true);
            if (config.autoSprint) options.sprintKey.setPressed(true);
            if (config.autoJump && player.isOnGround()) {
                options.jumpKey.setPressed(dy >= 1.0D || obstacleAhead(player, world, dx, dz));
            }
        } else {
            options.forwardKey.setPressed(false);
            options.sprintKey.setPressed(false);
            if (dy >= 1.5D && player.isOnGround()) options.jumpKey.setPressed(true);
        }
    }

    private boolean obstacleAhead(ClientPlayerEntity player, ClientWorld world, double dx, double dz) {
        int sx = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int sz = dz > 0 ? 1 : (dz < 0 ? -1 : 0);
        BlockPos front = player.getBlockPos().add(sx, 0, sz);
        BlockState at = world.getBlockState(front);
        BlockState above = world.getBlockState(front.up());
        return !at.isAir() && at.isSolidBlock(world, front) && above.isAir();
    }

    /** Nhin vao mat do se dat (chinh xac hon la nhin vao tam block). */
    private void lookAtBestPoint(ClientPlayerEntity player, ClientWorld world, Task task) {
        Direction face = pickSupportFace(player, world, task.pos);
        Vec3d point = face == null
                ? Vec3d.ofCenter(task.pos)
                : Vec3d.ofCenter(task.pos.offset(face));
        LookUtil.lookSmoothAt(player, point, config.lookSpeed);
    }

    private Direction pickSupportFace(ClientPlayerEntity player, ClientWorld world, BlockPos target) {
        Vec3d eye = player.getEyePos();
        Direction best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            BlockPos neighbor = target.offset(d);
            BlockState s = world.getBlockState(neighbor);
            if (s.isAir() || s.isLiquid()) continue;
            double dist = Vec3d.ofCenter(neighbor).squaredDistanceTo(eye);
            if (dist < bestDist) { bestDist = dist; best = d; }
        }
        return best;
    }

    /* ================= Lay do tu ruong ================= */

    private void handleChest(MinecraftClient client, ClientPlayerEntity player, ClientWorld world, GameOptions options) {
        if (chestPos == null) { chestStage = 0; return; }
        Vec3d center = Vec3d.ofCenter(chestPos);

        /* Dang mo ruong -> quick move block can thiet ve tui */
        if (client.currentScreen != null) {
            chestStage = 2;
            ScreenHandler handler = player.currentScreenHandler;
            if (handler == null) { client.setScreen(null); chestStage = 0; return; }

            int chestSlots = handler.slots.size() - 36;
            if (chestSlots <= 0) { player.closeHandledScreen(); chestStage = 0; chestCooldown = 20; return; }

            boolean moved = false;
            for (int i = 0; i < chestSlots; i++) {
                Slot slot = handler.slots.get(i);
                if (slot == null) continue;
                if (!slot.getStack().isEmpty() && needItem != null && slot.getStack().isOf(needItem)) {
                    client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                player.closeHandledScreen();
                chestStage = 0;
                chestCooldown = 30;
                if (warnCooldown <= 0 && needItem != null) {
                    ChatUtil.error("Ruog khong con: " + needItem.getName().getString());
                    warnCooldown = 200;
                }
            }
            return;
        }

        /* Di toi gan ruong roi mo no */
        double dist = center.distanceTo(player.getEyePos());
        if (dist > 3.4D) {
            options.sneakKey.setPressed(false);
            LookUtil.lookSmoothAt(player, center, config.lookSpeed);
            walkTowards(player, world, options, chestPos);
            return;
        }

        options.forwardKey.setPressed(false);
        options.sprintKey.setPressed(false);
        options.jumpKey.setPressed(false);
        LookUtil.lookSmoothAt(player, center, config.lookSpeed);

        if (chestCooldown > 0) return;
        Direction face = BlockStateUtil.horizontalTowards(
                player.getX() - chestPos.getX(), player.getZ() - chestPos.getZ()).getOpposite();
        Vec3d normal = new Vec3d(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ());
        Vec3d hitVec = center.add(normal.multiply(0.5D));

        client.interactionManager.interactBlock(player, Hand.MAIN_HAND,
                new BlockHitResult(hitVec, face, chestPos, false));
        player.swingHand(Hand.MAIN_HAND);
        chestCooldown = Math.max(6, config.chestRestockDelay);
    }
}
