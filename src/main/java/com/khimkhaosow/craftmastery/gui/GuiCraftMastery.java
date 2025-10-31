package com.khimkhaosow.craftmastery.gui;
import org.lwjgl.input.Mouse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.khimkhaosow.craftmastery.experience.ExperienceManager;
import com.khimkhaosow.craftmastery.experience.PlayerExperienceData;
import com.khimkhaosow.craftmastery.experience.PointsType;
import com.khimkhaosow.craftmastery.permissions.PermissionManager;
import com.khimkhaosow.craftmastery.permissions.PermissionType;
import com.khimkhaosow.craftmastery.recipe.RecipeEntry;
import com.khimkhaosow.craftmastery.recipe.RecipeManager;
import com.khimkhaosow.craftmastery.recipe.RecipeTag;
import com.khimkhaosow.craftmastery.gui.widgets.LevelProgressWidget;
import com.khimkhaosow.craftmastery.gui.widgets.RecipeTreeWidget;
import com.khimkhaosow.craftmastery.gui.widgets.TabBarWidget;
import com.khimkhaosow.craftmastery.gui.widgets.PointsPanelWidget;
import com.khimkhaosow.craftmastery.gui.GuiRecipeBrowser;
import com.khimkhaosow.craftmastery.gui.GuiAdminSettingsScreen;
import com.khimkhaosow.craftmastery.gui.GuiPlayerSettings;
import com.khimkhaosow.craftmastery.gui.RecipeTreeTabEditorScreen;
import com.khimkhaosow.craftmastery.tabs.TabManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Основной GUI интерфейс мода CraftMastery - Стилизованная книга
 */
public class GuiCraftMastery extends GuiScreen implements GuiYesNoCallback {
    // Текстура книги
    private static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("craftmastery", "textures/gui/cniga.png");
    // Текстуры для элементов интерфейса (пока используем стандартные)
    // private static final ResourceLocation GUI_ELEMENTS = new ResourceLocation("craftmastery", "textures/gui/elements.png");
    // Размеры интерфейса (можно масштабировать относительно размера окна)
    private static final int BOOK_WIDTH = 400; // Примерная ширина книги
    private static final int BOOK_HEIGHT = 300; // Примерная высота книги
    private final EntityPlayer player;
    private PlayerExperienceData experienceData;

    // Виджеты
    private RecipeTreeWidget recipeTree; // Пока старый, обновим позже
    private TabBarWidget tabBar;
    private PointsPanelWidget pointsPanel;
    private LevelProgressWidget levelProgress;

    // Текущая страница интерфейса
    public enum Page {
        MAIN, TABS, SETTINGS, SEARCH
    }
    private Page currentPage = Page.MAIN;

    // Позиции элементов (зависят от размера окна)
    private int bookX;
    private int bookY;
    private int bookWidth;
    private int bookHeight;
    private int leftPanelWidth;
    private int topPanelHeight;
    private int bottomPanelHeight;

    // Навигация по дереву рецептов (берем из старого кода)
    private float offsetX = 0;
    private float offsetY = 0;
    private float scale = 1.0f;

    // Кнопки
    private GuiButton settingsGearButton;
    private GuiButton backButton;
    private GuiButton addRecipeButton;

    private RecipeEntry pendingStudyRecipe;

    // Прокрутка вкладок
    private int tabScrollOffset = 0;

    // Поиск
    private GuiTextField searchField;
    private List<RecipeEntry> searchResults;
    private int searchPage = 0;
    private static final int RESULTS_PER_PAGE = 8;

    public GuiCraftMastery(EntityPlayer player) {
        this.player = player;
        this.experienceData = ExperienceManager.getInstance().getPlayerData(player);
        this.recipeTree = new RecipeTreeWidget(Minecraft.getMinecraft(), player, experienceData);

        // Пример инициализации виджетов с новыми позициями (позиции будут уточнены в initGui)
        // TabBarWidget нужно будет обновить, чтобы он знал, где рисоваться
        // PointsPanelWidget - левая панель
        // LevelProgressWidget - нижняя панель
        // AdminPanelWidget - возможно, не нужен, или как часть левой панели
    }

    @Override
    public void initGui() {
        super.initGui();

        // Рассчитываем позиции и размеры элементов интерфейса
        calculateGuiDimensions();

        // --- НОВАЯ ЛОГИКА РАЗМЕЩЕНИЯ TabBarWidget ---
        int iconSize = 22; // из addCoreButtons
        int topY = 12;     // из addCoreButtons
        int topButtonBottom = topY + iconSize; // Где заканчиваются верхние кнопки

        // Позиция вкладок - чуть ниже верхних кнопок
        int tabBarY = topButtonBottom + 10; // Отступ 10 пикселей

        // Высота вкладок из TabBarWidget
        int tabBarHeight = TabBarWidget.getPreferredHeight(); // TAB_HEIGHT = 32

        // Позиция нижней панели (из addCoreButtons)
        int bottomPanelTop = height - bottomPanelHeight; // Где начинается нижняя панель (backButton)
        int backButtonHeight = 20; // Высота кнопки "Назад"

        // Проверяем, помещаются ли вкладки между верхними кнопками и нижней панелью
        int availableSpace = bottomPanelTop - topButtonBottom;
        if (availableSpace >= tabBarHeight) {
            // Хватает места, размещаем вкладки между
            // tabBarY уже рассчитан как чуть ниже верхних кнопок
        } else {
            // Не хватает места, размещаем вкладки ближе к верхним кнопкам
            tabBarY = topButtonBottom + 2; // Минимальный отступ
        }

        // Убедимся, что вкладки не наезжают на кнопку "Назад"
        if (tabBarY + tabBarHeight > bottomPanelTop - backButtonHeight - 5) { // 5 - отступ от кнопки
            tabBarY = bottomPanelTop - backButtonHeight - tabBarHeight - 5;
        }

        // Создаем TabBarWidget с новой позицией
        this.tabBar = new TabBarWidget(Minecraft.getMinecraft(), player, width, tabBarY, this::handleTabSelection);
        this.tabBar.updateTabList();
        // --- КОНЕЦ НОВОЙ ЛОГИКИ ---

        refreshExperienceWidgets();
        updateButtonsForPage();

        if (currentPage == Page.SEARCH && searchField == null) {
            int centerX = width / 2;
            searchField = new GuiTextField(5, Minecraft.getMinecraft().fontRenderer, centerX - 100, topPanelHeight + 20, 200, 20);
            searchField.setFocused(true);
            searchField.setMaxStringLength(50);
        }
    }

    private void performSearch() {
        if (searchField == null) {
            return;
        }
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            searchResults = null;
            searchPage = 0;
            updateButtonsForPage();
            return;
        }

        List<RecipeEntry> allRecipes = RecipeManager.getInstance().searchRecipes(query);
        searchResults = new ArrayList<>();
        for (RecipeEntry entry : allRecipes) {
            ItemStack result = entry.getRecipeResult();
            if (!result.isEmpty() && result.getDisplayName().toLowerCase().contains(query)) {
                searchResults.add(entry);
            }
        }

        searchPage = 0;
        updateButtonsForPage();
    }

    private void calculateGuiDimensions() {
        // Рассчитываем размеры интерфейса относительно размера окна
        // Можно использовать проценты или фиксированные отступы
        // Примерные пропорции для книги:
        // - Левая панель: 60-80 пикселей
        // - Верхняя панель: 40-50 пикселей
        // - Нижняя панель: 30-40 пикселей
        // - Центральная область (книга): остальное
        leftPanelWidth = Math.min(80, width / 8); // Примерно 1/8 ширины или 80px, в зависимости от чего меньше
        topPanelHeight = Math.min(50, height / 10);
        bottomPanelHeight = Math.min(40, height / 10);

        bookWidth = width - 2 * leftPanelWidth; // Основная область книги
        bookHeight = height - topPanelHeight - bottomPanelHeight;
        bookX = leftPanelWidth; // Центрируем книгу по горизонтали (или оставим слева?)
        bookY = topPanelHeight; // Ниже верхней панели
    }

    private void updateButtonsForPage() {
        buttonList.clear();

        // Добавляем основные кнопки (шестеренки, назад, плюс)
        addCoreButtons();

        // Поле поиска (только на странице поиска)
        if (currentPage == Page.SEARCH && searchField != null) {
            int centerX = width / 2;
            searchField.x = centerX - 100;
            searchField.y = topPanelHeight + 20;
        }

        // Добавляем кнопки в зависимости от страницы
        switch (currentPage) {
            case MAIN:
                // На главной только основные элементы управления и дерево
                break;
            case TABS:
                break;
            case SETTINGS:
                addSettingsPageButtons();
                break;
            case SEARCH:
                addSearchPageButtons();
                break;
        }
    }

    // --- Остальные методы для кнопок (addTabPageButtons, addSettingsPageButtons, addSearchPageButtons) ---
    // (Код этих методов остается прежним, но возможно, их позиции нужно будет пересмотреть)
    // ... (копируем из старого кода с минимальными изменениями) ...

    private static final int ADMIN_BTN_MANAGE_TABS = 410;
    private static final int ADMIN_BTN_EXPERIENCE = 411;
    private static final int ADMIN_BTN_RESET = 412;

    private void addSettingsPageButtons() {
        int x = 50;
        int y = 40;
        int width = 200;

        if (PermissionManager.getInstance().hasPermission(player, PermissionType.MANAGE_TABS)) {
            buttonList.add(new GuiButton(ADMIN_BTN_MANAGE_TABS, x, y, width, 20, "Управление вкладками"));
            y += 25;
        }

        if (PermissionManager.getInstance().hasPermission(player, PermissionType.ADMIN_SETTINGS)) {
            buttonList.add(new GuiButton(ADMIN_BTN_EXPERIENCE, x, y, width, 20, "Множители и опыт"));
            y += 25;
            buttonList.add(new GuiButton(ADMIN_BTN_RESET, x, y, width, 20, "Сброс данных игрока"));
            y += 25;
        } else {
            GuiButton info = new GuiButton(409, x, y, width, 20, TextFormatting.RED + "Нет доступа к админ-настройкам");
            info.enabled = false;
            buttonList.add(info);
            y += 25;
        }
    }
    private void addSearchPageButtons() {
        int centerX = width / 2;
        // Кнопки управления поиском
        buttonList.add(new GuiButton(500, centerX - 155, topPanelHeight + 40, 100, 20, "Поиск"));
        buttonList.add(new GuiButton(501, centerX + 55, topPanelHeight + 40, 100, 20, "Очистить"));
        // Если нет результатов поиска, показываем подсказку
        if (searchResults == null || searchResults.isEmpty()) {
            return;
        }
        // Вычисляем диапазон для текущей страницы
        int startIndex = searchPage * RESULTS_PER_PAGE;
        int endIndex = Math.min(startIndex + RESULTS_PER_PAGE, searchResults.size());
        int totalPages = (searchResults.size() - 1) / RESULTS_PER_PAGE + 1;
        // Результаты поиска
        int resultY = topPanelHeight + 80;
        for (int i = startIndex; i < endIndex; i++) {
            RecipeEntry recipe = searchResults.get(i);
            ItemStack result = recipe.getRecipeResult();
            if (!result.isEmpty()) {
                String recipeName = result.getDisplayName();
                boolean isStudied = recipe.isStudiedByPlayer(player.getUniqueID());
                boolean canStudy = recipe.canPlayerStudy(player, experienceData);
                // Определяем цвет и статус
                TextFormatting color;
                String status;
                if (isStudied) {
                    color = TextFormatting.GREEN;
                    status = " (изучен)";
                } else if (canStudy) {
                    color = TextFormatting.YELLOW;
                    status = " (" + recipe.getRequiredLearningPoints() + " очков)";
                } else {
                    color = TextFormatting.RED;
                    status = " (недоступен)";
                }
                GuiButton resultButton = new GuiButton(600 + (i - startIndex),
                    centerX - 150, resultY, 300, 20,
                    color + recipeName + status);
                buttonList.add(resultButton);
                resultY += 25;
            }
        }
        // Кнопки навигации по страницам
        if (totalPages > 1) {
            if (searchPage > 0) {
                buttonList.add(new GuiButton(502, centerX - 100, height - bottomPanelHeight - 25, 80, 20, "< Назад"));
            }
            if (searchPage < totalPages - 1) {
                buttonList.add(new GuiButton(503, centerX + 20, height - bottomPanelHeight - 25, 80, 20, "Далее >"));
            }
            // Счетчик страниц
            String pageInfo = String.format("Страница %d из %d", searchPage + 1, totalPages);
            drawCenteredString(Minecraft.getMinecraft().fontRenderer, pageInfo,
                centerX, height - bottomPanelHeight - 40, 0xFFFFFF);
        }
    }

    // --- Обработка действий кнопок ---
    // (Код actionPerformed и связанные методы остается прежним)
    // ... (копируем из старого кода с минимальными изменениями) ...

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        ButtonAction action = getButtonAction(button.id);
        if (action != null) {
            handleButtonAction(action, button);
        }
    }

    private enum ButtonAction {
        SETTINGS,          // Пользовательские настройки
        ADMIN_SETTINGS,    // Админ-панель
        BACK,
        ADD_RECIPE,
        SEARCH,
        CLEAR_SEARCH,
        SEARCH_PREV,
        SEARCH_NEXT,
        RECIPE,
        EDIT_TREE,
        ADMIN_MANAGE_TABS,
        ADMIN_EXPERIENCE,
        ADMIN_RESET_DATA,
        NONE
    }

    private ButtonAction getButtonAction(int buttonId) {
        switch (buttonId) {
            case 10: return ButtonAction.SETTINGS;
            case 11: return ButtonAction.ADMIN_SETTINGS;
            case 12: return ButtonAction.BACK;
            case 13: return ButtonAction.ADD_RECIPE;
            case 14: return ButtonAction.ADMIN_MANAGE_TABS;
            case 500: return ButtonAction.SEARCH;
            case 501: return ButtonAction.CLEAR_SEARCH;
            case 502: return ButtonAction.SEARCH_PREV;
            case 503: return ButtonAction.SEARCH_NEXT;
            case 600: case 601: case 602: case 603: case 604:
            case 605: case 606: case 607: case 608: case 609:
                return ButtonAction.RECIPE;
            case ADMIN_BTN_MANAGE_TABS: return ButtonAction.ADMIN_MANAGE_TABS;
            case ADMIN_BTN_EXPERIENCE: return ButtonAction.ADMIN_EXPERIENCE;
            case ADMIN_BTN_RESET: return ButtonAction.ADMIN_RESET_DATA;
            default: return ButtonAction.NONE;
        }
    }

    private void handleButtonAction(ButtonAction action, GuiButton button) {
        switch (action) {
            case SETTINGS:
                openPlayerSettings();
                break;
            case ADMIN_SETTINGS:
                if (PermissionManager.getInstance().hasPermission(player, PermissionType.ADMIN_SETTINGS)) {
                    openAdminInterface();
                } else {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "Недостаточно прав для админ-настроек"));
                }
                break;
            case BACK:
                if (currentPage != Page.MAIN) {
                    currentPage = Page.MAIN;
                    updateButtonsForPage();
                } else {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                }
                break;
            case ADD_RECIPE:
                if (PermissionManager.getInstance().hasPermission(player, PermissionType.CREATE_RECIPES)) {
                    openTreeEditor();
                } else {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "Недостаточно прав для добавления рецептов"));
                }
                break;
            case SEARCH:
                performSearch();
                break;
            case CLEAR_SEARCH:
                if (searchField != null) {
                    searchField.setText("");
                    searchResults = null;
                    searchPage = 0;
                }
                updateButtonsForPage();
                break;
            case SEARCH_PREV:
                if (searchPage > 0) {
                    searchPage--;
                    updateButtonsForPage();
                }
                break;
            case SEARCH_NEXT:
                if (searchResults != null &&
                    searchPage < (searchResults.size() - 1) / RESULTS_PER_PAGE) {
                    searchPage++;
                    updateButtonsForPage();
                }
                break;
            case RECIPE:
                handleRecipeAction(button.id - 600);
                break;
            case EDIT_TREE:
                openTreeEditor();
                break;
            case ADMIN_MANAGE_TABS:
                openTabEditor();
                break;
            case ADMIN_EXPERIENCE:
                if (PermissionManager.getInstance().hasPermission(player, PermissionType.ADMIN_SETTINGS)) {
                    Minecraft.getMinecraft().displayGuiScreen(new GuiAdminSettingsScreen(this, player));
                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Нет прав"));
                }
                break;
            case ADMIN_RESET_DATA:
                if (PermissionManager.getInstance().hasPermission(player, PermissionType.ADMIN_SETTINGS)) {
                    ExperienceManager.getInstance().resetPlayerData(player);
                    player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Данные опыта сброшены"));
                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Нет прав"));
                }
                break;
        }
    }

    // --- Остальные вспомогательные методы ---
    // (Код методов handleTabAction, handleRecipeAction, openAdminInterface, openRecipeCreation, performSearch, showRecipeDetails, handleTabAction, getResultsWord и InputHandler остается прежним)
    // ... (копируем из старого кода с минимальными изменениями) ...

    private void handleRecipeAction(int recipeIndex) {
        String searchText = searchField != null ? searchField.getText() : "";
        List<RecipeEntry> searchResults = RecipeManager.getInstance().searchRecipes(searchText);
        if (recipeIndex >= 0 && recipeIndex < searchResults.size()) {
            RecipeEntry recipe = searchResults.get(recipeIndex);
            showRecipeDetails(recipe);
        }
    }
    private void openAdminInterface() {
        currentPage = Page.SETTINGS;
        updateButtonsForPage();
    }

    private void openPlayerSettings() {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPlayerSettings(this));
    }
    private void openTreeEditor() {
        if (recipeTree != null) {
            recipeTree.setEditingEnabled(true);
        }
        RecipeTreeEditorScreen editor = new RecipeTreeEditorScreen(this, player, recipeTree);
        Minecraft.getMinecraft().displayGuiScreen(editor);
    }

    public void refreshTabs(String focusTabId) {
        if (tabBar != null) {
            tabBar.updateTabList();
            if (focusTabId != null) {
                tabBar.setActiveTab(focusTabId);
            }
        }
        if (recipeTree != null) {
            recipeTree.refresh();
            if (focusTabId != null) {
                recipeTree.setActiveTab(focusTabId);
            }
        }
        updateButtonsForPage();
    }

    public void onRecipeEditorClosed(boolean reload, String focusTab) {
        if (reload && recipeTree != null) {
            recipeTree.refresh();
            if (focusTab != null) {
                recipeTree.setActiveTab(focusTab);
                if (tabBar != null) {
                    tabBar.updateTabList();
                    tabBar.setActiveTab(focusTab);
                }
            }
        }
        if (tabBar != null) {
            tabBar.updateTabList();
        }
        updateButtonsForPage();
    }

    private void handleTabSelection(String tabId) {
        if (tabId == null || tabId.isEmpty()) {
            currentPage = Page.MAIN;
        } else {
            switch (tabId.toLowerCase()) {
                case "main":
                case "default":
                    currentPage = Page.MAIN;
                    break;
                case "settings":
                    currentPage = Page.SETTINGS;
                    break;
                case "search":
                    currentPage = Page.SEARCH;
                    break;
                default:
                    currentPage = Page.TABS;
                    break;
            }
        }

        if (recipeTree != null) {
            recipeTree.setActiveTab(tabId);
        }

        updateButtonsForPage();
    }
    private void handleTabAction(com.khimkhaosow.craftmastery.tabs.Tab tab) {
        if (tab.isStudiedByPlayer(player.getUniqueID())) {
            // Показать информацию о вкладке
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.GREEN + "Вкладка '" + tab.getName() + "' уже изучена"));
        } else if (tab.canPlayerStudy(player.getUniqueID(), experienceData)) {
            // Изучить вкладку
            boolean success = TabManager.getInstance().studyTab(player, tab.getId());
            if (success) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.GREEN + "Вкладка '" + tab.getName() + "' изучена!"));
                experienceData = ExperienceManager.getInstance().getPlayerData(player); // Обновляем данные
                updateButtonsForPage();
            } else {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.RED + "Не удалось изучить вкладку"));
            }
        } else {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.RED + "Недостаточно спец-очков или прав доступа"));
        }
    }
    private void showRecipeDetails(RecipeEntry recipe) {
        if (recipe == null) return;
        // Показываем детали рецепта в чате или создаем отдельное окно
        player.sendMessage(new net.minecraft.util.text.TextComponentString(
            TextFormatting.GOLD + "=== " + recipe.getRecipeResult().getDisplayName() + " ==="));
        if (recipe.isStudiedByPlayer(player.getUniqueID())) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.GREEN + "Статус: Изучен"));
        } else if (recipe.canPlayerStudy(player, experienceData)) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.YELLOW + "Статус: Доступен для изучения"));
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.WHITE + "Стоимость: " + recipe.getRequiredLearningPoints() + " очков изучения"));
        } else {
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.RED + "Статус: Недоступен"));
        }
        player.sendMessage(new net.minecraft.util.text.TextComponentString(
            TextFormatting.WHITE + "Требуемый уровень: " + recipe.getRequiredLevel()));
    }
    private String getResultsWord(int count) {
        // Правильное склонение слова "результат"
        count = Math.abs(count) % 100;
        int rem = count % 10;
        if (count > 10 && count < 20) return "результатов";
        if (rem > 1 && rem < 5) return "результата";
        if (rem == 1) return "результат";
        return "результатов";
    }

    private static class InputHandler {
        private final GuiCraftMastery gui;
        public InputHandler(GuiCraftMastery gui) {
            this.gui = gui;
        }
        public boolean handleKeyboard(char typedChar, int keyCode) throws IOException {
            // Обработка Escape - всегда работает
            if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                if (gui.currentPage != Page.MAIN) {
                    gui.currentPage = Page.MAIN;
                    gui.updateButtonsForPage();
                    return true;
                }
                return false;
            }
            // Обработка поиска
            if (gui.currentPage == Page.SEARCH && gui.searchField != null) {
                if (gui.searchField.isFocused()) {
                    if (keyCode == org.lwjgl.input.Keyboard.KEY_RETURN) {
                        gui.performSearch();
                        return true;
                    }
                    gui.searchField.textboxKeyTyped(typedChar, keyCode);
                    return true;
                }
            }
            // Горячие клавиши
            if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) ||
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL)) {
                switch (keyCode) {
                    case org.lwjgl.input.Keyboard.KEY_F: // Ctrl+F - поиск
                        gui.currentPage = Page.SEARCH;
                        gui.updateButtonsForPage();
                        if (gui.searchField != null) {
                            gui.searchField.setFocused(true);
                        }
                        return true;
                    case org.lwjgl.input.Keyboard.KEY_H: // Ctrl+H - домой
                        gui.currentPage = Page.MAIN;
                        gui.updateButtonsForPage();
                        return true;
                }
            }
            return false;
        }
        public boolean handleMouseWheel(int mouseWheel) {
            if (gui.currentPage == Page.MAIN && mouseWheel != 0) {
                // Масштабирование дерева рецептов
                if (mouseWheel > 0) {
                    gui.scale *= 1.1f;
                } else {
                    gui.scale *= 0.9f;
                }
                gui.scale = Math.max(0.5f, Math.min(gui.scale, 2.0f));
                return true;
            }
            return false;
        }
    }
    private final InputHandler inputHandler = new InputHandler(this);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Рисуем фон книги (всё окно)
        drawBookBackground();

        // Рисуем верхнюю панель (вкладки)
        drawTopPanel();
        if (tabBar != null) {
            tabBar.draw(mouseX, mouseY);
        }

        // Рисуем левую боковую панель (настройки, очки)
        drawLeftPanel();
        if (pointsPanel != null) {
            pointsPanel.draw();
        }

        // Рисуем нижнюю панель (прогресс)
        drawBottomPanel();

        // Рисуем содержимое страницы (дерево рецептов в центральной области)
        drawPageContent(mouseX, mouseY);

        // Рисуем кнопки
        for (GuiButton button : buttonList) {
            if (button != null) {
                button.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
            }
        }

        // Рисуем поле поиска (если активно)
        if (currentPage == Page.SEARCH && searchField != null) {
            searchField.drawTextBox();
        }
    }

    private void drawBookBackground() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (BOOK_TEXTURE != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(BOOK_TEXTURE);
            // Рисуем текстуру на всё окно
            drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        } else {
            // Рисуем простой фон
            drawRect(0, 0, width, height, 0xFF8B4513); // Коричневый цвет книги
            drawRect(20, 20, width - 20, height - 20, 0xFFF5DEB3); // Бежевый цвет страниц
        }
    }

    private void drawTopPanel() {
        // Простой прямоугольник или можно использовать TabBarWidget.draw()
        // drawRect(0, 0, width, topPanelHeight, 0x80000000); // Полупрозрачный фон
        // tabBar.draw(mouseX, mouseY); // Вызываем draw у виджета, если он готов к этому
        // Пока просто рисуем границу или фон
        // drawRect(0, 0, width, topPanelHeight, 0x40000000); // Примерный фон верхней панели
    }

    private void drawLeftPanel() {
        // Простой прямоугольник или можно использовать PointsPanelWidget.draw()
        // drawRect(0, topPanelHeight, leftPanelWidth, height - bottomPanelHeight, 0x80000000); // Полупрозрачный фон
        // pointsPanel.draw(); // Вызываем draw у виджета
        // Пока просто рисуем границу или фон
        // drawRect(0, topPanelHeight, leftPanelWidth, height - bottomPanelHeight, 0x40000000); // Примерный фон левой панели
    }

    private void drawBottomPanel() {
        drawRect(0, height - bottomPanelHeight, width, height, 0x80000000);
        if (levelProgress != null) {
            levelProgress.draw();
        }
    }

    private void drawPageContent(int mouseX, int mouseY) {
        switch (currentPage) {
            case MAIN:
                drawMainPage(mouseX, mouseY);
                break;
            case TABS:
                drawTabsPage();
                break;
            case SETTINGS:
                drawSettingsPage();
                break;
            case SEARCH:
                drawSearchPage();
                break;
        }
    }

    private void drawMainPage(int mouseX, int mouseY) {
        // Рисуем центральную область (страницу книги)
        // drawRect(bookX, bookY, bookX + bookWidth, bookY + bookHeight, 0x22FFFFFF); // Фон страницы (временно)

        // Рисуем виджеты в нужных местах
        // PointsPanelWidget (левая панель)
        // TabBarWidget (верхняя панель)
        if (tabBar != null) {
            GlStateManager.pushMatrix();
            tabBar.draw(mouseX, mouseY);
            GlStateManager.popMatrix();
        }

        // Дерево рецептов (центральная область книги)
        if (recipeTree != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(bookX, bookY, 0);
            recipeTree.draw(bookWidth, bookHeight, offsetX, offsetY, scale);
            GlStateManager.popMatrix();
        }

        // Рисуем стрелки навигации (если они есть) - они уже добавлены в buttonList
        // Их отрисовка происходит в drawScreen -> for (GuiButton button : buttonList)
    }

    private void drawTabsPage() {
        drawString(Minecraft.getMinecraft().fontRenderer, TextFormatting.GOLD + "Вкладки", 10, 25, 0xFFFFFF);
        List<com.khimkhaosow.craftmastery.tabs.Tab> tabs = TabManager.getInstance().getAvailableTabs(player);
        drawString(Minecraft.getMinecraft().fontRenderer, TextFormatting.WHITE + "Доступно: " + tabs.size(), 10, height - 60, 0xFFFFFF);
    }

    private void drawSettingsPage() {
        drawString(Minecraft.getMinecraft().fontRenderer, TextFormatting.GOLD + "Настройки", 10, 25, 0xFFFFFF);
        drawString(Minecraft.getMinecraft().fontRenderer, TextFormatting.GRAY + "Используйте кнопки слева для изменения параметров.", 10, 45, 0xFFFFFF);
    }

    private void drawSearchPage() {
        int centerX = width / 2;
        drawCenteredString(Minecraft.getMinecraft().fontRenderer,
            TextFormatting.GOLD + "Поиск рецептов", centerX, topPanelHeight + 5, 0xFFFFFF);

        if (searchField != null) {
            searchField.drawTextBox();
            if (searchField.getText().isEmpty() && !searchField.isFocused()) {
                drawString(Minecraft.getMinecraft().fontRenderer,
                    TextFormatting.GRAY + "Введите название рецепта...",
                    searchField.x + 4, searchField.y + 6, 0x88FFFFFF);
            }
        }

        int totalResults = searchResults == null ? 0 : searchResults.size();
        String resultInfo = totalResults == 0
            ? TextFormatting.RED + "Ничего не найдено"
            : TextFormatting.YELLOW + String.format("Найдено %d %s", totalResults, getResultsWord(totalResults));
        drawCenteredString(Minecraft.getMinecraft().fontRenderer,
            resultInfo, centerX, topPanelHeight + 45, 0xFFFFFF);

        if (searchResults != null && !searchResults.isEmpty()) {
            int startIndex = searchPage * RESULTS_PER_PAGE;
            int endIndex = Math.min(startIndex + RESULTS_PER_PAGE, searchResults.size());
            int textY = topPanelHeight + 70;
            for (int i = startIndex; i < endIndex; i++) {
                RecipeEntry result = searchResults.get(i);
                drawString(Minecraft.getMinecraft().fontRenderer,
                    TextFormatting.WHITE + result.getDisplayName(), centerX - 140, textY, 0xFFFFFF);
                textY += 20;
            }
        } else {
            drawCenteredString(Minecraft.getMinecraft().fontRenderer,
                TextFormatting.GRAY + "Результаты будут отображены здесь", centerX, topPanelHeight + 70, 0xFFFFFF);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mouseWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (mouseWheel != 0) {
            inputHandler.handleMouseWheel(mouseWheel);
        }
        // Обработка перетаскивания для дерева рецептов
        int eventButton = Mouse.getEventButton();
        boolean buttonState = Mouse.getEventButtonState();
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;

        if (currentPage == Page.MAIN && recipeTree != null) {
            boolean insideTree = mouseX >= bookX && mouseX <= bookX + bookWidth
                    && mouseY >= bookY && mouseY <= bookY + bookHeight;

            if (eventButton == 0 && buttonState && insideTree) {
                float relativeX = mouseX - (bookX + bookWidth / 2.0f);
                float relativeY = mouseY - (bookY + bookHeight / 2.0f);

                if (!recipeTree.handleEditorClick(relativeX, relativeY, offsetX, offsetY, scale, eventButton)) {
                    nodeDragActive = recipeTree.beginNodeDrag(relativeX, relativeY, offsetX, offsetY, scale);
                }
            } else if (eventButton == 0 && !buttonState) {
                if (recipeTree.isLinking()) {
                    float relativeX = mouseX - (bookX + bookWidth / 2.0f);
                    float relativeY = mouseY - (bookY + bookHeight / 2.0f);
                    recipeTree.completeLink(relativeX, relativeY, offsetX, offsetY, scale);
                } else if (nodeDragActive) {
                    recipeTree.endNodeDrag(true);
                    nodeDragActive = false;
                }
            } else if (eventButton == -1 && Mouse.isButtonDown(0)) {
                if (recipeTree.isLinking()) {
                    float relativeX = mouseX - (bookX + bookWidth / 2.0f);
                    float relativeY = mouseY - (bookY + bookHeight / 2.0f);
                    recipeTree.updateLinkCursor(relativeX, relativeY, offsetX, offsetY, scale);
                } else if (nodeDragActive) {
                    recipeTree.updateNodeDrag(mouseX - (bookX + bookWidth / 2.0f),
                            mouseY - (bookY + bookHeight / 2.0f), offsetX, offsetY, scale);
                }
            } else if (eventButton == 2 && buttonState && insideTree) {
                recipeTree.startDragging(mouseX, mouseY);
                isDragging = true;
            } else if (eventButton == 2 && !buttonState && isDragging) {
                recipeTree.stopDragging();
                isDragging = false;
            } else if (eventButton == -1 && Mouse.isButtonDown(2) && isDragging) {
                int[] delta = recipeTree.handleMouseDrag(mouseX, mouseY);
                if (delta != null) {
                    offsetX += delta[0] / scale;
                    offsetY += delta[1] / scale;
                }
            } else if (eventButton == 1 && buttonState) {
                if (recipeTree.isLinking()) {
                    recipeTree.cancelLink();
                }
            }
        }
    }

    private boolean isDragging = false;
    private boolean nodeDragActive = false;

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Проверяем клики по виджетам в порядке приоритета
        // ВНИМАНИЕ: Нужно обновить логику кликов, чтобы учитывать новые позиции
        // if (adminPanel != null && adminPanel.handleMouseClick(mouseX, mouseY, mouseButton)) {
        //     return;
        // }
        // tabBar.handleMouseClick - нужно обновить
        if (tabBar != null && tabBar.containsY(mouseY)) {
            if (tabBar.handleMouseClick(mouseX, mouseY, mouseButton)) {
                currentPage = tabBar.getCurrentPage();
                updateButtonsForPage();
                return;
            }
        }

        // pointsPanel.handleMouseClick - нужно обновить
        if (mouseX < leftPanelWidth && mouseY > topPanelHeight && mouseY < height - bottomPanelHeight) {
            // Клик в левую панель
            // pointsPanel.handleMouseClick(mouseX, mouseY, mouseButton); // Вызов для виджета
            // Обработка клика по элементам левой панели (шестеренки, очки)
        }

        // levelProgress.handleMouseClick - нужно обновить
        if (mouseY >= height - bottomPanelHeight) {
            // Клик в нижнюю панель
            // levelProgress.handleMouseClick(mouseX, mouseY, mouseButton); // Вызов для виджета
            // Обработка клика по элементам нижней панели (прогресс-бар)
        }

        // Обработка клика по дереву рецептов (центральная область)
        if (currentPage == Page.MAIN && mouseX >= bookX && mouseX <= bookX + bookWidth &&
            mouseY >= bookY && mouseY <= bookY + bookHeight) {
            float relativeX = mouseX - (bookX + bookWidth / 2.0f);
            float relativeY = mouseY - (bookY + bookHeight / 2.0f);

            // Переводим координаты клика в систему координат дерева
            boolean doubleClick = recipeTree.handleMouseClick((int)relativeX, (int)relativeY, offsetX, offsetY, scale);
            if (recipeTree.hasSelectedRecipe()) {
                RecipeEntry selected = recipeTree.getSelectedRecipe();
                showRecipeDetails(selected);
                if (doubleClick && selected != null && selected.canPlayerStudy(player, experienceData)) {
                    openStudyConfirmation(selected);
                }
            }
            return; // Обработали клик по дереву
        }

        // Проверяем клик по полю поиска
        if (currentPage == Page.SEARCH && searchField != null &&
            mouseX >= searchField.x && mouseX <= searchField.x + searchField.width &&
            mouseY >= searchField.y && mouseY <= searchField.y + searchField.height) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        // Обрабатываем клики по кнопкам интерфейса
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isEditorDragKeyDown() {
        return RecipeTreeEditorScreen.isEditorOpen() && org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
    }

    private void openTabEditor() {
        Minecraft.getMinecraft().displayGuiScreen(new RecipeTreeTabEditorScreen(new RecipeTreeEditorScreen(this, player, recipeTree)));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // Игра не ставится на паузу
    }

    private void addCoreButtons() {
        int iconSize = 22;
        int spacing = 8;
        int topY = 12;

        settingsGearButton = new GuiButton(10, 10, topY, iconSize, iconSize, "\u2699");
        buttonList.add(settingsGearButton);

        GuiButton adminSettingsButton = new GuiButton(11, settingsGearButton.x + iconSize + spacing, topY, iconSize, iconSize, "\u26A0");
        buttonList.add(adminSettingsButton);

        backButton = new GuiButton(12, 10, height - bottomPanelHeight - 28, 60, 20, "Назад");
        buttonList.add(backButton);

        int plusSize = 28;
        addRecipeButton = new GuiButton(13, width - plusSize - 14, topY - 1, plusSize, plusSize, "+");
        buttonList.add(addRecipeButton);

        if (PermissionManager.getInstance().hasPermission(player, PermissionType.MANAGE_TABS)) {
            GuiButton manageTabsButton = new GuiButton(14, width - (plusSize * 2) - spacing - 14, topY - 1, plusSize, plusSize, "T");
            buttonList.add(manageTabsButton);
        }
    }

    private void openStudyConfirmation(RecipeEntry recipe) {
        if (recipe == null) {
            return;
        }
        this.pendingStudyRecipe = recipe;
        String title = TextFormatting.GOLD + "Изучить рецепт?";
        String details = recipe.getDisplayName() + TextFormatting.RESET + " — стоимость: " +
                recipe.getRequiredLearningPoints() + " очков";
        GuiYesNo confirmation = new GuiYesNo(this, title, details,
                TextFormatting.GREEN + "Изучить", TextFormatting.RED + "Отмена", 0);
        mc.displayGuiScreen(confirmation);
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result && pendingStudyRecipe != null) {
            boolean success = RecipeManager.getInstance().studyRecipe(player, pendingStudyRecipe.getRecipeId());
            if (success) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "Рецепт '" + pendingStudyRecipe.getDisplayName() + "' изучен."));
                experienceData = ExperienceManager.getInstance().getPlayerData(player);
                recipeTree = new RecipeTreeWidget(mc, player, experienceData);
                refreshExperienceWidgets();
                updateButtonsForPage();
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "Не удалось изучить рецепт."));
            }
        }
        pendingStudyRecipe = null;
        mc.displayGuiScreen(this);
    }

    private void refreshExperienceWidgets() {
        if (mc == null) {
            return;
        }
        int panelWidth = Math.max(80, leftPanelWidth + 24);
        int panelHeight = Math.min(120, height - topPanelHeight - bottomPanelHeight - 20);
        int panelY = height - bottomPanelHeight - panelHeight - 8;
        pointsPanel = new PointsPanelWidget(mc, experienceData, 10, panelY, panelHeight, panelWidth);

        int progressWidth = 220;
        int progressX = (width - progressWidth) / 2;
        int progressY = height - bottomPanelHeight + 8;
        levelProgress = new LevelProgressWidget(mc, experienceData, progressX, progressY, progressWidth);
    }

    private int pointsPanelY() {
        return height - bottomPanelHeight - 8;
    }
}