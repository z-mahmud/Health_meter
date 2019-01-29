package com.arlong.stepcounter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;

import java.sql.SQLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import filebrowser.FileBrowser;


public class SensorReadoutActivity extends Activity implements  OnClickListener, RefreshableView ,LocationChangeListener{

    // Sensor 配置
    private static final String TAG = "SensorScan";
    private SensorManager sensorManager;

    private Sensor accSensor;
    private Sensor gyroSensor;

    // 计步参数
    private int accNo = 0;
    public int stepNo = 0;
    public float stepLength;
    public float heading;
    public static boolean getInitAngle;
    private float[] accold1 = new float[2];
    private float[] accold2 = new float[2];
    private float[] accnew = new float[2];
    private float[] peakmax = new float[2];
    private float[] peakmin = new float[2];
    private float deltaTime = 0.15f;
    private float deltaA = 1.4f;
    private boolean peakmaxReady;
    private float locationx, locationy;
    //采样频率
    public static final int sampleRate = 20;

    private TextView tvStep;
    private TextView tvStepLength;
    private TextView tvHeading;
    private TextView tvLocation;

    //平均滤波参数
    private int N_windows = 5;
    private float[] value_buf = new float[N_windows];
    private int i_filter=0;

    // 读取传感器数据时用到的变量
    long timeStart=0;// the timestamp of the first sample，因为event.timestamp格式为long，这里是为了保证相减前不丢数据，否则后面间隔可能为负
    float timestamp;// 距离first sample 的时间间隔

    public float beta = 0.3f;								// 2 * proportional gain (Kp)
    public float q0 = 1.0f, q1 = 0.0f, q2 = 0.0f, q3 = 0.0f;	// quaternion of sensor frame relative to auxiliary frame

    /**
     * @uml.property name="user"
     * @uml.associationEnd
     */
    //private Thread ticker;
    private int xTick = 0;
    private Madgwick mMadgwick = new Madgwick();

    /**
     * @uml.property name="log"
     * @uml.associationEnd
     */
    protected Logger log = new Logger(SensorReadoutActivity.class);

    public static final String SITE_KEY = "SITE";

    // public static final int START_NEW = 1, START_LOAD = 2;

    protected static final int DIALOG_TITLE = 1, DIALOG_SCANNING = 2, DIALOG_CHANGE_SIZE = 3, DIALOG_SET_BACKGROUND = 4, DIALOG_SET_SCALE_OF_MAP = 5,
            DIALOG_ADD_KNOWN_AP = 6, DIALOG_SELECT_BSSIDS = 7, DIALOG_FRESH_SITE = 8, DIALOG_ASK_CHANGE_SCALE = 9, DIALOG_ASK_FOR_NORTH = 10,
            DIALOG_CHANGE_SCAN_INTERVAL = 11;

    protected static final int MESSAGE_REFRESH = 1, MESSAGE_START_WIFISCAN = 2, MESSAGE_PERSIST_RESULT = 3;

    protected static final int FILEBROWSER_REQUEST = 1;

    /**
     * how often should we start a wifi scan
     */
    protected int schedulerTime = 10;

    /**
     * @uml.property name="multiTouchView"
     * @uml.associationEnd
     */
    protected MultiTouchView multiTouchView;

    /**
     * @uml.property name="map"
     * @uml.associationEnd
     */
    protected SiteMapDrawable map;

    /**
     * @uml.property name="site"
     * @uml.associationEnd
     */
    protected ProjectSite site;

    /**
     * @uml.property name="databaseHelper"
     * @uml.associationEnd
     */
    protected DatabaseHelper databaseHelper = null;

    protected Dao<ProjectSite, Integer> projectSiteDao = null;

    protected AlertDialog scanAlertDialog;

    protected ImageView scanningImageView;

    protected boolean ignoreWifiResults = false;

    protected BroadcastReceiver wifiBroadcastReceiver;

    /**
     * @uml.property name="user"
     * @uml.associationEnd
     */
    protected UserDrawable user;

    /**
     * @uml.property name="scaler"
     * @uml.associationEnd
     */
    protected ScaleLineDrawable scaler = null;

    protected final Context context = this;

    protected TextView backgroundPathTextView;

    protected float scalerDistance;

    /**
     * @uml.property name="stepDetectionProvider"
     * @uml.associationEnd
     */
    protected StepDetectionProvider stepDetectionProvider = null;

    /**
     * @uml.property name="northDrawable"
     * @uml.associationEnd
     */
    protected NorthDrawable northDrawable = null;

    protected Handler messageHandler;

    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    protected Runnable wifiRunnable;

    protected ScheduledFuture<?> scheduledTask = null;

    protected boolean walkingAndScanning = false;

    protected boolean freshSite = false;

    protected boolean trackSteps= true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            this.setContentView(R.layout.activity_main);
            super.onCreate(savedInstanceState);
            Intent intent = this.getIntent();

            int siteId = intent.getExtras().getInt(SITE_KEY, -1);
            if (siteId == -1) {
                throw new SiteNotFoundException("ProjectSiteActivity called without a correct site ID!");
            }

            databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
            projectSiteDao = databaseHelper.getDao(ProjectSite.class);
            site = projectSiteDao.queryForId(siteId);

            if (site == null) {
                throw new SiteNotFoundException("The ProjectSite Id could not be found in the database!");
            }

            MultiTouchDrawable.setGridSpacing(site.getGridSpacingX(), site.getGridSpacingY());

            map = new SiteMapDrawable(this, this);
            map.setAngleAdjustment(site.getNorth());

            if (site.getWidth() == 0 || site.getHeight() == 0) {
                // the site has never been loaded
                freshSite = true;
                site.setSize(map.getWidth(), map.getHeight());
            } else {
                map.setSize(site.getWidth(), site.getHeight());
            }
            if (site.getBackgroundBitmap() != null) {
                map.setBackgroundImage(site.getBackgroundBitmap());
            }

            user = new UserDrawable(this, map);

            if (site.getLastLocation() != null) {
                user.setRelativePosition(site.getLastLocation().getX(), site.getLastLocation().getY());
            } else {
                user.setRelativePosition(map.getWidth() / 2, map.getHeight() / 2);
            }

            LocationServiceFactory.getLocationService().setRelativeNorth(site.getNorth());
            LocationServiceFactory.getLocationService().setGridSpacing(site.getGridSpacingX(), site.getGridSpacingY());
            stepDetectionProvider = new StepDetectionProvider(this);
            stepDetectionProvider.setLocationChangeListener(this);

            initUI();
            initSensors();
            initSlidingMenu();
        } catch (Exception ex) {
            log.error("Failed to create ProjectSiteActivity: " + ex.getMessage(), ex);
            Toast.makeText(this, R.string.project_site_load_failed, Toast.LENGTH_LONG).show();
            this.finish();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        log.debug("setting context");

        multiTouchView.loadImages(this);
        map.load();
        // stepDetectionProvider.start();

        if (walkingAndScanning) {
            setWalkingAndScanning(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //重写了Menu监听，实现按下手机Menu键弹出和关闭侧滑菜单
        if(keyCode==KeyEvent.KEYCODE_MENU){
            initSlidingMenu();
            }
         return super.onKeyDown(keyCode, event);
        }
    //Sensor 函数
    //按键监听
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.scanStart:
                try {
                    doStartScan();
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case R.id.scanStop:
                try {
                    doStopScan();
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case R.id.btnClear:
                try {
                    doClear();
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case R.id.project_site_toggle_autorotate:

                ToggleButton button = (ToggleButton) findViewById(R.id.project_site_toggle_autorotate);

                if (button.isChecked()) {
                    map.startAutoRotate();
                    Logger.d("Started autorotate.");
                } else {
                    map.stopAutoRotate();
                    Logger.d("Stopped autorotate.");
                }

                break;
        }
    }

    protected void setWalkingAndScanning(boolean shouldRun) {
        if (!shouldRun) {
            // stop!

            if (stepDetectionProvider.isRunning())
                stepDetectionProvider.stop();

        } else {
            // start

            if (!stepDetectionProvider.isRunning()) {
                stepDetectionProvider.start();
            }
        }
    }
    //****************************************/
    // 初始化函数
    //******************************************/
    private void initUI(){
        ((Button)findViewById(R.id.scanStart)).setOnClickListener(this);
        ((Button)findViewById(R.id.scanStop)).setOnClickListener(this);
        //((Button)findViewById(R.id.btnClear)).setOnClickListener(this);
        ((ToggleButton) findViewById(R.id.project_site_toggle_autorotate)).setOnClickListener(this);
        //tvStep = (TextView)findViewById(R.id.tvStep);
        //tvStepLength = (TextView)findViewById(R.id.tvStepLength);
        //tvHeading = (TextView)findViewById(R.id.tvHeading);
        //tvLocation = (TextView)findViewById(R.id.tvLocation);
        multiTouchView = ((MultiTouchView) findViewById(R.id.mapView));
        multiTouchView.setRearrangable(false);
        multiTouchView.addDrawable(map);

        if (freshSite) {
            // start configuration dialog
            Dialogshow(DIALOG_FRESH_SITE);
        }

    }
    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    private void initSlidingMenu(){
        // configure the SlidingMenu
        SlidingMenu menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        // 设置触摸屏幕的模式
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
//        menu.setShadowDrawable(R.drawable.shadow);

        // 设置滑动菜单视图的宽度
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        // 设置渐入渐出效果的值
        menu.setFadeDegree(0.35f);
        /**
         * SLIDING_WINDOW will include the Title/ActionBar in the content
         * section of the SlidingMenu, while SLIDING_CONTENT does not.
         */
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        //为侧滑菜单设置布局
        menu.setMenu(R.layout.layout_menu);
        }


    private Dialog CreateDialog(int id) {
        switch (id) {

            case DIALOG_SET_BACKGROUND:

                AlertDialog.Builder bckgAlert = new AlertDialog.Builder(this);
                bckgAlert.setTitle(R.string.project_site_dialog_background_title);
                bckgAlert.setMessage(R.string.project_site_dialog_background_message);

                LinearLayout bckgLayout = new LinearLayout(this);
                bckgLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                bckgLayout.setGravity(Gravity.CENTER);
                bckgLayout.setOrientation(LinearLayout.VERTICAL);
                bckgLayout.setPadding(5, 5, 5, 5);

                final TextView pathTextView = new TextView(this);
                backgroundPathTextView = pathTextView;
                pathTextView.setText(R.string.project_site_dialog_background_default_path);
                pathTextView.setPadding(10, 0, 10, 10);

                bckgLayout.addView(pathTextView);

                Button pathButton = new Button(this);
                pathButton.setText(R.string.project_site_dialog_background_path_button);
                pathButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(context, FileBrowser.class);
                        i.putExtra(FileBrowser.EXTRA_MODE, FileBrowser.MODE_LOAD);
                        i.putExtra(FileBrowser.EXTRA_ALLOWED_EXTENSIONS, "jpg,png,gif,jpeg,bmp");
                        startActivityForResult(i, FILEBROWSER_REQUEST);
                    }

                });

                bckgLayout.addView(pathButton);

                bckgAlert.setView(bckgLayout);

                bckgAlert.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setBackgroundImage(pathTextView.getText().toString());
                        if (freshSite) {
                            Dialogshow(DIALOG_ASK_CHANGE_SCALE);
                        }
                    }
                });

                bckgAlert.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                        freshSite = false;
                    }
                });

                Dialog bckgDialog = bckgAlert.create();
                bckgDialog.setCanceledOnTouchOutside(true);

                return bckgDialog;

            case DIALOG_SET_SCALE_OF_MAP:
                AlertDialog.Builder scaleOfMapDialog = new AlertDialog.Builder(this);

                scaleOfMapDialog.setTitle(R.string.project_site_dialog_scale_of_map_title);
                scaleOfMapDialog.setMessage(R.string.project_site_dialog_scale_of_map_message);

                // Set an EditText view to get user input
                final EditText scaleInput = new EditText(this);
                scaleInput.setSingleLine(true);
                scaleInput.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                scaleOfMapDialog.setView(scaleInput);

                scaleOfMapDialog.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        try {
                            float value = Float.parseFloat(scaleInput.getText().toString());
                            setScaleOfMap(value);
                        } catch (NumberFormatException nfe) {
                            Logger.w("Wrong number format format!");
                            Toast.makeText(context, getString(R.string.not_a_number, scaleInput.getText()), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                scaleOfMapDialog.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                return scaleOfMapDialog.create();

            case DIALOG_FRESH_SITE:

                AlertDialog.Builder freshBuilder = new AlertDialog.Builder(context);
                freshBuilder.setTitle(R.string.project_site_dialog_fresh_site_title);
                freshBuilder.setMessage(R.string.project_site_dialog_fresh_site_message);

                freshBuilder.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Dialogshow(DIALOG_SET_BACKGROUND);
                    }

                });

                freshBuilder.setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                        freshSite = false;
                    }
                });

                return freshBuilder.create();

            case DIALOG_ASK_CHANGE_SCALE:

                AlertDialog.Builder askScaleBuilder = new AlertDialog.Builder(context);
                askScaleBuilder.setTitle(R.string.project_site_dialog_ask_change_scale_title);
                askScaleBuilder.setMessage(R.string.project_site_dialog_ask_change_scale_message);

                askScaleBuilder.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        scaleOfMap();
                    }

                });

                askScaleBuilder.setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                        freshSite = false;
                    }
                });

                return askScaleBuilder.create();

            case DIALOG_ASK_FOR_NORTH:

                AlertDialog.Builder askNorthBuilder = new AlertDialog.Builder(context);
                askNorthBuilder.setTitle(R.string.project_site_dialog_ask_north_title);
                askNorthBuilder.setMessage(R.string.project_site_dialog_ask_north_message);

                askNorthBuilder.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setMapNorth();
                        freshSite = false;
                    }

                });

                askNorthBuilder.setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                        freshSite = false;
                    }
                });

                return askNorthBuilder.create();

            default:
                return CreateDialog(id);
        }
    }

    private void Dialogshow(int id){
        CreateDialog(id).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.project_site, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.project_site_menu_change_name:
                Dialogshow(DIALOG_TITLE);

                return false;

            case R.id.project_site_menu_save:
                //saveProjectSite();
                return false;

            case R.id.project_site_menu_change_size:
                Dialogshow(DIALOG_CHANGE_SIZE);
                return false;

            case R.id.project_site_menu_set_background:
                Dialogshow(DIALOG_SET_BACKGROUND);
                return false;

            case R.id.project_site_menu_set_scale_of_map:

                if (scaler == null) {
                    scaleOfMap();
                } else {
                    // just hide the scalers, don't change the scaleing
                    scaler.removeScaleSliders();
                    map.removeSubDrawable(scaler);
                    scaler = null;
                    invalidate();
                }

                return false;

            case R.id.project_site_menu_set_north:

                setMapNorth();
                return false;

            case R.id.project_site_menu_select_bssids:

                this.Dialogshow(DIALOG_SELECT_BSSIDS);

                return false;

            case R.id.project_site_reset_zoom_button:
                Logger.d("resetting Zoom");
                multiTouchView.resetAllScale();
                multiTouchView.resetAllXY();
                multiTouchView.resetAllAngle();
                multiTouchView.recalculateDrawablePositions();
                multiTouchView.invalidate();
                break;

            case R.id.project_site_snap_user_button:
                Logger.d("Snapping user to grid");
                user.snapPositionToGrid();
                multiTouchView.invalidate();
                break;

            case R.id.project_site_add_known_ap:
                Dialogshow(DIALOG_ADD_KNOWN_AP);
                break;

            case R.id.project_site_menu_change_scan_interval:
                Dialogshow(DIALOG_CHANGE_SCAN_INTERVAL);
                break;

            case R.id.project_site_menu_track_steps:
                trackSteps=!trackSteps;
                if(trackSteps==false){
                    // tracking disabled
                    map.deleteAllSteps();
                }
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return false;
    }
    /**
     *
     */
    protected void setMapNorth() {
        if (northDrawable == null) {
            // Stop auto-rotate when map north is set
            ((ToggleButton) findViewById(R.id.project_site_toggle_autorotate)).setChecked(false);
            map.stopAutoRotate();

            // create the icon the set the north
            northDrawable = new NorthDrawable(this, map, site) {

                /*
                 * (non-Javadoc)
                 *
                 * @see at.fhstp.wificompass.view.NorthDrawable#onOk()
                 */
                @Override
                public void onOk() {
                    super.onOk();
                    northDrawable = null;
                    site.setNorth(ToolBox.normalizeAngle(adjustmentAngle));
                    map.setAngleAdjustment(site.getNorth());

                    LocationServiceFactory.getLocationService().setRelativeNorth(site.getNorth());
                    Logger.d("set adjustment angle of map to "+site.getNorth());
                    Toast.makeText(ctx, R.string.project_site_nort_set, Toast.LENGTH_SHORT).show();
                    getInitAngle = true;
                }

            };
            northDrawable.setRelativePosition(site.getWidth() / 2, site.getHeight() / 2);
            northDrawable.setAngle(map.getAngle() + site.getNorth());

        } else {
            map.removeSubDrawable(northDrawable);
            // do not set the angle, if the menu option is clicked
            // site.setNorth(northDrawable.getAngle());
            // LocationServiceFactory.getLocationService().setRelativeNorth(site.getNorth());
            northDrawable = null;
        }

        multiTouchView.invalidate();

    }

    /**
     *
     */
    protected void scaleOfMap() {
        if (scaler == null) {
            scaler = new ScaleLineDrawable(context, map, new OkCallback() {

                @Override
                public void onOk() {
                    onMapScaleSelected();
                }
            });
            scaler.getSlider(1).setRelativePosition(user.getRelativeX() - 80, user.getRelativeY());
            scaler.getSlider(2).setRelativePosition(user.getRelativeX() + 80, user.getRelativeY());
            multiTouchView.invalidate();
        } else {
            onMapScaleSelected();
        }
    }
    protected void onMapScaleSelected() {
        scalerDistance = scaler.getSliderDistance();
        scaler.removeScaleSliders();
        map.removeSubDrawable(scaler);
        scaler = null;
        invalidate();
        Dialogshow(DIALOG_SET_SCALE_OF_MAP);
    }
    protected void setBackgroundImage(String path) {

        try {
            Bitmap bmp = BitmapFactory.decodeFile(path);
            site.setBackgroundBitmap(bmp);
            map.setBackgroundImage(bmp);
            site.setSize(bmp.getWidth(), bmp.getHeight());
            map.setSize(bmp.getWidth(), bmp.getHeight());
            user.setRelativePosition(bmp.getWidth() / 2, bmp.getHeight() / 2);
            multiTouchView.invalidate();
            Toast.makeText(context, "set " + path + " as new background image!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Logger.e("could not set background", e);
            Toast.makeText(context, getString(R.string.project_site_set_background_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    protected void setScaleOfMap(float scale) {
        float mapScale = scalerDistance / scale;
        site.setGridSpacingX(mapScale);
        site.setGridSpacingY(mapScale);
        LocationServiceFactory.getLocationService().setGridSpacing(site.getGridSpacingX(), site.getGridSpacingY());
        MultiTouchDrawable.setGridSpacing(mapScale, mapScale);
        multiTouchView.invalidate();
        Toast.makeText(this, getString(R.string.project_site_mapscale_changed, mapScale), Toast.LENGTH_SHORT).show();

        if (freshSite) {
            Dialogshow(DIALOG_ASK_FOR_NORTH);
        }
    }
    /*
 * (non-Javadoc)
 *
 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Logger.d("Activity result of " + requestCode + " " + resultCode + " " + (data != null ? data.toString() : ""));

        switch (requestCode) {
            case FILEBROWSER_REQUEST:

                if (resultCode == Activity.RESULT_OK && data != null) {
                    String path = data.getExtras().getString(FileBrowser.EXTRA_PATH);

                    if (backgroundPathTextView != null) {
                        backgroundPathTextView.setText(path);
                    } else {
                        Logger.w("the background image dialog textview should not be null?!?");
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }

    }
    //****************************************/
    // 计步函数
    //******************************************/
    private boolean stepDetecter(float Time,float acc){
        float[] normAcc = new float[2];
        boolean isStep;
        isStep = false;
        normAcc[1] = acc;
        normAcc[0] = Time;

        if(xTick == 0)
            accold1 = normAcc;
        else if(accNo == 1)
            accold2 = normAcc;
        else{
            accnew = normAcc;
            if(accold1[1]<accold2[1] && accold2[1] > accnew[1]){
                if (accold2[0]-peakmin[0]> deltaTime)
                    peakmax = accold2;
                    peakmaxReady = true;
            }
            else if(accold1[1]>accold2[1] && accold2[1] < accnew[1]){
                if (accold2[0] - peakmax[0] >deltaTime  && peakmaxReady){
                    peakmin = accold2;
                    if(peakmax[1] - peakmin[1] >deltaA){
                        stepLength =(float)( 0.5 * Math.pow(peakmax[1] - peakmin[1],0.25) );
                        String str = String.format("%.2f",stepLength);
                        tvStepLength.setText(str);
                        isStep = true;
                        peakmaxReady = false;
                    }
                }

            }
            accold1 = accold2;
            accold2 = accnew;
        }
        return isStep;
    }
    //****************************************/
    // 平滑滤波
    //******************************************/

    private float filter(float data){
        int count;
        float  sum=0;
        value_buf[i_filter] = data;
        i_filter++;
        if ( i_filter == N_windows )   i_filter = 0;
        for ( count=0;count<N_windows;count++){
            sum = sum + value_buf[count];
        }
        return (sum/N_windows);
    }
    //****************************************/
    // 传感器数据读取开始与停止函数
    //******************************************/
    private void doStartScan() throws Exception{
        Log.d(TAG, "Create File");

        //timeStart = 0;//开始时时间归零
        //peakmax[0] = 0;peakmin[0] = 0;
        //peakmax[1] = 0;peakmin[1] =0;
        Log.d(TAG, "Start Listener");
        setWalkingAndScanning(true);
        //ticker = new Ticker(this);
        //ticker.start();
        //sensorManager.registerListener((SensorEventListener) ticker, accSensor, 1000 / sampleRate);
        //sensorManager.registerListener((SensorEventListener) ticker, gyroSensor, 1000 / sampleRate);
        doNotify("Scan Starting");

    }
    private void doStopScan() throws Exception {
        Log.d(TAG, "Stop Scan");
        Log.d(TAG, "Stop Listener");
        //sensorManager.unregisterListener((SensorEventListener) ticker);
        //ticker.interrupt();
        //ticker.join();
        //ticker = null;
        setWalkingAndScanning(false);
        doNotify("Scan Stop!");
    }
    private void doClear() throws Exception{
        //xTick = 0;
        //timeStart=0;
        //stepNo = 0;
        q0 = 1.0f; q1 = 0.0f; q2 = 0.0f; q3 = 0.0f;
        //tvStep.setText(String.format("%s", stepNo));
    }

    //****************************************/
    // Lagrange 二次插值
    //******************************************/
    public float Lagrange2_interpolation(float[] x, float[] y, float xk){
        float x0 = x[0];
        float y0 = y[0];
        float x1 = x[1];
        float y1 = y[1];
        float x2 = x[2];
        float y2 = y[2];
        float yk;
        yk = y0 * ( (xk-x1)*(xk-x2)/(x0-x1)/(x0-x2) ) + y1 * ( (xk-x0)*(xk-x2)/(x1-x0)/(x1-x2) ) + y2 * ( (xk-x0)*(xk-x1)/(x2-x0)/(x2-x1) );
        return yk;
    }

    //****************************************/
    // 屏幕显示通知函数的简化
    //******************************************/
    public void doNotify(String message) {
        doNotify(message, false);
    }
    public void doNotify(String message, boolean longMessage) {
        (Toast.makeText(this, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
        Log.d(TAG, "Notify: " + message);
    }

    /**
     * Periodically called by the ticker
     *
     * @param accEvent,gyroEvent
     *          current sensor data.
     */
    protected void doPdrDraw(SensorEvent accEvent, SensorEvent gyroEvent) {

        float NS2S=1.0f/1000000000.0f;//纳秒转为秒
        if(timeStart==0){
            timeStart=accEvent.timestamp;
            timestamp=0;
        }
        else
            timestamp=(accEvent.timestamp-timeStart)*NS2S;
        float yvalue = (float)Math.sqrt(accEvent.values[0]*accEvent.values[0]+accEvent.values[1]*accEvent.values[1]+accEvent.values[2]*accEvent.values[2]);
        float lpyvalue = 0;
        if(xTick+1>N_windows)
            lpyvalue = filter(yvalue);
        else
            value_buf[xTick] = yvalue;
        float[] euler = mMadgwick.rotMat2euler(mMadgwick.quatern2rotMat(mMadgwick.MadgwickAHRSupdateGyro(gyroEvent.values[0], gyroEvent.values[1], gyroEvent.values[2])));
        heading = euler[2];
        tvHeading.setText(String.format("%.2f", heading * 180 / Math.PI));
        //Log.d(TAG,String.format("%.2f,%f",timestamp,lpyvalue));
        if (stepDetecter(timestamp,lpyvalue)){
            stepNo= stepNo+1;
            if(stepNo==1) {
                locationx = (float)(0f + stepLength * Math.sin(heading));
                locationy = (float)(0f + stepLength * Math.cos(heading));
            }
            else {
                locationx = (float)(locationx + stepLength * Math.sin(heading));
                locationy = (float)(locationy + stepLength * Math.cos(heading));
            }
            tvStep.setText(String.format("%s",stepNo));
            tvLocation.setText(String.format("( %.2f, %.2f)", locationx, locationy));
            map.addStep(new PointF(-locationx,locationy));
            map.setPos(-locationx * map.getScaleX(), locationy * map.getScaleY(), 30, 30, (float) Math.PI, true);
            multiTouchView.invalidate();
        }
        xTick++;
    }
    @Override
    public void invalidate() {
        if (multiTouchView != null) {
            multiTouchView.invalidate();
        }
    }

    @Override
    public void onLocationChange(Location loc) {
        // info from StepDetectionProvider, that the location changed.
        user.setRelativePosition(loc.getX(), loc.getY());
        map.addStep(new PointF(loc.getX(), loc.getY()));
        //messageHandler.sendEmptyMessage(MESSAGE_REFRESH);
    }

}