package com.bpitindia.attendance.addsubject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SubjectAutoCompleteAdapter extends ArrayAdapter<SubjectItem> {
    private final List<SubjectItem> allSubjectItemsList;
    private List<SubjectItem> filteredSubjectItemsList;

    public SubjectAutoCompleteAdapter(@NonNull Context context, @NonNull List<SubjectItem> placesList) {
        super(context, 0, placesList);
        allSubjectItemsList = new ArrayList<>(placesList);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return placeFilter;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false
            );
        }

        TextView placeLabel = convertView.findViewById(android.R.id.text1);

        SubjectItem place = getItem(position);
        if (place != null) {
            placeLabel.setText(place.toString());
        }

        return convertView;
    }

    private final Filter placeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            filteredSubjectItemsList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredSubjectItemsList.addAll(allSubjectItemsList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (SubjectItem place : allSubjectItemsList) {
                    if (place.toString().toLowerCase().contains(filterPattern)) {
                        filteredSubjectItemsList.add(place);
                    }
                }
            }

            results.values = filteredSubjectItemsList;
            results.count = filteredSubjectItemsList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            addAll(filteredSubjectItemsList);
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return resultValue.toString();
        }
    };
}