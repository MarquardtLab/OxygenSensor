// Oxygen Sensor
// DataCollectionFragment
//
// This fragment defines the layout of the data collection tab
// look at the fragment_data_collection.xml file in the layout
// package of the res folder to see the views

package com.example.oxygensensor;

import com.example.oxygensensor.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
 
public class DataCollectionFragment extends Fragment {
 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
 
        View rootView = inflater.inflate(R.layout.fragment_data_collection, container, false);
         
        return rootView;
    }
}