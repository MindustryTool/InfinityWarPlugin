package infinitywar;

import arc.Core;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquid;

public class InfinityWarPlugin extends Plugin {

    @Override
    public void init() {
        Timer.schedule(() -> {
            if (!Vars.state.isPlaying())
                return;

            int total = Groups.build.size();

            if (total == 0)
                return;

            Groups.build.each(build -> processBuild(build));
        }, 0, 0.5f);
    }

    private void processBuild(Building build) {
        var block = build.block();

        if (build.items == null)
            return;

        for (var consumer : block.consumers) {
            if (consumer instanceof ConsumeItems ci) {
                if (block == Blocks.thoriumReactor) {
                    Core.app.post(() -> build.items.add(Items.thorium, 30 - build.items.get(Items.thorium)));
                    continue;
                }

                for (var stack : ci.items) {
                    if (build.items.get(stack.item) < 1000) {
                        Core.app.post(() -> build.items.add(stack.item, 2000));
                    }
                }
            } else if (consumer instanceof ConsumeLiquid cl) {
                if (build.liquids.get(cl.liquid) < 1000) {
                    Core.app.post(() -> build.liquids.add(cl.liquid, 2000));
                }
            }
        }
    }
}
