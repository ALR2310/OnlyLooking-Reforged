package alr.onlylooking;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(OnlyLooking.MODID)
public class OnlyLooking {
    public static final String MODID = "onlylooking";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OnlyLooking(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ModConfigs.COMMON_SPEC);
    }
}
