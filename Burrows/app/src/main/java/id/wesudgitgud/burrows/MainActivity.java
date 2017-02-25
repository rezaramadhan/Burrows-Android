package id.wesudgitgud.burrows;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import id.wesudgitgud.burrows.Controller.DatabaseManager;
import id.wesudgitgud.burrows.models.Constants;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {
    /*, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener*/
    private static final String TAG = "MainActivity";
    private static final long UPDATE_INTERVAL = 300000;
    private static final long FASTEST_INTERVAL = 200000;
    private static final int REQUEST_LOCATION_PERMISSION = 744;
    ViewFlipper viewFlipper;
    Button next;
    Button prev;

    private SensorManager sensorManager;
    private long lastUpdate;
    private TextView editShake;

    private GoogleApiClient mGoogleApiClient;
    //private AddressResultReceiver mResultReceiver;
    //private Location mLocation;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView titleMenu = (TextView)findViewById(R.id.textTitleMenu);
        titleMenu.setText("Burrows");

        viewFlipper = (ViewFlipper)findViewById(R.id.viewFlipper);
        next = (Button) findViewById(R.id.btnNext);
        prev = (Button) findViewById(R.id.btnPrev);

        next.setOnClickListener(this);
        prev.setOnClickListener(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lastUpdate = System.currentTimeMillis();
        editShake = (TextView) findViewById(R.id.textView2);

//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .addApi(LocationServices.API)
//                .build();
//
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
//        }
//
//        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//
//        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
//            Log.d("gps_on","GPS is on");
//            Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
//        }else{
//            Log.d("gps_off","GPS is off");
//            showGPSDisabledAlertToUser();
//        }
//
//        mResultReceiver = new AddressResultReceiver(new android.os.Handler());

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        int current = settings.getInt("petnum",0);
        ViewFlipper pet_image = (ViewFlipper) findViewById(R.id.viewFlipper);
        pet_image.setDisplayedChild(current);

        Log.d("petnum_start",String.valueOf(current));

        try {
            updateInfo();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view){
        if (view == next){
            viewFlipper.showNext();
            Log.d("petnum",String.valueOf(viewFlipper.getDisplayedChild()));
        } else {
            viewFlipper.showPrevious();
            Log.d("petnum",String.valueOf(viewFlipper.getDisplayedChild()));
        }
        try {
            updateInfo();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void gotoMain(View v) {
        startActivity(new Intent(this, MainActivity.class));
    }

    public void gotoShop(View v) {
        startActivity(new Intent(MainActivity.this, ShopActivity.class));
    }

    public void gotoItem(View v) {
        startActivity(new Intent(MainActivity.this, ItemsActivity.class));
    }

    public void gotoAchievement(View v) {
        startActivity(new Intent(MainActivity.this, AchievementActivity.class));
    }

    public void gotoFriend(View v) {
        startActivity(new Intent(MainActivity.this, FriendsActivity.class));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            changeBackground(event);
        }
    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        float accelationSquareRoot = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        long actualTime = event.timestamp;
        if (accelationSquareRoot >= 4) //
        {
            if (actualTime - lastUpdate < 200) {
                return;
            }
            lastUpdate = actualTime;
            Log.d("Test","Accelerometer working");
            editShake.setText(getString(R.string.dont_shake));
            new CountDownTimer(3000, 1000) {

                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    editShake.setText("");
                }
            }.start();
        }
    }

    private void changeBackground(SensorEvent event) {
        RelativeLayout back = (RelativeLayout) findViewById(R.id.activity_main);
        Log.d("Lightlvl",Float.toString(event.values[0]));
        if (event.values[0] < 5) {
            //Dark
            back.setBackgroundColor(Color.GRAY);
        } else if (event.values[0] > 20) {
            //Bright
            back.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    protected void onStart() {
//        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
//        mGoogleApiClient.disconnect();
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        ViewFlipper pet_image = (ViewFlipper) findViewById(R.id.viewFlipper);
        int current = pet_image.getDisplayedChild();
        editor.putInt("petnum",current);
        editor.commit();

        Log.d("petnum_end",String.valueOf(current));

        super.onStop();
    }

//    @Override
//    @SuppressWarnings({"MissingPermission"})
//    public void onConnected(@Nullable Bundle bundle) {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
//        }
//        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            LocationRequest mLocationRequest = new LocationRequest();
//            mLocationRequest.setPriority(PRIORITY_HIGH_ACCURACY);
//            mLocationRequest.setInterval(UPDATE_INTERVAL);
//            mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
//            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//            Location mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//            startIntentService(mLocation);
//        }
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//        Log.i(TAG, "Connection Suspended");
//        mGoogleApiClient.connect();
//    }
//
//    @Override
//    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
//    }
//
//    @Override
//    public void onLocationChanged(Location location) {
//        startIntentService(location);
//    }
//
//    private void showGPSDisabledAlertToUser(){
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setCancelable(false);
//        builder.setMessage("GPS is disabled in your device. Would you like to enable it?");
//        builder.setPositiveButton("Go to settings to enable GPS", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
//            }
//        });
//        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//        AlertDialog alert = builder.create();
//        alert.show();
//    }
//
//    class AddressResultReceiver extends ResultReceiver {
//        public AddressResultReceiver(Handler handler) {
//            super(handler);
//        }
//
//        @Override
//        protected void onReceiveResult(int resultCode, Bundle resultData) {
//
//            // Display the address string
//            // or an error message sent from the intent service.
//            String mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
//            String[] temp = mAddressOutput.split("\n");
//            List<String> addressComponent = new ArrayList<String>(Arrays.asList(temp));
//            try {
//                Log.d("Loc", addressComponent.get(3).substring(5));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            // Show a toast message if an address was found.
//            if (resultCode == Constants.SUCCESS_RESULT) {
//                Toast.makeText(MainActivity.this, R.string.address_found, Toast.LENGTH_SHORT).show();
//            }
//
//        }
//    }
//
//    protected void startIntentService(Location mLocation) {
//        Intent intent = new Intent(this, FetchAddressIntentService.class);
//        intent.putExtra(Constants.RECEIVER, mResultReceiver);
//        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLocation);
//        startService(intent);
//    }

    private void updateInfo() throws JSONException {
        FirebaseUser FU = FirebaseAuth.getInstance().getCurrentUser();
        if (FU != null) {
            String username = "user/" + FU.getDisplayName();
            DatabaseManager data = new DatabaseManager(username);

            String exp = data.getJSONDObject().getString("exp");
            String money = data.getJSONDObject().getString("money");
            int lvl = Integer.parseInt(exp)/1000;

            TextView exp_view = (TextView) findViewById(R.id.exp_view);
            TextView money_view = (TextView) findViewById(R.id.money_view);
            TextView level = (TextView) findViewById(R.id.level);

            exp_view.setText(exp + "/1000");
            money_view.setText(money);
            level.setText(String.valueOf(lvl));

            String userpet = "userpet/" + FU.getDisplayName();
            DatabaseManager petdata = new DatabaseManager(userpet);

            ViewFlipper petview = (ViewFlipper) findViewById(R.id.viewFlipper);
            Log.d("petnumup",String.valueOf(petview.getDisplayedChild()));
            JSONObject petinfo = petdata.getJSONDObject();
            TextView pet_exp_view = (TextView) findViewById(R.id.pet_exp_b);
            TextView pet_lvl_view = (TextView) findViewById(R.id.pet_lvl_b);

            switch (petview.getDisplayedChild()) {
                case 0:
                    petinfo = petinfo.getJSONObject("beaver");
                    break;
                case 1:
                    petinfo = petinfo.getJSONObject("mouse");
                    pet_exp_view = (TextView) findViewById(R.id.pet_exp_m);
                    pet_lvl_view = (TextView) findViewById(R.id.pet_lvl_m);
                    break;
                case 2:
                    petinfo = petinfo.getJSONObject("rabbit");
                    pet_exp_view = (TextView) findViewById(R.id.pet_exp_r);
                    pet_lvl_view = (TextView) findViewById(R.id.pet_lvl_r);
                    break;
            }

            String pet_exp = petinfo.getString("exp");
            String pet_lvl = petinfo.getString("lv");

            pet_exp_view.setText(pet_exp + "/1000");
            pet_lvl_view.setText(pet_lvl);
        }
    }
}
