package vn.autobuilder.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import vn.autobuilder.builder.Blueprint;
import vn.autobuilder.builder.BuilderEngine;
import vn.autobuilder.builder.SchematicParser;
import vn.autobuilder.util.ChatUtil;
import vn.autobuilder.util.Config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Danh sach file .litematic / .schem trong thu muc "schematics" cua game.
 * Dung nut Trang truoc / Trang sau de luot (khong dung widget cuon nang).
 */
public class SchematicListScreen extends Screen {

    private static final int PER_PAGE = 6;
    private static final int PANEL_W = 250;
    private static final int PANEL_H = 214;

    private final Screen parent;
    private final List<Path> files = new ArrayList<>();
    private int page = 0;
    private String message = "";

    public SchematicListScreen(Screen parent) {
        super(Text.literal("Chon ban ve"));
        this.parent = parent;
    }

    private int panelTop() { return Math.max(8, (this.height - PANEL_H) / 2); }
    private int panelLeft() { return (this.width - PANEL_W) / 2; }

    private void refresh() {
        files.clear();
        try {
            Path dir = Config.schematicsDir();
            if (!Files.exists(dir)) Files.createDirectories(dir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) {
                    String name = p.getFileName().toString().toLowerCase();
                    if (name.endsWith(".litematic") || name.endsWith(".schem") || name.endsWith(".schematic")) {
                        files.add(p);
                    }
                }
            }
            files.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));
        } catch (Exception e) {
            message = "Loi doc thu muc: " + e.getMessage();
        }
    }

    private int maxPage() { return Math.max(0, (files.size() - 1) / PER_PAGE); }

    @Override
    protected void init() {
        refresh();
        int left = panelLeft();
        int top = panelTop();

        int y = top + 30;
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int index = start + i;
            if (index >= files.size()) break;
            Path path = files.get(index);
            String name = path.getFileName().toString();
            if (name.length() > 28) name = name.substring(0, 27) + "..";
            addDrawableChild(ButtonWidget.builder(Text.literal(name), b -> open(path))
                    .dimensions(left + 4, y, PANEL_W - 8, 20).build());
            y += 22;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("<  Truoc"), b -> {
            if (page > 0) { page--; this.clearChildren(); this.init(); }
        }).dimensions(left + 4, top + 162, (PANEL_W - 12) / 2, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Sau  >"), b -> {
            if (page < maxPage()) { page++; this.clearChildren(); this.init(); }
        }).dimensions(left + 8 + (PANEL_W - 12) / 2, top + 162, (PANEL_W - 12) / 2, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Dong"), b -> close())
                .dimensions(left + 4, top + 186, PANEL_W - 8, 20).build());
    }

    private void open(Path path) {
        try {
            Blueprint bp = SchematicParser.load(path);
            BuilderEngine.get().setBlueprint(bp, path.getFileName().toString());
            Config.get().lastFile = path.getFileName().toString();
            Config.get().save();
            ChatUtil.say("Da nap: " + bp.describe());
            if (this.client != null) this.client.setScreen(parent);
        } catch (IOException e) {
            message = e.getMessage() == null ? "Doc file loi" : e.getMessage();
            ChatUtil.error("Doc file loi: " + message);
        } catch (Exception e) {
            message = e.toString();
            ChatUtil.error("Loi: " + message);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = panelLeft();
        int top = panelTop();

        context.fill(0, 0, this.width, this.height, 0xC00A0E14);
        context.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF20D131C);
        context.fill(left, top, left + PANEL_W, top + 1, 0xFF34D399);
        context.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF34D399);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top + 8, 0xFF34D399);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Thu muc: .minecraft/schematics  (" + files.size() + " file)"),
                left + 4, top + 19, 0xFF94A3B8);

        if (files.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("Chua co ban ve nao!"),
                    left + 4, top + 62, 0xFFFCA5A5);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("Copy file .litematic / .schem vao thu muc"),
                    left + 4, top + 74, 0xFF94A3B8);
        }

        if (!message.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(message),
                    left + 4, top + 145, 0xFFFCA5A5);
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
