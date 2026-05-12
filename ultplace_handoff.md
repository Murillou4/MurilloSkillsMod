# Handoff — UltPlace preview + Farmer hoe area

Projeto: MurilloSkills (Fabric 1.21.10, Java 21). Já foi reescrito o `UltPlaceConfigScreen.java` para resolver um crash ao abrir o config. Faltam duas tarefas descritas abaixo.

---

## Tarefa 1 — Preview opaco dos blocos que serão colocados

**Objetivo:** enquanto a toggle do UltPlace está ativa e o player segura um `BlockItem`, o preview deve mostrar cada bloco que será colocado como um **bloco sólido/opaco ghost** (modelo 3D do bloco que está na mão), não só o outline atual.

**Arquivo a editar:** `src/client/java/com/murilloskills/render/UltPlacePreview.java`

**Como está hoje:** chama `VeinMinerPreview.renderOutlines(context, blocks, primary, ...)` que só desenha as arestas do cubo.

**O que fazer:**
1. Obter o `ItemStack` do player: primeiro `getMainHandStack()`, se não for `BlockItem` usa `getOffHandStack()`.
2. Converter para `BlockState`: `((BlockItem) stack.getItem()).getBlock().getDefaultState()`.
3. Para cada `BlockPos` em `blocks`, renderizar o `BlockState` transladado no `MatrixStack`:
   ```java
   MinecraftClient client = MinecraftClient.getInstance();
   Vec3d cam = client.gameRenderer.getCamera().getPos();
   MatrixStack matrices = context.matrices();
   BlockRenderManager brm = client.getBlockRenderManager();
   VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

   for (BlockPos pos : blocks) {
       matrices.push();
       matrices.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
       VertexConsumer vc = consumers.getBuffer(RenderLayers.getMovingBlockLayer(state));
       brm.renderBlockAsEntity(state, matrices, consumers, 0x00F000F0, OverlayTexture.DEFAULT_UV);
       matrices.pop();
   }
   consumers.draw();
   ```
   `0x00F000F0` = full block+sky light. Se preferir translúcido de leve, usar uma `RenderLayer` customizada ou trocar alpha via shader — mas o usuário pediu **opaco**, então `renderBlockAsEntity` com luz máxima já resolve.
4. Manter (opcional) o outline atual por cima para destacar o primário — pode também chamar `VeinMinerPreview.renderOutlines(...)` depois do loop de blocos.

**Imports novos prováveis:**
- `net.minecraft.client.render.block.BlockRenderManager`
- `net.minecraft.client.render.OverlayTexture`
- `net.minecraft.client.render.RenderLayers`
- `net.minecraft.client.render.VertexConsumerProvider`
- `net.minecraft.block.BlockState`

**Cuidados:**
- Se `stack` não for `BlockItem`, já tem guard no topo do método `render(...)` — manter.
- `renderBlockAsEntity` pode não funcionar bem para blocos com `BlockEntity` (baú, forno). Aceitar — usuário provavelmente vai usar blocos normais.

---

## Tarefa 2 — Enxada em área (Farmer)

**Objetivo:** quando o player Farmer tem o modo "área" ativo (3x3, 5x5, 7x7, 9x9 dependendo do level), e usa uma `HoeItem` num bloco arável (dirt/grass/coarse_dirt/etc.), deve arar na área inteira, não só no bloco clicado.

**Arquivo novo:** `src/main/java/com/murilloskills/mixin/HoeItemMixin.java`

**Estrutura (espelhar `SeedItemMixin.java`):**
```java
package com.murilloskills.mixin;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public class HoeItemMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void onHoeUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) return;

        PlayerSkillData data = serverPlayer.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        if (!data.isSkillSelected(MurilloSkillsList.FARMER)) return;

        var stats = data.getSkill(MurilloSkillsList.FARMER);
        if (stats.level < SkillConfig.FARMER_AREA_PLANTING_LEVEL) return;

        int radius = FarmerSkill.getAreaPlantingRadius(serverPlayer.getUuid(), stats.level);
        if (radius <= 0) return;

        // Enxada só tem efeito em faces laterais/topo — ignora se clicou por baixo
        if (context.getSide() == Direction.DOWN) return;

        BlockPos origin = context.getBlockPos();
        ItemStack hoeStack = context.getStack();
        int tilled = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = origin.add(dx, 0, dz);
                if (tryTill(world, pos)) {
                    tilled++;
                }
            }
        }

        if (tilled > 0) {
            world.playSound(null, origin, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
            // Damage a enxada 1 vez (não 1/bloco — senão quebra na hora)
            hoeStack.damage(1, serverPlayer, net.minecraft.entity.EquipmentSlot.MAINHAND);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    private static boolean tryTill(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.up());
        if (!above.isAir()) return false; // farmland precisa de ar em cima

        // Grass/dirt/coarse_dirt/podzol viram farmland; rooted dirt → dirt + hanging_roots drop
        if (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.PODZOL)
                || state.isOf(Blocks.DIRT_PATH)) {
            world.setBlockState(pos, state.isOf(Blocks.COARSE_DIRT)
                    ? Blocks.DIRT.getDefaultState()
                    : Blocks.FARMLAND.getDefaultState());
            return true;
        }
        return false;
    }
}
```

**Registrar o mixin:** adicionar `"HoeItemMixin"` na lista `mixins` de `src/main/resources/murilloskills.mixins.json` (junto com os outros mixins do main source set).

**Cuidados:**
- Durability da enxada: se der damage 1/bloco arado, a enxada quebra instantaneamente. Dar só 1 damage no total por ativação.
- Coarse dirt vira dirt normal (não farmland) — vanilla tem esse comportamento; imitar.
- `FARMER_AREA_PLANTING_LEVEL` já existe em `SkillConfig` — verificar nome exato (é `FARMER_AREA_PLANTING_LEVEL` em `src/main/java/com/murilloskills/mixin/SeedItemMixin.java:65`).

---

## Contexto do repo (para o próximo chat)

- Build: `./gradlew build` (shell bash, Windows, caminho: `E:\Desktop\Development\Projetos pessoais\Minecraft Mods\MurilloSkillsMod`)
- Estrutura split: `src/main` (common+server) vs `src/client` (cliente). Preview fica em `src/client`, mixins de seed/hoe em `src/main`.
- Referências úteis:
  - `src/main/java/com/murilloskills/mixin/SeedItemMixin.java` — template do mixin de enxada
  - `src/client/java/com/murilloskills/render/VeinMinerPreview.java` — template de render de preview (mostra como pegar matrices/camera)
  - `src/main/java/com/murilloskills/impl/FarmerSkill.java:320` — `getAreaPlantingRadius` (já existe)
  - `src/main/java/com/murilloskills/utils/SkillConfig.java` — `FARMER_AREA_PLANTING_LEVEL` constante

## Validação final
```bash
./gradlew build
./gradlew runClient
```
- Preview: ativar UltPlace (V), segurar um bloco, mirar numa face — deve aparecer o modelo 3D dos blocos em cada posição que será preenchida.
- Enxada: com Farmer selecionado e modo área ligado (G), clicar enxada em grass/dirt → ara área inteira, enxada toma 1 de dano, som toca uma vez.
