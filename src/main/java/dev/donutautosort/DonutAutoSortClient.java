package dev.donutautosort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DonutAutoSortClient implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final Path CONFIG_PATH = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("donut-auto-sort.json");

    private static List<String> priorities = new ArrayList<>(List.of(
            "minecraft:totem_of_undying",
            "minecraft:ender_pearl",
            "minecraft:end_crystal",
            "minecraft:obsidian",
            "minecraft:golden_apple"
    ));

    private static KeyMapping openConfigKey;
    private static KeyMapping sortKey;

    @Override
    public void onInitializeClient() {
        loadConfig();
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donut_auto_sort.config", GLFW.GLFW_KEY_I,
                KeyMapping.Category.MISC));
        sortKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donut_auto_sort.sort", GLFW.GLFW_KEY_K,
                KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new SortConfigScreen(client.screen, priorities));
                }
            }
            while (sortKey.consumeClick()) {
                sortInventory(client);
            }
        });
    }

    static List<String> getPriorities() {
        return new ArrayList<>(priorities);
    }

    static void setPriorities(List<String> newPriorities) {
        priorities = new ArrayList<>();
        for (String value : newPriorities) {
            String id = value.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty() && !priorities.contains(id)) priorities.add(id);
        }
        saveConfig();
    }

    private static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                List<String> loaded = GSON.fromJson(Files.readString(CONFIG_PATH), LIST_TYPE);
                if (loaded != null && !loaded.isEmpty()) priorities = loaded;
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(priorities));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sortInventory(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;
        if (client.player.containerMenu != client.player.inventoryMenu) {
            client.player.displayClientMessage(Component.literal(
                    "Schließe zuerst das geöffnete Menü, um dein Inventar zu sortieren."), true);
            return;
        }

        // Player menu slots 9..44 cover all 27 main-inventory and 9 hotbar slots.
        List<ItemStack> desired = new ArrayList<>(36);
        for (int slot = 9; slot <= 44; slot++) {
            desired.add(client.player.inventoryMenu.getSlot(slot).getItem().copy());
        }

        Comparator<ItemStack> comparator = Comparator
                .comparingInt(DonutAutoSortClient::priorityOf)
                .thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .thenComparingInt(stack -> -stack.getCount());

        desired.sort((a, b) -> {
            if (a.isEmpty() && b.isEmpty()) return 0;
            if (a.isEmpty()) return 1;
            if (b.isEmpty()) return -1;
            return comparator.compare(a, b);
        });

        for (int index = 0; index < desired.size(); index++) {
            int targetSlot = 9 + index;
            ItemStack target = desired.get(index);
            ItemStack current = client.player.inventoryMenu.getSlot(targetSlot).getItem();
            if (sameStack(current, target)) continue;

            int sourceSlot = -1;
            for (int candidate = targetSlot + 1; candidate <= 44; candidate++) {
                if (sameStack(client.player.inventoryMenu.getSlot(candidate).getItem(), target)) {
                    sourceSlot = candidate;
                    break;
                }
            }
            if (sourceSlot < 0) continue;

            client.gameMode.handleInventoryMouseClick(0, targetSlot, 0, ClickType.PICKUP, client.player);
            client.gameMode.handleInventoryMouseClick(0, sourceSlot, 0, ClickType.PICKUP, client.player);
            client.gameMode.handleInventoryMouseClick(0, targetSlot, 0, ClickType.PICKUP, client.player);
        }
        client.player.displayClientMessage(Component.literal("Inventar sortiert."), true);
    }

    private static int priorityOf(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int index = priorities.indexOf(id);
        return index < 0 ? priorities.size() : index;
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount();
    }
}
