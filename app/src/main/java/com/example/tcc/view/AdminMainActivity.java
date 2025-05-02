package com.example.tcc.view;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.tcc.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        bottomNav = findViewById(R.id.bottom_nav_admin);
        loadFragment(new AdminMainFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            if (item.getItemId() == R.id.nav_main) {
                selected = new AdminMainFragment();
            } else if (item.getItemId() == R.id.nav_locais) {
                selected = new AdminGerenciaFragment();
            } else if (item.getItemId() == R.id.nav_conta) {
                selected = new AdminContaFragment();
            }
            return loadFragment(selected);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
