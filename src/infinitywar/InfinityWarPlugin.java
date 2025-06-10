package infinitywar;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import arc.Core;
import arc.Events;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquid;

public class InfinityWarPlugin extends Plugin {

    private HashSet<WeakReference<Building>> consumeBuildings = new HashSet<>();
    private long nextUpdateBuildTime = System.currentTimeMillis();
    private long nextFillTime = System.currentTimeMillis();

    @Override
    public void init() {
        Timer.schedule(() -> {
            try {
                Thread.sleep(10);
                if (!Vars.state.isPlaying())
                    return;

                if (System.currentTimeMillis() >= nextUpdateBuildTime) {
                    updateBuilding();
                    nextUpdateBuildTime = System.currentTimeMillis() + 5000;
                }

                if (System.currentTimeMillis() >= nextFillTime) {
                    fillBuilding();
                    nextFillTime = System.currentTimeMillis() + 1000;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 0.2f);

        Events.on(BlockBuildEndEvent.class, event -> {
            if (event.tile.build == null) {
                return;
            }

            processBuild(event.tile.build);

            if (event.tile.build.block().consumers.length > 0//
                    && consumeBuildings.stream().noneMatch(weak -> weak.get() == event.tile.build)//
            ) {
                consumeBuildings.add(new WeakReference<>(event.tile.build));
            }

        });
    }

    private void updateBuilding() {
        consumeBuildings.removeIf(ref -> ref.get() == null);

        Groups.build.each(build -> {
            System.out.println("Check building: " + build);

            if (build.block().consumers.length > 0
                    && consumeBuildings.stream().noneMatch(weak -> weak.get() == build)//
            ) {
                System.out.println("Add building: " + build);

                consumeBuildings.add(new WeakReference<>(build));
            }
        });
    }

    private void fillBuilding() {
        for (var weak : consumeBuildings) {
            var build = weak.get();

            System.out.println("Filling building: " + build);

            if (build == null)
                continue;

            processBuild(build);
        }
    }

    private void processBuild(Building build) {
        var block = build.block();

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
