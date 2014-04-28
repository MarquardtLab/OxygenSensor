// Oxygen Sensor
// TabsPagerAdapter
//
// This class is a view pager that acts as an overall layout manager for the app
// This view pager holds the two fragments as tabs

package com.example.oxygensensor.adapter;

import com.example.oxygensensor.CalibrationFragment;
import com.example.oxygensensor.DataCollectionFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
 
public class TabsPagerAdapter extends FragmentPagerAdapter {
 
    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }
 
    @Override
    public Fragment getItem(int index) {
        switch (index) {
        case 0:
            // Top Rated fragment activity
            return new DataCollectionFragment();
        case 1:
            // Games fragment activity
            return new CalibrationFragment();
        }
        return null;
        
    }
 
    @Override
    public int getCount() {
        // get item count - equal to number of tabs
        return 2;
    }
 
}