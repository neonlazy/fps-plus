package it.hiddenmultikeys;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Schermata "nascosta": tasti secondari, terziari... per ogni azione. */
public class ExtraBindingsScreen extends Screen {
    private static final int ROW_H = 24;
    private static final int TOP = 36;

    private final Screen parent;
    private int page = 0;
    private int pages = 1;
    private int perPage = 1;
    private KeyMapping listening = null;
    private int listeningIndex = -1; // -1 = nuovo tasto

    public ExtraBindingsScreen(Screen parent) {
        super(Component.translatable("hidden_multikeys.title"));
        this.parent = parent;
    }

    private KeyMapping[] all() {
        return this.minecraft.options.keyMappings;
    }

    @Override
    protected void init() {
        KeyMapping[] all = all();
        perPage = Math.max(1, (this.height - TOP - 50) / ROW_H);
        pages = Math.max(1, (all.length + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);

        int left = this.width / 2 - 190;
        int start = page * perPage;
        int end = Math.min(all.length, start + perPage);

        for (int i = start; i < end; i++) {
            KeyMapping km = all[i];
            int y = TOP + (i - start) * ROW_H;
            List<InputConstants.Key> extras = ExtraBindings.view(km);
            int x = left + 135;
            for (int j = 0; j < extras.size(); j++) {
                final int idx = j;
                boolean active = km == listening && listeningIndex == j;
                Component label = active ? waiting() : extras.get(j).getDisplayName();
                this.addRenderableWidget(Button.builder(label, b -> startListening(km, idx))
                        .bounds(x, y, 52, 20).build());
                x += 54;
            }
            if (extras.size() < ExtraBindings.MAX_EXTRA) {
                boolean active = km == listening && listeningIndex == -1;
                Component label = active ? waiting() : Component.literal("+");
                this.addRenderableWidget(Button.builder(label, b -> startListening(km, -1))
                        .bounds(x, y, active ? 52 : 20, 20).build());
            }
        }

        int by = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("hidden_multikeys.prev"), b -> {
            if (page > 0) { page--; rebuildWidgets(); }
        }).bounds(this.width / 2 - 154, by, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("hidden_multikeys.done"), b -> onClose())
                .bounds(this.width / 2 - 50, by, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("hidden_multikeys.next"), b -> {
            if (page < pages - 1) { page++; rebuildWidgets(); }
        }).bounds(this.width / 2 + 84, by, 70, 20).build());
    }

    private static Component waiting() {
        return Component.literal("> ? <").withStyle(ChatFormatting.YELLOW);
    }

    private void startListening(KeyMapping km, int index) {
        this.listening = km;
        this.listeningIndex = index;
        rebuildWidgets();
    }

    private void finishListening() {
        this.listening = null;
        this.listeningIndex = -1;
        ExtraBindings.save();
        rebuildWidgets();
    }

    private void assign(InputConstants.Key key) {
        List<InputConstants.Key> list = ExtraBindings.list(listening);
        int existing = list.indexOf(key);
        if (listeningIndex >= 0 && listeningIndex < list.size()) {
            if (existing >= 0 && existing != listeningIndex) list.remove(existing);
            list.set(Math.min(listeningIndex, list.size() - 1), key);
        } else if (existing < 0 && list.size() < ExtraBindings.MAX_EXTRA) {
            list.add(key);
        }
    }

    private void removeCurrent() {
        List<InputConstants.Key> list = ExtraBindings.list(listening);
        if (listeningIndex >= 0 && listeningIndex < list.size()) list.remove(listeningIndex);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            int code = event.key();
            if (code == GLFW.GLFW_KEY_ESCAPE) {
                // annulla
            } else if (code == GLFW.GLFW_KEY_BACKSPACE || code == GLFW.GLFW_KEY_DELETE) {
                removeCurrent();
            } else {
                assign(InputConstants.getKey(event));
            }
            finishListening();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listening != null) {
            assign(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            finishListening();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        KeyMapping[] all = all();
        int left = this.width / 2 - 190;
        int start = page * perPage;
        int end = Math.min(all.length, start + perPage);
        for (int i = start; i < end; i++) {
            int y = TOP + (i - start) * ROW_H;
            Component name = Component.translatable(all[i].getName());
            g.drawString(this.font, Language.getInstance().getVisualOrder(this.font.substrByWidth(name, 128)),
                    left, y + 6, 0xFFFFFFFF);
        }

        Component hint = listening != null
                ? Component.translatable("hidden_multikeys.listening").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("hidden_multikeys.hint").withStyle(ChatFormatting.GRAY);
        g.drawCenteredString(this.font, hint, this.width / 2, this.height - 42, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("hidden_multikeys.page", page + 1, pages),
                this.width / 2 + 120, 12, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        ExtraBindings.save();
        this.minecraft.setScreen(parent);
    }
}
