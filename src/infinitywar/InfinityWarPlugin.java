package infinitywar;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;
import mindustry.world.Block;
import mindustry.world.consumers.Consume;
import mindustry.world.consumers.ConsumeItemFilter;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumeLiquidFilter;
import mindustry.world.consumers.ConsumeLiquids;

public class InfinityWarPlugin extends Plugin {

    private final Set<Building> consumeBuildings = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Block, Seq<Consume>> blockConsumers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong nextUpdateBuildTime = new AtomicLong(0);

    @Override
    public void init() {
        executor.scheduleWithFixedDelay(() -> {
            try {
                if (!Vars.state.isPlaying()) {
                    return;
                }

                long now = System.currentTimeMillis();

                if (now >= nextUpdateBuildTime.get()) {
                    updateBuilding();
                    nextUpdateBuildTime.set(now + 1000);
                }

                fillBuilding();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);

        Events.on(BlockBuildEndEvent.class, event -> {
            if (event.tile.build != null && isFillable(event.tile.build)) {
                processBuild(event.tile.build);
                consumeBuildings.add(event.tile.build);
            }
        });

        Events.on(WorldLoadEvent.class, event -> clearCache());
        Events.on(ResetEvent.class, event -> clearCache());
    }

    private void clearCache() {
        consumeBuildings.clear();
        blockConsumers.clear();
        nextUpdateBuildTime.set(0);
    }

    private void updateBuilding() {
        consumeBuildings.removeIf(build -> !build.isValid());

        Groups.build.each(build -> {
            if (isFillable(build)) {
                consumeBuildings.add(build);
            }
        });
    }

    public boolean isFillable(Building build) {
        if (build == null) {
            return false;
        }

        Block block = build.block;

        Seq<Consume> consumers = blockConsumers.get(block);

        if (consumers == null) {
            consumers = new Seq<>();
            for (var consumer : block.consumers) {
                if (consumer instanceof ConsumeItems ||
                        consumer instanceof ConsumeLiquid ||
                        consumer instanceof ConsumeLiquids ||
                        consumer instanceof ConsumeItemFilter ||
                        consumer instanceof ConsumeLiquidFilter) {
                    consumers.add(consumer);
                }
            }
            blockConsumers.put(block, consumers);
        }

        return consumers.size > 0;
    }

    private void fillBuilding() {
        for (Building build : consumeBuildings) {
            if (!build.isValid()) {
                continue;
            }

            processBuild(build);
        }
    }

    private void processBuild(Building build) {
        Block block = build.block;

        Seq<Consume> consumers = blockConsumers.get(block);

        if (consumers == null || consumers.size == 0) {
            return;
        }

        for (var consumer : consumers) {
            if (consumer instanceof ConsumeItems ci) {
                if (block == Blocks.thoriumReactor) {
                    if (build.items.get(Items.thorium) < 10) {
                        Core.app.post(() -> build.items.add(Items.thorium, 30 - build.items.get(Items.thorium)));
                    }
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
                for (var item : Vars.content.items()) {
                    if (cif.filter.get(item) && build.items.get(item) < 2000) {
                        Core.app.post(() -> build.items.add(item, 2000));
                    }
                }
            } else if (consumer instanceof ConsumeLiquidFilter clf) {
                for (var liquid : Vars.content.liquids()) {
                    if (clf.filter.get(liquid) && build.liquids.get(liquid) < 2000) {
                        Core.app.post(() -> build.liquids.add(liquid, 2000));
                    }
                }
            }
        }
    }
}
