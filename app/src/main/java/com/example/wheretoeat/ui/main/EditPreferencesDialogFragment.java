package com.example.wheretoeat.ui.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.example.wheretoeat.R;
import com.example.wheretoeat.YelpService;
import com.example.wheretoeat.modals.Friends;
import com.example.wheretoeat.modals.Restaurant;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class EditPreferencesDialogFragment extends DialogFragment implements LocationListener{

    private EditText etCustomLocation;
    private Switch swUserLocation;
    private RadioGroup rgPrice;
    private RadioButton rv1;
    private RadioButton rv2;
    private RadioButton rv3;
    private RadioButton rv4;
    private Button btnClearPrice;
    private Button btnPrefDone;




    //for location functionality
    LocationManager lm;
    Double latitude;
    Double longitude;
    Criteria criteria;
    String bestProvider;

    String zipCode;





    private static final String TAG = "DialogFragment";

    public EditPreferencesDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static EditPreferencesDialogFragment newInstance(String title, ParseUser currUser, String username) {
        EditPreferencesDialogFragment frag = new EditPreferencesDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        frag.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialogTheme);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return getActivity().getLayoutInflater().inflate(R.layout.fragment_edit_preferences, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Fetch arguments from bundle and set title
        String title = "Restaurant Preferences";
        getDialog().setTitle(title);
        // Show soft keyboard automatically and request focus to field


        etCustomLocation = view.findViewById(R.id.etCustomLocation);
        swUserLocation = view.findViewById(R.id.swUserLocation);
        rgPrice = view.findViewById(R.id.rgPrice);
        btnPrefDone = view.findViewById(R.id.btnPrefDone);

        // Handle switch events
        swUserLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    etCustomLocation.setEnabled(false);
                    etCustomLocation.setText("");
                } else {
                    etCustomLocation.setEnabled(true);
                    etCustomLocation.requestFocus();
                    getDialog().getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
            }
        });
        // default focus
        etCustomLocation.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        btnPrefDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (swUserLocation.isChecked()) {
                    // begin process to get user's zip code
                    checkLocationPermissions();
                } else {
                    zipCode = etCustomLocation.getText().toString();
                    sendBackResult();
                }
            }
        });
        //Price Preferences
        btnClearPrice = view.findViewById(R.id.btnClearPrice);

        btnClearPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rgPrice.clearCheck();
            }
        });

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        return dialog;
    }


    @Override
    public void onPause() {
        super.onPause();
        if (lm != null) {
            lm.removeUpdates(this);
        }
    }


    public interface EditPreferencesDialogListener {
        void onFinishEditDialog(String location, String price);
    }

    // Call this method to send the data back to the parent fragment
    public void sendBackResult() {
        // Notice the use of `getTargetFragment` which will be set when the dialog is displayed
        EditPreferencesDialogListener listener = (EditPreferencesDialogListener) getTargetFragment();
        String pricePref = checkPricePref();
        listener.onFinishEditDialog(zipCode, pricePref);
        dismiss();
    }

    private String checkPricePref() {
        int buttonChecked = rgPrice.getCheckedRadioButtonId();
        if (buttonChecked == -1) {
            return null;
        }
        switch (buttonChecked) {
            case R.id.rv1:
                return "1";
            case R.id.rv2:
                return "2";
            case R.id.rv3:
                return "3";
            case R.id.rv4:
                return "4";
        }
        return null;
    }

    private void checkLocationPermissions() {
        Log.e(TAG, "in checklocationpermissions");
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        else {
            Log.e(TAG, "in checklocationpermissions-- else");
            try {
                getLongLat();
            } catch (IOException e) {
                Log.e(TAG, "check location permissions error", e);
                e.printStackTrace();

            }
        }
    }

    private void getLongLat() throws IOException {
        Log.e(TAG, "in getzipcode");
        lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(lm.getBestProvider(criteria, true));
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            Log.e(TAG, "location: " + latitude.toString() + longitude.toString());

            getZipCode();
        }
        else {
            lm.requestLocationUpdates(bestProvider, 1000, 0, this::onLocationChanged);
        }

    }
    @Override
    public void onLocationChanged(Location location) {
        //Hey, a non null location! Sweet!

        //remove location callback:
        lm.removeUpdates(this);

        //open the map:
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        Log.e(TAG, "location: " + latitude.toString() + longitude.toString());
        Toast.makeText(getContext(), "latitude:" + latitude + " longitude:" + longitude, Toast.LENGTH_SHORT).show();
        try {
            getZipCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void getZipCode() throws IOException {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

        String postalCode = addresses.get(0).getPostalCode();
        Log.e(TAG, "postalCode: " + postalCode);
        zipCode = postalCode;
        sendBackResult();
    }




    // Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.

                    Log.e(TAG, "in activtyresultlauncher");
                    try {
                        getLongLat();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Toast.makeText(getContext(), "Need location", Toast.LENGTH_SHORT).show();
                }
            });

}