package infinitywar;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import arc.Core;
import arc.Events;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;
import mindustry.world.consumers.ConsumeItemFilter;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumeLiquidFilter;
import mindustry.world.consumers.ConsumeLiquids;

public class InfinityWarPlugin extends Plugin {

    private final Set<WeakReference<Building>> consumeBuildings = Collections
            .newSetFromMap(new ConcurrentHashMap<WeakReference<Building>, Boolean>());
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private long nextUpdateBuildTime = System.currentTimeMillis();

    @Override
    public void init() {
        executor.scheduleWithFixedDelay(() -> {
            try {
                if (!Vars.state.isPlaying())
                    return;

                if (System.currentTimeMillis() >= nextUpdateBuildTime) {
                    updateBuilding();
                    nextUpdateBuildTime = System.currentTimeMillis() + 10000;
                }

                fillBuilding();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);

        Events.on(BlockBuildEndEvent.class, event -> {
            if (event.tile.build == null) {
                return;
            }

            try {
                processBuild(event.tile.build);

                synchronized (consumeBuildings) {
                    if (isFillable(event.tile.build)) {
                        consumeBuildings.add(new WeakReference<>(event.tile.build));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateBuilding() {
        synchronized (consumeBuildings) {
            consumeBuildings.removeIf(ref -> ref.get() == null || !ref.get().isAdded());

            Groups.build.each(build -> {
                if (isFillable(build)) {
                    consumeBuildings.add(new WeakReference<>(build));
                }
            });
        }
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
            } else if (consumer instanceof ConsumeItemFilter) {
                return true;
            } else if (consumer instanceof ConsumeLiquidFilter) {
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
            } else if (consumer instanceof ConsumeItemFilter cif) {
                for (var item : Vars.content.items().select(cif.filter)) {
                    if (build.items.get(item) < 2000) {
                        Core.app.post(() -> build.items.add(item, 2000));
                    }
                }
            } else if (consumer instanceof ConsumeLiquidFilter clf) {
                for (var liquid : Vars.content.liquids().select(clf.filter)) {
                    if (build.liquids.get(liquid) < 2000) {
                        Core.app.post(() -> build.liquids.add(liquid, 2000));
                    }
                }
            }
        }
    }
}
