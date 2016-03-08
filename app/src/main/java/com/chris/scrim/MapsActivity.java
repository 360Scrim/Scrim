package com.chris.scrim;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;



public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, Observer {
    private static final String TAG = MapsActivity.class.getName();
    private static final int PLACE_PICKER_REQUEST = 1;
    private GoogleMap mMap;
    private VitalizeAreaEditDialogManager vitalizeAreaEditDialogManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        VitalizeSlidingMenu.initializeSlidingMenu(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        /* Disable the my-location layer (this causes our LocationSource to be automatically deactivated.) */
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(mMap != null) {
                mMap.setMyLocationEnabled(false);
            }
        }
    }

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        vitalizeAreaEditDialogManager =  new VitalizeAreaEditDialogManager(this, googleMap);
        mMap = googleMap;

        // Get all areas and put it on the map when it is done loading.
        DBFireBaseHelper dbHelper = new DBFireBaseHelper(this);
        dbHelper.getAllScrimAreasFromFirebase();

        // Replace the (default) location source of the my-location layer with our custom LocationSource
        new FollowMeLocationListener(this, googleMap);
        setOnMapClickListener(mMap);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                final ScrimArea markerScrim = ScrimArea.getScrimAreaOfMarker(marker, VitalizeApplication.getAllAreas());
                if(markerScrim != null) {
                    AlertDialog.Builder markerInfoDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                    final View markerInfoView = MapsActivity.this.getLayoutInflater().inflate(R.layout.marker_info, null);
                    markerScrim.populateDateText((TextView)markerInfoView.findViewById(R.id.dateText));
                    //final Button
                    final ImageView typeImage = (ImageView) markerInfoView.findViewById(R.id.typeImage);
                    TextView spotsLeft = (TextView) markerInfoView.findViewById(R.id.spotsLeft);
                    TextView type = (TextView) markerInfoView.findViewById(R.id.typeText);
                    ((TextView) markerInfoView.findViewById(R.id.titleText)).setText(markerScrim.getTitle());
                    ((TextView) markerInfoView.findViewById(R.id.additInfoText)).setText(markerScrim.getAdditionalInfo());
                    typeImage.setImageResource((int) markerScrim.getTypeImage());
                    spotsLeft.setText("1/" + markerScrim.getNumSpots());
                    type.setText(markerScrim.getType());
                    final AlertDialog markerInfoDialog = markerInfoDialogBuilder.create();
                    final Button delete = (Button) markerInfoView.findViewById(R.id.deleteButton);
                    vitalizeAreaEditDialogManager.setDeleteClickListener(delete, marker, markerInfoDialog);
                    markerInfoView.findViewById(R.id.membersAndInvitesButton).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MapsActivity.this.startActivity(new Intent(MapsActivity.this, MembersAndInvitesActivity.class));
                        }
                    });
                    markerInfoView.findViewById(R.id.messageButton).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MapsActivity.this.startActivity(new Intent(MapsActivity.this, ChatActivity.class));
                        }
                    });
                    markerInfoView.findViewById(R.id.editButton).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            markerInfoDialog.dismiss();
                            vitalizeAreaEditDialogManager.showEditScrimDialog(markerScrim, null);
                        }
                    });
                    markerInfoDialog.setView(markerInfoView);
                    markerInfoDialog.getWindow().getAttributes().y = -600;
                    markerInfoDialog.show();
                }
                return true;
            }
        });
    }

    private void setOnMapClickListener (final GoogleMap mMap) {
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                //inflate layout we wantz
                vitalizeAreaEditDialogManager.showEditScrimDialog(null, latLng);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.find ) {
            try {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
            }
            catch(GooglePlayServicesNotAvailableException e) {

            }
            catch (GooglePlayServicesRepairableException e) {

            }
        }
        if (id == R.id.filter) {
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override

                public boolean onMenuItemClick(MenuItem item) {
                    //inflate layout we wantz
                    final View filterView = MapsActivity.this.getLayoutInflater().inflate(R.layout.filter, null);
                    final Spinner filterSpinner = (Spinner) filterView.findViewById(R.id.filter_spinner);
                    Button filterButton = (Button) filterView.findViewById(R.id.Filter_confirm);
                    Button cancelButton = (Button) filterView.findViewById(R.id.Filter_cancel);
                    //show a dialog that prompts the user if he/she wants to delete
                    AlertDialog.Builder addBuild = new AlertDialog.Builder(MapsActivity.this);
                    addBuild.setView(filterView);
                    final AlertDialog alertDialog = addBuild.create();
                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VitalizeApplication.getAllAreas();
                            alertDialog.dismiss();
                        }
                    });

                    filterButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int [] CheckId = {R.id.bballCheckBox, R.id.fballCheckBox,
                                    R.id.FrisbeeCheckBox, R.id.soccerCheckBox, R.id.tennisCheckBox, R.id.vballCheckBox};
                            final List<String> selectedTypes = new ArrayList<>();
                            for (int c = 0; c < CheckId.length; c++) {
                                CheckBox temp = (CheckBox) filterView.findViewById(CheckId[c]);
                                if (temp.isChecked()) {
                                    selectedTypes.add(temp.getText().toString());
                                }
                            }
                            //filter the item and just display the option chosen
                            if (selectedTypes.isEmpty()) {
                                for (ScrimArea a : VitalizeApplication.getAllAreas()) {
                                    a.getScrimMarker().setVisible(true);
                                }
                            } else {
                                for (ScrimArea a : VitalizeApplication.getAllAreas()) {
                                    a.getScrimMarker().setVisible(selectedTypes.contains(a.getType()));
                                }
                            }

                            alertDialog.dismiss();
                        }
                    });
                    alertDialog.show();
                    return false;
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                FollowMeLocationListener.moveToCurrentLocation(mMap, place.getLatLng());
                mMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName()+"").
                        draggable(false));
            }
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        ScrimArea.loadAllAreasOntoMap(mMap);
    }
}
