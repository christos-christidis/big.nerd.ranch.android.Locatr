package com.bignerdranch.locatr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class LocatrFragment extends Fragment {

    private static final String LOG_TAG = "LocatrFragment";

    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 0;

    private ImageView mImageView;
    private GoogleApiClient mClient;

    static LocatrFragment newInstance() {
        return new LocatrFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        assert getActivity() != null;
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        assert getActivity() != null;
                        // SOS: now menu item will be enabled
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        assert getActivity() != null;
        // SOS: this is not instant, the cmd is passed to the looper and handled later by a handler
        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_locatr, container, false);

        mImageView = view.findViewById(R.id.image_view);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_locatr, menu);

        MenuItem searchItem = menu.findItem(R.id.action_locate);
        searchItem.setEnabled(mClient.isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_locate) {
            if (hasLocationPermission()) {
                findImage();
            } else {
                requestPermissions(LOCATION_PERMISSIONS, LOCATION_PERMISSIONS_REQUEST_CODE);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // SOS: both FINE and COARSE permissions belong to the same group (p662), therefore I only need
    // to check for one of them. (if one has permission, the other has too)
    private boolean hasLocationPermission() {
        assert getActivity() != null;
        int result = ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    // SOS: in case of continuous updates, I'd also have to call removeLocationUpdates when I'm done.
    private void findImage() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(0);

        assert getActivity() != null;

        // SOS: ugh Android Studio is stupid, it requires a direct call to checkSelfPermission here...
        // I can't just call the wrapper hasLocationPermission, nor does it see that I've already
        // checked before I called findImage... ffs
        if (ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0])
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        new FusedLocationProviderClient(getActivity())
                .requestLocationUpdates(request, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Log.i(LOG_TAG, "Got a fix: " + locationResult);
                        List<Location> locations = locationResult.getLocations();
                        if (!locations.isEmpty()) {
                            new SearchTask(LocatrFragment.this).execute(locations.get(0));
                        }
                    }
                }, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            if (hasLocationPermission()) {
                findImage();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setImage(Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
    }

    private static class SearchTask extends AsyncTask<Location, Void, Void> {

        private final WeakReference<LocatrFragment> mFragmentRef;

        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;

        private SearchTask(LocatrFragment fragment) {
            mFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        protected Void doInBackground(Location... locations) {
            FlickrFetchr fetchr = new FlickrFetchr();
            List<GalleryItem> items = fetchr.searchPhotos(locations[0]);

            if (!items.isEmpty()) {
                mGalleryItem = items.get(0);

                try {
                    byte[] bytes = fetchr.getUrlBytes(mGalleryItem.getUrl());
                    mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } catch (IOException e) {
                    Log.i(LOG_TAG, "Unable to download bitmap", e);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            LocatrFragment fragment = mFragmentRef.get();
            if (fragment != null) {
                fragment.setImage(mBitmap);
            }
        }
    }
}
