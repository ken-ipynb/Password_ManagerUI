package com.example.password_managerui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AutoLockActivity {

    private static final int QR_SCAN_REQUEST = 200;

    private static final String PREFS_NAME = "WaultPrefs";
    private static final String PROFILE_IMAGE_URI = "profile_image_uri";

    // =========================================================
    // DRAWER
    // =========================================================

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton;

    private ImageView drawerProfileImage;
    private TextView drawerProfileName;
    private TextView drawerProfileEmail;

    // =========================================================
    // HOME
    // =========================================================

    private TextView passwordCount;
    private RecyclerView passwordRecyclerView;
    private FloatingActionButton addPasswordButton;

    // =========================================================
    // CATEGORIES
    // =========================================================

    private LinearLayout categoryContainer;
    private TextView categoryViewText;

    private String selectedCategory = "All";

    private final String[] categories = {
            "All",
            "Social",
            "Banking",
            "Work",
            "Shopping",
            "Other"
    };

    // =========================================================
    // TOP BAR
    // =========================================================

    private ImageView homeAppIcon;
    private LinearLayout titleContainer;
    private EditText topSearchInput;
    private ImageButton searchButton;
    private ImageButton closeSearchButton;

    // =========================================================
    // PASSWORD DATA
    // =========================================================

    private PasswordAdapter passwordAdapter;

    private final List<PasswordModel> passwordList =
            new ArrayList<>();

    private final List<PasswordModel> allPasswords =
            new ArrayList<>();

    private DatabaseReference databaseReference;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        // =====================================================
        // FIND VIEWS
        // =====================================================

        drawerLayout =
                findViewById(R.id.drawerLayout);

        navigationView =
                findViewById(R.id.navigationView);

        menuButton =
                findViewById(R.id.menuButton);

        passwordCount =
                findViewById(R.id.passwordCount);

        passwordRecyclerView =
                findViewById(R.id.passwordRecyclerView);

        addPasswordButton =
                findViewById(R.id.addPasswordButton);

        // =====================================================
        // CATEGORY VIEWS
        // =====================================================

        categoryContainer =
                findViewById(R.id.categoryContainer);

        categoryViewText =
                findViewById(R.id.categoryViewText);

        // =====================================================
        // TOP BAR
        // =====================================================

        homeAppIcon =
                findViewById(R.id.homeAppIcon);

        titleContainer =
                findViewById(R.id.titleContainer);

        topSearchInput =
                findViewById(R.id.topSearchInput);

        searchButton =
                findViewById(R.id.searchButton);

        closeSearchButton =
                findViewById(R.id.closeSearchButton);

        // =====================================================
        // CHECK USER
        // =====================================================

        FirebaseUser currentUser =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (currentUser == null) {

            goToLogin();
            return;
        }

        // =====================================================
        // FIREBASE
        // =====================================================

        databaseReference =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(currentUser.getUid())
                        .child("passwords");

        // =====================================================
        // SETUP
        // =====================================================

        setupDrawer();
        setupProfileHeader();
        setupRecyclerView();
        setupCategories();
        setupSearch();

        loadPasswords();

        // =====================================================
        // ADD PASSWORD
        // =====================================================

        if (addPasswordButton != null) {

            addPasswordButton.setOnClickListener(
                    v -> showAddCredentialDialog()
            );
        }
    }

    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (navigationView != null) {
            setupProfileHeader();
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {

        if (searchButton != null) {

            searchButton.setOnClickListener(
                    v -> openSearch()
            );
        }

        if (closeSearchButton != null) {

            closeSearchButton.setOnClickListener(
                    v -> closeSearch()
            );
        }

        if (topSearchInput != null) {

            topSearchInput.addTextChangedListener(
                    new TextWatcher() {

                        @Override
                        public void beforeTextChanged(
                                CharSequence s,
                                int start,
                                int count,
                                int after
                        ) {
                        }

                        @Override
                        public void onTextChanged(
                                CharSequence s,
                                int start,
                                int before,
                                int count
                        ) {

                            filterPasswords();
                        }

                        @Override
                        public void afterTextChanged(
                                Editable s
                        ) {
                        }
                    }
            );
        }
    }

    // =========================================================
    // OPEN SEARCH
    // =========================================================

    private void openSearch() {

        if (homeAppIcon != null) {
            homeAppIcon.setVisibility(View.GONE);
        }

        if (titleContainer != null) {
            titleContainer.setVisibility(View.GONE);
        }

        if (searchButton != null) {
            searchButton.setVisibility(View.GONE);
        }

        if (topSearchInput != null) {

            topSearchInput.setVisibility(View.VISIBLE);

            topSearchInput.requestFocus();

            topSearchInput.post(() -> {

                InputMethodManager imm =
                        (InputMethodManager)
                                getSystemService(
                                        Context.INPUT_METHOD_SERVICE
                                );

                if (imm != null) {

                    imm.showSoftInput(
                            topSearchInput,
                            InputMethodManager.SHOW_IMPLICIT
                    );
                }
            });
        }

        if (closeSearchButton != null) {
            closeSearchButton.setVisibility(View.VISIBLE);
        }
    }

    // =========================================================
    // CLOSE SEARCH
    // =========================================================

    private void closeSearch() {

        if (topSearchInput != null) {

            topSearchInput.setText("");
            topSearchInput.clearFocus();
            topSearchInput.setVisibility(View.GONE);
        }

        if (closeSearchButton != null) {
            closeSearchButton.setVisibility(View.GONE);
        }

        if (homeAppIcon != null) {
            homeAppIcon.setVisibility(View.VISIBLE);
        }

        if (titleContainer != null) {
            titleContainer.setVisibility(View.VISIBLE);
        }

        if (searchButton != null) {
            searchButton.setVisibility(View.VISIBLE);
        }

        InputMethodManager imm =
                (InputMethodManager)
                        getSystemService(
                                Context.INPUT_METHOD_SERVICE
                        );

        if (imm != null &&
                topSearchInput != null) {

            imm.hideSoftInputFromWindow(
                    topSearchInput.getWindowToken(),
                    0
            );
        }

        filterPasswords();
    }

    // =========================================================
    // DRAWER
    // =========================================================

    private void setupDrawer() {

        if (menuButton != null) {

            menuButton.setOnClickListener(
                    v -> drawerLayout.openDrawer(
                            GravityCompat.START
                    )
            );
        }

        if (navigationView == null) {
            return;
        }

        navigationView.setNavigationItemSelectedListener(
                item -> {

                    int id = item.getItemId();

                    if (id == R.id.nav_home) {

                        drawerLayout.closeDrawer(
                                GravityCompat.START
                        );

                    } else if (id == R.id.nav_profile) {

                        openSettingsSection("profile");

                    } else if (id == R.id.nav_app_lock) {

                        openSettingsSection("app_lock");

                    } else if (id == R.id.nav_change_pin) {

                        openSettingsSection("change_pin");

                    } else if (id == R.id.nav_auto_lock) {

                        openSettingsSection("auto_lock");

                    } else if (id == R.id.nav_backup) {

                        openSettingsSection("backup");

                    } else if (id == R.id.nav_delete_account) {

                        openSettingsSection("delete_account");

                    } else if (id == R.id.nav_about) {

                        drawerLayout.closeDrawer(
                                GravityCompat.START
                        );

                        showAboutDialog();

                    } else if (id == R.id.nav_logout) {

                        drawerLayout.closeDrawer(
                                GravityCompat.START
                        );

                        showLogoutConfirmation();
                    }

                    return true;
                }
        );
    }

    // =========================================================
    // PROFILE HEADER
    // =========================================================

    private void setupProfileHeader() {

        if (navigationView == null) {
            return;
        }

        View headerView =
                navigationView.getHeaderView(0);

        if (headerView == null) {
            return;
        }

        drawerProfileImage =
                headerView.findViewById(
                        R.id.profileImage
                );

        drawerProfileName =
                headerView.findViewById(
                        R.id.profileName
                );

        drawerProfileEmail =
                headerView.findViewById(
                        R.id.profileEmail
                );

        FirebaseUser user =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (user == null) {

            drawerProfileName.setText(
                    "Wault User"
            );

            drawerProfileEmail.setText(
                    "No email available"
            );

            drawerProfileImage.setImageResource(
                    android.R.drawable.ic_menu_myplaces
            );

            return;
        }

        String name =
                user.getDisplayName();

        if (name != null &&
                !name.trim().isEmpty()) {

            drawerProfileName.setText(name);

        } else {

            drawerProfileName.setText(
                    "Wault User"
            );
        }

        String email =
                user.getEmail();

        if (email != null &&
                !email.trim().isEmpty()) {

            drawerProfileEmail.setText(email);

        } else {

            drawerProfileEmail.setText(
                    "No email available"
            );
        }

        loadDrawerProfileImage(user);
    }

    // =========================================================
    // PROFILE IMAGE
    // =========================================================

    private void loadDrawerProfileImage(
            FirebaseUser user
    ) {

        if (drawerProfileImage == null) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        String savedPath =
                prefs.getString(
                        PROFILE_IMAGE_URI,
                        null
                );

        Glide.with(this)
                .clear(drawerProfileImage);

        if (savedPath != null &&
                !savedPath.trim().isEmpty()) {

            File profileFile =
                    new File(savedPath);

            if (profileFile.exists() &&
                    profileFile.isFile() &&
                    profileFile.length() > 0) {

                Glide.with(this)
                        .load(profileFile)
                        .circleCrop()
                        .into(drawerProfileImage);

                return;
            }

            prefs.edit()
                    .remove(PROFILE_IMAGE_URI)
                    .apply();
        }

        if (user != null &&
                user.getPhotoUrl() != null) {

            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(drawerProfileImage);

            return;
        }

        drawerProfileImage.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );
    }

    // =========================================================
    // SETTINGS
    // =========================================================

    private void openSettingsSection(
            String section
    ) {

        drawerLayout.closeDrawer(
                GravityCompat.START
        );

        drawerLayout.postDelayed(
                () -> {

                    Intent intent =
                            new Intent(
                                    HomeActivity.this,
                                    SettingsActivity.class
                            );

                    intent.putExtra(
                            "open_section",
                            section
                    );

                    startActivity(intent);

                },
                220
        );
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private void setupRecyclerView() {

        passwordAdapter =
                new PasswordAdapter(
                        passwordList,
                        password -> {

                            Intent intent =
                                    new Intent(
                                            HomeActivity.this,
                                            PasswordDetailsActivity.class
                                    );

                            intent.putExtra(
                                    "passwordId",
                                    password.getId()
                            );

                            intent.putExtra(
                                    "website",
                                    password.getWebsite()
                            );

                            intent.putExtra(
                                    "username",
                                    password.getUsername()
                            );

                            intent.putExtra(
                                    "password",
                                    password.getPassword()
                            );

                            intent.putExtra(
                                    "category",
                                    password.getCategory()
                            );

                            startActivity(intent);
                        }
                );

        passwordRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        passwordRecyclerView.setAdapter(
                passwordAdapter
        );
    }

    // =========================================================
    // CATEGORIES
    // =========================================================

    private void setupCategories() {

        createCategoryChips();

        if (categoryViewText != null) {

            categoryViewText.setOnClickListener(
                    v -> {

                        // CLEAR = return to All

                        selectedCategory = "All";

                        updateCategoryChips();

                        filterPasswords();
                    }
            );
        }
    }

    // =========================================================
    // CREATE CATEGORY CHIPS
    // =========================================================

    private void createCategoryChips() {

        if (categoryContainer == null) {
            return;
        }

        categoryContainer.removeAllViews();

        for (String category : categories) {

            TextView chip =
                    new TextView(this);

            chip.setText(category);
            chip.setTextSize(16);
            chip.setGravity(Gravity.CENTER);

            chip.setMinHeight(
                    dpToPx(54)
            );

            chip.setPadding(
                    dpToPx(22),
                    0,
                    dpToPx(22),
                    0
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            dpToPx(54)
                    );

            params.setMargins(
                    0,
                    0,
                    dpToPx(8),
                    0
            );

            chip.setLayoutParams(params);

            chip.setClickable(true);
            chip.setFocusable(true);

            chip.setOnClickListener(
                    v -> {

                        selectedCategory = category;

                        updateCategoryChips();

                        filterPasswords();
                    }
            );

            categoryContainer.addView(chip);
        }

        updateCategoryChips();
    }

    // =========================================================
    // UPDATE CATEGORY CHIP APPEARANCE
    // =========================================================

    private void updateCategoryChips() {

        if (categoryContainer == null) {
            return;
        }

        for (int i = 0;
             i < categoryContainer.getChildCount();
             i++) {

            View view =
                    categoryContainer.getChildAt(i);

            if (!(view instanceof TextView)) {
                continue;
            }

            TextView chip =
                    (TextView) view;

            String category =
                    chip.getText()
                            .toString();

            GradientDrawable background =
                    new GradientDrawable();

            background.setCornerRadius(
                    dpToPx(30)
            );

            if (category.equalsIgnoreCase(
                    selectedCategory
            )) {

                // SELECTED

                background.setColor(
                        Color.rgb(
                                108,
                                99,
                                255
                        )
                );

                chip.setTextColor(
                        Color.WHITE
                );

            } else {

                // NORMAL

                background.setColor(
                        Color.rgb(
                                23,
                                29,
                                40
                        )
                );

                chip.setTextColor(
                        Color.rgb(
                                210,
                                214,
                                224
                        )
                );
            }

            chip.setBackground(background);
        }
    }

    // =========================================================
    // DP TO PX
    // =========================================================

    private int dpToPx(int dp) {

        return Math.round(
                dp *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // =========================================================
    // ADD CREDENTIAL
    // =========================================================

    private void showAddCredentialDialog() {

        String[] options = {
                "🔐  Manual Password",
                "📷  Scan QR Code"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Credential")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                openManualPassword();

                            } else {

                                openQRScanner();
                            }
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void openManualPassword() {

        startActivity(
                new Intent(
                        HomeActivity.this,
                        AddPasswordActivity.class
                )
        );
    }

    // =========================================================
    // QR SCANNER
    // =========================================================

    private void openQRScanner() {

        startActivityForResult(
                new Intent(
                        HomeActivity.this,
                        QRScannerActivity.class
                ),
                QR_SCAN_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != QR_SCAN_REQUEST ||
                resultCode != RESULT_OK ||
                data == null) {

            return;
        }

        String qrResult =
                data.getStringExtra(
                        "qr_result"
                );

        if (qrResult == null ||
                qrResult.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid QR data",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        processQRResult(qrResult);
    }

    // =========================================================
    // PROCESS QR
    // =========================================================

    private void processQRResult(
            String qrResult
    ) {

        try {

            JSONObject jsonObject =
                    new JSONObject(qrResult);

            String website =
                    jsonObject.optString(
                            "website",
                            ""
                    ).trim();

            String username =
                    jsonObject.optString(
                            "username",
                            ""
                    ).trim();

            String password =
                    jsonObject.optString(
                            "password",
                            ""
                    ).trim();

            String category =
                    jsonObject.optString(
                            "category",
                            "Other"
                    ).trim();

            if (website.isEmpty() ||
                    username.isEmpty() ||
                    password.isEmpty()) {

                Toast.makeText(
                        this,
                        "QR data is incomplete",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            findExistingPassword(
                    website,
                    username,
                    password,
                    category
            );

        } catch (JSONException e) {

            Toast.makeText(
                    this,
                    "Invalid Wault QR code",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // FIND EXISTING PASSWORD
    // =========================================================

    private void findExistingPassword(
            String website,
            String username,
            String password,
            String category
    ) {

        databaseReference
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                String existingId = null;

                                for (DataSnapshot child :
                                        snapshot.getChildren()) {

                                    String existingWebsite =
                                            child.child("website")
                                                    .getValue(
                                                            String.class
                                                    );

                                    String existingUsername =
                                            child.child("username")
                                                    .getValue(
                                                            String.class
                                                    );

                                    if (existingWebsite != null &&
                                            existingUsername != null &&
                                            existingWebsite.equalsIgnoreCase(
                                                    website
                                            ) &&
                                            existingUsername.equalsIgnoreCase(
                                                    username
                                            )) {

                                        existingId =
                                                child.getKey();

                                        break;
                                    }
                                }

                                if (existingId != null) {

                                    updatePasswordFromQR(
                                            existingId,
                                            website,
                                            username,
                                            password,
                                            category
                                    );

                                } else {

                                    addPasswordFromQR(
                                            website,
                                            username,
                                            password,
                                            category
                                    );
                                }
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Failed to check vault",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // UPDATE QR
    // =========================================================

    private void updatePasswordFromQR(
            String id,
            String website,
            String username,
            String password,
            String category
    ) {

        PasswordModel model =
                new PasswordModel(
                        id,
                        website,
                        username,
                        password,
                        category
                );

        databaseReference
                .child(id)
                .setValue(model)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            HomeActivity.this,
                            "Password updated from QR",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadPasswords();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                HomeActivity.this,
                                "Failed to update password",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    // =========================================================
    // ADD QR
    // =========================================================

    private void addPasswordFromQR(
            String website,
            String username,
            String password,
            String category
    ) {

        String id =
                databaseReference
                        .push()
                        .getKey();

        if (id == null) {

            Toast.makeText(
                    HomeActivity.this,
                    "Failed to generate password ID",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        PasswordModel model =
                new PasswordModel(
                        id,
                        website,
                        username,
                        password,
                        category
                );

        databaseReference
                .child(id)
                .setValue(model)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            HomeActivity.this,
                            "Password added from QR",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadPasswords();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                HomeActivity.this,
                                "Failed to add password",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    // =========================================================
    // LOAD PASSWORDS
    // =========================================================

    private void loadPasswords() {

        databaseReference
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                allPasswords.clear();

                                for (DataSnapshot child :
                                        snapshot.getChildren()) {

                                    PasswordModel model =
                                            child.getValue(
                                                    PasswordModel.class
                                            );

                                    if (model != null) {

                                        allPasswords.add(model);
                                    }
                                }

                                passwordCount.setText(
                                        String.valueOf(
                                                allPasswords.size()
                                        )
                                );

                                filterPasswords();
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Failed to load passwords",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // FILTER PASSWORDS
    // =========================================================

    private void filterPasswords() {

        if (passwordAdapter == null) {
            return;
        }

        String searchText = "";

        if (topSearchInput != null) {

            searchText =
                    topSearchInput
                            .getText()
                            .toString()
                            .toLowerCase()
                            .trim();
        }

        passwordList.clear();

        for (PasswordModel password :
                allPasswords) {

            String website =
                    password.getWebsite() == null
                            ? ""
                            : password.getWebsite();

            String username =
                    password.getUsername() == null
                            ? ""
                            : password.getUsername();

            String category =
                    password.getCategory() == null
                            ? ""
                            : password.getCategory();

            boolean matchesSearch =
                    website.toLowerCase()
                            .contains(searchText)
                            ||
                            username.toLowerCase()
                                    .contains(searchText);

            boolean matchesCategory =
                    selectedCategory.equalsIgnoreCase("All")
                            ||
                            category.equalsIgnoreCase(
                                    selectedCategory
                            );

            if (matchesSearch &&
                    matchesCategory) {

                passwordList.add(password);
            }
        }

        passwordAdapter.updateList(
                passwordList
        );
    }

    // =========================================================
    // ABOUT
    // =========================================================

    private void showAboutDialog() {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About Wault.io")
                .setMessage(
                        "Wault.io\n\n" +
                                "Your secure digital password vault.\n\n" +
                                "Store, manage and organize your passwords " +
                                "in one place.\n\n" +
                                "Version 1.0"
                )
                .setPositiveButton(
                        "Done",
                        null
                )
                .show();
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void showLogoutConfirmation() {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(
                        "Are you sure you want to logout?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Logout",
                        (dialog, which) -> logout()
                )
                .show();
    }

    private void logout() {

        FirebaseAuth.getInstance()
                .signOut();

        Intent intent =
                new Intent(
                        HomeActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent =
                new Intent(
                        HomeActivity.this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // BACK
    // =========================================================

    @Override
    public void onBackPressed() {

        if (topSearchInput != null &&
                topSearchInput.getVisibility() ==
                        View.VISIBLE) {

            closeSearch();

            return;
        }

        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(
                        GravityCompat.START
                )) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );

            return;
        }

        super.onBackPressed();
    }
}