package dev.donutautosort;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public class SortConfigScreen extends Screen {
    private final Screen parent;
    private EditBox itemsField;

    public SortConfigScreen(Screen parent, List<String> priorities) {
        super(Component.literal("Donut Auto Sort"));
        this.parent = parent;
        this.initialText = String.join(", ", priorities);
    }

    private final String initialText;

    @Override
    protected void init() {
        int center = this.width / 2;
        itemsField = new EditBox(this.font, center - 150, this.height / 2 - 15, 300, 20,
                Component.literal("Priorisierte Items"));
        itemsField.setMaxLength(2000);
        itemsField.setValue(initialText);
        addRenderableWidget(itemsField);

        addRenderableWidget(Button.builder(Component.literal("Speichern"), button -> {
            List<String> entries = Arrays.stream(itemsField.getValue().split("[,\\n]"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            DonutAutoSortClient.setPriorities(entries);
            this.minecraft.setScreen(parent);
        }).bounds(center - 154, this.height / 2 + 20, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Standard"), button -> {
            itemsField.setValue("minecraft:totem_of_undying, minecraft:ender_pearl, minecraft:end_crystal, minecraft:obsidian, minecraft:golden_apple");
        }).bounds(center - 50, this.height / 2 + 20, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Zurück"), button -> this.minecraft.setScreen(parent))
                .bounds(center + 54, this.height / 2 + 20, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 65, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Item-IDs in Reihenfolge, getrennt durch Kommas:", this.width / 2,
                this.height / 2 - 40, 0xC0C0C0);
        graphics.drawCenteredString(this.font, "Beispiel: minecraft:totem_of_undying", this.width / 2,
                this.height / 2 + 55, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
