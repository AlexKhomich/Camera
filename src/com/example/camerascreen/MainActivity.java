package com.example.camerascreen;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


public class MainActivity extends Activity {

    SurfaceView sv;
    SurfaceHolder holder;
    HolderCallback holderCallback;
    Camera camera;
    MediaRecorder mediaRecorder;
    Chronometer chron;
    File photoFile;
    File videoFile;
    static final String DIR_SD = "/Pictures/.TEMP";
    static final String FILENAME_SD = "cam.conf";
    static final String FILENAME_SD_BUT = "but.conf";
    final boolean FULL_SCREEN = true;
    long time = 0;

    //set front or rear camera (settings reads from file)
    public int setCAMERA_ID() {
        int temp = 0;
        if (Environment.getExternalStorageDirectory().exists()) {
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD + "/" + FILENAME_SD);
            if (!sdPath.exists()) {
                sdPath.mkdirs();
            }
            try (Scanner in = new Scanner(sdPath)) {
                temp = in.nextInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return temp;
    }

    //photo or video mode that reads from file
    public int setFotoOrVid() {
        int temp = 0;
        File sdPath = Environment.getExternalStorageDirectory();
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD + "/" + FILENAME_SD_BUT);
        if (!sdPath.exists()) {
            sdPath.mkdirs();
        }
        try (Scanner in = new Scanner(sdPath)) {
            temp = in.nextInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return temp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        sv = (SurfaceView) findViewById(R.id.surfaceView);
        holder = sv.getHolder();
        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);
        sv.setOnTouchListener(new View.OnTouchListener() { //set touch listener for autofocus method
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                 float x = event.getX();
//                 float y = event.getY();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    meteringAreas();
                    if (setCAMERA_ID() == 0) {
                        camera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (!success)
                                    Toast.makeText(getApplicationContext(), "failed to focus!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                return false;
            }
        });
    }

    public class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                //selection view of buttons (video or photo)
                if (setFotoOrVid() == 0) {
                    ImageButton shotButton = (ImageButton) findViewById(R.id.pictureButton);
                    shotButton.setVisibility(View.VISIBLE);
                    ImageButton startRecBtn = (ImageButton) findViewById(R.id.startRecord);
                    startRecBtn.setVisibility(View.GONE);
                    ImageButton stopRecBtn = (ImageButton) findViewById(R.id.stopRecord);
                    stopRecBtn.setVisibility(View.GONE);
                    ImageButton vidChngBtn = (ImageButton) findViewById(R.id.vidChngBtn);
                    vidChngBtn.setVisibility(View.GONE);
                    ImageButton photoBtn = (ImageButton) findViewById(R.id.photoBtn);
                    photoBtn.setVisibility(View.GONE);
                } else if (setFotoOrVid() == 1) {
                    ImageButton shotButton = (ImageButton) findViewById(R.id.pictureButton);
                    shotButton.setVisibility(View.GONE);
                    ImageButton startRecBtn = (ImageButton) findViewById(R.id.startRecord);
                    startRecBtn.setVisibility(View.VISIBLE);
                    ImageButton stopRecBtn = (ImageButton) findViewById(R.id.stopRecord);
                    stopRecBtn.setImageResource(R.drawable.btn_shutter_default_disabled);
                    stopRecBtn.setVisibility(View.VISIBLE);
                    ImageButton vidBtn = (ImageButton) findViewById(R.id.vidChngBtn);
                    vidBtn.setVisibility(View.GONE);
                    ImageButton photoBtn = (ImageButton) findViewById(R.id.photoBtn);
                    photoBtn.setVisibility(View.GONE);
                    ImageButton camChngFront = (ImageButton) findViewById(R.id.cameraChngF);
                    camChngFront.setVisibility(View.GONE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            try {
                camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
            setCAMERA_ID();
            setCameraDisplayOrientation(setCAMERA_ID());
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open(setCAMERA_ID());
        setPreviewSize(FULL_SCREEN);
        if (setCAMERA_ID() == 0) {
            initSpinners();
            ImageButton camChngBtnFront = (ImageButton) findViewById(R.id.cameraChngF);
            ImageButton camChngBtnRear = (ImageButton) findViewById(R.id.cameraChngR);
            if (setCAMERA_ID() == 0) {
                camChngBtnRear.setVisibility(View.GONE);
                camChngBtnFront.setVisibility(View.VISIBLE);
            } else if (setCAMERA_ID() == 1) {
                camChngBtnRear.setVisibility(View.VISIBLE);
                camChngBtnFront.setVisibility(View.GONE);
            }
        } else {
            //settings of white balance (for front camera only)
            final List<String> whiteBalance = camera.getParameters().getSupportedWhiteBalance();
            Spinner spWhiteBalance = initSpinner(R.id.spWhiteBalance, whiteBalance, camera.getParameters().getWhiteBalance());
            spWhiteBalance.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    Parameters params = camera.getParameters();
                    params.setWhiteBalance(whiteBalance.get(arg2));
                    camera.setParameters(params);
                    Spinner spinner = (Spinner) findViewById(R.id.spWhiteBalance);
                    ((TextView) spinner.getChildAt(0)).setTextColor(Color.WHITE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null)
            camera.release();
        camera = null;
    }

    private void setPreviewSize(boolean fullScreen) {
        // get dimensions of the display
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);
        int realWidth = realMetrics.widthPixels;
        int realHeight = realMetrics.heightPixels;
        boolean widthIsMax = realWidth > realHeight;
        // define dimensions of the preview
        Size size = camera.getParameters().getPreviewSize();
        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();
        // RectF of the display, correspond to dimensions of the display
        rectDisplay.set(0, 0, realWidth, realHeight);
        // RectF preview
        if (widthIsMax) {
            // preview in a horizontal orientation
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // preview in a vertical orientation
            rectPreview.set(0, 0, size.height, size.width);
        }
        Matrix matrix = new Matrix();
        // prepare the matrix of conversion
        if (!fullScreen) {
            // if the preview will put into the screen
            matrix.setRectToRect(rectPreview, rectDisplay, Matrix.ScaleToFit.START);
        } else {
            // if the screen will put into the preview
            matrix.setRectToRect(rectDisplay, rectPreview, Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        // conversion
        matrix.mapRect(rectPreview);
        // correspond dimensions of surface received from conversion
        sv.getLayoutParams().height = (int) (rectPreview.bottom);
        sv.getLayoutParams().width = (int) (rectPreview.right);
    }

    public void setCameraDisplayOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        // determines how the screen is rotated from its normal position
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        //front-facing
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        // turn the finished photos to 90 °
        if (setCAMERA_ID() == 0) {
            Camera.Parameters parameters = camera.getParameters();
            switch (rotation) {
                case Surface.ROTATION_0:
                    rotation = degrees + 90;
                    break;
                case Surface.ROTATION_90:
                    rotation = degrees - 90;
                    break;
                case Surface.ROTATION_180:
                    rotation = degrees + 90;
                    break;
                case Surface.ROTATION_270:
                    rotation = degrees - 90;
                    break;
            }
            parameters.setRotation(rotation);
            camera.setParameters(parameters);
        } else {
            Camera.Parameters parameters = camera.getParameters();
            switch (rotation) {
                case Surface.ROTATION_0:
                    rotation = degrees + 270;
                    break;
                case Surface.ROTATION_90:
                    rotation = degrees - 90;
                    break;
                case Surface.ROTATION_180:
                    rotation = degrees - 90;
                    break;
                case Surface.ROTATION_270:
                    rotation = degrees - 90;
                    break;
            }
            parameters.setRotation(rotation);
            camera.setParameters(parameters);
        }
    }

    // spinners settings and effects
    void initSpinners() {
        // режимы вспышки
        final List<String> flashModes = camera.getParameters().getSupportedFlashModes();
        // settings of spinners
        Spinner spFlash = initSpinner(R.id.spFlash, flashModes, camera.getParameters().getFlashMode());
        // selection handler
        spFlash.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Parameters params = camera.getParameters();
                params.setFlashMode(flashModes.get(arg2));
                camera.setParameters(params);
                Spinner spinner = (Spinner) findViewById(R.id.spFlash);
                ((TextView) spinner.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        // color effects
        final List<String> colorEffects = camera.getParameters().getSupportedColorEffects();
        Spinner spEffect = initSpinner(R.id.spColor, colorEffects, camera.getParameters().getColorEffect());
        spEffect.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Parameters params = camera.getParameters();
                params.setColorEffect(colorEffects.get(arg2));
                camera.setParameters(params);
                Spinner spinner = (Spinner) findViewById(R.id.spColor);
                ((TextView) spinner.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        // focus modes
        final List<String> focusMode = camera.getParameters().getSupportedFocusModes();
        Spinner spFocusMode = initSpinner(R.id.spFocusMode, focusMode, camera.getParameters().getFocusMode());
        spFocusMode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Parameters params = camera.getParameters();
                params.setFocusMode(focusMode.get(arg2));
                camera.setParameters(params);
                Spinner spinner = (Spinner) findViewById(R.id.spFocusMode);
                ((TextView) spinner.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //antibanding
        final List<String> antiBand = camera.getParameters().getSupportedAntibanding();
        Spinner spAB = initSpinner(R.id.spAB, antiBand, camera.getParameters().getAntibanding());
        spAB.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Parameters params = camera.getParameters();
                params.setAntibanding(antiBand.get(arg2));
                camera.setParameters(params);
                Spinner spinner = (Spinner) findViewById(R.id.spAB);
                ((TextView) spinner.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //scenes
        final List<String> sceneMode = camera.getParameters().getSupportedSceneModes();
        Spinner spSceneMode = initSpinner(R.id.spSceneMode, sceneMode, camera.getParameters().getSceneMode());
        spSceneMode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Parameters params = camera.getParameters();
                params.setSceneMode(sceneMode.get(arg2));
                camera.setParameters(params);
                Spinner spinner = (Spinner) findViewById(R.id.spSceneMode);
                ((TextView) spinner.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //white balance
        final List<String> whiteBalance = camera.getParameters().getSupportedWhiteBalance();
        Spinner spWhiteBalance = initSpinner(R.id.spWhiteBalance, whiteBalance, camera.getParameters().getWhiteBalance());
        spWhiteBalance.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Parameters params = camera.getParameters();
                params.setWhiteBalance(whiteBalance.get(arg2));
                camera.setParameters(params);
                Spinner spinner = (Spinner) findViewById(R.id.spWhiteBalance);
                ((TextView) spinner.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    //setup spinners
    Spinner initSpinner(int spinnerId, List<String> data, String currentValue) {
        // Setting the spinner and adapter for him
        Spinner spinner = (Spinner) findViewById(spinnerId);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        // define a value in the list is the current setting
        for (int i = 0; i < data.size(); i++) {
            String item = data.get(i);
            if (item.equals(currentValue)) {
                spinner.setSelection(i);
            }
        }
        return spinner;
    }

    public void meteringAreas() {
        Camera.Parameters param = camera.getParameters(); // set Camera parameters for metering areas
        if (param.getMaxNumMeteringAreas() > 0) { // check that metering areas are supported
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
            Rect areaRect1 = new Rect(-500, -500, 500, 500);    // specify an area in center of image
            meteringAreas.add(new Camera.Area(areaRect1, 700)); // set weight to 60%
            param.setMeteringAreas(meteringAreas);
        }
        camera.setParameters(param);

    }

    public void focusAreas() {
        Camera.Parameters params = camera.getParameters(); // set Camera parameters for focus areas
        if (params.getMaxNumFocusAreas() > 0) { // check that metering areas are supported
            List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
            Rect areaRect1 = new Rect(-1000, -1000, 1000, 1000);    // specify an area in center of image
            focusAreas.add(new Camera.Area(areaRect1, 800)); // set weight to 60%
            params.setFocusAreas(focusAreas);
        }
        camera.setParameters(params);
    }

    //Sets the maximum resolution photo
    private void setPictureSize() {
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        if (sizes == null) {
            return;
        }
        int maxSize = 0;
        int width = 0;
        int height = 0;
        for (Size size : sizes) {
            int pix = size.width * size.height;
            if (pix > maxSize) {
                maxSize = pix;
                width = size.width;
                height = size.height;
            }
        }
        params.setPictureSize(width, height);
        camera.setParameters(params);
    }

    //handler pressing "take a picture"
    public void onClickPicture(View view) {
        setPictureSize();
        meteringAreas();
        ImageButton shotBtn = (ImageButton) findViewById(R.id.pictureButton);
        if (shotBtn.isPressed()) {
            shotBtn.setImageResource(R.drawable.btn_shutter_default_disabled);
        } else {
            shotBtn.setImageResource(R.drawable.btn_shutter_default);
        }
        //creating callback for autofocus
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    camera.takePicture(null, null, new PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            //generate time for use as part of the file name
                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
                            File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            if (!pictures.exists()) {
                                pictures.mkdirs();
                                if (!pictures.mkdirs()) {
                                    Log.d("picture", "failed to create directory");
                                }
                            }
                            photoFile = new File(pictures, "IMG_" + timeStamp + ".jpg");
                            try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                                fos.write(data);
                                Toast.makeText(getApplicationContext(), "file " + photoFile + " saved", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(getApplicationContext(), "Cannot save a file. May be SD card is not available! " + photoFile, Toast.LENGTH_SHORT).show();
                            }
                            camera.startPreview();
                            //reverse image replacement button after start preview
                            ImageButton shotBtn = (ImageButton) findViewById(R.id.pictureButton);
                            shotBtn.setImageResource(R.drawable.btn_shutter_default);
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "failed to focus", Toast.LENGTH_SHORT).show();
                    ImageButton shotBtn = (ImageButton) findViewById(R.id.pictureButton);
                    shotBtn.setImageResource(R.drawable.btn_shutter_default);
                }
            }
        });
    }

    //menu with a choice of shooting settings
    public void onClickSetButton(View view) {
        //buttons
        ImageButton setButton = (ImageButton) findViewById(R.id.setButton);
        setButton.setVisibility(View.GONE);
        ImageButton exit = (ImageButton) findViewById(R.id.exitButton);
        exit.setVisibility(View.VISIBLE);
        ImageButton vidChngBtn = (ImageButton) findViewById(R.id.vidChngBtn);
        ImageButton startRecBtn = (ImageButton) findViewById(R.id.startRecord);
        ImageButton photoBtn = (ImageButton) findViewById(R.id.photoBtn);
        //button selection switch modes depending on the shooting mode: video or photo
        if (startRecBtn.isShown()) {
            vidChngBtn.setVisibility(View.GONE);
            photoBtn.setVisibility(View.VISIBLE);
        } else {
            photoBtn.setVisibility(View.GONE);
            vidChngBtn.setVisibility(View.VISIBLE);
        }
        //text
        View flashMode = findViewById(R.id.textView);
        flashMode.setVisibility(View.VISIBLE);
        View colorEffect = findViewById(R.id.textViewCeF);
        colorEffect.setVisibility(View.VISIBLE);
        View focusMode = findViewById(R.id.textViewFm);
        focusMode.setVisibility(View.VISIBLE);
        View antiBand = findViewById(R.id.textViewAB);
        antiBand.setVisibility(View.VISIBLE);
        View sceneMode = findViewById(R.id.textViewSM);
        sceneMode.setVisibility(View.VISIBLE);
        View whiteBalance = findViewById(R.id.textViewWB);
        whiteBalance.setVisibility(View.VISIBLE);
        //spinners
        Spinner flashSP = (Spinner) findViewById(R.id.spFlash);
        flashSP.setVisibility(View.VISIBLE);
        Spinner colorSP = (Spinner) findViewById(R.id.spColor);
        colorSP.setVisibility(View.VISIBLE);
        Spinner focusModeSP = (Spinner) findViewById(R.id.spFocusMode);
        focusModeSP.setVisibility(View.VISIBLE);
        Spinner antiBandSP = (Spinner) findViewById(R.id.spAB);
        antiBandSP.setVisibility(View.VISIBLE);
        Spinner sceneModeSP = (Spinner) findViewById(R.id.spSceneMode);
        sceneModeSP.setVisibility(View.VISIBLE);
        Spinner whiteBalanceSP = (Spinner) findViewById(R.id.spWhiteBalance);
        whiteBalanceSP.setVisibility(View.VISIBLE);
        //side of camera
        ImageButton camChngF = (ImageButton) findViewById(R.id.cameraChngF);
        camChngF.setVisibility(View.GONE);
        ImageButton camChngB = (ImageButton) findViewById(R.id.cameraChngR);
        camChngB.setVisibility(View.GONE);
        //ImageButton vidBtn = (ImageButton) findViewById(R.id.vidChngBtn);
        if (setCAMERA_ID() == 1) {
            vidChngBtn.setVisibility(View.GONE);
            photoBtn.setVisibility(View.GONE);
        }
    }

    //exits the menu settings
    public void onClickExit(View view) {
        //buttons
        ImageButton setButton = (ImageButton) findViewById(R.id.setButton);
        setButton.setVisibility(View.VISIBLE);
        ImageButton exit = (ImageButton) findViewById(R.id.exitButton);
        exit.setVisibility(View.GONE);
        ImageButton vidChngBtn = (ImageButton) findViewById(R.id.vidChngBtn);
        vidChngBtn.setVisibility(View.GONE);
        ImageButton photoBtn = (ImageButton) findViewById(R.id.photoBtn);
        photoBtn.setVisibility(View.GONE);
        //text
        View flashMode = findViewById(R.id.textView);
        flashMode.setVisibility(View.GONE);
        View colorEffect = findViewById(R.id.textViewCeF);
        colorEffect.setVisibility(View.GONE);
        View focusMode = findViewById(R.id.textViewFm);
        focusMode.setVisibility(View.GONE);
        View antiBand = findViewById(R.id.textViewAB);
        antiBand.setVisibility(View.GONE);
        View sceneMode = findViewById(R.id.textViewSM);
        sceneMode.setVisibility(View.GONE);
        View whiteBalance = findViewById(R.id.textViewWB);
        whiteBalance.setVisibility(View.GONE);
        //spinners
        Spinner flashSP = (Spinner) findViewById(R.id.spFlash);
        flashSP.setVisibility(View.GONE);
        Spinner colorSP = (Spinner) findViewById(R.id.spColor);
        colorSP.setVisibility(View.GONE);
        Spinner focusModeSP = (Spinner) findViewById(R.id.spFocusMode);
        focusModeSP.setVisibility(View.GONE);
        Spinner antiBandSP = (Spinner) findViewById(R.id.spAB);
        antiBandSP.setVisibility(View.GONE);
        Spinner sceneModeSP = (Spinner) findViewById(R.id.spSceneMode);
        sceneModeSP.setVisibility(View.GONE);
        Spinner whiteBalanceSP = (Spinner) findViewById(R.id.spWhiteBalance);
        whiteBalanceSP.setVisibility(View.GONE);
        //side of camera
        ImageButton camChngF = (ImageButton) findViewById(R.id.cameraChngF);
        camChngF.setVisibility(View.VISIBLE);
        ImageButton camChngB = (ImageButton) findViewById(R.id.cameraChngR);
        camChngB.setVisibility(View.GONE);
    }

    // Selecting a shooting mode (video)
    public void onClickVidChngBtn(View view) {
        camera.stopPreview();
        File sdPath = Environment.getExternalStorageDirectory();
        // add a directory to the path
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        // создаем каталог
//        sdPath.mkdirs();
        // forming an object File, which contains the path to the file
        File sdFile = new File(sdPath, FILENAME_SD_BUT);
        try (PrintWriter out = new PrintWriter(sdFile.getAbsoluteFile())) {
            out.print(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        recreate();
    }

    //Selecting a shooting mode (photo)
    public void onClickPhoto(View view) {
        camera.stopPreview();
        File sdPath = Environment.getExternalStorageDirectory();
        // add a directory to the path
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        // создаем каталог
//        sdPath.mkdirs();
        // forming an object File, which contains the path to the file
        File sdFile = new File(sdPath, FILENAME_SD_BUT);
        try (PrintWriter out = new PrintWriter(sdFile.getAbsoluteFile())) {
            out.print(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        recreate();
    }

    //camera change (front)
    public void onClickChngCamF(View view) {
        File sdPath = Environment.getExternalStorageDirectory();
        // add a directory to the path
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        // создаем каталог
//        sdPath.mkdirs();
        // forming an object File, which contains the path to the file
        File sdFile = new File(sdPath, FILENAME_SD);
        try (PrintWriter out = new PrintWriter(sdFile.getAbsoluteFile())) {
            out.print(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageButton camChngF = (ImageButton) findViewById(R.id.cameraChngF);
        camChngF.setVisibility(View.GONE);
        ImageButton camChngB = (ImageButton) findViewById(R.id.cameraChngR);
        camChngB.setVisibility(View.VISIBLE);
        recreate();
    }

    //camera change (rear)
    public void onClickChngCamR(View view) {
        File sdPath = Environment.getExternalStorageDirectory();
        // add a directory to the path
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        // создаем каталог
//        sdPath.mkdirs();
        // forming an object File, which contains the path to the file
        File sdFile = new File(sdPath, FILENAME_SD);
        try (PrintWriter out = new PrintWriter(sdFile.getAbsoluteFile())) {
            out.print(0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ImageButton camChngF = (ImageButton) findViewById(R.id.cameraChngF);
        camChngF.setVisibility(View.VISIBLE);
        ImageButton camChngB = (ImageButton) findViewById(R.id.cameraChngR);
        camChngB.setVisibility(View.GONE);
        recreate();
    }

    //mediarecorder methods
    //starting mediarecorder
    public void onClickStartRecord(View view) {
        ImageButton startRecBtn = (ImageButton) findViewById(R.id.startRecord);
        ImageButton stopRecBtn = (ImageButton) findViewById(R.id.stopRecord);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!pictures.exists()) {
            pictures.mkdirs();
            if (!pictures.mkdirs()) {
                Log.d("picture", "failed to create directory");
            }
        }
        videoFile = new File(pictures, "VID_" + timeStamp + ".mp4");
        if (prepareVideoRecorder()) {
            mediaRecorder.start();
            setPreviewSize(FULL_SCREEN);
            focusAreas();
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            camera.setParameters(params);
            //start the timer for creating a video
            chron = (Chronometer) findViewById(R.id.chrono);
            ImageView recIm = (ImageView) findViewById(R.id.recIm);
            chron = (Chronometer) findViewById(R.id.chrono);
            recIm.setVisibility(View.VISIBLE);
            chron.setVisibility(View.VISIBLE);
            chron.setBase(SystemClock.elapsedRealtime() + time);
            chron.start();
            ImageButton ChngCamF = (ImageButton) findViewById(R.id.cameraChngF);
            ChngCamF.setVisibility(View.GONE);
        } else releaseMediaRecorder();
        if (startRecBtn.isPressed()) {
            startRecBtn.setImageResource(R.drawable.btn_shutter_default_disabled);
            stopRecBtn.setImageResource(R.drawable.btn_shutter_video_recording);
        }
    }

    public void onClickStopRecord(View view) {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            chron = (Chronometer) findViewById(R.id.chrono);
            ImageView recIm = (ImageView) findViewById(R.id.recIm);
            chron = (Chronometer) findViewById(R.id.chrono);
            chron.stop();
            recIm.setVisibility(View.GONE);
            chron.setVisibility(View.GONE);
            ImageButton ChngCamF = (ImageButton) findViewById(R.id.cameraChngF);
            ChngCamF.setVisibility(View.VISIBLE);
            releaseMediaRecorder();
            Toast.makeText(getApplicationContext(), "file " + videoFile + " saved", Toast.LENGTH_SHORT).show();
        }
        ImageButton startRecBtn = (ImageButton) findViewById(R.id.startRecord);
        ImageButton stopRecBtn = (ImageButton) findViewById(R.id.stopRecord);
        if (stopRecBtn.isPressed()) {
            stopRecBtn.setImageResource(R.drawable.btn_shutter_default_disabled);
        }
        startRecBtn.setImageResource(R.drawable.btn_shutter_video_default);
        camera.startPreview();
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            camera.lock();           // lock camera for later use

        }
    }

    //prepare the mediarecorder
    private boolean prepareVideoRecorder() {

        camera.stopPreview();

        mediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();

        mediaRecorder.setCamera(camera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        // Step 4: Set output file
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(sv.getHolder().getSurface());


        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();

        } catch (Exception e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }
}
