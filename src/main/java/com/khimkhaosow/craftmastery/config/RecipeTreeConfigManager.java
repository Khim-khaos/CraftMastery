package com.khimkhaosow.craftmastery.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.khimkhaosow.craftmastery.CraftMastery;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Управляет конфигурацией древ дерева рецептов, хранящейся в файле config/craftmastery/recipe_tree.json.
 * Формат файла описан в RecipeTreeConfigManager.TreeConfig.
 */
public class RecipeTreeConfigManager {

    private static final RecipeTreeConfigManager INSTANCE = new RecipeTreeConfigManager();

    private static final String CONFIG_DIR = "config/craftmastery";
    private static final String CONFIG_FILE = "recipe_tree.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private TreeConfig config;

    private RecipeTreeConfigManager() {
        loadInternal();
    }

    public static RecipeTreeConfigManager getInstance() {
        return INSTANCE;
    }

    public synchronized void reload() {
        loadInternal();
    }

    public synchronized void save() {
        ensureConfigPresent();
        File file = getConfigFile();
        File directory = file.getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            CraftMastery.logger.error("Failed to create recipe tree config directory: {}", directory.getAbsolutePath());
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            CraftMastery.logger.error("Failed to save recipe tree config", e);
        }
    }

    public synchronized List<TabData> getTabs() {
        ensureConfigPresent();
        return Collections.unmodifiableList(config.tabs);
    }

    public synchronized Optional<TabData> getTab(String tabId) {
        ensureConfigPresent();
        return config.tabs.stream().filter(tab -> Objects.equals(tab.id, tabId)).findFirst();
    }

    public synchronized List<NodeData> getNodes() {
        ensureConfigPresent();
        return Collections.unmodifiableList(config.nodes);
    }

    public synchronized Optional<NodeData> getNode(String nodeId) {
        ensureConfigPresent();
        return config.nodes.stream().filter(node -> Objects.equals(node.id, nodeId)).findFirst();
    }

    public synchronized void upsertNode(NodeData node) {
        ensureConfigPresent();
        Objects.requireNonNull(node, "node");
        if (node.id == null || node.id.trim().isEmpty()) {
            throw new IllegalArgumentException("Node id must not be empty");
        }

        getNode(node.id).ifPresent(existing -> config.nodes.remove(existing));
        config.nodes.add(node);
    }

    public synchronized void upsertTab(TabData tab) {
        ensureConfigPresent();
        Objects.requireNonNull(tab, "tab");
        if (tab.id == null || tab.id.trim().isEmpty()) {
            throw new IllegalArgumentException("Tab id must not be empty");
        }
        getTab(tab.id).ifPresent(existing -> config.tabs.remove(existing));
        config.tabs.add(tab);
    }

    public synchronized boolean removeTab(String tabId) {
        ensureConfigPresent();
        if (tabId == null || tabId.trim().isEmpty()) {
            return false;
        }
        boolean removed = config.tabs.removeIf(tab -> Objects.equals(tab.id, tabId));
        if (removed) {
            for (NodeData node : config.nodes) {
                if (tabId.equals(node.tab)) {
                    node.tab = TabData.defaultTab().id;
                }
                if (node.unlocks != null && node.unlocks.tabs != null) {
                    node.unlocks.tabs.removeIf(tabId::equals);
                }
            }
        }
        return removed;
    }

    public synchronized boolean removeLink(String sourceId, String targetId) {
        ensureConfigPresent();
        if (sourceId == null || targetId == null) {
            return false;
        }
        NodeData source = getNode(sourceId).orElse(null);
        NodeData target = getNode(targetId).orElse(null);
        if (source == null || target == null) {
            return false;
        }
        boolean changed = false;
        if (source.unlocks != null && source.unlocks.nodes != null) {
            changed |= source.unlocks.nodes.removeIf(targetId::equals);
        }
        if (target.availability != null && target.availability.requiredNodes != null) {
            changed |= target.availability.requiredNodes.removeIf(sourceId::equals);
        }
        if (changed) {
            upsertNode(source);
            upsertNode(target);
        }
        return changed;
    }

    public synchronized boolean removeNode(String nodeId) {
        ensureConfigPresent();
        return config.nodes.removeIf(node -> Objects.equals(node.id, nodeId));
    }

    public synchronized void replaceTabs(List<TabData> tabs) {
        ensureConfigPresent();
        config.tabs.clear();
        if (tabs != null) {
            config.tabs.addAll(tabs);
        }
    }

    public synchronized File getConfigFile() {
        return new File(CONFIG_DIR, CONFIG_FILE);
    }

    private void loadInternal() {
        File file = getConfigFile();
        if (!file.exists()) {
            config = createDefaultConfig();
            save();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            TreeConfig loaded = gson.fromJson(reader, TreeConfig.class);
            if (loaded == null) {
                CraftMastery.logger.warn("Recipe tree config is empty, regenerating default");
                config = createDefaultConfig();
                save();
            } else {
                normalizeConfig(loaded);
                config = loaded;
            }
        } catch (IOException | JsonParseException ex) {
            CraftMastery.logger.error("Failed to load recipe tree config, using default", ex);
            config = createDefaultConfig();
            save();
        }
    }

    private void ensureConfigPresent() {
        if (config == null) {
            config = createDefaultConfig();
        }
    }

    private void normalizeConfig(TreeConfig target) {
        if (target.tabs == null) {
            target.tabs = new ArrayList<>();
        }
        if (target.nodes == null) {
            target.nodes = new ArrayList<>();
        }
        target.tabs.removeIf(Objects::isNull);
        target.nodes.removeIf(Objects::isNull);
    }

    private TreeConfig createDefaultConfig() {
        TreeConfig defaultConfig = new TreeConfig();
        defaultConfig.tabs.add(TabData.defaultTab());
        return defaultConfig;
    }

    public static class TreeConfig {
        public List<TabData> tabs = new ArrayList<>();
        public List<NodeData> nodes = new ArrayList<>();
    }

    public static class TabData {
        public String id;
        public String title;
        public String icon;

        public static TabData defaultTab() {
            TabData tab = new TabData();
            tab.id = "default";
            tab.title = "Основная";
            tab.icon = "craftmastery:textures/gui/default_tab_icon.png";
            return tab;
        }
    }

    public static class NodeData {
        public String id;
        public String tab;
        public String recipeId;
        public String displayName;
        public int studyCost;
        public Position position = new Position();
        public Availability availability = new Availability();
        public Unlocks unlocks = new Unlocks();
        public boolean grantsCraftAccess = false;
        public String customIcon;
        public String nodeTitle;
    }

    public static class Position {
        public int x;
        public int y;
    }

    public static class Availability {
        public static final String MODE_ALWAYS = "always";
        public static final String MODE_REQUIRES_NODES = "requires_nodes";

        public String mode = MODE_ALWAYS;
        public List<String> requiredNodes = new ArrayList<>();
        public boolean allowIndependentStudy = false;
    }

    public static class Unlocks {
        public List<String> nodes = new ArrayList<>();
        public List<String> tabs = new ArrayList<>();
        public List<String> permissions = new ArrayList<>();
    }
}
