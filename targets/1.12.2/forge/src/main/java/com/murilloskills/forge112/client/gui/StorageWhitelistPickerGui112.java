package com.murilloskills.forge112.client.gui;

import com.murilloskills.forge112.client.config.ClientUltmineConfig;
import com.murilloskills.forge112.network.ModNetwork112;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.murilloskills.forge112.client.gui.Forge112UiSupport.clamp;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.drawPanelBorder;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.flatButton;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.invisibleButton;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.inside;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.renderCornerAccents;
import static com.murilloskills.forge112.client.gui.Forge112UiSupport.renderItemStack;

public final class StorageWhitelistPickerGui112 extends GuiScreen {
    private static final int BACK = 35000;
    private static final int OPTION_BASE = 35100;
    private final GuiScreen parent;
    private final List<String> allIds = new ArrayList<String>();
    private final List<String> ids = new ArrayList<String>();
    private GuiTextField searchField;
    private int scroll;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int rowX;
    private int rowY;
    private int rowW;
    private int rowH;
    private int visibleRows;

    public StorageWhitelistPickerGui112(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        ClientUltmineConfig.load();
        String query = searchField == null ? "" : searchField.getText();
        buttonList.clear();
        allIds.clear();
        for (ResourceLocation id : Item.REGISTRY.getKeys()) {
            Item item = Item.REGISTRY.getObject(id);
            if (id != null && item != null && item != Items.AIR) {
                allIds.add(id.toString());
            }
        }
        Collections.sort(allIds);
        filter(query);
        panelW = Math.min(620, width - 20);
        panelH = Math.min(390, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        rowX = panelX + 14;
        rowY = panelY + 86;
        rowW = panelW - 28;
        rowH = 22;
        visibleRows = Math.max(1, (panelH - 128) / (rowH + 3));
        scroll = clamp(scroll, 0, Math.max(0, ids.size() - visibleRows));
        buttonList.add(flatButton(BACK, panelX + panelW - 82, panelY + panelH - 28, 70, 20, "Back"));
        searchField = new GuiTextField(7, fontRenderer, panelX + 14, panelY + 48, panelW - 28, 18);
        searchField.setText(query);
        searchField.setMaxStringLength(80);
        int last = Math.min(ids.size(), scroll + visibleRows);
        for (int i = scroll; i < last; i++) {
            int local = i - scroll;
            buttonList.add(invisibleButton(OPTION_BASE + local, rowX, rowY + local * (rowH + 3), rowW, rowH));
        }
    }

    private void filter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        ids.clear();
        for (String id : allIds) {
            if (normalized.length() == 0 || id.toLowerCase(Locale.ROOT).contains(normalized)) {
                ids.add(id);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BACK) {
            ClientUltmineConfig.save();
            ModNetwork112.sendUltmineConfigToServer();
            mc.displayGuiScreen(parent);
            return;
        }
        int local = button.id - OPTION_BASE;
        int index = scroll + local;
        if (local >= 0 && index >= 0 && index < ids.size()) {
            String id = ids.get(index);
            if (ClientUltmineConfig.isStorageWhitelisted(id)) {
                ClientUltmineConfig.removeStorageWhitelistItem(id);
            } else {
                ClientUltmineConfig.addStorageWhitelistItem(id);
            }
            ClientUltmineConfig.save();
            ModNetwork112.sendUltmineConfigToServer();
            initGui();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            scroll = 0;
            initGui();
            searchField.setFocused(true);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            scroll = clamp(scroll + (delta > 0 ? -1 : 1), 0, Math.max(0, ids.size() - visibleRows));
            initGui();
        }
    }

    @Override
    public void updateScreen() {
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(0, 0, width, height, 0xC0000000);
        GuiScreen.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, Palette.SECTION_BG);
        drawPanelBorder(panelX, panelY, panelW, panelH, Palette.SECTION_BORDER);
        renderCornerAccents(panelX, panelY, panelW, panelH, 8, Palette.ACCENT_GOLD);
        GuiScreen.drawRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 32, Palette.PANEL_BG_HEADER);
        drawCenteredString(fontRenderer, "Storage Whitelist", width / 2, panelY + 10, Palette.TEXT_GOLD);
        drawString(fontRenderer, ClientUltmineConfig.getStorageWhitelist().size() + " items allowed",
                panelX + 14, panelY + 38, Palette.TEXT_MUTED);
        if (searchField != null) {
            searchField.drawTextBox();
        }
        int last = Math.min(ids.size(), scroll + visibleRows);
        for (int i = scroll; i < last; i++) {
            int local = i - scroll;
            String id = ids.get(i);
            int y = rowY + local * (rowH + 3);
            boolean selected = ClientUltmineConfig.isStorageWhitelisted(id);
            boolean hovered = inside(mouseX, mouseY, rowX, y, rowW, rowH);
            GuiScreen.drawRect(rowX + 1, y + 1, rowX + rowW - 1, y + rowH - 1,
                    selected ? Palette.SECTION_BG_ACTIVE : Palette.CARD_BG_SUBTLE);
            drawPanelBorder(rowX, y, rowW, rowH, selected ? Palette.ACCENT_GREEN : hovered ? Palette.ACCENT_GOLD : Palette.SECTION_BORDER);
            renderItemStack(mc, iconForId(id), rowX + 5, y + 3, 0.86F);
            drawString(fontRenderer, fit(id, rowW - 74), rowX + 26, y + 7,
                    selected ? Palette.TEXT_GREEN : Palette.TEXT_LIGHT);
            String state = selected ? "ON" : "ADD";
            drawString(fontRenderer, state, rowX + rowW - fontRenderer.getStringWidth(state) - 8, y + 7,
                    selected ? Palette.TEXT_GREEN : Palette.TEXT_MUTED);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private ItemStack iconForId(String id) {
        Item item = Item.getByNameOrId(id);
        return item == null ? new ItemStack(Blocks.CHEST) : new ItemStack(item);
    }

    private String fit(String text, int maxWidth) {
        String out = text == null ? "" : text;
        while (out.length() > 3 && fontRenderer.getStringWidth(out) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
