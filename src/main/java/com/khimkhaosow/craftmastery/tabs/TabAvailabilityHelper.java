package com.khimkhaosow.craftmastery.tabs;

import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.NodeData;
import com.khimkhaosow.craftmastery.config.RecipeTreeConfigManager.TabData;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;

import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Utility methods for evaluating tab availability based on configuration rules.
 */
public final class TabAvailabilityHelper {

    private TabAvailabilityHelper() {
    }

    public static boolean isTabUnlocked(EntityPlayer player, TabData tabData) {
        return describeLockReasons(player, tabData).isEmpty();
    }

    public static List<String> describeLockReasons(EntityPlayer player, TabData tabData) {
        if (player == null || tabData == null) {
            return Collections.singletonList("Недостаточно данных для проверки");
        }

        Set<String> reasons = new LinkedHashSet<>();
        UUID playerUUID = player.getUniqueID();
        TabManager tabManager = TabManager.getInstance();

        // Required tabs
        for (String requiredTabId : safeList(tabData.requiredTabs)) {
            String trimmed = requiredTabId.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!isTabStudied(tabManager, playerUUID, trimmed)) {
                reasons.add(String.format("Нужно изучить вкладку \"%s\"", trimmed));
            }
        }

        // Required nodes
        for (String requiredNodeId : safeList(tabData.requiredNodes)) {
            String trimmed = requiredNodeId.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!isNodeStudied(player, trimmed)) {
                reasons.add(String.format("Нужно изучить узел \"%s\"", trimmed));
            }
        }

        // Required permissions
        for (String permissionId : safeList(tabData.requiredPermissions)) {
            String trimmed = permissionId.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            PermissionType permissionType = resolvePermission(trimmed);
            if (permissionType == null) {
                reasons.add(String.format("Неизвестное право \"%s\"", trimmed));
                continue;
            }
            if (!PermissionManager.getInstance().hasPermission(player, permissionType)) {
                reasons.add(String.format("Нужно право \"%s\"", trimmed));
            }
        }

        return new ArrayList<>(reasons);
    }

    private static List<String> safeList(List<String> source) {
        return source != null ? source : Collections.emptyList();
    }

    private static boolean isTabStudied(TabManager tabManager, UUID playerUUID, String tabId) {
        if ("default".equalsIgnoreCase(tabId) || "main".equalsIgnoreCase(tabId)) {
            return true;
        }
        Tab tab = tabManager.getTab(tabId);
        if (tab != null) {
            return tab.isStudiedByPlayer(playerUUID);
        }
        // If the tab is not registered in TabManager, assume requirement is not met
        return false;
    }

    private static boolean isNodeStudied(EntityPlayer player, String nodeId) {
        Optional<NodeData> nodeOptional = RecipeTreeConfigManager.getInstance().getNode(nodeId);
        if (!nodeOptional.isPresent()) {
            // Unknown node – treat as satisfied so that configuration typos do not brick the GUI
            return true;
        }
        NodeData node = nodeOptional.get();
        if (node.recipeId == null || node.recipeId.trim().isEmpty()) {
            // Custom nodes without recipes are considered always available for now
            return true;
        }
        RecipeEntry recipe = RecipeManager.getInstance().getRecipe(node.recipeId);
        if (recipe == null) {
            return true;
        }
        return recipe.isStudiedByPlayer(player.getUniqueID());
    }

    private static PermissionType resolvePermission(String permissionId) {
        try {
            return PermissionType.valueOf(permissionId.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
