package org.ole.planet.myplanet.ui.library;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.JsonObject;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.callback.OnHomeItemClickListener;
import org.ole.planet.myplanet.callback.OnLibraryItemSelected;
import org.ole.planet.myplanet.callback.OnRatingChangeListener;
import org.ole.planet.myplanet.databinding.RowLibraryBinding;
import org.ole.planet.myplanet.model.RealmMyLibrary;
import org.ole.planet.myplanet.model.RealmTag;
import org.ole.planet.myplanet.ui.course.AdapterCourses;
import org.ole.planet.myplanet.utilities.TimeUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fisk.chipcloud.ChipCloud;
import fisk.chipcloud.ChipCloudConfig;
import io.noties.markwon.Markwon;
import io.noties.markwon.movement.MovementMethodPlugin;
import io.realm.Realm;

public class AdapterLibrary extends RecyclerView.Adapter<AdapterLibrary.ViewHolderLibrary> {
    private RowLibraryBinding rowLibraryBinding;
    private Context context;
    private List<RealmMyLibrary> libraryList;
    private List<RealmMyLibrary> selectedItems;
    private OnLibraryItemSelected listener;
    private ChipCloudConfig config;
    private OnHomeItemClickListener homeItemClickListener;
    private HashMap<String, JsonObject> ratingMap;
    private OnRatingChangeListener ratingChangeListener;
    private Markwon markwon;
    private Realm realm;
    private boolean areAllSelected = true;

    public AdapterLibrary(Context context, List<RealmMyLibrary> libraryList, HashMap<String, JsonObject> ratingMap, Realm realm) {
        this.ratingMap = ratingMap;
        this.context = context;
        markwon = Markwon.builder(context)
                .usePlugin(MovementMethodPlugin.none())
                .build();
        this.realm = realm;
        this.libraryList = libraryList;
        this.selectedItems = new ArrayList<>();
        config = Utilities.getCloudConfig().selectMode(ChipCloud.SelectMode.single);
        if (context instanceof OnHomeItemClickListener) {
            homeItemClickListener = (OnHomeItemClickListener) context;
        }
    }

    public void setRatingChangeListener(OnRatingChangeListener ratingChangeListener) {
        this.ratingChangeListener = ratingChangeListener;
    }

    public List<RealmMyLibrary> getLibraryList() {
        return libraryList;
    }

    public void setLibraryList(List<RealmMyLibrary> libraryList) {
        this.libraryList = libraryList;
        notifyDataSetChanged();
    }

    public void setListener(OnLibraryItemSelected listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolderLibrary onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        rowLibraryBinding = RowLibraryBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolderLibrary(rowLibraryBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderLibrary holder, final int position) {
        Utilities.log("On bind " + position);
        rowLibraryBinding.title.setText(libraryList.get(position).getTitle());
        Utilities.log(libraryList.get(position).getDescription());
        markwon.setMarkdown(rowLibraryBinding.description, libraryList.get(position).getDescription());
        rowLibraryBinding.timesRated.setText(libraryList.get(position).getTimesRated() + context.getString(R.string.total));
        rowLibraryBinding.checkbox.setChecked(selectedItems.contains(libraryList.get(position)));
        rowLibraryBinding.rating.setText(TextUtils.isEmpty(libraryList.get(position).getAverageRating()) ? "0.0" : String.format("%.1f", Double.parseDouble(libraryList.get(position).getAverageRating())));
        rowLibraryBinding.tvDate.setText(TimeUtils.formatDate(libraryList.get(position).getCreatedDate(), "MMM dd, yyyy"));
        displayTagCloud(rowLibraryBinding.flexboxDrawable, position);
        holder.itemView.setOnClickListener(view -> openLibrary(libraryList.get(position)));
        rowLibraryBinding.ivDownloaded.setImageResource(libraryList.get(position).isResourceOffline() ? R.drawable.ic_eye : R.drawable.ic_download);
        if (ratingMap.containsKey(libraryList.get(position).getResource_id())) {
            JsonObject object = ratingMap.get(libraryList.get(position).getResource_id());
            AdapterCourses.showRating(object, rowLibraryBinding.rating, rowLibraryBinding.timesRated, rowLibraryBinding.ratingBar);
        } else {
            rowLibraryBinding.ratingBar.setRating(0);
        }

        rowLibraryBinding.checkbox.setOnClickListener((view) -> {
            Utilities.handleCheck(((CheckBox) view).isChecked(), position, (ArrayList) selectedItems, libraryList);
            if (listener != null) listener.onSelectedListChange(selectedItems);
            notifyDataSetChanged();
        });
    }

    public boolean areAllSelected(){
        if (selectedItems.size() != libraryList.size()) {
            areAllSelected = false;
        }
        return areAllSelected;
    }
    public void selectAllItems(boolean selectAll) {
        if (selectAll) {
            selectedItems.clear();
            selectedItems.addAll(libraryList);
        } else {
            selectedItems.clear();
        }

        notifyDataSetChanged();

        if (listener != null) {
            listener.onSelectedListChange(selectedItems);
        }
    }

    private void openLibrary(RealmMyLibrary library) {
        if (homeItemClickListener != null) {
            homeItemClickListener.openLibraryDetailFragment(library);
        }
    }

    private void displayTagCloud(FlexboxLayout flexboxDrawable, int position) {
        flexboxDrawable.removeAllViews();
        final ChipCloud chipCloud = new ChipCloud(context, flexboxDrawable, config);
        List<RealmTag> tags = realm.where(RealmTag.class).equalTo("db", "resources").equalTo("linkId", libraryList.get(position).getId()).findAll();
        for (RealmTag tag : tags) {
            RealmTag parent = realm.where(RealmTag.class).equalTo("id", tag.getTagId()).findFirst();
            try {
                chipCloud.addChip(parent.getName());
            } catch (Exception err) {
                chipCloud.addChip("--");
            }
            chipCloud.setListener((i, b, b1) -> {
                if (b1 && listener != null) {
                    listener.onTagClicked(parent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return libraryList.size();
    }

    class ViewHolderLibrary extends RecyclerView.ViewHolder {
        RowLibraryBinding rowLibraryBinding;
        public ViewHolderLibrary(RowLibraryBinding rowLibraryBinding) {
            super(rowLibraryBinding.getRoot());
            this.rowLibraryBinding = rowLibraryBinding;
            rowLibraryBinding.ratingBar.setOnTouchListener((v1, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    homeItemClickListener.showRatingDialog("resource", libraryList.get(getAdapterPosition()).getResource_id(), libraryList.get(getAdapterPosition()).getTitle(), ratingChangeListener);
                return true;
            });
        }
    }
}
