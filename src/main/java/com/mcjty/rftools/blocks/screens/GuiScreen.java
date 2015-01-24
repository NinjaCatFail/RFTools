package com.mcjty.rftools.blocks.screens;

import com.mcjty.container.GenericGuiContainer;
import com.mcjty.gui.Window;
import com.mcjty.gui.events.ButtonEvent;
import com.mcjty.gui.layout.PositionalLayout;
import com.mcjty.gui.widgets.Button;
import com.mcjty.gui.widgets.Panel;
import com.mcjty.gui.widgets.ToggleButton;
import com.mcjty.gui.widgets.Widget;
import com.mcjty.rftools.RFTools;
import com.mcjty.rftools.blocks.screens.modulesclient.ClientScreenModule;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class GuiScreen  extends GenericGuiContainer<SimpleScreenTileEntity> {
    public static final int SCREEN_WIDTH = 256;
    public static final int SCREEN_HEIGHT = 224;

    private static final ResourceLocation iconLocation = new ResourceLocation(RFTools.MODID, "textures/gui/screen.png");

    private Panel toplevel;
    private ToggleButton buttons[] = new ToggleButton[7];
    private Panel modulePanels[] = new Panel[7];
    private ClientScreenModule[] clientScreenModules = new ClientScreenModule[7];

    private int selected = -1;

    public GuiScreen(SimpleScreenTileEntity screenTileEntity, ScreenContainer container) {
        super(screenTileEntity, container);

        xSize = SCREEN_WIDTH;
        ySize = SCREEN_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();

        toplevel = new Panel(mc, this).setBackground(iconLocation).setLayout(new PositionalLayout());

        for (int i = 0 ; i < 7 ; i++) {
            buttons[i] = new ToggleButton(mc, this).setLayoutHint(new PositionalLayout.PositionalHint(30, 7 + i*18 + 1, 55, 16)).setEnabled(false);
            final int finalI = i;
            buttons[i].addButtonEvent(new ButtonEvent() {
                @Override
                public void buttonClicked(Widget parent) {
                    selectPanel(finalI);
                }
            });
            toplevel.addChild(buttons[i]);
            modulePanels[i] = null;
            clientScreenModules[i] = null;
        }

        toplevel.setBounds(new Rectangle(guiLeft, guiTop, xSize, ySize));

        window = new Window(this, toplevel);

        selected = -1;
    }

    private void selectPanel(int i) {
        if (buttons[i].isPressed()) {
            selected = i;
        } else {
            selected = -1;
        }
    }

    private void refreshButtons() {
        for (int i = 0 ; i < 7 ; i++) {
            final ItemStack slot = tileEntity.getStackInSlot(i);
            if (slot != null && slot.getItem() != null && slot.getItem() instanceof ModuleProvider) {
                buttons[i].setEnabled(true);
                ModuleProvider moduleProvider = (ModuleProvider) slot.getItem();
                Class<? extends ClientScreenModule> clientScreenModuleClass = moduleProvider.getClientScreenModule();
                if (!clientScreenModuleClass.isInstance(clientScreenModules[i])) {
                    toplevel.removeChild(modulePanels[i]);
                    try {
                        ClientScreenModule clientScreenModule = clientScreenModuleClass.newInstance();
                        clientScreenModules[i] = clientScreenModule;
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }

                    NBTTagCompound tagCompound = slot.getTagCompound();
                    if (tagCompound == null) {
                        tagCompound = new NBTTagCompound();
                    }

                    final NBTTagCompound finalTagCompound = tagCompound;
                    final int finalI = i;
                    modulePanels[i] = clientScreenModules[i].createGui(mc, this, tagCompound, new ModuleGuiChanged() {
                        @Override
                        public void updateData() {
                            slot.setTagCompound(finalTagCompound);
                            tileEntity.setInventorySlotContents(finalI, slot);
                        }
                    });
                    modulePanels[i].setLayoutHint(new PositionalLayout.PositionalHint(90, 7, 140, 130));
                    toplevel.addChild(modulePanels[i]);
                }

                buttons[i].setText(moduleProvider.getName());
            } else {
                buttons[i].setEnabled(false);
                buttons[i].setText("");
                clientScreenModules[i] = null;
                toplevel.removeChild(modulePanels[i]);
                modulePanels[i] = null;
                if (selected == i) {
                    selected = -1;
                }
            }
            if (modulePanels[i] != null) {
                modulePanels[i].setVisible(selected == i);
                buttons[i].setPressed(selected == i);
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int i, int i2) {
//        sendServerCommand(SimpleScreenTileEntity.CMD_SETTINGS,
//                new Argument("on", Integer.parseInt(onEnergy.getText())),
//                new Argument("off", Integer.parseInt(offEnergy.getText())));

        refreshButtons();
        window.draw();
    }
}