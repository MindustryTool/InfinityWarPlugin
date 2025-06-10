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
import mindustry.world.consumers.ConsumeLiquids;

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

            if (isFillable(event.tile.build)) {
                consumeBuildings.add(new WeakReference<>(event.tile.build));
            }

        });
    }

    private void updateBuilding() {
        consumeBuildings.removeIf(ref -> ref.get() == null);

        Groups.build.each(build -> {
            if (isFillable(build)) {
                consumeBuildings.add(new WeakReference<>(build));
            }
        });
    }

    public boolean isFillable(Building build) {
        if (build == null)
            return false;

        if (consumeBuildings.stream().anyMatch(weak -> weak.get() == build)) {
            return false;
        }

        for (var consumer : build.block().consumers) {
            if (consumer instanceof ConsumeItems) {
                return true;
            } else if (consumer instanceof ConsumeLiquid) {
                return true;
            } else if (consumer instanceof ConsumeLiquids) {
                return true;
            }
        }

        return false;
    }

    private void fillBuilding() {
        for (var weak : consumeBuildings) {
            var build = weak.get();

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
                    if (build.items.get(stack.item) < 2000) {
                        Core.app.post(() -> build.items.add(stack.item, 2000));
                    }
                }
            } else if (consumer instanceof ConsumeLiquid cl) {
                if (build.liquids.get(cl.liquid) < 2000) {
                    Core.app.post(() -> build.liquids.add(cl.liquid, 2000));
                }
            } else if (consumer instanceof ConsumeLiquids cl) {
                for (var stack : cl.liquids) {
                    if (build.liquids.get(stack.liquid) < 2000) {
                        Core.app.post(() -> build.liquids.add(stack.liquid, 2000));
                    }
                }
            }
        }
    }
}
