package com.example.capstone;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Button Task Assignment
        Button btnTaskAssignment = view.findViewById(R.id.btnTaskAssignment);
        btnTaskAssignment.setOnClickListener(v -> {
            Fragment taskFragment = new TaskAssignmentFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, taskFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Button Set Schedule
        Button btnSetSchedule = view.findViewById(R.id.btnSetSchedule);
        btnSetSchedule.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new ScheduleFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Tambahkan listener untuk tombol lainnya jika diperlukan
    }
}