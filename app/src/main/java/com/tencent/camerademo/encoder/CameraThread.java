package com.tencent.camerademo.encoder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.serenegiant.encoder.MediaVideoBufferEncoder;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.tencent.av.sdk.AVAudioCtrl;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by gengqifu on 2018/3/14.
 */

public final class CameraThread extends Thread {
    private static final String TAG = "CameraThread";
    private static final boolean DEBUG = false;
    public static final int MSG_OPEN = 0;
    public static final int MSG_CLOSE = 1;
    public static final int MSG_PREVIEW_START = 2;
    public static final int MSG_PREVIEW_STOP = 3;
    public static final int MSG_CAPTURE_STILL = 4;
    public static final int MSG_CAPTURE_START = 5;
    public static final int MSG_CAPTURE_STOP = 6;
    public static final int MSG_MEDIA_UPDATE = 7;
    public static final int MSG_RELEASE = 9;
    public static final int MSG_CAMERA_FOUCS = 10;
    public static final int MSG_CAPTURE_FRAME = 11;
    public static final int MSG_AUDIO_DATA = 12;

    private final Object mSync = new Object();
    private final Class<? extends CameraHandler> mHandlerClass;
    private final WeakReference<Context> mWeakParent;
    //private final WeakReference<CameraViewInterface> mWeakCameraView;
    private final int mEncoderType;
    //private final Set<AbstractUVCCameraHandler.CameraCallback> mCallbacks = new CopyOnWriteArraySet<AbstractUVCCameraHandler.CameraCallback>();
    private int mWidth, mHeight, mPreviewMode;
    private float mBandwidthFactor;
    private boolean mIsPreviewing;
    private boolean mIsRecording;

    // 播放声音
//		private SoundPool mSoundPool;
//		private int mSoundId;
    //private AbstractUVCCameraHandler mHandler;
    // 处理与Camera相关的逻辑，比如获取byte数据流等
    private UVCCamera mUVCCamera;

    //		private MediaMuxerWrapper mMuxer;
    private MediaVideoBufferEncoder mVideoEncoder;
    private Mp4MediaMuxer mMuxer;
    private String videoPath;
//		private boolean isAudioThreadStart;

    public class CameraHandler extends Handler {
        boolean mReleased = false;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_OPEN:
                    handleOpen((USBMonitor.UsbControlBlock) msg.obj);
                    break;
                case MSG_CLOSE:
                    handleClose();
                    break;
                case MSG_PREVIEW_START:
                    //handleStartPreview(msg.obj);
                    break;
                case MSG_PREVIEW_STOP:
                    //handleStopPreview();
                    break;
                case MSG_CAPTURE_STILL:
                    //handleCaptureStill((String)msg.obj);
                    break;
                case MSG_CAPTURE_START:
                    handleStartRecording((RecordParams) msg.obj);
                    break;
                case MSG_CAPTURE_STOP:
                    handleStopRecording();
                    break;
                case MSG_MEDIA_UPDATE:
                    handleUpdateMedia((String) msg.obj);
                    break;
                case MSG_RELEASE:
                    handleRelease();
                    break;
                // 自动对焦
                case MSG_CAMERA_FOUCS:
                    handleCameraFoucs();
                    break;
                case MSG_CAPTURE_FRAME:
                    Bundle bundle = msg.getData();
                    byte[] data = bundle.getByteArray("ba");
                    handleCaptureFrme(data);
                    break;
                case MSG_AUDIO_DATA:
                    /*Bundle audioBundle = msg.getData();
                    byte[] audioData = audioBundle.getByteArray("ad");
                    handleAudioData(audioData);*/
                    AVAudioCtrl.AudioFrameWithByteBuffer audioFrameWithByteBuffer = (AVAudioCtrl.AudioFrameWithByteBuffer)msg.obj;
                    handleAudioData(audioFrameWithByteBuffer);
                    break;
                // 音频线程
//			case MSG_AUDIO_START:
//				thread.startAudioRecord();
//				break;
//			case MSG_AUDIO_STOP:
//				thread.stopAudioRecord();
//				break;
                default:
                    throw new RuntimeException("unsupported message:what=" + msg.what);
            }
        }
    }

    //private void handleAudioData(byte[] data) {
    public void handleAudioData(AVAudioCtrl.AudioFrameWithByteBuffer audioFrameWithByteBuffer) {
        /*Message msg = Message.obtain();
        msg.what = AACEncodeConsumer.AUDIO_DATA;
        Bundle bundle = new Bundle();
        bundle.putByteArray("ad", data);
        if(mAacConsumer != null) {
            mAacConsumer.mHandler.sendMessage(msg);
        }*/
        int len = audioFrameWithByteBuffer.data.capacity();
        byte[] audio = new byte[len];
        audioFrameWithByteBuffer.data.get(audio);
        audioFrameWithByteBuffer.data.clear();
        if(mAacConsumer != null) {
            mAacConsumer.setAudioData(audio, audio.length);
        }
    }

    private void handleCaptureFrme(byte[] data) {

        // 捕获图片
        if (isCaptureStill) {

        }
        // 视频
        if (mH264Consumer != null) {
            /*int len = frame.capacity();
            byte[] yuv = new byte[len];
            if (frame.remaining() >= len) {
                frame.get(yuv);
                // 修改分辨率参数

            }*/
            mH264Consumer.setRawYuv(data, mWidth, mHeight);
        }
    }

    public CameraHandler mHandler = new CameraHandler();

    public void setListener(OnEncodeResultListener listener) {
        mListener = listener;
    }

    public OnEncodeResultListener mListener;

    public interface OnEncodeResultListener {
        void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type);

        void onRecordResult(String videoPath);
    }

    /**
     * 构造方法
     * <p>
     * clazz 继承于AbstractUVCCameraHandler
     * parent Activity子类
     * cameraView 用于捕获静止图像
     * encoderType 0表示使用MediaSurfaceEncoder;1表示使用MediaVideoEncoder, 2表示使用MediaVideoBufferEncoder
     * width  分辨率的宽
     * height 分辨率的高
     * format 颜色格式，0为FRAME_FORMAT_YUYV；1为FRAME_FORMAT_MJPEG
     * bandwidthFactor
     */
    public CameraThread(final Class<? extends CameraHandler> clazz,
                        final Context parent,
                        final int encoderType, final int width, final int height, final int format,
                        final float bandwidthFactor, OnEncodeResultListener listener) {

        super("CameraThread");
        mHandlerClass = clazz;
        mEncoderType = encoderType;
        mWidth = width;
        mHeight = height;
        mPreviewMode = format;
        mBandwidthFactor = bandwidthFactor;
        mWeakParent = new WeakReference<>(parent);
        mListener = listener;
        //mWeakCameraView = new WeakReference<>(cameraView);
//			loadShutterSound(parent);
    }

    @Override
    protected void finalize() throws Throwable {
        Log.i(TAG, "CameraThread#finalize");
        super.finalize();
    }

    /*public AbstractUVCCameraHandler getHandler() {
        if (DEBUG) Log.v(TAG_THREAD, "getHandler:");
        synchronized (mSync) {
            if (mHandler == null)
                try {
                    mSync.wait();
                } catch (final InterruptedException e) {
                }
        }
        return mHandler;
    }*/

    public int getWidth() {
        synchronized (mSync) {
            return mWidth;
        }
    }

    public int getHeight() {
        synchronized (mSync) {
            return mHeight;
        }
    }

    public boolean isCameraOpened() {
        synchronized (mSync) {
            return mUVCCamera != null;
        }
    }

    public boolean isPreviewing() {
        synchronized (mSync) {
            return mUVCCamera != null && mIsPreviewing;
        }
    }

    public boolean isRecording() {
        synchronized (mSync) {
            return (mUVCCamera != null) && (mMuxer != null);
        }
    }

//		public boolean isAudioRecording(){
//			synchronized (mSync){
//				return isAudioThreadStart;
//			}
//		}

    public boolean isEqual(final UsbDevice device) {
        return (mUVCCamera != null) && (mUVCCamera.getDevice() != null) && mUVCCamera.getDevice().equals(device);
    }

    public void handleOpen(final USBMonitor.UsbControlBlock ctrlBlock) {
        if (DEBUG) Log.v(TAG, "handleOpen:");
        handleClose();
        try {
            final UVCCamera camera = new UVCCamera();
            camera.open(ctrlBlock);
            synchronized (mSync) {
                mUVCCamera = camera;
            }
            //callOnOpen();
        } catch (final Exception e) {
            //callOnError(e);
        }
        if (DEBUG)
            Log.i(TAG, "supportedSize:" + (mUVCCamera != null ? mUVCCamera.getSupportedSize() : null));
    }

    public void handleClose() {
        if (DEBUG) Log.v(TAG, "handleClose:");
        handleStopRecording();
        final UVCCamera camera;
        synchronized (mSync) {
            camera = mUVCCamera;
            mUVCCamera = null;
        }
        if (camera != null) {
            camera.stopPreview();
            camera.destroy();
            //callOnClose();
        }
    }

    /*public void handleStartPreview(final Object surface) {
        if (DEBUG) Log.v(TAG_THREAD, "handleStartPreview:");
        if ((mUVCCamera == null) || mIsPreviewing) return;
        try {
            mUVCCamera.setPreviewSize(mWidth, mHeight, 1, 31, mPreviewMode, mBandwidthFactor);
            if(mPreviewListener != null){
                mPreviewListener.onPreviewResult(true);
            }
            // 获取USB Camera预览数据
            mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);

        } catch (final IllegalArgumentException e) {
            // 添加分辨率参数合法性检测
            if(mPreviewListener != null){
                mPreviewListener.onPreviewResult(false);
            }
        }
        if (surface instanceof SurfaceHolder) {
            mUVCCamera.setPreviewDisplay((SurfaceHolder)surface);
        } if (surface instanceof Surface) {
            mUVCCamera.setPreviewDisplay((Surface)surface);
        } else {
            mUVCCamera.setPreviewTexture((SurfaceTexture)surface);
        }
        mUVCCamera.startPreview();
        mUVCCamera.updateCameraParams();
        synchronized (mSync) {
            mIsPreviewing = true;
        }
        callOnStartPreview();
    }*/

    /*public void handleStopPreview() {
        if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:");
        if (mIsPreviewing) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
            }
            synchronized (mSync) {
                mIsPreviewing = false;
                mSync.notifyAll();
            }
            callOnStopPreview();
        }
        if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:finished");
    }
*/
    // 捕获静态图片
    /*public void handleCaptureStill(final String path) {
        if (DEBUG) Log.v(TAG, "handleCaptureStill:");
        final Activity parent = mWeakParent.get();
        if (parent == null) return;
//			mSoundPool.play(mSoundId, 0.2f, 0.2f, 0, 0, 1.0f);	// play shutter sound
        try {
            final Bitmap bitmap = mWeakCameraView.get().captureStillImage(mWidth,mHeight);
            // get buffered output stream for saving a captured still image as a file on external storage.
            // the file name is came from current time.
            // You should use extension name as same as CompressFormat when calling Bitmap#compress.
            final File outputFile = TextUtils.isEmpty(path)
                    ? MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".jpg")
                    : new File(path);
            final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
            try {
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    os.flush();
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_MEDIA_UPDATE, outputFile.getPath()));
                } catch (final IOException e) {
                }
            } finally {
                os.close();
            }
            if(mCaptureListener != null) {
                mCaptureListener.onCaptureResult(path);
            }
        } catch (final Exception e) {
            callOnError(e);
        }
    }*/

    public AACEncodeConsumer mAacConsumer;
    private H264EncodeConsumer mH264Consumer;

    public void handleStartRecording(RecordParams params) {
        /*if ((mUVCCamera == null) || (mMuxer != null))
            return;*/
        if (mMuxer != null)
            return;
        if (params == null)
            throw new NullPointerException("RecordParams can not be null!");
//			// 获取USB Camera预览数据
//			mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
        // 初始化混合器
        videoPath = params.getRecordPath();
        mMuxer = new Mp4MediaMuxer(params.getRecordPath(),
                params.getRecordDuration() * 60 * 1000, params.isVoiceClose());

        // 启动视频编码线程
        startVideoRecord();
        // 启动音频编码线程
        if (!params.isVoiceClose()) {
            startAudioRecord();
        }
        //callOnStartRecording();
    }


    public void handleStopRecording() {
        // 停止混合器
        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
            Log.i(TAG, TAG + "---->停止本地录制");
        }
        // 停止音视频编码线程
        stopAudioRecord();
        stopVideoRecord();
        // 停止捕获视频数据
        /*if (mUVCCamera != null) {
            mUVCCamera.stopCapture();
            mUVCCamera.setFrameCallback(null, 0);
        }*/
        //mWeakCameraView.get().setVideoEncoder(null);
        // you should not wait here
        //callOnStopRecording();
        // 返回路径
        if (mListener != null) {
            mListener.onRecordResult(videoPath + ".mp4");
        }
    }

    private void startVideoRecord() {
        mH264Consumer = new H264EncodeConsumer(getWidth(), getHeight());
        mH264Consumer.setOnH264EncodeResultListener(new H264EncodeConsumer.OnH264EncodeResultListener() {
            @Override
            public void onEncodeResult(byte[] data, int offset, int length, long timestamp) {
                if (mListener != null) {
                    mListener.onEncodeResult(data, offset, length, timestamp, 1);
                }
            }
        });
        mH264Consumer.start();
        // 添加混合器
        if (mH264Consumer != null) {
            mH264Consumer.setTmpuMuxer(mMuxer);
        }
    }

    private void stopVideoRecord() {
        if (mH264Consumer != null) {
            mH264Consumer.exit();
            mH264Consumer.setTmpuMuxer(null);
            try {
                Thread t2 = mH264Consumer;
                mH264Consumer = null;
                if (t2 != null) {
                    t2.interrupt();
                    t2.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startAudioRecord() {
        mAacConsumer = new AACEncodeConsumer();
        mAacConsumer.setOnAACEncodeResultListener(new AACEncodeConsumer.OnAACEncodeResultListener() {
            @Override
            public void onEncodeResult(byte[] data, int offset, int length, long timestamp) {
                if (mListener != null) {
                    mListener.onEncodeResult(data, offset, length, timestamp, 0);
                }
            }
        });
        mAacConsumer.start();
        // 添加混合器
        if (mAacConsumer != null) {
            mAacConsumer.setTmpuMuxer(mMuxer);
        }
//			isAudioThreadStart = true;
    }

    private void stopAudioRecord() {
        if (mAacConsumer != null) {
            mAacConsumer.exit();
            mAacConsumer.setTmpuMuxer(null);
            try {
                Thread t1 = mAacConsumer;
                mAacConsumer = null;
                if (t1 != null) {
                    t1.interrupt();
                    t1.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//			isAudioThreadStart = false;
    }

    private boolean isCaptureStill;

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
//				final MediaVideoBufferEncoder videoEncoder;
//				synchronized (mSync) {
//					videoEncoder = mVideoEncoder;
//				}
//				if (videoEncoder != null) {
//					videoEncoder.frameAvailableSoon();
//					videoEncoder.encode(frame);
//				}
            int len = frame.capacity();
            byte[] yuv = new byte[len];
            frame.get(yuv);
            // 捕获图片
            if (isCaptureStill) {

            }
            // 视频
            if (mH264Consumer != null) {
                // 修改分辨率参数
                mH264Consumer.setRawYuv(yuv, mWidth, mHeight);
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void handleUpdateMedia(final String path) {
        if (DEBUG) Log.v(TAG, "handleUpdateMedia:path=" + path);
        final Context parent = mWeakParent.get();
        final boolean released = (mHandler == null) || mHandler.mReleased;
        if (parent != null && parent.getApplicationContext() != null) {
            try {
                if (DEBUG) Log.i(TAG, "MediaScannerConnection#scanFile");
                MediaScannerConnection.scanFile(parent.getApplicationContext(), new String[]{path}, null, null);
            } catch (final Exception e) {
                Log.e(TAG, "handleUpdateMedia:", e);
            }
            if (released || (parent instanceof Activity) && ((Activity)parent).isDestroyed())
                handleRelease();
        } else {
            Log.w(TAG, "MainActivity already destroyed");
            // give up to add this movie to MediaStore now.
            // Seeing this movie on Gallery app etc. will take a lot of time.
            handleRelease();
        }
    }

    public void handleRelease() {
        if (DEBUG) Log.v(TAG, "handleRelease:mIsRecording=" + mIsRecording);
        handleClose();
        //mCallbacks.clear();
        if (!mIsRecording) {
            mHandler.mReleased = true;
            Looper.myLooper().quit();
        }
        if (DEBUG) Log.v(TAG, "handleRelease:finished");
    }

    // 自动对焦
    public void handleCameraFoucs() {
        if (DEBUG) Log.v(TAG, "handleStartPreview:");
//        if ((mUVCCamera == null) || !mIsPreviewing)
        if (!mIsPreviewing)
            return;
        //mUVCCamera.setAutoFocus(true);
    }

    // 获取支持的分辨率
    public List<Size> getSupportedSizes() {
        if ((mUVCCamera == null) || !mIsPreviewing)
            return null;
        return mUVCCamera.getSupportedSizeList();
    }

    /*private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
            mIsRecording = true;
            if (encoder instanceof MediaVideoEncoder)
                try {
                    //mWeakCameraView.get().setVideoEncoder((MediaVideoEncoder)encoder);
                } catch (final Exception e) {
                    Log.e(TAG, "onPrepared:", e);
                }
            if (encoder instanceof MediaSurfaceEncoder)
                try {
                    //mWeakCameraView.get().setVideoEncoder((MediaSurfaceEncoder)encoder);
                    mUVCCamera.startCapture(((MediaSurfaceEncoder)encoder).getInputSurface());
                } catch (final Exception e) {
                    Log.e(TAG, "onPrepared:", e);
                }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
            if ((encoder instanceof MediaVideoEncoder)
                    || (encoder instanceof MediaSurfaceEncoder))
                try {
                    mIsRecording = false;
                    final Activity parent = mWeakParent.get();
                    //mWeakCameraView.get().setVideoEncoder(null);
                    synchronized (mSync) {
                        if (mUVCCamera != null) {
                            mUVCCamera.stopCapture();
                        }
                    }
                    final String path = encoder.getOutputPath();
                    if (!TextUtils.isEmpty(path)) {
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_MEDIA_UPDATE, path), 1000);
                    } else {
                        final boolean released = (mHandler == null) || mHandler.mReleased;
                        if (released || parent == null || parent.isDestroyed()) {
                            handleRelease();
                        }
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "onPrepared:", e);
                }
        }

        @Override
        public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
            if(mListener != null){
                mListener.onEncodeResult(data, offset, length, timestamp, type);
            }
        }

    };*/

    @Override
    public void run() {
        Looper.prepare();
        CameraHandler handler = null;
        try {
            final Constructor<? extends CameraHandler> constructor = mHandlerClass.getDeclaredConstructor(CameraThread.class);
            handler = constructor.newInstance(this);
        } catch (final NoSuchMethodException e) {
            Log.w(TAG, e);
        } catch (final IllegalAccessException e) {
            Log.w(TAG, e);
        } catch (final InstantiationException e) {
            Log.w(TAG, e);
        } catch (final InvocationTargetException e) {
            Log.w(TAG, e);
        }
        if (handler != null) {
            synchronized (mSync) {
                mHandler = handler;
                mSync.notifyAll();
            }
            Looper.loop();
//				if (mSoundPool != null) {
//					mSoundPool.release();
//					mSoundPool = null;
//				}
            if (mHandler != null) {
                mHandler.mReleased = true;
            }
        }
        //mCallbacks.clear();
        synchronized (mSync) {
            mHandler = null;
            mSync.notifyAll();
        }
    }

    /*private void callOnOpen() {
        for (final AbstractUVCCameraHandler.CameraCallback callback: mCallbacks) {
            try {
                callback.onOpen();
            } catch (final Exception e) {
                mCallbacks.remove(callback);
                Log.w(TAG, e);
            }
        }
    }

    private void callOnClose() {
        for (final CameraCallback callback: mCallbacks) {
            try {
                callback.onClose();
            } catch (final Exception e) {
                mCallbacks.remove(callback);
                Log.w(TAG, e);
            }
        }
    }

    private void callOnStartPreview() {
        for (final CameraCallback callback: mCallbacks) {
            try {
                callback.onStartPreview();
            } catch (final Exception e) {
                mCallbacks.remove(callback);
                Log.w(TAG, e);
            }
        }
    }

    private void callOnStopPreview() {
        for (final CameraCallback callback: mCallbacks) {
            try {
                callback.onStopPreview();
            } catch (final Exception e) {
                mCallbacks.remove(callback);
                Log.w(TAG, e);
            }
        }
    }

    private void callOnStartRecording() {
        for (final CameraCallback callback: mCallbacks) {
            try {
                callback.onStartRecording();
            } catch (final Exception e) {
                mCallbacks.remove(callback);
                Log.w(TAG, e);
            }
        }
    }

    private void callOnStopRecording() {
        for (final CameraCallback callback: mCallbacks) {
            try {
                callback.onStopRecording();
            } catch (final Exception e) {
                mCallbacks.remove(callback);
                Log.w(TAG, e);
            }
        }
    }

    private void callOnError(final Exception e) {
        for (final CameraCallback callback: mCallbacks) {
            try {
                callback.onError(e);
            } catch (final Exception e1) {
                mCallbacks.remove(callback);
                Log.w(TAG, e);
            }
        }
    }*/
}
