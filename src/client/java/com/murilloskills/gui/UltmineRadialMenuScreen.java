package com.murilloskills.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.network.UltmineShapeSelectC2SPayload;
import com.murilloskills.skills.UltmineShape;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Performance-focused Ultmine radial menu:
 * - Radial lines are rendered once into dynamic textures on open.
 * - Per-frame cost is just textured quad draw + labels.
 */
public class UltmineRadialMenuScreen extends Screen {
    private static final ColorPalette PALETTE = ColorPalette.premium();
    private static final RenderPipeline GUI_PIPELINE = RenderPipelines.GUI_TEXTURED;
    private static final UltmineShape[] SHAPE_ORDER = {
            UltmineShape.S_3x3,
            UltmineShape.R_2x1,
            UltmineShape.LEGACY,
            UltmineShape.LINE,
            UltmineShape.STAIRS,
            UltmineShape.SQUARE_20x20_D1
    };

    private static final float INNER_RADIUS = 58.0f;
    private static final float OUTER_RADIUS = 142.0f;
    private static final float SEGMENT_GAP_RADIANS = 0.090f;
    private static final int RADIAL_MARGIN = 18;
    private static final int TEXTURE_SCALE = 1;
    private static final int RASTER_SCALE = 2;
    private static final float SELECTED_SLICE_GROWTH = 0.0f;
    private static final long OPEN_ANIM_MS = 180L;
    private static final float STROKE_AA = 0.75f;
    private static final float LABEL_MAIN_SCALE = 1.04f;
    private static final float LABEL_SUB_SCALE = 0.92f;
    private static final float CENTER_TITLE_SCALE = 0.86f;
    private static final float CENTER_CURRENT_SCALE = 0.82f;
    private static final float HINT_SCALE = 0.78f;

    private final UltmineShape[] shapes = SHAPE_ORDER;
    private final Text[] shortNameTexts = new Text[shapes.length];
    private final Text[] shapeNameTexts = new Text[shapes.length];

    private static Identifier sharedBaseTextureId;
    private static NativeImageBackedTexture sharedBaseTexture;
    private static final Identifier[] sharedOverlayTextureIds = new Identifier[SHAPE_ORDER.length];
    private static final NativeImageBackedTexture[] sharedOverlayTextures = new NativeImageBackedTexture[SHAPE_ORDER.length];
    private static int sharedDisplayHalfSize;
    private static int sharedDisplaySize;
    private static int sharedTextureHalfSize;
    private static int sharedTextureSize;
    private static boolean sharedTexturesReady;

    private Identifier baseTextureId;
    private NativeImageBackedTexture baseTexture;
    private final Identifier[] overlayTextureIds = new Identifier[shapes.length];
    private final NativeImageBackedTexture[] overlayTextures = new NativeImageBackedTexture[shapes.length];
    private int displayHalfSize;
    private int displaySize;
    private int textureHalfSize;
    private int textureSize;

    private int hoveredIndex = -1;
    private int selectedIndex = 0;
    private int initialSelectedIndex = 0;
    private boolean selectionChanged = false;
    private long openedAtMs;

    public UltmineRadialMenuScreen() {
        super(Text.translatable("murilloskills.ultmine.menu.title"));
        for (int i = 0; i < shapes.length; i++) {
            shortNameTexts[i] = Text.literal(getShortName(shapes[i]));
            shapeNameTexts[i] = Text.translatable(shapes[i].getTranslationKey());
        }
    }

    @Override
    protected void init() {
        openedAtMs = Util.getMeasuringTimeMs();

        UltmineShape current = UltmineClientState.getSelectedShape();
        for (int i = 0; i < shapes.length; i++) {
            if (shapes[i] == current) {
                selectedIndex = i;
                initialSelectedIndex = i;
                break;
            }
        }
        selectionChanged = false;

        ensureRadialTextures();
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float open = getOpenProgress();
        float eased = easeOutCubic(open);
        float ringScale = 1.0f;
        renderBackdrop(context, eased);

        hoveredIndex = getHoveredIndex(mouseX, mouseY, ringScale, 0.0f);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int baseDrawSize = displaySize;
        int texX = centerX - displayHalfSize;
        int texY = centerY - displayHalfSize;
        if (baseTextureId != null) {
            context.drawTexture(GUI_PIPELINE, baseTextureId, texX, texY, 0.0f, 0.0f,
                    baseDrawSize, baseDrawSize, textureSize, textureSize, textureSize, textureSize);
        }

        int activeIndex = hoveredIndex >= 0 ? hoveredIndex : selectedIndex;
        if (activeIndex >= 0 && activeIndex < overlayTextureIds.length) {
            Identifier overlayId = overlayTextureIds[activeIndex];
            if (overlayId != null) {
                context.drawTexture(GUI_PIPELINE, overlayId, texX, texY, 0.0f, 0.0f,
                        baseDrawSize, baseDrawSize, textureSize, textureSize, textureSize, textureSize);
            }
        }

        float slice = (float) (Math.PI * 2.0 / shapes.length);
        for (int i = 0; i < shapes.length; i++) {
            float start = (-MathHelper.HALF_PI + i * slice) + (SEGMENT_GAP_RADIANS * 0.5f);
            float end = start + slice - SEGMENT_GAP_RADIANS;
            float dynamicOuter = OUTER_RADIUS * ringScale;
            float dynamicInner = INNER_RADIUS * ringScale;
            renderSegmentLabel(context, i, centerX, centerY, start, end, dynamicInner, dynamicOuter, i == hoveredIndex,
                    i == selectedIndex);
        }

        renderCenterPanel(context, centerX, centerY, eased);
        renderHintText(context);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (hoveredIndex >= 0) {
                selectedIndex = hoveredIndex;
                selectionChanged = true;
                selectShape(hoveredIndex);
            } else if (selectionChanged && selectedIndex >= 0) {
                selectShape(selectedIndex);
            } else {
                close();
            }
            return true;
        }
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            close();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE)
                && selectedIndex >= 0) {
            selectShape(selectedIndex);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0.0) {
            return false;
        }
        int dir = verticalAmount > 0.0 ? -1 : 1;
        selectedIndex = (selectedIndex + dir + shapes.length) % shapes.length;
        selectionChanged = true;
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public void releaseAndClose() {
        if (selectionChanged && selectedIndex >= 0) {
            selectShape(selectedIndex);
            return;
        }
        if (hoveredIndex >= 0) {
            selectShape(hoveredIndex);
            return;
        }
        if (selectedIndex != initialSelectedIndex) {
            selectShape(selectedIndex);
            return;
        }
        close();
    }

    private void ensureRadialTextures() {
        if (sharedTexturesReady && sharedBaseTextureId != null) {
            this.baseTextureId = sharedBaseTextureId;
            this.baseTexture = sharedBaseTexture;
            this.displayHalfSize = sharedDisplayHalfSize;
            this.displaySize = sharedDisplaySize;
            this.textureHalfSize = sharedTextureHalfSize;
            this.textureSize = sharedTextureSize;
            for (int i = 0; i < overlayTextureIds.length; i++) {
                this.overlayTextureIds[i] = sharedOverlayTextureIds[i];
                this.overlayTextures[i] = sharedOverlayTextures[i];
            }
            return;
        }

        buildRadialTextures();
        sharedBaseTextureId = this.baseTextureId;
        sharedBaseTexture = this.baseTexture;
        sharedDisplayHalfSize = this.displayHalfSize;
        sharedDisplaySize = this.displaySize;
        sharedTextureHalfSize = this.textureHalfSize;
        sharedTextureSize = this.textureSize;
        for (int i = 0; i < overlayTextureIds.length; i++) {
            sharedOverlayTextureIds[i] = this.overlayTextureIds[i];
            sharedOverlayTextures[i] = this.overlayTextures[i];
        }
        sharedTexturesReady = this.baseTextureId != null;
    }

    private void buildRadialTextures() {
        if (this.client == null) {
            return;
        }

        displayHalfSize = Math.round(OUTER_RADIUS + RADIAL_MARGIN + SELECTED_SLICE_GROWTH + 2.0f);
        displaySize = displayHalfSize * 2;
        textureHalfSize = displayHalfSize * TEXTURE_SCALE;
        textureSize = textureHalfSize * 2;
        int rasterHalfSize = textureHalfSize * RASTER_SCALE;
        int rasterSize = textureSize * RASTER_SCALE;
        int center = rasterHalfSize;
        float slice = MathHelper.TAU / shapes.length;
        float scaledInner = INNER_RADIUS * TEXTURE_SCALE * RASTER_SCALE;
        float scaledOuter = OUTER_RADIUS * TEXTURE_SCALE * RASTER_SCALE;
        float edgeStroke = 1.30f * TEXTURE_SCALE * RASTER_SCALE;

        NativeImage baseRaster = new NativeImage(rasterSize, rasterSize, true);
        clearImage(baseRaster);

        for (int i = 0; i < shapes.length; i++) {
            float start = (-MathHelper.HALF_PI + i * slice) + (SEGMENT_GAP_RADIANS * 0.5f);
            float end = start + slice - SEGMENT_GAP_RADIANS;

            int baseBorderColor = withAlpha(0x00FFFFFF, 172);
            drawSectorOutline(baseRaster, center, center, scaledInner, scaledOuter, start, end, edgeStroke,
                    baseBorderColor);

            NativeImage overlayRaster = new NativeImage(rasterSize, rasterSize, true);
            clearImage(overlayRaster);
            int selectedFill = withAlpha(0x00FFFFFF, 46);
            int selectedInnerAccent = withAlpha(0x00FFFFFF, 74);

            drawSolidSlice(overlayRaster, center, center, scaledInner + (2.2f * TEXTURE_SCALE * RASTER_SCALE),
                    scaledOuter - (2.2f * TEXTURE_SCALE * RASTER_SCALE), start, end, selectedFill);
            drawSolidSlice(overlayRaster, center, center, scaledInner + (2.8f * TEXTURE_SCALE * RASTER_SCALE),
                    scaledInner + (4.8f * TEXTURE_SCALE * RASTER_SCALE), start, end, selectedInnerAccent);

            NativeImage overlayImage = downsampleImage(overlayRaster, RASTER_SCALE);
            registerOverlayTexture(i, overlayImage);
        }

        NativeImage baseImage = downsampleImage(baseRaster, RASTER_SCALE);
        registerBaseTexture(baseImage);
    }

    private void registerBaseTexture(NativeImage image) {
        if (this.client == null) {
            image.close();
            return;
        }
        baseTextureId = Identifier.of("murilloskills", "dynamic/ultmine_radial_base_shared");
        baseTexture = new NativeImageBackedTexture(() -> "murilloskills_ultmine_radial_base", image);
        this.client.getTextureManager().registerTexture(baseTextureId, baseTexture);
    }

    private void registerOverlayTexture(int index, NativeImage image) {
        if (this.client == null) {
            image.close();
            return;
        }
        Identifier id = Identifier.of("murilloskills", "dynamic/ultmine_radial_overlay_shared_" + index);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(
                () -> "murilloskills_ultmine_radial_overlay_" + index, image);
        this.client.getTextureManager().registerTexture(id, texture);
        overlayTextureIds[index] = id;
        overlayTextures[index] = texture;
    }

    private void disposeRadialTextures() {
        if (this.client == null) {
            baseTextureId = null;
            baseTexture = null;
            for (int i = 0; i < overlayTextureIds.length; i++) {
                overlayTextureIds[i] = null;
                overlayTextures[i] = null;
            }
            return;
        }

        if (baseTextureId != null) {
            this.client.getTextureManager().destroyTexture(baseTextureId);
            baseTextureId = null;
        }
        baseTexture = null;

        for (int i = 0; i < overlayTextureIds.length; i++) {
            if (overlayTextureIds[i] != null) {
                this.client.getTextureManager().destroyTexture(overlayTextureIds[i]);
                overlayTextureIds[i] = null;
            }
            overlayTextures[i] = null;
        }
    }

    private void renderBackdrop(DrawContext context, float eased) {
        int top = withAlpha(0x000A1118, Math.round(118 * eased));
        int bottom = withAlpha(0x00060C12, Math.round(146 * eased));
        context.fillGradient(0, 0, this.width, this.height, top, bottom);

        int vignette = withAlpha(0x00000000, Math.round(52 * eased));
        int sideWidth = Math.max(18, this.width / 10);
        int topHeight = Math.max(14, this.height / 12);
        context.fillGradient(0, 0, sideWidth, this.height, vignette, 0x00000000);
        context.fillGradient(this.width - sideWidth, 0, this.width, this.height, 0x00000000, vignette);
        context.fillGradient(0, 0, this.width, topHeight, vignette, 0x00000000);
        context.fillGradient(0, this.height - topHeight, this.width, this.height, 0x00000000, vignette);
    }

    private void renderSegmentLabel(DrawContext context, int index, int centerX, int centerY, float start, float end,
            float innerR, float outerR, boolean hovered, boolean selected) {
        float mid = (start + end) * 0.5f;
        float radius = (innerR + outerR) * 0.5f;
        int x = Math.round(centerX + MathHelper.cos(mid) * radius);
        int y = Math.round(centerY + MathHelper.sin(mid) * radius);

        int mainColor = hovered ? withAlpha(PALETTE.textWhite(), 252) : withAlpha(PALETTE.textLight(), 236);
        int subColor = selected ? withAlpha(PALETTE.textWhite(), 228) : withAlpha(PALETTE.textMuted(), 210);

        drawScaledCenteredText(context, shortNameTexts[index], x, y - 10, mainColor, LABEL_MAIN_SCALE, true);
        drawScaledCenteredText(context, shapeNameTexts[index], x, y + 2, subColor, LABEL_SUB_SCALE, true);
    }

    private void renderCenterPanel(DrawContext context, int centerX, int centerY, float eased) {
        drawScaledCenteredText(context, this.title, centerX, centerY - 11,
                withAlpha(PALETTE.textWhite(), Math.round(234 * eased)), CENTER_TITLE_SCALE, true);
        drawScaledCenteredText(context,
                Text.translatable("murilloskills.ultmine.menu.current", shapeNameTexts[selectedIndex]),
                centerX, centerY + 3, withAlpha(PALETTE.textWhite(), Math.round(226 * eased)), CENTER_CURRENT_SCALE, true);
    }

    private void renderHintText(DrawContext context) {
        drawScaledCenteredText(context, Text.literal("LMB select  |  Wheel cycle  |  ESC cancel"),
                this.width / 2, this.height - 20, withAlpha(PALETTE.textGray(), 128), HINT_SCALE, false);
    }

    private int getHoveredIndex(double mouseX, double mouseY, float ringScale, float spin) {
        float dx = (float) (mouseX - (this.width * 0.5f));
        float dy = (float) (mouseY - (this.height * 0.5f));
        float distance = MathHelper.sqrt(dx * dx + dy * dy);

        float inner = INNER_RADIUS * ringScale;
        float outer = (OUTER_RADIUS + SELECTED_SLICE_GROWTH + 6.0f) * ringScale;
        if (distance < inner || distance > outer) {
            return -1;
        }

        float angle = (float) Math.atan2(dy, dx);
        float normalized = wrapAngle(angle - spin + MathHelper.HALF_PI);
        float slice = (float) (Math.PI * 2.0 / shapes.length);
        int index = MathHelper.floor(normalized / slice);
        if (index < 0 || index >= shapes.length) {
            return -1;
        }

        float angleInSlice = normalized - (index * slice);
        if (angleInSlice < (SEGMENT_GAP_RADIANS * 0.5f) || angleInSlice > (slice - SEGMENT_GAP_RADIANS * 0.5f)) {
            return -1;
        }
        return index;
    }

    private void selectShape(int index) {
        if (index < 0 || index >= shapes.length) {
            return;
        }
        UltmineShape shape = shapes[index];
        UltmineClientState.applyShapeDefaults(shape);
        ClientPlayNetworking.send(new UltmineShapeSelectC2SPayload(
                shape, UltmineClientState.getDepth(), UltmineClientState.getLength()));
        close();
    }

    private static void clearImage(NativeImage image) {
        image.fillRect(0, 0, image.getWidth(), image.getHeight(), 0x00000000);
    }

    private static void drawSolidSlice(NativeImage image, int centerX, int centerY, float innerRadius,
            float outerRadius, float startAngle, float endAngle, int color) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha <= 0) {
            return;
        }

        float innerSq = innerRadius * innerRadius;
        float outerSq = outerRadius * outerRadius;
        float aa = STROKE_AA;
        int minX = Math.max(0, MathHelper.floor(centerX - outerRadius - 1.0f));
        int maxX = Math.min(image.getWidth() - 1, MathHelper.ceil(centerX + outerRadius + 1.0f));
        int minY = Math.max(0, MathHelper.floor(centerY - outerRadius - 1.0f));
        int maxY = Math.min(image.getHeight() - 1, MathHelper.ceil(centerY + outerRadius + 1.0f));

        for (int y = minY; y <= maxY; y++) {
            float dy = (y + 0.5f) - centerY;
            for (int x = minX; x <= maxX; x++) {
                float dx = (x + 0.5f) - centerX;
                float distSq = dx * dx + dy * dy;
                if (distSq < (innerSq - aa * aa) || distSq > (outerSq + aa * aa)) {
                    continue;
                }

                float angle = wrapAngle((float) Math.atan2(dy, dx));
                float radius = MathHelper.sqrt(distSq);

                float innerDist = radius - innerRadius;
                float outerDist = outerRadius - radius;
                float radialCoverage = Math.min(
                        smoothstep(-aa, aa, innerDist),
                        smoothstep(-aa, aa, outerDist));
                if (radialCoverage <= 0.0f) {
                    continue;
                }

                float boundaryAngDist = Math.min(
                        Math.abs(shortestAngleDiff(angle, startAngle)),
                        Math.abs(shortestAngleDiff(angle, endAngle)));
                float boundaryPixelDist = boundaryAngDist * radius;
                boolean insideAngle = isAngleWithinSlice(angle, startAngle, endAngle);
                float angularCoverage = insideAngle
                        ? 1.0f
                        : (1.0f - Math.min(1.0f, boundaryPixelDist / aa));

                float coverage = radialCoverage * Math.max(0.0f, angularCoverage);
                if (coverage <= 0.0f) {
                    continue;
                }

                int scaledColor = withAlpha(color, Math.round(alpha * coverage));
                blendPixel(image, x, y, scaledColor);
            }
        }
    }

    private static void drawSectorOutline(NativeImage image, int centerX, int centerY, float innerRadius,
            float outerRadius, float startAngle, float endAngle, float strokeWidth, int color) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha <= 0) {
            return;
        }

        float half = Math.max(0.5f, strokeWidth * 0.5f);
        float pad = half + STROKE_AA + 1.0f;
        int minX = Math.max(0, MathHelper.floor(centerX - outerRadius - pad));
        int maxX = Math.min(image.getWidth() - 1, MathHelper.ceil(centerX + outerRadius + pad));
        int minY = Math.max(0, MathHelper.floor(centerY - outerRadius - pad));
        int maxY = Math.min(image.getHeight() - 1, MathHelper.ceil(centerY + outerRadius + pad));

        float startCos = MathHelper.cos(startAngle);
        float startSin = MathHelper.sin(startAngle);
        float endCos = MathHelper.cos(endAngle);
        float endSin = MathHelper.sin(endAngle);

        float startX0 = startCos * innerRadius;
        float startY0 = startSin * innerRadius;
        float startX1 = startCos * outerRadius;
        float startY1 = startSin * outerRadius;
        float endX0 = endCos * innerRadius;
        float endY0 = endSin * innerRadius;
        float endX1 = endCos * outerRadius;
        float endY1 = endSin * outerRadius;

        float minRadius = innerRadius - pad;
        float maxRadius = outerRadius + pad;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = (x + 0.5f) - centerX;
                float py = (y + 0.5f) - centerY;
                float coverage = computeSectorOutlineCoverage(px, py, innerRadius, outerRadius, startAngle, endAngle,
                        half, minRadius, maxRadius, startX0, startY0, startX1, startY1, endX0, endY0, endX1, endY1);
                if (coverage <= 0.0f) {
                    continue;
                }

                int scaledColor = withAlpha(color, Math.round(alpha * coverage));
                blendPixel(image, x, y, scaledColor);
            }
        }
    }

    private static float computeSectorOutlineCoverage(float px, float py, float innerRadius, float outerRadius,
            float startAngle, float endAngle, float half, float minRadius, float maxRadius,
            float startX0, float startY0, float startX1, float startY1,
            float endX0, float endY0, float endX1, float endY1) {
        float radius = MathHelper.sqrt(px * px + py * py);
        if (radius < minRadius || radius > maxRadius) {
            return 0.0f;
        }

        float angle = wrapAngle((float) Math.atan2(py, px));
        boolean insideSlice = isAngleWithinSlice(angle, startAngle, endAngle);
        float minDist = Float.POSITIVE_INFINITY;

        if (insideSlice) {
            minDist = Math.min(minDist, Math.abs(radius - innerRadius));
            minDist = Math.min(minDist, Math.abs(radius - outerRadius));
        }

        minDist = Math.min(minDist, pointToSegmentDistance(px, py, startX0, startY0, startX1, startY1));
        minDist = Math.min(minDist, pointToSegmentDistance(px, py, endX0, endY0, endX1, endY1));

        if (!Float.isFinite(minDist) || minDist > (half + STROKE_AA)) {
            return 0.0f;
        }
        return smoothstep(half + STROKE_AA, half - STROKE_AA, minDist);
    }

    private static float pointToSegmentDistance(float px, float py, float x0, float y0, float x1, float y1) {
        float vx = x1 - x0;
        float vy = y1 - y0;
        float lenSq = vx * vx + vy * vy;
        if (lenSq <= 1.0e-5f) {
            float dx = px - x0;
            float dy = py - y0;
            return MathHelper.sqrt(dx * dx + dy * dy);
        }

        float t = ((px - x0) * vx + (py - y0) * vy) / lenSq;
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        float projX = x0 + (vx * t);
        float projY = y0 + (vy * t);
        float dx = px - projX;
        float dy = py - projY;
        return MathHelper.sqrt(dx * dx + dy * dy);
    }

    private static NativeImage downsampleImage(NativeImage source, int factor) {
        if (factor <= 1) {
            return source;
        }

        int srcW = source.getWidth();
        int srcH = source.getHeight();
        int dstW = Math.max(1, srcW / factor);
        int dstH = Math.max(1, srcH / factor);
        NativeImage out = new NativeImage(dstW, dstH, true);
        int sampleCount = factor * factor;

        for (int y = 0; y < dstH; y++) {
            int syBase = y * factor;
            for (int x = 0; x < dstW; x++) {
                int sxBase = x * factor;
                float sumA = 0.0f;
                float sumPR = 0.0f;
                float sumPG = 0.0f;
                float sumPB = 0.0f;

                for (int oy = 0; oy < factor; oy++) {
                    int sy = syBase + oy;
                    for (int ox = 0; ox < factor; ox++) {
                        int sx = sxBase + ox;
                        int c = source.getColorArgb(sx, sy);
                        int a8 = (c >>> 24) & 0xFF;
                        if (a8 <= 0) {
                            continue;
                        }
                        float a = a8 / 255.0f;
                        sumA += a;
                        sumPR += ((c >>> 16) & 0xFF) * a;
                        sumPG += ((c >>> 8) & 0xFF) * a;
                        sumPB += (c & 0xFF) * a;
                    }
                }

                float avgA = sumA / sampleCount;
                if (avgA <= 1.0e-5f) {
                    out.setColorArgb(x, y, 0x00000000);
                    continue;
                }

                float avgPR = sumPR / sampleCount;
                float avgPG = sumPG / sampleCount;
                float avgPB = sumPB / sampleCount;
                int a8 = MathHelper.clamp(Math.round(avgA * 255.0f), 0, 255);
                int r8 = MathHelper.clamp(Math.round(avgPR / avgA), 0, 255);
                int g8 = MathHelper.clamp(Math.round(avgPG / avgA), 0, 255);
                int b8 = MathHelper.clamp(Math.round(avgPB / avgA), 0, 255);
                out.setColorArgb(x, y, (a8 << 24) | (r8 << 16) | (g8 << 8) | b8);
            }
        }

        source.close();
        return out;
    }

    private void drawScaledCenteredText(DrawContext context, Text text, int centerX, int y, int color, float scale,
            boolean shadow) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX, y);
        matrices.scale(scale, scale);
        int drawX = -Math.round(this.textRenderer.getWidth(text) * 0.5f);
        if (shadow) {
            context.drawTextWithShadow(this.textRenderer, text, drawX, 0, color);
        } else {
            context.drawText(this.textRenderer, text, drawX, 0, color, false);
        }
        matrices.popMatrix();
    }

    private static void blendPixel(NativeImage image, int x, int y, int src) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return;
        }
        int srcA = (src >>> 24) & 0xFF;
        if (srcA <= 0) {
            return;
        }
        int dst = image.getColorArgb(x, y);
        int out = alphaBlend(dst, src);
        image.setColorArgb(x, y, out);
    }

    private static int alphaBlend(int dst, int src) {
        int srcA = (src >>> 24) & 0xFF;
        if (srcA >= 255) {
            return src;
        }
        int dstA = (dst >>> 24) & 0xFF;

        int srcR = (src >>> 16) & 0xFF;
        int srcG = (src >>> 8) & 0xFF;
        int srcB = src & 0xFF;
        int dstR = (dst >>> 16) & 0xFF;
        int dstG = (dst >>> 8) & 0xFF;
        int dstB = dst & 0xFF;

        int invSrcA = 255 - srcA;
        int outA = srcA + ((dstA * invSrcA + 127) / 255);
        if (outA <= 0) {
            return 0;
        }

        int outR = (srcR * srcA + (dstR * dstA * invSrcA + 127) / 255) / outA;
        int outG = (srcG * srcA + (dstG * dstA * invSrcA + 127) / 255) / outA;
        int outB = (srcB * srcA + (dstB * dstA * invSrcA + 127) / 255) / outA;

        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    private static int withAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static float wrapAngle(float angle) {
        float wrapped = angle % MathHelper.TAU;
        return wrapped < 0.0f ? wrapped + MathHelper.TAU : wrapped;
    }

    private static boolean isAngleWithinSlice(float angle, float start, float end) {
        float a = wrapAngle(angle);
        float s = wrapAngle(start);
        float e = wrapAngle(end);
        if (s <= e) {
            return a >= s && a <= e;
        }
        return a >= s || a <= e;
    }

    private static float shortestAngleDiff(float a, float b) {
        float diff = wrapAngle(a) - wrapAngle(b);
        if (diff > MathHelper.PI) {
            diff -= MathHelper.TAU;
        } else if (diff < -MathHelper.PI) {
            diff += MathHelper.TAU;
        }
        return diff;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        if (edge0 == edge1) {
            return x < edge0 ? 0.0f : 1.0f;
        }
        float t = MathHelper.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private float getOpenProgress() {
        long elapsed = Util.getMeasuringTimeMs() - openedAtMs;
        return MathHelper.clamp(elapsed / (float) OPEN_ANIM_MS, 0.0f, 1.0f);
    }

    private static float easeOutCubic(float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        float p = 1.0f - t;
        return 1.0f - (p * p * p);
    }

    private static String getShortName(UltmineShape shape) {
        return switch (shape) {
            case S_3x3 -> "3x3";
            case R_2x1 -> "2x1";
            case LEGACY -> "OLD";
            case LINE -> "LINE";
            case STAIRS -> "STAIRS";
            case SQUARE_20x20_D1 -> "20x20";
        };
    }
}
