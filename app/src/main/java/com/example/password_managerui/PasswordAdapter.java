package com.example.password_managerui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PasswordAdapter
        extends RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder> {

    private List<PasswordModel> passwordList;
    private final List<PasswordModel> filteredList;

    public interface OnPasswordClickListener {
        void onPasswordClick(PasswordModel password);
    }

    private final OnPasswordClickListener listener;

    public PasswordAdapter(
            List<PasswordModel> passwordList,
            OnPasswordClickListener listener) {

        this.passwordList = passwordList;
        this.filteredList = new ArrayList<>(passwordList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PasswordViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_password, parent, false);

        return new PasswordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PasswordViewHolder holder,
            int position) {

        PasswordModel password = filteredList.get(position);

        holder.websiteText.setText(
                password.getWebsite() == null
                        ? ""
                        : password.getWebsite()
        );

        holder.usernameText.setText(
                password.getUsername() == null
                        ? ""
                        : password.getUsername()
        );

        holder.categoryText.setText(
                password.getCategory() == null
                        ? ""
                        : password.getCategory()
        );

        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {
                listener.onPasswordClick(password);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String text) {

        filteredList.clear();

        if (text == null || text.trim().isEmpty()) {

            filteredList.addAll(passwordList);

        } else {

            String searchText =
                    text.toLowerCase().trim();

            for (PasswordModel password : passwordList) {

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

                if (website.toLowerCase().contains(searchText)
                        || username.toLowerCase().contains(searchText)
                        || category.toLowerCase().contains(searchText)) {

                    filteredList.add(password);
                }
            }
        }

        notifyDataSetChanged();
    }

    public void updateList(List<PasswordModel> newList) {

        passwordList = newList;

        filteredList.clear();
        filteredList.addAll(newList);

        notifyDataSetChanged();
    }

    public static class PasswordViewHolder
            extends RecyclerView.ViewHolder {

        TextView websiteText;
        TextView usernameText;
        TextView categoryText;

        public PasswordViewHolder(
                @NonNull View itemView) {

            super(itemView);

            websiteText =
                    itemView.findViewById(
                            R.id.websiteText
                    );

            usernameText =
                    itemView.findViewById(
                            R.id.usernameText
                    );

            categoryText =
                    itemView.findViewById(
                            R.id.categoryText
                    );
        }
    }
}