package com.waitou.towards.model.graffiti;

import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableFloat;
import android.databinding.ObservableInt;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.waitou.photo_library.PhotoPickerFinal;
import com.waitou.photo_library.bean.PhotoInfo;
import com.waitou.towards.R;
import com.waitou.towards.bean.GraffitiToolInfo;
import com.waitou.towards.databinding.ItemSeekBarBinding;
import com.waitou.towards.enums.GraffitiToolEnum;
import com.waitou.towards.util.KitUtils;
import com.waitou.towards.view.dialog.BaseDialog;
import com.waitou.towards.view.dialog.ListOfDialog;
import com.waitou.wt_library.base.XPresent;
import com.waitou.wt_library.recycler.LayoutManagerUtil;
import com.waitou.wt_library.recycler.adapter.BaseViewAdapter;
import com.waitou.wt_library.recycler.adapter.SingleTypeAdapter;
import com.xw.repo.BubbleSeekBar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by waitou on 17/3/27.
 * 涂鸦
 */

public class GraffitiPresenter extends XPresent<GraffitiActivity> implements BaseViewAdapter.Presenter {

    //工具选择的type
    public ObservableInt           toolType      = new ObservableInt(0);
    //画笔粗细控制
    public ObservableInt           toolWidth     = new ObservableInt(14);
    //画笔颜色
    public ObservableInt           toolColor     = new ObservableInt(Color.parseColor("#99ff0000"));
    //图片缩放
    public ObservableFloat         scale         = new ObservableFloat(1.0f);
    //图片旋转
    public ObservableInt           rotate        = new ObservableInt(0);
    //左移右移
    public ObservableInt           leftMoveRight = new ObservableInt(0);
    //上移下移
    public ObservableInt           topMoveBottom = new ObservableInt(0);
    //上传的图片
    public ObservableField<Bitmap> bitmapField   = new ObservableField<>();
    //图片是否可以操作
    public ObservableBoolean       enable        = new ObservableBoolean(checkBitmap());

    private SingleTypeAdapter<GraffitiToolInfo> mGraffitiToolAdapter;
    private BaseDialog                          mToolDialog;
    private BaseDialog                          mSeekBarDialog;

    /**
     * 选择工具
     */
    public void selectToolShowDialog() {
        if (mGraffitiToolAdapter == null) {
            mGraffitiToolAdapter = new SingleTypeAdapter<>(getV(), R.layout.item_select_tool);
            mGraffitiToolAdapter.setPresenter(this);
            List<GraffitiToolInfo> toolInfoList = new ArrayList<>();
            for (GraffitiToolEnum toolEnum : GraffitiToolEnum.values()) {
                GraffitiToolInfo info = new GraffitiToolInfo(Objects.requireNonNull(
                        ContextCompat.getDrawable(getV(), toolEnum.getRedId())), toolEnum.getTool());
                toolInfoList.add(info);
            }
            mGraffitiToolAdapter.set(toolInfoList);
        }
        if (mToolDialog == null) {
            mToolDialog = new ListOfDialog(getV())
                    .setLayoutManager(LayoutManagerUtil.getGridLayoutManager(getV(), 3))
                    .setAdapter(mGraffitiToolAdapter)
                    .setTitle("工具选择");
        }
        mToolDialog.show();
    }

    /**
     * 选择工具的点击回调方法
     */
    public void selectToolItemClick(int position) {
        toolType.set(position);
        mToolDialog.dismiss();
    }

    /**
     * 调整画笔宽度
     */
    public void seekToolWidthDialog() {
        if (mSeekBarDialog == null) {
            mSeekBarDialog = new BaseDialog(getV());
            ItemSeekBarBinding inflate = DataBindingUtil.inflate(getV().getLayoutInflater(), R.layout.item_seek_bar, null, false);
            inflate.seekBar.setProgress(toolWidth.get());
            inflate.seekBar.setOnProgressChangedListener(mChangedListener);
            mSeekBarDialog.setDialogContentView(inflate.getRoot());
            mSeekBarDialog.setTitle("画笔大小");
        }
        mSeekBarDialog.show();
    }

    private BubbleSeekBar.OnProgressChangedListenerAdapter mChangedListener = new BubbleSeekBar.OnProgressChangedListenerAdapter() {
        @Override
        public void getProgressOnActionUp(int progress, float progressFloat) {
            toolWidth.set(progress);
            mSeekBarDialog.dismiss();
        }
    };

    /**
     * 选择颜色
     */
    public void selectColorDialog() {
        new ColorPickerDialog(getV(), toolColor.get(), color -> toolColor.set(color)).show();
    }

    /**
     * 上传图片
     */
    public void uploadPic() {
        getV().pend(PhotoPickerFinal.get()
                .with(getV())
                .isMultiMode(false)
                .isCrop(true)
                .executePhoto(info -> {
                    PhotoInfo photoInfo = info.get(0);
                    Bitmap resource = ImageUtils.getBitmap(photoInfo.photoPath, photoInfo.photoWidth, photoInfo.photoHeight);
                    reset();
                    bitmapField.set(resource);
                    enable.set(checkBitmap());
                }));
    }

    /**
     * 放大图片
     */
    public void scaleBigPic() {
        this.scale.set(scale.get() + 0.05f);
    }

    /**
     * 缩小图片
     */
    public void scaleSmallPic() {
        float scale = this.scale.get() - 0.05f;
        if (scale < 0.1) {
            return;
        }
        this.scale.set(scale);
    }

    /**
     * 旋转图片 90
     */
    public void rotatePic() {
        this.rotate.set((rotate.get() + 90) % 360);
    }

    /**
     * 重置图片属性
     */
    public void reset() {
        this.scale.set(1f);
        this.rotate.set(0);
        this.leftMoveRight.set(0);
        this.topMoveBottom.set(0);
    }

    /**
     * 图片向右移动
     */
    public Consumer<Integer> moveRight() {
        return integer -> interval(integer, aLong -> this.leftMoveRight.set(this.leftMoveRight.get() + 1));
    }

    /**
     * 图片向左移动
     */
    public Consumer<Integer> moveLeft() {
        return integer -> interval(integer, aLong -> this.leftMoveRight.set(this.leftMoveRight.get() - 1));
    }

    /**
     * 图片向上移动
     */
    public Consumer<Integer> moveTop() {
        return integer -> interval(integer, aLong -> this.topMoveBottom.set(this.topMoveBottom.get() - 1));
    }

    /**
     * 图片向下移动
     */
    public Consumer<Integer> moveBottom() {
        return integer -> interval(integer, aLong -> this.topMoveBottom.set(this.topMoveBottom.get() + 1));
    }

    private boolean checkBitmap() {
        return bitmapField.get() != null;
    }

    private Disposable Disposable;

    private void interval(int actionMasked, Consumer<Long> action) {
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            Disposable = Flowable.interval(0, 10, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .onBackpressureDrop()
                    .subscribe(action);
        }
        if (actionMasked == MotionEvent.ACTION_UP) {
            if (Disposable != null && !Disposable.isDisposed()) {
                Disposable.dispose();
            }
        }
    }

    @BindingAdapter("move")
    public static void onTouch(View view, Consumer<Integer> action) {
        view.setOnTouchListener((v, event) -> {
            try {
                action.accept(event.getAction());
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        });
    }

    /**
     * 图片保存
     */
    public void save(GraffitiView graffitiView, GraffitiPicView graffitiPicView) {
        PermissionUtils.permission(PermissionConstants.STORAGE)
                .rationale(shouldRequest -> shouldRequest.again(true))
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(List<String> permissionsGranted) {
                        Bitmap bitmap = Bitmap.createBitmap(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight(), Bitmap.Config.ARGB_8888);
                        Canvas bitCanvas = new Canvas(bitmap);
                        if (graffitiPicView.checkSave() || graffitiView.checkSave()) {
                            graffitiPicView.doDraw(bitCanvas);
                            graffitiView.doDraw(bitCanvas);
                            String imageCacheSavePath = PathUtils.getExternalPicturesPath() + File.separator + "IMAGE_" + System.currentTimeMillis() + ".jpg";
                            boolean save = ImageUtils.save(bitmap, imageCacheSavePath, Bitmap.CompressFormat.JPEG, true);
                            if (save) {
                                KitUtils.saveImageToGallery(new File(imageCacheSavePath));
                                ToastUtils.showShort("图片成功保存到相册O(∩_∩)O~");
                            }
                        } else {
                            ToastUtils.showShort("先绘制点什么吧!╮(╯▽╰)╭");
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                        ToastUtils.showShort(ObjectUtils.isEmpty(permissionsDeniedForever) ? "保存图片需要授权该权限！" : "请到应用设置中开启存储权限!"); //拒绝了权限
                    }
                }).request();
    }
}
