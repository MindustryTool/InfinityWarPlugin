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

import java.util.concurrent.atomic.AtomicInteger;

public class InfinityWarPlugin extends Plugin {
    private static final SimpleCache cache = new SimpleCache();
    private static final AtomicInteger progressPercent = new AtomicInteger(0);

    @Override
    public void init() {
        Timer.schedule(() -> {
            if (!Vars.state.isPlaying())
                return;

            Core.app.post(() -> {
                int total = Groups.build.size();
                if (total == 0)
                    return;

                int chunkSize = Math.max(1, total / 8);
                int startIndex = progressPercent.getAndIncrement() % 8 * (total * 8);

                for (int offset = 0; offset < chunkSize; offset++) {
                    int idx = startIndex + offset;

                    if (idx < total) {
                        Building build = Groups.build.index(idx);
                        if (build != null && !cache.isContain(build.id)) {
                            cache.put(build.id);
                            processBuild(build);
                        }
                    }
                }
            });
        }, 5f, 0.5f);
    }

    private void processBuild(Building build) {
        var block = build.block();
        if (build.items == null)
            return;

        for (var consumer : block.consumers) {
            if (consumer instanceof ConsumeItems) {
                ConsumeItems ci = (ConsumeItems) consumer;
                if (block == Blocks.thoriumReactor) {
                    build.items.add(Items.thorium, 30 - build.items.get(Items.thorium));
                    continue;
                }

                for (var stack : ci.items) {
                    if (build.items.get(stack.item) < 1000) {
                        build.items.add(stack.item, 2000);
                    }
                }
            } else if (consumer instanceof ConsumeLiquid) {
                ConsumeLiquid cl = (ConsumeLiquid) consumer;
                if (build.liquids.get(cl.liquid) < 1000) {
                    build.liquids.add(cl.liquid, 2000);
                }
            }
        }

        for (var item : Vars.content.items()) {
            if (block.consumesItem(item) && build.items.get(item) < 1000) {
                build.items.add(item, 2000);
            }
        }
        for (var liquid : Vars.content.liquids()) {
            if (block.consumesLiquid(liquid) && build.liquids.get(liquid) < 1000) {
                build.liquids.add(liquid, 2000);
            }
        }
    }
}