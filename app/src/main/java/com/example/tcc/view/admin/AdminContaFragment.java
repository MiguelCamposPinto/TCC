package com.example.tcc.view.admin;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.tcc.R;
import com.example.tcc.model.AuthService;

public class AdminContaFragment extends Fragment {

    private Button btnLogout;
    private AuthService authService;

    public AdminContaFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_conta, container, false);

        authService = new AuthService();
        btnLogout = view.findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> authService.logout(requireActivity()));

        return view;
    }
}