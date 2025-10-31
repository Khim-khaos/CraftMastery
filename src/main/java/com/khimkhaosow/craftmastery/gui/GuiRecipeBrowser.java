package com.khimkhaosow.craftmastery.gui;

import org.lwjgl.input.Mouse;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/**
 * GUI браузера рецептов для добавления в дерево навыков
 */
public class GuiRecipeBrowser extends GuiScreen {
    public interface RecipeSelectionHandler {
        void onRecipeSelected(RecipeEntry recipe);

        default void onSelectionCancelled() {
        }
    }

    private final GuiScreen parentScreen;
    private final RecipeSelectionHandler selectionHandler;
    private final EntityPlayer player;
    private GuiTextField searchField;
    private TabButton minecraftTab;
    private TabButton modsTab;
    private boolean minecraftSelected = true;
    private List<RecipeEntry> allRecipes;
    private Map<String, List<RecipeEntry>> groupedRecipes;
    private List<String> visibleCategories;
    private Map<String, List<RecipeEntry>> visibleGroupedRecipes;
    private RecipeEntry selectedRecipe;
    private String selectedCategory;
    private int scrollOffset = 0;
    private int totalVisibleRows = 0;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int initialScrollOffset;
    private static final int ITEM_HEIGHT = 20;
    private static final int LIST_LEFT_MARGIN = 200;
    private static final int LIST_TOP = 60;
    private static final int LIST_HEIGHT_MARGIN = 120;

    public GuiRecipeBrowser(EntityPlayer player) {
        this(null, player, null);
    }

    public GuiRecipeBrowser(GuiScreen parentScreen, EntityPlayer player, RecipeSelectionHandler handler) {
        this.parentScreen = parentScreen;
        this.selectionHandler = handler;
        this.player = player;
        loadAllRecipes();
    }

    private void loadAllRecipes() {
        allRecipes = RecipeManager.getInstance().getAllRecipes();
        groupedRecipes = groupByMod(allRecipes);
        refreshVisibleRecipes();
    }

    @Override
    public void initGui() {
        super.initGui();

        // Поле поиска
        searchField = new GuiTextField(0, mc.fontRenderer, width / 2 - 100, 20, 200, 20);
        searchField.setFocused(true);
        searchField.setMaxStringLength(50);

        int tabWidth = 80;
        minecraftTab = new TabButton(10, width / 2 - 200, 45, tabWidth, 18, "Minecraft", true);
        modsTab = new TabButton(11, width / 2 - 200 + tabWidth + 4, 45, tabWidth, 18, "Моды", false);
        buttonList.add(minecraftTab);
        buttonList.add(modsTab);

        // Кнопки
        buttonList.add(new GuiButton(2, width / 2 - 90, height - 40, 80, 20, "Выбрать"));
        buttonList.add(new GuiButton(3, width / 2 + 10, height - 40, 80, 20, "Отмена"));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        searchField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Заголовок
        drawCenteredString(mc.fontRenderer, "Браузер рецептов", width / 2, 5, 0xFFFFFF);

        // Поля
        searchField.drawTextBox();

        // Подписи
        drawString(mc.fontRenderer, "Поиск:", width / 2 - 150, 26, 0xFFFFFF);

        drawRecipeList(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 2: // Добавить
                addRecipeToTree();
                break;
            case 3: // Отмена
                cancelAndReturn();
                break;
            case 4: // Обновить поиск
                performSearch();
                break;
            case 10:
            case 11:
                setTabSelected(button.id == 10);
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.isFocused()) {
            searchField.textboxKeyTyped(typedChar, keyCode);
            performSearch();
        } else if (keyCode == 1) { // ESC
            cancelAndReturn();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        if (!handleScrollbarClick(mouseX, mouseY, mouseButton)) {
            handleListClick(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getDWheel();
        if (scroll != 0 && totalVisibleRows > getMaxVisibleRows()) {
            int maxOffset = totalVisibleRows - getMaxVisibleRows();
            if (scroll > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxOffset, scrollOffset + 1);
            }
        }
        if (draggingScrollbar && Mouse.isButtonDown(0) && totalVisibleRows > getMaxVisibleRows()) {
            int currentMouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight;
            updateScrollFromDrag(currentMouseY - dragStartY);
        } else if (!Mouse.isButtonDown(0)) {
            draggingScrollbar = false;
        }
    }

    private void performSearch() {
        refreshVisibleRecipes();
    }

    private void setTabSelected(boolean minecraft) {
        this.minecraftSelected = minecraft;
        minecraftTab.setSelected(minecraft);
        modsTab.setSelected(!minecraft);
        refreshVisibleRecipes();
    }

    private void refreshVisibleRecipes() {
        Map<String, List<RecipeEntry>> filteredByTab = getRecipesForSelectedTab();
        String query = searchField != null ? searchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        Map<String, List<RecipeEntry>> resultMap = query.isEmpty()
            ? filteredByTab
            : filterGroupedRecipes(filteredByTab, query);

        visibleGroupedRecipes = resultMap;
        visibleCategories = new ArrayList<>(visibleGroupedRecipes.keySet());
        selectedCategory = null;
        selectedRecipe = null;
        scrollOffset = 0;
        recalculateVisibleRows();
    }

    private Map<String, List<RecipeEntry>> getRecipesForSelectedTab() {
        Map<String, List<RecipeEntry>> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, List<RecipeEntry>> entry : groupedRecipes.entrySet()) {
            List<RecipeEntry> matches = entry.getValue().stream()
                .filter(recipe -> minecraftSelected == "minecraft".equals(getNamespace(recipe.getRecipeLocation())))
                .collect(Collectors.toList());
            if (!matches.isEmpty()) {
                filtered.put(entry.getKey(), matches);
            }
        }
        return filtered;
    }

    private Map<String, List<RecipeEntry>> groupByMod(List<RecipeEntry> recipes) {
        Map<String, List<RecipeEntry>> result = new LinkedHashMap<>();
        Map<String, String> modNames = getModNames();

        // Собираем рецепты по namespace
        Map<String, List<RecipeEntry>> byNamespace = recipes.stream()
            .collect(Collectors.groupingBy(recipe -> getNamespace(recipe.getRecipeLocation())));

        List<String> namespaces = new ArrayList<>(byNamespace.keySet());
        Collections.sort(namespaces);

        for (String namespace : namespaces) {
            String modName = modNames.getOrDefault(namespace, namespace);
            List<RecipeEntry> entries = new ArrayList<>(byNamespace.get(namespace));
            entries.sort(Comparator.comparing(entry -> entry.getRecipeResult().getDisplayName().toLowerCase(Locale.ROOT)));
            result.put(modName, entries);
        }

        return result;
    }

    private Map<String, List<RecipeEntry>> filterGroupedRecipes(Map<String, List<RecipeEntry>> source, String query) {
        Map<String, List<RecipeEntry>> filtered = new LinkedHashMap<>();
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<RecipeEntry>> entry : source.entrySet()) {
            List<RecipeEntry> matching = entry.getValue().stream()
                .filter(recipe -> recipe.getRecipeResult().getDisplayName().toLowerCase(Locale.ROOT).contains(lowerQuery)
                    || getNamespace(recipe.getRecipeLocation()).contains(lowerQuery)
                    || entry.getKey().toLowerCase(Locale.ROOT).contains(lowerQuery))
                .collect(Collectors.toList());
            if (!matching.isEmpty()) {
                filtered.put(entry.getKey(), matching);
            }
        }
        return filtered;
    }

    private void drawRecipeList(int mouseX, int mouseY) {
        int startY = LIST_TOP;
        int listLeft = width / 2 - LIST_LEFT_MARGIN;
        int listRight = width / 2 + LIST_LEFT_MARGIN;
        int listHeight = height - LIST_HEIGHT_MARGIN;

        drawRect(listLeft, startY - 5, listRight, startY + listHeight + 5, 0x80000000);

        int y = startY;
        int rowIndex = 0;
        int maxRows = getMaxVisibleRows();

        for (String category : visibleCategories) {
            if (rowIndex + visibleGroupedRecipes.get(category).size() + 1 <= scrollOffset) {
                rowIndex += visibleGroupedRecipes.get(category).size() + 1;
                continue;
            }

            if (rowIndex >= scrollOffset + maxRows) {
                break;
            }

            // Отрисовка заголовка категории
            if (rowIndex >= scrollOffset) {
                int headerY = y + (rowIndex - scrollOffset) * ITEM_HEIGHT;
                drawRect(listLeft + 5, headerY, listRight - 5, headerY + ITEM_HEIGHT - 2, 0xFF555555);
                drawString(mc.fontRenderer, category, listLeft + 20, headerY + 6, 0xFFFFFF);
            }
            rowIndex++;

            List<RecipeEntry> recipes = visibleGroupedRecipes.get(category);
            for (RecipeEntry recipe : recipes) {
                if (rowIndex < scrollOffset) {
                    rowIndex++;
                    continue;
                }
                if (rowIndex >= scrollOffset + maxRows) {
                    break;
                }

                int rowY = y + (rowIndex - scrollOffset) * ITEM_HEIGHT;
                boolean isSelected = recipe.equals(selectedRecipe);
                int bgColor = isSelected ? 0xFF444444 : ((rowIndex % 2 == 0) ? 0xFF222222 : 0xFF333333);
                drawRect(listLeft + 5, rowY, listRight - 5, rowY + ITEM_HEIGHT - 2, bgColor);

                ItemStack result = recipe.getRecipeResult();
                if (!result.isEmpty()) {
                    mc.getRenderItem().renderItemIntoGUI(result, listLeft + 10, rowY + 2);
                }

                String name = result.getDisplayName();
                drawString(mc.fontRenderer, name, listLeft + 40, rowY + 6, 0xFFFFFF);

                String status = recipe.isStudiedByPlayer(player.getUniqueID()) ? "Изучен" : "Не изучен";
                int statusColor = recipe.isStudiedByPlayer(player.getUniqueID()) ? 0x00FF00 : 0xFFFF00;
                drawString(mc.fontRenderer, status, listRight - 50, rowY + 6, statusColor);

                rowIndex++;
            }
        }

        if (totalVisibleRows > maxRows) {
            drawScrollbar(startY, listHeight, listRight + 10, maxRows);
        }

        if (selectedRecipe != null) {
            int infoY = startY + listHeight + 20;
            drawString(mc.fontRenderer, "Выбран: " + selectedRecipe.getRecipeResult().getDisplayName(), listLeft, infoY, 0xFFFFFF);
        }
    }

    private void handleListClick(int mouseX, int mouseY, int mouseButton) {
        int startY = LIST_TOP;
        int listLeft = width / 2 - LIST_LEFT_MARGIN;
        int listRight = width / 2 + LIST_LEFT_MARGIN;
        int listHeight = height - LIST_HEIGHT_MARGIN;
        if (mouseX < listLeft || mouseX > listRight || mouseY < startY || mouseY > startY + listHeight) {
            return;
        }

        int relativeY = mouseY - startY;
        int clickedRow = scrollOffset + (relativeY / ITEM_HEIGHT);
        int currentRow = 0;

        for (String category : visibleCategories) {
            if (currentRow == clickedRow) {
                selectedCategory = category;
                selectedRecipe = null;
                return;
            }

            currentRow++;
            List<RecipeEntry> recipes = visibleGroupedRecipes.get(category);
            if (clickedRow < currentRow + recipes.size()) {
                int index = clickedRow - currentRow;
                selectedCategory = category;
                selectedRecipe = recipes.get(index);
                return;
            }

            currentRow += recipes.size();
        }
    }

    private void recalculateVisibleRows() {
        totalVisibleRows = 0;
        for (String category : visibleGroupedRecipes.keySet()) {
            totalVisibleRows += 1; // заголовок
            totalVisibleRows += visibleGroupedRecipes.get(category).size();
        }
    }

    private int getMaxVisibleRows() {
        int listHeight = height - LIST_HEIGHT_MARGIN;
        return listHeight / ITEM_HEIGHT;
    }

    private boolean handleScrollbarClick(int mouseX, int mouseY, int mouseButton) {
        if (totalVisibleRows <= getMaxVisibleRows() || mouseButton != 0) {
            return false;
        }
        int startY = LIST_TOP;
        int listHeight = height - LIST_HEIGHT_MARGIN;
        int scrollbarX = width / 2 + LIST_LEFT_MARGIN + 10;
        int scrollbarWidth = 12;
        int knobHeight = getScrollbarKnobHeight(listHeight);
        int knobTop = getScrollbarKnobTop(startY, listHeight, knobHeight);
        if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && mouseY >= knobTop && mouseY <= knobTop + knobHeight) {
            draggingScrollbar = true;
            dragStartY = mouseY;
            initialScrollOffset = scrollOffset;
            return true;
        }
        return false;
    }

    private void updateScrollFromDrag(int dragDelta) {
        int listHeight = height - LIST_HEIGHT_MARGIN;
        int knobHeight = getScrollbarKnobHeight(listHeight);
        int trackHeight = listHeight - knobHeight;
        if (trackHeight <= 0) {
            return;
        }
        int maxOffset = totalVisibleRows - getMaxVisibleRows();
        float ratio = (float) dragDelta / trackHeight;
        scrollOffset = Math.min(Math.max(0, initialScrollOffset + Math.round(ratio * maxOffset)), maxOffset);
    }

    private void drawScrollbar(int startY, int listHeight, int x, int maxRows) {
        int scrollbarWidth = 12;
        drawRect(x, startY, x + scrollbarWidth, startY + listHeight, 0x60000000);
        int knobHeight = getScrollbarKnobHeight(listHeight);
        int knobTop = getScrollbarKnobTop(startY, listHeight, knobHeight);
        int knobColor = draggingScrollbar ? 0xFFFFFFFF : 0xFFCCCCCC;
        drawRect(x + 2, knobTop, x + scrollbarWidth - 2, knobTop + knobHeight, knobColor);
    }

    private int getScrollbarKnobHeight(int listHeight) {
        int maxRows = getMaxVisibleRows();
        if (totalVisibleRows <= 0) {
            return listHeight;
        }
        int knobHeight = Math.max(20, (int) ((float) maxRows / totalVisibleRows * listHeight));
        return Math.min(knobHeight, listHeight);
    }

    private int getScrollbarKnobTop(int startY, int listHeight, int knobHeight) {
        int maxOffset = Math.max(1, totalVisibleRows - getMaxVisibleRows());
        int trackHeight = listHeight - knobHeight;
        if (trackHeight <= 0) {
            return startY;
        }
        return startY + Math.round((float) scrollOffset / maxOffset * trackHeight);
    }

    private class TabButton extends GuiButton {
        private boolean selected;

        TabButton(int id, int x, int y, int widthIn, int heightIn, String text, boolean selected) {
            super(id, x, y, widthIn, heightIn, text);
            this.selected = selected;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) {
                return;
            }
            int bg = selected ? 0xFF666666 : 0xFF333333;
            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, bg);
            int textColor = selected ? 0xFFFFFF00 : 0xFFFFFFFF;
            drawCenteredString(mc.fontRenderer, this.displayString, this.x + this.width / 2, this.y + 5, textColor);
        }
    }

    private Map<String, String> getModNames() {
        Map<String, String> modNames = new LinkedHashMap<>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            modNames.put(container.getModId().toLowerCase(Locale.ROOT), container.getName());
        }
        modNames.put("minecraft", "Minecraft");
        return modNames;
    }

    private String getNamespace(ResourceLocation location) {
        return location == null ? "unknown" : location.getNamespace().toLowerCase(Locale.ROOT);
    }
    private void addRecipeToTree() {
        if (selectedRecipe == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "Выберите рецепт!"));
            return;
        }

        if (selectionHandler != null) {
            selectionHandler.onRecipeSelected(selectedRecipe);
        }

        returnToParent();
    }

    private void cancelAndReturn() {
        if (selectionHandler != null) {
            selectionHandler.onSelectionCancelled();
        }
        returnToParent();
    }

    private void returnToParent() {
        if (mc == null) {
            Minecraft.getMinecraft().displayGuiScreen(parentScreen);
        } else {
            mc.displayGuiScreen(parentScreen);
        }
        if (parentScreen instanceof RecipeTreeEditorScreen) {
            ((RecipeTreeEditorScreen) parentScreen).restoreAfterChild();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
