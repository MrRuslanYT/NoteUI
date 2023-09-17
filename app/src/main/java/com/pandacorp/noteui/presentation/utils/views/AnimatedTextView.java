package com.pandacorp.noteui.presentation.utils.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

@SuppressLint("RtlHardcoded")
public class AnimatedTextView extends View {

    public static class AnimatedTextDrawable extends Drawable {

        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private int gravity = 0;

        private boolean isRTL = false;

        private float currentWidth, currentHeight;
        private Part[] currentParts;
        private CharSequence currentText;

        private float oldWidth, oldHeight;
        private Part[] oldParts;
        private CharSequence oldText;

        private static class Part {
            StaticLayout layout;
            float offset;
            int toOppositeIndex;
            float left, width;

            public Part(StaticLayout layout, float offset, int toOppositeIndex) {
                this.layout = layout;
                this.offset = offset;
                this.toOppositeIndex = toOppositeIndex;
                this.left = layout == null || layout.getLineCount() <= 0 ? 0 : layout.getLineLeft(0);
                this.width = layout == null || layout.getLineCount() <= 0 ? 0 : layout.getLineWidth(0);
            }
        }

        private float t = 0;
        private boolean moveDown = true;
        private ValueAnimator animator;
        private CharSequence toSetText;
        private boolean toSetTextMoveDown;

        private long animateDelay = 0;
        private long animateDuration = 450;
        private TimeInterpolator animateInterpolator = CubicBezierInterpolator.EASE_OUT_QUINT;
        private float moveAmplitude = 1f;

        private int alpha = 255;
        private final Rect bounds = new Rect();

        private final boolean splitByWords;
        private final boolean preserveIndex;
        private final boolean startFromEnd;

        private Runnable onAnimationFinishListener;
        private boolean allowCancel;
        public boolean ignoreRTL;
        public boolean updateAll;

        public AnimatedTextDrawable(boolean splitByWords, boolean preserveIndex, boolean startFromEnd) {
            this.splitByWords = splitByWords;
            this.preserveIndex = preserveIndex;
            this.startFromEnd = startFromEnd;
        }

        public void setAllowCancel(boolean allowCancel) {
            this.allowCancel = allowCancel;
        }

        public void setOnAnimationFinishListener(Runnable listener) {
            onAnimationFinishListener = listener;
        }

        private void applyAlphaInternal(float t) {
            textPaint.setAlpha((int) (alpha * t));
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.save();
            canvas.translate(bounds.left, bounds.top);
            int fullWidth = bounds.width();
            int fullHeight = bounds.height();
            if (currentParts != null && oldParts != null && t != 1) {
                float width = AndroidUtilities.lerp(oldWidth, currentWidth, t);
                float height = AndroidUtilities.lerp(oldHeight, currentHeight, t);
                canvas.translate(0, (fullHeight - height) / 2f);
                for (Part current : currentParts) {
                    int j = current.toOppositeIndex;
                    float x = current.offset, y = 0;
                    if (isRTL && !ignoreRTL) {
                        x = currentWidth - (x + current.width);
                    }
                    if (j >= 0) {
                        Part old = oldParts[j];
                        float oldX = old.offset;
                        if (isRTL && !ignoreRTL) {
                            oldX = oldWidth - (oldX + old.width);
                        }
                        x = AndroidUtilities.lerp(oldX - old.left, x - current.left, t);
                        applyAlphaInternal(1f);
                    } else {
                        x -= current.left;
                        y = -textPaint.getTextSize() * moveAmplitude * (1f - t) * (moveDown ? 1f : -1f);
                        applyAlphaInternal(t);
                    }
                    canvas.save();
                    float lwidth = j >= 0 ? width : currentWidth;
                    if ((gravity | ~Gravity.LEFT) != ~0) {
                        if ((gravity | ~Gravity.RIGHT) == ~0) {
                            x += fullWidth - lwidth;
                        } else if ((gravity | ~Gravity.CENTER_HORIZONTAL) == ~0) {
                            x += (fullWidth - lwidth) / 2f;
                        } else if (isRTL && !ignoreRTL) {
                            x += fullWidth - lwidth;
                        }
                    }
                    canvas.translate(x, y);
                    current.layout.draw(canvas);
                    canvas.restore();
                }
                for (Part old : oldParts) {
                    int j = old.toOppositeIndex;
                    if (j >= 0) {
                        continue;
                    }
                    float x = old.offset;
                    float y = textPaint.getTextSize() * moveAmplitude * t * (moveDown ? 1f : -1f);
                    applyAlphaInternal(1f - t);
                    canvas.save();
                    if (isRTL && !ignoreRTL) {
                        x = oldWidth - (x + old.width);
                    }
                    x -= old.left;
                    if ((gravity | ~Gravity.LEFT) != ~0) {
                        if ((gravity | ~Gravity.RIGHT) == ~0) {
                            x += fullWidth - oldWidth;
                        } else if ((gravity | ~Gravity.CENTER_HORIZONTAL) == ~0) {
                            x += (fullWidth - oldWidth) / 2f;
                        } else if (isRTL && !ignoreRTL) {
                            x += fullWidth - oldWidth;
                        }
                    }
                    canvas.translate(x, y);
                    old.layout.draw(canvas);
                    canvas.restore();
                }
            } else {
                canvas.translate(0, (fullHeight - currentHeight) / 2f);
                if (currentParts != null) {
                    applyAlphaInternal(1f);
                    for (Part currentPart : currentParts) {
                        canvas.save();
                        float x = currentPart.offset;
                        if (isRTL && !ignoreRTL) {
                            x = currentWidth - (x + currentPart.width);
                        }
                        x -= currentPart.left;
                        if ((gravity | ~Gravity.LEFT) != ~0) {
                            if ((gravity | ~Gravity.RIGHT) == ~0) {
                                x += fullWidth - currentWidth;
                            } else if ((gravity | ~Gravity.CENTER_HORIZONTAL) == ~0) {
                                x += (fullWidth - currentWidth) / 2f;
                            } else if (isRTL && !ignoreRTL) {
                                x += fullWidth - currentWidth;
                            }
                        }
//                        boolean isAppeared = currentLayoutToOldIndex != null && i < currentLayoutToOldIndex.length && currentLayoutToOldIndex[i] < 0;
                        canvas.translate(x, 0);
                        currentPart.layout.draw(canvas);
                        canvas.restore();
                    }
                }
            }
            canvas.restore();
        }

        public boolean isAnimating() {
            return animator != null && animator.isRunning();
        }

        public void setText(CharSequence text) {
            setText(text, true);
        }

        public void setText(CharSequence text, boolean animated) {
            setText(text, animated, true);
        }

        public void setText(CharSequence text, boolean animated, boolean moveDown) {
            if (this.currentText == null || text == null) {
                animated = false;
            }
            if (text == null) {
                text = "";
            }
            final int width = bounds.width();
            if (animated) {
                if (allowCancel) {
                    if (animator != null) {
                        animator.cancel();
                        animator = null;
                    }
                } else if (isAnimating()) {
                    toSetText = text;
                    toSetTextMoveDown = moveDown;
                    return;
                }

                if (text.equals(currentText)) {
                    return;
                }

                oldText = currentText;
                currentText = text;

                ArrayList<Part> currentParts = new ArrayList<>();
                ArrayList<Part> oldParts = new ArrayList<>();

                currentWidth = currentHeight = 0;
                oldWidth = oldHeight = 0;
                isRTL = AndroidUtilities.isRTL(currentText);

                // order execution matters
                RegionCallback onEqualRegion = (part, from, to) -> {
                    StaticLayout layout = makeLayout(part, width - (int) Math.ceil(Math.min(currentWidth, oldWidth)));
                    final Part currentPart = new Part(layout, currentWidth, oldParts.size());
                    final Part oldPart = new Part(layout, oldWidth, oldParts.size());
                    currentParts.add(currentPart);
                    oldParts.add(oldPart);
                    float partWidth = currentPart.width;
                    currentWidth += partWidth;
                    oldWidth += partWidth;
                    currentHeight = Math.max(currentHeight, layout.getHeight());
                    oldHeight = Math.max(oldHeight, layout.getHeight());
                };
                RegionCallback onNewPart = (part, from, to) -> {
                    StaticLayout layout = makeLayout(part, width - (int) Math.ceil(currentWidth));
                    final Part currentPart = new Part(layout, currentWidth, -1);
                    currentParts.add(currentPart);
                    currentWidth += currentPart.width;
                    currentHeight = Math.max(currentHeight, layout.getHeight());
                };
                RegionCallback onOldPart = (part, from, to) -> {
                    StaticLayout layout = makeLayout(part, width - (int) Math.ceil(oldWidth));
                    final Part oldPart = new Part(layout, oldWidth, -1);
                    oldParts.add(oldPart);
                    oldWidth += oldPart.width;
                    oldHeight = Math.max(oldHeight, layout.getHeight());
                };

                CharSequence from = splitByWords ? new WordSequence(oldText) : oldText;
                CharSequence to = splitByWords ? new WordSequence(currentText) : currentText;

                diff(from, to, onEqualRegion, onNewPart, onOldPart);
//                betterDiff(from, to, onEqualRegion, onNewPart, onOldPart);

                if (this.currentParts == null || this.currentParts.length != currentParts.size()) {
                    this.currentParts = new Part[currentParts.size()];
                }
                currentParts.toArray(this.currentParts);
                if (this.oldParts == null || this.oldParts.length != oldParts.size()) {
                    this.oldParts = new Part[oldParts.size()];
                }
                oldParts.toArray(this.oldParts);
                if (animator != null) {
                    animator.cancel();
                }

                this.moveDown = moveDown;
                animator = ValueAnimator.ofFloat(t = 0f, 1f);
                animator.addUpdateListener(anm -> {
                    t = (float) anm.getAnimatedValue();
                    invalidateSelf();
                });
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        AnimatedTextDrawable.this.oldParts = null;
                        oldText = null;
                        oldWidth = 0;
                        t = 0;
                        invalidateSelf();
                        animator = null;

                        if (toSetText != null) {
                            setText(toSetText, true, toSetTextMoveDown);
                            toSetText = null;
                            toSetTextMoveDown = false;
                        } else if (onAnimationFinishListener != null) {
                            onAnimationFinishListener.run();
                        }
                    }
                });
                animator.setStartDelay(animateDelay);
                animator.setDuration(animateDuration);
                animator.setInterpolator(animateInterpolator);
                animator.start();
            } else {
                if (animator != null) {
                    animator.cancel();
                }
                animator = null;
                toSetText = null;
                toSetTextMoveDown = false;
                t = 0;

                if (!text.equals(currentText)) {
                    currentParts = new Part[1];
                    currentParts[0] = new Part(makeLayout(currentText = text, width), 0, -1);
                    currentWidth = currentParts[0].width;
                    currentHeight = currentParts[0].layout.getHeight();
                    isRTL = AndroidUtilities.isRTL(currentText);
                }

                oldParts = null;
                oldText = null;
                oldWidth = 0;
                oldHeight = 0;

                invalidateSelf();
            }
        }

        public CharSequence getText() {
            return currentText;
        }

        public float getWidth() {
            return Math.max(currentWidth, oldWidth);
        }

        public float getCurrentWidth() {
            if (currentParts != null && oldParts != null) {
                return AndroidUtilities.lerp(oldWidth, currentWidth, t);
            }
            return currentWidth;
        }

        public float getHeight() {
            return currentHeight;
        }

        private StaticLayout makeLayout(CharSequence textPart, int width) {
            if (width <= 0) {
                width = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return StaticLayout.Builder.obtain(textPart, 0, textPart.length(), textPaint, width)
                        .setMaxLines(1)
                        .setLineSpacing(0, 1)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .setEllipsizedWidth(width)
                        .build();
            } else {
                return new StaticLayout(
                    textPart,
                    0, textPart.length(),
                    textPaint,
                    width,
                    Layout.Alignment.ALIGN_NORMAL,
                    1,
                    0,
                    false,
                    TextUtils.TruncateAt.END,
                    width
                );
            }
        }

        private static class WordSequence implements CharSequence {
            private static final char SPACE = ' ';

            private final CharSequence[] words;

            public WordSequence(CharSequence text) {
                int length;
                if (text == null) {
                    words = new CharSequence[0];
                    return;
                }
                length = text.length();
                int spacesCount = 0;
                for (int i = 0; i < length; ++i) {
                    if (text.charAt(i) == SPACE) {
                        spacesCount++;
                    }
                }
                int j = 0;
                words = new CharSequence[spacesCount + 1];
                int start = 0;
                for (int i = 0; i <= length; ++i) {
                    if (i == length || text.charAt(i) == SPACE) {
                        words[j++] = text.subSequence(start, i + (i < length ? 1 : 0));
                        start = i + 1;
                    }
                }
            }

            public CharSequence wordAt(int i) {
                if (i < 0 || i >= words.length) {
                    return null;
                }
                return words[i];
            }

            @Override
            public int length() {
                return words.length;
            }

            @Override
            public char charAt(int i) {
                for (CharSequence word : words) {
                    if (i < word.length())
                        return word.charAt(i);
                    i -= word.length();
                }
                return 0;
            }

            @NonNull
            @Override
            public CharSequence subSequence(int from, int to) {
                return TextUtils.concat(Arrays.copyOfRange(words, from, to));
            }

            @Override
            @NonNull
            public String toString() {
                StringBuilder sb = new StringBuilder();
                for (CharSequence word : words) {
                    sb.append(word);
                }
                return sb.toString();
            }

            public CharSequence toCharSequence() {
                return TextUtils.concat(words);
            }

            @Override
            public IntStream chars() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return toCharSequence().chars();
                }
                return null;
            }

            @Override
            public IntStream codePoints() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return toCharSequence().codePoints();
                }
                return null;
            }
        }

        public static boolean partEquals(CharSequence a, CharSequence b, int aIndex, int bIndex) {
            if (a instanceof WordSequence && b instanceof WordSequence) {
                CharSequence wordA = ((WordSequence) a).wordAt(aIndex);
                CharSequence wordB = ((WordSequence) b).wordAt(bIndex);
                return wordA == null && wordB == null || wordA != null && wordA.equals(wordB);
            }
            return (a == null && b == null || a != null && b != null && a.charAt(aIndex) == b.charAt(bIndex));
        }

        private void diff(final CharSequence oldText, final CharSequence newText, RegionCallback onEqualPart, RegionCallback onNewPart, RegionCallback onOldPart) {
            if (updateAll) {
                onOldPart.run(oldText, 0, oldText.length());
                onNewPart.run(newText, 0, newText.length());
                return;
            }
            if (preserveIndex) {
                boolean equal = true;
                int start = 0;
                int minLength = Math.min(newText.length(), oldText.length());
                if (startFromEnd) {
                    ArrayList<Integer> indexes = new ArrayList<>();
                    boolean eq = true;
                    for (int i = 0; i <= minLength; ++i) {
                        int a = newText.length() - i - 1;
                        int b = oldText.length() - i - 1;
                        boolean thisEqual = a >= 0 && b >= 0 && partEquals(newText, oldText, a, b);
                        if (equal != thisEqual || i == minLength) {
                            if (i - start > 0) {
                                if (indexes.size() == 0) {
                                    eq = equal;
                                }
                                indexes.add(i - start);
                            }
                            equal = thisEqual;
                            start = i;
                        }
                    }
                    int a = newText.length() - minLength;
                    int b = oldText.length() - minLength;
                    if (a > 0) {
                        onNewPart.run(newText.subSequence(0, a), 0, a);
                    }
                    if (b > 0) {
                        onOldPart.run(oldText.subSequence(0, b), 0, b);
                    }
                    for (int i = indexes.size() - 1; i >= 0; --i) {
                        int count = indexes.get(i);
                        if ((i % 2 == 0) == eq) {
                            if (newText.length() > oldText.length()) {
                                onEqualPart.run(newText.subSequence(a, a + count), a, a + count);
                            } else {
                                onEqualPart.run(oldText.subSequence(b, b + count), b, b + count);
                            }
                        } else {
                            onNewPart.run(newText.subSequence(a, a + count), a, a + count);
                            onOldPart.run(oldText.subSequence(b, b + count), b, b + count);
                        }
                        a += count;
                        b += count;
                    }
                } else {
                    for (int i = 0; i <= minLength; ++i) {
                        boolean thisEqual = i < minLength && partEquals(newText, oldText, i, i);
                        if (equal != thisEqual || i == minLength) {
                            if (i - start > 0) {
                                if (equal) {
                                    onEqualPart.run(newText.subSequence(start, i), start, i);
                                } else {
                                    onNewPart.run(newText.subSequence(start, i), start, i);
                                    onOldPart.run(oldText.subSequence(start, i), start, i);
                                }
                            }
                            equal = thisEqual;
                            start = i;
                        }
                    }
                    if (newText.length() - minLength > 0) {
                        onNewPart.run(newText.subSequence(minLength, newText.length()), minLength, newText.length());
                    }
                    if (oldText.length() - minLength > 0) {
                        onOldPart.run(oldText.subSequence(minLength, oldText.length()), minLength, oldText.length());
                    }
                }
            } else {
                int astart = 0, bstart = 0;
                boolean equal = true;
                int a = 0, b = 0;
                int minLength = Math.min(newText.length(), oldText.length());
                for (; a <= minLength; ++a) {
                    boolean thisEqual = a < minLength && partEquals(newText, oldText, a, b);
                    if (equal != thisEqual || a == minLength) {
                        if (a == minLength) {
                            a = newText.length();
                            b = oldText.length();
                        }
                        int alen = a - astart, blen = b - bstart;
                        if (alen > 0 || blen > 0) {
                            if (alen == blen && equal) {
                                // equal part on [astart, a)
                                onEqualPart.run(newText.subSequence(astart, a), astart, a);
                            } else {
                                if (alen > 0) {
                                    // new part on [astart, a)
                                    onNewPart.run(newText.subSequence(astart, a), astart, a);
                                }
                                if (blen > 0) {
                                    // old part on [bstart, b)
                                    onOldPart.run(oldText.subSequence(bstart, b), bstart, b);
                                }
                            }
                        }
                        equal = thisEqual;
                        astart = a;
                        bstart = b;
                    }
                    if (thisEqual) {
                        b++;
                    }
                }
            }
        }

        public void setTextSize(float textSizePx) {
            textPaint.setTextSize(textSizePx);
        }

        public float getTextSize() {
            return textPaint.getTextSize();
        }

        public void setTextColor(int color) {
            textPaint.setColor(color);
            alpha = Color.alpha(color);
        }

        public void setGravity(int gravity) {
            this.gravity = gravity;
        }

        public void setAnimationProperties(float moveAmplitude, long startDelay, long duration, TimeInterpolator interpolator) {
            this.moveAmplitude = moveAmplitude;
            animateDelay = startDelay;
            animateDuration = duration;
            animateInterpolator = interpolator;
        }

        private interface RegionCallback {
            void run(CharSequence part, int start, int end);
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha;
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            textPaint.setColorFilter(colorFilter);
        }

        @Deprecated @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }

        @Override
        public void setBounds(@NonNull Rect bounds) {
            super.setBounds(bounds);
            this.bounds.set(bounds);
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            this.bounds.set(left, top, right, bottom);
        }

        @NonNull
        @Override
        public Rect getDirtyBounds() {
            return this.bounds;
        }

    }

    private final AnimatedTextDrawable drawable;
    private int lastMaxWidth;

    public CharSequence text;
    public CharSequence hint;

    private CharSequence toSetText;
    private boolean toSetMoveDown;

    public AnimatedTextView(Context context) {
        this(context, false, false, false);
    }

    public AnimatedTextView(Context context, boolean splitByWords, boolean preserveIndex, boolean startFromEnd) {
        super(context);
        drawable = new AnimatedTextDrawable(splitByWords, preserveIndex, startFromEnd);
        drawable.setCallback(this);
        drawable.setOnAnimationFinishListener(() -> {
            if (toSetText != null) {
                // wrapped toSetText here to do requestLayout()
                AnimatedTextView.this.setText(toSetText, toSetMoveDown, true);
                toSetText = null;
                toSetMoveDown = false;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (lastMaxWidth != width && getLayoutParams().width != 0) {
            drawable.setBounds(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom());
            setTextInternal(drawable.getText(), false, true);
        }
        lastMaxWidth = width;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST || width == 0) {
            width = getPaddingLeft() + (int) Math.ceil(drawable.getWidth()) + getPaddingRight();
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST || height == 0) {
            height = getPaddingTop() + (int) Math.ceil(drawable.getHeight()) + getPaddingBottom();
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        drawable.setBounds(getPaddingLeft(), getPaddingTop(), getMeasuredWidth() - getPaddingRight(), getMeasuredHeight() - getPaddingBottom());
        drawable.draw(canvas);
    }

    public void setText(CharSequence text) {
        setText(text, true, true);
    }

    public void setText(CharSequence text, boolean withAnimation, boolean moveDown) {
        this.text = text;
        if (this.text != null && this.text.length() > 0) {
            setTextInternal(text, withAnimation, moveDown);
        } else {
            setHint(hint, withAnimation, moveDown);
        }
    }

    private boolean first = true;
    private void setTextInternal(CharSequence text, boolean animated, boolean moveDown) {
        animated = !first && animated;
        first = false;
        if (animated) {
            if (drawable.allowCancel) {
                if (drawable.animator != null) {
                    drawable.animator.cancel();
                    drawable.animator = null;
                }
            } else if (drawable.isAnimating()) {
                toSetText = text;
                toSetMoveDown = moveDown;
                return;
            }
        }
        int wasWidth = (int) drawable.getWidth();
        drawable.setBounds(getPaddingLeft(), getPaddingTop(), lastMaxWidth - getPaddingRight(), getMeasuredHeight() - getPaddingBottom());
        drawable.setText(text, animated, moveDown);
        if (wasWidth < drawable.getWidth() || !animated && wasWidth != drawable.getWidth()) {
            requestLayout();
        }
    }

    public final void setHint(CharSequence hint, boolean withAnimation, boolean moveDown) {
        this.hint = TextUtils.stringOrSpannedString(hint);
        if (text == null || text.length() == 0) {
            setHintInternal(hint, withAnimation, moveDown);
        }
    }

    private void setHintInternal(CharSequence hint, boolean withAnimation, boolean moveDown) {
        setTextInternal(hint, withAnimation, moveDown);
    }

    public int width() {
        return getPaddingLeft() + (int) Math.ceil(drawable.getCurrentWidth()) + getPaddingRight();
    }

    public void setTextColor(int color) {
        drawable.setTextColor(color);
        invalidate();
    }

    public CharSequence getText() {
        return text;
    }

    public void setTextSize(float textSizePx) {
        drawable.setTextSize(textSizePx);
    }

    public void setGravity(int gravity) {
        drawable.setGravity(gravity);
    }

    public void setAnimationProperties(float moveAmplitude, long startDelay, long duration, TimeInterpolator interpolator) {
        drawable.setAnimationProperties(moveAmplitude, startDelay, duration, interpolator);
    }

    public AnimatedTextDrawable getDrawable() {
        return drawable;
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        super.invalidateDrawable(drawable);
        invalidate();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.TextView");
        info.setText(getText());
    }

}