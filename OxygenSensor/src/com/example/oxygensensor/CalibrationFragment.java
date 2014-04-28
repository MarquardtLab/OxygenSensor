// Oxygen Sensor
// CalibrationFragment
//
// This fragment defines the layout of the calibration tab
// look at the fragment_calibration.xml file in the layout
// package of the res folder to see the views

package com.example.oxygensensor;

import com.example.oxygensensor.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
 
public class CalibrationFragment extends Fragment {
 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
 
        View rootView = inflater.inflate(R.layout.fragment_calibration, container, false);
         
        return rootView;
    }
}