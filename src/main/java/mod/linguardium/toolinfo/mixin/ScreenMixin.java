package mod.linguardium.toolinfo.mixin;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.text.LiteralText;
import net.minecraft.text.StringRenderable;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow protected ItemRenderer itemRenderer;

    @Shadow protected TextRenderer textRenderer;

    @Shadow public abstract void renderTooltip(MatrixStack matrices, List<? extends StringRenderable> lines, int x, int y);

    @Shadow public abstract List<Text> getTooltipFromItem(ItemStack stack);

    @Shadow public int width;

    @Shadow public int height;

    @Unique
    public int startWidth = 0;

    @ModifyConstant(method="renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;II)V",constant=@Constant(intValue = 0,ordinal = 0))
    private int initialWidth(int zero) {
        return startWidth;
    }

    @Inject(at=@At(value="HEAD"),method="Lnet/minecraft/client/gui/screen/Screen;renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemStack;II)V", cancellable = true)
    private void renderItemStats(MatrixStack matrices, ItemStack stack, int mouse_x, int mouse_y, CallbackInfo info) {
        if (stack.getItem() instanceof ToolItem) {
            startWidth=80;
            String miningLevel = String.valueOf(((ToolItem) stack.getItem()).getMaterial().getMiningLevel());
            int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY,stack);
            String miningSpeed = String.valueOf(((ToolItem) stack.getItem()).getMaterial().getMiningSpeedMultiplier() + (efficiency*efficiency+1));
            List<StringRenderable> tooltips= Lists.newArrayList();
            tooltips.add(new LiteralText(" "));
            tooltips.add(new LiteralText(" "));
            tooltips.addAll(getTooltipFromItem(stack));
            this.renderTooltip(matrices, tooltips, mouse_x, mouse_y);
            int x = mouse_x + 12;
            int y = mouse_y - 12;
            int n = 8+2+(tooltips.size()-1)*10;
            int w = 80;
            for(StringRenderable tooltip : tooltips) {
                w= Math.max(w, textRenderer.getWidth(tooltip));
            }
            if (x + w > width) {
                x -= 28 + w;
            }
            if (y + n + 6 > height) {
                y = height - n-6;
            }
            matrices.push();
            matrices.translate(0, 0, 500);

            float fontScale = 1.0f;
            VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            ItemStack level = new ItemStack(Items.IRON_PICKAXE);
            ItemStack speed = new ItemStack(Items.FEATHER);
            itemRenderer.zOffset += 400; // to render above the framing
            itemRenderer.renderInGuiWithOverrides(level, x, y);
            itemRenderer.renderInGuiWithOverrides(speed, x + 28, y);
            itemRenderer.zOffset -= 400;
            x = x + 18;
            matrices.scale(fontScale, fontScale, fontScale);
            textRenderer.draw(matrices, miningLevel, x / fontScale, (y + 5) / fontScale, -1);
            textRenderer.draw(matrices, miningSpeed, (x + 28) / fontScale, (y + 5) / fontScale, -1);
            matrices.pop();

            info.cancel();
        }
        startWidth=0;
    }
}
