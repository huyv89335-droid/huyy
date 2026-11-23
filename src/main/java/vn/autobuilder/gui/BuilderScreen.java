package vn.autobuilder.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import vn.autobuilder.builder.BuilderEngine;
import vn.autobuilder.util.ChatUtil;
import vn.autobuilder.util.Config;
import vn.autobuilder.util.LookUtil;

import java.util.List;

/**
 * Menu chinh - ve 100% 2D (fill + text), khong co hieu ung 3D/blur nang.
 */
public class BuilderScreen extends Screen {

    private static final int PANEL_W = 246;
    private static final int PANEL_H = 252;

    private final Screen parent;
    private TextFieldWidget ox, oy, oz, cx, cy, cz;
    private ButtonWidget rotateButton;
    private ButtonWidget speedButton;

    public BuilderScreen(Screen parent) {
        super(Text.literal("AutoBuilder"));
        this.parent = parent;
    }

    private BuilderEngine engine() { return BuilderEngine.get(); }

    private int panelTop() { return Math.max(8, (this.height - PANEL_H) / 2); }
    private int panelLeft() { return (this.width - PANEL_W) / 2; }

    @Override
    protected void init() {
        BuilderEngine eng = engine();
        int left = panelLeft();
        int top = panelTop();
        int fieldW = (PANEL_W - 8 - 8) / 3;

        /* --- Goc xay X Y Z --- */
        ox = addField(left + 4, top + 60, fieldW, String.valueOf(eng.getOrigin().getX()));
        oy = addField(left + 8 + fieldW, top + 60, fieldW, String.valueOf(eng.getOrigin().getY()));
        oz = addField(left + 12 + fieldW * 2, top + 60, fieldW, String.valueOf(eng.getOrigin().getZ()));

        addBtn(left + 4, top + 82, PANEL_W - 8, 20, "Lay vi tri dang stand", b -> {
            if (this.client != null && this.client.player != null) {
                BlockPos pos = this.client.player.getBlockPos();
                eng.setOrigin(pos);
                ox.setText(String.valueOf(pos.getX()));
                oy.setText(String.valueOf(pos.getY()));
                oz.setText(String.valueOf(pos.getZ()));
            }
        });

        /* --- Ruong do X Y Z --- */
        BlockPos chest = eng.getChestPos();
        cx = addField(left + 4, top + 124, fieldW, chest == null ? "" : String.valueOf(chest.getX()));
        cy = addField(left + 8 + fieldW, top + 124, fieldW, chest == null ? "" : String.valueOf(chest.getY()));
        cz = addField(left + 12 + fieldW * 2, top + 124, fieldW, chest == null ? "" : String.valueOf(chest.getZ()));

        addBtn(left + 4, top + 146, PANEL_W - 8, 20, "Ruong = block dang nhin", b -> {
            if (this.client == null || this.client.player == null) return;
            HitResult hit = this.client.crosshairTarget;
            if (hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK) {
                eng.setChestPos(((BlockHitResult) hit).getBlockPos());
                BlockPos p = ((BlockHitResult) hit).getBlockPos();
                cx.setText(String.valueOf(p.getX()));
                cy.setText(String.valueOf(p.getY()));
                cz.setText(String.valueOf(p.getZ()));
            } else {
                ChatUtil.error("Dang khong nhin vao block nao.");
            }
        });

        /* --- Xoay + toc do --- */
        rotateButton = addBtn(left + 4, top + 170, (PANEL_W - 12) / 2, 20,
                "Xoay: " + (eng.getRotation() * 90) + " do", b -> {
            eng.cycleRotation();
            rotateButton.setMessage(Text.literal("Xoay: " + (eng.getRotation() * 90) + " do"));
        });

        speedButton = addBtn(left + 8 + (PANEL_W - 12) / 2, top + 170, (PANEL_W - 12) / 2, 20,
                "Nghi: " + eng.getPlaceDelay() + "t", b -> {
            eng.cycleSpeed();
            speedButton.setMessage(Text.literal("Nghi: " + eng.getPlaceDelay() + "t"));
        });

        /* --- BAT / TAT --- */
        addBtn(left + 4, top + 194, (PANEL_W - 12) / 2, 20, "BAT", b -> eng.start());
        addBtn(left + 8 + (PANEL_W - 12) / 2, top + 194, (PANEL_W - 12) / 2, 20, "TAT", b -> {
            eng.stop(null);
            ChatUtil.say("Da TAT auto builder.");
        });

        addBtn(left + 4, top + 218, PANEL_W - 8, 20, "Chon ban ve", b -> {
            if (this.client != null) this.client.setScreen(new SchematicListScreen(this));
        });
    }

    private TextFieldWidget addField(int x, int y, int w, String text) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, w, 16, Text.literal("so"));
        f.setText(text);
        f.setMaxLength(10);
        f.setChangedListener(s -> {
            if (s.isEmpty()) return;
            String clean = s.replaceAll("[^0-9-]", "");
            if (!clean.equals(s)) f.setText(clean);
        });
        addDrawableChild(f);
        return f;
    }

    private ButtonWidget addBtn(int x, int y, int w, int h, String label, ButtonWidget.PressAction action) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), action)
                .dimensions(x, y, w, h)
                .build();
        addDrawableChild(b);
        return b;
    }

    private int parseInt(TextFieldWidget field, int def) {
        try { return Integer.parseInt(field.getText().trim()); } catch (Exception e) { return def; }
    }

    public void saveFields() {
        BlockPos origin = new BlockPos(parseInt(ox, 0), parseInt(oy, 64), parseInt(oz, 0));
        engine().setOrigin(origin);
        if (cx == null || cx.getText().trim().isEmpty()
                || cy == null || cy.getText().trim().isEmpty()
                || cz == null || cz.getText().trim().isEmpty()) {
            engine().setChestPos(null);
        } else {
            engine().setChestPos(new BlockPos(parseInt(cx, 0), parseInt(cy, 64), parseInt(cz, 0)));
        }
        Config.get().save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = panelLeft();
        int top = panelTop();

        // Nen toi don gian (khong blur, khong 3D) -> gan nhu khong ton GPU
        context.fill(0, 0, this.width, this.height, 0xC00A0E14);
        context.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF20D131C);
        context.fill(left, top, left + PANEL_W, top + 1, 0xFF34D399);
        context.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF34D399);

        // Cac nut + o nhap
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top + 8, 0xFF34D399);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Ban ve:"), left + 4, top + 20, 0xFF94A3B8);

        String file = engine().getFileName().isEmpty() ? "(chua chon)" : engine().getFileName();
        context.drawTextWithShadow(this.textRenderer, Text.literal(file), left + 4, top + 32, 0xFFE2E8F0);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Goc bat dau xay  X  Y  Z"), left + 4, top + 50, 0xFF94A3B8);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Ruong do  X  Y  Z (bo trong = khong dung)"), left + 4, top + 114, 0xFF94A3B8);

        // Bang trang thai
        int by = top + 224;
        context.fill(left + 4, by - 4, left + PANEL_W - 4, top + PANEL_H - 6, 0x88000000);
        List<String> lines = engine().statusLines();
        for (int i = 0; i < lines.size(); i++) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(lines.get(i)), left + 8, by + i * 5, 0xFFCBD5E1);
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        saveFields();
        if (this.client != null) this.client.setScreen(parent);
    }
}
