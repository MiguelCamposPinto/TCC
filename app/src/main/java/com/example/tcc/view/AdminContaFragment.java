package com.example.tcc.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.tcc.R;
import com.example.tcc.controller.AuthController;


public class AdminContaFragment extends Fragment {

    private Button btnLogout;
    private AuthController authController;

    public AdminContaFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_conta, container, false);

        authController = new AuthController(requireActivity());
        btnLogout = view.findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> authController.logout());

        return view;
    }
}