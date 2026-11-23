package vn.autobuilder.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Tim block trong tui do, dua len hotbar, chon slot.
 */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    /** Slot hotbar dang cam (0..8). */
    public static int currentSlot(ClientPlayerEntity player) {
        // Yarn 1.21.11: selectedSlot la private, phai dung getter/setter.
        // (Yarn cu <= 1.21.8: player.getInventory().selectedSlot)
        return player.getInventory().getSelectedSlot();
    }

    public static void setHotbarSlot(ClientPlayerEntity player, int slot) {
        PlayerInventory inv = player.getInventory();
        int s = Math.max(0, Math.min(8, slot));
        inv.setSelectedSlot(s);
    }

    public static int findInHotbar(ClientPlayerEntity player, Item item) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    /** Tim trong tui chinh (slot 9..35). */
    public static int findInBackpack(ClientPlayerEntity player, Item item) {
        PlayerInventory inv = player.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    /** Doi block tu tui chinh len hotbar. */
    public static boolean moveToHotbar(ClientPlayerEntity player, Item item, int hotbarSlot) {
        PlayerInventory inv = player.getInventory();
        int src = findInBackpack(player, item);
        if (src < 0) return false;
        ItemStack tmp = inv.getStack(hotbarSlot);
        inv.setStack(hotbarSlot, inv.getStack(src));
        inv.setStack(src, tmp);
        return true;
    }

    public static int countInInventory(ClientPlayerEntity player, Item item) {
        PlayerInventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack st = inv.getStack(i);
            if (st.isOf(item)) n += st.getCount();
        }
        return n;
    }

    /** Chi dung o Creative: tu tao block de khong phai mo tui. */
    public static void giveCreative(ClientPlayerEntity player, Item item) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isEmpty()) {
                inv.setStack(i, new ItemStack(item, 64));
                return;
            }
        }
        inv.setStack(currentSlot(player), new ItemStack(item, 64));
    }
}
