package com.r19012817.mapinator6;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

// classes needed to initialize map
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonElement;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.BubbleLayout;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

// classes needed to add the location component
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;

// classes needed to add a marker
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.plugins.places.picker.PlacePicker;
import com.mapbox.mapboxsdk.plugins.places.picker.model.PlacePickerOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

// classes to calculate a route
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

// classes needed to launch navigation UI
import android.view.View;
import android.widget.Button;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {
    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;
    // variables for adding location layer
    //onmap comment
    //search comment add favourite landmark

    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    // variables needed to initialize navigation
    private Button button;

    private FloatingActionButton fab_showRoute;
    private  FloatingActionButton fab_info;
    private FloatingActionButton fab_search;
    private FloatingActionButton fab_recentre;
    private TextView selectedLocationTextView;

    //holds the point on the map that the user clicks
    private Point clickedPoint;
    private static final int PLACE_SELECTION_REQUEST_CODE = 56789;

    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;

    //info stuff
    private static final String GEOJSON_SOURCE_ID = "GEOJSON_SOURCE_ID";
    private static final String MARKER_IMAGE_ID = "MARKER_IMAGE_ID";
    private static final String CALLOUT_IMAGE_ID = "CALLOUT_IMAGE_ID";
    private static final String MARKER_LAYER_ID = "MARKER_LAYER_ID";
    private static final String CALLOUT_LAYER_ID = "CALLOUT_LAYER_ID";
    private GeoJsonSource source;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mv_map_view);
        //mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fab_showRoute= findViewById(R.id.fab_showRoute);
        fab_search = findViewById(R.id.fab_search);
        fab_info = findViewById(R.id.fab_showInfo);
        fab_recentre= findViewById(R.id.fab_recentre);

        fab_showRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRouteOnMap();
            }
        });


        fab_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              getLocationInfo();
             //   enableLocationComponent(mapboxMap.getStyle());
            }
        });


        fab_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchActivity();
            }
        });

        fab_recentre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                locationComponent.setCameraMode(CameraMode.TRACKING);
                //enableLocationComponent(mapboxMap.getStyle());
            }
        });

    }


    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                enableLocationComponent(style);

                addDestinationIconSymbolLayer(style);

                Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.blue_marker_view, null);
                Bitmap mBitmap = BitmapUtils.getBitmapFromDrawable(drawable);

                // Add the symbol layer icon to map for future use
               // style.addImage(symbolIconId, BitmapFactory.decodeResource(
              //        MainActivity.this.getResources(), R.drawable.blue_marker_view));

                style.addImage(symbolIconId, mBitmap);

                // Create an empty GeoJSON source using the empty feature collection
                setUpSource(style);// Set up a new symbol layer for displaying the searched location's feature coordinates
                setupLayer(style);

                mapboxMap.addOnMapClickListener(MainActivity.this);
                button = findViewById(R.id.btn_start_navigation);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                .directionsRoute(currentRoute)
                                .shouldSimulateRoute(false)
                                .build();
                        // Call this method with Context from within an Activity
                        NavigationLauncher.startNavigation(MainActivity.this, options);
                    }
                });
            }
        });

    }

/*
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

               // initSearchFab();

               //  addUserLocations();

                Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.blue_marker_view, null);
                Bitmap mBitmap = BitmapUtils.getBitmapFromDrawable(drawable);

                // Add the symbol layer icon to map for future use
               // style.addImage(symbolIconId, BitmapFactory.decodeResource(
              //        MainActivity.this.getResources(), R.drawable.blue_marker_view));

                style.addImage(symbolIconId, mBitmap);

                // Create an empty GeoJSON source using the empty feature collection
                setUpSource(style);// Set up a new symbol layer for displaying the searched location's feature coordinates
                setupLayer(style);
            }
        });
    }
*/

    //displays the intent for search
    private void showSearchActivity()
    {

        Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .geocodingTypes("poi")
                                .proximity(originPoint)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(MainActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
    }

    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }

    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                iconImage(symbolIconId),
                iconOffset(new Float[] {0f, -8f})
        ));
    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
        BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {


        Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        Point  originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());
        clickedPoint = destinationPoint;

      //  GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
      //  if (source != null)
      //  {
      //      source.setGeoJson(Feature.fromGeometry(destinationPoint));
      //  }


        getRoute(originPoint, destinationPoint);
        button.setEnabled(true);
        button.setBackgroundResource(R.color.mapboxBlue);

        handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
        return true;
    }

    //____________info stuff_________________
    /**
     * Sets up all of the sources and layers needed for this example
     */
    public void setUpData() {
        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> {
                setupSource(style);
                setUpClickLocationIconImage(style);
                setUpClickLocationMarkerLayer(style);
                setUpInfoWindowLayer(style);
            });
        }
    }

    /**
     * Adds the GeoJSON source to the map
     */
    private void setupSource(@NonNull Style loadedStyle) {
        source = new GeoJsonSource(GEOJSON_SOURCE_ID);
        loadedStyle.addSource(source);
    }

    /**
     * Adds the marker image to the map for use as a SymbolLayer icon
     */
    private void setUpClickLocationIconImage(@NonNull Style loadedStyle) {
        loadedStyle.addImage(MARKER_IMAGE_ID, BitmapFactory.decodeResource(
                this.getResources(), R.drawable.blue_marker_view));
    }

    /**
     * Needed to show the Feature properties info window.
     */
    private void refreshSource(Feature featureAtClickPoint) {
        if (source != null) {
            source.setGeoJson(featureAtClickPoint);
        }
    }

    /**
     * Adds a SymbolLayer to the map to show the click location marker icon.
     */
    private void setUpClickLocationMarkerLayer(@NonNull Style loadedStyle) {
        loadedStyle.addLayer(new SymbolLayer(MARKER_LAYER_ID, GEOJSON_SOURCE_ID)
                .withProperties(
                        iconImage(MARKER_IMAGE_ID),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true),
                        iconOffset(new Float[] {0f, -8f})
                ));
    }

    /**
     * Adds a SymbolLayer to the map to show the Feature properties info window.
     */
    private void setUpInfoWindowLayer(@NonNull Style loadedStyle) {
        loadedStyle.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, GEOJSON_SOURCE_ID)
                .withProperties(
// show image with id title based on the value of the name feature property
                        iconImage(CALLOUT_IMAGE_ID),

// set anchor of icon to bottom-left
                       iconAnchor(ICON_ANCHOR_BOTTOM),

// prevent the feature property window icon from being visible even
// if it collides with other previously drawn symbols
                    //    iconAllowOverlap(false),

// prevent other symbols from being visible even if they collide with the feature property window icon
                        iconIgnorePlacement(false),

// offset the info window to be above the marker
                        iconOffset(new Float[] {-2f, -28f})
                ));
    }

    /**
     * This method handles click events for SymbolLayer symbols.
     *
     * @param screenPoint the point on screen clicked
     */
    private boolean handleClickIcon(PointF screenPoint) {
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint);
        if (!features.isEmpty()) {
            Feature feature = features.get(0);

            StringBuilder stringBuilder = new StringBuilder();

            if (feature.properties() != null) {
                for (Map.Entry<String, JsonElement> entry : feature.properties().entrySet()) {
                    stringBuilder.append(String.format("%s - %s", entry.getKey(), entry.getValue()));
                    stringBuilder.append(System.getProperty("line.separator"));
                }
                new GenerateViewIconTask(MainActivity.this).execute(FeatureCollection.fromFeature(feature));
            }
        } else {
            Toast.makeText(this, getString(R.string.query_feature_no_properties_found), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * Invoked when the bitmap has been generated from a view.
     */
    public void setImageGenResults(HashMap<String, Bitmap> imageMap) {
        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> {
                style.addImages(imageMap);
            });
        }
    }

    /**
     * AsyncTask to generate Bitmap from Views to be used as iconImage in a SymbolLayer.
     * <p>
     * Call be optionally be called to update the underlying data source after execution.
     * </p>
     * <p>
     * Generating Views on background thread since we are not going to be adding them to the view hierarchy.
     * </p>
     */
    private static class GenerateViewIconTask extends AsyncTask<FeatureCollection, Void, HashMap<String, Bitmap>> {

        private final WeakReference<MainActivity> activityRef;
        private Feature featureAtMapClickPoint;

        GenerateViewIconTask(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected HashMap<String, Bitmap> doInBackground(FeatureCollection... params) {
            MainActivity activity = activityRef.get();
            HashMap<String, Bitmap> imagesMap = new HashMap<>();
            if (activity != null) {
                LayoutInflater inflater = LayoutInflater.from(activity);

                if (params[0].features() != null) {
                    featureAtMapClickPoint = params[0].features().get(0);

                    StringBuilder stringBuilder = new StringBuilder();

                    BubbleLayout bubbleLayout = (BubbleLayout) inflater.inflate(
                            R.layout.activity_query_feature_window_symbol_layer, null);

                    TextView titleTextView = bubbleLayout.findViewById(R.id.info_window_title);
                    titleTextView.setText(activity.getString(R.string.query_feature_marker_title));

                    if (featureAtMapClickPoint.properties() != null) {
                        for (Map.Entry<String, JsonElement> entry : featureAtMapClickPoint.properties().entrySet()) {
                            stringBuilder.append(String.format("%s - %s", entry.getKey(), entry.getValue()));
                            stringBuilder.append(System.getProperty("line.separator"));
                        }
                    //    Toast.makeText(activity, stringBuilder.toString(), Toast.LENGTH_SHORT).show();

                        Log.i("INFO", stringBuilder.toString());

                        TextView propertiesListTextView = bubbleLayout.findViewById(R.id.info_window_feature_properties_list);
                        propertiesListTextView.setText(stringBuilder.toString());

                        int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        bubbleLayout.measure(measureSpec, measureSpec);

                        float measuredWidth = bubbleLayout.getMeasuredWidth();

                        bubbleLayout.setArrowPosition(measuredWidth / 2 - 5);

                        Bitmap bitmap = MainActivity.SymbolGenerator.generate(bubbleLayout);
                        imagesMap.put(CALLOUT_IMAGE_ID, bitmap);
                    }
                }
            }

            return imagesMap;
        }

        @Override
        protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
            super.onPostExecute(bitmapHashMap);
            MainActivity activity = activityRef.get();
            if (activity != null && bitmapHashMap != null) {
                activity.setImageGenResults(bitmapHashMap);
                activity.refreshSource(featureAtMapClickPoint);
            }
        }

    }

    /**
     * Utility class to generate Bitmaps for Symbol.
     */
    private static class SymbolGenerator {

        /**
         * Generate a Bitmap from an Android SDK View.
         *
         * @param view the View to be drawn to a Bitmap
         * @return the generated bitmap
         */
        static Bitmap generate(@NonNull View view) {
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(measureSpec, measureSpec);

            int measuredWidth = view.getMeasuredWidth();
            int measuredHeight = view.getMeasuredHeight();

            view.layout(0, 0, measuredWidth, measuredHeight);
            Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.TRANSPARENT);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        }
    }















    //gets the address name in an intent
    public void getLocationInfo()
    {
        Intent intent = new PlacePicker.IntentBuilder()
                .accessToken(Mapbox.getAccessToken())
                .placeOptions(
                        PlacePickerOptions.builder()
                                .statingCameraPosition(
                                        new CameraPosition.Builder()
                                                .target(new LatLng(clickedPoint.latitude(), clickedPoint.longitude()))
                                                .zoom(16)
                                                .build())
                                .build())
                .build(this);


        startActivityForResult(intent, PLACE_SELECTION_REQUEST_CODE);
    }

/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_SELECTION_REQUEST_CODE && resultCode == RESULT_OK){

            // Retrieve the information from the selected location's CarmenFeature

            CarmenFeature carmenFeature = PlacePicker.getPlace(data);

        }
    }
*/


    //method display the info about place
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

// Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);
// Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
// Then retrieve and update the source designated for showing a selected location's symbol layer icon

            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[]{Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }

// Move map camera to the selected location
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                            ((Point) selectedCarmenFeature.geometry()).longitude()))
                                    .zoom(14)
                                    .build()), 4000);
                }
            }
        }
    }

    //method to show the route on the map
    public void showRouteOnMap()
    {

      if (clickedPoint != null)
      {
          Point destinationPoint = clickedPoint;
          Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                  locationComponent.getLastKnownLocation().getLatitude());

          GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
          if (source != null) {
              source.setGeoJson(Feature.fromGeometry(destinationPoint));
          }

          getRoute(originPoint, destinationPoint);
          button.setEnabled(true);
          button.setBackgroundResource(R.color.mapboxBlue);
      }
      else
      {
          Toast.makeText(this, "PLease select a location.", Toast.LENGTH_SHORT).show();
      }

    }

    //gets route and directions
    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request


        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}