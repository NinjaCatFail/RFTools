package mcjty.rftools.blocks.crafter;

import mcjty.lib.container.InventoryHelper;
import mcjty.lib.entity.GenericEnergyReceiverTileEntity;
import mcjty.lib.network.Argument;
import mcjty.lib.varia.BlockTools;
import mcjty.lib.varia.Logging;
import mcjty.rftools.blocks.RedstoneMode;
import mcjty.rftools.items.storage.StorageFilterCache;
import mcjty.rftools.items.storage.StorageFilterItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.Map;

public class CrafterBaseTE extends GenericEnergyReceiverTileEntity implements ISidedInventory {
    public static final int SPEED_SLOW = 0;
    public static final int SPEED_FAST = 1;

    public static final String CMD_MODE = "mode";
    public static final String CMD_REMEMBER = "remember";
    public static final String CMD_FORGET = "forget";

    private InventoryHelper inventoryHelper = new InventoryHelper(this, CrafterContainer.factory,
            10 + CrafterContainer.BUFFER_SIZE + CrafterContainer.BUFFEROUT_SIZE + 1);

    private ItemStack[] ghostSlots = new ItemStack[CrafterContainer.BUFFER_SIZE + CrafterContainer.BUFFEROUT_SIZE];

    private CraftingRecipe recipes[];
    private int supportedRecipes;

    private StorageFilterCache filterCache = null;

    private RedstoneMode redstoneMode = RedstoneMode.REDSTONE_IGNORED;
    private int speedMode = SPEED_SLOW;

    private InventoryCrafting workInventory = new InventoryCrafting(new Container() {
        @Override
        public boolean canInteractWith(EntityPlayer var1) {
            return false;
        }
    }, 3, 3);


    public CrafterBaseTE() {
        super(CrafterConfiguration.MAXENERGY, CrafterConfiguration.RECEIVEPERTICK);
        setSupportedRecipes(8);
    }

    public ItemStack[] getGhostSlots() {
        return ghostSlots;
    }

    public void setSupportedRecipes(int supportedRecipes) {
        this.supportedRecipes = supportedRecipes;
        recipes =  new CraftingRecipe[supportedRecipes];
        for (int i = 0 ; i < recipes.length ; i++) {
            recipes[i] = new CraftingRecipe();
        }
    }

    public int getSupportedRecipes() {
        return supportedRecipes;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public int getSpeedMode() {
        return speedMode;
    }

    public void setSpeedMode(int speedMode) {
        this.speedMode = speedMode;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public CraftingRecipe getRecipe(int index) {
        return recipes[index];
    }

    @Override
    public int getSizeInventory() {
        return inventoryHelper.getCount();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventoryHelper.getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        return inventoryHelper.decrStackSize(index, amount);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == CrafterContainer.SLOT_FILTER_MODULE) {
            filterCache = null;
        }
        inventoryHelper.setInventorySlotContents(getInventoryStackLimit(), index, stack);
    }

    @Override
    public String getInventoryName() {
        return "Crafter Inventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return canPlayerAccess(player);
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    private void getFilterCache() {
        if (filterCache == null) {
            filterCache = StorageFilterItem.getCache(inventoryHelper.getStackInSlot(CrafterContainer.SLOT_FILTER_MODULE));
        }
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index >= CrafterContainer.SLOT_BUFFER && index < CrafterContainer.SLOT_BUFFEROUT) {
            ItemStack ghostSlot = ghostSlots[index - CrafterContainer.SLOT_BUFFER];
            if (ghostSlot != null) {
                if (!ghostSlot.isItemEqual(stack)) {
                    return false;
                }
            }
            if (inventoryHelper.containsItem(CrafterContainer.SLOT_FILTER_MODULE)) {
                getFilterCache();
                if (filterCache != null) {
                    return filterCache.match(stack);
                }
            }
        } else if (index >= CrafterContainer.SLOT_BUFFEROUT && index < CrafterContainer.SLOT_FILTER_MODULE) {
            ItemStack ghostSlot = ghostSlots[index - CrafterContainer.SLOT_BUFFEROUT + CrafterContainer.BUFFER_SIZE];
            if (ghostSlot != null) {
                if (!ghostSlot.isItemEqual(stack)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return CrafterContainer.factory.getAccessibleSlots();
    }

    @Override
    public boolean canInsertItem(int index, ItemStack item, int side) {
        if (!isItemValidForSlot(index, item)) {
            return false;
        }
        return CrafterContainer.factory.isInputSlot(index);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack item, int side) {
        return CrafterContainer.factory.isOutputSlot(index);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound);
        readGhostBufferFromNBT(tagCompound);
        readRecipesFromNBT(tagCompound);

        int m = tagCompound.getInteger("rsMode");
        redstoneMode = RedstoneMode.values()[m];

        speedMode = tagCompound.getByte("speedMode");
    }

    private void readBufferFromNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = tagCompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < bufferTagList.tagCount() ; i++) {
            NBTTagCompound nbtTagCompound = bufferTagList.getCompoundTagAt(i);
            inventoryHelper.setStackInSlot(i+CrafterContainer.SLOT_BUFFER, ItemStack.loadItemStackFromNBT(nbtTagCompound));
        }
    }

    private void readGhostBufferFromNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = tagCompound.getTagList("GItems", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < bufferTagList.tagCount() ; i++) {
            NBTTagCompound nbtTagCompound = bufferTagList.getCompoundTagAt(i);
            ghostSlots[i] = ItemStack.loadItemStackFromNBT(nbtTagCompound);
        }
    }

    private void readRecipesFromNBT(NBTTagCompound tagCompound) {
        NBTTagList recipeTagList = tagCompound.getTagList("Recipes", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < recipeTagList.tagCount() ; i++) {
            NBTTagCompound nbtTagCompound = recipeTagList.getCompoundTagAt(i);
            recipes[i].readFromNBT(nbtTagCompound);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound);
        writeGhostBufferToNBT(tagCompound);
        writeRecipesToNBT(tagCompound);
        tagCompound.setByte("rsMode", (byte)redstoneMode.ordinal());
        tagCompound.setByte("speedMode", (byte) speedMode);
    }

    private void writeBufferToNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = new NBTTagList();
        for (int i = CrafterContainer.SLOT_BUFFER ; i < inventoryHelper.getCount() ; i++) {
            ItemStack stack = inventoryHelper.getStackInSlot(i);
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            if (stack != null) {
                stack.writeToNBT(nbtTagCompound);
            }
            bufferTagList.appendTag(nbtTagCompound);
        }
        tagCompound.setTag("Items", bufferTagList);
    }

    private void writeGhostBufferToNBT(NBTTagCompound tagCompound) {
        NBTTagList bufferTagList = new NBTTagList();
        for (int i = 0 ; i < ghostSlots.length ; i++) {
            ItemStack stack = ghostSlots[i];
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            if (stack != null) {
                stack.writeToNBT(nbtTagCompound);
            }
            bufferTagList.appendTag(nbtTagCompound);
        }
        tagCompound.setTag("GItems", bufferTagList);
    }

    private void writeRecipesToNBT(NBTTagCompound tagCompound) {
        NBTTagList recipeTagList = new NBTTagList();
        for (CraftingRecipe recipe : recipes) {
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            recipe.writeToNBT(nbtTagCompound);
            recipeTagList.appendTag(nbtTagCompound);
        }
        tagCompound.setTag("Recipes", recipeTagList);
    }

    @Override
    protected void checkStateServer() {
        super.checkStateServer();

        if (redstoneMode != RedstoneMode.REDSTONE_IGNORED) {
            int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
            boolean rs = BlockTools.getRedstoneSignal(meta);
            if (redstoneMode == RedstoneMode.REDSTONE_OFFREQUIRED && rs) {
                return;
            } else if (redstoneMode == RedstoneMode.REDSTONE_ONREQUIRED && !rs) {
                return;
            }
        }

        int steps = 1;
        if (speedMode == SPEED_FAST) {
            steps = CrafterConfiguration.speedOperations;
        }

        for (int i = 0 ; i < steps ; i++) {
            craftOneCycle();
        }
    }

    private void craftOneCycle() {
        // 0%: rf -> rf
        // 100%: rf -> rf / 2
        int rf = (int) (CrafterConfiguration.rfPerOperation * (2.0f - getInfusedFactor()) / 2.0f);

        if (getEnergyStored(ForgeDirection.DOWN) < rf) {
            return;
        }

        boolean energyConsumed = false;

        for (int index = 0 ; index < supportedRecipes ; index++) {
            CraftingRecipe craftingRecipe = recipes[index];

            if (craftingRecipe != null) {
                if (craftOneItemNew(craftingRecipe)) {
                    energyConsumed = true;
                }
            }
        }

        if (energyConsumed) {
            consumeEnergy(rf);
        }
    }

    private boolean craftOneItemNew(CraftingRecipe craftingRecipe) {
        IRecipe recipe = craftingRecipe.getCachedRecipe(worldObj);
        if (recipe == null) {
            return false;
        }

        Map<Integer,ItemStack> undo = new HashMap<Integer, ItemStack>();

        if (!testAndConsumeCraftingItems(craftingRecipe, undo)) {
            undo(undo);
            return false;
        }

//        ItemStack result = recipe.getCraftingResult(craftingRecipe.getInventory());
        ItemStack result = null;
        try {
            result = recipe.getCraftingResult(workInventory);
        } catch (Exception e) {
            // Ignore this error for now to make sure we don't crash on bad recipes.
            Logging.log("Problem with recipe!");
        }

        // Try to merge the output. If there is something that doesn't fit we undo everything.
        if (result != null && placeResult(craftingRecipe.isCraftInternal(), result, undo)) {
            return true;
        } else {
            // We don't have place. Undo the operation.
            undo(undo);
            return false;
        }
    }

    private boolean testAndConsumeCraftingItems(CraftingRecipe craftingRecipe, Map<Integer,ItemStack> undo) {
        boolean internal = craftingRecipe.isCraftInternal();
        int keep = craftingRecipe.isKeepOne() ? 1 : 0;
        InventoryCrafting inventory = craftingRecipe.getInventory();

        for (int i = 0 ; i < inventory.getSizeInventory() ; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null) {
                int count = stack.stackSize;
                for (int j = 0 ; j < CrafterContainer.BUFFER_SIZE ; j++) {
                    int slotIdx = CrafterContainer.SLOT_BUFFER + j;
                    ItemStack input = inventoryHelper.getStackInSlot(slotIdx);
                    if (input != null && input.stackSize > keep) {
                        if (OreDictionary.itemMatches(stack, input, false)) {
                            workInventory.setInventorySlotContents(i, input.copy());
                            if (input.getItem().hasContainerItem(input)) {
                                ItemStack containerItem = input.getItem().getContainerItem(input);
                                if (containerItem != null) {
                                    if ((!containerItem.isItemStackDamageable()) || containerItem.getItemDamage() <= containerItem.getMaxDamage()) {
                                        if (!placeResult(internal, containerItem, undo)) {
                                            // Not enough room.
                                            return false;
                                        }
                                    }
                                }
                            }
                            int ss = count;
                            if (input.stackSize - ss < keep) {
                                ss = input.stackSize - keep;
                            }
                            count -= ss;
                            if (!undo.containsKey(slotIdx)) {
                                undo.put(slotIdx, input.copy());
                            }
                            input.splitStack(ss);        // This consumes the items
                            if (input.stackSize == 0) {
                                inventoryHelper.setStackInSlot(slotIdx, null);
                            }
                        }
                    }
                    if (count == 0) {
                        break;
                    }
                }
                if (count > 0) {
                    return false;   // Couldn't find all items.
                }
            } else {
                workInventory.setInventorySlotContents(i, null);
            }
        }
        return true;
    }


    private void undo(Map<Integer,ItemStack> undo) {
        for (Map.Entry<Integer, ItemStack> entry : undo.entrySet()) {
            inventoryHelper.setStackInSlot(entry.getKey(), entry.getValue());
        }
    }

    private boolean placeResult(boolean internal, ItemStack result, Map<Integer,ItemStack> undo) {
        int start;
        int stop;
        if (internal) {
            start = CrafterContainer.SLOT_BUFFER;
            stop = CrafterContainer.SLOT_BUFFER + CrafterContainer.BUFFER_SIZE;
        } else {
            start = CrafterContainer.SLOT_BUFFEROUT;
            stop = CrafterContainer.SLOT_BUFFEROUT + CrafterContainer.BUFFEROUT_SIZE;
        }
        return InventoryHelper.mergeItemStack(this, true, result, start, stop, undo) == 0;
    }

    private void rememberItems() {
        for (int i = 0 ; i < ghostSlots.length ; i++) {
            int slotIdx;
            if (i < CrafterContainer.BUFFER_SIZE) {
                slotIdx = i + CrafterContainer.SLOT_BUFFER;
            } else {
                slotIdx = i + CrafterContainer.SLOT_BUFFEROUT - CrafterContainer.BUFFER_SIZE;
            }
            if (inventoryHelper.containsItem(slotIdx)) {
                ItemStack stack = inventoryHelper.getStackInSlot(slotIdx);
                ghostSlots[i] = stack.copy();
                ghostSlots[i].stackSize = 1;
            }
        }
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    private void forgetItems() {
        for (int i = 0 ; i < ghostSlots.length ; i++) {
            ghostSlots[i] = null;
        }
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, Map<String, Argument> args) {
        boolean rc = super.execute(playerMP, command, args);
        if (rc) {
            return true;
        }
        if (CMD_MODE.equals(command)) {
            String m = args.get("rs").getString();
            setRedstoneMode(RedstoneMode.getMode(m));
            setSpeedMode(args.get("speed").getInteger());
            return true;
        } else if (CMD_REMEMBER.equals(command)) {
            rememberItems();
            return true;
        } else if (CMD_FORGET.equals(command)) {
            forgetItems();
            return true;
        }
        return false;
    }
}
