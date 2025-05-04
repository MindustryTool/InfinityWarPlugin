package infinitywar;

import arc.util.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Groups;
import mindustry.mod.*;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquid;

public class InfinityWarPlugin extends Plugin {

    @Override
    public void init() {
        Timer.schedule(() -> {
            if (!Vars.state.isPlaying())
                return;
            try {
                Groups.build.each(build -> {
                    var block = build.block();

                    if (build.items == null)
                        return;

                    for (var consumer : block.consumers) {
                        if (consumer instanceof ConsumeItems consumeItems) {
                            if (block.name.equals(Blocks.thoriumReactor.name)) {
                                for (var stack : consumeItems.items) {
                                    if (build.items.get(stack.item) < block.itemCapacity) {
                                        build.items.add(stack.item, block.itemCapacity);
                                        continue;
                                    }
                                }
                            }

                            for (var stack : consumeItems.items) {
                                if (build.items.get(stack.item) < 100) {
                                    build.items.add(stack.item, 1000);
                                }
                            }
                        } else if ((consumer instanceof ConsumeLiquid consumeLiquid)) {
                            if (build.liquids.get(consumeLiquid.liquid) < 100) {
                                build.liquids.add(consumeLiquid.liquid, 1000);
                            }
                        }
                    }

                    for (var item : Vars.content.items()) {
                        if (block.consumesItem(item)) {
                            if (build.items.get(item) < 100) {
                                build.items.add(item, 1000);
                            }
                        }
                    }

                    for (var liquid : Vars.content.liquids()) {
                        if (block.consumesLiquid(liquid)) {
                            if (build.liquids.get(liquid) < 100) {
                                build.liquids.add(liquid, 1000);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Log.err(e);
            }
        }, 10, 1);
    }
}
