package com.mcjty.rftools.blocks.screens.modulesclient;

import com.mcjty.gui.RenderHelper;
import com.mcjty.gui.layout.VerticalLayout;
import com.mcjty.gui.widgets.Panel;
import com.mcjty.rftools.blocks.screens.ModuleGuiChanged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.nbt.NBTTagCompound;

public class EnergyBarClientScreenModule implements ClientScreenModule {
    @Override
    public TransformMode getTransformMode() {
        return TransformMode.TEXT;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public void render(FontRenderer fontRenderer, int currenty) {
        RenderHelper.drawHorizontalGradientRect(7 + 10, currenty+1, 7 + 60, currenty + 8, 0xffff0000, 0xff333300);
    }

    @Override
    public Panel createGui(Minecraft mc, Gui gui, NBTTagCompound currentData, ModuleGuiChanged moduleGuiChanged) {
        Panel panel = new Panel(mc, gui).setLayout(new VerticalLayout());
        return panel;
    }

    @Override
    public void setupFromNBT(NBTTagCompound tagCompound) {

    }
}
