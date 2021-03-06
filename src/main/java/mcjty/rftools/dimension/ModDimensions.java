package mcjty.rftools.dimension;

import cpw.mods.fml.common.registry.GameRegistry;
import mcjty.rftools.dimension.world.GenericWorldGenerator;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class ModDimensions {
    public static void init() {
        GameRegistry.registerWorldGenerator(new GenericWorldGenerator(), 1000);
    }

    public static void initDimensions() {
        WorldServer world = DimensionManager.getWorld(0);
        if (world != null) {
            RfToolsDimensionManager dimensionManager = RfToolsDimensionManager.getDimensionManager(world);
            if (dimensionManager != null) {
                dimensionManager.registerDimensions();
            }
        }
    }
}
